package com.havamania

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Constrained phone viewport on the tablet; never changes device display or font settings. */
class ProfileUnavailableResponsiveUiTest {
    @get:Rule val compose = createComposeRule()
    private fun show(theme: AppTheme, width: Int, height: Int, state: ProfileState,
        retry: () -> Unit = {}, back: () -> Unit = {}) {
        compose.setContent {
            val colors = ThemeFactory.createColors(theme)
            val density = LocalDensity.current.density
            val config = Configuration(LocalConfiguration.current).apply {
                screenWidthDp = width; screenHeightDp = height
            }
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalConfiguration provides config, LocalDensity provides Density(density, 2f)) {
                MaterialTheme(colorScheme = ThemeManager.getColorScheme(theme)) {
                    ProvideResponsiveLayout {
                        Box(Modifier.requiredSize(width.dp, height.dp).clipToBounds()
                            .background(colors.background).testTag("profile-viewport")) {
                            ProfileUnavailableContent("fixture", state, 0, retry, back)
                        }
                    }
                }
            }
        }
    }
    private fun assertNotTruncated(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        compose.runOnIdle {
            assertTrue(layouts.isNotEmpty())
            assertTrue("Text overflows its layout", layouts.all { !it.hasVisualOverflow })
        }
    }
    private fun snapshot(name: String) {
        val bitmap = compose.onNodeWithTag("profile-viewport").captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        require(name.matches(Regex("[a-z0-9.-]+")))
        val source = java.io.File(context.externalCacheDir, name)
        source.outputStream().use {
            assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
        }
        // AGP uninstalls QA APKs after the run; retain only these synthetic images for host visual review.
        val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "cp ${source.absolutePath} /data/local/tmp/havamania-$name")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
        val check = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "ls /data/local/tmp/havamania-$name")
        val retained = android.os.ParcelFileDescriptor.AutoCloseInputStream(check).bufferedReader().use { it.readText().trim() }
        assertEquals("/data/local/tmp/havamania-$name", retained)
    }
    @Test fun lightCompactPhoneLargeTextKeepsLongErrorAndActionsReachable() {
        var retries = 0; var backs = 0
        show(AppTheme.LIGHT, 320, 480, ProfileState.Error(PROFILE_CONNECTION_ERROR), { retries++ }, { backs++ })
        assertNotTruncated("Profil yüklenemedi")
        assertNotTruncated(PROFILE_CONNECTION_ERROR)
        compose.onNodeWithText("Tekrar dene").performScrollTo().assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText("Geri dön").performScrollTo().assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(1, retries); assertEquals(1, backs) }
        snapshot("profile-error-light-320.png")
    }
    @Test fun darkShortLandscapeLargeTextKeepsActionsReachable() {
        var retries = 0; var backs = 0
        show(AppTheme.DARK, 480, 240, ProfileState.Error(PROFILE_DATA_ERROR), { retries++ }, { backs++ })
        assertNotTruncated(PROFILE_DATA_ERROR)
        compose.onNodeWithText("Tekrar dene").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Geri dön").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, retries); assertEquals(1, backs) }
        snapshot("profile-error-dark-landscape.png")
    }
    @Test fun loadingHasNamedProgressAndPoliteHeading() {
        compose.mainClock.autoAdvance = false
        show(AppTheme.LIGHT, 320, 480, ProfileState.Loading)
        compose.onNodeWithContentDescription("Profil bilgileri yükleniyor").assertExists()
        compose.onNodeWithText("Profil yükleniyor…")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        compose.mainClock.advanceTimeBy(8_100)
        compose.onNodeWithContentDescription("Profil bilgileri yükleniyor").assertDoesNotExist()
        compose.onNodeWithText("Profil henüz yüklenemedi").assertExists()
        compose.mainClock.autoAdvance = true // Real scroll needs frame-clock progress.
        compose.onNodeWithText("Tekrar dene").performScrollTo().assertIsDisplayed()
    }
}