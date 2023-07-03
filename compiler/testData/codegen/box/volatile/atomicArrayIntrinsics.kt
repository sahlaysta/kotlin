// TARGET_BACKEND: NATIVE

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(kotlin.ExperimentalStdlibApi::class)

import kotlin.native.concurrent.*
import kotlin.concurrent.*
import kotlin.native.internal.*

class IntArrayWrapper(val arr: IntArray) {
    fun get(index: Int) = getArrayElement(arr, index)
}

fun testIntArray(): String {
    val wrap = IntArrayWrapper(intArrayOf(1, 2, 3))
    val res = wrap.get(2)
    return if (res == 2) "OK" else "FAILURE" + res
}

fun box() : String {
    return testIntArray()
}
