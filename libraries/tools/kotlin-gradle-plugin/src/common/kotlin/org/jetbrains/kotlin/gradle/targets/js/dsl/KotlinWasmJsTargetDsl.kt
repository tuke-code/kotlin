/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.d8.D8Exec
import org.jetbrains.kotlin.gradle.targets.js.ir.IKotlinJsIrSubTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinD8Ir

interface KotlinWasmSubTargetContainerDsl : KotlinTarget {
    val subTargets: NamedDomainObjectContainer<IKotlinJsIrSubTarget>

    val d8: KotlinWasmD8Dsl

    val isD8Configured: Boolean
        get() = subTargets.filterIsInstance<KotlinD8Ir>().isNotEmpty()

    fun whenD8Configured(body: KotlinWasmD8Dsl.() -> Unit) {
        subTargets
            .matching { it is KotlinD8Ir }
            .all { body(it as KotlinD8Ir) }
    }
}

interface KotlinWasmJsTargetDsl : KotlinWasmTargetDsl, KotlinJsTargetDsl {
    fun d8() = d8 { }
    fun d8(body: KotlinWasmD8Dsl.() -> Unit)
    fun d8(fn: Action<KotlinWasmD8Dsl>) {
        d8 {
            fn.execute(this)
        }
    }
}

@OptIn(ExperimentalWasmDsl::class)
interface KotlinWasmD8Dsl : KotlinJsSubTargetDsl {
    fun runTask(body: Action<D8Exec>)
}