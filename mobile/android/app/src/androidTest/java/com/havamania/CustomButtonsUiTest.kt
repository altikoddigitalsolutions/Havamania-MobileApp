package com.havamania

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.*
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CustomButtonsUiTest {
    @get:Rule val compose = createComposeRule()
    private fun show(scale: Float = 2f, content: @Composable () -> Unit) {
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.LIGHT)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalDensity provides Density(LocalDensity.current.density, scale)) {
                ProvideResponsiveLayout { MaterialTheme(colorScheme = ThemeManager.getColorScheme(AppTheme.LIGHT)) {
                    Column(Modifier.requiredWidth(280.dp).background(colors.background),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
                } }
            }
        }
    }
    private fun assertWrapsWithoutTruncation(text: String) {
        val result = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(result) }
        compose.runOnIdle {
            assertTrue(result.isNotEmpty())
            assertTrue(result.all { it.lineCount > 1 && !it.hasVisualOverflow })
        }
    }
    @Test fun largeLabelsAndIconsWrapAndGrowOnCompactWidth() {
        val primary = "Kişiselleştirme tercihlerini kaydet ve devam et"
        val secondary = "Önceki ekrana geri dön ve seçimleri yeniden incele"
        var clicks = 0
        show {
            HavamaniaPrimaryButton(primary, { clicks++ }, Modifier.testTag("primary"), icon = Icons.Rounded.ArrowBack)
            HavamaniaSecondaryButton(secondary, { clicks++ }, Modifier.testTag("secondary"), icon = Icons.Rounded.ArrowBack)
        }
        assertWrapsWithoutTruncation(primary)
        assertWrapsWithoutTruncation(secondary)
        compose.onNodeWithTag("primary").assertHeightIsAtLeast(56.dp).assertIsDisplayed().performClick()
        compose.onNodeWithTag("secondary").assertHeightIsAtLeast(56.dp).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(2, clicks) }
    }
    @Test fun disabledAndBusyButtonsRejectClicksAndBusyStateIsNamed() {
        val enabled = mutableStateOf(false)
        val busy = mutableStateOf(false)
        var clicks = 0
        show {
            HavamaniaPrimaryButton("Kaydet", { clicks++ }, Modifier.testTag("primary"), enabled = enabled.value, isLoading = busy.value)
            HavamaniaSecondaryButton("Geri dön", { clicks++ }, Modifier.testTag("secondary"), enabled = enabled.value, isLoading = busy.value)
        }
        for (tag in listOf("primary", "secondary")) {
            compose.onNodeWithTag(tag).assertIsNotEnabled().performTouchInput { click() }
        }
        compose.runOnIdle { assertEquals(0, clicks); enabled.value = true; busy.value = true }
        for (tag in listOf("primary", "secondary")) {
            compose.onNodeWithTag(tag).assertIsNotEnabled()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "İşlem sürüyor"))
                .performTouchInput { click() }
        }
        compose.runOnIdle { assertEquals(0, clicks); busy.value = false }
        compose.onNodeWithTag("primary").assertIsEnabled().performClick()
        compose.onNodeWithTag("secondary").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(2, clicks) }
    }
    @Test fun compactContentWidthAndSmallRequestedHeightRemainUsable() {
        show(scale = 1f) {
            HavamaniaPrimaryButton("Tamam", {}, Modifier.testTag("primary"), height = 20.dp, fillMaxWidth = false)
            HavamaniaSecondaryButton("Geri", {}, Modifier.testTag("secondary"), height = 20.dp, fillMaxWidth = false)
        }
        for (tag in listOf("primary", "secondary")) {
            compose.onNodeWithTag(tag).assertHeightIsAtLeast(48.dp).assertIsDisplayed()
            assertTrue(compose.onNodeWithTag(tag).getUnclippedBoundsInRoot().width < 240.dp)
        }
    }
}