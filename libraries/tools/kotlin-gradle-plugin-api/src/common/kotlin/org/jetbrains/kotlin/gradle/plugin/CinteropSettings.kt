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
 * This class provides base settings for interoperability with C.
 * In terms of Kotlin Native it provides a convenient way for setting options for [cinterop tool](https://kotlinlang.org/docs/native-c-interop.html).
 */
interface CInteropSettings : Named {

    /**
     * The collection of included headers and header filters.
     */
    interface IncludeDirectories {
        /**
         * Adds the specified directories into the set of included headers.
         * Equals passing `-compiler-options -I{firstIncludeDir}...-I{lastIncludeDir}` to the `cinterop` tool.
         *
         * @param includeDirs The directories to be included.
         */
        fun allHeaders(vararg includeDirs: Any)

        /**
         * Same as [allHeaders]
         *
         * @param includeDirs The collection of directories to be included
         */
        fun allHeaders(includeDirs: Collection<Any>)

        /**
         * Adds given directories to the set of included header filters.
         * Equals passing `-headerFilterAdditionalSearchPrefix` to the `cinterop` tool.
         *
         * @param includeDirs The directories to be included for the header filter.
         */
        fun headerFilterOnly(vararg includeDirs: Any)

        /**
         * Same as [headerFilterOnly]
         *
         * @param includeDirs The collection of directories to be included for the header filter.
         */
        fun headerFilterOnly(includeDirs: Collection<Any>)
    }

    /**
     * The collection of libraries, which will be used for building during the C interoperability process.
     * Equals passing `-library`/`-l` to the `cinterop` tool.
     */
    var dependencyFiles: FileCollection

    /**
     * Sets the `.def` file, in which all the settings for `cinterop` util could be passed.
     * Equals passing `-def` to the `cinterop` tool.
     *
     *
     * @param file path to the `.def` file to be used in the interoperability with C
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
     * Same as [header], but for multiple files
     *
     * @param files The header files to be included for interoperability with C.
     */
    fun headers(vararg files: Any)

    /**
     * Same as [header], but for collection of files
     *
     * @param files The collection of header files to be included for interoperability with C.
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
     * Same as [includeDirs]
     */
    fun includeDirs(action: Action<IncludeDirectories>)

    /**
     * Same as [includeDirs]
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
     * Same as [compilerOpts]
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
     * Same as [linkerOpts]
     */
    fun linkerOpts(values: List<String>)

    /**
     * Adds any additional options that can be passed to the `cinterop` tool.
     */
    fun extraOpts(vararg values: Any)

    /**
     * Same as [extraOpts]
     */
    fun extraOpts(values: List<Any>)
}
