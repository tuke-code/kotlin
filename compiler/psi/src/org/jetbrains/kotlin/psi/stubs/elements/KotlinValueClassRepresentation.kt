/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeBean

/**
 * This class is intended to provide all necessary information via stubs to
 * create [org.jetbrains.kotlin.descriptors.ValueClassRepresentation] during stub -> FIR
 * conversion.
 */
data class KotlinValueClassRepresentation(
    /**
     * Returns **true** for [org.jetbrains.kotlin.descriptors.InlineClassRepresentation]
     * and **false** for [org.jetbrains.kotlin.descriptors.MultiFieldValueClassRepresentation]
     */
    val isInline: Boolean,

    /**
     * If [isInline] **true** then this list contains zero or one element. Otherwise, it has at least one element.
     */
    val underlyingTypes: List<KotlinTypeBean>,
)
