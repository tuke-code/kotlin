/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.types.AbstractTypePreparator
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

class ConeTypePreparator(val session: FirSession) : AbstractTypePreparator() {
    private fun prepareType(type: ConeSimpleKotlinType, dropAttributes: Boolean): ConeSimpleKotlinType {
        return when (type) {
            is ConeClassLikeType -> type.fullyExpandedType(session).let {
                if (!dropAttributes) it else it.withAttributes(
                    ConeAttributes.create(
                        it.attributes.filter { attr -> !attr.implementsEquality }
                    )
                ).let {
                    if (!dropAttributes) it else it.withArguments { prepareTypeProjection(it) }
                }
            }
            else -> type
        }
    }

    private fun prepareTypeProjection(projection: ConeTypeProjection): ConeTypeProjection {
        return when (projection.kind) {
            ProjectionKind.STAR -> projection
            ProjectionKind.IN -> ConeKotlinTypeProjectionIn(prepareType(projection.type!!, true))
            ProjectionKind.OUT -> ConeKotlinTypeProjectionOut(prepareType(projection.type!!, true))
            ProjectionKind.INVARIANT -> prepareType(projection as ConeKotlinType, true)
        }
    }

    override fun prepareType(type: KotlinTypeMarker, dropAttributes: Boolean): ConeKotlinType {
        if (type !is ConeKotlinType) {
            throw AssertionError("Unexpected type in ConeTypePreparator: ${this::class.java}")
        }
        return when (type) {
            is ConeFlexibleType -> {
                val lowerBound = prepareType(type.lowerBound, dropAttributes)
                if (lowerBound === type.lowerBound) return type

                ConeFlexibleType(lowerBound, prepareType(type.upperBound, dropAttributes))
            }
            is ConeSimpleKotlinType -> prepareType(type, dropAttributes)
        }
    }
}
