/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.wasi.internal

import kotlin.wasm.unsafe.*

/**
 * Read command-line argument data.
 */
internal fun args_get(): List<String> {
    val (numArgs, bufSize) = args_sizes_get()
    val byteArrayBuf = ByteArray(bufSize)
    withScopedMemoryAllocator { allocator ->
        val argv = allocator.allocate(numArgs * PTR_SIZE)
        val argv_buf = allocator.allocate(bufSize)
        __unsafe__args_get(argv, argv_buf)
        val result = mutableListOf<String>()
        repeat(numArgs) { idx ->
            val argv_ptr = argv + idx * PTR_SIZE
            var ptr = Pointer(argv_ptr.loadInt().toUInt())
            val endIndex = readZeroTerminatedByteArray(ptr, byteArrayBuf)
            val str = byteArrayBuf.decodeToString(endIndex = endIndex)
            result += str
        }
        return result
    }
}

/**
 * Read environment variable data.
 */
internal fun environ_get(): Map<String, String> {
    val (numArgs, bufSize) = environ_sizes_get()
    val tmpByteArray = ByteArray(bufSize)
    withScopedMemoryAllocator { allocator ->
        val environ = allocator.allocate(numArgs * PTR_SIZE)
        val environ_buf = allocator.allocate(bufSize)
        __unsafe__environ_get(environ, environ_buf)
        val result = mutableMapOf<String, String>()
        repeat(numArgs) { idx ->
            val environ_ptr = environ + idx * PTR_SIZE
            var ptr = Pointer(environ_ptr.loadInt().toUInt())
            val endIdx = readZeroTerminatedByteArray(ptr, tmpByteArray)
            val str = tmpByteArray.decodeToString(endIndex = endIdx)
            val (key, value) = str.split("=", limit = 2)
            result[key] = value
        }
        return result
    }
}

/**
 * Read from a file descriptor, without using and updating the file descriptor's offset.
 * Note: This is similar to `preadv` in POSIX.
 * ## Parameters
 * `iovs` - List of scatter/gather vectors in which to store data.
 * `offset` - The offset within the file at which to read.
 * ## Return
 * The number of bytes read.
 */
internal fun fd_pread(
    fd: Fd,
    iovs: List<ByteArray>,
    offset: Filesize
): Size {
    withScopedMemoryAllocator { allocator ->
        val mapped_iovs = iovs.map { __unsafe__Iovec(allocator.writeToLinearMemory(it), it.size) }
        val result = __unsafe__fd_pread(allocator, fd, mapped_iovs, offset)
        return result
    }
}

internal fun fd_write(
    fd: Fd,
    ivos: List<ByteArray>
): Size {
    withScopedMemoryAllocator { allocator ->
        val iovs = ivos.map { __unsafe__Ciovec(allocator.writeToLinearMemory(it), it.size) }
        val res = __unsafe__fd_write(allocator, fd, iovs)
        return res
    }
}

internal fun fd_prestat_dir_name(fd: Fd): String {
    val path_len = (fd_prestat_get(fd) as Prestat.dir).value.pr_name_len
    withScopedMemoryAllocator { allocator ->
        val path = allocator.allocate(path_len)
        __unsafe__fd_prestat_dir_name(fd, path, path_len)
        return loadString(path, path_len)
    }
}

internal fun random_get(): Int {
    withScopedMemoryAllocator { allocator ->
        val memory = allocator.allocate(Int.SIZE_BYTES)
        __unsafe__random_get(memory, Int.SIZE_BYTES)
        return memory.loadInt()
    }
}
