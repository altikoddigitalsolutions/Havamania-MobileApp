package com.havamania

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.util.UUID

internal const val ASSISTANT_MAX_PHOTOS = 4
internal class PhotosDisabledException : Exception("Fotoğraf gönderme şu anda kullanılamıyor.")

// Bytes live only in this ViewModel session; never written to Room, disk or logs.
class AssistantPhoto(val id: String = UUID.randomUUID().toString(), val bytes: ByteArray)

internal fun prepareAssistantPhoto(resolver: ContentResolver, uri: Uri): AssistantPhoto {
    require(uri.scheme == "content") { "Bir fotoğraf seçin." }
    val raw = resolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var count: Int
        while (input.read(buffer).also { count = it } != -1) {
            require(output.size() + count <= 20 * 1024 * 1024) { "Kaynak fotoğraf çok büyük." }
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    } ?: error("Fotoğraf okunamadı.")
    return prepareAssistantPhotoBytes(raw)
}

internal fun prepareAssistantPhotoBytes(raw: ByteArray): AssistantPhoto {
    require(raw.size <= 20 * 1024 * 1024) { "Kaynak fotoğraf çok büyük." }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0 && bounds.outWidth.toLong() * bounds.outHeight <= 50_000_000L) {
        "Fotoğraf biçimi desteklenmiyor veya çözünürlüğü çok yüksek. JPEG, PNG veya WebP seçin."
    }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2560) sample *= 2
    val decoded = BitmapFactory.decodeByteArray(raw, 0, raw.size, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: error("Fotoğraf açılamadı.")
    var transformed: Bitmap? = null
    try {
        val exif = runCatching { ExifInterface(raw.inputStream()) }.getOrNull()
        val matrix = Matrix().apply {
            if (exif?.isFlipped == true) postScale(-1f, 1f)
            postRotate((exif?.rotationDegrees ?: 0).toFloat())
        }
        transformed = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        val output = ByteArrayOutputStream()
        check(transformed.compress(Bitmap.CompressFormat.JPEG, 85, output)) { "Fotoğraf hazırlanamadı." }
        require(output.size() <= 8 * 1024 * 1024) { "Fotoğraf çok büyük. Daha küçük bir fotoğraf seçin." }
        // A fresh JPEG carries no source EXIF/GPS metadata.
        return AssistantPhoto(bytes = output.toByteArray())
    } finally {
        if (transformed !== decoded) transformed?.recycle()
        decoded.recycle()
    }
}
