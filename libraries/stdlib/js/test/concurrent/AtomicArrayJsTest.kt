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
import kotlin.test.assertEquals
import kotlin.test.assertFails

class AtomicIntArrayJsTest {
    @Test
    fun ctor() {
        assertFails {
            val arrNegativeSize = AtomicIntArray(-5)
        }
    }
}

class AtomicLongArrayJsTest {
    @Test
    fun ctor() {
        assertFails {
            val arrNegativeSize = AtomicLongArray(-5)
        }
    }
}

class AtomicArrayJsTest {
    @Test
    fun ctor() {
        assertFails {
            val arrNegativeSize = AtomicArray<Data?>(-5) { Data(1) }
        }
    }
}