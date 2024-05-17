/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.config.JvmDefaultMode.ALL_COMPATIBILITY
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_DEFAULT_NO_COMPATIBILITY_FQ_NAME
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.*
import org.jetbrains.kotlin.resolve.LanguageVersionSettingsProvider
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPrivateApi
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_DEFAULT_WITH_COMPATIBILITY_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmDefaultNoCompatibilityAnnotation
import org.jetbrains.kotlin.resolve.jvm.annotations.isCompiledToJvmDefault
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.util.getNonPrivateTraitMembersForDelegation
import org.jetbrains.kotlin.util.findNonPrivateInterfaceImplementation

/**
 * Checks several things related to the interoperability of `-Xjvm-default` modes, as well as compatibility annotations:
 *
 * 1. Report an error if `@JvmDefaultWithCompatibility` or `@JvmDefaultWithoutCompatibility` is applied incorrectly.
 * 2. "Specialization check" (KT-39603): report an error if a class compiled in the 'all-compatibility' mode inherits a non-abstract
 *    interface member with a signature change (e.g. because of generic specialization).
 * 3. "Mixed mode check": report an error in case an implicit override in base class compiled in the 'disable' mode would incorrectly be
 *    called at runtime in a diamond hierarchy:
 *    ```
 *    // -Xjvm-default=disable
 *    interface A { fun f() {} }
 *    open class AImpl : A
 *
 *    // -Xjvm-default=all/all-compatibility
 *    interface B { override fun f() {} }
 *    class BImpl : AImpl(), B
 *    // ^ error, because otherwise `BImpl().f()` would call the implementation from A, not from B.
 *    ```
 *
 * Note that if the specialization check reports an error for a member, the mixed mode check is not run for that member.
 */
class JvmDefaultChecker(project: Project) : DeclarationChecker {
    private val ideService = LanguageVersionSettingsProvider.getInstance(project)

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val jvmDefaultMode = context.languageVersionSettings.getFlag(JvmAnalysisFlags.jvmDefaultMode)

        if (checkJvmCompatibilityAnnotations(descriptor, declaration, context, jvmDefaultMode)) return

        if (!jvmDefaultMode.isEnabled || descriptor !is ClassDescriptor || isInterface(descriptor) || isAnnotationClass(descriptor)) return

        val performSpecializationCheck =
            jvmDefaultMode == ALL_COMPATIBILITY && !descriptor.hasJvmDefaultNoCompatibilityAnnotation() &&
                    //TODO: maybe remove this check for JVM compatibility
                    (descriptor.modality == Modality.OPEN || descriptor.modality == Modality.ABSTRACT) &&
                    !descriptor.isEffectivelyPrivateApi

        if (!performSpecializationCheck && descriptor.getSuperClassNotAny() == null) return

        for ((fakeOverride, implementation) in getNonPrivateTraitMembersForDelegation(descriptor, returnImplNotDelegate = true)) {
            if (!implementation.isCompiledToJvmDefaultWithProperMode(jvmDefaultMode)) continue

            var diagnostic: Diagnostic? = null
            if (performSpecializationCheck) {
                if (implementation is FunctionDescriptor && fakeOverride is FunctionDescriptor) {
                    diagnostic = checkSpecializationInCompatibilityMode(fakeOverride, implementation, declaration)
                } else if (implementation is PropertyDescriptor && fakeOverride is PropertyDescriptor) {
                    diagnostic = checkSpecializationInCompatibilityMode(fakeOverride.getter, implementation.getter, declaration)
                        ?: checkSpecializationInCompatibilityMode(fakeOverride.setter, implementation.setter, declaration)
                }
            }

            if (diagnostic == null) {
                diagnostic = checkDiamondInheritanceInMixedMode(fakeOverride, implementation, jvmDefaultMode, declaration)
            }

            diagnostic?.let(context.trace::report)
        }
    }

    private fun checkJvmCompatibilityAnnotations(
        descriptor: DeclarationDescriptor,
        declaration: KtDeclaration,
        context: DeclarationCheckerContext,
        jvmDefaultMode: JvmDefaultMode
    ): Boolean {
        descriptor.annotations.findAnnotation(JVM_DEFAULT_NO_COMPATIBILITY_FQ_NAME)?.let { annotationDescriptor ->
            val reportOn = DescriptorToSourceUtils.getSourceFromAnnotation(annotationDescriptor) ?: declaration
            if (!jvmDefaultMode.isEnabled) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_IN_DECLARATION.on(reportOn, "JvmDefaultWithoutCompatibility"))
                return true
            }
        }

        descriptor.annotations.findAnnotation(JVM_DEFAULT_WITH_COMPATIBILITY_FQ_NAME)?.let { annotationDescriptor ->
            val reportOn = DescriptorToSourceUtils.getSourceFromAnnotation(annotationDescriptor) ?: declaration
            if (jvmDefaultMode != JvmDefaultMode.ALL) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_WITH_COMPATIBILITY_IN_DECLARATION.on(reportOn))
                return true
            } else if (!isInterface(descriptor)) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_WITH_COMPATIBILITY_NOT_ON_INTERFACE.on(reportOn))
                return true
            }
        }

        return false
    }

    private fun checkSpecializationInCompatibilityMode(
        fakeOverride: FunctionDescriptor?,
        implementation: FunctionDescriptor?,
        declaration: KtDeclaration,
    ): Diagnostic? {
        if (implementation is JavaMethodDescriptor) return null
        if (fakeOverride == null || implementation == null) return null
        val inheritedSignature = fakeOverride.computeJvmDescriptor(withReturnType = true, withName = false)
        val originalImplementation = implementation.original
        val actualSignature = originalImplementation.computeJvmDescriptor(withReturnType = true, withName = false)
        if (inheritedSignature != actualSignature) {
            //NB: this diagnostics should be a bit tuned, see box/jvm8/defaults/allCompatibility/kt14243_2.kt for details
            return ErrorsJvm.EXPLICIT_OVERRIDE_REQUIRED_IN_COMPATIBILITY_MODE.on(
                declaration, getDirectMember(fakeOverride), getDirectMember(originalImplementation)
            )
        }
        return null
    }

    private fun checkDiamondInheritanceInMixedMode(
        fakeOverride: CallableMemberDescriptor,
        implementation: CallableMemberDescriptor,
        jvmDefaultMode: JvmDefaultMode,
        declaration: KtDeclaration,
    ): Diagnostic? {
        val clashMember = findProblematicDiamondMember(fakeOverride, jvmDefaultMode) ?: return null
        return ErrorsJvm.EXPLICIT_OVERRIDE_REQUIRED_IN_MIXED_MODE.on(
            declaration, getDirectMember(implementation), getDirectMember(clashMember), jvmDefaultMode.description
        )
    }

    private fun findProblematicDiamondMember(
        fakeOverride: CallableMemberDescriptor,
        jvmDefaultMode: JvmDefaultMode
    ): CallableMemberDescriptor? {
        val classDescriptor = fakeOverride.containingDeclaration
        if (classDescriptor !is ClassDescriptor || classDescriptor.getSuperClassNotAny() == null) return null
        val classMembers =
            fakeOverride.overriddenDescriptors.filter { !isInterface(it.containingDeclaration) && !isAnnotationClass(it.containingDeclaration) }
        val implicitDefaultImplsDelegate =
            classMembers.firstOrNull {
                //TODO: additional processing for platform dependent method is required (https://youtrack.jetbrains.com/issue/KT-42697)
                it !is JavaCallableMemberDescriptor &&
                        findNonPrivateInterfaceImplementation(it, true)?.isCompiledToJvmDefaultWithProperMode(jvmDefaultMode) == false
            }
        if (implicitDefaultImplsDelegate != null) return implicitDefaultImplsDelegate
        return classMembers.firstNotNullOfOrNull { findProblematicDiamondMember(it, jvmDefaultMode) }
    }

    private fun CallableMemberDescriptor.isCompiledToJvmDefaultWithProperMode(compilationDefaultMode: JvmDefaultMode) =
        isCompiledToJvmDefaultWithProperMode(ideService, compilationDefaultMode)
}

internal fun CallableMemberDescriptor.isCompiledToJvmDefaultWithProperMode(
    ideService: LanguageVersionSettingsProvider?,
    compilationDefaultMode: JvmDefaultMode
): Boolean {
    val jvmDefault =
        if (this is DeserializedDescriptor) compilationDefaultMode/*doesn't matter*/ else ideService?.getModuleLanguageVersionSettings(module)
            ?.getFlag(JvmAnalysisFlags.jvmDefaultMode) ?: compilationDefaultMode
    return isCompiledToJvmDefault(jvmDefault)
}
