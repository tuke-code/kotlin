/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Severity.ERROR
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isLhsOfAssignment
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*

// TODO: rewrite generator, since FirErrors.kt is auto generated.
val RETURN_VALUE_NOT_USED: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("RETURN_VALUE_NOT_USED", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)

object CheckReturnValue : FirBasicExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression !is FirExpression) return // TODO: are we sure that these are all cases?
        if (expression is FirBlock) return

        if (expression.isLhsOfAssignment(context)) return
        if (expression is FirAnnotation) return

        if (expression.isLocalPropertyOrParameterOrThis()) return

        // Do not check everything marked as 'propagating' in walkUp and still `is FirExpression`:
        when (expression) {
            is FirSmartCastExpression,
            is FirTypeOperatorCall,
            is FirCheckNotNullCall,
            is FirTryExpression,
            is FirWhenExpression
                -> return
        }

        // Try resolve reference to see if it is excluded
        val calleeReference = expression.toReference(context.session)
        val resolvedReference = calleeReference?.resolved

        // Exclusions
        if (resolvedReference?.toResolvedCallableSymbol()?.isExcluded() == true) return

        // Ignore Unit or Nothing
        if (expression.resolvedType.run { isNothingOrNullableNothing || isUnitOrNullableUnit }) return

        // If not the outermost call, then it is used as an argument
        if (context.callsOrAssignments.lastOrNull { it != expression } != null) return

        if (hasUsages(context, expression)) return

        reporter.reportOn(
            expression.source,
            RETURN_VALUE_NOT_USED,
            "Unused value: " + (resolvedReference?.toResolvedCallableSymbol()?.callableId?.toString() ?: "<${expression.render()}>"),
            context
        )
    }

    private fun hasUsages(context: CheckerContext, thisExpression: FirExpression): Boolean {
        val stack = context.containingElements.asReversed()
        var lastPropagating: FirElement = thisExpression

        for (e in stack) {
            if (e == thisExpression) continue
            when (e) {
                // Propagate further:
                is FirSmartCastExpression,
                is FirArgumentList,
                is FirTypeOperatorCall,
                is FirCheckNotNullCall,
                    -> {
                    lastPropagating = e
                    continue
                }

                // Conditional (?) propagation:

                is FirTryExpression,
                is FirCatch,
                    -> {
                    lastPropagating = e
                    continue
                }

                is FirWhenBranch -> {
                    // If it is condition, it is used, otherwise it is result and we propagate up:
                    if (e.condition == lastPropagating) return true
                    lastPropagating = e
                    continue
                }

                is FirWhenExpression -> {
                    // If it is subject, it is used, otherwise it is branch and we propagate up:
                    if (e.subject == lastPropagating) return true
                    lastPropagating = e
                    continue
                }

                // Expressions that always use what's down the stack:

                is FirSafeCallExpression -> return true // receiver == given
                is FirReturnExpression -> return true
                is FirThrowExpression -> return true // exception == given
                is FirElvisExpression -> return true // lhs == given || rhs == given
                is FirComparisonExpression -> return true // compareToCall == given
                is FirBooleanOperatorExpression -> return true // leftOperand == given || rightOperand == given

                is FirEqualityOperatorCall -> return true // given in argumentList.arguments
                is FirStringConcatenationCall -> return true // given in argumentList.arguments
                is FirGetClassCall -> return true // given in argumentList.arguments

                // Initializers
                // FirField can occur in `by` interface delegation
                is FirProperty, is FirValueParameter, is FirField -> return true

                // Conditional usage:

                is FirBlock -> {
                    // Special case: ++x is desugared to FirBlock, we consider result of pre/post increment as discardable.
                    if (e.source?.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement) return true

                    // FirBlock result is the last statement, other statements are not used
                    if (e.statements.lastOrNull() == lastPropagating) {
                        lastPropagating = e
                        continue
                    }
                    return false
                }

                is FirLoop -> return e.condition == lastPropagating

                else -> return false
            }
        }
        return false
    }


    private fun FirExpression.isLocalPropertyOrParameterOrThis(): Boolean {
        if (this is FirThisReceiverExpression) return true
        if (this !is FirPropertyAccessExpression) return false
        return when (calleeReference.symbol) {
            is FirValueParameterSymbol -> true
            is FirPropertySymbol -> calleeReference.toResolvedPropertySymbol()?.isLocal == true
            else -> false
        }
    }

    private fun FirCallableSymbol<*>.isExcluded(): Boolean {
        val id = callableId
        // TODO: write normal checker w.r.t overloads
        val s = id.toString()
        return s in exclusionList
    }

    private val exclusionList = listOf(
        // Collection operations:
        "kotlin/collections/MutableCollection.add",
        "kotlin/collections/MutableList.add",
        "kotlin/collections/MutableSet.add",
        "kotlin/collections/MutableList.set",
        "kotlin/collections/MutableMap.put",
        "kotlin/collections/MutableMap.remove",

        "kotlin/collections/removeLast",

        "java/util/LinkedHashSet.add",
        "java/util/HashSet.add",
        "java/util/TreeSet.add",
        "java/util/SortedSet.add",
        "java/util/ArrayList.add",
        "java/util/ArrayList.addAll",
        "java/util/HashMap.put",
        "java/util/TreeMap.put",
        "java/util/LinkedHashMap.put",
        "java/util/HashSet.remove",
        "java/util/TreeSet.remove",

        // StringBuilder operations that return `this`:
        "kotlin/text/StringBuilder.append",
        "java/lang/StringBuilder.append",
        "java/lang/StringBuilder.appendLine",
        "kotlin/text/StringBuilder.appendLine",
        "kotlin/text/Appendable.append",
        "kotlin/text/Appendable.appendLine",
        "kotlin/text/appendRange",

        // Array operations that return `destination`:
        "kotlin/collections/copyInto",
        "kotlin/text/toCharArray", // TODO: only one overload of toCharArray is discardable

        // Buffer operations that return `this`:
        "java/nio/Buffer.flip",
        "java/nio/charset/CharsetDecoder.reset",
        "java/nio/ByteBuffer.compact",
        "java/nio/Buffer.position",
        "java/nio/Buffer.flip",

        // General utilities

        "kotlin/requireNotNull",

        // Test utilities

        "kotlin/test/assertFailsWith",
        "kotlin/test/assertFails",
        "kotlin/test/assertNotNull",
        "kotlin/test/assertIs",
    )
}

