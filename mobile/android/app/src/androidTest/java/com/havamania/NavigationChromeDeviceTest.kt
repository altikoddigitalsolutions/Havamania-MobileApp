package com.havamania

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NavigationChromeDeviceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun lightLargeBottomLabelsAndSelectionRemainUsable() = bottom(AppTheme.LIGHT, 2f)
    @Test fun darkBottomRejectsDuplicateRootNavigation() = bottom(AppTheme.DARK, 1f)
    private fun bottom(theme: AppTheme, scale: Float) {
        var route by mutableStateOf("sub_ai_history_detail/item")
        val calls = mutableListOf<String>()
        compose.setContent { Fixture(theme, scale) {
            Box(Modifier.width(320.dp)) { WeatherBottomBar(route, { route = it; calls.add(it) }) }
        } }
        compose.onNode(isSelectable() and hasText("Asistan")).assertIsSelected().performClick().performClick()
        compose.runOnIdle { assertEquals(listOf(Routes.AI_ROOT), calls) }
        compose.onNode(isSelectable() and hasText("Takvim")).performClick().assertIsSelected()
        compose.onNode(isSelectable() and hasText("Asistan")).assertIsNotSelected()
        saveImage("navigation-bottom-${theme.name.lowercase()}-20261125")
        listOf("Hava", "Takvim", "Asistan", "Profil").forEach { label ->
            compose.onNodeWithText(label, useUnmergedTree = true).assertIsDisplayed()
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { action ->
                    val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>(); action(layouts)
                    val layout = layouts.single()
                    assertEquals(label, layout.layoutInput.text.text)
                    assertEquals("Menu label must stay whole: $label", 1, layout.lineCount)
                    assertFalse("$label size=${layout.size} width=${layout.didOverflowWidth} height=${layout.didOverflowHeight}", layout.hasVisualOverflow)
                }
            compose.onNode(isSelectable() and hasText(label)).assertHeightIsAtLeast(48.dp)
        }
        saveImage("navigation-bottom-${theme.name.lowercase()}-20261125")
    }
    @Test fun longTopTitleAndActionsRemainReachableAtLargeFont() {
        var backs = 0; var infos = 0
        val title = "Bildirim tercihleri ve akıllı uyarılar"
        compose.setContent { Fixture(AppTheme.LIGHT, 2f) {
            Box(Modifier.width(320.dp)) {
                HavamaniaTopBar(title, { backs++ }) {
                    IconButton({ infos++ }) { Icon(Icons.Rounded.Info, "Bilgi") }
                }
            }
        } }
        compose.onNodeWithText(title, useUnmergedTree = true).assertIsDisplayed()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { action ->
                val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>(); action(layouts)
                assertFalse(layouts.single().hasVisualOverflow)
            }
        val heading = compose.onNodeWithText(title).getUnclippedBoundsInRoot()
        val back = compose.onNodeWithContentDescription("Geri").getUnclippedBoundsInRoot()
        assertTrue("Large heading must use the full row below controls", heading.top >= back.bottom)
        saveImage("navigation-top-large-20261125")
        compose.onNodeWithContentDescription("Geri").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithContentDescription("Bilgi").performClick()
        compose.runOnIdle { assertEquals(1, backs); assertEquals(1, infos) }
        saveImage("navigation-top-large-20261125")
    }
    @Test fun dismissErrorHasFullTouchTarget() {
        var closed = 0
        compose.setContent { Fixture(AppTheme.DARK, 2f) {
            Box(Modifier.width(320.dp)) { HavamaniaErrorCard("Bağlantı kurulamadı. Yeniden deneyin.", onDismiss = { closed++ }) }
        } }
        compose.onNodeWithContentDescription("Kapat").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(1, closed) }
    }
    @Composable private fun Fixture(theme: AppTheme, scale: Float, content: @Composable () -> Unit) {
        val density = LocalDensity.current.density
        val colors = ThemeFactory.createColors(theme)
        CompositionLocalProvider(LocalDensity provides Density(density, scale), LocalHavamaniaColors provides colors,
            LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
            ProvideResponsiveLayout { MaterialTheme { Box(Modifier.background(colors.background)) { content() } } }
        }
    }
    private fun saveImage(name: String) {
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
