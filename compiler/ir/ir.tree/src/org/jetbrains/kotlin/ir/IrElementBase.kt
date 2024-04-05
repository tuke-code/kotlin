/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class IrElementBase : IrElement {
    private var dynamicPropertyMap: Array<Any?>? = null


    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrElement =
        accept(transformer, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        // No children by default
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        // No children by default
    }


    /**
     * Returns a snapshot of all dynamic properties held by this element.
     * Designated mainly for debugging.
     */
    val allDynamicProperties: Map<IrDynamicPropertyKey<*, *>, Any>
        get() {
            val dynamicPropertyMap = dynamicPropertyMap
                ?: return emptyMap()
            return buildMap(dynamicPropertyMap.size / 2) {
                var i = 0
                while (i < dynamicPropertyMap.size) {
                    val key = dynamicPropertyMap[i] as IrDynamicPropertyKey<*, *>?
                    if (key != null) {
                        val value = dynamicPropertyMap[i + 1]!!
                        put(key, value)
                    }

                    i += 2
                }
            }
        }

    internal open fun <T> getDynamicPropertyInternal(key: IrDynamicPropertyKey<*, T>): T? {
        val foundIndex = findDynamicPropertyIndex(key)
        if (foundIndex < 0) {
            return null
        } else {
            @Suppress("UNCHECKED_CAST")
            return dynamicPropertyMap!![foundIndex + 1] as T
        }
    }

    internal fun <T> setDynamicPropertyInternal(key: IrDynamicPropertyKey<*, T>, value: T?): T? {
        val foundIndex = findDynamicPropertyIndex(key)
        val previousValue: T? = if (foundIndex >= 0) {
            @Suppress("UNCHECKED_CAST")
            dynamicPropertyMap!![foundIndex + 1] as T
        } else null

        putDynamicProperty(foundIndex, key, value)
        return previousValue
    }

    internal fun <T> getOrPutDynamicPropertyInternal(key: IrDynamicPropertyKey<*, T>, compute: () -> T): T {
        val foundIndex = findDynamicPropertyIndex(key)
        if (foundIndex >= 0) {
            @Suppress("UNCHECKED_CAST")
            return dynamicPropertyMap!![foundIndex + 1] as T
        }

        val newValue = compute()
        putDynamicProperty(foundIndex, key, newValue)
        return newValue
    }

    private fun findDynamicPropertyIndex(key: IrDynamicPropertyKey<*, *>): Int {
        val dynamicPropertyMap = dynamicPropertyMap
            ?: return -1

        var i = 0
        while (i < dynamicPropertyMap.size) {
            val foundKey = dynamicPropertyMap[i]
                ?: break
            if (foundKey === key) {
                return i
            }

            i += 2
        }
        return -i - 1
    }

    private fun <T> initializeDynamicProperties(firstKey: IrDynamicPropertyKey<*, T>, firstValue: T?) {
        val initialSlots = 1
        val dynamicPropertyMap = arrayOfNulls<Any?>(initialSlots * 2)
        dynamicPropertyMap[0] = firstKey
        dynamicPropertyMap[1] = firstValue
        this.dynamicPropertyMap = dynamicPropertyMap
    }

    private fun <T> putDynamicProperty(existingIndex: Int, key: IrDynamicPropertyKey<*, T>, value: T?) {
        if (existingIndex >= 0) {
            if (value == null) {
                removeDynamicPropertyAt(existingIndex)
            } else {
                dynamicPropertyMap!![existingIndex + 1] = value
            }
        } else if (value != null) {
            if (dynamicPropertyMap == null) {
                initializeDynamicProperties(key, value)
            } else {
                val newEntryIndex = -(existingIndex + 1)
                addDynamicPropertyAt(newEntryIndex, key, value)
            }
        }
    }

    private fun <T> addDynamicPropertyAt(index: Int, key: IrDynamicPropertyKey<*, T>, value: T?) {
        var dynamicPropertyMap = dynamicPropertyMap!!
        if (dynamicPropertyMap.size <= index) {
            val newSlots = 2
            dynamicPropertyMap = dynamicPropertyMap.copyOf(dynamicPropertyMap.size + newSlots * 2)
            this.dynamicPropertyMap = dynamicPropertyMap
        }

        dynamicPropertyMap[index] = key
        dynamicPropertyMap[index + 1] = value
    }

    private fun removeDynamicPropertyAt(keyIndex: Int) {
        val dynamicPropertyMap = dynamicPropertyMap!!
        val lastKeyIndex = dynamicPropertyMap.size - 2
        if (lastKeyIndex > keyIndex) {
            dynamicPropertyMap[keyIndex] = dynamicPropertyMap[lastKeyIndex]
            dynamicPropertyMap[keyIndex + 1] = dynamicPropertyMap[lastKeyIndex + 1]
        }
        dynamicPropertyMap[lastKeyIndex] = null
        dynamicPropertyMap[lastKeyIndex + 1] = null
    }
}
