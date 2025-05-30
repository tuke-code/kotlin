/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.InlineStatus
import org.jetbrains.kotlin.fir.declarations.getAnnotationRetention
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassLikeSymbol
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.expressions.unwrapArgument
import org.jetbrains.kotlin.fir.references.isError

object FirInlinedLambdaNonSourceAnnotationsChecker : FirAnonymousFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirAnonymousFunction) {
        if (declaration.inlineStatus != InlineStatus.Inline && declaration.inlineStatus != InlineStatus.CrossInline) {
            return
        }

        // If the lambda belongs to a function call which is already an error, do not report non-source annotation.
        for (call in context.callsOrAssignments) {
            if (call is FirFunctionCall && call.calleeReference.isError()) {
                val mapping = call.resolvedArgumentMapping ?: continue
                for ((argument, parameter) in mapping) {
                    if ((argument.unwrapArgument() as? FirAnonymousFunctionExpression)?.anonymousFunction === declaration) {
                        return
                    }
                }
            }
        }

        for (it in declaration.annotations) {
            val annotationSymbol = it.toAnnotationClassLikeSymbol(context.session) ?: continue

            if (annotationSymbol.getAnnotationRetention(context.session) != AnnotationRetention.SOURCE) {
                reporter.reportOn(it.source, FirErrors.NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION)
            }
        }
    }
}
