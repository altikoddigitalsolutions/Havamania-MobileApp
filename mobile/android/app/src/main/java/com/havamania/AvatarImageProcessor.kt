package com.havamania

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

internal fun avatarSampleSize(width: Int, height: Int): Int {
    require(width > 0 && height > 0)
    var sample = 1
    while (maxOf(width, height).toLong() > 1024L * sample) sample *= 2
    return sample
}

internal fun avatarOutputSize(width: Int, height: Int): Pair<Int, Int> {
    require(width > 0 && height > 0)
    val ratio = minOf(1.0, 512.0 / maxOf(width, height))
    return (width * ratio).roundToInt().coerceAtLeast(1) to (height * ratio).roundToInt().coerceAtLeast(1)
}

/** Caller retains ownership of source; identity may return the same bitmap. */
internal fun orientAvatarBitmap(source: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        2 -> matrix.setScale(-1f, 1f)
        3 -> matrix.setRotate(180f)
        4 -> matrix.setScale(1f, -1f)
        5 -> matrix.setValues(floatArrayOf(0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f))
        6 -> matrix.setRotate(90f)
        7 -> matrix.setValues(floatArrayOf(0f, -1f, 0f, -1f, 0f, 0f, 0f, 0f, 1f))
        8 -> matrix.setRotate(270f)
        else -> return source
    }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

/** Own every bitmap inside the worker context; only JPEG bytes cross the dispatcher boundary. */
internal suspend fun prepareAvatarJpeg(open: () -> InputStream?): ByteArray = withContext(Dispatchers.Default) {
    val source = snapshotAvatarSource(open)
    fun stream() = source.open()
    val orientation = try {
        stream().use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
    } catch (_: java.io.IOException) { ExifInterface.ORIENTATION_NORMAL }
    currentCoroutineContext().ensureActive()
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    stream().use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw AvatarImagePreparationException()
    val options = BitmapFactory.Options().apply { inSampleSize = avatarSampleSize(bounds.outWidth, bounds.outHeight) }
    currentCoroutineContext().ensureActive()
    var working: Bitmap? = null
    try {
        // Acquire ownership before use closes the stream: close itself may fail.
        stream().use { working = BitmapFactory.decodeStream(it, null, options) }
        val decoded = working ?: throw AvatarImagePreparationException()
        currentCoroutineContext().ensureActive()
        val oriented = orientAvatarBitmap(decoded, orientation)
        if (oriented !== decoded) { working = oriented; decoded.recycle() }
        currentCoroutineContext().ensureActive()
        val (width, height) = avatarOutputSize(oriented.width, oriented.height)
        val scaled = Bitmap.createScaledBitmap(oriented, width, height, true)
        if (scaled !== oriented) { working = scaled; oriented.recycle() }
        currentCoroutineContext().ensureActive()
        val jpegBitmap = if (scaled.hasAlpha()) {
            val opaque = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
            working = opaque
            try { Canvas(opaque).apply { drawColor(Color.WHITE); drawBitmap(scaled, 0f, 0f, null) } }
            finally { scaled.recycle() }
            opaque
        } else scaled
        currentCoroutineContext().ensureActive()
        ByteArrayOutputStream().use { output ->
            if (!jpegBitmap.compress(Bitmap.CompressFormat.JPEG, 75, output)) throw AvatarImagePreparationException()
            currentCoroutineContext().ensureActive()
            output.toByteArray()
        }
    } finally { working?.recycle() }
}
