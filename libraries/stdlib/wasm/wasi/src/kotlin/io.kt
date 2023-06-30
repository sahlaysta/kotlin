/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io

import kotlin.wasm.wasi.internal.*
import kotlin.wasm.wasi.*

@Suppress("UNUSED_PARAMETER")
internal fun printError(error: String?): Unit = TODO("wasi")

@Suppress("UNUSED_PARAMETER")
private fun printlnImpl(message: String?): Unit {
    val output = message.toString() ?: ""
    fdWrite(StandardDescriptor.STDOUT, listOf(output.encodeToByteArray()))
}

@Suppress("UNUSED_PARAMETER")
private fun printImpl(message: String?): Unit {
    if (message != null) {
        fdWrite(StandardDescriptor.STDOUT, listOf(message.toString().encodeToByteArray()))
    }
}

/** Prints the line separator to the standard output stream. */
public actual fun println() {
    printlnImpl("")
}

/** Prints the given [message] and the line separator to the standard output stream. */
public actual fun println(message: Any?) {
    printlnImpl(message?.toString())
}

/** Prints the given [message] to the standard output stream. */
public actual fun print(message: Any?) {
    printImpl(message?.toString())
}

@SinceKotlin("1.6")
public actual fun readln(): String = TODO("wasi")

@SinceKotlin("1.6")
public actual fun readlnOrNull(): String? = TODO("wasi")