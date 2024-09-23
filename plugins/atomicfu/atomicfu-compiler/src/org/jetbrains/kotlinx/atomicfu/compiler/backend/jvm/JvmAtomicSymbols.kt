/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addExtensionReceiver
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicSymbols

class JvmAtomicSymbols(
    context: IrPluginContext,
    moduleFragment: IrModuleFragment
) : AbstractAtomicSymbols(context, moduleFragment) {

    private val javaLang: IrPackageFragment = createPackage("java.lang")
    private val kotlinJvm: IrPackageFragment = createPackage("kotlin.jvm")
    private val javaUtilConcurrent: IrPackageFragment = createPackage("java.util.concurrent.atomic")
    private val javaLangClass: IrClassSymbol = createClass(javaLang, "Class", ClassKind.CLASS, Modality.FINAL)

    override val volatileAnnotationClass: IrClass
        get() = context.referenceClass(ClassId(FqName("kotlin.jvm"), Name.identifier("Volatile")))?.owner
            ?: error("kotlin.jvm.Volatile class is not found")

    // java.util.concurrent.AtomicIntegerFieldUpdater
    val javaAtomicIntegerFieldUpdaterClass: IrClassSymbol by lazy {
        createClass(javaUtilConcurrent, "AtomicIntegerFieldUpdater", ClassKind.CLASS, Modality.FINAL).apply {
            owner.addFunction("newUpdater", this.defaultType, isStatic = true).apply {
                addValueParameter("tclass", javaLangClass.starProjectedType)
                addValueParameter("fieldName", irBuiltIns.stringType)
            }
            owner.addFunction(name = "get", returnType = irBuiltIns.intType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
            }
            owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
                addValueParameter("newValue", irBuiltIns.intType)
            }
            owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
                addValueParameter("expect", irBuiltIns.intType)
                addValueParameter("update", irBuiltIns.intType)
            }
            owner.addFunction(name = "addAndGet", returnType = irBuiltIns.intType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
                addValueParameter("delta", irBuiltIns.intType)
            }
            owner.addFunction(name = "getAndAdd", returnType = irBuiltIns.intType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
                addValueParameter("delta", irBuiltIns.intType)
            }
            owner.addFunction(name = "incrementAndGet", returnType = irBuiltIns.intType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
            }
            owner.addFunction(name = "getAndIncrement", returnType = irBuiltIns.intType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
            }
            owner.addFunction(name = "decrementAndGet", returnType = irBuiltIns.intType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
            }
            owner.addFunction(name = "getAndDecrement", returnType = irBuiltIns.intType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
            }
            owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
                addValueParameter("newValue", irBuiltIns.intType)
            }
            owner.addFunction(name = "getAndSet", returnType = irBuiltIns.intType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
                addValueParameter("newValue", irBuiltIns.intType)
            }
        }
    }

    // java.util.concurrent.AtomicLongFieldUpdater
    val javaAtomicLongFieldUpdaterClass: IrClassSymbol by lazy {
        createClass(javaUtilConcurrent, "AtomicLongFieldUpdater", ClassKind.CLASS, Modality.FINAL).apply {
            owner.addFunction("newUpdater", this.defaultType, isStatic = true).apply {
                addValueParameter("tclass", javaLangClass.starProjectedType)
                addValueParameter("fieldName", irBuiltIns.stringType)
            }
            owner.addFunction(name = "get", returnType = irBuiltIns.longType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
            }
            owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
                addValueParameter("newValue", irBuiltIns.longType)
            }
            owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
                addValueParameter("expect", irBuiltIns.longType)
                addValueParameter("update", irBuiltIns.longType)
            }
            owner.addFunction(name = "addAndGet", returnType = irBuiltIns.longType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
                addValueParameter("delta", irBuiltIns.longType)
            }
            owner.addFunction(name = "getAndAdd", returnType = irBuiltIns.longType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
                addValueParameter("delta", irBuiltIns.longType)
            }
            owner.addFunction(name = "incrementAndGet", returnType = irBuiltIns.longType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
            }
            owner.addFunction(name = "getAndIncrement", returnType = irBuiltIns.longType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
            }
            owner.addFunction(name = "decrementAndGet", returnType = irBuiltIns.longType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
            }
            owner.addFunction(name = "getAndDecrement", returnType = irBuiltIns.longType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
            }
            owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
                addValueParameter("newValue", irBuiltIns.longType)
            }
            owner.addFunction(name = "getAndSet", returnType = irBuiltIns.longType).apply {
                addValueParameter("obj", irBuiltIns.anyType)
                addValueParameter("newValue", irBuiltIns.longType)
            }
        }
    }

    // java.util.concurrent.AtomicReferenceFieldUpdater
    val javaAtomicRefFieldUpdaterClass: IrClassSymbol by lazy {
        createClass(javaUtilConcurrent, "AtomicReferenceFieldUpdater", ClassKind.CLASS, Modality.FINAL).apply {
            owner.addFunction("newUpdater", this.defaultType, isStatic = true).apply {
                addValueParameter("tclass", javaLangClass.starProjectedType)
                addValueParameter("vclass", javaLangClass.starProjectedType)
                addValueParameter("fieldName", irBuiltIns.stringType)
            }
            owner.addFunction(name = "get", returnType = irBuiltIns.anyNType).apply {
                val valueType = addTypeParameter("T", irBuiltIns.anyNType)
                addValueParameter("obj", irBuiltIns.anyType)
                returnType = valueType.defaultType
            }
            owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
                val valueType = addTypeParameter("T", irBuiltIns.anyNType)
                addValueParameter("obj", irBuiltIns.anyType)
                addValueParameter("newValue", valueType.defaultType)
            }
            owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
                val valueType = addTypeParameter("T", irBuiltIns.anyNType)
                addValueParameter("obj", irBuiltIns.anyType)
                addValueParameter("expect", valueType.defaultType)
                addValueParameter("update", valueType.defaultType)
            }
            owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
                val valueType = addTypeParameter("T", irBuiltIns.anyNType)
                addValueParameter("obj", irBuiltIns.anyType)
                addValueParameter("newValue", valueType.defaultType)
            }
            owner.addFunction(name = "getAndSet", returnType = irBuiltIns.anyNType).apply {
                val valueType = addTypeParameter("T", irBuiltIns.anyNType)
                addValueParameter("obj", irBuiltIns.anyType)
                addValueParameter("newValue", valueType.defaultType)
                returnType = valueType.defaultType
            }
        }
    }

    fun newUpdater(atomicUpdaterClassSymbol: IrClassSymbol): IrSimpleFunctionSymbol =
        atomicUpdaterClassSymbol.getSimpleFunction("newUpdater")
            ?: error("No newUpdater function was found for ${atomicUpdaterClassSymbol.owner.render()} ")

    // java.util.concurrent.AtomicIntegerArray
    override val atomicIntArrayClassSymbol: IrClassSymbol by lazy {
        createClass(javaUtilConcurrent, "AtomicIntegerArray", ClassKind.CLASS, Modality.FINAL).apply {
            owner.addConstructor().apply {
                addValueParameter("length", irBuiltIns.intType)
            }
            owner.addFunction(name = "get", returnType = irBuiltIns.intType).apply {
                addValueParameter("i", irBuiltIns.intType)
            }
            owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
                addValueParameter("i", irBuiltIns.intType)
                addValueParameter("newValue", irBuiltIns.intType)
            }
            owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
                addValueParameter("i", irBuiltIns.intType)
                addValueParameter("expect", irBuiltIns.intType)
                addValueParameter("update", irBuiltIns.intType)
            }
            owner.addFunction(name = "addAndGet", returnType = irBuiltIns.intType).apply {
                addValueParameter("i", irBuiltIns.intType)
                addValueParameter("delta", irBuiltIns.intType)
            }
            owner.addFunction(name = "getAndAdd", returnType = irBuiltIns.intType).apply {
                addValueParameter("i", irBuiltIns.intType)
                addValueParameter("delta", irBuiltIns.intType)
            }
            owner.addFunction(name = "incrementAndGet", returnType = irBuiltIns.intType).apply {
                addValueParameter("i", irBuiltIns.intType)
            }
            owner.addFunction(name = "getAndIncrement", returnType = irBuiltIns.intType).apply {
                addValueParameter("i", irBuiltIns.intType)
            }
            owner.addFunction(name = "decrementAndGet", returnType = irBuiltIns.intType).apply {
                addValueParameter("i", irBuiltIns.intType)
            }
            owner.addFunction(name = "getAndDecrement", returnType = irBuiltIns.intType).apply {
                addValueParameter("i", irBuiltIns.intType)
            }
            owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
                addValueParameter("i", irBuiltIns.intType)
                addValueParameter("newValue", irBuiltIns.intType)
            }
            owner.addFunction(name = "getAndSet", returnType = irBuiltIns.intType).apply {
                addValueParameter("i", irBuiltIns.intType)
                addValueParameter("newValue", irBuiltIns.intType)
            }
        }
    }

    // java.util.concurrent.AtomicLongArray
    override val atomicLongArrayClassSymbol: IrClassSymbol by lazy {
        createClass(javaUtilConcurrent, "AtomicLongArray", ClassKind.CLASS, Modality.FINAL).apply {
            owner.addConstructor().apply {
                addValueParameter("length", irBuiltIns.intType)
            }
            owner.addFunction(name = "get", returnType = irBuiltIns.longType).apply {
                addValueParameter("i", irBuiltIns.intType)
            }
            owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
                addValueParameter("i", irBuiltIns.intType)
                addValueParameter("newValue", irBuiltIns.longType)
            }
            owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
                addValueParameter("i", irBuiltIns.intType)
                addValueParameter("expect", irBuiltIns.longType)
                addValueParameter("update", irBuiltIns.longType)
            }
            owner.addFunction(name = "addAndGet", returnType = irBuiltIns.longType).apply {
                addValueParameter("i", irBuiltIns.intType)
                addValueParameter("delta", irBuiltIns.longType)
            }
            owner.addFunction(name = "getAndAdd", returnType = irBuiltIns.longType).apply {
                addValueParameter("i", irBuiltIns.intType)
                addValueParameter("delta", irBuiltIns.longType)
            }
            owner.addFunction(name = "incrementAndGet", returnType = irBuiltIns.longType).apply {
                addValueParameter("i", irBuiltIns.intType)
            }
            owner.addFunction(name = "getAndIncrement", returnType = irBuiltIns.longType).apply {
                addValueParameter("i", irBuiltIns.intType)
            }
            owner.addFunction(name = "decrementAndGet", returnType = irBuiltIns.longType).apply {
                addValueParameter("i", irBuiltIns.intType)
            }
            owner.addFunction(name = "getAndDecrement", returnType = irBuiltIns.longType).apply {
                addValueParameter("i", irBuiltIns.intType)
            }
            owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
                addValueParameter("i", irBuiltIns.intType)
                addValueParameter("newValue", irBuiltIns.longType)
            }
            owner.addFunction(name = "getAndSet", returnType = irBuiltIns.longType).apply {
                addValueParameter("i", irBuiltIns.intType)
                addValueParameter("newValue", irBuiltIns.longType)
            }
        }
    }

    // java.util.concurrent.AtomicReferenceArray
    override val atomicRefArrayClassSymbol: IrClassSymbol by lazy {
        createClass(javaUtilConcurrent, "AtomicReferenceArray", ClassKind.CLASS, Modality.FINAL).apply {
            owner.addConstructor().apply {
                addTypeParameter("T", irBuiltIns.anyNType)
                addValueParameter("length", irBuiltIns.intType)
            }
            owner.addConstructor().apply {
                val valueType = addTypeParameter("T", irBuiltIns.anyNType)
                addValueParameter("array", irBuiltIns.arrayClass.defaultType)
            }
            owner.addFunction(name = "get", returnType = irBuiltIns.anyNType).apply {
                val valueType = addTypeParameter("T", irBuiltIns.anyNType)
                addValueParameter("i", irBuiltIns.intType)
                returnType = valueType.defaultType
            }
            owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
                val valueType = addTypeParameter("T", irBuiltIns.anyNType)
                addValueParameter("i", irBuiltIns.intType)
                addValueParameter("newValue", valueType.defaultType)
            }
            owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
                val valueType = addTypeParameter("T", irBuiltIns.anyNType)
                addValueParameter("i", irBuiltIns.intType)
                addValueParameter("expect", valueType.defaultType)
                addValueParameter("update", valueType.defaultType)
            }
            owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
                val valueType = addTypeParameter("T", irBuiltIns.anyNType)
                addValueParameter("i", irBuiltIns.intType)
                addValueParameter("newValue", valueType.defaultType)
            }
            owner.addFunction(name = "getAndSet", returnType = irBuiltIns.anyNType).apply {
                val valueType = addTypeParameter("T", irBuiltIns.anyNType)
                addValueParameter("i", irBuiltIns.intType)
                addValueParameter("newValue", valueType.defaultType)
                returnType = valueType.defaultType
            }
        }
    }

    // java.util.concurrent.AtomicInteger

    val javaAtomicIntegerClass: IrClassSymbol by lazy {
        createClass(javaUtilConcurrent, "AtomicInteger", ClassKind.CLASS, Modality.FINAL).apply {
            owner.addConstructor().apply {
                addValueParameter("value", irBuiltIns.intType)
            }
            owner.addFunction(name = "get", returnType = irBuiltIns.intType)
            owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
                addValueParameter("newValue", irBuiltIns.intType)
            }
            owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
                addValueParameter("expect", irBuiltIns.intType)
                addValueParameter("update", irBuiltIns.intType)
            }
            owner.addFunction(name = "addAndGet", returnType = irBuiltIns.intType).apply {
                addValueParameter("delta", irBuiltIns.intType)
            }
            owner.addFunction(name = "getAndAdd", returnType = irBuiltIns.intType).apply {
                addValueParameter("delta", irBuiltIns.intType)
            }
            owner.addFunction(name = "incrementAndGet", returnType = irBuiltIns.intType)
            owner.addFunction(name = "getAndIncrement", returnType = irBuiltIns.intType)
            owner.addFunction(name = "decrementAndGet", returnType = irBuiltIns.intType)
            owner.addFunction(name = "getAndDecrement", returnType = irBuiltIns.intType)
            owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
                addValueParameter("newValue", irBuiltIns.intType)
            }
            owner.addFunction(name = "getAndSet", returnType = irBuiltIns.intType).apply {
                addValueParameter("newValue", irBuiltIns.intType)
            }
        }
    }

    // java.util.concurrent.AtomicLong

    val javaAtomicLongClass: IrClassSymbol by lazy {
        createClass(javaUtilConcurrent, "AtomicLong", ClassKind.CLASS, Modality.FINAL).apply {
            owner.addConstructor().apply {
                addValueParameter("value", irBuiltIns.longType)
            }
            owner.addFunction(name = "get", returnType = irBuiltIns.longType)
            owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
                addValueParameter("newValue", irBuiltIns.longType)
            }
            owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
                addValueParameter("expect", irBuiltIns.longType)
                addValueParameter("update", irBuiltIns.longType)
            }
            owner.addFunction(name = "addAndGet", returnType = irBuiltIns.longType).apply {
                addValueParameter("delta", irBuiltIns.longType)
            }
            owner.addFunction(name = "getAndAdd", returnType = irBuiltIns.longType).apply {
                addValueParameter("delta", irBuiltIns.longType)
            }
            owner.addFunction(name = "incrementAndGet", returnType = irBuiltIns.longType)
            owner.addFunction(name = "getAndIncrement", returnType = irBuiltIns.longType)
            owner.addFunction(name = "decrementAndGet", returnType = irBuiltIns.longType)
            owner.addFunction(name = "getAndDecrement", returnType = irBuiltIns.longType)
            owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
                addValueParameter("newValue", irBuiltIns.longType)
            }
            owner.addFunction(name = "getAndSet", returnType = irBuiltIns.longType).apply {
                addValueParameter("newValue", irBuiltIns.longType)
            }
        }
    }

    val javaAtomicReferenceClass: IrClassSymbol by lazy {
        createClass(javaUtilConcurrent, "AtomicReference", ClassKind.CLASS, Modality.FINAL).apply {
            owner.addConstructor().apply {
                addValueParameter("value", irBuiltIns.anyNType)
            }
            owner.addFunction(name = "get", returnType = irBuiltIns.anyNType).apply {
                val valueType = addTypeParameter("T", irBuiltIns.anyNType)
                returnType = valueType.defaultType
            }
            owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
                val valueType = addTypeParameter("T", irBuiltIns.anyNType)
                addValueParameter("newValue", valueType.defaultType)
            }
            owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
                val valueType = addTypeParameter("T", irBuiltIns.anyNType)
                addValueParameter("expect", valueType.defaultType)
                addValueParameter("update", valueType.defaultType)
            }
            owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
                val valueType = addTypeParameter("T", irBuiltIns.anyNType)
                addValueParameter("newValue", valueType.defaultType)
            }
            owner.addFunction(name = "getAndSet", returnType = irBuiltIns.anyNType).apply {
                val valueType = addTypeParameter("T", irBuiltIns.anyNType)
                addValueParameter("newValue", valueType.defaultType)
                returnType = valueType.defaultType
            }
        }
    }

    val javaAtomicBooleanClass: IrClassSymbol by lazy {
        createClass(javaUtilConcurrent, "AtomicBoolean", ClassKind.CLASS, Modality.FINAL).apply {
            owner.addConstructor().apply {
                addValueParameter("value", irBuiltIns.booleanType)
            }
            owner.addFunction(name = "get", returnType = irBuiltIns.booleanType).apply {
                returnType = irBuiltIns.booleanType
            }
            owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
                addValueParameter("newValue", irBuiltIns.booleanType)
            }
            owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
                addValueParameter("expect", irBuiltIns.booleanType)
                addValueParameter("update", irBuiltIns.booleanType)
            }
            owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
                addValueParameter("newValue", irBuiltIns.booleanType)
            }
            owner.addFunction(name = "getAndSet", returnType = irBuiltIns.booleanType).apply {
                addValueParameter("newValue", irBuiltIns.booleanType)
                returnType = irBuiltIns.booleanType
            }
        }
    }

    private val BOXED_ATOMIC_TYPES = setOf(
        javaAtomicIntegerClass,
        javaAtomicLongClass,
        javaAtomicBooleanClass,
        javaAtomicReferenceClass
    )

    private val ATOMIC_FIELD_UPDATER_TYPES = setOf(
        javaAtomicIntegerFieldUpdaterClass,
        javaAtomicLongFieldUpdaterClass,
        javaAtomicRefFieldUpdaterClass
    )

    fun isAtomicFieldUpdaterHandlerType(type: IrType) = type.classOrNull in ATOMIC_FIELD_UPDATER_TYPES
    fun isBoxedAtomicHandlerType(type: IrType) = type.classOrNull in BOXED_ATOMIC_TYPES

    fun javaFUClassSymbol(valueType: IrType): IrClassSymbol =
        when {
            valueType.isInt() -> javaAtomicIntegerFieldUpdaterClass
            valueType.isLong() -> javaAtomicLongFieldUpdaterClass
            valueType.isBoolean() -> javaAtomicIntegerFieldUpdaterClass
            !valueType.isPrimitiveType() -> javaAtomicRefFieldUpdaterClass
            else -> error("Non of the Java field updater types Atomic(Integer|Long|Reference)FieldUpdater can be used to atomically update a property of the given type: ${valueType.render()}")
        }

    fun javaAtomicBoxClassSymbol(valueType: IrType): IrClassSymbol =
        when {
            valueType.isInt() -> javaAtomicIntegerClass
            valueType.isLong() -> javaAtomicLongClass
            valueType.isBoolean() -> javaAtomicBooleanClass
            !valueType.isPrimitiveType() -> javaAtomicReferenceClass
            else -> error("Non of the boxed Java atomic types Atomic(Integer|Long|Boolean|Reference) can be used to atomically update a property of the given type: ${valueType.render()}")
        }

    private fun createClass(
        irPackage: IrPackageFragment,
        shortName: String,
        classKind: ClassKind,
        classModality: Modality,
        isValueClass: Boolean = false,
    ): IrClassSymbol = irFactory.buildClass {
        name = Name.identifier(shortName)
        kind = classKind
        modality = classModality
        isValue = isValueClass
    }.apply {
        parent = irPackage
        createImplicitParameterDeclarationWithWrappedDescriptor()
    }.symbol

    private val kotlinKClassJava: IrPropertySymbol = irFactory.buildProperty {
        name = Name.identifier("java")
    }.apply {
        parent = kotlinJvm
        addGetter().apply {
            addExtensionReceiver(irBuiltIns.kClassClass.starProjectedType)
            returnType = javaLangClass.defaultType
        }
    }.symbol

    private fun kClassToJavaClass(kClassReference: IrExpression): IrCall =
        buildIrGet(javaLangClass.starProjectedType, null, kotlinKClassJava.owner.getter!!.symbol).apply {
            extensionReceiver = kClassReference
        }

    fun javaClassReference(classType: IrType): IrCall = kClassToJavaClass(kClassReference(classType))

    private fun kClassReference(classType: IrType): IrClassReferenceImpl =
        IrClassReferenceImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, irBuiltIns.kClassClass.starProjectedType, irBuiltIns.kClassClass, classType
        )

    private fun buildIrGet(
        type: IrType,
        receiver: IrExpression?,
        getterSymbol: IrFunctionSymbol
    ): IrCall = IrCallImpl(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
        type,
        getterSymbol as IrSimpleFunctionSymbol,
        typeArgumentsCount = getterSymbol.owner.typeParameters.size,
        origin = IrStatementOrigin.GET_PROPERTY
    ).apply {
        dispatchReceiver = receiver
    }

    override fun createBuilder(symbol: IrSymbol, startOffset: Int, endOffset: Int): JvmAtomicfuIrBuilder =
        JvmAtomicfuIrBuilder(this, symbol, startOffset, endOffset)
}