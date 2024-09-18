/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

import java.util.concurrent.atomic.*

public actual class AtomicIntArray public actual constructor(public actual val size: Int) {
    public actual constructor(array: IntArray) : this(array.size)

    public actual fun loadAt(index: Int): Int = TODO()

    public actual fun storeAt(index: Int, newValue: Int) { TODO() }

    public actual fun exchangeAt(index: Int, newValue: Int): Int = TODO()

    public actual fun compareAndSetAt(index: Int, expectedValue: Int, newValue: Int): Boolean = TODO()

    public actual fun compareAndExchangeAt(index: Int, expectedValue: Int, newValue: Int): Int = TODO()

    public actual fun fetchAndAddAt(index: Int, delta: Int): Int = TODO()

    public actual fun addAndFetchAt(index: Int, delta: Int): Int = TODO()

    public actual fun fetchAndIncrementAt(index: Int): Int = TODO()

    public actual fun incrementAndFetchAt(index: Int): Int = TODO()

    public actual fun fetchAndDecrementAt(index: Int): Int = TODO()

    public actual fun decrementAndFetchAt(index: Int): Int = TODO()

    public actual override fun toString(): String = TODO()
}

/**
 * Casts the given [AtomicIntArray] instance to [java.util.concurrent.atomic.AtomicIntegerArray].
 */
@Suppress("UNCHECKED_CAST")
public fun AtomicIntArray.asJavaAtomicArray(): AtomicIntegerArray = this as AtomicIntegerArray

/**
 * Casts the given [java.util.concurrent.atomic.AtomicIntegerArray] instance to [AtomicIntArray].
 */
@Suppress("UNCHECKED_CAST")
public fun AtomicIntegerArray.asKotlinAtomicArray(): AtomicIntArray = this as AtomicIntArray

public actual class AtomicLongArray public actual constructor(public actual val size: Int) {

    public actual constructor(array: LongArray) : this(array.size) { TODO() }

    public actual fun loadAt(index: Int): Long = TODO()

    public actual fun storeAt(index: Int, newValue: Long) { TODO() }

    public actual fun exchangeAt(index: Int, newValue: Long): Long = TODO()

    public actual fun compareAndSetAt(index: Int, expectedValue: Long, newValue: Long): Boolean = TODO()

    public actual fun compareAndExchangeAt(index: Int, expectedValue: Long, newValue: Long): Long = TODO()

    public actual fun fetchAndAddAt(index: Int, delta: Long): Long = TODO()

    public actual fun addAndFetchAt(index: Int, delta: Long): Long = TODO()

    public actual fun fetchAndIncrementAt(index: Int): Long = TODO()

    public actual fun incrementAndFetchAt(index: Int): Long = TODO()

    public actual fun fetchAndDecrementAt(index: Int): Long = TODO()

    public actual fun decrementAndFetchAt(index: Int): Long = TODO()

    public actual override fun toString(): String = TODO()
}

/**
 * Casts the given [AtomicLongArray] instance to [java.util.concurrent.atomic.AtomicLongArray].
 */
@Suppress("UNCHECKED_CAST")
public fun AtomicLongArray.asJavaAtomicArray(): java.util.concurrent.atomic.AtomicLongArray = this as java.util.concurrent.atomic.AtomicLongArray

/**
 * Casts the given [java.util.concurrent.atomic.AtomicLongArray] instance to [AtomicLongArray].
 */
@Suppress("UNCHECKED_CAST")
public fun java.util.concurrent.atomic.AtomicLongArray.asKotlinAtomicArray(): AtomicLongArray = this as AtomicLongArray

public actual class AtomicArray<T> {
    private val array: Array<T>

    public actual constructor(array: Array<T>) {
        this.array = array.copyOf()
    }

    public actual val size: Int get() = array.size

    public actual fun loadAt(index: Int): T = TODO()

    public actual fun storeAt(index: Int, newValue: T) { TODO() }

    public actual fun exchangeAt(index: Int, newValue: T): T = TODO()

    public actual fun compareAndSetAt(index: Int, expectedValue: T, newValue: T): Boolean = TODO()

    public actual fun compareAndExchangeAt(index: Int, expectedValue: T, newValue: T): T = TODO()

    public actual override fun toString(): String = TODO()
}

/**
 * Casts the given [AtomicArray]<T> instance to [java.util.concurrent.atomic.AtomicReferenceArray]<T>.
 */
@Suppress("UNCHECKED_CAST")
public fun <T> AtomicArray<T>.asJavaAtomicArray(): AtomicReferenceArray<T> = this as AtomicReferenceArray<T>

/**
 * Casts the given [java.util.concurrent.atomic.AtomicReferenceArray]<T> instance to [AtomicArray]<T>.
 */
@Suppress("UNCHECKED_CAST")
public fun <T> AtomicReferenceArray<T>.asKotlinAtomicArray(): AtomicArray<T> = this as AtomicArray<T>