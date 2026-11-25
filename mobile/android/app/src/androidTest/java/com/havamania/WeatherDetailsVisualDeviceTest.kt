package com.havamania

import android.content.ContentValues
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import com.havamania.ui.theme.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WeatherDetailsVisualDeviceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun darkDetailsUseOneSectionAndPreserveAllValues() = verify(AppTheme.DARK, 420, 1f)
    @Test fun lightLargeTextStacksMetricsAndKeepsSolarTimesReachable() = verify(AppTheme.LIGHT, 320, 2f)
    private fun verify(theme: AppTheme, width: Int, fontScale: Float) {
        val sample = WeatherData(details = listOf(
            WeatherDetailData("Hissedilen", "16°", "Rüzgâr etkisi dâhil"),
            WeatherDetailData("Yağış olasılığı", "%8", "Son tahmin"),
            WeatherDetailData("Görüş", "34 km", "Yatay görüş mesafesi")
        ), windDirectionDegrees = 178, windDirectionLabel = "G",
            sunriseTime = "07:47", solarNoon = "12:51", sunsetTime = "17:55")
        compose.setContent {
            val density = LocalDensity.current.density
            val colors = ThemeFactory.createColors(theme)
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale),
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                ProvideResponsiveLayout { MaterialTheme {
                    Box(Modifier.width(width.dp).height(650.dp).background(colors.background)) {
                        Column(Modifier.verticalScroll(rememberScrollState())) { WeatherDetailsPanel(sample) }
                    }
                } }
            }
        }
        compose.onNodeWithText("HİSSEDİLEN").assertIsDisplayed()
        if (fontScale > 1.3f) {
            val first = compose.onNodeWithText("HİSSEDİLEN").getUnclippedBoundsInRoot()
            val second = compose.onNodeWithText("YAĞIŞ OLASILIĞI").getUnclippedBoundsInRoot()
            assertTrue("Metrics must stack at large font", second.top > first.bottom)
        }
        saveImage("weather-details-${theme.name.lowercase()}")
        listOf("34 km", "RÜZGAR YÖNÜ", "Gün doğumu", "Gündüz ortası", "Gün batımı", "17:55").forEach {
            compose.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
    }
    private fun saveImage(name: String) {
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Havamania-QA")
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        resolver.openOutputStream(uri)!!.use {
            check(compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}
