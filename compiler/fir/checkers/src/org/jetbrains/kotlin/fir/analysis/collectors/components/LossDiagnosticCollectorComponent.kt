/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.impl.PendingDiagnosticsCollectorWithSuppress
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder

class LossDiagnosticCollectorComponent(
    session: FirSession,
    reporter: DiagnosticReporter,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    override fun visitElement(element: FirElement, data: CheckerContext) {
        if (element !is FirDiagnosticHolder || data.suppressedDiagnostics.isNotEmpty()) {
            return
        }

        if (reporter !is PendingDiagnosticsCollectorWithSuppress) {
            return
        }

        val source = element.source
        reporter.reportOn(source, FirErrors.OTHER_ERROR, data)
    }
}
