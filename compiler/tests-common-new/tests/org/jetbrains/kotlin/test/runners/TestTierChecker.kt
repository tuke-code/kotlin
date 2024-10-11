/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.backend.handlers.JvmBoxRunner
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_K2
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_K2_MULTI_MODULE
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_MULTI_MODULE
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.utils.addToStdlib.previous
import org.opentest4j.AssertionFailedError
import java.io.File

/**
 * See the description of
 * [RUN_PIPELINE_TILL][org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.RUN_PIPELINE_TILL].
 */
enum class TestTiers {
    SOURCE,
    FIR,
    FIR2IR,
    KLIB,
    BACKEND,
    BOX,
}

fun WrappedException.guessTierOfFailure(): TestTiers? {
    val tierArtifactKind = when (this) {
        is WrappedException.FromFacade -> facade.outputKind
        is WrappedException.FromHandler -> when {
            handler is JvmBoxRunner -> return TestTiers.BOX
            else -> handler.artifactKind
        }
        else -> return null
    }

    return when (tierArtifactKind) {
        FrontendKinds.FIR -> TestTiers.FIR
        BackendKinds.IrBackend -> TestTiers.FIR2IR
        BackendKinds.IrBackendForK1AndK2 -> TestTiers.FIR2IR
        ArtifactKinds.KLib -> TestTiers.KLIB
        ArtifactKinds.Jvm -> TestTiers.BACKEND
        ArtifactKinds.JvmFromK1AndK2 -> TestTiers.BACKEND
        ArtifactKinds.Js -> TestTiers.BACKEND
        ArtifactKinds.Native -> TestTiers.BACKEND
        ArtifactKinds.Wasm -> TestTiers.BACKEND
        else -> TestTiers.BACKEND
    }
}

private val IGNORE_BACKEND_DIRECTIVES = listOf(
    IGNORE_BACKEND,
    IGNORE_BACKEND_K2,
    IGNORE_BACKEND_MULTI_MODULE,
    IGNORE_BACKEND_K2_MULTI_MODULE,
)

class TestTierChecker(
    private val lastTierCurrentPipelineExecutes: TestTiers,
    private val targetBackend: TargetBackend,
    testServices: TestServices,
) : AfterAnalysisChecker(testServices) {
    override fun check(failedAssertions: List<WrappedException>) {}

    private fun analyzeFailures(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (FirDiagnosticsDirectives.RUN_PIPELINE_TILL !in testServices.moduleStructure.allDirectives) {
            return emptyList()
        }

        val declaredTier = testServices.moduleStructure.allDirectives[FirDiagnosticsDirectives.RUN_PIPELINE_TILL]
            .first().let(TestTiers::valueOf)

        val latestMarkedTier = testServices.moduleStructure.modules.maxOf {
            when {
                it.targetPlatform.isCommon() -> TestTiers.KLIB
                IGNORE_BACKEND_DIRECTIVES.any { directive -> targetBackend in it.directives[directive] } -> TestTiers.KLIB
                else -> TestTiers.BOX
            }
        }

        val (trackedFailures, ignoredFailures) = failedAssertions.partition {
            val tierOfFailure = it.guessTierOfFailure() ?: return@partition true
            tierOfFailure <= declaredTier
        }

        if (trackedFailures.isNotEmpty()) {
            return trackedFailures
        }

        fun requestTierUpgrade(to: TestTiers): Nothing =
            throw "Looks like some tiers above $declaredTier pass with no exceptions. Please update the tier directive to `// ${FirDiagnosticsDirectives.RUN_PIPELINE_TILL}: ${to.name}` and regenerate tests."
                .let(::TieredHandlerException)

        if (ignoredFailures.isEmpty()) {
            val possibleTierUpgrade = minOf(latestMarkedTier, lastTierCurrentPipelineExecutes)

            if (declaredTier < possibleTierUpgrade) {
                requestTierUpgrade(possibleTierUpgrade)
            }

            if (latestMarkedTier == declaredTier && latestMarkedTier < lastTierCurrentPipelineExecutes) {
                return emptyList()
            }

            if (latestMarkedTier < declaredTier) {
                throw "The test declares the tier $declaredTier, but is configured to only run up to $latestMarkedTier. Please set `// ${FirDiagnosticsDirectives.RUN_PIPELINE_TILL}: ${latestMarkedTier}` and regenerate tests as we can't check later tiers for it"
                    .let(::TieredHandlerException)
            }

            if (lastTierCurrentPipelineExecutes == TestTiers.entries.last()) {
                return emptyList()
            }

            throw "Please regenerate tests, this test runner can only run up to $lastTierCurrentPipelineExecutes (including), but the test seems to successfully pass all these tiers."
                .let(::TieredHandlerException)
        }

        // `ignoredFailures` can never contain the first tier
        val lowestIgnoredTier = ignoredFailures.mapNotNull { it.guessTierOfFailure() }.min()
        val suggestedDeclaredTier = lowestIgnoredTier.previous

        if (lastTierCurrentPipelineExecutes < suggestedDeclaredTier) {
            throw "Test infrastructure misconfiguration: this `TestTierChecker` declares `lastTierCurrentPipelineExecutes = $lastTierCurrentPipelineExecutes`, but we've just caught an error from a higher tier ($suggestedDeclaredTier)"
                .let(::TieredHandlerException)
        }

        return when (declaredTier) {
            suggestedDeclaredTier -> emptyList()
            else -> requestTierUpgrade(suggestedDeclaredTier)
        }
    }

    private class TieredHandlerException(override val message: String) : Exception(message)

    companion object {
        const val TIERED_FAILURE_EXTENSION = ".tiered-failure.txt"
    }

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        return try {
            analyzeFailures(failedAssertions)
        } catch (e: TieredHandlerException) {
            val originalFile = testServices.moduleStructure.modules.first().files.first().originalFile
            val failFile = File(originalFile.path.removeSuffix(".kt") + TIERED_FAILURE_EXTENSION)

            if (!failFile.exists()) {
                return WrappedException.FromAfterAnalysisChecker(e).let(::listOf)
            }

            try {
                testServices.assertions.assertEqualsToFile(failFile, e.message, sanitizer = { it.replace("\r", "").trim() })
            } catch (a: AssertionFailedError) {
                return WrappedException.FromAfterAnalysisChecker(a).let(::listOf)
            }

            emptyList()
        }
    }
}
