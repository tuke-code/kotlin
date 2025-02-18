/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.runners

import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractDiagnosticCompilerTestDataTest
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

abstract class AbstractLLFirSerializationDiagnosticTest : AbstractDiagnosticCompilerTestDataTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureSerializationFirPsiDiagnosticTest()
    }
}
