/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

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
    fun get(index: Int): Int = array.getArrayElement(index)

    /**
     * Atomically sets the value of the element at index[index] to the [given value][value].
     */
    fun set(index: Int, value: Int) = array.setArrayElement(index, value)

    /**
     * Atomically sets the value of the element at index[index] to the given [new value][newValue] and returns the old value.
     */
    fun getAndSet(index: Int, newValue: Int) = array.getAndSetArrayElement(index, newValue)

    /**
     * Atomically sets the value of the element at index[index] to the given [new value][newValue]
     * if the current value equals the [expected value][expected],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     */
    fun compareAndSet(index: Int, expected: Int, newValue: Int) = array.compareAndSetArrayElement(index, expected, newValue)

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
internal fun IntArray.getArrayElement(index: Int): Int = this.get(index)

internal fun IntArray.setArrayElement(index: Int, value: Int) = this.set(index, value)

internal fun IntArray.getAndSetArrayElement(index: Int, value: Int): Int = TODO()

internal fun IntArray.getAndAddArrayElement(index: Int, delta: Int): Int = TODO()

internal fun IntArray.compareAndSetArrayElement(index: Int, expectedValue: Int, newValue: Int): Boolean = TODO()
