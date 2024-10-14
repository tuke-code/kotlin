/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.tooling.core.HasMutableExtras

/**
 * Represents a logical group of Kotlin files, including sources, resources and additional metadata describing how
 * this group participates in the compilation of this project.
 *
 * For example, here's a common way to access all available within Kotlin source sets:
 * ```
 * kotlin.sourceSets.configureEach {
 *    // Here you can configure all Kotlin source sets.
 *    // For example, to add an additional source directory.
 *    kotlin.srcDir(project.layout.buildDirectory.dir("generatedSources"))
 * }
 * ```
 *
 * @see KotlinSourceSetContainer
 */
interface KotlinSourceSet : Named, HasProject, HasMutableExtras, HasKotlinDependencies {

    /**
     * Represents a set of Kotlin source files that are included in this [KotlinSourceSet].
     *
     * @see SourceDirectorySet
     */
    val kotlin: SourceDirectorySet

    /**
     * Configures the [SourceDirectorySet] containing Kotlin source files with the provided configuration.
     *
     * @see [kotlin]
     */
    fun kotlin(configure: SourceDirectorySet.() -> Unit): SourceDirectorySet

    /**
     * Configures the [SourceDirectorySet] containing Kotlin source files with the provided configuration.
     *
     * @see [kotlin]
     */
    fun kotlin(configure: Action<SourceDirectorySet>): SourceDirectorySet

    /**
     * Represents a set of resource files that are included in this [KotlinSourceSet].
     *
     * @see SourceDirectorySet
     */
    val resources: SourceDirectorySet

    /**
     * Provides the DSL to configure a subset of Kotlin compilation language settings for this [KotlinSourceSet].
     *
     * **Note**: This interface is soft-deprecated.
     * Instead, it is better to use the existing `compilerOptions` DSL.
     */
    val languageSettings: LanguageSettingsBuilder

    /**
     * Configures the [LanguageSettingsBuilder] source set with the provided configuration.
     *
     * **Note**: This interface is soft-deprecated.
     * Instead, it is better to use the existing `compilerOptions` DSL.
     */
    fun languageSettings(configure: LanguageSettingsBuilder.() -> Unit): LanguageSettingsBuilder

    /**
     * Configures this source set [LanguageSettingsBuilder] with the provided configuration.
     *
     * **Note**: This interface is soft-deprecated.
     * Instead, it is better to use the existing `compilerOptions` DSL.
     */
    fun languageSettings(configure: Action<LanguageSettingsBuilder>): LanguageSettingsBuilder

    /**
     * Adds a Kotlin-specific relation to the [other] source set.
     *
     * Consider a general example that has Kotlin source sets `A` and `B`. The expression `A.dependsOn(B)` instructs Kotlin that:
     * - `A` observes the API from `B`, including internal declarations.
     * - `A` can provide actual implementations for expected declarations from `B`. This is a necessary and sufficient condition,
     * as `A` can provide actuals for `B` if and only if `A.dependsOn(B)` either directly or transitive `dependsOn` relation.
     * - `B` can compile to all the targets that `A` compiles to, in addition to its own targets.
     * - `A` inherits all the regular dependencies of `B`.
     *
     * For more information, see [`dependsOn` and source set hierarchies](https://kotlinlang.org/docs/multiplatform-advanced-project-structure.html#dependson-and-source-set-hierarchies).
     */
    fun dependsOn(other: KotlinSourceSet)

    /**
     * Returns a set of source set to which this source set has [dependsOn] relationship.
     *
     * @return a set of source sets added with `dependsOn` relationship at the current state of configuration.
     * Note that Kotlin Gradle plugin may add additional required
     * source sets on late stages of Gradle configuration and the most reliable way to get a full final set is
     * to use this property as a task input with [org.gradle.api.provider.Provider] type.
     */
    val dependsOn: Set<KotlinSourceSet>

    /**
     * @suppress
     */
    @Deprecated(message = "KT-55312")
    val apiMetadataConfigurationName: String

    /**
     * @suppress
     */
    @Deprecated(message = "KT-55312")
    val implementationMetadataConfigurationName: String

    /**
     * @suppress
     */
    @Deprecated(message = "KT-55312")
    val compileOnlyMetadataConfigurationName: String

    /**
     * @suppress
     */
    @Deprecated(message = "KT-55230: RuntimeOnly scope is not supported for metadata dependency transformation")
    val runtimeOnlyMetadataConfigurationName: String

    /**
     * Constants for [KotlinSourceSet].
     */
    companion object {

        /**
         * The default source set name for the main common [KotlinSourceSet] in a Kotlin project targeting multiple platforms.
         *
         * This [KotlinSourceSet] is used to group common Kotlin code shared between all targets.
         */
        const val COMMON_MAIN_SOURCE_SET_NAME = "commonMain"

        /**
         * The default source set name for the tests common [KotlinSourceSet] in a Kotlin project targeting multiple platforms.
         *
         * This [KotlinSourceSet] is used to group common test Kotlin code shared between all targets.
         */
        const val COMMON_TEST_SOURCE_SET_NAME = "commonTest"
    }

    /**
     * Contains a set of custom file extensions used to identify source files for this [KotlinSourceSet].
     *
     * These extensions are evaluated lazily and can include additional custom source file types beyond the default ".kt" and ".kts" ones.
     */
    val customSourceFilesExtensions: Iterable<String> // lazy iterable expected

    /**
     * Adds additional custom source file extensions for the [KotlinSourceSet] so that they are included as compilation inputs.
     *
     * @param extensions A list of string extensions to be added to custom source files. For example:
     * ```
     * kotlin.addCustomSourceFilesExtension(listOf(".customKotlinScript"))
     * ```
     */
    fun addCustomSourceFilesExtensions(extensions: List<String>) {}
}
