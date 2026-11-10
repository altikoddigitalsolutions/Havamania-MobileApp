package com.havamania

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FilterInputStream
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AvatarImageProcessorDeviceTest {
    @Test fun allEightExifOrientationsMapPixelsCorrectly() {
        val colors = intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN)
        val expected = listOf(
            intArrayOf(0,1,2,3,4,5), intArrayOf(1,0,3,2,5,4), intArrayOf(5,4,3,2,1,0),
            intArrayOf(4,5,2,3,0,1), intArrayOf(0,2,4,1,3,5), intArrayOf(4,2,0,5,3,1),
            intArrayOf(5,3,1,4,2,0), intArrayOf(1,3,5,0,2,4))
        for (orientation in 1..8) {
            val source = Bitmap.createBitmap(colors, 2, 3, Bitmap.Config.ARGB_8888)
            val result = orientAvatarBitmap(source, orientation)
            try {
                val width = if (orientation >= 5) 3 else 2
                assertEquals(width, result.width)
                val pixels = IntArray(6)
                result.getPixels(pixels, 0, width, 0, 0, width, 6 / width)
                assertArrayEquals("EXIF $orientation", expected[orientation - 1].map { colors[it] }.toIntArray(), pixels)
            } finally { if (result !== source) result.recycle(); source.recycle() }
        }
    }

    @Test fun largeJpegIsDownsampledAndEveryStreamCloses() = runBlocking {
        val source = Bitmap.createBitmap(4096, 2048, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.BLUE)
        val encoded = ByteArrayOutputStream().use { out -> source.compress(Bitmap.CompressFormat.JPEG, 90, out); out.toByteArray() }
        source.recycle()
        var opened = 0
        var closed = 0
        val jpeg = prepareAvatarJpeg {
            opened++
            object : FilterInputStream(ByteArrayInputStream(encoded)) {
                private var done = false
                override fun close() { if (!done) { done = true; closed++ }; super.close() }
            }
        }
        val output = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
        try { assertEquals(512, output.width); assertEquals(256, output.height) }
        finally { output.recycle() }
        assertEquals(1, opened); assertEquals(opened, closed)
    }

    @Test fun realExifTagIsReadBeforeJpegEncoding() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "avatar-exif-${UUID.randomUUID()}.jpg")
        val source = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        try {
            file.outputStream().use { source.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            source.recycle()
            ExifInterface(file.absolutePath).apply { setAttribute(ExifInterface.TAG_ORIENTATION, "6"); saveAttributes() }
            val jpeg = prepareAvatarJpeg { file.inputStream() }
            val output = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
            try { assertEquals(200, output.width); assertEquals(100, output.height) }
            finally { output.recycle() }
        } finally { if (!source.isRecycled) source.recycle(); file.delete() }
    }

    @Test fun corruptInputFailsWithoutLeavingStreamsOpen() = runBlocking {
        var opened = 0
        var closed = 0
        try {
            prepareAvatarJpeg {
                opened++
                object : ByteArrayInputStream(byteArrayOf(1, 2, 3)) {
                    override fun close() { closed++; super.close() }
                }
            }
            fail("Expected invalid image")
        } catch (_: AvatarImagePreparationException) { }
        assertEquals(opened, closed)
    }
}
