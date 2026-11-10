package com.havamania

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AvatarFormatDeviceTest {
    private fun encode(bitmap: Bitmap, format: Bitmap.CompressFormat): ByteArray =
        ByteArrayOutputStream().use { out -> assertTrue(bitmap.compress(format, 100, out)); out.toByteArray() }

    @Test fun transparentPngUsesWhiteBackgroundAndPreservesOpaqueColor() = runBlocking {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        for (y in 32..63) for (x in 32..63) bitmap.setPixel(x, y, Color.RED)
        val bytes = try { encode(bitmap, Bitmap.CompressFormat.PNG) } finally { bitmap.recycle() }
        val jpeg = prepareAvatarJpeg { ByteArrayInputStream(bytes) }
        val output = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
        try {
            val white = output.getPixel(8, 8)
            assertTrue(Color.red(white) > 240 && Color.green(white) > 240 && Color.blue(white) > 240)
            val red = output.getPixel(48, 48)
            assertTrue(Color.red(red) > 200 && Color.green(red) < 50 && Color.blue(red) < 50)
        } finally { output.recycle() }
    }
    @Test fun partialAlphaIsCompositedBeforeJpegEncoding() = runBlocking {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.argb(128, 255, 0, 0))
        val bytes = try { encode(bitmap, Bitmap.CompressFormat.PNG) } finally { bitmap.recycle() }
        val jpeg = prepareAvatarJpeg { ByteArrayInputStream(bytes) }
        val output = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
        try {
            val pixel = output.getPixel(32, 32)
            assertTrue(Color.red(pixel) > 230)
            assertTrue(Color.green(pixel) in 105..150 && Color.blue(pixel) in 105..150)
        } finally { output.recycle() }
    }
    @Suppress("DEPRECATION")
    @Test fun pngAndWebpProduceValidJpegWithoutUpscaling() = runBlocking {
        for (format in listOf(Bitmap.CompressFormat.PNG, Bitmap.CompressFormat.WEBP)) {
            val bitmap = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.GREEN)
            val bytes = try { encode(bitmap, format) } finally { bitmap.recycle() }
            val jpeg = prepareAvatarJpeg { ByteArrayInputStream(bytes) }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, bounds)
            assertEquals("image/jpeg", bounds.outMimeType)
            assertEquals(64, bounds.outWidth); assertEquals(32, bounds.outHeight)
        }
    }
    @Test fun exifBeyondFirstSourceChunkIsApplied() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "avatar-chunk-${UUID.randomUUID()}.jpg")
        try {
            val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
            file.writeBytes(try { encode(bitmap, Bitmap.CompressFormat.JPEG) } finally { bitmap.recycle() })
            ExifInterface(file.absolutePath).apply { setAttribute(ExifInterface.TAG_ORIENTATION, "6"); saveAttributes() }
            val original = file.readBytes()
            val padded = ByteArrayOutputStream().use { out ->
                out.write(original, 0, 2)
                out.write(byteArrayOf(255.toByte(), 226.toByte(), 255.toByte(), 255.toByte()))
                out.write(ByteArray(65533))
                out.write(original, 2, original.size - 2); out.toByteArray()
            }
            assertEquals(6, ExifInterface(ByteArrayInputStream(padded)).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1))
            val jpeg = prepareAvatarJpeg { ByteArrayInputStream(padded) }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, bounds)
            assertEquals(200, bounds.outWidth); assertEquals(100, bounds.outHeight)
        } finally { file.delete() }
    }
}
