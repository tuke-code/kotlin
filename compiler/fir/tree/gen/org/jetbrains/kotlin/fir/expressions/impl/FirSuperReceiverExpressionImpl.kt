/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirSuperReceiverExpression
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace

@OptIn(UnresolvedExpressionTypeAccess::class)
internal class FirSuperReceiverExpressionImpl(
    @property:UnresolvedExpressionTypeAccess
    override var coneTypeOrNull: ConeKotlinType?,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
    override var typeArguments: MutableOrEmptyList<FirTypeProjection>,
    override var dispatchReceiver: FirExpression?,
    override var source: KtSourceElement?,
    override var nonFatalDiagnostics: MutableOrEmptyList<ConeDiagnostic>,
    override var calleeReference: FirSuperReference,
) : FirSuperReceiverExpression() {
    override val contextArguments: List<FirExpression>
        get() = emptyList()
    override val explicitReceiver: FirExpression?
        get() = null
    override val extensionReceiver: FirExpression?
        get() = null

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeArguments.forEach { it.accept(visitor, data) }
        dispatchReceiver?.accept(visitor, data)
        calleeReference.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirSuperReceiverExpressionImpl {
        transformAnnotations(transformer, data)
        transformTypeArguments(transformer, data)
        dispatchReceiver = dispatchReceiver?.transform(transformer, data)
        transformCalleeReference(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirSuperReceiverExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirSuperReceiverExpressionImpl {
        typeArguments.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirSuperReceiverExpressionImpl {
        return this
    }

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirSuperReceiverExpressionImpl {
        calleeReference = calleeReference.transform(transformer, data)
        return this
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?) {
        coneTypeOrNull = newConeTypeOrNull
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceContextArguments(newContextArguments: List<FirExpression>) {}

    override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>) {
        typeArguments = newTypeArguments.toMutableOrEmpty()
    }

    override fun replaceExplicitReceiver(newExplicitReceiver: FirExpression?) {}

    override fun replaceDispatchReceiver(newDispatchReceiver: FirExpression?) {
        dispatchReceiver = newDispatchReceiver
    }

    override fun replaceExtensionReceiver(newExtensionReceiver: FirExpression?) {}

    @FirImplementationDetail
    override fun replaceSource(newSource: KtSourceElement?) {
        source = newSource
    }

    override fun replaceNonFatalDiagnostics(newNonFatalDiagnostics: List<ConeDiagnostic>) {
        nonFatalDiagnostics = newNonFatalDiagnostics.toMutableOrEmpty()
    }

    override fun replaceCalleeReference(newCalleeReference: FirSuperReference) {
        calleeReference = newCalleeReference
    }

    override fun replaceCalleeReference(newCalleeReference: FirReference) {
        require(newCalleeReference is FirSuperReference)
        replaceCalleeReference(newCalleeReference)
    }
}
