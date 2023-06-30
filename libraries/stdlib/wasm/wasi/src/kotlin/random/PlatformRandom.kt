/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.random

import kotlin.wasm.wasi.internal.random_get

internal actual fun defaultPlatformRandom(): Random =
    Random(random_get())