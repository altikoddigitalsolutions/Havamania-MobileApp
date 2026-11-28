package com.havamania

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.io.ByteArrayOutputStream
import org.junit.Rule
import org.junit.Test

class AssistantMessagePhotosDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun eachThumbnailOpensAndClosesWithoutSendingAnything() {
        val image = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().also { image.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
        image.recycle()
        val photos = listOf(AssistantPhoto("first", bytes), AssistantPhoto("second", bytes))
        compose.setContent { AssistantMessagePhotos(photos) }
        for (index in 1..2) {
            compose.onNodeWithContentDescription("Fotoğraf $index, büyüt").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Büyütülmüş fotoğraf").assertIsDisplayed()
            compose.onNodeWithText("Fotoğrafı kapat").performClick()
            compose.onNodeWithContentDescription("Büyütülmüş fotoğraf").assertDoesNotExist()
        }
    }
}
