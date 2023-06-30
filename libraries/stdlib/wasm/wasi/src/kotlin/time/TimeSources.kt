/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER") // TODO: Remove after bootstrap advance

package kotlin.time

import kotlin.time.TimeSource.Monotonic.ValueTimeMark
import kotlin.time.Duration.Companion.milliseconds

@SinceKotlin("1.3")
internal actual object MonotonicTimeSource : TimeSource.WithComparableMarks {
    actual override fun markNow(): ValueTimeMark = TODO("wasi")
    actual fun elapsedFrom(timeMark: ValueTimeMark): Duration = TODO("wasi")
    actual fun adjustReading(timeMark: ValueTimeMark, duration: Duration): ValueTimeMark = TODO("wasi")
    actual fun differenceBetween(one: ValueTimeMark, another: ValueTimeMark): Duration = TODO("wasi")
    override fun toString(): String = TODO("wasi")
}

@Suppress("ACTUAL_WITHOUT_EXPECT") // visibility
internal actual typealias ValueTimeMarkReading = Double