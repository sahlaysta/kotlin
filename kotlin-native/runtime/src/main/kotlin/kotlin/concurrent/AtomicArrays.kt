/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

import kotlin.native.internal.*
import kotlin.reflect.*
import kotlin.concurrent.*
import kotlin.native.concurrent.*

/**
 * An array of [Int] values that are always updated atomically.
 */
class AtomicIntegerArray {
    private var array: IntArray

    /**
     * Creates a new [AtomicIntegerArray] of the specified [size], with all elements initialized to zero.
     */
    constructor(size: Int) {
        array = IntArray(size)
    }

    /**
     * Creates a new [AtomicIntegerArray] with elements copied from the given [array] of [Int] values.
     */
    constructor(array: IntArray) {
        this.array = IntArray(array.size) { i -> array[i] }
    }

    /**
     * Returns the size of the array.
     */
    fun length(): Int = array.size

    /**
     * Atomically reads the current value of the element at the given [index].
     */
    fun get(index: Int): Int = array.get(index)

    /**
     * Atomically sets the value of the element at index[index] to the [given value][value].
     */
    fun set(index: Int, value: Int) = setArrayElement(array, index, value)

    /**
     * Atomically sets the value of the element at index[index] to the given [new value][newValue] and returns the old value.
     */
    fun getAndSet(index: Int, newValue: Int) = array.getAndSetArrayElement(index, newValue)

    /**
     * Atomically increments the current value of the element at index[index] and returns the old value.
     */
    fun getAndIncrement(index: Int): Int = array.getAndAddArrayElement(index, 1)

    /**
     * Atomically decrements the current value of the element at index[index] and returns the old value.
     */
    fun getAndDecrement(index: Int): Int = array.getAndAddArrayElement(index, -1)

    /**
     * Atomically adds the [given value][delta] to the current value of the element at index[index] and returns the old value.
     */
    fun getAndAdd(index: Int, delta: Int): Int = array.getAndAddArrayElement(index, delta)
}

// 1. todo pass IntArray as first arg -> IntrinsicGenerator
@TypedIntrinsic(IntrinsicType.ATOMIC_GET_ARRAY_ELEMENT)
internal external fun getArrayElement(array: IntArray, index: Int): Int

//@TypedIntrinsic(IntrinsicType.ATOMIC_SET_ARRAY_ELEMENT)
internal fun setArrayElement(array: IntArray, index: Int, value: Int) = array.set(index, value)

//@TypedIntrinsic(IntrinsicType.ATOMIC_GET_ARRAY_ELEMENT)
//internal external fun compareAndSetArrayElement(array: IntArray, index: Int, expectedValue: Int, newValue: Int): Boolean

internal fun IntArray.getAndSetArrayElement(index: Int, value: Int): Int = TODO("Not implemented" + index + value)

internal fun IntArray.getAndAddArrayElement(index: Int, delta: Int): Int = TODO("Not implemented" + index + delta)
