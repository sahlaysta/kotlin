/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.config

enum class WasmTarget {
    JS,
    WASI;

    companion object {
        fun fromName(name: String): WasmTarget? = when (name) {
            "wasm-js" -> JS
            "wasm-wasi" -> WASI
            else -> null
        }
    }
}