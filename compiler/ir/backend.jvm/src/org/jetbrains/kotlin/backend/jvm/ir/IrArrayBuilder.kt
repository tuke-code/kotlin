/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.ir

import org.jetbrains.kotlin.backend.jvm.unboxInlineClass
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.getArrayElementType
import org.jetbrains.kotlin.ir.util.isBoxedArray

inline fun JvmIrBuilder.irArray(arrayType: IrType, block: IrArrayBuilder.() -> Unit): IrExpression =
    IrArrayBuilder(this, arrayType).apply { block() }.build()

fun JvmIrBuilder.irArrayOf(arrayType: IrType, elements: List<IrExpression> = listOf()): IrExpression =
    irArray(arrayType) { elements.forEach { +it } }

private class IrArrayElement(val expression: IrExpression, val isSpread: Boolean)

class IrArrayBuilder(val builder: JvmIrBuilder, val arrayType: IrType) {
    // We build unboxed arrays for inline classes (UIntArray, etc) by first building
    // an unboxed array of the underlying primitive type, then coercing the result
    // to the correct type.
    val unwrappedArrayType = arrayType.unboxInlineClass()

    // Check if the array type is an inline class wrapper (UIntArray, etc.)
    val isUnboxedInlineClassArray
        get() = arrayType !== unwrappedArrayType

    // The unwrapped element type
    val elementType = unwrappedArrayType.getArrayElementType(builder.context.irBuiltIns)

    private val elements: MutableList<IrArrayElement> = mutableListOf()

    private val hasSpread
        get() = elements.any { it.isSpread }

    operator fun IrExpression.unaryPlus() = add(this)
    fun add(expression: IrExpression) = elements.add(IrArrayElement(expression, false))

    fun addSpread(expression: IrExpression) = elements.add(IrArrayElement(expression, true))

    fun build(): IrExpression {
        val array = when {
            elements.isEmpty() -> newArray(0)
            !hasSpread -> buildSimpleArray()
            elements.size == 1 -> copyArray(elements.single().expression)
            else -> buildComplexArray()
        }
        return coerce(array, arrayType)
    }

    // Construct a new array of the specified size
    private fun newArray(size: Int) = newArray(builder.irInt(size))

    private fun newArray(size: IrExpression): IrExpression {
        val arrayConstructor = if (unwrappedArrayType.isBoxedArray)
            builder.irSymbols.arrayOfNulls
        else
            unwrappedArrayType.classOrNull!!.constructors.single { it.owner.hasShape(regularParameters = 1) }

        return builder.irCall(arrayConstructor, unwrappedArrayType).apply {
            if (typeArguments.size >= 1) {
                typeArguments[0] = elementType
            }
            arguments[0] = size
        }
    }

    // Build an array without spreads
    private fun buildSimpleArray(): IrExpression =
        builder.irBlock {
            val result = irTemporary(newArray(elements.size))

            val set = unwrappedArrayType.classOrNull!!.functions.single {
                it.owner.name.asString() == "set"
            }

            for ((index, element) in elements.withIndex()) {
                +irCall(set).apply {
                    arguments[0] = irGet(result)
                    arguments[1] = irInt(index)
                    arguments[2] = coerce(element.expression, elementType)
                }
            }

            +irGet(result)
        }

    // Copy a single spread expression, unless it refers to a newly constructed array.
    private fun copyArray(spread: IrExpression): IrExpression {
        if (spread is IrConstructorCall ||
            (spread is IrFunctionAccessExpression && spread.symbol == builder.irSymbols.arrayOfNulls))
            return spread

        return builder.irBlock {
            val spreadVar = if (spread is IrGetValue) spread.symbol.owner else irTemporary(spread)
            val size = unwrappedArrayType.classOrNull!!.getPropertyGetter("size")!!
            val arrayCopyOf = builder.irSymbols.getArraysCopyOfFunction(unwrappedArrayType as IrSimpleType)
            // TODO consider using System.arraycopy if the requested array type is non-generic.
            +irCall(arrayCopyOf).apply {
                arguments[0] = coerce(irGet(spreadVar), unwrappedArrayType)
                arguments[1] = irCall(size).apply { dispatchReceiver = irGet(spreadVar) }
            }
        }
    }

    // Build an array containing spread expressions.
    private fun buildComplexArray(): IrExpression {
        val spreadBuilder = if (unwrappedArrayType.isBoxedArray)
            builder.irSymbols.spreadBuilder
        else
            builder.irSymbols.primitiveSpreadBuilders.getValue(elementType)

        val addElement = spreadBuilder.functions.single { it.owner.name.asString() == "add" }
        val addSpread = spreadBuilder.functions.single { it.owner.name.asString() == "addSpread" }
        val toArray = spreadBuilder.functions.single { it.owner.name.asString() == "toArray" }

        return builder.irBlock {
            val spreadBuilderVar = irTemporary(irCallConstructor(spreadBuilder.constructors.single(), listOf()).apply {
                arguments[0] = irInt(elements.size)
            })

            for (element in elements) {
                +irCall(if (element.isSpread) addSpread else addElement).apply {
                    arguments[0] = irGet(spreadBuilderVar)
                    arguments[1] = coerce(element.expression, if (element.isSpread) unwrappedArrayType else elementType)
                }
            }

            val toArrayCall = irCall(toArray).apply {
                arguments[0] = irGet(spreadBuilderVar)
                if (unwrappedArrayType.isBoxedArray) {
                    val size = spreadBuilder.functions.single { it.owner.name.asString() == "size" }
                    arguments[1] = irCall(builder.irSymbols.arrayOfNulls, arrayType).apply {
                        typeArguments[0] = elementType
                        arguments[0] = irCall(size).apply {
                            arguments[0] = irGet(spreadBuilderVar)
                        }
                    }
                }
            }

            if (unwrappedArrayType.isBoxedArray)
                +builder.irImplicitCast(toArrayCall, unwrappedArrayType)
            else
                +toArrayCall
        }
    }

    // Coerce expression to irType if we are working with an inline class array type
    private fun coerce(expression: IrExpression, irType: IrType): IrExpression =
        if (isUnboxedInlineClassArray)
            builder.irCall(builder.irSymbols.unsafeCoerceIntrinsic, irType).apply {
                typeArguments[0] = expression.type
                typeArguments[1] = irType
                arguments[0] = expression
            }
        else expression
}
