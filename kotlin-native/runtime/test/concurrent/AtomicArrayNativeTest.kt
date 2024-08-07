/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.concurrent

import kotlin.concurrent.*
import kotlin.test.*

class AtomicIntArrayNativeTest {
    @Test
    fun ctor() {
        assertFailsWith<IllegalArgumentException> {
            val arrNegativeSize = AtomicIntArray(-5)
        }.let { ex ->
            assertEquals("Negative array size", ex.message)
        }
    }
}

class AtomicLongArrayNativeTest {
    @Test
    fun ctor() {
        assertFailsWith<IllegalArgumentException> {
            val arrNegativeSize = AtomicLongArray(-5)
        }.let { ex ->
            assertEquals("Negative array size", ex.message)
        }
    }
}

class AtomicArrayNativeTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun ctor() {
        assertFailsWith<IllegalArgumentException> {
            val arrNegativeSize = AtomicArray<Data?>(-5) { Data(1) }
        }.let { ex ->
            assertEquals("Negative array size", ex.message)
        }
    }
}