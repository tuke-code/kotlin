/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.concurrent

import kotlin.concurrent.AtomicArray
import kotlin.concurrent.AtomicIntArray
import kotlin.concurrent.AtomicLongArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AtomicIntArrayJvmTest {
    @Test
    fun ctor() {
        assertFailsWith<NegativeArraySizeException> {
            val arrNegativeSize = AtomicIntArray(-5)
        }
    }
}

class AtomicLongArrayJvmTest {
    @Test
    fun ctor() {
        assertFailsWith<NegativeArraySizeException> {
            val arrNegativeSize = AtomicLongArray(-5)
        }
    }
}

class AtomicArrayJvmTest {
    @Test
    fun ctor() {
        assertFailsWith<NegativeArraySizeException> {
            val arrNegativeSize = AtomicArray<Data?>(-5) { Data(1) }
        }
    }
}