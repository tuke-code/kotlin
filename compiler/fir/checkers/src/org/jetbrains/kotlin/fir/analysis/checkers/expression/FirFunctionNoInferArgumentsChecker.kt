/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker

object FirFunctionNoInferArgumentsChecker : FirFunctionCallChecker() {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val mapping = (expression.argumentList as? FirResolvedArgumentList)?.mapping ?: return
        // Don't allocate map for functions without type parameters
        val parametersMapping by lazy(LazyThreadSafetyMode.NONE) { mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>() }

        fun checkArgumentAndParameter(
            functionArgument: FirExpression,
            functionParameterType: ConeKotlinType,
            argumentType: ConeKotlinType = functionArgument.resolvedType,
            parameterType: ConeKotlinType = functionParameterType
        ) {
            if (parameterType is ConeTypeParameterType) {
                val parameterSymbol = parameterType.toSymbol(context.session) as FirTypeParameterSymbol
                if (parameterType.attributes.contains(CompilerConeAttributes.NoInfer)) {
                    val inferredType = parametersMapping[parameterSymbol]
                    if (inferredType != null && !AbstractTypeChecker.equalTypes(context.session.typeContext, argumentType, inferredType)) {
                        // Substitution is required for case with nested type parameters because expected type doesn't exist:
                        //
                        // ```
                        // fun <T> test4(t1: T, t2: List<@kotlin.internal.NoInfer T>): T = t1`
                        // test4(1, <!TYPE_MISMATCH("List<Int>; List<String>")!>listOf("a")<!>)
                        // ```
                        //
                        // In simple case it's possible to use just `functionParameterType`, but I simplified it since
                        // performance in code with errors is not important
                        reporter.reportOn(
                            functionArgument.source,
                            FirErrors.ARGUMENT_TYPE_MISMATCH,
                            ConeSubstitutorByMap(parametersMapping, context.session).substituteOrSelf(functionParameterType),
                            functionArgument.resolvedType,
                            false,
                            context
                        )
                    }
                } else {
                    // If type parameter is encountered, then it's type is captured by type argument
                    // It could cause type mismatch error on other type parameters marked with `@NoInfer` annotation
                    parametersMapping[parameterSymbol] = argumentType
                }
            } else {
                val argumentTypeArguments = argumentType.typeArguments
                val parameterTypeArguments = parameterType.typeArguments
                val count = minOf(argumentTypeArguments.size, parameterTypeArguments.size)
                for (index in 0 until count) {
                    argumentTypeArguments[index].type?.let { argType ->
                        parameterTypeArguments[index].type?.let { paramType ->
                            checkArgumentAndParameter(functionArgument, functionParameterType, argType, paramType)
                        }
                    }
                }
            }
        }

        expression.extensionReceiver?.let { receiverArg ->
            expression.toResolvedCallableSymbol()?.resolvedReceiverTypeRef?.coneType?.let { receiverParam ->
                checkArgumentAndParameter(receiverArg, receiverParam)
            }
        }

        for ((argument, parameter) in mapping) {
            checkArgumentAndParameter(argument, parameter.returnTypeRef.coneType)
        }
    }
}