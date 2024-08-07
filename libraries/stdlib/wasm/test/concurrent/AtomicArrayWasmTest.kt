/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package concurrent

import test.concurrent.Data
import kotlin.concurrent.AtomicArray
import kotlin.concurrent.AtomicIntArray
import kotlin.concurrent.AtomicLongArray
import kotlin.test.Test
import kotlin.test.*

class AtomicIntArrayWasmTest {
    @Test
    fun ctor() {
        assertFailsWith<IllegalArgumentException> {
            val arrNegativeSize = AtomicIntArray(-5)
        }.let { ex ->
            assertEquals("Negative array size", ex.message)
        }
    }
}

class AtomicLongArrayWasmTest {
    @Test
    fun ctor() {
        assertFailsWith<IllegalArgumentException> {
            val arrNegativeSize = AtomicLongArray(-5)
        }.let { ex ->
            assertEquals("Negative array size", ex.message)
        }
    }
}

class AtomicArrayWasmTest {
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