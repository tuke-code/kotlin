/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClassBuilder
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Builder
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class BuilderGenerator(
    session: FirSession,
) : AbstractBuilderGenerator<Builder>(session) {
    override val builderModality: Modality = Modality.FINAL

    override val annotationClassId: ClassId = LombokNames.BUILDER_ID

    override fun getBuilder(classSymbol: FirBasedSymbol<*>): Builder? {
        return lombokService.getBuilder(classSymbol)
    }

    override fun constructBuilderType(builderClassId: ClassId): ConeClassLikeType {
        return builderClassId.constructClassLikeType(emptyArray(), isMarkedNullable = false)
    }

    override fun getBuilderType(builderClassSymbol: FirRegularClassSymbol): ConeKotlinType {
        return builderClassSymbol.defaultType()
    }

    override fun MutableMap<Name, FirJavaMethod>.addBuilderMethodsIfNeeded(
        builder: Builder,
        classSymbol: FirClassSymbol<*>,
        builderClassSymbol: FirRegularClassSymbol,
        existingFunctionNames: Set<Name>,
    ) {
        addIfNeeded(Name.identifier(builder.buildMethodName), existingFunctionNames) {
            builderClassSymbol.createJavaMethod(
                it,
                valueParameters = emptyList(),
                returnTypeRef = classSymbol.defaultType().toFirResolvedTypeRef(),
                visibility = builder.visibility.toVisibility(),
                modality = Modality.FINAL
            )
        }
    }

    override fun FirJavaClassBuilder.completeBuilder(
        classSymbol: FirClassSymbol<*>, builderClassSymbol: FirRegularClassSymbol,
    ) {
        superTypeRefs += listOf(session.builtinTypes.anyType)
    }
}