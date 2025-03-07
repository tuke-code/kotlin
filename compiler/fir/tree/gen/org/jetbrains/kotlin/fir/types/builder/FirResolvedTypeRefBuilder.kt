/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.types.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl

@FirBuilderDsl
class FirResolvedTypeRefBuilder : FirAnnotationContainerBuilder {
    var source: KtSourceElement? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var coneType: ConeKotlinType
    var delegatedTypeRef: FirTypeRef? = null

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirResolvedTypeRef {
        return FirResolvedTypeRefImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneType,
            delegatedTypeRef,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedTypeRef(init: FirResolvedTypeRefBuilder.() -> Unit): FirResolvedTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirResolvedTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedTypeRefCopy(original: FirResolvedTypeRef, init: FirResolvedTypeRefBuilder.() -> Unit): FirResolvedTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirResolvedTypeRefBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneType = original.coneType
    copyBuilder.delegatedTypeRef = original.delegatedTypeRef
    return copyBuilder.apply(init).build()
}
