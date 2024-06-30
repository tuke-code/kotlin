/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.symbols.IrDeclarationWithAccessorsSymbol
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.adaptedPropertyReference]
 */
abstract class IrAdaptedPropertyReference : IrExpression() {
    abstract var originalProperty: IrDeclarationWithAccessorsSymbol

    abstract val capturedValues: MutableList<IrExpression>

    abstract var getterFunction: IrFunctionExpression

    abstract var setterFunction: IrFunctionExpression?

    abstract var origin: IrStatementOrigin?

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitAdaptedPropertyReference(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        capturedValues.forEach { it.accept(visitor, data) }
        getterFunction.accept(visitor, data)
        setterFunction?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        capturedValues.transformInPlace(transformer, data)
        getterFunction = getterFunction.transform(transformer, data) as IrFunctionExpression
        setterFunction = setterFunction?.transform(transformer, data) as IrFunctionExpression?
    }
}
