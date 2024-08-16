/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irFunctionReference
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.utils.zipIfSizesAreEqual

class DowngradeCallableReferences(val context: BackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transform(DowngradeReflectionReferencesVisitor(context), null)
    }
}

private class DowngradeReflectionReferencesVisitor(val context: BackendContext) : IrBuildingTransformer(context) {
    override fun visitFile(declaration: IrFile): IrFile {
        return super.visitFile(declaration).also {
            if (it.dumpBeforeUpgrade != null && it.dumpBeforeUpgrade != it.dump()) {
                println("Dump changed for ${it.fileEntry.name}:")
                println("================================================")
                println("Before:")
                println(it.dumpBeforeUpgrade)
                println("After:")
                println(it.dump())
                println("================================================")
            }
        }
    }

    fun IrFunctionReference.samIfNeeded(expectedType: IrType?): IrExpression {
        if (expectedType == null) return this
        return IrTypeOperatorCallImpl(
            startOffset, endOffset, expectedType, IrTypeOperator.SAM_CONVERSION, expectedType, this
        )
    }

    override fun visitAdaptedFunctionReference(expression: IrAdaptedFunctionReference): IrExpression {
        expression.transformChildrenVoid()
        require(expression.capturedValues.size <= 1) { "Multiple captured values are not supported yet" }
        if (expression.invokeFunction.function.isTrivialWrapperFor(expression.originalFunction.owner)) {
            val call = (expression.invokeFunction.function.body!!.statements.single() as IrReturn).value as IrFunctionAccessExpression
            return IrFunctionReferenceImpl.fromSymbolOwner(
                startOffset = expression.startOffset,
                endOffset = expression.endOffset,
                type = expression.type,
                symbol = expression.originalFunction,
                typeArgumentsCount = call.typeArgumentsCount,
                reflectionTarget = expression.originalFunction,
                origin = expression.origin
            ).apply {
                copyTypeArgumentsFrom(call)
                if (call.dispatchReceiver != null) dispatchReceiver = expression.capturedValues.singleOrNull()
                if (call.extensionReceiver != null) extensionReceiver = expression.capturedValues.singleOrNull()
            }.samIfNeeded(expression.samConversion)
        } else {
            return builder.irBlock(origin = expression.origin) {
                +expression.invokeFunction.function.apply {
                    isInline = false
                    if (expression.capturedValues.isNotEmpty()) {
                        extensionReceiverParameter = valueParameters.first()
                        valueParameters = valueParameters.drop(1)
                    }
                }
                +irFunctionReference(expression.type, expression.invokeFunction.function.symbol).apply {
                    origin = expression.origin
                    if (expression.capturedValues.isNotEmpty()) {
                        extensionReceiver = expression.capturedValues.single()
                    }
                    reflectionTarget = expression.originalFunction
                }.samIfNeeded(expression.samConversion)
            }
        }
    }

    // TODO: this is in fact not very correct, e.g. for references to inline properties
    // but we don't have any way of representing adapted properties in legacy format anyway
    override fun visitAdaptedPropertyReference(expression: IrAdaptedPropertyReference): IrExpression {
        super.visitAdaptedPropertyReference(expression)
        return when (val original = expression.originalProperty) {
            is IrPropertySymbol -> {
                require(original.owner.getter?.let { expression.getterFunction.function.isTrivialWrapperFor(it) } != false)
                require(original.owner.setter?.let { expression.setterFunction?.function?.isTrivialWrapperFor(it) } != false)
                val call = (expression.getterFunction.function.body!!.statements.single() as IrReturn).value as IrFunctionAccessExpression
                IrPropertyReferenceImpl(
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset,
                    type = expression.type,
                    symbol = original,
                    typeArgumentsCount = original.owner.getter?.typeParameters?.size ?: 0,
                    field = original.owner.backingField?.symbol,
                    getter = original.owner.getter?.symbol,
                    setter = original.owner.setter?.symbol,
                    origin = expression.origin,
                ).apply {
                    copyTypeArgumentsFrom(call)
                    if (call.dispatchReceiver != null) dispatchReceiver = expression.capturedValues.singleOrNull()
                    if (call.extensionReceiver != null) extensionReceiver = expression.capturedValues.singleOrNull()
                }
            }
            is IrLocalDelegatedPropertySymbol -> {
                require(original.owner.getter.let { expression.getterFunction.function.isTrivialWrapperFor(it) } != false)
                require(original.owner.setter?.let { expression.setterFunction?.function?.isTrivialWrapperFor(it) } != false)
                IrLocalDelegatedPropertyReferenceImpl(
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset,
                    type = expression.type,
                    symbol = original,
                    delegate = original.owner.delegate.symbol,
                    getter = original.owner.getter.symbol,
                    setter = original.owner.setter?.symbol,
                    origin = expression.origin
                )
            }
        }
    }

    private fun IrFunction.isTrivialWrapperFor(function: IrFunction) : Boolean {
        val returnStatement = body?.statements?.singleOrNull() as? IrReturn ?: return false
        val returnValue = returnStatement.value as? IrFunctionAccessExpression ?: return false
        if (isSuspend != function.isSuspend) return false
        if (explicitParameters.size != function.explicitParameters.size) return false
        if (returnValue.symbol != function.symbol) return false
        if (returnValue !is IrCall && returnValue !is IrConstructorCall) return false
        val forward = returnValue.getArgumentsWithIr().map { it.second }.zipIfSizesAreEqual(explicitParameters) ?: return false
        return forward.all { (first, second) -> first is IrGetValue && first.symbol == second.symbol }
    }
}
