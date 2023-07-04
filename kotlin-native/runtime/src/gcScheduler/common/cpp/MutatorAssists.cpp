/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MutatorAssists.hpp"

#include "KAssert.h"
#include "ThreadData.hpp"

using namespace kotlin;

void gcScheduler::internal::MutatorAssists::ThreadData::safePoint() noexcept {
    int64_t epoch = owner_.assistsEpoch_.load(std::memory_order_acquire);
    auto noNeedToWait = [this, epoch] { return owner_.completedEpoch_.load(std::memory_order_acquire) >= epoch; };
    if (noNeedToWait()) return;
    auto prevState = thread_.suspensionData().setStateNoSwitch(ThreadState::kNative);
    RuntimeAssert(prevState == ThreadState::kRunnable, "Expected runnable state");
    startedWaiting_.store(epoch, std::memory_order_release);
    {
        std::unique_lock guard(owner_.m_);
        owner_.cv_.wait(guard, [this, epoch]() noexcept { return owner_.completedEpoch_.load(std::memory_order_acquire) >= epoch; });
    }
    stoppedWaiting_.store(epoch, std::memory_order_release);
    // Not doing a safe point. We're a safe point.
    prevState = thread_.suspensionData().setStateNoSwitch(ThreadState::kRunnable);
    RuntimeAssert(prevState == ThreadState::kNative, "Expected native state");
}

bool gcScheduler::internal::MutatorAssists::ThreadData::completedEpoch(int64_t epoch) noexcept {
    // Didn't even start waiting.
    if (startedWaiting_.load(std::memory_order_acquire) < epoch) {
        return true;
    }
    // If stopped waiting this or one of the next epochs, we're done.
    return stoppedWaiting_.load(std::memory_order_acquire) >= epoch;
}

void gcScheduler::internal::MutatorAssists::requestAssists(int64_t epoch) noexcept {
    assistsEpoch_.store(epoch, std::memory_order_release);
}

void gcScheduler::internal::MutatorAssists::markEpochCompleted(int64_t epoch) noexcept {
    {
        std::unique_lock guard(m_);
        completedEpoch_.store(epoch, std::memory_order_release);
    }
    cv_.notify_all();
}
