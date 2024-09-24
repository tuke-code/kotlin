/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import com.intellij.psi.PsiElement
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
        if (expression.isPropagating) return

        // 1. Check NOT only resolvable references
        val calleeReference = expression.toReference(context.session)
        val resolvedReference = calleeReference?.resolved

//        if (resolvedReference == null && expression !is FirEqualityOperatorCall && expression !is FirComparisonExpression) return

        // Exclusions
        if (resolvedReference?.toResolvedCallableSymbol()?.isExcluded() == true) return

        // Ignore Unit or Nothing
        // TODO: FirWhenExpression has Unit type if it is not assigned anywhere, even if branches are non-Unit
        if (expression.resolvedType.run { isNothingOrNullableNothing || isUnitOrNullableUnit }) return

        // If not the outermost call, then it is used as an argument
        if (context.callsOrAssignments.lastOrNull { it != expression } != null) return

        val (outerExpression, given) = context.propagate(expression)
//        val outerExpression = context.firstNonPropagatingOuterElementOf(expression)
//        val (a, b) = context.propagate(expression)

        // Used directly in return expression, property or parameter declaration
        if (outerExpression.isVarInitializationOrReturn) return

//        if (outerExpression.uses(given ?: expression)) return
        if (outerExpression.uses(given ?: expression)) return

        reporter.reportOn(expression.source, RETURN_VALUE_NOT_USED, "Unused expression: " + (resolvedReference?.toResolvedCallableSymbol()?.callableId?.toString() ?: "<${expression.render()}>"), context)
    }

    private fun FirElement?.uses(given: FirExpression): Boolean = when (this) {
        // Safe calls for some reason are not part of context.callsOrAssignments, so have to be checked separately
        is FirSafeCallExpression -> true // receiver == given

        is FirWhenExpression -> subject == given

        // Includes FirWhileLoop, FirDoWhileLoop, and FirErrorLoop. I have no idea what FirErrorLoop is.
        is FirLoop -> condition == given // TODO: `given` should be not original, but last propagated (i.e. unwrap FirTypeOperatorCall/SmartCast)

        is FirThrowExpression -> true // exception == given
        is FirElvisExpression -> true // lhs == given || rhs == given
        is FirComparisonExpression -> true // compareToCall == given
        is FirBooleanOperatorExpression -> true // leftOperand == given || rightOperand == given

        // I think MAYBE we can just include FirCall in general here, although FirTypeOperator is propagating.
        is FirEqualityOperatorCall -> true // given in argumentList.arguments
        is FirStringConcatenationCall -> true // given in argumentList.arguments
        is FirGetClassCall -> true // given in argumentList.arguments

        // in if(x) and when(x), x is always used
        // Most complex case, as we should check whether given is the last expression in a block.
        // TODO: FirWhenExpression has Unit type if it is not assigned anywhere, even if branches are non-Unit. requires separate handling.
        is FirWhenBranch -> condition == given || result == given // TODO: `given` should be not original, but last propagated (i.e. unwrap FirTypeOperatorCall/SmartCast)

        is FirTryExpression -> tryBlock == given || finallyBlock == given
        is FirCatch -> block == given

        else -> false
    }

    private fun CheckerContext.firstNonPropagatingOuterElementOf(thisExpression: FirExpression): FirElement? =
        containingElements.lastOrNull { it != thisExpression && !it.isPropagating }

    private fun CheckerContext.propagate(thisExpression: FirExpression): Pair<FirElement?, FirExpression?> {
        var lastPropagating: FirExpression? = null
        for (e in containingElements.asReversed()) {
            if (e == thisExpression) continue
            if (e.isPropagating) {
                lastPropagating = e as? FirExpression
                continue
            }
            // FirBlock is propagating only if this is the last statement
            if (e is FirBlock && e.statements.lastOrNull() == (lastPropagating ?: thisExpression)) {
                lastPropagating = e
                continue
            }
            return e to lastPropagating
        }
        return null to null
    }

    private val FirElement.isPropagating: Boolean
        get() = when (this) {
            is FirSmartCastExpression,
            is FirArgumentList,
            is FirTypeOperatorCall,
            is FirCheckNotNullCall,
                -> true
            else -> false
        }

    private val FirElement?.isVarInitializationOrReturn: Boolean get() = this is FirReturnExpression || this is FirProperty || this is FirValueParameter

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
        // TODO: write normal checker
        val s = id.toString()
        return s in exclusionList
    }

    private val exclusionList = listOf(
        "kotlin/collections/MutableCollection.add",
        "kotlin/collections/MutableList.add",
        "kotlin/collections/MutableSet.add",
        "kotlin/collections/MutableList.set",
        "kotlin/collections/MutableMap.put",
        "kotlin/text/StringBuilder.append",
        "java/lang/StringBuilder.append",
        "java/lang/StringBuilder.appendLine",
        "kotlin/text/StringBuilder.appendLine",
        "kotlin/text/Appendable.append",
        "kotlin/text/Appendable.appendLine",
        "kotlin/test/assertFailsWith",
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
//        "kotlin/Throwable.printStackTrace",
//        "kotlin/Throwable.addSuppressed",
//        "kotlin/Throwable.getSuppressed",
    )
}

