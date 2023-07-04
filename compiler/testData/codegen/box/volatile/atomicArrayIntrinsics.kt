// TARGET_BACKEND: NATIVE

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(kotlin.ExperimentalStdlibApi::class)

import kotlin.native.concurrent.*
import kotlin.concurrent.*
import kotlin.native.internal.*

class AtomicArrayTest {
    private val intArr = intArrayOf(1, 2, 3, 4, 5, 6)

    fun atomicGet(index: Int) = getArrayElement(intArr, index)

    fun testAtomicIntArray() {
        val res = atomicGet(1)
    }
}

fun box() : String {
    val testClass = AtomicArrayTest()
    testClass.testAtomicIntArray()
    val res = testClass.atomicGet(3)
    return "OK$res"
}
