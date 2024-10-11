/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.irFlag

var IrAttributeContainer.localClassName: String? by irAttribute(followAttributeOwner = true)

var IrFunction.isCalledFromExportedInlineFunction: Boolean by irFlag(followAttributeOwner = false)

var IrClass.isConstructedFromExportedInlineFunctions: Boolean by irFlag(followAttributeOwner = false)