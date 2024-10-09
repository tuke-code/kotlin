/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElement
import org.jetbrains.kotlin.KtFakeSourceElementKind.*
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfTypeSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.fir.declarations.isLazyResolvable
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

@LLFirInternals
object PsiBasedContainingClassCalculator {
    /**
     * Returns a containing class symbol for the given symbol, computing it solely from the source information.
     */
    fun getContainingClassSymbol(symbol: FirBasedSymbol<*>): FirClassLikeSymbol<*>? {
        if (!symbol.origin.isLazyResolvable) {
            // Handle only source or source-based declarations for now as below we use the PSI tree
            return null
        }

        if (!canHaveContainingClassSymbol(symbol)) {
            return null
        }

        val source = symbol.source ?: return null

        when (source) {
            is KtFakeSourceElement -> {
                val fakeKind = source.kind

                if (symbol is FirConstructorSymbol && fakeKind == ImplicitConstructor) {
                    return computeContainingClass(symbol, source.psi)
                }

                if (symbol is FirPropertyAccessorSymbol) {
                    if (fakeKind == DefaultAccessor) {
                        val containingProperty = source.psi
                        return if (containingProperty is KtProperty || containingProperty is KtParameter) {
                            computeContainingClass(symbol, (containingProperty as KtDeclaration).containingClassOrObject)
                        } else {
                            null
                        }
                    }

                    if (fakeKind == DelegatedPropertyAccessor) {
                        val delegateExpression = source.psi as? KtExpression
                        val containingDelegate = delegateExpression?.parent as? KtPropertyDelegate
                        val containingProperty = containingDelegate?.parent as? KtProperty
                        return computeContainingClass(symbol, containingProperty?.containingClassOrObject)
                    }

                    if (fakeKind == PropertyFromParameter) {
                        val containingParameter = source.psi as? KtParameter
                        return computeContainingClass(symbol, containingParameter?.containingClassOrObject)
                    }
                }

                if (symbol is FirPropertySymbol && fakeKind == PropertyFromParameter) {
                    val containingParameter = source.psi as? KtParameter
                    return computeContainingClass(symbol, containingParameter?.containingClassOrObject)
                }

                if (fakeKind == EnumGeneratedDeclaration) {
                    return computeContainingClass(symbol, source.psi)
                }

                if (fakeKind == DataClassGeneratedMembers) {
                    val containingClass = when (val psi = source.psi) {
                        is KtClassOrObject -> psi
                        is KtParameter -> psi.containingClassOrObject // component() functions point to 'KtParameter's
                        else -> null
                    }
                    return computeContainingClass(symbol, containingClass)
                }
            }
            is KtRealPsiSourceElement -> {
                if (symbol is FirClassLikeSymbol<*>) {
                    val selfClass = source.psi as? KtClassOrObject
                    return computeContainingClass(symbol, selfClass?.containingClassOrObject)
                }

                if (symbol is FirAnonymousInitializerSymbol) {
                    val selfInitializer = source.psi as? KtClassInitializer
                    return computeContainingClass(symbol, selfInitializer?.containingClassOrObject)
                }

                if (symbol is FirCallableSymbol<*>) {
                    val selfCallable = source.psi
                    return when (selfCallable) {
                        is KtCallableDeclaration, is KtEnumEntry -> {
                            computeContainingClass(symbol, selfCallable.containingClassOrObject)
                        }
                        is KtPropertyAccessor -> {
                            val containingProperty = selfCallable.property
                            computeContainingClass(symbol, containingProperty.containingClassOrObject)
                        }
                        else -> null
                    }
                }
            }
            is KtLightSourceElement -> {}
        }

        return null
    }

    private fun canHaveContainingClassSymbol(symbol: FirBasedSymbol<*>): Boolean {
        return when (symbol) {
            is FirValueParameterSymbol,
            is FirFileSymbol,
            is FirDanglingModifierSymbol,
            is FirScriptSymbol,
            is FirCodeFragmentSymbol,
            is FirAnonymousFunctionSymbol
                -> false
            is FirPropertySymbol
                -> !symbol.isLocal
            else -> true
        }
    }

    private fun computeContainingClass(symbol: FirBasedSymbol<*>, psi: PsiElement?): FirClassLikeSymbol<*>? {
        if (psi !is KtClassOrObject) {
            return null
        }

        val module = symbol.llFirModuleData.ktModule
        val resolveSession = module.getFirResolveSession(module.project)
        return psi.resolveToFirSymbolOfTypeSafe<FirClassLikeSymbol<*>>(resolveSession)
    }
}