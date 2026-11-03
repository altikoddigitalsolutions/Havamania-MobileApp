package com.havamania

import java.io.InputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal const val AVATAR_SOURCE_MAX_BYTES = 20_000_000
internal class AvatarSourceTooLargeException : Exception("Avatar source exceeds limit")

/** Private chunks keep each decode pass on the same bytes without a second full-size copy. */
internal class AvatarSourceSnapshot(private val chunks: List<ByteArray>) {
    private val offsets = IntArray(chunks.size + 1).apply {
        chunks.forEachIndexed { index, bytes -> this[index + 1] = this[index] + bytes.size }
    }
    fun open(): InputStream = object : InputStream() {
        private var position = 0
        private var marked = 0
        private fun index(): Int = offsets.binarySearch(position).let { if (it >= 0) it else -it - 2 }
        override fun available(): Int = offsets.last() - position
        override fun read(): Int {
            if (available() == 0) return -1
            val index = index()
            return (chunks[index][position++ - offsets[index]].toInt() and 255)
        }
        override fun read(destination: ByteArray, offset: Int, length: Int): Int {
            if (offset < 0 || length < 0 || offset > destination.size - length) throw IndexOutOfBoundsException()
            if (length == 0) return 0
            if (available() == 0) return -1
            val count = minOf(length, available())
            var copied = 0
            while (copied < count) {
                val index = index()
                val start = position - offsets[index]
                val part = minOf(count - copied, chunks[index].size - start)
                chunks[index].copyInto(destination, offset + copied, start, start + part)
                copied += part; position += part
            }
            return count
        }
        override fun skip(count: Long): Long {
            val skipped = count.coerceIn(0, available().toLong())
            position += skipped.toInt(); return skipped
        }
        override fun markSupported(): Boolean = true
        override fun mark(readlimit: Int) { marked = position }
        override fun reset() { position = marked }
    }
}

internal suspend fun snapshotAvatarSource(
    open: () -> InputStream?, maxBytes: Int = AVATAR_SOURCE_MAX_BYTES
): AvatarSourceSnapshot {
    require(maxBytes in 1..AVATAR_SOURCE_MAX_BYTES)
    val chunks = mutableListOf<ByteArray>()
    var total = 0
    currentCoroutineContext().ensureActive()
    (open() ?: throw AvatarImagePreparationException()).use { input ->
        while (true) {
            currentCoroutineContext().ensureActive()
            val buffer = ByteArray(minOf(65536, maxBytes - total + 1))
            var used = 0
            while (used < buffer.size) {
                currentCoroutineContext().ensureActive()
                val read = input.read(buffer, used, buffer.size - used)
                if (read < 0) break
                if (read == 0) {
                    val one = input.read()
                    if (one < 0) break
                    buffer[used++] = one.toByte()
                } else used += read
                if (total + used > maxBytes) throw AvatarSourceTooLargeException()
            }
            if (used == 0) break
            chunks.add(if (used == buffer.size) buffer else buffer.copyOf(used))
            total += used
            if (used < buffer.size) break
        }
    }
    currentCoroutineContext().ensureActive()
    return AvatarSourceSnapshot(chunks)
}
