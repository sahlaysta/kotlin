/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <condition_variable>
#include <cstdint>
#include <mutex>

#include "ThreadRegistry.hpp"
#include "Utils.hpp"

namespace kotlin::gcScheduler::internal {

class MutatorAssists : private Pinned {
public:
    class ThreadData : private Pinned {
    public:
        ThreadData(MutatorAssists& owner, mm::ThreadData& thread) noexcept : owner_(owner), thread_(thread) {}

        void safePoint() noexcept;

    private:
        friend class MutatorAssists;

        bool completedEpoch(int64_t epoch) noexcept;

        MutatorAssists& owner_;
        mm::ThreadData& thread_;
        // Contains epoch * 2. The lower bit is 1, if completed waiting.
        std::atomic<int64_t> startedWaiting_ = 1;
    };

    void requestAssists(int64_t epoch) noexcept;

    template <typename F>
    void completeEpoch(int64_t epoch, F&& f) noexcept {
        markEpochCompleted(epoch);
        mm::ThreadRegistry::Instance().waitAllThreads(
                [f = std::forward<F>(f), epoch](mm::ThreadData& threadData) noexcept { return f(threadData).completedEpoch(epoch); });
    }

private:
    void markEpochCompleted(int64_t epoch) noexcept;

    std::atomic<int64_t> assistsEpoch_ = 0;
    std::atomic<int64_t> completedEpoch_ = 0;
    std::mutex m_;
    std::condition_variable cv_;
};

} // namespace kotlin::gcScheduler::internal
