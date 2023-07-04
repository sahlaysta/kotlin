/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GC.hpp"

#include "SameThreadMarkAndSweep.hpp"

namespace kotlin {
namespace gc {

using GCImpl = SameThreadMarkAndSweep;

class GC::Impl : private Pinned {
public:
    explicit Impl(gcScheduler::GCScheduler& gcScheduler) noexcept : gc_(objectFactory_, gcScheduler) {}

    mm::ObjectFactory<gc::GCImpl>& objectFactory() noexcept { return objectFactory_; }
    GCImpl& gc() noexcept { return gc_; }

private:
    mm::ObjectFactory<gc::GCImpl> objectFactory_;
    GCImpl gc_;
};

class GC::ThreadData::Impl : private Pinned {
public:
    Impl(GC& gc, mm::ThreadData& threadData) noexcept :
        gc_(gc.impl_->gc(), threadData), objectFactoryThreadQueue_(gc.impl_->objectFactory(), gc_.CreateAllocator()) {}

    GCImpl::ThreadData& gc() noexcept { return gc_; }
    mm::ObjectFactory<GCImpl>::ThreadQueue& objectFactoryThreadQueue() noexcept { return objectFactoryThreadQueue_; }

private:
    GCImpl::ThreadData gc_;
    mm::ObjectFactory<GCImpl>::ThreadQueue objectFactoryThreadQueue_;
};

} // namespace gc
} // namespace kotlin
