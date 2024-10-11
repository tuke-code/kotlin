/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.SimpleDiagnosticsCollector
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.DISABLE_TYPEALIAS_EXPANSION
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.DUMP_VFIR
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.TEST_ALONGSIDE_K1_TESTDATA
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.USE_LATEST_LANGUAGE_VERSION
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.WITH_EXPERIMENTAL_CHECKERS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.WITH_EXTRA_CHECKERS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JDK_KIND
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.WITH_REFLECT
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.EXPLICIT_API_MODE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.EXPLICIT_RETURN_TYPES_MODE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE_VERSION
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.classic.handlers.FirTestDataConsistencyHandler
import org.jetbrains.kotlin.test.frontend.fir.*
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.ScriptingEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.fir.FirOldFrontendMetaConfigurator
import org.jetbrains.kotlin.test.services.fir.FirWithoutAliasExpansionTestSuppressor
import org.jetbrains.kotlin.test.services.fir.LatestLanguageVersionMetaConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind

abstract class AbstractFirDiagnosticTestBase(val parser: FirParser) : AbstractKotlinCompilerTest() {
    override fun TestConfigurationBuilder.configuration() {
        baseFirDiagnosticTestConfiguration()
        enableLazyResolvePhaseChecking()
        configureFirParser(parser)
    }
}

abstract class AbstractFirPsiDiagnosticTest : AbstractFirDiagnosticTestBase(FirParser.Psi)
abstract class AbstractFirLightTreeDiagnosticsTest : AbstractFirDiagnosticTestBase(FirParser.LightTree) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useAdditionalService { LightTreeSyntaxDiagnosticsReporterHolder() }
    }
}

abstract class AbstractFirLightTreeDiagnosticsWithLatestLanguageVersionTest : AbstractFirLightTreeDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurationForTestWithLatestLanguageVersion()
    }
}

abstract class AbstractFirLightTreeDiagnosticsWithoutAliasExpansionTest : AbstractFirLightTreeDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +DISABLE_TYPEALIAS_EXPANSION
            }

            useAfterAnalysisCheckers(::FirWithoutAliasExpansionTestSuppressor)
        }
    }
}

abstract class AbstractTieredFirJvmTest(parser: FirParser) : AbstractFirDiagnosticTestBase(parser) {
    override fun TestConfigurationBuilder.configuration() {
        baseFirDiagnosticTestConfiguration()
        enableLazyResolvePhaseChecking()
        configureFirParser(parser)

        configureFirHandlersStep {
            useHandlers(
                // Makes the FIR tier fail if there are errors; otherwise, it would fail on meta-infos mismatch.
                ::NoFirCompilationErrorsHandler,
            )
        }

        useAfterAnalysisCheckers(
            { TestTierChecker(TestTiers.FIR, TargetBackend.JVM_IR, it) },
        )
    }
}

open class AbstractTieredFirJvmLightTreeTest : AbstractTieredFirJvmTest(FirParser.LightTree)

class LightTreeSyntaxDiagnosticsReporterHolder : TestService {
    val reporter = SimpleDiagnosticsCollector(BaseDiagnosticsCollector.RawReporter.DO_NOTHING)
}

val TestServices.lightTreeSyntaxDiagnosticsReporterHolder: LightTreeSyntaxDiagnosticsReporterHolder? by TestServices.nullableTestServiceAccessor()

abstract class AbstractFirWithActualizerDiagnosticsTest(val parser: FirParser) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +CodegenTestDirectives.IGNORE_FIR2IR_EXCEPTIONS_IF_FIR_CONTAINS_ERRORS
            }
        }
    }

    override fun TestConfigurationBuilder.configuration() {
        configureFirParser(parser)
        baseFirDiagnosticTestConfiguration()

        facadeStep(::Fir2IrResultsConverter)
        irHandlersStep {
            useHandlers(
                ::IrDiagnosticsHandler
            )
        }

        useAdditionalService(::LibraryProvider)

        @OptIn(TestInfrastructureInternals::class)
        useModuleStructureTransformers(DuplicateFileNameChecker, PlatformModuleProvider)
    }
}

open class AbstractFirPsiWithActualizerDiagnosticsTest : AbstractFirWithActualizerDiagnosticsTest(FirParser.Psi)

open class AbstractFirLightTreeWithActualizerDiagnosticsTest : AbstractFirWithActualizerDiagnosticsTest(FirParser.LightTree)
open class AbstractFirLightTreeWithActualizerDiagnosticsWithLatestLanguageVersionTest : AbstractFirLightTreeWithActualizerDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurationForTestWithLatestLanguageVersion()
    }
}

fun TestConfigurationBuilder.configurationForClassicAndFirTestsAlongside(
    testDataConsistencyHandler: Constructor<AfterAnalysisChecker> = ::FirTestDataConsistencyHandler,
) {
    defaultDirectives {
        +TEST_ALONGSIDE_K1_TESTDATA
    }
    useAfterAnalysisCheckers(
        ::FirFailingTestSuppressor,
        testDataConsistencyHandler,
    )
    useMetaTestConfigurators(::FirOldFrontendMetaConfigurator)
}

// `baseDir` is used in Kotlin plugin from IJ infra
fun TestConfigurationBuilder.baseFirDiagnosticTestConfiguration(
    baseDir: String = ".",
    frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>> = ::FirFrontendFacade,
    testDataConsistencyHandler: Constructor<AfterAnalysisChecker> = ::FirTestDataConsistencyHandler,
) {
    globalDefaults {
        frontend = FrontendKinds.FIR
        targetPlatform = JvmPlatforms.defaultJvmPlatform
        dependencyKind = DependencyKind.Source
    }

    enableMetaInfoHandler()

    useConfigurators(
        ::CommonEnvironmentConfigurator,
        ::JvmEnvironmentConfigurator,
        ::ScriptingEnvironmentConfigurator,
    )

    useAdditionalSourceProviders(
        ::AdditionalDiagnosticsSourceFilesProvider.bind(baseDir),
        ::CoroutineHelpersSourceFilesProvider.bind(baseDir),
    )

    facadeStep(frontendFacade)
    firHandlersStep {
        useHandlers(
            ::FirDiagnosticsHandler,
            ::FirDumpHandler,
            ::FirCfgDumpHandler,
            ::FirVFirDumpHandler,
            ::FirCfgConsistencyHandler,
            ::FirResolvedTypesVerifier,
            ::FirScopeDumpHandler,
        )
    }

    useMetaInfoProcessors(::PsiLightTreeMetaInfoProcessor)

    forTestsMatching("compiler/testData/diagnostics/*") {
        configurationForClassicAndFirTestsAlongside(testDataConsistencyHandler)
    }

    forTestsMatching("compiler/fir/analysis-tests/testData/*") {
        defaultDirectives {
            +FIR_DUMP
        }
    }

    forTestsMatching("compiler/fir/analysis-tests/testData/resolve/vfir/*") {
        defaultDirectives {
            +DUMP_VFIR
        }
    }

    forTestsMatching("compiler/fir/analysis-tests/testData/resolve/withAllowedKotlinPackage/*") {
        defaultDirectives {
            +ALLOW_KOTLIN_PACKAGE
        }
    }

    forTestsMatching(
        "compiler/testData/diagnostics/testsWithStdLib/*" or
                "compiler/fir/analysis-tests/testData/resolveWithStdlib/*" or
                "compiler/testData/diagnostics/tests/unsignedTypes/*"
    ) {
        defaultDirectives {
            +WITH_STDLIB
        }
    }

    forTestsMatching("compiler/testData/diagnostics/tests/testsWithExplicitApi/*") {
        defaultDirectives {
            EXPLICIT_API_MODE with ExplicitApiMode.STRICT
        }
    }

    forTestsMatching("compiler/testData/diagnostics/tests/testsWithExplicitReturnTypes/*") {
        defaultDirectives {
            EXPLICIT_RETURN_TYPES_MODE with ExplicitApiMode.STRICT
        }
    }

    forTestsMatching(
        "compiler/fir/analysis-tests/testData/resolve/extraCheckers/*" or
                "compiler/testData/diagnostics/tests/controlFlowAnalysis/deadCode/*"
    ) {
        defaultDirectives {
            +WITH_EXTRA_CHECKERS
        }
    }

    forTestsMatching(
        "compiler/fir/analysis-tests/testData/resolve/extraCheckers/*" or
                "compiler/fir/analysis-tests/testData/resolveWithStdlib/contracts/fromSource/bad/returnsImplies/*" or
                "compiler/fir/analysis-tests/testData/resolveWithStdlib/contracts/fromSource/good/returnsImplies/*"
    ) {
        defaultDirectives {
            +WITH_EXPERIMENTAL_CHECKERS
        }
    }

    forTestsMatching("compiler/testData/diagnostics/tests/testsWithJava17/*") {
        defaultDirectives {
            JDK_KIND with TestJdkKind.FULL_JDK_17
            +WITH_STDLIB
            +WITH_REFLECT
        }
    }

    forTestsMatching("compiler/testData/diagnostics/tests/testsWithJava21/*") {
        defaultDirectives {
            JDK_KIND with TestJdkKind.FULL_JDK_21
            +WITH_STDLIB
            +WITH_REFLECT
        }
    }

    forTestsMatching("compiler/fir/analysis-tests/testData/resolveWithStdlib/properties/backingField/*") {
        defaultDirectives {
            LANGUAGE + "+ExplicitBackingFields"
        }
    }

    forTestsMatching("compiler/testData/diagnostics/tests/multiplatform/*") {
        defaultDirectives {
            LANGUAGE + "+MultiPlatformProjects"
        }
    }

    defaultDirectives {
        LANGUAGE + "+EnableDfaWarningsInK2"
    }
}

fun TestConfigurationBuilder.configurationForTestWithLatestLanguageVersion() {
    defaultDirectives {
        LANGUAGE_VERSION with LanguageVersion.entries.last()
        +ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
        +USE_LATEST_LANGUAGE_VERSION
    }
    useMetaTestConfigurators(::LatestLanguageVersionMetaConfigurator)
    useAfterAnalysisCheckers(
        ::FirTestDataConsistencyHandler,
        ::LatestLVIdenticalChecker,
    )
}


class FirLazyDeclarationResolverWithPhaseCheckingSessionComponentRegistrar : FirSessionComponentRegistrar() {
    private val lazyResolver = FirCompilerLazyDeclarationResolverWithPhaseChecking()

    @OptIn(SessionConfiguration::class)
    override fun registerAdditionalComponent(session: FirSession) {
        session.register(FirLazyDeclarationResolver::class, lazyResolver)
    }
}

fun TestConfigurationBuilder.enableLazyResolvePhaseChecking() {
    useAdditionalServices(
        service<FirSessionComponentRegistrar>(::FirLazyDeclarationResolverWithPhaseCheckingSessionComponentRegistrar.coerce())
    )

    // It's important to filter out failures from lazy resolve before calling other suppressors like BlackBoxCodegenSuppressor
    // Otherwise other suppressors can filter out every failure from test and keep it as ignored even if
    // the only problem in lazy resolve contracts, which disables with special directive
    useAfterAnalysisCheckers(::DisableLazyResolveChecksAfterAnalysisChecker, insertAtFirst = true)

    configureFirHandlersStep {
        useHandlers(
            ::FirResolveContractViolationErrorHandler,
        )
    }
}

