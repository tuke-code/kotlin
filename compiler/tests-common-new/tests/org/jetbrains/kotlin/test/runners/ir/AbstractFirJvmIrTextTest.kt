/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.ir

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_KT_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_SIGNATURES
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.TEST_ALONGSIDE_K1_TESTDATA
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.TestTierChecker
import org.jetbrains.kotlin.test.runners.TestTiers
import org.jetbrains.kotlin.test.runners.codegen.FirPsiCodegenTest
import org.jetbrains.kotlin.test.services.fir.FirOldFrontendMetaConfigurator

abstract class AbstractFirJvmIrTextTest(
    private val parser: FirParser,
) : AbstractJvmIrTextTest<FirOutputArtifact>() {
    override val frontend: FrontendKind<*>
        get() = FrontendKinds.FIR
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade
    override val converter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrResultsConverter

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.commonConfigurationForK2(parser)
    }
}

open class AbstractFirLightTreeJvmIrTextTest : AbstractFirJvmIrTextTest(FirParser.LightTree)

@FirPsiCodegenTest
open class AbstractFirPsiJvmIrTextTest : AbstractFirJvmIrTextTest(FirParser.Psi)

abstract class AbstractTieredFir2IrJvmTest(parser: FirParser) : AbstractFirJvmIrTextTest(parser) {
    override val includeAllDumpHandlers get() = false

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)

        with(builder) {
            defaultDirectives {
                +WITH_STDLIB

                // In the future we'll probably want to preserve all dumps from lower levels,
                // but for now I'd like to avoid clashing test data files.
//                +FIR_DUMP
                -DUMP_IR
                -DUMP_KT_IR
                -DUMP_SIGNATURES

                +TEST_ALONGSIDE_K1_TESTDATA
            }

            // Otherwise, GlobalMetadataInfoHandler may want to write differences to the K1 test data file, not K2
            useMetaTestConfigurators(::FirOldFrontendMetaConfigurator)

            useAfterAnalysisCheckers(
                { TestTierChecker(TestTiers.FIR2IR, targetBackend, it) },
            )
        }
    }
}

open class AbstractTieredFir2IrJvmLightTreeTest : AbstractTieredFir2IrJvmTest(FirParser.LightTree)
