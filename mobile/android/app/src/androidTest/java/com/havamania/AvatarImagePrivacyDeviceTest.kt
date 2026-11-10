package com.havamania

import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AvatarImagePrivacyDeviceTest {
    private fun jpeg(): ByteArray {
        val bitmap = Bitmap.createBitmap(24, 32, Bitmap.Config.ARGB_8888)
        return try { ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it); it.toByteArray() } }
        finally { bitmap.recycle() }
    }

    @Test fun preparedJpegDoesNotCarrySourceGpsDeviceOrComment() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "avatar-private-${UUID.randomUUID()}.jpg")
        val tags = mapOf(
            ExifInterface.TAG_MAKE to "PRIVATE_MAKER",
            ExifInterface.TAG_MODEL to "PRIVATE_MODEL",
            ExifInterface.TAG_ARTIST to "PRIVATE_ARTIST",
            ExifInterface.TAG_IMAGE_DESCRIPTION to "PRIVATE_DESCRIPTION",
            ExifInterface.TAG_USER_COMMENT to "PRIVATE_COMMENT",
            ExifInterface.TAG_DATETIME_ORIGINAL to "2020:01:02 03:04:05",
            ExifInterface.TAG_GPS_LATITUDE to "41/1,0/1,0/1",
            ExifInterface.TAG_GPS_LATITUDE_REF to "N",
            ExifInterface.TAG_GPS_LONGITUDE to "29/1,0/1,0/1",
            ExifInterface.TAG_GPS_LONGITUDE_REF to "E")
        try {
            file.writeBytes(jpeg())
            ExifInterface(file.absolutePath).apply {
                tags.forEach { (tag, value) -> setAttribute(tag, value) }; saveAttributes()
            }
            val original = ExifInterface(file.absolutePath)
            tags.keys.forEach { assertNotNull("Source tag $it must exist", original.getAttribute(it)) }
            val result = prepareAvatarJpeg { file.inputStream() }
            val outputExif = ExifInterface(ByteArrayInputStream(result))
            tags.keys.forEach { assertNull("Output tag $it", outputExif.getAttribute(it)) }
            assertFalse(String(result, Charsets.ISO_8859_1).contains("PRIVATE_"))
        } finally { file.delete() }
    }

    @Test fun revokedAccessDuringSnapshotProducesNoPreparedImage() = runBlocking {
        val bytes = jpeg()
        var opened = 0
        var closed = 0
        try {
            prepareAvatarJpeg {
                opened++
                object : ByteArrayInputStream(bytes) {
                    private var reads = 0
                    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                        if (++reads > 1) throw SecurityException("private-content-uri")
                        return super.read(buffer, offset, minOf(length, 8))
                    }
                    override fun close() { closed++; super.close() }
                }
            }
            fail("No image after revoked access")
        } catch (error: SecurityException) {
            assertFalse(avatarErrorMessage(error).contains("private-content-uri"))
        }
        assertEquals(1, opened); assertEquals(1, closed)
    }

    @Test fun sourceStreamCloseFailureDoesNotReturnPartialImage() = runBlocking {
        val bytes = jpeg()
        var opened = 0
        var closed = 0
        try {
            prepareAvatarJpeg {
                val index = ++opened
                object : FilterInputStream(ByteArrayInputStream(bytes)) {
                    override fun close() { super.close(); closed++; if (index == 1) throw IOException("injected close failure") }
                }
            }
            fail("No partial JPEG after close failure")
        } catch (_: IOException) { }
        assertEquals(1, opened); assertEquals(1, closed)
    }

    @Test fun cancellationAfterSnapshotCannotReturnJpeg() = runBlocking {
        val bytes = jpeg()
        var opened = 0
        var closed = 0
        lateinit var work: Job
        work = launch(start = CoroutineStart.LAZY) {
            prepareAvatarJpeg {
                val index = ++opened
                object : ByteArrayInputStream(bytes) {
                    override fun close() { super.close(); closed++; if (index == 1) work.cancel() }
                }
            }
            fail("No JPEG from cancelled operation")
        }
        work.start(); work.join()
        assertTrue(work.isCancelled); assertEquals(1, opened); assertEquals(1, closed)
    }
    @Test fun changingProviderIsOpenedOnlyOnce() = runBlocking {
        val first = jpeg()
        var opens = 0
        val result = prepareAvatarJpeg {
            opens++
            ByteArrayInputStream(if (opens == 1) first else byteArrayOf(1, 2, 3))
        }
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(result, 0, result.size)
        try { assertEquals(24, bitmap.width); assertEquals(32, bitmap.height); assertEquals(1, opens) }
        finally { bitmap.recycle() }
    }

}
