/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Cleaner.h"

#include "ExternalRCRef.hpp"
#include "Memory.h"
#include "Runtime.h"
#include "Worker.h"
#include "std_support/Atomic.hpp"

using namespace kotlin;

// Defined in Cleaner.kt
extern "C" void Kotlin_CleanerImpl_shutdownCleanerWorker(KInt, bool);
extern "C" KInt Kotlin_CleanerImpl_createCleanerWorker();

namespace {

struct CleanerImpl {
    ObjHeader header;
    mm::RawExternalRCRef* cleanerStablePtr;
};

constexpr KInt kCleanerWorkerUninitialized = 0;
constexpr KInt kCleanerWorkerInitializing = -1;
constexpr KInt kCleanerWorkerShutdown = -2;

std::atomic globalCleanerWorker = kCleanerWorkerUninitialized;

void disposeCleaner(CleanerImpl* thiz) {
    auto worker = globalCleanerWorker.load();
    RuntimeAssert(
            worker != kCleanerWorkerUninitialized && worker != kCleanerWorkerInitializing,
            "Cleaner worker must've been initialized by now");
    if (worker == kCleanerWorkerShutdown) {
        if (Kotlin_cleanersLeakCheckerEnabled()) {
            konan::consoleErrorf(
                    "Cleaner %p was disposed during program exit\n"
                    "Use `Platform.isCleanersLeakCheckerActive = false` to avoid this check.\n",
                    thiz);
            RuntimeCheck(false, "Terminating now");
        }
        return;
    }

    RuntimeAssert(worker > 0, "Cleaner worker must be fully initialized here");

    bool result = WorkerSchedule(worker, mm::OwningExternalRCRef(thiz->cleanerStablePtr));
    RuntimeAssert(result, "Couldn't find Cleaner worker");
}

} // namespace

RUNTIME_NOTHROW void DisposeCleaner(KRef thiz) {
    try {
        disposeCleaner(reinterpret_cast<CleanerImpl*>(thiz));
    } catch (...) {
        // A trick to terminate with unhandled exception. This will print a stack trace
        // and write to iOS crash log.
        std::terminate();
    }
}

void ShutdownCleaners(bool executeScheduledCleaners) {
    KInt worker = 0;
    {
        // This loop may spin waiting for a proper worker state. Switch to the native thread state.
        kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
        do {
            worker = globalCleanerWorker.load();
            RuntimeAssert(worker != kCleanerWorkerShutdown, "Cleaner worker must not be shutdown twice");
            if (worker == kCleanerWorkerUninitialized) {
                if (!globalCleanerWorker.compare_exchange_weak(worker, kCleanerWorkerShutdown)) {
                    // Someone is trying to initialize the worker. Try again.
                    continue;
                }
                // worker was never initialized. Just return.
                return;
            }
            if (worker == kCleanerWorkerInitializing) {
                // Someone is trying to initialize the worker. Try again.
                continue;
            }

            // Worker is in some proper state.
            break;

        } while (true);
    }

    RuntimeAssert(worker > 0, "Cleaner worker must be fully initialized here");

    globalCleanerWorker.store(kCleanerWorkerShutdown);
    Kotlin_CleanerImpl_shutdownCleanerWorker(worker, executeScheduledCleaners);
    WaitNativeWorkerTermination(worker);
}

extern "C" KInt Kotlin_CleanerImpl_getCleanerWorker() {
    KInt worker = 0;
    // This loop may spin waiting for a proper worker state. Switch to the native thread state.
    kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
    do {
        worker = globalCleanerWorker.load();
        RuntimeAssert(worker != kCleanerWorkerShutdown, "Cleaner worker must not have been shutdown");
        if (worker == kCleanerWorkerUninitialized) {
            if (!globalCleanerWorker.compare_exchange_weak(worker, kCleanerWorkerInitializing)) {
                // Someone else is trying to initialize the worker. Try again.
                continue;
            }
            worker = kotlin::CallWithThreadState<kotlin::ThreadState::kRunnable>(Kotlin_CleanerImpl_createCleanerWorker);
            if (!kotlin::std_support::atomic_compare_exchange_strong(globalCleanerWorker, kCleanerWorkerInitializing, worker)) {
                RuntimeCheck(false, "Someone interrupted worker initializing");
            }
            // Worker is initialized.
            break;
        }
        if (worker == kCleanerWorkerInitializing) {
            // Someone is trying to initialize the worker. Try again.
            continue;
        }

        // Worker is in some proper state.
        break;
    } while (true);

    RuntimeAssert(worker > 0, "Cleaner worker must be fully initialized here");

    return worker;
}

void ResetCleanerWorkerForTests() {
    globalCleanerWorker.store(kCleanerWorkerUninitialized);
}
