/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GCScheduler.hpp"

#include "AppStateTracking.hpp"
#include "GCSchedulerConfig.hpp"
#include "GlobalData.hpp"
#include "HeapGrowthController.hpp"
#include "Logging.hpp"
#include "MutatorAssists.hpp"
#include "RegularIntervalPacer.hpp"
#include "RepeatedTimer.hpp"
#include "SafePoint.hpp"
#include "ThreadData.hpp"

namespace kotlin::gcScheduler {

namespace internal {
template <typename Clock>
class GCSchedulerDataAdaptive;
}

class GCScheduler::ThreadData::Impl : private Pinned {
public:
    Impl(GCSchedulerData& scheduler, mm::ThreadData& thread) noexcept;

    internal::GCSchedulerDataAdaptive<steady_clock>& scheduler() noexcept { return scheduler_; }

    internal::MutatorAssists::ThreadData& mutatorAssists() noexcept { return mutatorAssists_; }

private:
    internal::GCSchedulerDataAdaptive<steady_clock>& scheduler_;
    internal::MutatorAssists::ThreadData mutatorAssists_;
};

namespace internal {

template <typename Clock>
class GCSchedulerDataAdaptive : public GCSchedulerData {
public:
    GCSchedulerDataAdaptive(GCSchedulerConfig& config, std::function<int64_t()> scheduleGC) noexcept :
        config_(config),
        scheduleGC_(std::move(scheduleGC)),
        appStateTracking_(mm::GlobalData::Instance().appStateTracking()),
        heapGrowthController_(config),
        regularIntervalPacer_(config),
        timer_("GC Timer thread", config_.regularGcInterval(), [this] {
            if (appStateTracking_.state() == mm::AppStateTracking::State::kBackground) {
                return;
            }
            if (regularIntervalPacer_.NeedsGC()) {
                RuntimeLogDebug({kTagGC}, "Scheduling GC by timer");
                schedule();
            }
        }) {
        RuntimeLogInfo({kTagGC}, "Adaptive GC scheduler initialized");
    }

    void OnPerformFullGC() noexcept override {
        regularIntervalPacer_.OnPerformFullGC();
        timer_.restart(config_.regularGcInterval());
    }

    void SetAllocatedBytes(size_t bytes) noexcept override {
        auto boundary = heapGrowthController_.SetAllocatedBytes(bytes);
        switch (boundary) {
            case HeapGrowthController::MemoryBoundary::kNone:
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

    void onGCFinish(int64_t epoch, size_t bytes) noexcept {
        heapGrowthController_.UpdateAliveSetBytes(bytes);
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
    GCSchedulerConfig& config_;
    std::function<int64_t()> scheduleGC_;
    mm::AppStateTracking& appStateTracking_;
    HeapGrowthController heapGrowthController_;
    RegularIntervalPacer<Clock> regularIntervalPacer_;
    RepeatedTimer<Clock> timer_;
    MutatorAssists mutatorAssists_;
};

} // namespace internal

} // namespace kotlin::gcScheduler
