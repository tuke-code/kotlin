/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.analysis.checkers.checkRepeatedAnnotation
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.analysis.checkers.getDefaultUseSiteTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType

object FirExpressionAnnotationChecker : FirBasicExpressionChecker() {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        // Declarations are checked separately
        // See KT-33658 about annotations on non-expression statements
        if (expression is FirDeclaration) return

        val annotations = expression.annotations
        if (annotations.isEmpty()) return

        val annotationsMap = hashMapOf<ConeKotlinType, MutableList<AnnotationUseSiteTarget?>>()

        // It's possible to annotate the expression with annotation without EXPRESSION target or even with any target
        // It includes cases when the expression is erroneous or doesn't return anything (FirWhenExpression)
        // Even if target is always correct, repeated annotations should be checked anyway, that why there is no return here
        val alwaysCorrectTarget = expression.isTargetAlwaysCorrect()

        for (annotation in annotations) {
            val useSiteTarget = annotation.useSiteTarget ?: expression.getDefaultUseSiteTarget(annotation, context)
            val existingTargetsForAnnotation = annotationsMap.getOrPut(annotation.annotationTypeRef.coneType) { arrayListOf() }

            if (!alwaysCorrectTarget && KotlinTarget.EXPRESSION !in annotation.getAllowedAnnotationTargets(context.session)) {
                reporter.reportOn(annotation.source, FirErrors.WRONG_ANNOTATION_TARGET, "expression", context)
            }

            checkRepeatedAnnotation(useSiteTarget, existingTargetsForAnnotation, annotation, context, reporter)

            existingTargetsForAnnotation.add(useSiteTarget)
        }
    }

    private fun FirStatement.isTargetAlwaysCorrect(): Boolean {
        if (this !is FirExpression || this is FirErrorExpression) return true

        if (this is FirBlock) {
            if (source?.kind == KtRealSourceElementKind) {
                return true // Any annotation can be placed to a real block enclosed by curly braces
            }

            // If block expression is fake (desugared), its applicability is detected by the last statement
            return statements.last().isTargetAlwaysCorrect()
        }

        return this is FirWhenExpression && !usedAsExpression // That's the single case when FirExpression is actually not a real expression
    }
}
