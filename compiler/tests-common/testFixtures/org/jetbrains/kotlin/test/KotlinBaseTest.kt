/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.testFramework.FrontendBackendConfiguration
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File

abstract class KotlinBaseTest : KtUsefulTestCase(), FrontendBackendConfiguration {
    protected open fun updateConfiguration(configuration: CompilerConfiguration) {
        configureIrFir(configuration)
    }

    protected open fun setupEnvironment(environment: KotlinCoreEnvironment) {}

    protected fun createConfiguration(
        kind: ConfigurationKind,
        jdkKind: TestJdkKind,
        classpath: List<File>,
    ): CompilerConfiguration {
        val configuration = KotlinTestUtils.newConfiguration(kind, jdkKind, classpath, emptyList())
        updateConfiguration(configuration)
        return configuration
    }
}
