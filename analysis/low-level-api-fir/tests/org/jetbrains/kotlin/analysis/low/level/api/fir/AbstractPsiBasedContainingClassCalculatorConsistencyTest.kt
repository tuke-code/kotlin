/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractPsiBasedContainingClassCalculatorConsistencyTest.Directives.ALLOW_PSI_PRESENCE
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.PsiBasedContainingClassCalculator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import kotlin.test.assertEquals
import kotlin.test.fail

abstract class AbstractPsiBasedContainingClassCalculatorConsistencyTest : AbstractAnalysisApiBasedTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val resolveSession = mainModule.ktModule.getFirResolveSession(mainFile.project)
        val firFile = mainFile.getOrBuildFirFile(resolveSession)

        val allowedPsiPresence = mainModule.testModule.directives[ALLOW_PSI_PRESENCE].toSet()

        firFile.accept(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                if (element is FirDeclaration) {
                    checkDeclaration(element, allowedPsiPresence)
                }

                element.acceptChildren(this)
            }
        })
    }

    private fun checkDeclaration(fir: FirDeclaration, allowedPsiPresence: Set<String>) {
        val symbol = fir.symbol
        val compilerContainingSymbol = symbol.getContainingClassSymbol()
        val psiContainingSymbol = PsiBasedContainingClassCalculator.getContainingClassSymbol(symbol)

        val signature = computeSignature(symbol)

        if (signature in allowedPsiPresence) {
            if (psiContainingSymbol == null) {
                fail("Containing symbol for $signature is not calculated by PSI, the directive is useless")
            } else if (compilerContainingSymbol != null) {
                fail("Containing symbol for $signature is calculated by PSI and compiler, and the latter is unexpected")
            }
        } else {
            assertEquals(compilerContainingSymbol, psiContainingSymbol, "Containing declarations for $signature do not match")
        }
    }

    private fun computeSignature(symbol: FirBasedSymbol<*>): String {
        return when (symbol) {
            is FirClassLikeSymbol<*> -> symbol.classId.asString()
            is FirCallableSymbol<*> -> symbol.callableId.toString()
            else -> "$symbol"
        }
    }

    object Directives : SimpleDirectivesContainer() {
        val ALLOW_PSI_PRESENCE by stringDirective("Do not fail the test if the containing class can be calculated only by PSI")
    }
}