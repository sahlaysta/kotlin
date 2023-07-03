/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCSchedulerImpl.hpp"

#include "CallsChecker.hpp"
#include "GlobalData.hpp"
#include "Memory.h"
#include "Logging.hpp"
#include "Porting.h"

using namespace kotlin;

gcScheduler::GCScheduler::ThreadData::ThreadData(gcScheduler::GCScheduler& scheduler) noexcept : impl_(std_support::make_unique<Impl>(scheduler.gcData())) {}

gcScheduler::GCScheduler::ThreadData::~ThreadData() = default;

gcScheduler::GCScheduler::GCScheduler() noexcept :
    gcData_(std_support::make_unique<internal::GCSchedulerDataAdaptive<steady_clock>>(config_, []() noexcept {
        // This call acquires a lock, but the lock are always short-lived,
        // so we ignore thread state switching to avoid recursive safe points.
        CallsCheckerIgnoreGuard guard;
        mm::GlobalData::Instance().gc().Schedule();
    })) {}

ALWAYS_INLINE void gcScheduler::GCScheduler::ThreadData::safePoint() noexcept {
    impl().mutatorAssists().safePoint();
}

ALWAYS_INLINE void gcScheduler::GCScheduler::onGCFinish(int64_t epoch, size_t aliveBytes) noexcept {
    static_cast<internal::GCSchedulerDataAdaptive<steady_clock>&>(gcData()).onGCFinish(epoch, aliveBytes);
}
