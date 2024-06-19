/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.config.JvmDefaultMode.ALL_COMPATIBILITY
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.isAnnotationClass
import org.jetbrains.kotlin.descriptors.isClass
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.isCompiledToJvmDefault
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.isSubstitutionOverride
import org.jetbrains.kotlin.fir.java.jvmDefaultModeState
import org.jetbrains.kotlin.fir.originalForSubstitutionOverride
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmDescriptor
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_DEFAULT_NO_COMPATIBILITY_CLASS_ID
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_DEFAULT_WITH_COMPATIBILITY_CLASS_ID
import org.jetbrains.kotlin.name.StandardClassIds

object FirJvmDefaultChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (checkJvmCompatibilityAnnotations(declaration, context, reporter)) return

        val jvmDefaultMode = context.session.jvmDefaultModeState
        if (!jvmDefaultMode.isEnabled || declaration !is FirClass ||
            declaration.isInterface || declaration.classKind.isAnnotationClass
        ) return

        val performSpecializationCheck =
            jvmDefaultMode == ALL_COMPATIBILITY && !declaration.hasAnnotation(JVM_DEFAULT_NO_COMPATIBILITY_CLASS_ID, context.session) &&
                    //TODO: maybe remove this check for JVM compatibility
                    (declaration.modality == Modality.OPEN || declaration.modality == Modality.ABSTRACT) &&
                    !declaration.effectiveVisibility.privateApi

        if (!performSpecializationCheck && declaration.getSuperClassNotAny(context.session) == null) return

        val scope = declaration.unsubstitutedScope(context)
        scope.processAllProperties {
            checkSpecializationAndMixedMode(it, declaration, context, reporter, performSpecializationCheck)
        }
        scope.processAllFunctions {
            checkSpecializationAndMixedMode(it, declaration, context, reporter, performSpecializationCheck)
        }
    }

    private fun checkJvmCompatibilityAnnotations(
        declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter,
    ): Boolean {
        val jvmDefaultMode = context.session.jvmDefaultModeState
        val annotationNoCompatibility = declaration.getAnnotationByClassId(JVM_DEFAULT_NO_COMPATIBILITY_CLASS_ID, context.session)
        if (annotationNoCompatibility != null) {
            val source = annotationNoCompatibility.source
            if (!jvmDefaultMode.isEnabled) {
                reporter.reportOn(source, FirJvmErrors.JVM_DEFAULT_IN_DECLARATION, "JvmDefaultWithoutCompatibility", context)
                return true
            }
        }
        val annotationWithCompatibility = declaration.getAnnotationByClassId(JVM_DEFAULT_WITH_COMPATIBILITY_CLASS_ID, context.session)
        if (annotationWithCompatibility != null) {
            val source = annotationWithCompatibility.source
            when {
                jvmDefaultMode != JvmDefaultMode.ALL -> {
                    reporter.reportOn(source, FirJvmErrors.JVM_DEFAULT_WITH_COMPATIBILITY_IN_DECLARATION, context)
                    return true
                }
                declaration !is FirRegularClass || !declaration.isInterface -> {
                    reporter.reportOn(source, FirJvmErrors.JVM_DEFAULT_WITH_COMPATIBILITY_NOT_ON_INTERFACE, context)
                    return true
                }
            }
        }
        return false
    }

    private fun checkSpecializationAndMixedMode(
        symbol: FirCallableSymbol<*>,
        klass: FirClass,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        performSpecializationCheck: Boolean,
    ) {
        if (performSpecializationCheck && symbol.isSubstitutionOverride) {
            if (checkSpecializationInCompatibilityMode(symbol, klass, context, reporter)) return
        }

        // TODO (KT-71221): report EXPLICIT_OVERRIDE_REQUIRED_IN_MIXED_MODE
    }

    private fun checkSpecializationInCompatibilityMode(
        symbol: FirCallableSymbol<*>, klass: FirClass, context: CheckerContext, reporter: DiagnosticReporter,
    ): Boolean {
        val original = symbol.originalForSubstitutionOverride ?: return false

        // If the original is not compiled to JVM default, it means that its body is in the `DefaultImpls` class, so the backend _must_
        // generate a bridge, and reporting an error is incorrect.
        if (!original.isCompiledToJvmDefault(context.session, ALL_COMPATIBILITY)) return false

        val symbolContainingClass = symbol.getContainingSymbol(context.session) as? FirClassSymbol ?: return false
        // If the substitution override is located in some class which is not the class where the check has originated from, it means that
        // either the bridge is generated, or the error is reported on that class, so there's no need to report it again in the subclass.
        if (symbolContainingClass.classId != klass.classId && symbolContainingClass.isClass) return false

        if (symbol.jvmSignature == original.jvmSignature) return false

        reporter.reportOn(klass.source, FirJvmErrors.EXPLICIT_OVERRIDE_REQUIRED_IN_COMPATIBILITY_MODE, symbol, original, context)
        return true
    }

    @OptIn(SymbolInternals::class)
    private val FirCallableSymbol<*>.jvmSignature: String
        get() = when (this) {
            is FirFunctionSymbol<*> -> fir.computeJvmDescriptor()
            is FirPropertySymbol -> fir.computeJvmDescriptor()
            else -> error("Unknown callable: $this")
        }

    private fun FirClass.getSuperClassNotAny(session: FirSession): FirClassSymbol<*>? =
        superConeTypes.firstNotNullOfOrNull { supertype ->
            val klass = supertype.fullyExpandedType(session).lookupTag.toSymbol(session) as? FirClassSymbol
            klass?.takeIf { it.classKind.isClass || it.isEnumClass }
        }?.takeUnless { it.classId == StandardClassIds.Any }
}
