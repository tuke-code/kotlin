/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.smoke

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.mpp.KmpIncrementalITBase
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceFirst
import org.jetbrains.kotlin.gradle.util.replaceText
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path

/**
 * Multi-module KMP tests assert that IC is fine, when API changes between two modules, or common/platform parts of a module.
 */
@DisplayName("Basic multi-module incremental scenarios with KMP - K2")
@MppGradlePluginTests
open class MultiModuleIncrementalCompilationIT : KmpIncrementalITBase() {

    @DisplayName("Verify IC builds on change in lib/commonMain")
    @GradleTest
    fun testTouchLibCommon(gradleVersion: GradleVersion) = withProject(gradleVersion) {
        /**
         * [Cross-Module] touch libCommon, affect appCommon, appJs
         * Variants: 1. code-compatible ABI breakage (change deduced return type), 2. no ABI breakage, 3. error + fix
         */

        build("assemble")
//TODO ayy, it's the last one to write, innit?
        //we already know that touching lib/common activates all builds
        //we can just test the rebuild of affected files, i guess, in each sourceset
        /**
         * Step 1: code-compatible ABI breakage
         */
        testCase(
            incrementalPath = resolvePath("app", "commonMain", "PlainPublicClass.kt").addPrivateVal(),
            executedTasks = setOf(
                ":app:compileKotlinJvm",
                ":app:compileKotlinJs",
                ":app:compileKotlinNative"
            )
        )

        /**
         * Step 2: keep ABI, recompile anyway
         */
    }

    /**
     * Three platforms, two steps for each. Do source-compatible changes: first add default parameter,
     * then change return type.
     * lib/platform utils are used in app/platform with deduced return type.
     */
    @DisplayName("Verify IC builds on change in lib/platformMain")
    @GradleTest
    fun testTouchLibPlatform(gradleVersion: GradleVersion) = withProject(gradleVersion) {
        build("assemble")

        // initially utils have form "fun libJvmPlatformUtil(): Int = 400", with different numbers
        fun Path.addDefaultParameter(): Path {
            replaceFirst("Util():", "Util(unused: Int = 1):")
            return this
        }

        fun Path.changeReturnType(): Path {
            replaceFirst(": Int = ", ": Double = 1.")
            return this
        }

        /**
         * Step 1 - jvm
         */
        val jvmUtil = resolvePath("lib", "jvmMain", "libJvmPlatformUtil.kt")
        multiStepTestCase(
            executedTasks = setOf(
                ":app:compileKotlinJvm",
                ":lib:compileKotlinJvm"
            ),
            steps = listOf(
                { jvmUtil.addDefaultParameter() },
                { jvmUtil.changeReturnType() }
            )
        ) {
            assertCompiledKotlinSources( //TODO what a boring boilerplate, need to write a better util there
                expectedSources = listOf(
                    jvmUtil,
                    resolvePath("app", "jvmMain", "useLibJvmPlatformUtil.kt")
                ).relativizeTo(projectPath),
                output = output
            )
        }

        /**
         * Step 2 - js
         */
        val jsUtil = resolvePath("lib", "jsMain", "libJsPlatformUtil.kt")
        multiStepTestCase(
            executedTasks = setOf(
                ":app:compileKotlinJs",
                ":lib:compileKotlinJs"
            ),
            steps = listOf(
                { jsUtil.addDefaultParameter() },
                { jsUtil.changeReturnType() }
            )
        ) {
            assertIncrementalCompilation( //TODO what a boring boilerplate, need to write a better util there
                listOf(
                    jsUtil,
                    resolvePath("app", "jsMain", "useLibJsPlatformUtil.kt")
                ).relativizeTo(projectPath)
            )
        }

        /**
         * Step 3 - native
         */
        val nativeUtil = resolvePath("lib", "nativeMain", "libNativePlatformUtil.kt")
        multiStepTestCase(
            executedTasks = setOf(
                ":app:compileKotlinNative",
                ":lib:compileKotlinNative"
            ),
            steps = listOf(
                { nativeUtil.addDefaultParameter() },
                { nativeUtil.changeReturnType() }
            )
        )
    }

    /**
     * Main smoke tests for api changes on the source set boundary
     */
    @DisplayName("Verify IC builds on change in app/commonMain")
    @GradleTest
    fun testTouchAppCommon(gradleVersion: GradleVersion) = withProject(gradleVersion) {
        //remove the use of util from test sources
        resolvePath("app", "commonTest", "TrivialTest.kt").replaceText("sayYes()", "\"Yes\"")

        build("assemble")

        // fun sayYes from CommonUtils.kt is used by useAppCommon.kt in every module to initialize a member value
        val utilPath = resolvePath("app", "commonMain", "CommonUtils.kt")

        /**
         * Step 1 - add default param
         */
        utilPath.replaceFirst(
            "fun sayYes() = \"Yes\"",
            "fun sayYes(clarification: String = \"\") = \"Yes\$clarification\""
        )
        testCase(
            executedTasks = setOf(
                ":app:compileKotlinJvm",
                ":app:compileKotlinJs",
                ":app:compileKotlinNative"
            )
        ) {
            assertIncrementalCompilation(
                listOf(
                    utilPath,
                    resolvePath("app", "jsMain", "useAppCommon.kt"),
                    resolvePath("app", "jvmMain", "useAppCommon.kt")
                ).relativizeTo(projectPath)
            )
        }

        /**
         * Step 2 - change return type with source compatibility
         */
        utilPath.replaceFirst(
            "= \"Yes\$clarification\"",
            "= 15"
        )
        testCase(
            executedTasks = setOf(
                ":app:compileKotlinJvm",
                ":app:compileKotlinJs",
                ":app:compileKotlinNative"
            )
        ) {
            assertIncrementalCompilation(
                listOf(
                    utilPath,
                    resolvePath("app", "jsMain", "useAppCommon.kt"),
                    resolvePath("app", "jvmMain", "useAppCommon.kt")
                ).relativizeTo(projectPath)
            )
        }
        //TODO fix duplication - probably would get fixed by broken compilation
        // or by multiStepTestCase

        //TODO KT-56963 : confirm and create issues for these source-compatible changes
        //utilKtPath.replaceFirst("fun multiplyByTwo(n: Int): Int", "fun <T> multiplyByTwo(n: T): T") - breaks native
        //utilKtPath.replaceFirst("fun multiplyByTwo(n: Int): Int", "fun multiplyByTwo(n: Int, unused: Int = 42): Int") - breaks js
    }


    /**
     * Platform changes in a non-dependency shouldn't affect anything else
     */
    @DisplayName("Verify IC builds on change in app/platformMain")
    @GradleTest
    fun testTouchAppPlatform(gradleVersion: GradleVersion) = withProject(gradleVersion) {
        build("assemble")

        fun Path.addParentClass(childName: String, parentName: String): Path {
            replaceFirst("class $childName {", "class $childName : $parentName() {")
            return this
        }

        /**
         * Step 1 - jvm
         */
        val changedJvmSource = resolvePath("app", "jvmMain", "PlainPublicClassJvm.kt").addParentClass(
            "PlainPublicClassJvm", "PlainPublicClass"
        )
        testCase(
            executedTasks = setOf(":app:compileKotlinJvm")
        ) {
            assertCompiledKotlinSources(listOf(changedJvmSource).relativizeTo(projectPath), output)
        }

        /**
         * Step 2 - js
         */
        testCase(
            incrementalPath = resolvePath("app", "jsMain", "PlainPublicClassJs.kt").addParentClass(
                "PlainPublicClassJs", "PlainPublicClass"
            ),
            executedTasks = setOf(":app:compileKotlinJs")
        )

        /**
         * Step 3 - native
         */
        resolvePath("app", "nativeMain", "PlainPublicClassNative.kt").addParentClass(
            "PlainPublicClassNative", "PlainPublicClass"
        )
        testCase(
            executedTasks = setOf(":app:compileKotlinNative")
        )
    }
}

@DisplayName("Incremental scenarios with expect/actual - K1")
class MultiModuleIncrementalCompilationK1IT : MultiModuleIncrementalCompilationIT() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copyEnsuringK1()
}
