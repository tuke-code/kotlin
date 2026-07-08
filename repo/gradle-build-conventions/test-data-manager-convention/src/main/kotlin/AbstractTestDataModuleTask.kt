/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Base class for the `-P`-driven test data manager tasks: [CheckTestDataModuleTask] (`checkTestData`)
 * and [UpdateTestDataModuleTask] (`updateTestData`).
 *
 * Both tasks are [JavaExec] tasks that run the module's tests via `TestDataManagerRunner` with a
 * **fixed** mode. Their options are accepted **only** as Gradle properties (`-P`), never as `--option`
 * CLI flags.
 *
 * ## Why `-P` options instead of `@Option`
 *
 * Only values **consumed during the configuration phase** are part of Gradle's configuration-cache
 * (CC) key. `@Option` CLI flags are applied while the task is *configured*, so iterating on, e.g.,
 * `--test-data-path` via `--option` re-runs configuration (1–2 minutes on this project) for each
 * distinct value. `-P` properties read **only at execution time** are not configuration inputs, so
 * the CC entry stays stable across option values and subsequent invocations skip reconfiguration.
 *
 * ## Trade-off: options are not tracked inputs
 *
 * These tasks do not declare the options as `@Input` properties at all — [exec] reads the `-P` values
 * directly. As a result Gradle cannot see the options as task inputs, so the tasks are never
 * UP-TO-DATE, never restored from the build cache, and the runner is invoked on every invocation. This
 * is acceptable because they are [JavaExec] tasks with no declared outputs that always re-run anyway.
 *
 * Note this is a deliberate simplification, **not** a requirement of CC-friendliness: `@Input` values
 * feed task up-to-date/build-cache identity at *execution* time, not the CC key. The options could
 * instead be exposed as `@Input` providers fed from `-P` and still keep the CC stable, as long as
 * those providers are never resolved during configuration.
 *
 * ## Options
 *
 * All options are passed as Gradle properties; they are forwarded to the test runner as `-D<key>`
 * system properties at execution time.
 *
 * | Property                                                              | Effect                                          |
 * |-----------------------------------------------------------------------|-------------------------------------------------|
 * | `org.jetbrains.kotlin.testDataManager.options.testDataPath`           | Comma-separated test data paths (dir or file)   |
 * | `org.jetbrains.kotlin.testDataManager.options.testClassPattern`       | Regex pattern for test class names              |
 * | `org.jetbrains.kotlin.testDataManager.options.goldenOnly`             | Run only golden tests (empty variant chain)     |
 * | `org.jetbrains.kotlin.testDataManager.options.incremental`            | Only run variant tests for changed golden paths |
 *
 * @see CheckTestDataModuleTask
 * @see UpdateTestDataModuleTask
 */
abstract class AbstractTestDataModuleTask : JavaExec() {
    @get:Inject
    protected abstract val providers: ProviderFactory

    /**
     * The fixed mode this task runs in; forwarded to the runner. Each concrete subclass pins it.
     *
     * `@Internal` because the mode is a constant of the task type, not a tracked input — see the
     * class KDoc on why these tasks deliberately expose no `@Input` options.
     */
    @get:Internal
    protected abstract val mode: Mode

    init {
        group = "verification"
        mainClass.set("org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerRunner")
    }

    /**
     * Forwards the fixed [mode] and all `-P` options to the test runner as `-D` system properties,
     * then delegates to [JavaExec.exec].
     *
     * All options are forwarded regardless of [mode] for symmetry; the runner ignores options that are
     * irrelevant to the current mode (e.g. `incremental` is effective only in update mode).
     */
    @TaskAction
    override fun exec() {
        systemProperty(TestDataManagerOption.MODE, mode)
        forwardOption(TestDataManagerOption.TEST_DATA_PATH)
        forwardOption(TestDataManagerOption.TEST_CLASS_PATTERN)
        forwardOption(TestDataManagerOption.GOLDEN_ONLY)
        forwardOption(TestDataManagerOption.INCREMENTAL)
        super.exec()
    }

    private fun forwardOption(key: String) {
        providers.gradleProperty(key).orNull?.let { systemProperty(key, it) }
    }

    /**
     * How a test data manager task handles test data file mismatches:
     * - [CHECK] fails on mismatches without modifying anything.
     * - [UPDATE] rewrites mismatched files.
     *
     * The forwarded name is parsed by `TestDataManagerRunner` inside the test process.
     */
    protected enum class Mode {
        CHECK,
        UPDATE,
    }
}
