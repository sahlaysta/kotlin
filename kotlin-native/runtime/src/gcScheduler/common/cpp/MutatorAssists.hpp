/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstdint>

#include "Utils.hpp"

namespace kotlin::gcScheduler::internal {

class MutatorAssists : private Pinned {
public:
    class ThreadData : private Pinned {
    public:
        explicit ThreadData(MutatorAssists& owner) noexcept;

        void safePoint() noexcept;

    private:
        MutatorAssists& owner_;
    };

    void enableAssists(int64_t epoch) noexcept;
    void disableAssists(int64_t epoch) noexcept;

private:
};

}
