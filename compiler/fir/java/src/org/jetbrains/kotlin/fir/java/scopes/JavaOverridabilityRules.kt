/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.scopes.PlatformSpecificOverridabilityRules
import org.jetbrains.kotlin.fir.unwrapFakeOverrides

class JavaOverridabilityRules(session: FirSession) : PlatformSpecificOverridabilityRules {
    private val javaOverrideChecker =
        JavaOverrideChecker(session, JavaTypeParameterStack.EMPTY, baseScopes = null, considerReturnTypeKinds = true)

    override fun isOverriddenFunction(overrideCandidate: FirSimpleFunction, baseDeclaration: FirSimpleFunction): Boolean? {
        return if (shouldApplyJavaChecker(overrideCandidate, baseDeclaration)) {
            javaOverrideChecker.isOverriddenFunction(overrideCandidate, baseDeclaration).takeIf { !it }
        } else {
            null
        }
    }

    override fun isOverriddenProperty(overrideCandidate: FirCallableDeclaration, baseDeclaration: FirProperty): Boolean? {
        return if (shouldApplyJavaChecker(overrideCandidate, baseDeclaration)) {
            javaOverrideChecker.isOverriddenProperty(overrideCandidate, baseDeclaration).takeIf { !it }
        } else {
            null
        }
    }

    private fun shouldApplyJavaChecker(overrideCandidate: FirCallableDeclaration, baseDeclaration: FirCallableDeclaration): Boolean {
        return when {
            overrideCandidate.isOriginallyFromJava() && baseDeclaration.isOriginallyFromJava() -> true
            overrideCandidate.isFromJava() || baseDeclaration.isFromJava() -> true
            else -> false
        }
    }

    private fun FirCallableDeclaration.isFromJava(): Boolean = origin == FirDeclarationOrigin.Enhancement

    private fun FirCallableDeclaration.isOriginallyFromJava(): Boolean = unwrapFakeOverrides().origin == FirDeclarationOrigin.Enhancement
}
