/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GCScheduler.hpp"

#include <functional>

#include "GCSchedulerConfig.hpp"
#include "HeapGrowthController.hpp"
#include "Logging.hpp"
#include "MutatorAssists.hpp"
#include "SafePoint.hpp"
#include "SafePointTracker.hpp"
#include "ThreadData.hpp"

namespace kotlin::gcScheduler {

namespace internal {
class GCSchedulerDataAggressive;
}

class GCScheduler::ThreadData::Impl : private Pinned {
public:
    Impl(GCSchedulerData& scheduler, mm::ThreadData& thread) noexcept;

    internal::GCSchedulerDataAggressive& scheduler() noexcept { return scheduler_; }

    internal::MutatorAssists::ThreadData& mutatorAssists() noexcept { return mutatorAssists_; }

private:
    internal::GCSchedulerDataAggressive& scheduler_;
    internal::MutatorAssists::ThreadData mutatorAssists_;
};

namespace internal {

// The slowpath will trigger GC if this thread didn't meet this safepoint/allocation site before.
class GCSchedulerDataAggressive : public GCSchedulerData {
public:
    GCSchedulerDataAggressive(GCSchedulerConfig& config, std::function<int64_t()> scheduleGC) noexcept :
        scheduleGC_(std::move(scheduleGC)), heapGrowthController_(config) {
        RuntimeLogInfo({kTagGC}, "Aggressive GC scheduler initialized");
    }

    void OnPerformFullGC() noexcept override {}
    void SetAllocatedBytes(size_t bytes) noexcept override {
        // Still checking allocations: with a long running loop all safepoints
        // might be "met", so that's the only trigger to not run out of memory.
        auto boundary = heapGrowthController_.SetAllocatedBytes(bytes);
        switch (boundary) {
            case HeapGrowthController::MemoryBoundary::kNone:
                safePoint();
                return;
            case HeapGrowthController::MemoryBoundary::kSoft:
                RuntimeLogDebug({kTagGC}, "Scheduling GC by allocation");
                schedule();
                return;
            case HeapGrowthController::MemoryBoundary::kHard:
                RuntimeLogDebug({kTagGC}, "Scheduling GC by allocation");
                auto epoch = schedule();
                RuntimeLogWarning({kTagGC}, "Pausing the mutators");
                mutatorAssists_.requestAssists(epoch);
                return;
        }
    }

    void safePoint() noexcept {
        if (safePointTracker_.registerCurrentSafePoint(1)) {
            RuntimeLogDebug({kTagGC}, "Scheduling GC by safepoint");
            schedule();
        }
    }

    void onGCFinish(int64_t epoch, size_t aliveBytes) noexcept {
        heapGrowthController_.UpdateAliveSetBytes(aliveBytes);
        // Must wait for all mutators to be released. GC thread cannot continue.
        // This is the contract between GC and mutators. With regular native state
        // each mutator must check that GC is not doing something. Here GC must check
        // that each mutator has done all it needs.
        mutatorAssists_.completeEpoch(epoch, [](mm::ThreadData& threadData) noexcept -> MutatorAssists::ThreadData& {
            return threadData.gcScheduler().impl().mutatorAssists();
        });
    }

    int64_t schedule() noexcept { return scheduleGC_(); }

    MutatorAssists& mutatorAssists() noexcept { return mutatorAssists_; }

private:
    std::function<int64_t()> scheduleGC_;
    HeapGrowthController heapGrowthController_;
    SafePointTracker<> safePointTracker_;
    mm::SafePointActivator safePointActivator_;
    MutatorAssists mutatorAssists_;
};

} // namespace internal

} // namespace kotlin::gcScheduler
