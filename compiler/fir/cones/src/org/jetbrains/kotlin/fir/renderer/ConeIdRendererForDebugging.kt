/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.name.CallablePath
import org.jetbrains.kotlin.name.ClassId

class ConeIdRendererForDebugging : ConeIdRenderer() {
    override fun renderClassId(classId: ClassId) {
        builder.append(classId.asString())
    }

    override fun renderCallableId(callablePath: CallablePath) {
        builder.append(callablePath.callableName)
    }
}