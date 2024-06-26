/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SingleObjectPage.hpp"

#include <atomic>
#include <cstdint>

#include "AllocatorImpl.hpp"
#include "CustomAllocator.hpp"
#include "CustomAllocConstants.hpp"
#include "CustomLogging.hpp"
#include "GCApi.hpp"

namespace {
ALWAYS_INLINE ObjHeader* objAt(uint8_t* address) {
    return reinterpret_cast<kotlin::alloc::HeapObjHeader*>(address)->object();
}
}

namespace kotlin::alloc {

SingleObjectPage* SingleObjectPage::Create(uint64_t cellCount) noexcept {
    CustomAllocInfo("SingleObjectPage::Create(%" PRIu64 ")", cellCount);
    RuntimeAssert(!compiler::pagedAllocator() || cellCount > NEXT_FIT_PAGE_MAX_BLOCK_SIZE, "blockSize too small for SingleObjectPage");
    uint64_t size = sizeof(SingleObjectPage) + cellCount * sizeof(uint64_t);
    return new (SafeAlloc(size)) SingleObjectPage(size);
}

SingleObjectPage::SingleObjectPage(size_t size) noexcept {}

// FIXME now used only by tests?
void SingleObjectPage::Destroy() noexcept {
    Free(this, pageSize());
}

uint8_t* SingleObjectPage::Data() noexcept {
    return data_;
}

uint8_t* SingleObjectPage::Allocate(size_t objectSizeBytes) noexcept {
    auto& heap = mm::GlobalData::Instance().allocator().impl().heap();
    heap.allocatedSizeTracker().recordDifference(static_cast<ptrdiff_t>(objectSizeBytes), false);
    return Data();
}

bool SingleObjectPage::SweepAndDestroy(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept {
    CustomAllocDebug("SingleObjectPage@%p::SweepAndDestroy()", this);
    if (SweepObject(Data(), finalizerQueue, sweepHandle)) {
        return true;
    }

    auto size = static_cast<ptrdiff_t>(objectSize());
    uint64_t cellCount = (size + sizeof(Cell) - 1) / sizeof(Cell);
    size = cellCount * sizeof(Cell); // FIXME what a mess!!!
    auto& heap = mm::GlobalData::Instance().allocator().impl().heap();
    heap.allocatedSizeTracker().recordDifference(-size, false);

    Free(this, pageSize()); // FIXME dangerous!!
    return false;
}

size_t SingleObjectPage::objectSize() noexcept {
    return CustomAllocator::GetAllocatedHeapSize(objAt(data_));
}

size_t SingleObjectPage::pageSize() noexcept {
    return sizeof(SingleObjectPage) + objectSize();
}

std::vector<uint8_t*> SingleObjectPage::GetAllocatedBlocks() noexcept {
    std::vector<uint8_t*> allocated;
    allocated.push_back(data_);
    return allocated;
}

} // namespace kotlin::alloc
