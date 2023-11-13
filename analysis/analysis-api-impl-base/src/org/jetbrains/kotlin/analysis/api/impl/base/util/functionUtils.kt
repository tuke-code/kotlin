/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.util

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.CallablePath
import org.jetbrains.kotlin.util.OperatorNameConventions

val kotlinFunctionInvokeCallablePaths = (0..23).flatMapTo(hashSetOf()) { arity ->
    listOf(
        CallablePath(StandardNames.getFunctionClassId(arity), OperatorNameConventions.INVOKE),
        CallablePath(StandardNames.getSuspendFunctionClassId(arity), OperatorNameConventions.INVOKE)
    )
}
