/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <cstdint>

#include "ThreadRegistry.hpp"
#include "Utils.hpp"

namespace kotlin::gcScheduler::internal {

class MutatorAssists : private Pinned {
public:
    class ThreadData : private Pinned {
    public:
        explicit ThreadData(MutatorAssists& owner) noexcept : owner_(owner) {}

        void safePoint() noexcept;

    private:
        friend class MutatorAssists;

        MutatorAssists& owner_;
    };

    void requestAssists(int64_t epoch) noexcept;

    template <typename F>
    void completeEpoch(int64_t epoch, F&& f) noexcept {
        TODO();
        mm::ThreadRegistry::Instance().waitAllThreads([f = std::forward<F>(f), epoch](mm::ThreadData& threadData) noexcept {
            std::invoke(std::forward<F>(f), threadData).completedEpoch(epoch);
        });
    }

private:
};

} // namespace kotlin::gcScheduler::internal
