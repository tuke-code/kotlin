/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallableReferenceAccessChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.REFERENCE_TO_PACKAGE_PRIVATE_CLASS_FIELD
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.isJavaOrEnhancement
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.resolvedType
import kotlin.math.exp

object FirPackagePrivateClassFieldReferenceChecker : FirCallableReferenceAccessChecker(MppCheckerKind.Common) {
    override fun check(
        expression: FirCallableReferenceAccess,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val fieldSymbol = (expression.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirFieldSymbol ?: return
        if (fieldSymbol.isStatic) return
        val ownerSymbol = fieldSymbol.containingClassLookupTag()?.toSymbol(context.session) ?: return
        if (ownerSymbol.visibility != JavaVisibilities.PackageVisibility && ownerSymbol != JavaVisibilities.ProtectedAndPackage) return
        val receiver = expression.dispatchReceiver ?: expression.explicitReceiver ?: return
        val receiverClassSymbol = if (receiver is FirResolvedQualifier) {
            receiver.symbol
        } else {
            (receiver.resolvedType as? ConeClassLikeType)?.lookupTag?.toSymbol(context.session)
        }
        if (receiverClassSymbol?.isJavaOrEnhancement != true) return

        reporter.reportOn(expression.source, REFERENCE_TO_PACKAGE_PRIVATE_CLASS_FIELD, context)
    }
}
