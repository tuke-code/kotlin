import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

abstract class TestInputsCheckExtensionV2 @Inject constructor(objects: ObjectFactory) {
    /**
     * Enable or disable test input checking
     */
    val enabled: Property<Boolean> = objects.property<Boolean>().convention(true)

    /**
     * In fail fast mode, an exception will be thrown immediately after accessing an undeclared input.
     * It's mostly useful for debugging when the tests take a long time to finish.
     */
    val failFast: Property<Boolean> = objects.property<Boolean>().convention(false)

    /**
     * After running your tests at least once, you may want to speed up your feedback loop
     * (adding/removing inputs and verifying if they are enough). This property is here to help.
     *
     * The recommended approach:
     * 1. Remove all custom inputs (like `withStdlibCommon()`) from your `projectTests { ... }`.
     * 2. Execute the test task (it's ok if some of the tests fail). This will collect JFR events about which files are accessed.
     * 3. Enable `testInputsCheck.skipTests`.
     * 4. Start adding the inputs in `projectTests { ... }` one by one.
     * 5. After each change, execute the test task again. This time, the tests will be skipped, and only inputs will be checked.
     *    If the number of undeclared inputs decreased, it means the input you just added is actually needed. If not, remove it.
     * 6. IMPORTANT: Remember to disable `testInputsCheck.skipTests` after finishing. It's only for debugging!
     */
    val skipTests: Property<Boolean> = objects.property<Boolean>().convention(false)
}
