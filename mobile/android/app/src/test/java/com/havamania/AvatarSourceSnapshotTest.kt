package com.havamania

import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AvatarSourceSnapshotTest {
    @Test fun repeatedPassesUseOneProviderReadAndKeepIdenticalBytes() = runBlocking {
        val bytes = ByteArray(150000) { (it % 251).toByte() }
        var opens = 0
        var closes = 0
        val source = snapshotAvatarSource({
            opens++
            object : ByteArrayInputStream(bytes) { override fun close() { closes++; super.close() } }
        })
        repeat(3) { source.open().use { assertArrayEquals(bytes, it.readBytes()) } }
        assertEquals(1, opens); assertEquals(1, closes)
    }
    @Test fun oversizedSourceIsRejectedAndClosed() = runBlocking {
        var closed = false
        try {
            snapshotAvatarSource({ object : ByteArrayInputStream(byteArrayOf(1,2,3)) {
                override fun close() { closed = true; super.close() }
            } }, maxBytes = 2)
            fail("Expected size limit")
        } catch (_: AvatarSourceTooLargeException) { }
        assertTrue(closed)
    }
    @Test fun exactLimitAndZeroLengthReadAreHandled() = runBlocking {
        var first = true
        val source = snapshotAvatarSource({ object : ByteArrayInputStream(byteArrayOf(1,2,3)) {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (first) { first = false; return 0 }
                return super.read(buffer, offset, length)
            }
        } }, maxBytes = 3)
        source.open().use { assertArrayEquals(byteArrayOf(1,2,3), it.readBytes()) }
    }
    @Test fun markResetAndBulkReadCrossChunkBoundary() = runBlocking {
        val bytes = ByteArray(150000) { (it % 251).toByte() }
        val snapshot = snapshotAvatarSource({ ByteArrayInputStream(bytes) })
        snapshot.open().use { input ->
            assertEquals(bytes.size, input.available())
            assertTrue(input.markSupported())
            assertEquals(65530L, input.skip(65530))
            input.mark(100000)
            val block = ByteArray(20)
            assertEquals(20, input.read(block))
            assertArrayEquals(bytes.copyOfRange(65530, 65550), block)
            input.reset()
            assertEquals(20, input.read(block))
            assertArrayEquals(bytes.copyOfRange(65530, 65550), block)
            assertEquals((bytes.size - 65550).toLong(), input.skip(Long.MAX_VALUE))
            assertEquals(-1, input.read())
            assertEquals(0, input.read(ByteArray(0)))
        }
    }
    @Test fun snapshotReadersHaveIndependentPositions() = runBlocking {
        val snapshot = snapshotAvatarSource({ ByteArrayInputStream(byteArrayOf(255.toByte(), 2, 3)) })
        snapshot.open().use { first -> snapshot.open().use { second ->
            assertEquals(255, first.read()); assertEquals(3, second.available())
            assertEquals(0L, second.skip(-1)); assertEquals(255, second.read())
            first.reset(); assertEquals(255, first.read())
        } }
    }
    @Test fun invalidReadRangeFailsWithoutAdvancing() = runBlocking {
        val snapshot = snapshotAvatarSource({ ByteArrayInputStream(byteArrayOf(1,2,3)) })
        snapshot.open().use { input ->
            try { input.read(ByteArray(1), 1, 1); fail("Expected invalid range") }
            catch (_: IndexOutOfBoundsException) { }
            assertEquals(1, input.read())
        }
    }

    @Test fun sizeErrorDoesNotExposeSourceDetails() {
        assertTrue(avatarErrorMessage(AvatarSourceTooLargeException()).contains("20 MB"))
    }
}
