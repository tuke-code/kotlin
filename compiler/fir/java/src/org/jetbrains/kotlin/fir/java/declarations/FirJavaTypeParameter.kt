/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase.Companion.ANALYZED_DEPENDENCIES
import org.jetbrains.kotlin.fir.declarations.FirResolvedToPhaseState
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.ResolveStateAccess
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.java.FirJavaTypeConversionMode
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.resolveIfJavaType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(FirImplementationDetail::class, ResolveStateAccess::class)
class FirJavaTypeParameter(
    override val source: KtSourceElement?,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override val name: Name,
    override val symbol: FirTypeParameterSymbol,
    override val containingDeclarationSymbol: FirBasedSymbol<*>,
    private var initialBounds: MutableOrEmptyList<FirTypeRef>,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
) : FirTypeParameter() {
    override val variance: Variance
        get() = Variance.INVARIANT

    override val isReified: Boolean
        get() = false

    private var enhancedBounds: MutableList<FirResolvedTypeRef>? = null

    private var conversionModeObserver = FirJavaTypeConversionMode.DEFAULT

    override val bounds: List<FirTypeRef>
        get() {
            enhancedBounds?.let { return it }
            if (containingDeclarationSymbol is FirClassSymbol) {
                throw AssertionError(
                    "Attempt to access Java class type parameter bounds before their enhancement!" +
                            " ownerSymbol = $containingDeclarationSymbol typeParameter = $name"
                )
            }
            return initialBounds
        }

    init {
        symbol.bind(this)
        resolveState = FirResolvedToPhaseState(ANALYZED_DEPENDENCIES)
    }

    internal fun areBoundsAlreadyResolved(): Boolean =
        conversionModeObserver == FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_AFTER_FIRST_ROUND

    internal fun resolveInitialBoundsIfJavaTypes(
        session: FirSession,
        javaTypeParameterStack: JavaTypeParameterStack,
        source: KtSourceElement?,
        mode: FirJavaTypeConversionMode,
    ) {
        when (mode) {
            FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_FIRST_ROUND -> {
                if (conversionModeObserver != FirJavaTypeConversionMode.DEFAULT) {
                    throw AssertionError(
                        "Attempt to repeat the first round of Java type parameter bounds enhancement!" +
                                " ownerSymbol = $containingDeclarationSymbol typeParameter = $name"
                    )
                }
            }
            FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_AFTER_FIRST_ROUND -> {
                if (conversionModeObserver != FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_FIRST_ROUND) {
                    throw AssertionError(
                        "Attempt to repeat the second round of Java type parameter bounds enhancement!" +
                                " ownerSymbol = $containingDeclarationSymbol typeParameter = $name"
                    )
                }
            }
            else -> throw AssertionError("Incorrect conversion mode for Java type parameter bounds enhancement $mode")
        }
        conversionModeObserver = mode
        enhancedBounds = initialBounds.mapTo(mutableListOf()) {
            it.resolveIfJavaType(
                session, javaTypeParameterStack, source, mode
            ) as FirResolvedTypeRef
        }
        if (mode == FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_AFTER_FIRST_ROUND) {
            initialBounds = MutableOrEmptyList.empty()
        }
    }

    internal inline fun replaceEnhancedBounds(
        crossinline block: (FirTypeParameter, FirResolvedTypeRef) -> FirResolvedTypeRef
    ) {
        enhancedBounds!!.replaceAll { block(this, it) }
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        bounds.forEach { it.accept(visitor, data) }
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirJavaTypeParameter {
        throw AssertionError("Should not be called")
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirJavaTypeParameter {
        throw AssertionError("Should not be called")
    }

    override fun replaceBounds(newBounds: List<FirTypeRef>) {
        throw AssertionError("Should not be called")
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }
}

@FirBuilderDsl
class FirJavaTypeParameterBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    lateinit var moduleData: FirModuleData
    lateinit var origin: FirDeclarationOrigin
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    lateinit var name: Name
    lateinit var symbol: FirTypeParameterSymbol
    lateinit var containingDeclarationSymbol: FirBasedSymbol<*>
    val bounds: MutableList<FirTypeRef> = mutableListOf()
    override val annotations: MutableList<FirAnnotation> = mutableListOf()

    override fun build(): FirTypeParameter {
        return FirJavaTypeParameter(
            source,
            moduleData,
            origin,
            attributes,
            name,
            symbol,
            containingDeclarationSymbol,
            bounds.toMutableOrEmpty(),
            annotations.toMutableOrEmpty(),
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildJavaTypeParameter(init: FirJavaTypeParameterBuilder.() -> Unit): FirTypeParameter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirJavaTypeParameterBuilder().apply(init).build()
}
