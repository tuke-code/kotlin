/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

/**
 * This interface was named this way to avoid popping up when searching for "Tiered",
 * as you're most likely want to find the actual test runners.
 */
interface SupportForTieredTestRunners {
    /**
     * Test runners of later [tiers][TestTiers] may be run for test data
     * originally designed for lower tiers, but sometimes handlers interfere
     * with one another.
     * Until this is fixed, tiered runners need a workaround.
     * See: KT-67281.
     */
    val includeAllDumpHandlers: Boolean get() = true

    /**
     * From the point of view of [TestTiers], `BOX` is a separate tier:
     * if the test declares `BACKEND`, but passes for `BOX`, we want
     * to emit a notification suggesting bumping up the tier.
     */
    val enableBoxHandler: Boolean get() = true
}
