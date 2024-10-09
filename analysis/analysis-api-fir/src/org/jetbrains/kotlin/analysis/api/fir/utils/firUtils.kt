/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.analysis.api.fir.utils

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirAnnotationValueConverter
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirCompileTimeConstantEvaluator
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.PsiBasedContainingClassCalculator
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.util.OperatorNameConventions

internal fun PsiElement.unwrap(): PsiElement {
    return when (this) {
        is KtExpression -> this.unwrap()
        else -> this
    }
}

internal fun KtExpression.unwrap(): KtExpression {
    return when (this) {
        is KtLabeledExpression -> baseExpression?.unwrap()
        is KtAnnotatedExpression -> baseExpression?.unwrap()
        is KtFunctionLiteral -> (parent as? KtLambdaExpression)?.unwrap()
        else -> this
    } ?: this
}

/**
 * Returns a containing class symbol for the given symbol, or `null` if the symbol is not a class member.
 *
 * The function behaves like [getContainingClassSymbol], but tries to find the parent using the PSI source first.
 * Not only the PSI-based search is faster (as it does not use indices), but it also supports cases when there are several classes with
 * the same [ClassId].
 */
@OptIn(LLFirInternals::class)
internal fun FirBasedSymbol<*>.getContainingClassSymbolPsiAware(): FirClassLikeSymbol<*>? {
    return PsiBasedContainingClassCalculator.getContainingClassSymbol(this)
        ?: getContainingClassSymbol()
}

/**
 * @receiver A symbol that needs to be imported
 * @return An [FqName] by which this symbol can be imported (if it is possible)
 */
internal fun FirCallableSymbol<*>.computeImportableName(): FqName? {
    if (callableId.isLocal) return null

    // SAM constructors are synthetic, but can be imported
    if (origin is FirDeclarationOrigin.SamConstructor) return callableId.asSingleFqName()

    // if classId == null, callable is topLevel
    val containingClassId = callableId.classId
        ?: return callableId.asSingleFqName()

    val containingClass = getContainingClassSymbolPsiAware() ?: return null

    if (this is FirConstructorSymbol) return if (!containingClass.isInner) containingClassId.asSingleFqName() else null

    // Java static members, enums, and object members can be imported
    val canBeImported = containingClass.origin is FirDeclarationOrigin.Java && isStatic ||
            containingClass.classKind == ClassKind.ENUM_CLASS && isStatic ||
            containingClass.classKind == ClassKind.OBJECT

    return if (canBeImported) callableId.asSingleFqName() else null
}

internal fun FirExpression.asKaInitializerValue(builder: KaSymbolByFirBuilder, forAnnotationDefaultValue: Boolean): KaInitializerValue {
    val ktExpression = psi as? KtExpression
    val evaluated = FirCompileTimeConstantEvaluator.evaluateAsKtConstantValue(this)

    return when (evaluated) {
        null -> if (forAnnotationDefaultValue) {
            val annotationConstantValue = FirAnnotationValueConverter.toConstantValue(this, builder)
            if (annotationConstantValue != null) {
                KaConstantValueForAnnotation(annotationConstantValue, ktExpression)
            } else {
                KaNonConstantInitializerValue(ktExpression)
            }
        } else {
            KaNonConstantInitializerValue(ktExpression)
        }
        else -> KaConstantInitializerValue(evaluated, ktExpression)
    }
}

internal fun FirEqualityOperatorCall.processEqualsFunctions(
    session: FirSession,
    analysisSession: KaFirSession,
    processor: (FirNamedFunctionSymbol) -> Unit,
) {
    val lhs = arguments.firstOrNull() ?: return
    val scope = lhs.resolvedType.scope(
        useSiteSession = session,
        scopeSession = analysisSession.getScopeSessionFor(analysisSession.firSession),
        callableCopyTypeCalculator = CallableCopyTypeCalculator.DoNothing,
        requiredMembersPhase = FirResolvePhase.STATUS,
    ) ?: return

    scope.processFunctionsByName(OperatorNameConventions.EQUALS) { functionSymbol ->
        val parameterSymbol = functionSymbol.valueParameterSymbols.singleOrNull()
        if (parameterSymbol != null && parameterSymbol.resolvedReturnType.isNullableAny) {
            processor(functionSymbol)
        }
    }
}
