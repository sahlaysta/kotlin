/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCSchedulerImpl.hpp"

#include "GC.hpp"
#include "GlobalData.hpp"

using namespace kotlin;

gcScheduler::GCScheduler::ThreadData::ThreadData(gcScheduler::GCScheduler&) noexcept : impl_(std_support::make_unique<Impl>()) {}

gcScheduler::GCScheduler::ThreadData::~ThreadData() = default;

gcScheduler::GCScheduler::GCScheduler() noexcept : gcData_(std_support::make_unique<internal::GCSchedulerDataManual>()) {}

ALWAYS_INLINE void gcScheduler::GCScheduler::ThreadData::safePoint() noexcept {}

void gcScheduler::GCScheduler::schedule() noexcept {
    RuntimeLogInfo({kTagGC}, "Scheduling GC manually");
    mm::GlobalData::Instance().gc().Schedule();
}

void gcScheduler::GCScheduler::scheduleAndWaitFinished() noexcept {
    RuntimeLogInfo({kTagGC}, "Scheduling GC manually");
    auto& gc = mm::GlobalData::Instance().gc();
    auto epoch = gc.Schedule();
    NativeOrUnregisteredThreadGuard guard(/* reentrant = */ true);
    gc.WaitFinished(epoch);
}

void gcScheduler::GCScheduler::scheduleAndWaitFinalized() noexcept {
    RuntimeLogInfo({kTagGC}, "Scheduling GC manually");
    auto& gc = mm::GlobalData::Instance().gc();
    auto epoch = gc.Schedule();
    NativeOrUnregisteredThreadGuard guard(/* reentrant = */ true);
    gc.WaitFinalizers(epoch);
}

ALWAYS_INLINE void gcScheduler::GCScheduler::onGCFinish(int64_t epoch, size_t aliveBytes) noexcept {}
