/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import java.lang.ref.WeakReference
import java.util.function.Function
import kotlin.reflect.KProperty

/**
 * A key for storing additional data inside [IrElement].
 *
 * Example usage:
 * ```kotlin
 * val BinaryName by createIrDynamicProperty<IrFunction, String>()
 *
 * fun computeBinaryName(function: IrFunction) {
 *     function[BinaryName] = function.findBinaryNameAnnotation() ?: function.name.mangle()
 * }
 * ```
 *
 * ##### Migration note:
 * This class implements [MutableMap] for the purpose of easing migration from maps in the form
 * of MutableMap<IrElement, T> to dynamic properties.
 * It means that you can replace such map with IrDynamicPropertyKey<IrElement, T> in-place, and
 * all the usual map-based syntax will continue to work.
 *
 * E.g. for `val binaryNames = mutableMapOf<IrDeclaration, String>()` you can replace it
 * with `val binaryNames by createIrDynamicProperty<IrDeclaration, String>()`.
 * `binaryNames[element]` will then behave the same way as `element[binaryNames]`.
 * Similarly, `MutableSet<IrElement>` can be replaced with `irDynamicFlag<IrElement>()`
 *
 * However, a proper migration to the latter syntax is eventually expected, and this functionality
 * is to be removed.
 * Note that collection operations, like iterating over all entries of the map, are not supported
 * and will throw at runtime. If there is a need to use them, such a map should not be converted to
 * dynamic property.
 *
 * @param E restricts the type of [IrElement] on which this property can be stored.
 * @param T the type of the data stored in the property. Nullable types are allowed,
 * but note that null values are not distinguishable from an absence of the property.
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class IrDynamicPropertyKey<E : IrElement, T> internal constructor() : AbstractMutableMap<E, T>() {
    var name: String? = null
        private set

    /**
     * Used solely for debug, to help distinguish between multiple instances of dynamic property keys.
     * This may happen if the key is defined inside some class, instead of on top level.
     */
    var owner: WeakReference<Any>? = null
        private set

    operator fun getValue(thisRef: Any?, property: KProperty<*>): IrDynamicPropertyKey<E, T> {
        name = property.name
        owner = thisRef?.let(::WeakReference)
        return this
    }

    override fun toString(): String {
        return when {
            name != null && owner != null -> "$name (inside of $owner)"
            name != null -> name!!
            else -> "Unnamed ${super.toString()}"
        }
    }


    // The following Map functions have a deprecation annotations meant for a help with migration.
    // When locally uncommented, IJ will suggest a refactoring quickly changing map-based syntax to a new one.
    // They cannot be uncommented in general, because it would cause deprecation warnings, which are treated as errors.

    //@Deprecated("Old-school syntax", ReplaceWith("element[this]", "org.jetbrains.kotlin.ir.get"))
    override operator fun get(element: E): T? {
        return element[this]
    }

    //@Deprecated("Old-school syntax", ReplaceWith("element[this] != null", "org.jetbrains.kotlin.ir.get"))
    override fun containsKey(element: E): Boolean {
        return element[this] != null
    }

    //@Deprecated("Old-school syntax", ReplaceWith("element.set(this, value)", "org.jetbrains.kotlin.ir.set"))
    override fun put(element: E, value: T): T? {
        return element.set(this, value)
    }

    //@Deprecated("Old-school syntax", ReplaceWith("element.set(this, null)", "org.jetbrains.kotlin.ir.set"))
    override fun remove(element: E): T? {
        return element.set(this, null)
    }

    //@Deprecated("Old-school syntax", ReplaceWith("element.getOrPutDynamicProperty(this, compute)", "org.jetbrains.kotlin.ir.getOrPutDynamicProperty"))
    fun getOrPut(element: E, compute: () -> T): T {
        return element.getOrPutDynamicProperty(this, compute)
    }

    //@Deprecated("Old-school syntax", ReplaceWith("element.getOrPutDynamicProperty(this, compute)", "org.jetbrains.kotlin.ir.getOrPutDynamicProperty"))
    fun computeIfAbsent(element: E, compute: (E) -> T): T {
        return element.getOrPutDynamicProperty(this) { compute(element) }
    }

    override fun computeIfAbsent(element: E, mappingFunction: Function<in E, out T>): T {
        return element.getOrPutDynamicProperty(this) { mappingFunction.apply(element) }
    }


    override val keys: MutableSet<E> = KeyCollection()

    override val entries: MutableSet<MutableMap.MutableEntry<E, T>> get() = unsupportedMapOperation()

    override val size: Int get() = unsupportedMapOperation()

    override fun clear() = unsupportedMapOperation()


    private inner class KeyCollection : AbstractMutableSet<E>() {
        override fun contains(element: E): Boolean {
            return element[this@IrDynamicPropertyKey] != null
        }

        override val size: Int
            get() = throw UnsupportedOperationException()

        override fun add(element: E): Boolean {
            throw UnsupportedOperationException()
        }

        override fun iterator(): MutableIterator<E> {
            throw UnsupportedOperationException()
        }
    }

    // A helper for migration from `MutableSet<IrElement>`, the same way as `IrDynamicPropertyKey`
    // supports migration from `MutableMap<IrElement, T>`.
    class Flag<E : IrElement> internal constructor(
        private val key: IrDynamicPropertyKey<E, Boolean>,
    ) : AbstractMutableSet<E>() {
        override fun contains(element: E): Boolean {
            return element[key] == true
        }

        override fun add(element: E): Boolean {
            return element.set(key, true) != true
        }

        override fun remove(element: E): Boolean {
            return element.set(key, null) == true
        }


        override val size: Int get() = unsupportedMapOperation()

        override fun iterator(): MutableIterator<E> = unsupportedMapOperation()

        override fun clear() = unsupportedMapOperation()
    }

    companion object {
        private fun unsupportedMapOperation(): Nothing =
            throw UnsupportedOperationException("This map-based operation is unsupported by IR dynamic property")
    }
}