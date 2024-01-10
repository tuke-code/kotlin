/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
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

/**
 * This checker is needed because of the nature of @NoInfer.
 * [org.jetbrains.kotlin.resolve.calls.inference.components.TypeCheckerStateForConstraintSystem.addSubtypeConstraint] returns `true`
 * if `subType` or `superType` contains `@NoInfer` annotation.
 * It implies that `addSubtypeConstraintIfCompatible` in [org.jetbrains.kotlin.fir.resolve.calls.checkApplicabilityForArgumentType]
 * also returns `true` and `ArgumentTypeMismatch` diagnostics is not reported at the arguments resolving phase.
 * At the checkers phase all types are resolved, and it's possible to find missing mismatches using `isSubtypeOf` function and calculated substitution
 *
 * Given the following example:
 *
 * ```kt
 * fun <T> test1(t1: T, t2: @kotlin.internal.NoInfer T): T = t1
 * ...
 * test1(1, <!ARGUMENT_TYPE_MISMATCH("Int; String")!>"312"<!>)
 * ```
 *
 * The second argument is not participated in the inference process, and `T` is captured by the type of the first argument.
 * It causes type mismatch error on the second argument.
 */
object FirFunctionNoInferArgumentsChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val mapping = (expression.argumentList as? FirResolvedArgumentList)?.mapping ?: return

        fun ConeKotlinType.containsNoInferAttribute(): Boolean = contains { it.attributes.contains(CompilerConeAttributes.NoInfer) }

        if (mapping.none { (argument, valueParameter) ->
                argument.resolvedType.containsNoInferAttribute() || valueParameter.returnTypeRef.coneType.containsNoInferAttribute()
            }
        ) {
            // Optimization: don't allocate anything if there is no `@NoInfer` type parameter (most common case)
            return
        }

        val substitution = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
        val substitutor = ConeSubstitutorByMap(substitution, context.session)

        fun matchTypeArgumentAndParameter(argumentType: ConeKotlinType, parameterType: ConeKotlinType) {
            if (parameterType is ConeTypeParameterType) {
                val parameterSymbol = parameterType.toSymbol(context.session) as FirTypeParameterSymbol
                if (substitution[parameterSymbol] == null) {
                    // If type parameter is encountered, then it's type is captured by type argument
                    // It could cause type mismatch error on other type parameters marked with `@NoInfer` annotation
                    substitution[parameterSymbol] = argumentType
                }
            } else {
                val argumentTypeArguments = argumentType.typeArguments
                val parameterTypeArguments = parameterType.typeArguments
                val count = minOf(argumentTypeArguments.size, parameterTypeArguments.size)
                for (index in 0 until count) {
                    argumentTypeArguments[index].type?.let { argType ->
                        parameterTypeArguments[index].type?.let { paramType ->
                            matchTypeArgumentAndParameter(argType, paramType)
                        }
                    }
                }
            }
        }

        fun checkNoInferMismatch(argumentExpression: FirExpression, parameterType: ConeKotlinType) {
            if (argumentExpression.resolvedType.containsNoInferAttribute() || parameterType.containsNoInferAttribute()) {
                val argumentType = argumentExpression.resolvedType
                val substitutedType = substitutor.substituteOrSelf(parameterType)
                if (!AbstractTypeChecker.isSubtypeOf(context.session.typeContext, argumentType, substitutedType)) {
                    reporter.reportOn(
                        argumentExpression.source,
                        FirErrors.ARGUMENT_TYPE_MISMATCH,
                        substitutedType,
                        argumentType,
                        false,
                        context
                    )
                }
            }
        }

        expression.extensionReceiver?.let { receiverArgument ->
            expression.toResolvedCallableSymbol()?.resolvedReceiverTypeRef?.coneType?.let { receiverParameterType ->
                matchTypeArgumentAndParameter(receiverArgument.resolvedType, receiverParameterType)
                checkNoInferMismatch(receiverArgument, receiverParameterType)
            }
        }

        for ((argument, valueParameter) in mapping) {
            matchTypeArgumentAndParameter(argument.resolvedType, valueParameter.returnTypeRef.coneType)
            checkNoInferMismatch(argument, valueParameter.returnTypeRef.coneType)
        }
    }
}