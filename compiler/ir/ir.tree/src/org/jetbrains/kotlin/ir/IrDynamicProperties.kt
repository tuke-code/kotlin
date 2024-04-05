/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

/**
 * Creates new [IrDynamicPropertyKey] which can be used to store additional data
 * of type [T] inside of [E].
 *
 * See [IrDynamicPropertyKey] for details.
 */
fun <E : IrElement, T> irDynamicProperty(): IrDynamicPropertyKey<E, T> = IrDynamicPropertyKey()

/**
 * Creates new [IrDynamicPropertyKey] which can be used to put an additional mark
 * on an element of type [E].
 *
 * This is similar to using `irDynamicProperty<E, Boolean>()`, except:
 * - Boolean property has 3 states: `false`, `true` and `null`,
 * while flag has 2: set or not set.
 * - It is possible to store a flag a bit more efficiently,
 * by not allocating a slot for a value - not implemented yet.
 *
 * See [IrDynamicPropertyKey] for details.
 */
fun <E : IrElement> irDynamicFlag(): IrDynamicPropertyKey.Flag<E> =
    IrDynamicPropertyKey.Flag<E>(IrDynamicPropertyKey<E, Boolean>())

/**
 * Returns a value of property [key], or null if the value is either null or missing.
 * There is no way to distinguish between those two cases.
 */
operator fun <E : IrElement, T> E.get(key: IrDynamicPropertyKey<E, T>): T? {
    return (this as IrElementBase).getDynamicPropertyInternal(key)
}

/**
 * Stores a [value] associated with [key] in this IrElement, or removes an association if [value] is null.
 *
 * @return The previous value associated with the key, or null if the key was not present.
 */
operator fun <E : IrElement, T> E.set(key: IrDynamicPropertyKey<E, T>, value: T?): T? {
    return (this as IrElementBase).setDynamicPropertyInternal(key, value)
}

/**
 * Returns the value associated with [key] if the value is present.
 * Otherwise, calls the [compute] function,
 * stores the result under the given key and returns it.
 */
fun <E : IrElement, T> E.getOrPutDynamicProperty(key: IrDynamicPropertyKey<E, T>, compute: () -> T): T {
    return (this as IrElementBase).getOrPutDynamicPropertyInternal(key, compute)
}