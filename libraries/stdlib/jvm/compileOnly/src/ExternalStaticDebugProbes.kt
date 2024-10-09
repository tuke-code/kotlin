/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")

package kotlinx.coroutines.external

import kotlin.coroutines.Continuation

/**
 * ⚠️STUB!
 * This entire source file is a 'stub'. This `ExternalStaticDebugProbes` will not be packaged in the kotlin-stdlib!
 *
 * There are currently three ways of installing/engaging coroutines 'debug probes':
 * 1) Dynamic: Using byte-code transformation at runtime (ByteBuddy), the `kotlinx-coroutines-debug` module can replace the
 *  `DebugProbes.kt` at runtime, replacing the 'no-op's with something meaningful.
 *
 * 2) Static (Agent): A java agent can replace the `DebugProbesKt` from the kotlin-stdlib statically with an "enganged"/meaningful
 *  implementation
 *
 * 3) Static (ExternalStaticDebugProbes): The `DebugProbesKt` will check if this class `kotlinx.coroutines.external.ExternalStaticDebugProbes`
 *  is available at runtime and then calls into it. The class here is intended to act as 'stub' for which this `DebugProbesKt` can
 *  compile against. This stub class will not be packaged into the kotlin-stdlib, making room for applications to provide this class with
 *  an "engaged"/meaningful implementation statically.
 */
public object ExternalStaticDebugProbes {
    public fun <T> probeCoroutineCreated(completion: Continuation<T>): Continuation<T> {
        /** implementation of this function is replaced statically for advanced builds */
        return completion
    }


    @Suppress("UNUSED_PARAMETER")
    public fun probeCoroutineResumed(frame: Continuation<*>) {
        /** implementation of this function is replaced statically for advanced builds */
    }


    @Suppress("UNUSED_PARAMETER")
    public fun probeCoroutineSuspended(frame: Continuation<*>) {
        /** implementation of this function is replaced statically for advanced builds */
    }
}
