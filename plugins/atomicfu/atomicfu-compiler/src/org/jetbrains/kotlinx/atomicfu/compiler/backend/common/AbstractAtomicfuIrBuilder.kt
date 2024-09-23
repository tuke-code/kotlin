/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.common

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.atomicfu.compiler.backend.AtomicHandlerType
import org.jetbrains.kotlinx.atomicfu.compiler.backend.atomicfuRender
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuTransformer.Companion.VOLATILE
import org.jetbrains.kotlinx.atomicfu.compiler.backend.getArraySizeArgument
import org.jetbrains.kotlinx.atomicfu.compiler.backend.getAtomicFactoryValueArgument
import org.jetbrains.kotlinx.atomicfu.compiler.diagnostic.AtomicfuErrorMessages.CONSTRAINTS_MESSAGE

abstract class AbstractAtomicfuIrBuilder(
    protected val irBuiltIns: IrBuiltIns,
    symbol: IrSymbol,
    startOffset: Int,
    endOffset: Int
) : IrBuilderWithScope(IrGeneratorContextBase(irBuiltIns), Scope(symbol), startOffset, endOffset) {

    abstract val atomicfuSymbols: AbstractAtomicSymbols

    abstract fun irCallFunction (
        symbol: IrSimpleFunctionSymbol,
        dispatchReceiver: IrExpression?,
        extensionReceiver: IrExpression?,
        valueArguments: List<IrExpression?>,
        valueType: IrType
    ): IrCall

    protected fun invokeFunctionOnAtomicHandlerClass(
        getAtomicHandler: IrExpression,
        functionName: String,
        valueArguments: List<IrExpression?>,
        valueType: IrType,
    ): IrCall {
        val atomicHandlerClassSymbol = (getAtomicHandler.type as IrSimpleType).classOrNull
            ?: error("Failed to obtain the ClassSymbol of the type ${getAtomicHandler.render()}.")
        val functionSymbol = when (functionName) {
            "get", "<get-value>", "getValue" -> atomicHandlerClassSymbol.getSimpleFunction("get")
            "set", "<set-value>", "setValue", "lazySet" -> atomicHandlerClassSymbol.getSimpleFunction("set")
            else -> atomicHandlerClassSymbol.getSimpleFunction(functionName)
        } ?: error("No $functionName function found in ${atomicHandlerClassSymbol.owner.render()}")
        return irCallFunction(
            functionSymbol,
            dispatchReceiver = getAtomicHandler,
            extensionReceiver = null,
            valueArguments,
            valueType
        )
    }

    abstract fun invokeFunctionOnAtomicHandler(
        atomicHandlerType: AtomicHandlerType,
        getAtomicHandler: IrExpression,
        functionName: String,
        valueArguments: List<IrExpression?>,
        valueType: IrType,
    ): IrCall

    fun irGetProperty(property: IrProperty, dispatchReceiver: IrExpression?) =
        irCall(property.getter?.symbol ?: error("Getter is not defined for the property ${property.render()}")).apply {
            this.dispatchReceiver = dispatchReceiver?.deepCopyWithSymbols()
        }

    protected fun irVolatileField(
        name: String,
        valueType: IrType,
        annotations: List<IrConstructorCall>,
        parentContainer: IrDeclarationContainer,
    ): IrField {
        return context.irFactory.buildField {
            this.name = Name.identifier(name + VOLATILE)
            this.type = valueType
            isFinal = false
            isStatic = parentContainer is IrFile
            visibility = DescriptorVisibilities.PRIVATE
            origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FIELD
        }.apply {
            this.annotations = annotations + atomicfuSymbols.volatileAnnotationConstructorCall
            this.parent = parentContainer
        }
    }

    private fun irAtomicArrayField(
        name: Name,
        arrayClass: IrClassSymbol,
        isStatic: Boolean,
        annotations: List<IrConstructorCall>,
        size: IrExpression,
        valueType: IrType,
        dispatchReceiver: IrExpression?,
        parentContainer: IrDeclarationContainer
    ): IrField =
        context.irFactory.buildField {
            this.name = name
            type = arrayClass.defaultType
            this.isFinal = true
            this.isStatic = isStatic
            visibility = DescriptorVisibilities.PRIVATE
            origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FIELD
        }.apply {
            this.initializer = context.irFactory.createExpressionBody(
                newAtomicArray(arrayClass, size, valueType, dispatchReceiver)
            )
            this.annotations = annotations
            this.parent = parentContainer
        }

    fun buildVolatileField(
        atomicfuProperty: IrProperty,
        parentContainer: IrDeclarationContainer
    ): IrField {
        val atomicfuField = requireNotNull(atomicfuProperty.backingField) {
            "The backing field of the atomic property ${atomicfuProperty.render()} declared in ${parentContainer.render()} should not be null." + CONSTRAINTS_MESSAGE
        }
        return buildAndInitializeNewField(atomicfuField, parentContainer) { atomicFactoryCall: IrExpression ->
            val valueType = atomicfuSymbols.atomicToPrimitiveType(atomicfuField.type as IrSimpleType)
            val initValue = atomicFactoryCall.getAtomicFactoryValueArgument()
            buildVolatileFieldOfType(atomicfuProperty.name.asString(), valueType, atomicfuField.annotations, initValue, parentContainer)
        }
    }

    abstract fun buildVolatileFieldOfType(
        name: String,
        valueType: IrType,
        annotations: List<IrConstructorCall>,
        initExpr: IrExpression?,
        parentContainer: IrDeclarationContainer,
    ): IrField

    fun irAtomicArrayField(
        atomicfuProperty: IrProperty,
        parentContainer: IrDeclarationContainer
    ): IrField {
        val atomicfuArrayField = requireNotNull(atomicfuProperty.backingField) {
            "The backing field of the atomic array [${atomicfuProperty.atomicfuRender()}] should not be null." + CONSTRAINTS_MESSAGE
        }
        return buildAndInitializeNewField(atomicfuArrayField, parentContainer) { atomicFactoryCall: IrExpression ->
            val arraySize = atomicFactoryCall.getArraySizeArgument()
            irAtomicArrayField(
                atomicfuArrayField.name,
                atomicfuSymbols.getAtomicArrayHanlderType(atomicfuArrayField.type),
                atomicfuArrayField.isStatic,
                atomicfuArrayField.annotations,
                arraySize,
                atomicfuSymbols.atomicArrayToPrimitiveType(atomicfuArrayField.type),
                (atomicFactoryCall as IrFunctionAccessExpression).dispatchReceiver,
                parentContainer
            )
        }
    }

    protected fun buildAndInitializeNewField(oldAtomicField: IrField, parentContainer: IrDeclarationContainer, newFieldBuilder: (IrExpression) -> IrField): IrField {
        val initializer = oldAtomicField.initializer?.expression
        return if (initializer == null) {
            // replace field initialization in the init block
            val initBlock = oldAtomicField.getInitBlockForField(parentContainer)
            // property initialization order in the init block matters -> transformed initializer should be placed at the same position
            val initExprWithIndex = initBlock.getInitExprWithIndexFromInitBlock(oldAtomicField.symbol)
                ?: error("The atomic array ${oldAtomicField.render()} was not initialized neither at the declaration, nor in the init block." + CONSTRAINTS_MESSAGE)
            val atomicFactoryCall = initExprWithIndex.value.value
            val initExprIndex = initExprWithIndex.index
            newFieldBuilder(atomicFactoryCall).also { newField ->
                val initExpr = newField.initializer?.expression
                    ?: error("The generated field [${newField.render()}] should've already be initialized." + CONSTRAINTS_MESSAGE)
                newField.initializer = null
                initBlock.updateFieldInitialization(oldAtomicField.symbol, newField.symbol, initExpr, initExprIndex)
            }
        } else {
            newFieldBuilder(initializer)
        }
    }

    private fun IrAnonymousInitializer.getInitExprWithIndexFromInitBlock(
        oldFieldSymbol: IrFieldSymbol
    ): IndexedValue<IrSetField>? =
        body.statements.withIndex().singleOrNull { it.value is IrSetField && (it.value as IrSetField).symbol == oldFieldSymbol }?.let {
            @Suppress("UNCHECKED_CAST")
            it as IndexedValue<IrSetField>
        }

    private fun IrAnonymousInitializer.updateFieldInitialization(
        oldFieldSymbol: IrFieldSymbol,
        volatileFieldSymbol: IrFieldSymbol,
        initExpr: IrExpression,
        index: Int
    ) {
        // save the order of field initialization in init block
        body.statements.singleOrNull {
            it is IrSetField && it.symbol == oldFieldSymbol
        }?.let {
            it as IrSetField
            with(atomicfuSymbols.createBuilder(it.symbol)) {
                body.statements[index] = irSetField(it.receiver, volatileFieldSymbol.owner, initExpr)
            }
        }
    }

    private fun IrField.getInitBlockForField(parentContainer: IrDeclarationContainer): IrAnonymousInitializer {
        for (declaration in parentContainer.declarations) {
            if (declaration is IrAnonymousInitializer) {
                if (declaration.body.statements.any { it is IrSetField && it.symbol == this.symbol }) {
                    return declaration
                }
            }
        }
        error(
            "Failed to find initialization of the property [${this.correspondingPropertySymbol?.owner?.render()}] in the init block of the class [${this.parent.render()}].\n" +
                    "Please avoid complex data flow in property initialization, e.g. instead of this:\n" +
                    "```\n" +
                    "val a: AtomicInt\n" +
                    "init {\n" +
                    "  if (foo()) {\n" +
                    "    a = atomic(0)\n" +
                    "  } else { \n" +
                    "    a = atomic(1)\n" +
                    "  }\n" +
                    "}\n" +
                    "use simple direct assignment expression to initialize the property:\n" +
                    "```\n" +
                    "val a: AtomicInt\n" +
                    "init {\n" +
                    "  val initValue = if (foo()) 0 else 1\n" +
                    "  a = atomic(initValue)\n" +
                    "}\n" +
                    "```\n" + CONSTRAINTS_MESSAGE
        )
    }

    protected fun callArraySizeConstructor(
        atomicArrayClass: IrClassSymbol,
        size: IrExpression,
        dispatchReceiver: IrExpression?,
    ): IrFunctionAccessExpression? =
        atomicArrayClass.constructors.filter { it.owner.valueParameters.size == 1 && it.owner.valueParameters[0].type.isInt() }.singleOrNull()?.let { cons ->
            return irCall(cons).apply {
                putValueArgument(0, size)
                this.dispatchReceiver = dispatchReceiver
            }
        }

    protected fun callArraySizeAndInitConstructor(
        atomicArrayClass: IrClassSymbol,
        size: IrExpression,
        valueType: IrType,
        dispatchReceiver: IrExpression?,
    ): IrFunctionAccessExpression? =
        atomicArrayClass.constructors.filter { it.owner.valueParameters.size == 1 && it.owner.valueParameters[0].type.isArray() }.singleOrNull()?.let { cons ->
            return irCall(cons).apply {
                val arrayOfNulls = irCall(atomicfuSymbols.arrayOfNulls).apply {
                    putTypeArgument(0, valueType)
                    putValueArgument(0, size)
                }
                putTypeArgument(0, valueType)
                putValueArgument(0, arrayOfNulls)
                this.dispatchReceiver = dispatchReceiver
            }
        }

    abstract fun newAtomicArray(
        atomicArrayClass: IrClassSymbol,
        size: IrExpression,
        valueType: IrType,
        dispatchReceiver: IrExpression?,
    ): IrFunctionAccessExpression

    fun irPropertyReference(property: IrProperty, classReceiver: IrExpression?): IrPropertyReferenceImpl {
        val backingField = requireNotNull(property.backingField) { "Backing field of the property $property should not be null" }
        return IrPropertyReferenceImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            type = backingField.type,
            symbol = property.symbol,
            typeArgumentsCount = 0,
            field = backingField.symbol,
            getter = property.getter?.symbol,
            setter = property.setter?.symbol
        ).apply {
            dispatchReceiver = classReceiver
        }
    }

    companion object {
        // This counter is used to ensure uniqueness of functions for refGetter lambdas,
        // as several functions with the same name may be created in the same scope
        private var refGetterCounter: Int = 0
    }

    fun irVolatilePropertyRefGetter(
        irPropertyReference: IrExpression,
        propertyName: String,
        parentFunction: IrFunction
    ): IrExpression =
        IrFunctionExpressionImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            type = atomicfuSymbols.function0Type(irPropertyReference.type),
            function = irBuiltIns.irFactory.buildFun {
                name = Name.identifier("<$propertyName-getter-${refGetterCounter++}>")
                origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FUNCTION
                returnType = irPropertyReference.type
                isInline = true
                visibility = DescriptorVisibilities.LOCAL
            }.apply {
                val lambda = this
                body = irBlockBody {
                    +irReturn(
                        irPropertyReference
                    ).apply {
                        type = irBuiltIns.nothingType
                        returnTargetSymbol = lambda.symbol
                    }
                }
                this.parent = parentFunction
            },
            origin = IrStatementOrigin.LAMBDA
        )

    fun invokePropertyGetter(refGetter: IrExpression) = irCall(atomicfuSymbols.invoke0Symbol).apply { dispatchReceiver = refGetter }
    fun toBoolean(irExpr: IrExpression) = irEquals(irExpr, irInt(1)) as IrCall
    fun toInt(irExpr: IrExpression) = irIfThenElse(irBuiltIns.intType, irExpr, irInt(1), irInt(0))

    fun buildPropertyWithAccessors(
        field: IrField,
        visibility: DescriptorVisibility,
        isVar: Boolean,
        isStatic: Boolean,
        parentContainer: IrDeclarationContainer
    ) = context.irFactory.buildProperty {
        this.name = field.name
        this.visibility = visibility
        this.isVar = isVar
        origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_PROPERTY
    }.apply {
        backingField = field
        field.correspondingPropertySymbol = this.symbol
        parent = parentContainer
        addGetter(isStatic, parentContainer, irBuiltIns)
        if (isVar) {
            addSetter(isStatic, parentContainer, irBuiltIns)
        }
    }

    private fun IrProperty.addGetter(isStatic: Boolean, parentContainer: IrDeclarationContainer, irBuiltIns: IrBuiltIns) {
        val property = this
        val field = requireNotNull(backingField) { "The backing field of the property $property should not be null." }
        addGetter {
            visibility = property.visibility
            returnType = field.type
            origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_PROPERTY_ACCESSOR
        }.apply {
            dispatchReceiverParameter = if (isStatic) null else (parentContainer as? IrClass)?.thisReceiver?.deepCopyWithSymbols(this)
            body = factory.createBlockBody(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(
                    IrReturnImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        irBuiltIns.nothingType,
                        symbol,
                        IrGetFieldImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            field.symbol,
                            field.type,
                            dispatchReceiverParameter?.let {
                                IrGetValueImpl(
                                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                    it.type,
                                    it.symbol
                                )
                            }
                        )
                    )
                )
            )
        }
    }

    private fun IrProperty.addSetter(isStatic: Boolean, parentClass: IrDeclarationContainer, irBuiltIns: IrBuiltIns) {
        val property = this
        val field = requireNotNull(property.backingField) { "The backing field of the property $property should not be null." }
        this@addSetter.addSetter {
            visibility = property.visibility
            returnType = irBuiltIns.unitType
            origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_PROPERTY_ACCESSOR
        }.apply {
            dispatchReceiverParameter = if (isStatic) null else (parentClass as? IrClass)?.thisReceiver?.deepCopyWithSymbols(this)
            addValueParameter("value", field.type)
            val value = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, valueParameters[0].type, valueParameters[0].symbol)
            body = factory.createBlockBody(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(
                    IrReturnImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        irBuiltIns.unitType,
                        symbol,
                        IrSetFieldImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            field.symbol,
                            dispatchReceiverParameter?.let {
                                IrGetValueImpl(
                                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                    it.type,
                                    it.symbol
                                )
                            },
                            value,
                            irBuiltIns.unitType
                        )
                    )
                )
            )
        }
    }

    fun generateLoopBody(atomicHandlerType: AtomicHandlerType, valueType: IrType, valueParameters: List<IrValueParameter>) =
        irBlockBody {
            val atomicHandlerParam = valueParameters[0] // ATOMIC_HANDLER
            val getAtomicHandler = if (atomicHandlerType == AtomicHandlerType.NATIVE_PROPERTY_REF) invokePropertyGetter(irGet(atomicHandlerParam)) else irGet(atomicHandlerParam)
            val hasExtraArg = atomicHandlerType == AtomicHandlerType.ATOMIC_ARRAY || atomicHandlerType == AtomicHandlerType.ATOMIC_FIELD_UPDATER
            val extraArg = if (hasExtraArg) valueParameters[1] else null
            val cur = createTmpVariable(
                irExpression = invokeFunctionOnAtomicHandler(
                    atomicHandlerType = atomicHandlerType,
                    getAtomicHandler = getAtomicHandler,
                    functionName = "get",
                    valueArguments = if (extraArg != null) listOf(irGet(extraArg)) else emptyList(),
                    valueType = valueType
                ),
                nameHint = "atomicfu\$cur", false
            )
            val action = if (hasExtraArg) valueParameters[2] else valueParameters[1]
            +irWhile().apply {
                condition = irTrue()
                body = irBlock {
                    +irCall(atomicfuSymbols.invoke1Symbol).apply {
                        this.dispatchReceiver = irGet(action)
                        putValueArgument(0, irGet(cur))
                    }
                }
            }
        }

    fun generateUpdateBody(atomicHandlerType: AtomicHandlerType, valueType: IrType, valueParameters: List<IrValueParameter>, functionName: String) =
        irBlockBody {
            val atomicHandlerParam = valueParameters[0] // ATOMIC_HANDLER
            val getAtomicHandler = if (atomicHandlerType == AtomicHandlerType.NATIVE_PROPERTY_REF)
                invokePropertyGetter(irGet(atomicHandlerParam))
            else irGet(atomicHandlerParam)
            val hasExtraArg = atomicHandlerType == AtomicHandlerType.ATOMIC_ARRAY || atomicHandlerType == AtomicHandlerType.ATOMIC_FIELD_UPDATER
            val extraArg = if (hasExtraArg) valueParameters[1] else null
            val action = if (hasExtraArg) valueParameters[2] else valueParameters[1]
            +irWhile().apply {
                condition = irTrue()
                body = irBlock {
                    val cur = createTmpVariable(
                        irExpression = invokeFunctionOnAtomicHandler(
                            atomicHandlerType = atomicHandlerType,
                            getAtomicHandler = getAtomicHandler.deepCopyWithSymbols(),
                            functionName = "get",
                            valueArguments = if (extraArg != null) listOf(irGet(extraArg)) else emptyList(),
                            valueType = valueType
                        ),
                        nameHint = "atomicfu\$cur", false
                    )
                    val upd = createTmpVariable(
                        irCall(atomicfuSymbols.invoke1Symbol).apply {
                            dispatchReceiver = irGet(action)
                            putValueArgument(0, irGet(cur))
                        }, "atomicfu\$upd", false
                    )
                    +irIfThen(
                        type = atomicfuSymbols.irBuiltIns.unitType,
                        condition = invokeFunctionOnAtomicHandler(
                            atomicHandlerType = atomicHandlerType,
                            getAtomicHandler = getAtomicHandler.deepCopyWithSymbols(),
                            functionName = "compareAndSet",
                            valueArguments = buildList { if (extraArg != null) add(irGet(extraArg)); add(irGet(cur)); add(irGet(upd)) },
                            valueType = valueType
                        ),
                        thenPart = when (functionName) {
                            "update" -> irReturnUnit()
                            "getAndUpdate" -> irReturn(irGet(cur))
                            "updateAndGet" -> irReturn(irGet(upd))
                            else -> error("Unsupported atomicfu inline loop function name: $functionName")
                        }
                    )
                }
            }
        }
}
