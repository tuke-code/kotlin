/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

public actual class AtomicIntArray {
    private val array: IntArray

    public actual constructor(size: Int) {
        array = IntArray(size)
    }

    public actual constructor(array: IntArray) {
        this.array = array.copyOf()
    }

    public actual val size: Int get() = array.size

    public actual fun loadAt(index: Int): Int {
        checkBounds(index)
        return array[index]
    }

    public actual fun storeAt(index: Int, newValue: Int) {
        checkBounds(index)
        array[index] = newValue
    }

    public actual fun exchangeAt(index: Int, newValue: Int): Int {
        checkBounds(index)
        val oldValue = array[index]
        array[index] = newValue
        return oldValue
    }

    public actual fun compareAndSetAt(index: Int, expectedValue: Int, newValue: Int): Boolean {
        checkBounds(index)
        if (array[index] != expectedValue) return false
        array[index] = newValue
        return true
    }

    public actual fun compareAndExchangeAt(index: Int, expectedValue: Int, newValue: Int): Int {
        checkBounds(index)
        val oldValue = array[index]
        if (oldValue == expectedValue) {
            array[index] = newValue
        }
        return oldValue
    }

    public actual fun fetchAndAddAt(index: Int, delta: Int): Int {
        checkBounds(index)
        val oldValue = array[index]
        array[index] += delta
        return oldValue
    }

    public actual fun addAndFetchAt(index: Int, delta: Int): Int {
        checkBounds(index)
        array[index] += delta
        return array[index]
    }

    public actual fun fetchAndIncrementAt(index: Int): Int {
        checkBounds(index)
        return array[index]++
    }

    public actual fun incrementAndFetchAt(index: Int): Int {
        checkBounds(index)
        return ++array[index]
    }

    public actual fun fetchAndDecrementAt(index: Int): Int {
        checkBounds(index)
        return array[index]--
    }

    public actual fun decrementAndFetchAt(index: Int): Int {
        checkBounds(index)
        return --array[index]
    }

    public actual override fun toString(): String = array.toString()

    private fun checkBounds(index: Int) {
        if (index < 0 || index >= array.size) throw IndexOutOfBoundsException("index $index")
    }
}

public actual class AtomicLongArray {
    private val array: LongArray

    public actual constructor(size: Int) {
        array = LongArray(size)
    }

    public actual constructor(array: LongArray) {
        this.array = array.copyOf()
    }

    public actual val size: Int get() = array.size

    public actual fun loadAt(index: Int): Long {
        checkBounds(index)
        return array[index]
    }

    public actual fun storeAt(index: Int, newValue: Long) {
        checkBounds(index)
        array[index] = newValue
    }

    public actual fun exchangeAt(index: Int, newValue: Long): Long {
        checkBounds(index)
        val oldValue = array[index]
        array[index] = newValue
        return oldValue
    }

    public actual fun compareAndSetAt(index: Int, expectedValue: Long, newValue: Long): Boolean {
        checkBounds(index)
        if (array[index] != expectedValue) return false
        array[index] = newValue
        return true
    }

    public actual fun compareAndExchangeAt(index: Int, expectedValue: Long, newValue: Long): Long {
        checkBounds(index)
        val oldValue = array[index]
        if (oldValue == expectedValue) {
            array[index] = newValue
        }
        return oldValue
    }

    public actual fun fetchAndAddAt(index: Int, delta: Long): Long {
        checkBounds(index)
        val oldValue = array[index]
        array[index] += delta
        return oldValue
    }

    public actual fun addAndFetchAt(index: Int, delta: Long): Long {
        checkBounds(index)
        array[index] += delta
        return array[index]
    }

    public actual fun fetchAndIncrementAt(index: Int): Long {
        checkBounds(index)
        return array[index]++
    }

    public actual fun incrementAndFetchAt(index: Int): Long {
        checkBounds(index)
        return ++array[index]
    }

    public actual fun fetchAndDecrementAt(index: Int): Long {
        checkBounds(index)
        return array[index]--
    }

    public actual fun decrementAndFetchAt(index: Int): Long {
        checkBounds(index)
        return --array[index]
    }

    public actual override fun toString(): String = array.toString()

    private fun checkBounds(index: Int) {
        if (index < 0 || index >= array.size) throw IndexOutOfBoundsException("index $index")
    }
}

public actual class AtomicArray<T> {
    private val array: Array<T>

    public actual constructor (array: Array<T>) {
        this.array = array.copyOf()
    }

    public actual val size: Int get() = array.size

    public actual fun loadAt(index: Int): T {
        checkBounds(index)
        return array[index]
    }

    public actual fun storeAt(index: Int, newValue: T) {
        checkBounds(index)
        array[index] = newValue
    }

    public actual fun exchangeAt(index: Int, newValue: T): T {
        checkBounds(index)
        val oldValue = array[index]
        array[index] = newValue
        return oldValue
    }

    public actual fun compareAndSetAt(index: Int, expectedValue: T, newValue: T): Boolean {
        checkBounds(index)
        if (array[index] != expectedValue) return false
        array[index] = newValue
        return true
    }

    public actual fun compareAndExchangeAt(index: Int, expectedValue: T, newValue: T): T {
        checkBounds(index)
        val oldValue = array[index]
        if (oldValue == expectedValue) {
            array[index] = newValue
        }
        return oldValue
    }

    public actual override fun toString(): String = array.toString()

    private fun checkBounds(index: Int) {
        if (index < 0 || index >= array.size) throw IndexOutOfBoundsException("index $index")
    }
}
