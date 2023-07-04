/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GCScheduler.hpp"

#include "Logging.hpp"

namespace kotlin::gcScheduler {

namespace internal {

class GCSchedulerDataManual : public GCSchedulerData {
public:
    GCSchedulerDataManual() noexcept { RuntimeLogInfo({kTagGC}, "Manual GC scheduler initialized"); }

    void OnPerformFullGC() noexcept override {}
    void SetAllocatedBytes(size_t bytes) noexcept override {}
};

} // namespace internal

class GCScheduler::ThreadData::Impl : private Pinned {};

} // namespace kotlin::gcScheduler
