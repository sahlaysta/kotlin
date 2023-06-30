/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.TimeSource.Monotonic.ValueTimeMark
import kotlin.wasm.wasi.internal.Clockid
import kotlin.wasm.wasi.internal.clock_time_get

@SinceKotlin("1.3")
internal actual object MonotonicTimeSource : TimeSource.WithComparableMarks {
    actual override fun markNow(): ValueTimeMark =
        ValueTimeMark(clock_time_get(Clockid.MONOTONIC, 1))

    actual fun elapsedFrom(timeMark: ValueTimeMark): Duration =
        (clock_time_get(Clockid.MONOTONIC, 1) - timeMark.reading).nanoseconds

    actual fun adjustReading(timeMark: ValueTimeMark, duration: Duration): ValueTimeMark =
        ValueTimeMark(timeMark.reading + duration.toLong(DurationUnit.NANOSECONDS))

    actual fun differenceBetween(one: ValueTimeMark, another: ValueTimeMark): Duration {
        val ms1 = one.reading
        val ms2 = another.reading
        return if (ms1 == ms2) Duration.ZERO else (ms1 - ms2).nanoseconds
    }

    override fun toString(): String = "WASI monotonic time source"
}

@Suppress("ACTUAL_WITHOUT_EXPECT") // visibility
internal actual typealias ValueTimeMarkReading = Long