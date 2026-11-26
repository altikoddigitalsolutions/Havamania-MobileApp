package com.havamania

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File

class AssistantPhotoDeviceTest {
    @get:Rule val compose = createComposeRule()
    private fun jpeg(width: Int = 200, height: Int = 100): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(35, 118, 166))
        return try { ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray() }
        finally { bitmap.recycle() }
    }
    @Test fun preparationRotatesAndRemovesGpsAndExif() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("assistant-photo-test", ".jpg", context.cacheDir)
        try {
            file.writeBytes(jpeg())
            ExifInterface(file).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                setAttribute(ExifInterface.TAG_MAKE, "PRIVATE_CAMERA")
                setLatLong(39.6, 27.8)
                saveAttributes()
            }
            val prepared = prepareAssistantPhotoBytes(file.readBytes())
            val bitmap = BitmapFactory.decodeByteArray(prepared.bytes, 0, prepared.bytes.size)
            try { assertEquals(100, bitmap.width); assertEquals(200, bitmap.height) }
            finally { bitmap.recycle() }
            val metadata = ExifInterface(prepared.bytes.inputStream())
            assertNull(metadata.latLong)
            assertNull(metadata.getAttribute(ExifInterface.TAG_MAKE))
            assertTrue(prepared.bytes.size <= 8 * 1024 * 1024)
        } finally { file.delete() }
    }
    @Test fun preparationRejectsNonImagesAndBoundsLargePhotos() {
        try { prepareAssistantPhotoBytes("not an image".toByteArray()); fail("Invalid photo accepted") }
        catch (_: IllegalArgumentException) { }
        val prepared = prepareAssistantPhotoBytes(jpeg(3000, 1000))
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(prepared.bytes, 0, prepared.bytes.size, options)
        assertTrue(maxOf(options.outWidth, options.outHeight) <= 2560)
    }
    @Test fun autumnLargeTextPhotoControlsAndPhotoOnlySend() = controls(AppTheme.AUTUMN, 2f)
    @Test fun darkPhotoControlsAndPhotoOnlySend() = controls(AppTheme.DARK, 1f)
    private fun controls(theme: AppTheme, scale: Float) {
        var photos by mutableStateOf(List(4) { AssistantPhoto(bytes = jpeg()) })
        var sending by mutableStateOf(false)
        var sends = 0
        compose.setContent {
            val colors = ThemeFactory.createColors(theme)
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale),
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                ProvideResponsiveLayout { MaterialTheme {
                    Box(Modifier.width(320.dp)) {
                        ChatInput("", {}, { sends++ }, sending, colors, HavamaniaTheme.styles,
                            photos = photos, onRemovePhoto = { id -> photos = photos.filterNot { it.id == id } },
                            onAttachPhoto = {})
                    }
                } }
            }
        }
        compose.onNodeWithContentDescription("Fotoğraf ekle").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Fotoğraf 1 ekini kaldır").performClick()
        compose.runOnIdle { assertEquals(3, photos.size) }
        compose.onNodeWithContentDescription("Fotoğraf ekle").assertIsEnabled()
        capture("assistant-photo-${theme.name.lowercase()}")
        compose.onNodeWithContentDescription("Mesaj gönder").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, sends); sending = true }
        compose.onNodeWithContentDescription("Mesaj gönder").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Fotoğraf 1 ekini kaldır").assertIsNotEnabled()
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Havamania-QA")
        }
        val uri = checkNotNull(context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        context.contentResolver.openOutputStream(uri)!!.use {
            check(compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }

}
