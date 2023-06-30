/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io

import kotlin.wasm.wasi.internal.*

internal fun printError(error: String?) {
    fd_write(2, listOf(error.toString().encodeToByteArray()))
}

private fun printlnImpl(message: String?) {
    fd_write(1, listOf((message.toString() + "\n").encodeToByteArray()))
}

private fun printImpl(message: String?) {
    fd_write(1, listOf(message.toString().encodeToByteArray()))
}

/** Prints the line separator to the standard output stream. */
public actual fun println() {
    fd_write(1, listOf(("\n").encodeToByteArray()))
}

/** Prints the given [message] and the line separator to the standard output stream. */
public actual fun println(message: Any?) {
    printlnImpl(message.toString())
}

/** Prints the given [message] to the standard output stream. */
public actual fun print(message: Any?) {
    printImpl(message.toString())
}

@SinceKotlin("1.6")
public actual fun readln(): String = TODO("wasi")

@SinceKotlin("1.6")
public actual fun readlnOrNull(): String? = TODO("wasi")