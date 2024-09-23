/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlinx.atomicfu.compiler.backend.*
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuIrBuilder
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuTransformer

class AtomicfuJvmIrTransformer(
    override val atomicfuSymbols: JvmAtomicSymbols,
    pluginContext: IrPluginContext
) : AbstractAtomicfuTransformer(pluginContext) {

    override val atomicfuPropertyTransformer: AtomicPropertiesTransformer = JvmAtomicPropertiesTransformer()
    override val atomicfuExtensionsTransformer: AtomicExtensionTransformer = JvmAtomicExtensionsTransformer()
    override val atomicfuFunctionCallTransformer: AtomicFunctionCallTransformer = JvmAtomicFunctionCallTransformer()

    private inner class JvmAtomicExtensionsTransformer : AtomicExtensionTransformer() {
        override fun transformedExtensionsForAllAtomicHandlers(atomicExtension: IrFunction): List<IrSimpleFunction> = listOf(
            generateExtensionForAtomicHandler(AtomicHandlerType.ATOMIC_FIELD_UPDATER, atomicExtension),
            generateExtensionForAtomicHandler(AtomicHandlerType.BOXED_ATOMIC, atomicExtension),
            generateExtensionForAtomicHandler(AtomicHandlerType.ATOMIC_ARRAY, atomicExtension)
        )
    }

    private inner class JvmAtomicPropertiesTransformer : AtomicPropertiesTransformer() {
        override fun createAtomicHandler(
            atomicfuProperty: IrProperty,
            parentContainer: IrDeclarationContainer
        ): AtomicHandler<IrProperty>? {
            val isTopLevel = parentContainer is IrFile || (parentContainer is IrClass && parentContainer.kind == ClassKind.OBJECT)
            return when {
                atomicfuProperty.isAtomic() -> {
                    if (isTopLevel) {
                        buildBoxedAtomic(atomicfuProperty, parentContainer)
                    } else {
                        createAtomicFieldUpdater(atomicfuProperty, parentContainer as IrClass)
                    }
                }
                atomicfuProperty.isAtomicArray() -> {
                    createAtomicArray(atomicfuProperty, parentContainer)
                }
                else -> null
            }
        }

        /**
         * Creates a [BoxedAtomic] updater to replace a top-level atomicfu property on JVM:
         * builds a property of Java boxed atomic type: java.util.concurrent.atomic.Atomic(Integer|Long|Boolean|Reference).
         */
        private fun buildBoxedAtomic(atomicfuProperty: IrProperty, parentContainer: IrDeclarationContainer): BoxedAtomic {
            with(atomicfuSymbols.createBuilder(atomicfuProperty.symbol)) {
                val atomicArrayField = irBoxedAtomicField(atomicfuProperty, parentContainer)
                val atomicArrayProperty = buildPropertyWithAccessors(
                    atomicArrayField,
                    atomicfuProperty.visibility,
                    isVar = false,
                    isStatic = parentContainer is IrFile,
                    parentContainer
                )
                return BoxedAtomic(atomicArrayProperty)
            }
        }

        /**
         * Creates an [AtomicFieldUpdater] to replace an in-class atomicfu property on JVM:
         * builds a volatile property of the type corresponding to the type of the atomic property, plus a Java atomic field updater:
         * java.util.concurrent.atomic.Atomic(Integer|Long|Reference)FieldUpdater.
         *
         * Note that as there is no AtomicBooleanFieldUpdater in Java, AtomicBoolean is relpaced with a Volatile Int property
         * and updated with j.u.c.a.AtomicIntegerFieldUpdater.
         */
        private fun createAtomicFieldUpdater(atomicfuProperty: IrProperty, parentClass: IrClass): AtomicFieldUpdater {
            with(atomicfuSymbols.createBuilder(atomicfuProperty.symbol)) {
                val volatilePropertyHandler = createVolatileProperty(atomicfuProperty, parentClass)
                val atomicUpdaterField = irJavaAtomicFieldUpdater(volatilePropertyHandler.declaration.backingField!!, parentClass)
                val atomicUpdaterProperty = buildPropertyWithAccessors(
                    atomicUpdaterField,
                    atomicfuProperty.getMinVisibility(),
                    isVar = false,
                    isStatic = true,
                    parentClass
                )
                return AtomicFieldUpdater(volatilePropertyHandler, atomicUpdaterProperty)
            }
        }

        private fun IrProperty.getMinVisibility(): DescriptorVisibility {
            // To protect atomic properties from leaking out of the current sourceSet, they are required to be internal or private,
            // or the containing class may be internal or private.
            // This method returns the minimal visibility between the property visibility and the class visibility applied to atomic updaters or volatile wrappers.
            val classVisibility = if (this.parent is IrClass) parentAsClass.visibility else DescriptorVisibilities.PUBLIC
            val compare = visibility.compareTo(classVisibility)
                ?: -1 // in case of non-comparable visibilities (e.g. local and private) return property visibility
            return if (compare > 0) classVisibility else visibility
        }
    }

    private inner class JvmAtomicFunctionCallTransformer : AtomicFunctionCallTransformer() {

        override fun AtomicHandler<*>.getAtomicHandlerExtraArg(
            dispatchReceiver: IrExpression?,
            propertyGetterCall: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression? = when(this) {
            // OBJ: get class instance
            is AtomicFieldUpdater -> dispatchReceiver
            is AtomicFieldUpdaterValueParameter -> {
                require(parentFunction != null && parentFunction.isTransformedAtomicExtension())
                require(parentFunction.valueParameters[1].name.asString() == OBJ)
                val obj = parentFunction.valueParameters[1].capture()
                obj
            }
            is AtomicArray -> getAtomicArrayElementIndex(propertyGetterCall)
            is AtomicArrayValueParameter -> getAtomicArrayElementIndex(parentFunction)
            else -> null
        }

        private fun AbstractAtomicfuIrBuilder.getAtomicHandlerReceiver(
            atomicHandler: AtomicHandler<*>,
            dispatchReceiver: IrExpression?
        ): IrExpression = when(atomicHandler) {
            is AtomicFieldUpdater -> irGetProperty(atomicHandler.declaration, null)
            is BoxedAtomic, is AtomicArray -> irGetProperty((atomicHandler.declaration as IrProperty), dispatchReceiver)
            is AtomicFieldUpdaterValueParameter, is BoxedAtomicValueParameter, is AtomicArrayValueParameter -> (atomicHandler.declaration as IrValueParameter).capture()
            else -> error("Unexpected atomic handler type for JVM backend: ${atomicHandler.javaClass.simpleName}")
        }

        override fun AbstractAtomicfuIrBuilder.getAtomicHandlerCallReceiver(
            atomicHandler: AtomicHandler<*>,
            dispatchReceiver: IrExpression?
        ): IrExpression = getAtomicHandlerReceiver(atomicHandler, dispatchReceiver)

        override fun AbstractAtomicfuIrBuilder.getAtomicHandlerValueParameterReceiver(
            atomicHandler: AtomicHandler<*>,
            dispatchReceiver: IrExpression?,
            parentFunction: IrFunction
        ): IrExpression = getAtomicHandlerReceiver(atomicHandler, dispatchReceiver)

        override fun valueParameterToAtomicHandler(valueParameter: IrValueParameter): AtomicHandler<*> =
            when {
                atomicfuSymbols.isBoxedAtomicHandlerType(valueParameter.type) -> BoxedAtomicValueParameter(valueParameter)
                atomicfuSymbols.isAtomicFieldUpdaterHandlerType(valueParameter.type) -> AtomicFieldUpdaterValueParameter(valueParameter)
                atomicfuSymbols.isAtomicArrayHandlerType(valueParameter.type) -> AtomicArrayValueParameter(valueParameter)
                else -> error("The type of the given valueParameter=${valueParameter.render()} does not match any of the JVM AtomicHandler types.")
            }
    }

    override fun IrFunction.checkAtomicHandlerValueParameters(atomicHandlerType: AtomicHandlerType, valueType: IrType): Boolean =
        when (atomicHandlerType) {
            AtomicHandlerType.ATOMIC_FIELD_UPDATER -> {
                valueParameters.size > 2 &&
                        valueParameters[0].name.asString() == ATOMIC_HANDLER && valueParameters[0].type == atomicfuSymbols.javaFUClassSymbol(valueType).defaultType &&
                        valueParameters[1].name.asString() == OBJ && valueParameters[1].type == irBuiltIns.anyNType
            }
            AtomicHandlerType.BOXED_ATOMIC -> {
                valueParameters.size > 1 &&
                        valueParameters[0].name.asString() == ATOMIC_HANDLER &&
                        valueParameters[0].type == atomicfuSymbols.javaAtomicBoxClassSymbol(valueType).defaultType
            }
            AtomicHandlerType.ATOMIC_ARRAY -> {
                val arrayClassSymbol = atomicfuSymbols.getAtomicArrayClassByValueType(valueType)
                val type = if (arrayClassSymbol.owner.typeParameters.isNotEmpty()) {
                    arrayClassSymbol.typeWith(valueType)
                } else {
                    arrayClassSymbol.defaultType
                }
                valueParameters.size > 2 &&
                        valueParameters[0].name.asString() == ATOMIC_HANDLER && valueParameters[0].type == type &&
                        valueParameters[1].name.asString() == INDEX && valueParameters[1].type == irBuiltIns.intType
            }
            else -> error("Unexpected atomic handler type for JVM backend: $atomicHandlerType")
        }

    override fun IrFunction.addAtomicHandlerValueParameters(atomicHandlerType: AtomicHandlerType, valueType: IrType) {
        when (atomicHandlerType) {
            AtomicHandlerType.ATOMIC_FIELD_UPDATER -> {
                addValueParameter(ATOMIC_HANDLER, atomicfuSymbols.javaFUClassSymbol(valueType).defaultType)
                addValueParameter(OBJ, irBuiltIns.anyNType)
            }
            AtomicHandlerType.BOXED_ATOMIC -> {
                addValueParameter(ATOMIC_HANDLER, atomicfuSymbols.javaAtomicBoxClassSymbol(valueType).defaultType)
            }
            AtomicHandlerType.ATOMIC_ARRAY -> {
                val arrayClassSymbol = atomicfuSymbols.getAtomicArrayClassByValueType(valueType)
                val type = if (arrayClassSymbol.owner.typeParameters.isNotEmpty()) {
                    arrayClassSymbol.typeWith(valueType)
                } else {
                    arrayClassSymbol.defaultType
                }
                addValueParameter(ATOMIC_HANDLER, type)
                addValueParameter(INDEX, irBuiltIns.intType)
            }
            else -> error("Unexpected atomic handler type for JVM backend: $atomicHandlerType")
        }
    }
}