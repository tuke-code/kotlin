/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.FileCollection

/**
 * # C Interoperability Settings
 * This interface represents the configuration settings for invoking the [cinterop tool](https://kotlinlang.org/docs/native-c-interop.html)
 * in Kotlin/Native projects.
 * The cinterop tool provides the ability to use C libraries inside Kotlin projects.
 *
 * **Important:** Use the [CInteropSettings] API instead of directly accessing tasks, configurations,
 * and other related domain objects through the Gradle API.
 *
 * ## Example
 * Here is an example of how to use a [CInteropSettings] to configure cinterop task for the linuxX64 target:
 * ```kotlin
 * //build.gradle.kts
 * kotlin {
 *     linuxX64() {
 *         compilations.getByName("main") {
 *             cinterops {
 *                 val cinteropForLinuxX64 by creating {
 *                      // Configure the CInteropSettings here
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 * In this example, we've added a `cinterop` setting named `cinteropForLinuxX64` to the `linuxX64` `main` [KotlinCompilation].
 * These settings will be used to create and configure a `cinterop` task, along with the necessary dependencies for the compile task.
 */
interface CInteropSettings : Named {

    /**
     *  Directories to look for headers.
     */
    interface IncludeDirectories {
        /**
         * Directories for header search (an equivalent of the -I<path> compiler option).
         *
         * #### Usage example
         * The following example demonstrates how to add multiple directories containing header files in a `build.gradle.kts` file:
         * ```kotlin
         * //build.gradle.kts
         * kotlin {
         *     linuxX64() {
         *         compilations.getByName("main") {
         *             cinterops {
         *                 val cinterop by creating {
         *                     includeDirs {
         *                         allHeaders(project.file("src/main/headersDir1"), project.file("src/main/headersDir2"))
         *                     }
         *                 }
         *             }
         *         }
         *     }
         * ```
         * In the example above, the directories `src/main/headersDir1` and `src/main/headersDir2` in the project directory
         * are specified as locations containing the header files required for the `cinterop` process.
         *
         * @param includeDirs The directories to be included.
         */
        fun allHeaders(vararg includeDirs: Any)

        /**
         * @param includeDirs The collection of directories to be included
         * @see [allHeaders]
         */
        fun allHeaders(includeDirs: Collection<Any>)

        /**
         * Additional directories to search headers listed in the 'headerFilter' def-file option.
         * `-headerFilterAdditionalSearchPrefix` command line option equivalent.

         * #### Usage example
         * The following example demonstrates how to add multiple directories containing header files in a `build.gradle.kts` file:
         * ```kotlin
         * //build.gradle.kts
         * kotlin {
         *     linuxX64() {
         *         compilations.getByName("main") {
         *             cinterops {
         *                 val cinterop by creating {
         *                     includeDirs {
         *                         allHeaders(headerFilterOnly(project.file("include/libs"))
         *                     }
         *                 }
         *             }
         *         }
         *     }
         * ```
         * In the example above, the directory `include/libs` will be specified as prefix for the listed in the 'headerFilter' def-file option.
         *
         * @param includeDirs The directories to be included as a prefixes for the header filters.
         */
        fun headerFilterOnly(vararg includeDirs: Any)

        /**
         * @param includeDirs The collection of directories to be included as a prefixes for the header filters.
         * @see [headerFilterOnly]
         */
        fun headerFilterOnly(includeDirs: Collection<Any>)
    }

    /**
     * The collection of libraries, which will be used for building during the C interoperability process.
     * Equals passing `-library`/`-l` to the `cinterop` tool.
     */
    var dependencyFiles: FileCollection

    /**
     * Sets the path to the `.def` file, which declares bindings for the C libraries.
     * This function serves as a setter for the `.def` file path, equivalent to passing `-def` to the `cinterop` tool.
     * #### Usage example
     * The example below shows how to set a custom `.def` file path in a `build.gradle.kts` file:
     * ```kotlin
     * //build.gradle.kts
     * kotlin {
     *     linuxX64 {
     *         compilations.getByName("main").cinterops.create("customCinterop") {
     *             defFile(project.file("custom.def"))
     *         }
     *     }
     *}
     *```
     * In the example above, the `custom.def` file located in the project directory is set as the def file.
     *
     * @param file The path to the `.def` file to be used for C interoperability.
     * **Default value:** `src/nativeInterop/cinterop/{name_of_the_cinterop}.def`
     */
    fun defFile(file: Any)

    /**
     * Defines the package name for the generated bindings.
     * Equals passing `-pkg` to the `cinterop` tool.
     *
     * @param value The package name to be assigned.
     */
    fun packageName(value: String)

    /**
     * Adds a header file to produce kotlin bindings
     * Equals passing `-header` to the `cinterop` tool.
     *
     * @param file The header file to be included for interoperability with C.
     */
    fun header(file: Any) = headers(file)

    /**
     * Adds header files to produce kotlin bindings
     * Equals passing `-header` to the `cinterop` tool.
     *
     * @param files The header files to be included for interoperability with C.
     * @see [header]
     */
    fun headers(vararg files: Any)

    /**
     * Adds header files to produce kotlin bindings
     * Equals passing `-header` to the `cinterop` tool.
     *
     * @param files The collection of header files to be included for interoperability with C.
     * @see [headers]
     */
    fun headers(files: FileCollection)

    /**
     * Adds the specified directories into the set of included headers.
     * Equals passing `-compiler-options -I{firstIncludeDir}...-I{lastIncludeDir}` to the `cinterop` tool.
     *
     * @param values The directories to be included.
     */
    fun includeDirs(vararg values: Any)

    /**
     * @see [includeDirs]
     */
    fun includeDirs(action: Action<IncludeDirectories>)

    /**
     * @see [includeDirs]
     */
    fun includeDirs(configure: IncludeDirectories.() -> Unit)

    /**
     * Adds options that should be passed to the C compiler.
     * Equals passing `-compiler-option` to the `cinterop` tool.
     *
     * @param values compiler options
     */
    fun compilerOpts(vararg values: String)

    /**
     * @see [compilerOpts]
     */
    fun compilerOpts(values: List<String>)

    /**
     * Adds additional linker options.
     * Equals passing `-linker-options` to the `cinterop` tool.
     *
     * @param values linker options
     */
    fun linkerOpts(vararg values: String)

    /**
     * @see [linkerOpts]
     */
    fun linkerOpts(values: List<String>)

    /**
     * Adds any additional options that can be passed to the `cinterop` tool.
     */
    fun extraOpts(vararg values: Any)

    /**
     * @see [extraOpts]
     */
    fun extraOpts(values: List<Any>)
}
