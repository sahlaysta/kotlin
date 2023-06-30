/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

 */

package kotlin.wasm.wasi.internal

import kotlin.wasm.WasmImport
import kotlin.wasm.unsafe.*

internal typealias Size = Int

internal typealias Filesize = Long

internal typealias Timestamp = Long

internal enum class Clockid {
    /**
     * The clock measuring real time. Time value zero corresponds with 1970-01-01T00:00:00Z.
     */
    REALTIME,

    /**
     * The store-wide monotonic clock, which is defined as a clock measuring real time, whose value
     * cannot be adjusted and which cannot have negative clock jumps. The epoch of this clock is
     * undefined. The absolute time value of this clock therefore has no meaning.
     */
    MONOTONIC,

    /**
     * The CPU-time clock associated with the current process.
     */
    PROCESS_CPUTIME_ID,

    /**
     * The CPU-time clock associated with the current thread.
     */
    THREAD_CPUTIME_ID,
}

internal enum class Errno {
    /**
     * No error occurred. System call completed successfully.
     */
    SUCCESS,

    /**
     * Argument list too long.
     */
    _2BIG,

    /**
     * Permission denied.
     */
    ACCES,

    /**
     * Address in use.
     */
    ADDRINUSE,

    /**
     * Address not available.
     */
    ADDRNOTAVAIL,

    /**
     * Address family not supported.
     */
    AFNOSUPPORT,

    /**
     * Resource unavailable, or operation would block.
     */
    AGAIN,

    /**
     * Connection already in progress.
     */
    ALREADY,

    /**
     * Bad file descriptor.
     */
    BADF,

    /**
     * Bad message.
     */
    BADMSG,

    /**
     * Device or resource busy.
     */
    BUSY,

    /**
     * Operation canceled.
     */
    CANCELED,

    /**
     * No child processes.
     */
    CHILD,

    /**
     * Connection aborted.
     */
    CONNABORTED,

    /**
     * Connection refused.
     */
    CONNREFUSED,

    /**
     * Connection reset.
     */
    CONNRESET,

    /**
     * Resource deadlock would occur.
     */
    DEADLK,

    /**
     * Destination address required.
     */
    DESTADDRREQ,

    /**
     * Mathematics argument out of domain of function.
     */
    DOM,

    /**
     * Reserved.
     */
    DQUOT,

    /**
     * File exists.
     */
    EXIST,

    /**
     * Bad address.
     */
    FAULT,

    /**
     * File too large.
     */
    FBIG,

    /**
     * Host is unreachable.
     */
    HOSTUNREACH,

    /**
     * Identifier removed.
     */
    IDRM,

    /**
     * Illegal byte sequence.
     */
    ILSEQ,

    /**
     * Operation in progress.
     */
    INPROGRESS,

    /**
     * Interrupted function.
     */
    INTR,

    /**
     * Invalid argument.
     */
    INVAL,

    /**
     * I/O error.
     */
    IO,

    /**
     * Socket is connected.
     */
    ISCONN,

    /**
     * Is a directory.
     */
    ISDIR,

    /**
     * Too many levels of symbolic links.
     */
    LOOP,

    /**
     * File descriptor value too large.
     */
    MFILE,

    /**
     * Too many links.
     */
    MLINK,

    /**
     * Message too large.
     */
    MSGSIZE,

    /**
     * Reserved.
     */
    MULTIHOP,

    /**
     * Filename too long.
     */
    NAMETOOLONG,

    /**
     * Network is down.
     */
    NETDOWN,

    /**
     * Connection aborted by network.
     */
    NETRESET,

    /**
     * Network unreachable.
     */
    NETUNREACH,

    /**
     * Too many files open in system.
     */
    NFILE,

    /**
     * No buffer space available.
     */
    NOBUFS,

    /**
     * No such device.
     */
    NODEV,

    /**
     * No such file or directory.
     */
    NOENT,

    /**
     * Executable file format error.
     */
    NOEXEC,

    /**
     * No locks available.
     */
    NOLCK,

    /**
     * Reserved.
     */
    NOLINK,

    /**
     * Not enough space.
     */
    NOMEM,

    /**
     * No message of the desired type.
     */
    NOMSG,

    /**
     * Protocol not available.
     */
    NOPROTOOPT,

    /**
     * No space left on device.
     */
    NOSPC,

    /**
     * Function not supported.
     */
    NOSYS,

    /**
     * The socket is not connected.
     */
    NOTCONN,

    /**
     * Not a directory or a symbolic link to a directory.
     */
    NOTDIR,

    /**
     * Directory not empty.
     */
    NOTEMPTY,

    /**
     * State not recoverable.
     */
    NOTRECOVERABLE,

    /**
     * Not a socket.
     */
    NOTSOCK,

    /**
     * Not supported, or operation not supported on socket.
     */
    NOTSUP,

    /**
     * Inappropriate I/O control operation.
     */
    NOTTY,

    /**
     * No such device or address.
     */
    NXIO,

    /**
     * Value too large to be stored in data type.
     */
    OVERFLOW,

    /**
     * Previous owner died.
     */
    OWNERDEAD,

    /**
     * Operation not permitted.
     */
    PERM,

    /**
     * Broken pipe.
     */
    PIPE,

    /**
     * Protocol error.
     */
    PROTO,

    /**
     * Protocol not supported.
     */
    PROTONOSUPPORT,

    /**
     * Protocol wrong type for socket.
     */
    PROTOTYPE,

    /**
     * Result too large.
     */
    RANGE,

    /**
     * Read-only file system.
     */
    ROFS,

    /**
     * Invalid seek.
     */
    SPIPE,

    /**
     * No such process.
     */
    SRCH,

    /**
     * Reserved.
     */
    STALE,

    /**
     * Connection timed out.
     */
    TIMEDOUT,

    /**
     * Text file busy.
     */
    TXTBSY,

    /**
     * Cross-device link.
     */
    XDEV,

    /**
     * Extension: Capabilities insufficient.
     */
    NOTCAPABLE,
}

internal typealias Rights = Long

internal object RIGHTS {
    /**
     * The right to invoke `fd_datasync`. If `path_open` is set, includes the right to invoke
     * `path_open` with `fdflags::dsync`.

     */
    const val FD_DATASYNC: Rights = 1

    /**
     * The right to invoke `fd_read` and `sock_recv`. If `rights::fd_seek` is set, includes the right
     * to invoke `fd_pread`.

     */
    const val FD_READ: Rights = 2

    /**
     * The right to invoke `fd_seek`. This flag implies `rights::fd_tell`.
     */
    const val FD_SEEK: Rights = 4

    /**
     * The right to invoke `fd_fdstat_set_flags`.
     */
    const val FD_FDSTAT_SET_FLAGS: Rights = 8

    /**
     * The right to invoke `fd_sync`. If `path_open` is set, includes the right to invoke `path_open`
     * with `fdflags::rsync` and `fdflags::dsync`.

     */
    const val FD_SYNC: Rights = 16

    /**
     * The right to invoke `fd_seek` in such a way that the file offset remains unaltered (i.e.,
     * `whence::cur` with offset zero), or to invoke `fd_tell`.

     */
    const val FD_TELL: Rights = 32

    /**
     * The right to invoke `fd_write` and `sock_send`. If `rights::fd_seek` is set, includes the right
     * to invoke `fd_pwrite`.

     */
    const val FD_WRITE: Rights = 64

    /**
     * The right to invoke `fd_advise`.
     */
    const val FD_ADVISE: Rights = 128

    /**
     * The right to invoke `fd_allocate`.
     */
    const val FD_ALLOCATE: Rights = 256

    /**
     * The right to invoke `path_create_directory`.
     */
    const val PATH_CREATE_DIRECTORY: Rights = 512

    /**
     * If `path_open` is set, the right to invoke `path_open` with `oflags::creat`.
     */
    const val PATH_CREATE_FILE: Rights = 20

    /**
     * The right to invoke `path_link` with the file descriptor as the source directory.
     */
    const val PATH_LINK_SOURCE: Rights = 21

    /**
     * The right to invoke `path_link` with the file descriptor as the target directory.
     */
    const val PATH_LINK_TARGET: Rights = 22

    /**
     * The right to invoke `path_open`.
     */
    const val PATH_OPEN: Rights = 23

    /**
     * The right to invoke `fd_readdir`.
     */
    const val FD_READDIR: Rights = 24

    /**
     * The right to invoke `path_readlink`.
     */
    const val PATH_READLINK: Rights = 25

    /**
     * The right to invoke `path_rename` with the file descriptor as the source directory.
     */
    const val PATH_RENAME_SOURCE: Rights = 26

    /**
     * The right to invoke `path_rename` with the file descriptor as the target directory.
     */
    const val PATH_RENAME_TARGET: Rights = 27

    /**
     * The right to invoke `path_filestat_get`.
     */
    const val PATH_FILESTAT_GET: Rights = 28

    /**
     * The right to change a file's size (there is no `path_filestat_set_size`). If `path_open` is
     * set, includes the right to invoke `path_open` with `oflags::trunc`.

     */
    const val PATH_FILESTAT_SET_SIZE: Rights = 29

    /**
     * The right to invoke `path_filestat_set_times`.
     */
    const val PATH_FILESTAT_SET_TIMES: Rights = 40

    /**
     * The right to invoke `fd_filestat_get`.
     */
    const val FD_FILESTAT_GET: Rights = 41

    /**
     * The right to invoke `fd_filestat_set_size`.
     */
    const val FD_FILESTAT_SET_SIZE: Rights = 42

    /**
     * The right to invoke `fd_filestat_set_times`.
     */
    const val FD_FILESTAT_SET_TIMES: Rights = 43

    /**
     * The right to invoke `path_symlink`.
     */
    const val PATH_SYMLINK: Rights = 44

    /**
     * The right to invoke `path_remove_directory`.
     */
    const val PATH_REMOVE_DIRECTORY: Rights = 45

    /**
     * The right to invoke `path_unlink_file`.
     */
    const val PATH_UNLINK_FILE: Rights = 46

    /**
     * If `rights::fd_read` is set, includes the right to invoke `poll_oneoff` to subscribe to
     * `eventtype::fd_read`. If `rights::fd_write` is set, includes the right to invoke `poll_oneoff`
     * to subscribe to `eventtype::fd_write`.

     */
    const val POLL_FD_READWRITE: Rights = 47

    /**
     * The right to invoke `sock_shutdown`.
     */
    const val SOCK_SHUTDOWN: Rights = 48

    /**
     * The right to invoke `sock_accept`.
     */
    const val SOCK_ACCEPT: Rights = 49
}

typealias Fd = Int

internal data class __unsafe__Iovec(
    /**
     * The address of the buffer to be filled.
     */
    var buf: Pointer,
    /*<Byte>*/
    /**
     * The length of the buffer to be filled.
     */
    var buf_len: Size,
)

internal fun __load___unsafe__Iovec(ptr: Pointer): __unsafe__Iovec {
    return __unsafe__Iovec(
        Pointer((ptr + 0).loadInt().toUInt()),
        (ptr + 4).loadInt(),
    )
}

internal fun __store___unsafe__Iovec(x: __unsafe__Iovec, ptr: Pointer) {
    (ptr + 0).storeInt(x.buf.address.toInt())
    (ptr + 4).storeInt(x.buf_len)
}

internal data class __unsafe__Ciovec(
    /**
     * The address of the buffer to be written.
     */
    var buf: Pointer,
    /*<Byte>*/
    /**
     * The length of the buffer to be written.
     */
    var buf_len: Size,
)

internal fun __load___unsafe__Ciovec(ptr: Pointer): __unsafe__Ciovec {
    return __unsafe__Ciovec(
        Pointer((ptr + 0).loadInt().toUInt()),
        (ptr + 4).loadInt(),
    )
}

internal fun __store___unsafe__Ciovec(x: __unsafe__Ciovec, ptr: Pointer) {
    (ptr + 0).storeInt(x.buf.address.toInt())
    (ptr + 4).storeInt(x.buf_len)
}

internal typealias __unsafe__IovecArray = List<__unsafe__Iovec>

internal typealias __unsafe__CiovecArray = List<__unsafe__Ciovec>

internal typealias Filedelta = Long

internal enum class Whence {
    /**
     * Seek relative to start-of-file.
     */
    SET,

    /**
     * Seek relative to current position.
     */
    CUR,

    /**
     * Seek relative to end-of-file.
     */
    END,
}

internal typealias Dircookie = Long

internal typealias Dirnamlen = Int

internal typealias Inode = Long

internal enum class Filetype {
    /**
     * The type of the file descriptor or file is unknown or is different from any of the other types
     * specified.

     */
    UNKNOWN,

    /**
     * The file descriptor or file refers to a block device inode.
     */
    BLOCK_DEVICE,

    /**
     * The file descriptor or file refers to a character device inode.
     */
    CHARACTER_DEVICE,

    /**
     * The file descriptor or file refers to a directory inode.
     */
    DIRECTORY,

    /**
     * The file descriptor or file refers to a regular file inode.
     */
    REGULAR_FILE,

    /**
     * The file descriptor or file refers to a datagram socket.
     */
    SOCKET_DGRAM,

    /**
     * The file descriptor or file refers to a byte-stream socket.
     */
    SOCKET_STREAM,

    /**
     * The file refers to a symbolic link inode.
     */
    SYMBOLIC_LINK,
}

internal data class Dirent(
    /**
     * The offset of the next directory entry stored in this directory.
     */
    var d_next: Dircookie,
    /**
     * The serial number of the file referred to by this directory entry.
     */
    var d_ino: Inode,
    /**
     * The length of the name of the directory entry.
     */
    var d_namlen: Dirnamlen,
    /**
     * The type of the file referred to by this directory entry.
     */
    var d_type: Filetype,
)

internal fun __load_Dirent(ptr: Pointer): Dirent {
    return Dirent(
        (ptr + 0).loadLong(),
        (ptr + 8).loadLong(),
        (ptr + 16).loadInt(),
        Filetype.values()[(ptr + 20).loadByte().toInt()],
    )
}

internal fun __store_Dirent(x: Dirent, ptr: Pointer) {
    (ptr + 0).storeLong(x.d_next)
    (ptr + 8).storeLong(x.d_ino)
    (ptr + 16).storeInt(x.d_namlen)
    (ptr + 20).storeByte(x.d_type.ordinal.toByte())
}

internal enum class Advice {
    /**
     * The application has no advice to give on its behavior with respect to the specified data.
     */
    NORMAL,

    /**
     * The application expects to access the specified data sequentially from lower offsets to higher
     * offsets.

     */
    SEQUENTIAL,

    /**
     * The application expects to access the specified data in a random order.
     */
    RANDOM,

    /**
     * The application expects to access the specified data in the near future.
     */
    WILLNEED,

    /**
     * The application expects that it will not access the specified data in the near future.
     */
    DONTNEED,

    /**
     * The application expects to access the specified data once and then not reuse it thereafter.
     */
    NOREUSE,
}

internal typealias Fdflags = Short

internal object FDFLAGS {
    /**
     * Append mode: Data written to the file is always appended to the file's end.
     */
    const val APPEND: Fdflags = 1

    /**
     * Write according to synchronized I/O data integrity completion. Only the data stored in the file
     * is synchronized.

     */
    const val DSYNC: Fdflags = 2

    /**
     * Non-blocking mode.
     */
    const val NONBLOCK: Fdflags = 4

    /**
     * Synchronized read I/O operations.
     */
    const val RSYNC: Fdflags = 8

    /**
     * Write according to synchronized I/O file integrity completion. In addition to synchronizing the
     * data stored in the file, the implementation may also synchronously update the file's metadata.

     */
    const val SYNC: Fdflags = 16
}

internal data class Fdstat(
    /**
     * File type.
     */
    var fs_filetype: Filetype,
    /**
     * File descriptor flags.
     */
    var fs_flags: Fdflags,
    /**
     * Rights that apply to this file descriptor.
     */
    var fs_rights_base: Rights,
    /**
     * Maximum set of rights that may be installed on new file descriptors that are created through
     * this file descriptor, e.g., through `path_open`.

     */
    var fs_rights_inheriting: Rights,
)

internal fun __load_Fdstat(ptr: Pointer): Fdstat {
    return Fdstat(
        Filetype.values()[(ptr + 0).loadByte().toInt()],
        (ptr + 2).loadShort(),
        (ptr + 8).loadLong(),
        (ptr + 16).loadLong(),
    )
}

internal fun __store_Fdstat(x: Fdstat, ptr: Pointer) {
    (ptr + 0).storeByte(x.fs_filetype.ordinal.toByte())
    (ptr + 2).storeShort(x.fs_flags)
    (ptr + 8).storeLong(x.fs_rights_base)
    (ptr + 16).storeLong(x.fs_rights_inheriting)
}

internal typealias Device = Long

internal typealias Fstflags = Short

internal object FSTFLAGS {
    /**
     * Adjust the last data access timestamp to the value stored in `filestat::atim`.
     */
    const val ATIM: Fstflags = 1

    /**
     * Adjust the last data access timestamp to the time of clock `clockid::realtime`.
     */
    const val ATIM_NOW: Fstflags = 2

    /**
     * Adjust the last data modification timestamp to the value stored in `filestat::mtim`.
     */
    const val MTIM: Fstflags = 4

    /**
     * Adjust the last data modification timestamp to the time of clock `clockid::realtime`.
     */
    const val MTIM_NOW: Fstflags = 8
}

internal typealias Lookupflags = Int

internal object LOOKUPFLAGS {
    /**
     * As long as the resolved path corresponds to a symbolic link, it is expanded.
     */
    const val SYMLINK_FOLLOW: Lookupflags = 1
}

internal typealias Oflags = Short

internal object OFLAGS {
    /**
     * Create file if it does not exist.
     */
    const val CREAT: Oflags = 1

    /**
     * Fail if not a directory.
     */
    const val DIRECTORY: Oflags = 2

    /**
     * Fail if file already exists.
     */
    const val EXCL: Oflags = 4

    /**
     * Truncate file to size 0.
     */
    const val TRUNC: Oflags = 8
}

internal typealias Linkcount = Long

internal data class Filestat(
    /**
     * Device ID of device containing the file.
     */
    var dev: Device,
    /**
     * File serial number.
     */
    var ino: Inode,
    /**
     * File type.
     */
    var filetype: Filetype,
    /**
     * Number of hard links to the file.
     */
    var nlink: Linkcount,
    /**
     * For regular files, the file size in bytes. For symbolic links, the length in bytes of the
     * pathname contained in the symbolic link.

     */
    var size: Filesize,
    /**
     * Last data access timestamp.
     */
    var atim: Timestamp,
    /**
     * Last data modification timestamp.
     */
    var mtim: Timestamp,
    /**
     * Last file status change timestamp.
     */
    var ctim: Timestamp,
)

internal fun __load_Filestat(ptr: Pointer): Filestat {
    return Filestat(
        (ptr + 0).loadLong(),
        (ptr + 8).loadLong(),
        Filetype.values()[(ptr + 16).loadByte().toInt()],
        (ptr + 24).loadLong(),
        (ptr + 32).loadLong(),
        (ptr + 40).loadLong(),
        (ptr + 48).loadLong(),
        (ptr + 56).loadLong(),
    )
}

internal fun __store_Filestat(x: Filestat, ptr: Pointer) {
    (ptr + 0).storeLong(x.dev)
    (ptr + 8).storeLong(x.ino)
    (ptr + 16).storeByte(x.filetype.ordinal.toByte())
    (ptr + 24).storeLong(x.nlink)
    (ptr + 32).storeLong(x.size)
    (ptr + 40).storeLong(x.atim)
    (ptr + 48).storeLong(x.mtim)
    (ptr + 56).storeLong(x.ctim)
}

internal typealias Userdata = Long

internal enum class Eventtype {
    /**
     * The time value of clock `subscription_clock::id` has reached timestamp
     * `subscription_clock::timeout`.

     */
    CLOCK,

    /**
     * File descriptor `subscription_fd_readwrite::file_descriptor` has data available for reading.
     * This event always triggers for regular files.

     */
    FD_READ,

    /**
     * File descriptor `subscription_fd_readwrite::file_descriptor` has capacity available for
     * writing. This event always triggers for regular files.

     */
    FD_WRITE,
}

internal typealias Eventrwflags = Short

internal object EVENTRWFLAGS {
    /**
     * The peer of this socket has closed or disconnected.
     */
    const val FD_READWRITE_HANGUP: Eventrwflags = 1
}

internal data class EventFdReadwrite(
    /**
     * The number of bytes available for reading or writing.
     */
    var nbytes: Filesize,
    /**
     * The state of the file descriptor.
     */
    var flags: Eventrwflags,
)

internal fun __load_EventFdReadwrite(ptr: Pointer): EventFdReadwrite {
    return EventFdReadwrite(
        (ptr + 0).loadLong(),
        (ptr + 8).loadShort(),
    )
}

internal fun __store_EventFdReadwrite(x: EventFdReadwrite, ptr: Pointer) {
    (ptr + 0).storeLong(x.nbytes)
    (ptr + 8).storeShort(x.flags)
}

internal data class Event(
    /**
     * User-provided value that got attached to `subscription::userdata`.
     */
    var userdata: Userdata,
    /**
     * If non-zero, an error that occurred while processing the subscription request.
     */
    var error: Errno,
    /**
     * The type of event that occured*/
    var type: Eventtype,
    /**
     * The contents of the event, if it is an `eventtype::fd_read` or `eventtype::fd_write`.
     * `eventtype::clock` events ignore this field.
     */
    var fd_readwrite: EventFdReadwrite,
)

internal fun __load_Event(ptr: Pointer): Event {
    return Event(
        (ptr + 0).loadLong(),
        Errno.values()[(ptr + 8).loadShort().toInt()],
        Eventtype.values()[(ptr + 10).loadByte().toInt()],
        EventFdReadwrite(
            (ptr + 16 + 0).loadLong(),
            (ptr + 16 + 8).loadShort(),
        ),
    )
}

internal fun __store_Event(x: Event, ptr: Pointer) {
    (ptr + 0).storeLong(x.userdata)
    (ptr + 8).storeShort(x.error.ordinal.toShort())
    (ptr + 10).storeByte(x.type.ordinal.toByte())
    (ptr + 16 + 0).storeLong(x.fd_readwrite.nbytes)
    (ptr + 16 + 8).storeShort(x.fd_readwrite.flags)
}

internal typealias Subclockflags = Short

internal object SUBCLOCKFLAGS {
    /**
     * If set, treat the timestamp provided in `subscription_clock::timeout` as an absolute timestamp
     * of clock `subscription_clock::id`. If clear, treat the timestamp provided in
     * `subscription_clock::timeout` relative to the current time value of clock
     * `subscription_clock::id`.
     */
    const val SUBSCRIPTION_CLOCK_ABSTIME: Subclockflags = 1
}

internal data class SubscriptionClock(
    /**
     * The clock against which to compare the timestamp.
     */
    var id: Clockid,
    /**
     * The absolute or relative timestamp.
     */
    var timeout: Timestamp,
    /**
     * The amount of time that the implementation may wait additionally to coalesce with other
     * events.

     */
    var precision: Timestamp,
    /**
     * Flags specifying whether the timeout is absolute or relative*/
    var flags: Subclockflags,
)

internal fun __load_SubscriptionClock(ptr: Pointer): SubscriptionClock {
    return SubscriptionClock(
        Clockid.values()[(ptr + 0).loadInt().toInt()],
        (ptr + 8).loadLong(),
        (ptr + 16).loadLong(),
        (ptr + 24).loadShort(),
    )
}

internal fun __store_SubscriptionClock(x: SubscriptionClock, ptr: Pointer) {
    (ptr + 0).storeInt(x.id.ordinal.toInt())
    (ptr + 8).storeLong(x.timeout)
    (ptr + 16).storeLong(x.precision)
    (ptr + 24).storeShort(x.flags)
}

data class SubscriptionFdReadwrite(
    /**
     * The file descriptor on which to wait for it to become ready for reading or writing.
     */
    var file_descriptor: Fd,
)

internal fun __load_SubscriptionFdReadwrite(ptr: Pointer): SubscriptionFdReadwrite {
    return SubscriptionFdReadwrite(
        (ptr + 0).loadInt(),
    )
}

internal fun __store_SubscriptionFdReadwrite(x: SubscriptionFdReadwrite, ptr: Pointer) {
    (ptr + 0).storeInt(x.file_descriptor)
}

internal sealed class SubscriptionU {
    data class clock(var value: SubscriptionClock) : SubscriptionU()
    data class fd_read(var value: SubscriptionFdReadwrite) : SubscriptionU()
    data class fd_write(var value: SubscriptionFdReadwrite) : SubscriptionU()
}

internal data class Subscription(
    /**
     * User-provided value that is attached to the subscription in the implementation and returned
     * through `event::userdata`.

     */
    var userdata: Userdata,
    /**
     * The type of the event to which to subscribe, and its contents*/
    var u: SubscriptionU,
)

internal fun __load_Subscription(ptr: Pointer): Subscription {
    return Subscription(
        (ptr + 0).loadLong(),
        when ((ptr + 8).loadByte().toInt()) {
            0 -> {
                SubscriptionU.clock(
                    SubscriptionClock(
                        Clockid.values()[(ptr + 8 + 8 + 0).loadInt().toInt()],
                        (ptr + 8 + 8 + 8).loadLong(),
                        (ptr + 8 + 8 + 16).loadLong(),
                        (ptr + 8 + 8 + 24).loadShort(),
                    )
                )
            }
            1 -> {
                SubscriptionU.fd_read(
                    SubscriptionFdReadwrite(
                        (ptr + 8 + 8 + 0).loadInt(),
                    )
                )
            }
            2 -> {
                SubscriptionU.fd_write(
                    SubscriptionFdReadwrite(
                        (ptr + 8 + 8 + 0).loadInt(),
                    )
                )
            }
            else -> error("Invalid variant")
        },
    )
}

internal fun __store_Subscription(x: Subscription, ptr: Pointer) {
    (ptr + 0).storeLong(x.userdata)
    TODO()
}

internal typealias Exitcode = Int

internal enum class Signal {
    /**
     * No signal. Note that POSIX has special semantics for `kill(pid, 0)`, so this value is reserved.
     */
    NONE,

    /**
     * Hangup. Action: Terminates the process.
     */
    HUP,

    /**
     * Terminate interrupt signal. Action: Terminates the process.
     */
    INT,

    /**
     * Terminal quit signal. Action: Terminates the process.
     */
    QUIT,

    /**
     * Illegal instruction. Action: Terminates the process.
     */
    ILL,

    /**
     * Trace/breakpoint trap. Action: Terminates the process.
     */
    TRAP,

    /**
     * Process abort signal. Action: Terminates the process.
     */
    ABRT,

    /**
     * Access to an undefined portion of a memory object. Action: Terminates the process.
     */
    BUS,

    /**
     * Erroneous arithmetic operation. Action: Terminates the process.
     */
    FPE,

    /**
     * Kill. Action: Terminates the process.
     */
    KILL,

    /**
     * User-defined signal 1. Action: Terminates the process.
     */
    USR1,

    /**
     * Invalid memory reference. Action: Terminates the process.
     */
    SEGV,

    /**
     * User-defined signal 2. Action: Terminates the process.
     */
    USR2,

    /**
     * Write on a pipe with no one to read it. Action: Ignored.
     */
    PIPE,

    /**
     * Alarm clock. Action: Terminates the process.
     */
    ALRM,

    /**
     * Termination signal. Action: Terminates the process.
     */
    TERM,

    /**
     * Child process terminated, stopped, or continued. Action: Ignored.
     */
    CHLD,

    /**
     * Continue executing, if stopped. Action: Continues executing, if stopped.
     */
    CONT,

    /**
     * Stop executing. Action: Stops executing.
     */
    STOP,

    /**
     * Terminal stop signal. Action: Stops executing.
     */
    TSTP,

    /**
     * Background process attempting read. Action: Stops executing.
     */
    TTIN,

    /**
     * Background process attempting write. Action: Stops executing.
     */
    TTOU,

    /**
     * High bandwidth data is available at a socket. Action: Ignored.
     */
    URG,

    /**
     * CPU time limit exceeded. Action: Terminates the process.
     */
    XCPU,

    /**
     * File size limit exceeded. Action: Terminates the process.
     */
    XFSZ,

    /**
     * Virtual timer expired. Action: Terminates the process.
     */
    VTALRM,

    /**
     * Profiling timer expired. Action: Terminates the process.
     */
    PROF,

    /**
     * Window changed. Action: Ignored.
     */
    WINCH,

    /**
     * I/O possible. Action: Terminates the process.
     */
    POLL,

    /**
     * Power failure. Action: Terminates the process.
     */
    PWR,

    /**
     * Bad system call. Action: Terminates the process.
     */
    SYS,
}

internal typealias Riflags = Short

internal object RIFLAGS {
    /**
     * Returns the message without removing it from the socket's receive queue.
     */
    const val RECV_PEEK: Riflags = 1

    /**
     * On byte-stream sockets, block until the full amount of data can be returned.
     */
    const val RECV_WAITALL: Riflags = 2
}

internal typealias Roflags = Short

internal object ROFLAGS {
    /**
     * Returned by `sock_recv`: Message data has been truncated.
     */
    const val RECV_DATA_TRUNCATED: Roflags = 1
}

internal typealias Siflags = Short

internal typealias Sdflags = Byte

internal object SDFLAGS {
    /**
     * Disables further receive operations.
     */
    const val RD: Sdflags = 1

    /**
     * Disables further send operations.
     */
    const val WR: Sdflags = 2
}

internal enum class Preopentype {
    /**
     * A pre-opened directory.
     */
    DIR,
}

internal data class PrestatDir(
    /**
     *  The length of the directory name for use with `fd_prestat_dir_name`.
     */
    var pr_name_len: Size,
)

internal fun __load_PrestatDir(ptr: Pointer): PrestatDir {
    return PrestatDir(
        (ptr + 0).loadInt(),
    )
}

internal fun __store_PrestatDir(x: PrestatDir, ptr: Pointer) {
    (ptr + 0).storeInt(x.pr_name_len)
}

internal sealed class Prestat {
    data class dir(var value: PrestatDir) : Prestat()
}

/**
 * Read command-line argument data. The size of the array should match that returned by
 * `args_sizes_get`. Each argument is expected to be `\0` terminated.
 */
internal fun __unsafe__args_get(
    argv: Pointer, /*<Pointer/*<Byte>*/>*/
    argv_buf: Pointer, /*<Byte>*/
): Unit {
    val ret = _raw_wasm__args_get(argv.address.toInt(), argv_buf.address.toInt())
    return if (ret == 0) {
        Unit
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Return command-line argument data sizes.  * ## Return * Returns the number of arguments and the size of the argument string data, or an error.
 */
internal fun args_sizes_get(): Pair<Size, Size> {
    withScopedMemoryAllocator { allocator ->
        val rp0 = allocator.allocate(4)
        val rp1 = allocator.allocate(4)
        val ret = _raw_wasm__args_sizes_get(rp0.address.toInt(), rp1.address.toInt())
        return if (ret == 0) {
            Pair(
                (Pointer(rp0.address.toInt().toUInt())).loadInt(),
                (Pointer(rp1.address.toInt().toUInt())).loadInt()
            )
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/**
 * Read environment variable data. The sizes of the buffers should match that returned by
 * `environ_sizes_get`. Key/value pairs are expected to be joined with `=`s, and terminated with
 * `\0`s.
 */
internal fun __unsafe__environ_get(
    environ: Pointer, /*<Pointer/*<Byte>*/>*/
    environ_buf: Pointer, /*<Byte>*/
): Unit {
    val ret = _raw_wasm__environ_get(environ.address.toInt(), environ_buf.address.toInt())
    return if (ret == 0) {
        Unit
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Return environment variable data sizes.  * Returns the number of environment variable arguments and the size of the environment variable data.
 */
internal fun environ_sizes_get(): Pair<Size, Size> {
    withScopedMemoryAllocator { allocator ->
        val rp0 = allocator.allocate(4)
        val rp1 = allocator.allocate(4)
        val ret = _raw_wasm__environ_sizes_get(rp0.address.toInt(), rp1.address.toInt())
        return if (ret == 0) {
            Pair(
                (Pointer(rp0.address.toInt().toUInt())).loadInt(),
                (Pointer(rp1.address.toInt().toUInt())).loadInt()
            )
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/**
 * Return the resolution of a clock. Implementations are required to provide a non-zero value for
 * supported clocks. For unsupported clocks, return `errno::inval`. Note: This is similar to
 * `clock_getres` in POSIX.
 * ## Parameters
 *
 * `id` - The clock for which to return the resolution.
 *
 * ## Return
 * The resolution of the clock, or an error if one happened.
 */
internal fun clock_res_get(
    id: Clockid,
): Timestamp {
    withScopedMemoryAllocator { allocator ->
        val rp0 = allocator.allocate(8)
        val ret = _raw_wasm__clock_res_get(id.ordinal, rp0.address.toInt())
        return if (ret == 0) {
            (Pointer(rp0.address.toInt().toUInt())).loadLong()
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/**
 * Return the time value of a clock. Note: This is similar to `clock_gettime` in POSIX.
 *  ## Parameters
 * `id` - The clock for which to return the time.
 * `precision` - The maximum lag (exclusive) that the returned time value may have, compared to
 * its actual value.
 * ## Return
 * The time value of the clock.
 */
internal fun clock_time_get(
    id: Clockid,
    precision: Timestamp,
): Timestamp {
    withScopedMemoryAllocator { allocator ->
        val rp0 = allocator.allocate(8)
        val ret = _raw_wasm__clock_time_get(id.ordinal, precision.toLong(), rp0.address.toInt())
        return if (ret == 0) {
            (Pointer(rp0.address.toInt().toUInt())).loadLong()
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/**
 * Provide file advisory information on a file descriptor. Note: This is similar to `posix_fadvise`
 * in POSIX.
 *
 **  ## Parameters 
 * `offset` - The offset within the file to which the advisory applies. * `len` - The length of the region to which the advisory applies.
 * `advice` - The advice.
 */
internal fun fd_advise(
    fd: Fd,
    offset: Filesize,
    len: Filesize,
    advice: Advice,
): Unit {
    val ret = _raw_wasm__fd_advise(fd.toInt(), offset.toLong(), len.toLong(), advice.ordinal)
    return if (ret == 0) {
        Unit
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Force the allocation of space in a file. Note: This is similar to `posix_fallocate` in POSIX.
 *  ## Parameters 
 * `offset` - The offset at which to start the allocation.
 * `len` - The length of the area that is allocated.
 */
internal fun fd_allocate(
    fd: Fd,
    offset: Filesize,
    len: Filesize,
): Unit {
    val ret = _raw_wasm__fd_allocate(fd.toInt(), offset.toLong(), len.toLong())
    return if (ret == 0) {
        Unit
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Close a file descriptor. Note: This is similar to `close` in POSIX.
 */
internal fun fd_close(
    fd: Fd,
): Unit {
    val ret = _raw_wasm__fd_close(fd.toInt())
    return if (ret == 0) {
        Unit
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Synchronize the data of a file to disk. Note: This is similar to `fdatasync` in POSIX.
 */
internal fun fd_datasync(
    fd: Fd,
): Unit {
    val ret = _raw_wasm__fd_datasync(fd.toInt())
    return if (ret == 0) {
        Unit
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Get the attributes of a file descriptor. Note: This returns similar flags to `fsync(fd, F_GETFL)`
 * in POSIX, as well as additional fields.
 *  ## Return
 *  The buffer where the file descriptor's attributes are stored.
 */
internal fun fd_fdstat_get(
    fd: Fd,
): Fdstat {
    withScopedMemoryAllocator { allocator ->
        val rp0 = allocator.allocate(24)
        val ret = _raw_wasm__fd_fdstat_get(fd.toInt(), rp0.address.toInt())
        return if (ret == 0) {
            Fdstat(
                Filetype.values()[(Pointer(rp0.address.toInt().toUInt()) + 0).loadByte().toInt()],
                (Pointer(rp0.address.toInt().toUInt()) + 2).loadShort(),
                (Pointer(rp0.address.toInt().toUInt()) + 8).loadLong(),
                (Pointer(rp0.address.toInt().toUInt()) + 16).loadLong(),
            )
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/**
 * Adjust the flags associated with a file descriptor. Note: This is similar to `fcntl(fd, F_SETFL,
 * flags)` in POSIX.
 *  ## Parameters
 *  `flags` - The desired values of the file descriptor flags.
 */
internal fun fd_fdstat_set_flags(
    fd: Fd,
    flags: Fdflags,
): Unit {
    val ret = _raw_wasm__fd_fdstat_set_flags(fd.toInt(), flags.toInt())
    return if (ret == 0) {
        Unit
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Adjust the rights associated with a file descriptor. This can only be used to remove rights, and
 * returns `errno::notcapable` if called in a way that would attempt to add rights
 *  ## Parameters
 *  `fs_rights_base` - The desired rights of the file descriptor.
 */
internal fun fd_fdstat_set_rights(
    fd: Fd,
    fs_rights_base: Rights,
    fs_rights_inheriting: Rights,
): Unit {
    val ret =
        _raw_wasm__fd_fdstat_set_rights(
            fd.toInt(), fs_rights_base.toLong(), fs_rights_inheriting.toLong()
        )
    return if (ret == 0) {
        Unit
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Return the attributes of an open file.
 *  ## Return * The buffer where the file's attributes are stored.
 */
internal fun fd_filestat_get(
    fd: Fd,
): Filestat {
    withScopedMemoryAllocator { allocator ->
        val rp0 = allocator.allocate(64)
        val ret = _raw_wasm__fd_filestat_get(fd.toInt(), rp0.address.toInt())
        return if (ret == 0) {
            Filestat(
                (Pointer(rp0.address.toInt().toUInt()) + 0).loadLong(),
                (Pointer(rp0.address.toInt().toUInt()) + 8).loadLong(),
                Filetype.values()[(Pointer(rp0.address.toInt().toUInt()) + 16).loadByte().toInt()],
                (Pointer(rp0.address.toInt().toUInt()) + 24).loadLong(),
                (Pointer(rp0.address.toInt().toUInt()) + 32).loadLong(),
                (Pointer(rp0.address.toInt().toUInt()) + 40).loadLong(),
                (Pointer(rp0.address.toInt().toUInt()) + 48).loadLong(),
                (Pointer(rp0.address.toInt().toUInt()) + 56).loadLong(),
            )
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/**
 * Adjust the size of an open file. If this increases the file's size, the extra bytes are filled
 * with zeros. Note: This is similar to `ftruncate` in POSIX.
 *  ## Parameters
 *  `size` - The desired file size.
 */
internal fun fd_filestat_set_size(
    fd: Fd,
    size: Filesize,
): Unit {
    val ret = _raw_wasm__fd_filestat_set_size(fd.toInt(), size.toLong())
    return if (ret == 0) {
        Unit
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Adjust the timestamps of an open file or directory. Note: This is similar to `futimens` in POSIX.
 *  ## Parameters 
 * `atim` - The desired values of the data access timestamp.
 * `mtim` - The desired values of the data modification timestamp.
 * `fst_flags` - A bitmask indicating which timestamps to adjust.
 */
internal fun fd_filestat_set_times(
    fd: Fd,
    atim: Timestamp,
    mtim: Timestamp,
    fst_flags: Fstflags,
): Unit {
    val ret =
        _raw_wasm__fd_filestat_set_times(
            fd.toInt(), atim.toLong(), mtim.toLong(), fst_flags.toInt()
        )
    return if (ret == 0) {
        Unit
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Read from a file descriptor, without using and updating the file descriptor's offset. Note: This
 * is similar to `preadv` in POSIX.
 *  ## Parameters 
 * `iovs` - List of scatter/gather vectors in which to store data.
 * `offset` - The offset within the file at which to read.
 * ## Return * The number of bytes read.
 */
internal fun __unsafe__fd_pread(
    allocator: MemoryAllocator,
    fd: Fd,
    iovs: __unsafe__IovecArray,
    offset: Filesize,
): Size {
    val rp0 = allocator.allocate(4)
    val ret =
        _raw_wasm__fd_pread(
            fd.toInt(),
            allocator.writeToLinearMemory(iovs).address.toInt(),
            iovs.size,
            offset.toLong(),
            rp0.address.toInt()
        )
    return if (ret == 0) {
        (Pointer(rp0.address.toInt().toUInt())).loadInt()
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Return a description of the given preopened file descriptor.
 *  ## Return * The buffer where the description is stored.
 */
internal fun fd_prestat_get(
    fd: Fd,
): Prestat {
    withScopedMemoryAllocator { allocator ->
        val rp0 = allocator.allocate(8)
        val ret = _raw_wasm__fd_prestat_get(fd.toInt(), rp0.address.toInt())
        return if (ret == 0) {
            when ((Pointer(rp0.address.toInt().toUInt())).loadByte().toInt()) {
                0 -> {
                    Prestat.dir(
                        PrestatDir(
                            (Pointer(rp0.address.toInt().toUInt()) + 4 + 0).loadInt(),
                        )
                    )
                }
                else -> error("Invalid variant")
            }
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/**
 * Return a description of the given preopened file descriptor.
 *  ## Parameters * `path` - A buffer into which to write the preopened directory name.
 */
internal fun __unsafe__fd_prestat_dir_name(
    fd: Fd,
    path: Pointer, /*<Byte>*/
    path_len: Size,
): Unit {
    val ret = _raw_wasm__fd_prestat_dir_name(fd.toInt(), path.address.toInt(), path_len.toInt())
    return if (ret == 0) {
        Unit
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Write to a file descriptor, without using and updating the file descriptor's offset. Note: This
 * is similar to `pwritev` in POSIX.

 *  ## Parameters 
 * `iovs` - List of scatter/gather vectors from which to retrieve data. * `offset` - The offset within the file at which to write.
 * ## Return * The number of bytes written.
 */
internal fun __unsafe__fd_pwrite(
    allocator: MemoryAllocator,
    fd: Fd,
    iovs: __unsafe__CiovecArray,
    offset: Filesize,
): Size {
    val rp0 = allocator.allocate(4)
    val ret =
        _raw_wasm__fd_pwrite(
            fd.toInt(),
            allocator.writeToLinearMemory(iovs).address.toInt(),
            iovs.size,
            offset.toLong(),
            rp0.address.toInt()
        )
    return if (ret == 0) {
        (Pointer(rp0.address.toInt().toUInt())).loadInt()
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Read from a file descriptor. Note: This is similar to `readv` in POSIX.
 *  ## Parameters 
 * `iovs` - List of scatter/gather vectors to which to store data.
 * ## Return * The number of bytes read.
 */
internal fun __unsafe__fd_read(
    allocator: MemoryAllocator,
    fd: Fd,
    iovs: __unsafe__IovecArray,
): Size {
    val rp0 = allocator.allocate(4)
    val ret =
        _raw_wasm__fd_read(
            fd.toInt(),
            allocator.writeToLinearMemory(iovs).address.toInt(),
            iovs.size,
            rp0.address.toInt()
        )
    return if (ret == 0) {
        (Pointer(rp0.address.toInt().toUInt())).loadInt()
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Read directory entries from a directory. When successful, the contents of the output buffer
 * consist of a sequence of directory entries. Each directory entry consists of a `dirent` object,
 * followed by `dirent::d_namlen` bytes holding the name of the directory entry. This function fills
 * the output buffer as much as possible, potentially truncating the last directory entry. This
 * allows the caller to grow its read buffer size in case it's too small to fit a single large
 * directory entry, or skip the oversized directory entry.

 *  ## Parameters 
 * `buf` - The buffer where directory entries are stored * `cookie` - The location within the directory to start reading
 * ## Return
 * The number of bytes stored in the read buffer. If less than the size of the read buffer, the end of the directory has been reached.
 */
internal fun __unsafe__fd_readdir(
    allocator: MemoryAllocator,
    fd: Fd,
    buf: Pointer, /*<Byte>*/
    buf_len: Size,
    cookie: Dircookie,
): Size {
    val rp0 = allocator.allocate(4)
    val ret =
        _raw_wasm__fd_readdir(
            fd.toInt(), buf.address.toInt(), buf_len.toInt(), cookie.toLong(), rp0.address.toInt()
        )
    return if (ret == 0) {
        (Pointer(rp0.address.toInt().toUInt())).loadInt()
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Atomically replace a file descriptor by renumbering another file descriptor. Due to the strong
 * focus on thread safety, this environment does not provide a mechanism to duplicate or renumber a
 * file descriptor to an arbitrary number, like `dup2()`. This would be prone to race conditions, as
 * an actual file descriptor with the same number could be allocated by a different thread at the
 * same time. This function provides a way to atomically renumber file descriptors, which would
 * disappear if `dup2()` were to be removed entirely.

 *  ## Parameters * `to` - The file descriptor to overwrite.
 */
internal fun fd_renumber(
    fd: Fd,
    to: Fd,
): Unit {
    val ret = _raw_wasm__fd_renumber(fd.toInt(), to.toInt())
    return if (ret == 0) {
        Unit
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Move the offset of a file descriptor. Note: This is similar to `lseek` in POSIX.
 *  ## Parameters 
 * `offset` - The number of bytes to move. * `whence` - The base from which the offset is relative.
 * ## Return * The new offset of the file descriptor, relative to the start of the file.
 */
internal fun fd_seek(
    fd: Fd,
    offset: Filedelta,
    whence: Whence,
): Filesize {
    withScopedMemoryAllocator { allocator ->
        val rp0 = allocator.allocate(8)
        val ret = _raw_wasm__fd_seek(fd.toInt(), offset, whence.ordinal, rp0.address.toInt())
        return if (ret == 0) {
            (Pointer(rp0.address.toInt().toUInt())).loadLong()
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/**
 * Synchronize the data and metadata of a file to disk. Note: This is similar to `fsync` in POSIX.

 */
internal fun fd_sync(
    fd: Fd,
): Unit {
    val ret = _raw_wasm__fd_sync(fd.toInt())
    return if (ret == 0) {
        Unit
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Return the current offset of a file descriptor. Note: This is similar to `lseek(fd, 0, SEEK_CUR)`
 * in POSIX.
 *  ## Return * The current offset of the file descriptor, relative to the start of the file.

 */
internal fun fd_tell(
    fd: Fd,
): Filesize {
    withScopedMemoryAllocator { allocator ->
        val rp0 = allocator.allocate(8)
        val ret = _raw_wasm__fd_tell(fd.toInt(), rp0.address.toInt())
        return if (ret == 0) {
            (Pointer(rp0.address.toInt().toUInt())).loadLong()
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/**
 * Write to a file descriptor. Note: This is similar to `writev` in POSIX.
 * ## Parameters
 * `iovs` - List of scatter/gather vectors from which to retrieve data.
 */
internal fun __unsafe__fd_write(
    allocator: MemoryAllocator,
    fd: Fd,
    iovs: __unsafe__CiovecArray,
): Size {
    val rp0 = allocator.allocate(4)
    val ret =
        _raw_wasm__fd_write(
            fd.toInt(),
            allocator.writeToLinearMemory(iovs).address.toInt(),
            iovs.size,
            rp0.address.toInt()
        )
    return if (ret == 0) {
        (Pointer(rp0.address.toInt().toUInt())).loadInt()
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Create a directory. Note: This is similar to `mkdirat` in POSIX.
 *  ## Parameters * `path` - The path at which to create the directory.

 */
internal fun path_create_directory(
    fd: Fd,
    path: String,
): Unit {
    withScopedMemoryAllocator { allocator ->
        val ret =
            _raw_wasm__path_create_directory(
                fd.toInt(), allocator.writeToLinearMemory(path).address.toInt(), path.size
            )
        return if (ret == 0) {
            Unit
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/**
 * Return the attributes of a file or directory. Note: This is similar to `stat` in POSIX.
 *  ## Parameters 
 * `flags` - Flags determining the method of how the path is resolved. * `path` - The path of the file or directory to inspect.
 * ## Return * The buffer where the file's attributes are stored.
 */
internal fun path_filestat_get(
    fd: Fd,
    flags: Lookupflags,
    path: String,
): Filestat {
    withScopedMemoryAllocator { allocator ->
        val rp0 = allocator.allocate(64)
        val ret =
            _raw_wasm__path_filestat_get(
                fd.toInt(),
                flags.toInt(),
                allocator.writeToLinearMemory(path).address.toInt(),
                path.size,
                rp0.address.toInt()
            )
        return if (ret == 0) {
            Filestat(
                (Pointer(rp0.address.toInt().toUInt()) + 0).loadLong(),
                (Pointer(rp0.address.toInt().toUInt()) + 8).loadLong(),
                Filetype.values()[(Pointer(rp0.address.toInt().toUInt()) + 16).loadByte().toInt()],
                (Pointer(rp0.address.toInt().toUInt()) + 24).loadLong(),
                (Pointer(rp0.address.toInt().toUInt()) + 32).loadLong(),
                (Pointer(rp0.address.toInt().toUInt()) + 40).loadLong(),
                (Pointer(rp0.address.toInt().toUInt()) + 48).loadLong(),
                (Pointer(rp0.address.toInt().toUInt()) + 56).loadLong(),
            )
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/**
 * Adjust the timestamps of a file or directory. Note: This is similar to `utimensat` in POSIX.
 *  ## Parameters 
 * `flags` - Flags determining the method of how the path is resolved. * `path` - The path of the file or directory to operate on. * `atim` - The desired values of the data access timestamp. * `mtim` - The desired values of the data modification timestamp. * `fst_flags` - A bitmask indicating which timestamps to adjust.
 */
internal fun path_filestat_set_times(
    fd: Fd,
    flags: Lookupflags,
    path: String,
    atim: Timestamp,
    mtim: Timestamp,
    fst_flags: Fstflags,
): Unit {
    withScopedMemoryAllocator { allocator ->
        val ret =
            _raw_wasm__path_filestat_set_times(
                fd.toInt(),
                flags.toInt(),
                allocator.writeToLinearMemory(path).address.toInt(),
                path.size,
                atim.toLong(),
                mtim.toLong(),
                fst_flags.toInt()
            )
        return if (ret == 0) {
            Unit
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/**
 * Create a hard link. Note: This is similar to `linkat` in POSIX.
 *  ## Parameters 
 * `old_flags` - Flags determining the method of how the path is resolved. * `old_path` - The source path from which to link. * `new_fd` - The working directory at which the resolution of the new path starts.
 * `new_path` - The destination path at which to create the hard link.
 */
internal fun path_link(
    old_fd: Fd,
    old_flags: Lookupflags,
    old_path: String,
    new_fd: Fd,
    new_path: String,
): Unit {
    withScopedMemoryAllocator { allocator ->
        val ret =
            _raw_wasm__path_link(
                old_fd.toInt(),
                old_flags.toInt(),
                allocator.writeToLinearMemory(old_path).address.toInt(),
                old_path.size,
                new_fd.toInt(),
                allocator.writeToLinearMemory(new_path).address.toInt(),
                new_path.size
            )
        return if (ret == 0) {
            Unit
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/**
 * Open a file or directory. The returned file descriptor is not guaranteed to be the
 * lowest-numbered file descriptor not currently open; it is randomized to prevent applications from
 * depending on making assumptions about indexes, since this is error-prone in multi-threaded
 * contexts. The returned file descriptor is guaranteed to be less than 2**31. Note: This is similar
 * to `openat` in POSIX.

 *  ## Parameters 
 * `dirflags` - Flags determining the method of how the path is resolved. * `path` - The relative path of the file or directory to open, relative to the *    `path_open::fd` directory. * `oflags` - The method by which to open the file. * `fs_rights_base` - The initial rights of the newly created file descriptor. The *    implementation is allowed to return a file descriptor with fewer rights *    than specified, if and only if those rights do not apply to the type of *    file being opened. *    The *base* rights are rights that will apply to operations using the file *    descriptor itself, while the *inheriting* rights are rights that apply to *    file descriptors derived from it.
 * ## Return * The file descriptor of the file that has been opened.
 */
internal fun path_open(
    fd: Fd,
    dirflags: Lookupflags,
    path: String,
    oflags: Oflags,
    fs_rights_base: Rights,
    fs_rights_inheriting: Rights,
    fdflags: Fdflags,
): Fd {
    withScopedMemoryAllocator { allocator ->
        val rp0 = allocator.allocate(4)
        val ret =
            _raw_wasm__path_open(
                fd.toInt(),
                dirflags.toInt(),
                allocator.writeToLinearMemory(path).address.toInt(),
                path.size,
                oflags.toInt(),
                fs_rights_base.toLong(),
                fs_rights_inheriting.toLong(),
                fdflags.toInt(),
                rp0.address.toInt()
            )
        return if (ret == 0) {
            (Pointer(rp0.address.toInt().toUInt())).loadInt()
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/**
 * Read the contents of a symbolic link. Note: This is similar to `readlinkat` in POSIX.
 *  ## Parameters 
 * `path` - The path of the symbolic link from which to read. * `buf` - The buffer to which to write the contents of the symbolic link.
 * ## Return * The number of bytes placed in the buffer.
 */
internal fun __unsafe__path_readlink(
    allocator: MemoryAllocator,
    fd: Fd,
    path: String,
    buf: Pointer, /*<Byte>*/
    buf_len: Size,
): Size {
    val rp0 = allocator.allocate(4)
    val ret =
        _raw_wasm__path_readlink(
            fd.toInt(),
            allocator.writeToLinearMemory(path).address.toInt(),
            path.size,
            buf.address.toInt(),
            buf_len.toInt(),
            rp0.address.toInt()
        )
    return if (ret == 0) {
        (Pointer(rp0.address.toInt().toUInt())).loadInt()
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Remove a directory. Return `errno::notempty` if the directory is not empty. Note: This is similar
 * to `unlinkat(fd, path, AT_REMOVEDIR)` in POSIX.

 *  ## Parameters * `path` - The path to a directory to remove.
 */
internal fun path_remove_directory(
    fd: Fd,
    path: String,
): Unit {
    withScopedMemoryAllocator { allocator ->
        val ret =
            _raw_wasm__path_remove_directory(
                fd.toInt(), allocator.writeToLinearMemory(path).address.toInt(), path.size
            )
        return if (ret == 0) {
            Unit
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/**
 * Rename a file or directory. Note: This is similar to `renameat` in POSIX.
 *  ## Parameters 
 * `old_path` - The source path of the file or directory to rename. * `new_fd` - The working directory at which the resolution of the new path starts.
 * `new_path` - The destination path to which to rename the file or directory.
 */
internal fun path_rename(
    fd: Fd,
    old_path: String,
    new_fd: Fd,
    new_path: String,
): Unit {
    withScopedMemoryAllocator { allocator ->
        val ret =
            _raw_wasm__path_rename(
                fd.toInt(),
                allocator.writeToLinearMemory(old_path).address.toInt(),
                old_path.size,
                new_fd.toInt(),
                allocator.writeToLinearMemory(new_path).address.toInt(),
                new_path.size
            )
        return if (ret == 0) {
            Unit
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/**
 * Create a symbolic link. Note: This is similar to `symlinkat` in POSIX.
 *  ## Parameters 
 * `old_path` - The contents of the symbolic link.
 * `new_path` - The destination path at which to create the symbolic link.
 */
internal fun path_symlink(
    old_path: String,
    fd: Fd,
    new_path: String,
): Unit {
    withScopedMemoryAllocator { allocator ->
        val ret =
            _raw_wasm__path_symlink(
                allocator.writeToLinearMemory(old_path).address.toInt(),
                old_path.size,
                fd.toInt(),
                allocator.writeToLinearMemory(new_path).address.toInt(),
                new_path.size
            )
        return if (ret == 0) {
            Unit
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/**
 * Unlink a file. Return `errno::isdir` if the path refers to a directory. Note: This is similar to
 * `unlinkat(fd, path, 0)` in POSIX.

 *  ## Parameters * `path` - The path to a file to unlink.
 */
internal fun path_unlink_file(
    fd: Fd,
    path: String,
): Unit {
    withScopedMemoryAllocator { allocator ->
        val ret =
            _raw_wasm__path_unlink_file(
                fd.toInt(), allocator.writeToLinearMemory(path).address.toInt(), path.size
            )
        return if (ret == 0) {
            Unit
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/**
 * Concurrently poll for the occurrence of a set of events.
 *  ## Parameters 
 * `in_` - The events to which to subscribe. * `out` - The events that have occurred. * `nsubscriptions` - Both the number of subscriptions and events.
 * ## Return * The number of events stored.
 */
internal fun __unsafe__poll_oneoff(
    allocator: MemoryAllocator,
    in_: Pointer, /*<Subscription>*/
    out: Pointer, /*<Event>*/
    nsubscriptions: Size,
): Size {
    val rp0 = allocator.allocate(4)
    val ret =
        _raw_wasm__poll_oneoff(
            in_.address.toInt(), out.address.toInt(), nsubscriptions.toInt(), rp0.address.toInt()
        )
    return if (ret == 0) {
        (Pointer(rp0.address.toInt().toUInt())).loadInt()
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Terminate the process normally. An exit code of 0 indicates successful termination of the
 * program. The meanings of other values is dependent on the environment.

 *  ## Parameters * `rval` - The exit code returned by the process.
 */
internal fun proc_exit(
    rval: Exitcode,
) {
    _raw_wasm__proc_exit(rval.toInt())
}

/**
 * Send a signal to the process of the calling thread. Note: This is similar to `raise` in POSIX.

 *  ## Parameters * `sig` - The signal condition to trigger.
 */
internal fun proc_raise(
    sig: Signal,
): Unit {
    val ret = _raw_wasm__proc_raise(sig.ordinal)
    return if (ret == 0) {
        Unit
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Temporarily yield execution of the calling thread. Note: This is similar to `sched_yield` in
 * POSIX.
 */
internal fun sched_yield(): Unit {
    val ret = _raw_wasm__sched_yield()
    return if (ret == 0) {
        Unit
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Write high-quality random data into a buffer. This function blocks when the implementation is
 * unable to immediately provide sufficient high-quality random data. This function may execute
 * slowly, so when large mounts of random data are required, it's advisable to use this function to
 * seed a pseudo-random number generator, rather than to provide the random data directly.

 *  ## Parameters * `buf` - The buffer to fill with random data.
 */
internal fun __unsafe__random_get(
    buf: Pointer, /*<Byte>*/
    buf_len: Size,
): Unit {
    val ret = _raw_wasm__random_get(buf.address.toInt(), buf_len.toInt())
    return if (ret == 0) {
        Unit
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Accept a new incoming connection. Note: This is similar to `accept` in POSIX.
 *  ## Parameters 
 * `fd` - The listening socket. * `flags` - The desired values of the file descriptor flags.
 * ## Return * New socket connection*/
internal fun sock_accept(
    fd: Fd,
    flags: Fdflags,
): Fd {
    withScopedMemoryAllocator { allocator ->
        val rp0 = allocator.allocate(4)
        val ret = _raw_wasm__sock_accept(fd.toInt(), flags.toInt(), rp0.address.toInt())
        return if (ret == 0) {
            (Pointer(rp0.address.toInt().toUInt())).loadInt()
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/**
 * Receive a message from a socket. Note: This is similar to `recv` in POSIX, though it also
 * supports reading the data into multiple buffers in the manner of `readv`.
 *  ## Parameters 
 * `ri_data` - List of scatter/gather vectors to which to store data. * `ri_flags` - Message flags.
 * ## Return * Number of bytes stored in ri_data and message flags.
 */
internal fun __unsafe__sock_recv(
    allocator: MemoryAllocator,
    fd: Fd,
    ri_data: __unsafe__IovecArray,
    ri_flags: Riflags,
): Pair<
        Size,
        Roflags,
        > {
    val rp0 = allocator.allocate(4)
    val rp1 = allocator.allocate(2)
    val ret =
        _raw_wasm__sock_recv(
            fd.toInt(),
            allocator.writeToLinearMemory(ri_data).address.toInt(),
            ri_data.size,
            ri_flags.toInt(),
            rp0.address.toInt(),
            rp1.address.toInt()
        )
    return if (ret == 0) {
        Pair(
            (Pointer(rp0.address.toInt().toUInt())).loadInt(),
            (Pointer(rp1.address.toInt().toUInt())).loadShort()
        )
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Send a message on a socket. Note: This is similar to `send` in POSIX, though it also supports
 * writing the data from multiple buffers in the manner of `writev`.
 *  ## Parameters 
 * `si_data` - List of scatter/gather vectors to which to retrieve data * `si_flags` - Message flags.
 * ## Return * Number of bytes transmitted.
 */
internal fun __unsafe__sock_send(
    allocator: MemoryAllocator,
    fd: Fd,
    si_data: __unsafe__CiovecArray,
    si_flags: Siflags,
): Size {
    val rp0 = allocator.allocate(4)
    val ret =
        _raw_wasm__sock_send(
            fd.toInt(),
            allocator.writeToLinearMemory(si_data).address.toInt(),
            si_data.size,
            si_flags.toInt(),
            rp0.address.toInt()
        )
    return if (ret == 0) {
        (Pointer(rp0.address.toInt().toUInt())).loadInt()
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Shut down socket send and receive channels. Note: This is similar to `shutdown` in POSIX.
 *  ## Parameters
 *  `how` - Which channels on the socket to shut down.
 */
internal fun sock_shutdown(
    fd: Fd,
    how: Sdflags,
): Unit {
    val ret = _raw_wasm__sock_shutdown(fd.toInt(), how.toInt())
    return if (ret == 0) {
        Unit
    } else {
        throw WasiError(Errno.values()[ret])
    }
}

/**
 * Read command-line argument data. The size of the array should match that returned by
 * `args_sizes_get`. Each argument is expected to be `\0` terminated.
 */
@WasmImport("wasi_snapshot_preview1", "args_get")
private external fun _raw_wasm__args_get(
    arg0: Int,
    arg1: Int,
): Int

/**
 * Return command-line argument data sizes.
 */
@WasmImport("wasi_snapshot_preview1", "args_sizes_get")
private external fun _raw_wasm__args_sizes_get(
    arg0: Int,
    arg1: Int,
): Int

/**
 * Read environment variable data. The sizes of the buffers should match that returned by
 * `environ_sizes_get`. Key/value pairs are expected to be joined with `=`s, and terminated with
 * `\0`s.
 */
@WasmImport("wasi_snapshot_preview1", "environ_get")
private external fun _raw_wasm__environ_get(
    arg0: Int,
    arg1: Int,
): Int

/**
 * Return environment variable data sizes.
 */
@WasmImport("wasi_snapshot_preview1", "environ_sizes_get")
private external fun _raw_wasm__environ_sizes_get(
    arg0: Int,
    arg1: Int,
): Int

/**
 * Return the resolution of a clock. Implementations are required to provide a non-zero value for
 * supported clocks. For unsupported clocks, return `errno::inval`. Note: This is similar to
 * `clock_getres` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "clock_res_get")
private external fun _raw_wasm__clock_res_get(
    arg0: Int,
    arg1: Int,
): Int

/**
 * Return the time value of a clock. Note: This is similar to `clock_gettime` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "clock_time_get")
private external fun _raw_wasm__clock_time_get(
    arg0: Int,
    arg1: Long,
    arg2: Int,
): Int

/**
 * Provide file advisory information on a file descriptor. Note: This is similar to `posix_fadvise`
 * in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "fd_advise")
private external fun _raw_wasm__fd_advise(
    arg0: Int,
    arg1: Long,
    arg2: Long,
    arg3: Int,
): Int

/**
 * Force the allocation of space in a file. Note: This is similar to `posix_fallocate` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "fd_allocate")
private external fun _raw_wasm__fd_allocate(
    arg0: Int,
    arg1: Long,
    arg2: Long,
): Int

/**
 * Close a file descriptor. Note: This is similar to `close` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "fd_close")
private external fun _raw_wasm__fd_close(
    arg0: Int,
): Int

/**
 * Synchronize the data of a file to disk. Note: This is similar to `fdatasync` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "fd_datasync")
private external fun _raw_wasm__fd_datasync(
    arg0: Int,
): Int

/**
 * Get the attributes of a file descriptor. Note: This returns similar flags to `fsync(fd, F_GETFL)`
 * in POSIX, as well as additional fields.
 */
@WasmImport("wasi_snapshot_preview1", "fd_fdstat_get")
private external fun _raw_wasm__fd_fdstat_get(
    arg0: Int,
    arg1: Int,
): Int

/**
 * Adjust the flags associated with a file descriptor. Note: This is similar to `fcntl(fd, F_SETFL,
 * flags)` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "fd_fdstat_set_flags")
private external fun _raw_wasm__fd_fdstat_set_flags(
    arg0: Int,
    arg1: Int,
): Int

/**
 * Adjust the rights associated with a file descriptor. This can only be used to remove rights, and
 * returns `errno::notcapable` if called in a way that would attempt to add rights
 */
@WasmImport("wasi_snapshot_preview1", "fd_fdstat_set_rights")
private external fun _raw_wasm__fd_fdstat_set_rights(
    arg0: Int,
    arg1: Long,
    arg2: Long,
): Int

/**
 * Return the attributes of an open file.
 */
@WasmImport("wasi_snapshot_preview1", "fd_filestat_get")
private external fun _raw_wasm__fd_filestat_get(
    arg0: Int,
    arg1: Int,
): Int

/**
 * Adjust the size of an open file. If this increases the file's size, the extra bytes are filled
 * with zeros. Note: This is similar to `ftruncate` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "fd_filestat_set_size")
private external fun _raw_wasm__fd_filestat_set_size(
    arg0: Int,
    arg1: Long,
): Int

/**
 * Adjust the timestamps of an open file or directory. Note: This is similar to `futimens` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "fd_filestat_set_times")
private external fun _raw_wasm__fd_filestat_set_times(
    arg0: Int,
    arg1: Long,
    arg2: Long,
    arg3: Int,
): Int

/**
 * Read from a file descriptor, without using and updating the file descriptor's offset. Note: This
 * is similar to `preadv` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "fd_pread")
private external fun _raw_wasm__fd_pread(
    arg0: Int,
    arg1: Int,
    arg2: Int,
    arg3: Long,
    arg4: Int,
): Int

/**
 * Return a description of the given preopened file descriptor.
 */
@WasmImport("wasi_snapshot_preview1", "fd_prestat_get")
private external fun _raw_wasm__fd_prestat_get(
    arg0: Int,
    arg1: Int,
): Int

/**
 * Return a description of the given preopened file descriptor.
 */
@WasmImport("wasi_snapshot_preview1", "fd_prestat_dir_name")
private external fun _raw_wasm__fd_prestat_dir_name(
    arg0: Int,
    arg1: Int,
    arg2: Int,
): Int

/**
 * Write to a file descriptor, without using and updating the file descriptor's offset. Note: This
 * is similar to `pwritev` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "fd_pwrite")
private external fun _raw_wasm__fd_pwrite(
    arg0: Int,
    arg1: Int,
    arg2: Int,
    arg3: Long,
    arg4: Int,
): Int

/**
 * Read from a file descriptor. Note: This is similar to `readv` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "fd_read")
private external fun _raw_wasm__fd_read(
    arg0: Int,
    arg1: Int,
    arg2: Int,
    arg3: Int,
): Int

/**
 * Read directory entries from a directory. When successful, the contents of the output buffer
 * consist of a sequence of directory entries. Each directory entry consists of a `dirent` object,
 * followed by `dirent::d_namlen` bytes holding the name of the directory entry. This function fills
 * the output buffer as much as possible, potentially truncating the last directory entry. This
 * allows the caller to grow its read buffer size in case it's too small to fit a single large
 * directory entry, or skip the oversized directory entry.
 */
@WasmImport("wasi_snapshot_preview1", "fd_readdir")
private external fun _raw_wasm__fd_readdir(
    arg0: Int,
    arg1: Int,
    arg2: Int,
    arg3: Long,
    arg4: Int,
): Int

/**
 * Atomically replace a file descriptor by renumbering another file descriptor. Due to the strong
 * focus on thread safety, this environment does not provide a mechanism to duplicate or renumber a
 * file descriptor to an arbitrary number, like `dup2()`. This would be prone to race conditions, as
 * an actual file descriptor with the same number could be allocated by a different thread at the
 * same time. This function provides a way to atomically renumber file descriptors, which would
 * disappear if `dup2()` were to be removed entirely.
 */
@WasmImport("wasi_snapshot_preview1", "fd_renumber")
private external fun _raw_wasm__fd_renumber(
    arg0: Int,
    arg1: Int,
): Int

/**
 * Move the offset of a file descriptor. Note: This is similar to `lseek` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "fd_seek")
private external fun _raw_wasm__fd_seek(
    arg0: Int,
    arg1: Long,
    arg2: Int,
    arg3: Int,
): Int

/**
 * Synchronize the data and metadata of a file to disk. Note: This is similar to `fsync` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "fd_sync")
private external fun _raw_wasm__fd_sync(
    arg0: Int,
): Int

/**
 * Return the current offset of a file descriptor. Note: This is similar to `lseek(fd, 0, SEEK_CUR)` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "fd_tell")
private external fun _raw_wasm__fd_tell(
    arg0: Int,
    arg1: Int,
): Int

/**
 * Write to a file descriptor. Note: This is similar to `writev` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "fd_write")
private external fun _raw_wasm__fd_write(
    arg0: Int,
    arg1: Int,
    arg2: Int,
    arg3: Int,
): Int

/**
 * Create a directory. Note: This is similar to `mkdirat` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "path_create_directory")
private external fun _raw_wasm__path_create_directory(
    arg0: Int,
    arg1: Int,
    arg2: Int,
): Int

/**
 * Return the attributes of a file or directory. Note: This is similar to `stat` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "path_filestat_get")
private external fun _raw_wasm__path_filestat_get(
    arg0: Int,
    arg1: Int,
    arg2: Int,
    arg3: Int,
    arg4: Int,
): Int

/**
 * Adjust the timestamps of a file or directory. Note: This is similar to `utimensat` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "path_filestat_set_times")
private external fun _raw_wasm__path_filestat_set_times(
    arg0: Int,
    arg1: Int,
    arg2: Int,
    arg3: Int,
    arg4: Long,
    arg5: Long,
    arg6: Int,
): Int

/**
 * Create a hard link. Note: This is similar to `linkat` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "path_link")
private external fun _raw_wasm__path_link(
    arg0: Int,
    arg1: Int,
    arg2: Int,
    arg3: Int,
    arg4: Int,
    arg5: Int,
    arg6: Int,
): Int

/**
 * Open a file or directory. The returned file descriptor is not guaranteed to be the
 * lowest-numbered file descriptor not currently open; it is randomized to prevent applications from
 * depending on making assumptions about indexes, since this is error-prone in multi-threaded
 * contexts. The returned file descriptor is guaranteed to be less than 2**31. Note: This is similar
 * to `openat` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "path_open")
private external fun _raw_wasm__path_open(
    arg0: Int,
    arg1: Int,
    arg2: Int,
    arg3: Int,
    arg4: Int,
    arg5: Long,
    arg6: Long,
    arg7: Int,
    arg8: Int,
): Int

/**
 * Read the contents of a symbolic link. Note: This is similar to `readlinkat` in POSIX.
 *
 */
@WasmImport("wasi_snapshot_preview1", "path_readlink")
private external fun _raw_wasm__path_readlink(
    arg0: Int,
    arg1: Int,
    arg2: Int,
    arg3: Int,
    arg4: Int,
    arg5: Int,
): Int

/**
 * Remove a directory. Return `errno::notempty` if the directory is not empty. Note: This is similar
 * to `unlinkat(fd, path, AT_REMOVEDIR)` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "path_remove_directory")
private external fun _raw_wasm__path_remove_directory(
    arg0: Int,
    arg1: Int,
    arg2: Int,
): Int

/**
 * Rename a file or directory. Note: This is similar to `renameat` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "path_rename")
private external fun _raw_wasm__path_rename(
    arg0: Int,
    arg1: Int,
    arg2: Int,
    arg3: Int,
    arg4: Int,
    arg5: Int,
): Int

/**
 * Create a symbolic link. Note: This is similar to `symlinkat` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "path_symlink")
private external fun _raw_wasm__path_symlink(
    arg0: Int,
    arg1: Int,
    arg2: Int,
    arg3: Int,
    arg4: Int,
): Int

/**
 * Unlink a file. Return `errno::isdir` if the path refers to a directory. Note: This is similar to
 * `unlinkat(fd, path, 0)` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "path_unlink_file")
private external fun _raw_wasm__path_unlink_file(
    arg0: Int,
    arg1: Int,
    arg2: Int,
): Int

/**
 * Concurrently poll for the occurrence of a set of events.
 */
@WasmImport("wasi_snapshot_preview1", "poll_oneoff")
private external fun _raw_wasm__poll_oneoff(
    arg0: Int,
    arg1: Int,
    arg2: Int,
    arg3: Int,
): Int

/**
 * Terminate the process normally. An exit code of 0 indicates successful termination of the
 * program. The meanings of other values is dependent on the environment.
 */
@WasmImport("wasi_snapshot_preview1", "proc_exit")
private external fun _raw_wasm__proc_exit(
    arg0: Int,
): Unit

/**
 * Send a signal to the process of the calling thread. Note: This is similar to `raise` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "proc_raise")
private external fun _raw_wasm__proc_raise(
    arg0: Int,
): Int

/**
 * Temporarily yield execution of the calling thread. Note: This is similar to `sched_yield` in
 * POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "sched_yield")
private external fun _raw_wasm__sched_yield(): Int

/**
 * Write high-quality random data into a buffer. This function blocks when the implementation is
 * unable to immediately provide sufficient high-quality random data. This function may execute
 * slowly, so when large mounts of random data are required, it's advisable to use this function to
 * seed a pseudo-random number generator, rather than to provide the random data directly.
 */
@WasmImport("wasi_snapshot_preview1", "random_get")
private external fun _raw_wasm__random_get(
    arg0: Int,
    arg1: Int,
): Int

/**
 * Accept a new incoming connection. Note: This is similar to `accept` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "sock_accept")
private external fun _raw_wasm__sock_accept(
    arg0: Int,
    arg1: Int,
    arg2: Int,
): Int

/**
 * Receive a message from a socket. Note: This is similar to `recv` in POSIX, though it also
 * supports reading the data into multiple buffers in the manner of `readv`.
 */
@WasmImport("wasi_snapshot_preview1", "sock_recv")
private external fun _raw_wasm__sock_recv(
    arg0: Int,
    arg1: Int,
    arg2: Int,
    arg3: Int,
    arg4: Int,
    arg5: Int,
): Int

/**
 * Send a message on a socket. Note: This is similar to `send` in POSIX, though it also supports
 * writing the data from multiple buffers in the manner of `writev`.
 */
@WasmImport("wasi_snapshot_preview1", "sock_send")
private external fun _raw_wasm__sock_send(
    arg0: Int,
    arg1: Int,
    arg2: Int,
    arg3: Int,
    arg4: Int,
): Int

/**
 * Shut down socket send and receive channels. Note: This is similar to `shutdown` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "sock_shutdown")
private external fun _raw_wasm__sock_shutdown(
    arg0: Int,
    arg1: Int,
): Int
