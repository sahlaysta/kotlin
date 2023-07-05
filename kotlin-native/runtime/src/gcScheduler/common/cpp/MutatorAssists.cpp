/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MutatorAssists.hpp"

#include "KAssert.h"
#include "Logging.hpp"
#include "ThreadData.hpp"

using namespace kotlin;

void gcScheduler::internal::MutatorAssists::ThreadData::safePoint() noexcept {
    int64_t epoch = owner_.assistsEpoch_.load(std::memory_order_acquire);
    auto noNeedToWait = [this, epoch] { return owner_.completedEpoch_.load(std::memory_order_acquire) >= epoch; };
    if (noNeedToWait()) return;
    auto prevState = thread_.suspensionData().setStateNoSwitch(ThreadState::kNative);
    RuntimeAssert(prevState == ThreadState::kRunnable, "Expected runnable state");
    startedWaiting_.store(epoch * 2, std::memory_order_release);
    {
        std::unique_lock guard(owner_.m_);
        RuntimeLogDebug({kTagGC}, "Thread is assisting for epoch %" PRId64, epoch);
        owner_.cv_.wait(guard, noNeedToWait);
        RuntimeLogDebug({kTagGC}, "Thread has assisted for epoch %" PRId64, epoch);
    }
    startedWaiting_.store(epoch * 2 + 1, std::memory_order_release);
    // Not doing a safe point. We're a safe point.
    prevState = thread_.suspensionData().setStateNoSwitch(ThreadState::kRunnable);
    RuntimeAssert(prevState == ThreadState::kNative, "Expected native state");
}

bool gcScheduler::internal::MutatorAssists::ThreadData::completedEpoch(int64_t epoch) noexcept {
    auto value = startedWaiting_.load(std::memory_order_acquire);
    auto waitingEpoch = value / 2;
    bool isWaiting = value % 2 == 0;
    if (waitingEpoch > epoch)
        // Waiting for an epoch bigger than `epoch` => `epoch` is done here.
        return true;
    return !isWaiting;
}

void gcScheduler::internal::MutatorAssists::requestAssists(int64_t epoch) noexcept {
    RuntimeLogDebug({kTagGC}, "Enabling assists for epoch %" PRId64, epoch);
    assistsEpoch_.store(epoch, std::memory_order_release);
}

void gcScheduler::internal::MutatorAssists::markEpochCompleted(int64_t epoch) noexcept {
    RuntimeLogDebug({kTagGC}, "Disabling assists for epoch %" PRId64, epoch);
    {
        std::unique_lock guard(m_);
        completedEpoch_.store(epoch, std::memory_order_release);
    }
    cv_.notify_all();
}
