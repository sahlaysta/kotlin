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

namespace kotlin::gcScheduler {

namespace internal {

template <typename Clock>
class GCSchedulerDataAdaptive : public GCSchedulerData {
public:
    GCSchedulerDataAdaptive(GCSchedulerConfig& config, std::function<void()> scheduleGC) noexcept :
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
                scheduleGC_();
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
                if (scheduled_) return;
                scheduled_ = true;
                RuntimeLogDebug({kTagGC}, "Scheduling GC by allocation");
                scheduleGC_();
            case HeapGrowthController::MemoryBoundary::kHard:
                if (!scheduled_) {
                    scheduled_ = true;
                    scheduleGC_();
                }
                pauseMutators();
                RuntimeLogWarning({kTagGC}, "Scheduling GC by allocation");
        }
    }

    void safePoint() noexcept {
        kotlin::NativeOrUnregisteredThreadGuard guard(/* reentrant= */ true);
        while (stop_.load(std::memory_order_relaxed)) {
        }
    }

    void onGCFinish(int64_t epoch, size_t bytes) noexcept {
        heapGrowthController_.UpdateAliveSetBytes(bytes);
        scheduled_ = false;
        resumeMutators();
    }

    MutatorAssists& mutatorAssists() noexcept { return mutatorAssists_; }

private:
    void pauseMutators() noexcept {
        kotlin::NativeOrUnregisteredThreadGuard guard(/* reentrant= */ true);
        mm::SafePointActivator activator;
        stop_.store(true, std::memory_order_relaxed);
        safePoint();
    }

    void resumeMutators() noexcept {
        stop_.store(false, std::memory_order_relaxed);
        // TODO: Must wait for all to be released. GC thread cannot continue.
        //       This is the contract between GC and mutators. With regular native state
        //       each mutator must check that GC is not doing something. Here GC must check
        //       that each mutator is a-okay.
    }

    GCSchedulerConfig& config_;
    std::function<void()> scheduleGC_;
    mm::AppStateTracking& appStateTracking_;
    HeapGrowthController heapGrowthController_;
    RegularIntervalPacer<Clock> regularIntervalPacer_;
    RepeatedTimer<Clock> timer_;
    MutatorAssists mutatorAssists_;
    std::atomic<bool> scheduled_ = false;
    std::atomic<bool> stop_ = false;
};

}

class GCScheduler::ThreadData::Impl : private Pinned {
public:
    explicit Impl(GCSchedulerData& scheduler) noexcept : scheduler_(static_cast<internal::GCSchedulerDataAdaptive<steady_clock>&>(scheduler)), mutatorAssists_(scheduler_.mutatorAssists()) {}

    internal::GCSchedulerDataAdaptive<steady_clock>& scheduler() noexcept { return scheduler_; }

    internal::MutatorAssists::ThreadData& mutatorAssists() noexcept { return mutatorAssists_; }

private:
    internal::GCSchedulerDataAdaptive<steady_clock>& scheduler_;
    internal::MutatorAssists::ThreadData mutatorAssists_;
};

} // namespace kotlin::gcScheduler
