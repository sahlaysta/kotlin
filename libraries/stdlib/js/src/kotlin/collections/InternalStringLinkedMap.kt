/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

internal class InternalStringLinkedMap<K, V> : InternalStringMap<K, V>() {
    companion object {
        private const val EMPTY_INDEX = -1
        private const val INITIAL_CAPACITY = 8
    }

    private var nextIndexes = IntArray(INITIAL_CAPACITY)
    private var prevIndexes = IntArray(INITIAL_CAPACITY)

    private var headIndex = EMPTY_INDEX
    private var tailIndex = EMPTY_INDEX

    private fun ensureCapacity() {
        if (nextIndexes.size < size) {
            val newCapacity = AbstractList.newCapacity(nextIndexes.size, size)
            nextIndexes = nextIndexes.copyOf(newCapacity)
            prevIndexes = prevIndexes.copyOf(newCapacity)
        }
    }

    override fun put(key: K, value: V): V? {
        val prevValue = super.put(key, value)
        // new element inserted
        if (prevValue == null) {
            ensureCapacity()
            if (size == 1) {
                headIndex = 0
                tailIndex = 0
                nextIndexes[0] = EMPTY_INDEX
                prevIndexes[0] = EMPTY_INDEX
            } else {
                val oldTail = tailIndex
                tailIndex = size - 1

                nextIndexes[oldTail] = size - 1
                prevIndexes[tailIndex] = oldTail
                nextIndexes[tailIndex] = EMPTY_INDEX
            }
        }
        return prevValue
    }

    override fun removeKeyIndex(key: K, removingIndex: Int) {
        super.removeKeyIndex(key, removingIndex)

        // remove removedIndex from the list
        val nextIndex = nextIndexes[removingIndex]
        val prevIndex = prevIndexes[removingIndex]

        nextIndexes[prevIndex] = nextIndex
        prevIndexes[nextIndex] = prevIndex

        if (headIndex == removingIndex) {
            headIndex = nextIndex
        }
        if (tailIndex == removingIndex) {
            tailIndex = prevIndex
        }

        // if the removed index from the middle, we have to move the last index on its place
        val lastIndex = size
        if (removingIndex != lastIndex) {
            nextIndexes[removingIndex] = nextIndexes[lastIndex]
            prevIndexes[removingIndex] = prevIndexes[lastIndex]
            nextIndexes[prevIndexes[removingIndex]] = removingIndex
            prevIndexes[nextIndexes[removingIndex]] = removingIndex
            if (headIndex == lastIndex) {
                headIndex = removingIndex
            }
            if (tailIndex == lastIndex) {
                tailIndex = removingIndex
            }
        }
    }

    override fun clear() {
        super.clear()
        headIndex = EMPTY_INDEX
        tailIndex = EMPTY_INDEX

        nextIndexes = IntArray(INITIAL_CAPACITY)
        prevIndexes = IntArray(INITIAL_CAPACITY)
    }

    override fun keysIterator(): MutableIterator<K> = KeysLinkedItr(this)
    override fun valuesIterator(): MutableIterator<V> = ValuesLinkedItr(this)
    override fun entriesIterator(): MutableIterator<MutableMap.MutableEntry<K, V>> = EntriesLinkedItr(this)

    private abstract class BaseLinkedItr<K, V>(protected val map: InternalStringLinkedMap<K, V>) {
        protected var lastIndex = EMPTY_INDEX
        protected var index = map.headIndex

        protected fun goNext() {
            if (index == EMPTY_INDEX) {
                throw NoSuchElementException()
            }
            lastIndex = index
            index = map.nextIndexes[index]
        }

        fun hasNext(): Boolean = index != EMPTY_INDEX

        fun remove() {
            map.removeKeyIndex(map.keys.getElement(lastIndex), lastIndex)
            if (index == map.size) {
                index = lastIndex
            }
            lastIndex = EMPTY_INDEX
        }
    }

    private abstract class LinkedItr<I, K, V>(
        private val iterableArray: JsRawArray<I>,
        map: InternalStringLinkedMap<K, V>,
    ) : MutableIterator<I>, BaseLinkedItr<K, V>(map) {
        override fun next(): I {
            goNext()
            return iterableArray.getElement(lastIndex)
        }
    }

    private class KeysLinkedItr<K, V>(map: InternalStringLinkedMap<K, V>) : LinkedItr<K, K, V>(map.keys, map)
    private class ValuesLinkedItr<K, V>(map: InternalStringLinkedMap<K, V>) : LinkedItr<V, K, V>(map.values, map)

    private class EntriesLinkedItr<K, V>(map: InternalStringLinkedMap<K, V>) :
        MutableIterator<MutableMap.MutableEntry<K, V>>,
        BaseLinkedItr<K, V>(map) {
        override fun next(): MutableMap.MutableEntry<K, V> {
            goNext()
            val key = map.keys.getElement(lastIndex)
            val value = map.values.getElement(lastIndex)
            return EntryRef(key, value, map)
        }
    }
}
