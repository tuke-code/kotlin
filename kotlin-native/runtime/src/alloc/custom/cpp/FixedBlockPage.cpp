/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "FixedBlockPage.hpp"

#include <atomic>
#include <cstdint>
#include <cstring>
#include <random>

#include "CustomLogging.hpp"
#include "CustomAllocConstants.hpp"
#include "GCApi.hpp"

namespace kotlin::alloc {

FixedBlockPage* FixedBlockPage::Create(uint32_t blockSize) noexcept {
    CustomAllocInfo("FixedBlockPage::Create(%u)", blockSize);
    RuntimeAssert(blockSize <= FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE, "blockSize too large for FixedBlockPage");
    return new (SafeAlloc(FIXED_BLOCK_PAGE_SIZE())) FixedBlockPage(blockSize);
}

void FixedBlockPage::Destroy() noexcept {
    Free(this, FIXED_BLOCK_PAGE_SIZE());
}

FixedBlockPage::FixedBlockPage(uint32_t blockSize) noexcept : blockSize_(blockSize) {
    CustomAllocInfo("FixedBlockPage(%p)::FixedBlockPage(%u)", this, blockSize);
    nextFree_.first = 0;
    nextFree_.last = FIXED_BLOCK_PAGE_CELL_COUNT / blockSize * blockSize;
    end_ = FIXED_BLOCK_PAGE_CELL_COUNT / blockSize * blockSize;
}

// Computes the index of the bucket that the requested blockSize would end up
// in. Formally, it computes the number of unique bucket sizes smaller than
// BucketSize(blockSize).
ALWAYS_INLINE uint32_t FixedBlockPage::BucketIndex(uint32_t blockSize) noexcept {
    // If blockSize isn't big enough for the bucket to contain two sizes, then escape early
    if (blockSize < 2 << FIXED_BLOCK_PAGE_BUCKET_BIT_LENGTH) {
        return blockSize;
    }
    // Test if we have IEEE754 floating point on target CPU
    if constexpr (std::numeric_limits<float>::is_iec559) {
        // Convert to float
        float f = blockSize;
        // Take the raw bits of the floating point number. With C++20, this is std::bit_cast
        uint32_t bits = *(int*)&f;
        // Extract the exponent and BIT_LENGTH number of most significant bits of the fractional part
        uint32_t bucket = bits >> (23 - FIXED_BLOCK_PAGE_BUCKET_BIT_LENGTH);
        // Subtract the bias, so the buckets can start from 0 and align with the early escape
        bucket -= (128 << FIXED_BLOCK_PAGE_BUCKET_BIT_LENGTH);
        return bucket;
    } else {
        // Emulate the IEEE754 buckets, with bit length instead of exponent
        // Example for bit length 3
        // blockSize:  83 = 0b1010011
        // msb:               |- 9 -|
        // fraction, 3 bits: 0b010
        // bucket:     (9<<3) | 0b010
        // subtract:      4 << 3 = 32
        int msb = 31 - __builtin_clz(blockSize);
        int fraction = blockSize >> (msb - FIXED_BLOCK_PAGE_BUCKET_BIT_LENGTH) &FIXED_BLOCK_PAGE_BUCKET_BIT_MASK;
        int bucket = (msb << FIXED_BLOCK_PAGE_BUCKET_BIT_LENGTH) | fraction;
        bucket -= ((FIXED_BLOCK_PAGE_BUCKET_BIT_LENGTH - 1) << FIXED_BLOCK_PAGE_BUCKET_BIT_LENGTH);
        return bucket;
    }
}

// Rounds the requested blockSize up to the smallest bucket size where it fits.
// It keeps the (BIT_LENGTH+1) most significant bits intact, and sets the
// remaining less significant bits to 1.
ALWAYS_INLINE uint32_t FixedBlockPage::BucketSize(uint32_t blockSize) noexcept {
    uint32_t bucketSize = blockSize | (uint32_t(-1) >> (__builtin_clz(blockSize) + FIXED_BLOCK_PAGE_BUCKET_BIT_LENGTH + 1));
    return bucketSize;
}

ALWAYS_INLINE uint8_t* FixedBlockPage::TryAllocate(uint32_t blockSize) noexcept {
    RuntimeAssert(blockSize == blockSize_, "Trying to allocate block of size %d in the FixedBlockPage with block size %d", blockSize, blockSize_);
    uint32_t next = nextFree_.first;
    if (next < nextFree_.last) {
        nextFree_.first += blockSize;
        return cells_[next].data;
    }
    auto end = FIXED_BLOCK_PAGE_CELL_COUNT / blockSize * blockSize;
    if (next >= end) {
        return nullptr;
    }
    nextFree_ = cells_[next].nextFree;
    memset(&cells_[next], 0, sizeof(cells_[next]));
    return cells_[next].data;
}

void FixedBlockPage::OnPageOverflow() noexcept {
    RuntimeAssert(nextFree_.first >= end_, "Page must overflow");
    allocatedSizeTracker_.onPageOverflow(end_ * sizeof(FixedBlockCell));
}

bool FixedBlockPage::Sweep(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept {
    CustomAllocInfo("FixedBlockPage(%p)::Sweep()", this);
    FixedCellRange nextFree = nextFree_; // Accessing the previous free list structure.
    FixedCellRange* prevRange = &nextFree_; // Creating the new free list structure.
    uint32_t prevLive = -blockSize_;
    std::size_t aliveBlocksCount = 0;
    for (uint32_t cell = 0 ; cell < end_ ; cell += blockSize_) {
        // Go through the occupied cells.
        for (; cell < nextFree.first ; cell += blockSize_) {
            if (!SweepObject(cells_[cell].data, finalizerQueue, sweepHandle)) {
                // We should null this cell out, but we will do so in batch later.
                continue;
            }
            ++aliveBlocksCount;
            if (prevLive + blockSize_ < cell) {
                // We found an alive cell that ended a run of swept cells or a known unoccupied range.
                uint32_t prevCell = cell - blockSize_;
                // Nulling in batch.
                memset(&cells_[prevLive + blockSize_], 0, (prevCell - prevLive) * sizeof(FixedBlockCell));
                // Updating the free list structure.
                prevRange->first = prevLive + blockSize_;
                prevRange->last = prevCell;
                // And the next unoccupied range will be stored in the last unoccupied cell.
                prevRange = &cells_[prevCell].nextFree;
            }
            prevLive = cell;
        }
        // `cell` now points to a known unoccupied range.
        if (nextFree.last < end_) {
            cell = nextFree.last;
            nextFree = cells_[cell].nextFree;
            continue;
        }
        prevRange->first = prevLive + blockSize_;
        memset(&cells_[prevLive + blockSize_], 0, (cell - prevLive - blockSize_) * sizeof(FixedBlockCell));
        prevRange->last = end_;
        // And we're done.
        break;
    }

    allocatedSizeTracker_.afterSweep(aliveBlocksCount * blockSize_ * sizeof(FixedBlockCell));

    // The page is alive iff a range stored in the page header covers the entire page.
    return nextFree_.first > 0 || nextFree_.last < end_;
}

std::vector<uint8_t*> FixedBlockPage::GetAllocatedBlocks() noexcept {
    std::vector<uint8_t*> allocated;
    CustomAllocInfo("FixedBlockPage(%p)::Sweep()", this);
    FixedCellRange nextFree = nextFree_; // Accessing the previous free list structure.
    for (uint32_t cell = 0 ; cell < end_ ; cell += blockSize_) {
        for (; cell < nextFree.first ; cell += blockSize_) {
            allocated.push_back(cells_[cell].data);
        }
        if (nextFree.last >= end_) {
            break;
        }
        cell = nextFree.last;
        nextFree = cells_[cell].nextFree;
    }
    return allocated;
}

} // namespace kotlin::alloc
