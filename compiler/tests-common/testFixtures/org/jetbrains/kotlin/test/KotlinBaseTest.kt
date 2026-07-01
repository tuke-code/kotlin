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

abstract class KotlinBaseTest<F : KotlinBaseTest.TestFile> : KtUsefulTestCase(), FrontendBackendConfiguration {
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

    open class TestFile @JvmOverloads constructor(
        @JvmField val name: String,
        @JvmField val content: String,
        @JvmField val directives: Directives = Directives()
    ) : Comparable<TestFile> {
        override operator fun compareTo(other: TestFile): Int {
            return name.compareTo(other.name)
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is TestFile && other.name == name
        }

        override fun toString(): String {
            return name
        }

    }

    open class TestModule(
        @JvmField val name: String,
        @JvmField val dependenciesSymbols: List<String>,
        @JvmField val friendsSymbols: List<String>,
        // mimics the name from ModuleStructureExtractorImpl, thought later converted to `-Xfragment-refines` parameter
        @JvmField val dependsOnSymbols: List<String> = listOf(),
    ) : Comparable<TestModule> {

        val dependencies: MutableList<TestModule> = arrayListOf()
        val friends: MutableList<TestModule> = arrayListOf()
        val dependsOn: MutableList<TestModule> = arrayListOf()

        override fun compareTo(other: TestModule): Int = name.compareTo(other.name)

        override fun toString(): String = name
    }
}
