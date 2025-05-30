/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi.text

import com.intellij.openapi.util.IntellijInternalApi
import org.jetbrains.kotlin.analysis.decompiler.stub.COMPILED_DEFAULT_INITIALIZER
import org.jetbrains.kotlin.analysis.decompiler.stub.COMPILED_DEFAULT_PARAMETER_VALUE
import org.jetbrains.kotlin.analysis.decompiler.stub.computeParameterName
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.contracts.description.ContractProviderKey
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.psi.stubs.StubUtils
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.DescriptorRendererOptions
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumEntry
import org.jetbrains.kotlin.resolve.descriptorUtil.secondaryConstructors
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

private const val DECOMPILED_CODE_COMMENT = "/* compiled code */"
private const val DECOMPILED_COMMENT_FOR_PARAMETER = "/* = compiled code */"
private const val FLEXIBLE_TYPE_COMMENT = "/* platform type */"
private const val DECOMPILED_CONTRACT_STUB = "contract { /* compiled contract */ }"

fun DescriptorRendererOptions.defaultDecompilerRendererOptions() {
    withDefinedIn = false
    classWithPrimaryConstructor = true
    secondaryConstructorsAsPrimary = false
    modifiers = DescriptorRendererModifier.ALL
    excludedTypeAnnotationClasses = emptySet()
    alwaysRenderModifiers = true
    parameterNamesInFunctionalTypes = false // to support parameters names in decompiled text we need to load annotation arguments
    defaultParameterValueRenderer = { _ -> COMPILED_DEFAULT_PARAMETER_VALUE }
    includePropertyConstant = true
    propertyConstantRenderer = { _ -> COMPILED_DEFAULT_INITIALIZER }
    presentableUnresolvedTypes = true
}

/**
 * @see org.jetbrains.kotlin.analysis.decompiler.stub.mustNotBeWrittenToStubs
 */
internal fun CallableMemberDescriptor.mustNotBeWrittenToDecompiledText(): Boolean {
    return when (kind) {
        CallableMemberDescriptor.Kind.DECLARATION, CallableMemberDescriptor.Kind.DELEGATION -> false
        CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> true
        CallableMemberDescriptor.Kind.SYNTHESIZED -> syntheticMemberMustNotBeWrittenToDecompiledText()
    }
}

private fun CallableMemberDescriptor.syntheticMemberMustNotBeWrittenToDecompiledText(): Boolean {
    val containingClass = containingDeclaration as? ClassDescriptor ?: return false

    return when {
        containingClass.kind == ClassKind.ENUM_CLASS -> {
            name in arrayOf(
                StandardNames.ENUM_VALUES,
                StandardNames.ENUM_ENTRIES,
                StandardNames.ENUM_VALUE_OF,
            )
        }

        else -> false
    }
}

fun buildDecompiledText(
    packageFqName: FqName,
    descriptors: List<DeclarationDescriptor>,
    descriptorRenderer: DescriptorRenderer,
): DecompiledText = buildDecompiledTextImpl(
    packageFqName,
    descriptors,
    descriptorRenderer.withOptions {
        // Stub decompilation builds type stubs for expanded types instead of abbreviated types, as type aliases are transparent and
        // need to be treated as their expanded type at use sites. If we instead decompiled to the type alias, we run the risk of an
        // unresolved symbol, as the type alias doesn't need to be present in the dependencies of a use-site module (see KT-62889).
        //
        // To be consistent with stub decompilation, we need to render abbreviated types as their type expansions in decompiled text as
        // well. We also render the abbreviated type in a comment for clarity.
        renderTypeExpansions = true
        renderAbbreviatedTypeComments = true
    },
)

private fun buildDecompiledTextImpl(
    packageFqName: FqName,
    descriptors: List<DeclarationDescriptor>,
    descriptorRenderer: DescriptorRenderer,
): DecompiledText {
    val builder = StringBuilder()

    fun appendDecompiledTextAndPackageName() {
        builder.append("// IntelliJ API Decompiler stub source generated from a class file\n" + "// Implementation of methods is not available")
        builder.append("\n\n")
        if (!packageFqName.isRoot) {
            builder.append("package ").append(packageFqName.render()).append("\n\n")
        }
    }

    fun appendDescriptor(descriptor: DeclarationDescriptor, indent: String, lastEnumEntry: Boolean? = null) {
        if (isEnumEntry(descriptor)) {
            for (annotation in descriptor.annotations) {
                builder.append(descriptorRenderer.renderAnnotation(annotation))
                builder.append(" ")
            }
            builder.append(descriptor.name.asString().quoteIfNeeded())
            builder.append(if (lastEnumEntry!!) ";" else ",")
        } else {
            builder.append(descriptorRenderer.render(descriptor).replace("= ...", DECOMPILED_COMMENT_FOR_PARAMETER))
        }

        if (descriptor is CallableDescriptor) {
            //NOTE: assuming that only return types can be flexible
            if (descriptor.returnType!!.isFlexible()) {
                builder.append(" ").append(FLEXIBLE_TYPE_COMMENT)
            }
        }

        if (descriptor is FunctionDescriptor || descriptor is PropertyDescriptor) {
            if ((descriptor as MemberDescriptor).modality != Modality.ABSTRACT) {
                if (descriptor is FunctionDescriptor) {
                    with(builder) {
                        append(" { ")
                        if (descriptor.getUserData(ContractProviderKey)?.getContractDescription() != null) {
                            append(DECOMPILED_CONTRACT_STUB).append("; ")
                        }
                        append(DECOMPILED_CODE_COMMENT).append(" }")
                    }
                } else {
                    if (descriptor is PropertyDescriptor && descriptor.isDelegated) {
                        builder.append(" by ").append(COMPILED_DEFAULT_INITIALIZER)
                    }

                    // descriptor instanceof PropertyDescriptor
                    builder.append(" ").append(DECOMPILED_CODE_COMMENT)
                }
            }

            (descriptor as? DeserializedPropertyDescriptor)?.proto
                ?.getExtensionOrNull(JvmProtoBuf.propertySignature)
                ?.hasField()
                ?.let { hasBackingField ->
                    @OptIn(IntellijInternalApi::class)
                    builder.append(' ')
                        .append(StubUtils.HAS_BACKING_FIELD_COMMENT_PREFIX)
                        .append(hasBackingField)
                        .append(" */")
                }

            if (descriptor is PropertyDescriptor) {
                for (accessor in descriptor.accessors) {
                    val isNonDefault = !accessor.isDefault
                    val accessorVisibility = accessor.visibility
                    val accessorModality = accessor.modality
                    val isExternalAccessor = accessor.isExternal
                    val isInlineAccessor = accessor.isInline
                    val accessorAnnotations = accessor.annotations
                    if (!isNonDefault &&
                        accessorVisibility == descriptor.visibility &&
                        accessorModality == descriptor.modality &&
                        !isExternalAccessor &&
                        !isInlineAccessor &&
                        accessorAnnotations.isEmpty()
                    ) continue

                    builder.append("\n$indent    ")
                    for (annotation in accessorAnnotations) {
                        builder.append(descriptorRenderer.renderAnnotation(annotation))
                        builder.append(" ")
                    }

                    builder.append(accessorVisibility.internalDisplayName).append(" ")
                    builder.append(accessorModality.name.toLowerCaseAsciiOnly()).append(" ")
                    if (isExternalAccessor) {
                        builder.append("external ")
                    }

                    if (isInlineAccessor) {
                        builder.append("inline ")
                    }

                    if (accessor is PropertyGetterDescriptor) {
                        builder.append("get")
                        if (isNonDefault) {
                            builder.append("()")
                        }
                    } else if (accessor is PropertySetterDescriptor) {
                        builder.append("set")
                        if (isNonDefault) {
                            builder.append("(")
                            val parameterDescriptor = accessor.valueParameters[0]
                            for (annotation in parameterDescriptor.annotations) {
                                builder.append(descriptorRenderer.renderAnnotation(annotation))
                                builder.append(" ")
                            }
                            val parameterName = computeParameterName(parameterDescriptor.name)
                            builder.append(parameterName.asString()).append(": ")
                                .append(descriptorRenderer.renderType(parameterDescriptor.type))
                            builder.append(")")
                        }
                    }

                    if (isNonDefault) {
                        builder.append(" {").append(DECOMPILED_CODE_COMMENT).append(" }")
                    }
                }
            }
        } else if (descriptor is ClassDescriptor && !isEnumEntry(descriptor)) {
            builder.append(" {\n")

            val subindent = "$indent    "

            var firstPassed = false
            fun newlineExceptFirst() {
                if (firstPassed) {
                    builder.append("\n")
                } else {
                    firstPassed = true
                }
            }

            val allDescriptors = descriptor.secondaryConstructors + descriptor.defaultType.memberScope.getContributedDescriptors()
            val (enumEntries, members) = allDescriptors.partition(::isEnumEntry)

            for ((index, enumEntry) in enumEntries.withIndex()) {
                newlineExceptFirst()
                builder.append(subindent)
                appendDescriptor(enumEntry, subindent, index == enumEntries.lastIndex)
            }

            val companionObject = descriptor.companionObjectDescriptor
            if (companionObject != null) {
                newlineExceptFirst()
                builder.append(subindent)
                appendDescriptor(companionObject, subindent)
            }

            for (member in members) {
                if (member.containingDeclaration != descriptor) {
                    continue
                }
                if (member == companionObject) {
                    continue
                }
                if (member is CallableMemberDescriptor && member.mustNotBeWrittenToDecompiledText()) {
                    continue
                }
                newlineExceptFirst()
                builder.append(subindent)
                appendDescriptor(member, subindent)
            }

            builder.append(indent).append("}")
        }

        builder.append("\n")
    }

    appendDecompiledTextAndPackageName()
    for (member in descriptors) {
        appendDescriptor(member, "")
        builder.append("\n")
    }

    return DecompiledText(builder.toString())
}
