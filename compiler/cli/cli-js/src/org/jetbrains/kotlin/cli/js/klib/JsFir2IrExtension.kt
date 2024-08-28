/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js.klib

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.name.JsStandardClassIds

object JsFir2IrExtension : Fir2IrExtensions by Fir2IrExtensions.Default {
    override fun isTrueStatic(declaration: FirCallableDeclaration, session: FirSession): Boolean {
        return declaration.hasAnnotation(JsStandardClassIds.Annotations.JsNoDispatchReceiver, session)
    }
}