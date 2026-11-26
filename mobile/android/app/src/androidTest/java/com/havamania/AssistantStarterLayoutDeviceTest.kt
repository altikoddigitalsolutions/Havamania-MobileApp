package com.havamania

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.havamania.ui.theme.*
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real starter components, synthetic weather only; no account or network access. */
class AssistantStarterLayoutDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun longTripCardKeepsItsActionUsableAtLargeFont() {
        val enabled = mutableStateOf(false)
        var asks = 0
        val trip = TravelPlan(id = "local-layout-trip", userId = "local-layout-owner", city = "Şebinkarahisar",
            latitude = 40.28, longitude = 38.42,
            startDate = java.time.LocalDate.of(2027, 11, 28), endDate = java.time.LocalDate.of(2027, 11, 30))
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.LIGHT)
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f),
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                    ActiveTripContextCard(trip, colors, enabled.value) { asks++ }
                }
            }
        }
        compose.onNodeWithText(trip.displayName).assertIsDisplayed()
        compose.onNodeWithText("28 Kasım 2027").assertIsDisplayed()
        val action = compose.onNodeWithText("Havasını sor")
        action.performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, asks); enabled.value = true }
        action.assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, asks) }
        for (text in listOf(trip.displayName, "28 Kasım 2027", "Havasını sor")) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(text, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue(layouts.isNotEmpty())
            assertTrue(layouts.none { it.multiParagraph.didExceedMaxLines ||
                (0 until it.lineCount).any { line -> it.getLineLeft(line) < -1f || it.getLineRight(line) > it.size.width + 1f } })
        }
    }

    @Test fun themesAndWidthsKeepLongWeatherAndSuggestionsReadable() {
        val configuration = mutableStateOf(Triple(AppTheme.LIGHT, 320, 2f))
        val weather = WeatherData(cityName = "Balıkesir", districtName = "Altıeylül Paşa Alanı Mahallesi",
            temperature = "−12°", feelsLike = "−18°", condition = "Yer yer kuvvetli sağanak yağış", isStale = true)
        val sent = mutableListOf<String>()
        compose.setContent {
            val (theme, width, scale) = configuration.value
            val colors = ThemeFactory.createColors(theme)
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, scale),
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                key(theme, width) {
                    Column(Modifier.width(width.dp).fillMaxHeight().background(colors.background)
                        .testTag("starter_layout").verticalScroll(rememberScrollState())) {
                        AssistantWeatherCard(weather, colors)
                        AssistantStarterSuggestions(colors, true) { sent.add(it) }
                    }
                }
            }
        }
        val themes = listOf(AppTheme.LIGHT, AppTheme.DARK, AppTheme.SPRING,
            AppTheme.SUMMER, AppTheme.AUTUMN, AppTheme.WINTER)
        for (theme in themes) for (width in listOf(320, 760)) {
            compose.runOnIdle { configuration.value = Triple(theme, width, if (width == 320) 2f else 1f) }
            val texts = listOf("Balıkesir · Altıeylül Paşa Alanı Mahallesi", weather.condition,
                "Hissedilen: ${weather.feelsLike}", "Son kaydedilen hava bilgisi") + assistantStarterPrompts
            for (text in texts) {
                val node = compose.onNodeWithText(text, useUnmergedTree = true)
                node.performScrollTo().assertIsDisplayed()
                val layouts = mutableListOf<TextLayoutResult>()
                node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertTrue("Missing layout: $text", layouts.isNotEmpty())
                // Paragraph width can retain the offered constraint after Text shrinks.
                // Check actual line bounds, missing lines and height instead.
                assertTrue("Clipped text: $theme/$width/$text: ${layouts.map { "size=${it.size}, paragraphWidth=${it.multiParagraph.width}, paragraphHeight=${it.multiParagraph.height}" }}",
                    layouts.none { it.multiParagraph.didExceedMaxLines ||
                        (0 until it.lineCount).any { line ->
                            it.getLineLeft(line) < -1f || it.getLineRight(line) > it.size.width + 1f
                        } || it.multiParagraph.height - it.size.height >= 1f })
            }
            assistantStarterPrompts.forEach { compose.onNodeWithText(it).performScrollTo().performClick() }
            compose.onNodeWithText(texts.first()).performScrollTo()
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(
                "mkdir -p /sdcard/Pictures/Havamania-QA/starter-20261126")).use { it.readBytes() }
            android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(
                "screencap -p /sdcard/Pictures/Havamania-QA/starter-20261126/${theme.name}-$width.png")).use { it.readBytes() }
        }
        compose.runOnIdle { assertEquals(themes.size * 2 * assistantStarterPrompts.size, sent.size) }
    }
}
