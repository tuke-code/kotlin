/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.LambdaArgument
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.StubTypeForBuilderInference
import org.jetbrains.kotlin.types.typeUtil.contains

object StubForBuilderInferenceLambdaParameterTypeChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.NoBuilderInferenceWithoutAnnotationRestriction)) return
        for ((_, resolvedValueArgument) in resolvedCall.valueArguments) {
            for (valueArgument in resolvedValueArgument.arguments) {
                if (valueArgument is LambdaArgument) {
                    val functionLiteral = valueArgument.getLambdaExpression()?.functionLiteral ?: continue
                    checkFunctionLiteral(functionLiteral, context)
                }
            }
        }
        val resultingDescriptor = resolvedCall.resultingDescriptor as? SimpleFunctionDescriptor ?: return
        if (resultingDescriptor.dispatchReceiverParameter?.type?.contains { it is StubTypeForBuilderInference } == true) {
            context.trace.report(Errors.BUILDER_INFERENCE_STUB_PARAMETER_TYPE.on(reportOn, resultingDescriptor.name))
        }
        if (resultingDescriptor.returnType?.contains { it is StubTypeForBuilderInference } == true) {
            for (valueParameterDescriptor in resolvedCall.resultingDescriptor.valueParameters) {
                if (valueParameterDescriptor.type.contains { it is StubTypeForBuilderInference } == true) {
                    //context.trace.report(Errors.BUILDER_INFERENCE_STUB_PARAMETER_TYPE.on(reportOn, valueParameterDescriptor.name))
                }
            }
        }
    }

    internal fun checkFunctionLiteral(functionLiteral: KtFunctionLiteral, context: CallCheckerContext) {
        val functionDescriptor = context.trace.get(BindingContext.FUNCTION, functionLiteral) ?: return
        for ((index, valueParameterDescriptor) in functionDescriptor.valueParameters.withIndex()) {
            if (!valueParameterDescriptor.type.contains { it is StubTypeForBuilderInference }) continue
            val target = functionLiteral.valueParameters.getOrNull(index) ?: functionLiteral
            context.trace.report(Errors.BUILDER_INFERENCE_STUB_PARAMETER_TYPE.on(target, valueParameterDescriptor.name))
        }
        for (parameter in functionLiteral.valueParameters) {
            val destructuringDeclaration = parameter.destructuringDeclaration ?: continue
            for (entry in destructuringDeclaration.entries) {
                val componentVariable = context.trace.get(BindingContext.VARIABLE, entry) ?: continue
                if (!componentVariable.type.contains { it is StubTypeForBuilderInference }) continue
                context.trace.report(Errors.BUILDER_INFERENCE_STUB_PARAMETER_TYPE.on(entry, componentVariable.name))
            }
        }
    }
}


// ISSUE: KT-66272

data class DataClass(val data: String)

fun test() {
    A.create {
        it.group().apply(it) { DataClass(it) }
    }
}
open class A<O, F> {
    open fun group(): A<F, String> {
        return null!!
    }
    fun <R> apply(instance: A<O, *>, function: (F) -> R): A<O, R> {
        return null!!
    }
    companion object {
        fun <T> create(a: (A<T, T>) -> A<T, T>) {}
    }
}