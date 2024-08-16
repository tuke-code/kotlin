/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class UpgradeCallableReferences(val context: BackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transform(UpgradeReflectionReferencesVisitor(context), UpgradeReflectionReferencesVisitorData(irFile, false))
    }
}


private class UpgradeReflectionReferencesVisitorData(
    val parent: IrDeclarationParent,
    val isVolatileLambda: Boolean
) {
    fun setIsVolatileLambda(newValue: Boolean) = if (newValue == isVolatileLambda) this else
        UpgradeReflectionReferencesVisitorData(parent, newValue)
}

var IrFile.dumpBeforeUpgrade by irAttribute<IrFile, String>(followAttributeOwner = false)

private class UpgradeReflectionReferencesVisitor(val context: BackendContext) : IrElementTransformer<UpgradeReflectionReferencesVisitorData> {
    private val VOLATILE_LAMBDA_FQ_NAME = FqName.fromSegments(listOf("kotlin", "native", "internal", "VolatileLambda"))

    override fun visitFile(declaration: IrFile, data: UpgradeReflectionReferencesVisitorData): IrFile {
        declaration.dumpBeforeUpgrade = declaration.dump()
        return super.visitFile(declaration, data)
    }

    override fun visitElement(element: IrElement, data: UpgradeReflectionReferencesVisitorData): IrElement {
        if (element is IrDeclarationParent) {
            element.transformChildren(this, UpgradeReflectionReferencesVisitorData(element, false))
        } else {
            element.transformChildren(this, data.setIsVolatileLambda(false))
        }
        return element
    }

    override fun visitBlock(expression: IrBlock, data: UpgradeReflectionReferencesVisitorData): IrExpression {
        val origin = expression.origin
        if (
            origin != IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE &&
            origin != IrStatementOrigin.SUSPEND_CONVERSION &&
            origin != IrStatementOrigin.LAMBDA
        ) {
            return super.visitBlock(expression, data)
        }
        require(expression.statements.size == 2)
        val function = expression.statements[0] as IrSimpleFunction
        val (reference, samConversion) = when (val ref = expression.statements[1]) {
            is IrTypeOperatorCall -> {
                require(ref.operator == IrTypeOperator.SAM_CONVERSION)
                ref.argument as IrFunctionReference to ref.typeOperand
            }
            is IrFunctionReference -> ref to null
            else -> shouldNotBeCalled()
        }
        function.transformChildren(this, data)
        reference.transformChildren(this, data)
        require(function.contextReceiverParametersCount == 0)
        require(function.dispatchReceiverParameter == null)
        function.extensionReceiverParameter?.let {
            function.valueParameters = listOf(it) + function.valueParameters
            function.extensionReceiverParameter = null
        }
        return IrAdaptedFunctionReferenceImpl(
            expression.startOffset,
            expression.endOffset,
            reference.type,
            reference.reflectionTarget ?: function.symbol, // TODO: this is strange, but can happen on suspend conversion
            IrFunctionExpressionImpl(function.startOffset, function.endOffset, expression.type, function, origin),
            samConversion,
            origin
        ).apply {
            copyAttributes(expression)
            reference.extensionReceiver?.let {
                capturedValues.add(it)
            }
        }
    }

    override fun visitDeclaration(declaration: IrDeclarationBase, data: UpgradeReflectionReferencesVisitorData): IrStatement {
        return visitElement(declaration, data) as IrStatement
    }

    override fun visitCall(expression: IrCall, data: UpgradeReflectionReferencesVisitorData): IrElement {
        expression.dispatchReceiver = expression.dispatchReceiver?.transform(this, data.setIsVolatileLambda(false))
        expression.extensionReceiver = expression.extensionReceiver?.transform(this, data.setIsVolatileLambda(false))
        for (i in 0 until expression.valueArgumentsCount) {
            expression.putValueArgument(i, expression.getValueArgument(i)?.transform(
                this,
                data.setIsVolatileLambda(expression.symbol.owner.valueParameters[i].hasAnnotation(VOLATILE_LAMBDA_FQ_NAME))
            ))
        }
        return expression
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: UpgradeReflectionReferencesVisitorData): IrExpression {
        if (expression.operator == IrTypeOperator.SAM_CONVERSION) {
            expression.transformChildren(this, data)
            val argument = expression.argument
            if (argument is IrAdaptedFunctionReference) {
                argument.samConversion = expression.typeOperand
                return argument
            }
        }
        return super.visitTypeOperator(expression, data)
    }

    override fun visitFunctionReference(expression: IrFunctionReference, data: UpgradeReflectionReferencesVisitorData): IrExpression {
        expression.transformChildren(this, data)
        val arguments = expression.getArgumentsWithIr().map { it.second }
        if (data.isVolatileLambda) {
            require(arguments.isEmpty()) { "@VolatileLambda argument should have no bound arguments" }
            return IrRawFunctionReferenceImpl(
                expression.startOffset, expression.endOffset, expression.type,
                expression.symbol
            )
        }
        return IrAdaptedFunctionReferenceImpl(
            expression.startOffset,
            expression.endOffset,
            expression.type,
            expression.reflectionTarget ?: expression.symbol,
            expression.wrapFunction(arguments, data.parent, expression.symbol.owner),
            null,
            expression.origin,
        ).apply {
            copyAttributes(expression)
            capturedValues += arguments
        }
    }

    override fun visitPropertyReference(expression: IrPropertyReference, data: UpgradeReflectionReferencesVisitorData): IrExpression {
        val getter = expression.getter?.owner
        val arguments = expression.getArgumentsWithIr().map { it.second }
        return if (getter != null) {
            IrAdaptedPropertyReferenceImpl(
                expression.startOffset,
                expression.endOffset,
                expression.type,
                expression.symbol,
                expression.wrapFunction(arguments, data.parent, getter),
                expression.setter?.let { expression.wrapFunction(arguments, data.parent, it.owner, isPropertySetter = true) },
                expression.origin,
            )
        } else {
            val field = expression.field!!.owner
            IrAdaptedPropertyReferenceImpl(
                expression.startOffset,
                expression.endOffset,
                expression.type,
                expression.symbol,
                expression.wrapField(arguments, data.parent, field, isSetter = false),
                if (expression.type.isKMutableProperty()) expression.wrapField(arguments,data.parent, field, isSetter = true) else null,
                expression.origin
            )
        }.apply {
            copyAttributes(expression)
            capturedValues += arguments
        }
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference, data: UpgradeReflectionReferencesVisitorData): IrExpression {
        return IrAdaptedPropertyReferenceImpl(
            expression.startOffset,
            expression.endOffset,
            expression.type,
            expression.symbol,
            expression.wrapFunction(emptyList(), data.parent, expression.getter.owner),
            expression.setter?.let { expression.wrapFunction(emptyList(), data.parent, it.owner, isPropertySetter = true) },
            expression.origin
        )
    }

    private fun IrCallableReference<*>.buildWrapperFunExpression(
        captured: List<IrExpression>,
        parent: IrDeclarationParent,
        name: Name,
        returnType: IrType,
        isSuspend: Boolean,
        isPropertySetter: Boolean,
        body: IrBlockBodyBuilder.(List<IrValueParameter>) -> Unit,
    ) : IrFunctionExpression {
        val func = context.irFactory.buildFun {
            setSourceRange(this@buildWrapperFunExpression)
            origin = LoweredDeclarationOrigins.INLINE_LAMBDA
            this.name = name
            visibility = DescriptorVisibilities.LOCAL
            this.returnType = returnType
            this.isSuspend = isSuspend
        }.apply {
            this.parent = parent
            var index = 0
            for (arg in captured) {
                addValueParameter {
                    this.name = Name.identifier("p${index++}")
                    this.type = arg.type
                }
            }
            for (type in (this@buildWrapperFunExpression.type as IrSimpleType).arguments.dropLast(1)) {
                addValueParameter {
                    this.name = Name.identifier("p${index++}")
                    this.type = type.typeOrNull ?: context.irBuiltIns.anyNType
                }
            }
            if (isPropertySetter) {
                addValueParameter {
                    this.name = Name.identifier("value")
                    this.type = this@buildWrapperFunExpression.returnType
                }
            }
            this.body = context.createIrBuilder(symbol).run {
                irBlockBody {
                    body(valueParameters)
                }
            }
        }
        return IrFunctionExpressionImpl(startOffset, endOffset, type, func, IrStatementOrigin.LAMBDA)
    }

    private fun IrPropertyReference.wrapField(captured: List<IrExpression>, parent: IrDeclarationParent, field: IrField, isSetter: Boolean): IrFunctionExpression {
        return buildWrapperFunExpression(
            captured,
            parent,
            name = Name.special("<${if (isSetter) "set-" else "get-"}${symbol.owner.name}>"),
            returnType = field.type,
            isSuspend = false,
            isPropertySetter = isSetter
        ) { params ->
            val fieldReceiver = when {
                field.isStatic -> null
                else -> irGet(params[0])
            }
            val exprToReturn = if (isSetter) {
                irSetField(fieldReceiver, field, irGet(params[1]))
            } else {
                irGetField(fieldReceiver, field)
            }
            +irReturn(exprToReturn)
        }
    }

    private val IrCallableReference<*>.returnType
        get() = (type as IrSimpleType).arguments.last().typeOrNull ?: context.irBuiltIns.anyNType


    private fun IrCallableReference<*>.wrapFunction(captured: List<IrExpression>, parent: IrDeclarationParent, referencedFunction: IrFunction, isPropertySetter: Boolean = false): IrFunctionExpression {
        val functionReturnType = if (isPropertySetter) context.irBuiltIns.unitType else returnType
        return buildWrapperFunExpression(
            captured,
            parent,
            referencedFunction.name,
            functionReturnType,
            referencedFunction.isSuspend,
            isPropertySetter
        ) { parameters ->
            val exprToReturn = irCall(referencedFunction.symbol, functionReturnType).apply {
                copyTypeArgumentsFrom(this@wrapFunction)
                for ((parameter, localParameter) in referencedFunction.explicitParameters.zip(parameters)) {
                    putArgument(referencedFunction, parameter, irGet(localParameter))
                }
            }
            +irReturn(exprToReturn)
        }
    }
}