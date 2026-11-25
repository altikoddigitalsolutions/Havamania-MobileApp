package com.havamania

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.*
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ProfileSettingsVisualDeviceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun narrowSettingsStackLongValueAndRejectDisabledClick() {
        var clicks = 0
        val title = "Asistan üslubu"
        val value = "Ayrıntılı açıklamalar ve kişiselleştirilmiş öneriler"
        compose.setContent { Fixture(AppTheme.LIGHT, 2f) {
            Column(Modifier.width(320.dp)) {
                SettingsClickRow(title, value, Icons.Rounded.Info) { clicks++ }
                SettingsClickRow("Kullanılamayan seçenek", null, Icons.Rounded.Info, enabled = false) { clicks++ }
            }
        } }
        val heading = compose.onNodeWithText(title, useUnmergedTree = true).assertIsDisplayed().getUnclippedBoundsInRoot()
        val detail = compose.onNodeWithText(value, useUnmergedTree = true).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(detail.top >= heading.bottom)
        compose.onNode(hasClickAction() and hasText(title)).performClick()
        compose.onNode(hasClickAction() and hasText("Kullanılamayan seçenek")).assertIsNotEnabled().performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
        image("settings-stacked-large-20261125")
    }
    @Test fun lightLargeInterestChipKeepsSelectionAfterRestore() = interests(AppTheme.LIGHT, 2f)
    @Test fun darkInterestChipKeepsSelectionAfterRestore() = interests(AppTheme.DARK, 1f)
    private fun interests(theme: AppTheme, scale: Float) {
        val restore = StateRestorationTester(compose)
        var selected by mutableStateOf(emptySet<String>())
        var calls = 0
        val item = InterestItem("qa-interest", "Bulut fotoğrafçılığı ve doğa gözlemi", Icons.Rounded.Info)
        val category = InterestCategory("Doğa ve keşif", Icons.Rounded.Info,
            "Hava koşullarına göre dışarıda yapabileceklerinizi seçin.", listOf(item))
        restore.setContent { Fixture(theme, scale) {
            Column(Modifier.width(320.dp).heightIn(max = 650.dp).verticalScroll(rememberScrollState())) {
                PremiumInterestCategoryCard(category, selected) { selected = setOf(it); calls++ }
            }
        } }
        compose.onNode(hasClickAction() and hasText(category.title)).performClick()
        compose.onNode(isToggleable() and hasText(item.label)).performScrollTo().assertHeightIsAtLeast(48.dp)
            .assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText(item.label, useUnmergedTree = true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) {
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>(); it(layouts)
            assertFalse("Interest text must not overflow: ${layouts.single().size}", layouts.single().hasVisualOverflow)
        }
        restore.emulateSavedInstanceStateRestore()
        compose.onNode(isToggleable() and hasText(item.label)).performScrollTo().assertIsOn()
        compose.runOnIdle { assertEquals(1, calls) }
        image("profile-interest-${theme.name.lowercase()}-20261125")
    }
    @Test fun transportationAndOutdoorArePresentBeforeMoreIsOpened() {
        compose.setContent { Fixture(AppTheme.DARK, 1f) {
            Column(Modifier.width(320.dp).height(650.dp).verticalScroll(rememberScrollState())) {
                PremiumInterestsSection(emptySet()) {}
            }
        } }
        compose.onNodeWithText("Ulaşım").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Outdoor").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Sağlık").assertDoesNotExist()
        compose.onNodeWithText("Daha fazla göster").performScrollTo().performClick()
        compose.onNodeWithText("Sağlık").performScrollTo().assertIsDisplayed()
    }
    @Test fun themeSelectionHasOneRadioActionPerOption() {
        var current by mutableStateOf(AppTheme.LIGHT); var calls = 0
        compose.setContent { Fixture(AppTheme.LIGHT, 2f) {
            ThemeSelectionDialog(current, { current = it; calls++ }, {})
        } }
        compose.onNode(isSelectable() and hasText(AppTheme.WINTER.title)).performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(AppTheme.WINTER, current); assertEquals(1, calls) }
    }
    @Test fun toneSelectionHasOneRadioActionPerOption() {
        var current by mutableStateOf(AssistantTone.DENGELI); var calls = 0
        compose.setContent { Fixture(AppTheme.DARK, 2f) {
            ToneSelectionDialog(current, { current = it; calls++ }, {})
        } }
        compose.onNode(isSelectable() and hasText(AssistantTone.KISA_NET.title)).performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(AssistantTone.KISA_NET, current); assertEquals(1, calls) }
    }
    @Composable private fun Fixture(theme: AppTheme, scale: Float, content: @Composable () -> Unit) {
        val density = LocalDensity.current.density; val colors = ThemeFactory.createColors(theme)
        CompositionLocalProvider(LocalDensity provides Density(density, scale), LocalHavamaniaColors provides colors,
            LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
            ProvideResponsiveLayout { MaterialTheme { Box(Modifier.background(colors.background)) { content() } } }
        }
    }
    private fun image(name: String) {
        val resolver = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Havamania-QA")
        }
        val uri = checkNotNull(resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        resolver.openOutputStream(uri)!!.use {
            check(compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}
