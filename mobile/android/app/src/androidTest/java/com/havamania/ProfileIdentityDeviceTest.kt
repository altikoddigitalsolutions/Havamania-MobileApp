package com.havamania

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
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ProfileIdentityDeviceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun lightLargeHeaderKeepsIdentityAndStatisticsReachable() = header(AppTheme.LIGHT, 2f)
    @Test fun darkHeaderKeepsIdentityAndStatisticsReachable() = header(AppTheme.DARK, 1f)
    private fun header(theme: AppTheme, scale: Float) {
        val name = "ilker ışık uzun profil adı"
        val clicks = mutableListOf<String>()
        compose.setContent { Fixture(theme, scale) {
            Column(Modifier.width(320.dp).height(650.dp).verticalScroll(rememberScrollState())) {
                PremiumProfileHeader(name, "Hava ve seyahat notları", null, interests = emptySet(), aboutMe = "",
                    stats = mapOf("İlgi" to "12", "Rota" to "Bekleniyor", "Analiz" to "128"),
                    onAvatarClick = { error("Photo upload must remain disabled") }, onStatClick = { clicks.add(it) })
            }
        } }
        compose.onNodeWithText("İI").assertExists()
        compose.onNodeWithText(name).performScrollTo().assertIsDisplayed()
        image("profile-header-${theme.name.lowercase()}-20261125")
        listOf("Tercihler", "Rotalar", "Kaydedilen analiz").forEach {
            compose.onNode(hasClickAction() and hasText(it)).performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        }
        compose.runOnIdle { assertEquals(listOf("İlgi", "Rota", "Analiz"), clicks) }
    }
    @Test fun allQuickActionsRemainReachableWithLargeText() {
        val calls = mutableListOf<String>()
        compose.setContent { Fixture(AppTheme.LIGHT, 2f) {
            Column(Modifier.width(320.dp).height(650.dp).verticalScroll(rememberScrollState())) {
                QuickActionsGrid({ calls.add("city") }, { calls.add("ai") }, { calls.add("trip") },
                    { calls.add("preferences") }, { calls.add("edit") }, { calls.add("premium") })
            }
        } }
        listOf("Şehirler", "Asistan geçmişi", "Seyahatlerim", "Tercihlerim", "Premium", "Profili düzenle").forEach {
            compose.onNode(hasClickAction() and hasText(it)).performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        }
        compose.runOnIdle { assertEquals(listOf("city", "ai", "trip", "preferences", "premium", "edit"), calls) }
        image("profile-quick-actions-large-20261125")
    }
    @Test fun emptyAndFilledAboutCardsRemainEditable() {
        var empty = 0; var filled = 0
        compose.setContent { Fixture(AppTheme.LIGHT, 2f) {
            Column(Modifier.width(320.dp).height(650.dp).verticalScroll(rememberScrollState())) {
                PremiumAboutMeCard("") { empty++ }
                PremiumAboutMeCard("Kamp ve doğa yürüyüşü yapıyorum. Seyahat planlamayı seviyorum.") { filled++ }
            }
        } }
        compose.onAllNodes(hasClickAction() and hasText("Kendinden bahset"))[0].performScrollTo().performClick()
        compose.onAllNodes(hasClickAction() and hasText("Kendinden bahset"))[1].performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, empty); assertEquals(1, filled) }
        image("profile-about-cards-large-20261125")
    }
    @Test fun savingAboutMeDisablesEditorAndKeepsDraft() {
        var saving by mutableStateOf(false); var saved = ""
        compose.setContent { Fixture(AppTheme.LIGHT, 2f) {
            Box(Modifier.width(320.dp).height(650.dp)) {
                PremiumAboutMeContent("Mevcut taslak", isSaving = saving) { saved = it; saving = true }
            }
        } }
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("Korunan yeni taslak")
        compose.onNodeWithText("PROFİLİ GÜNCELLE").performScrollTo().performClick()
        compose.onNodeWithText("Korunan yeni taslak").performScrollTo().assertIsNotEnabled().assertTextContains("Korunan yeni taslak")
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onAllNodes(hasClickAction() and hasText("Outdoor")).assertCountEquals(0)
        compose.runOnIdle { assertEquals("Korunan yeni taslak", saved) }
        image("profile-about-saving-large-20261125")
    }
    @Test fun longSavedAnalysisTitlesAndLastRecordStayReachable() {
        val history = (0..19).map { AiHistorySummary("qa-$it", "Uzun kayıtlı hava analizi başlığı $it",
            "Analiz özeti $it", "Bursa", it.toLong(), false) }
        compose.setContent { Fixture(AppTheme.DARK, 2f) {
            Box(Modifier.width(320.dp)) { StatsDetailContent("Analiz", emptySet(), emptyList(), history) }
        } }
        compose.onNodeWithText(history.first().title).assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(19)
        compose.onNodeWithText(history.last().title).assertIsDisplayed()
        compose.onNodeWithText("Analiz özeti 19").assertIsDisplayed()
    }
    @Composable private fun Fixture(theme: AppTheme, scale: Float, content: @Composable () -> Unit) {
        val density = LocalDensity.current.density; val colors = ThemeFactory.createColors(theme)
        CompositionLocalProvider(LocalDensity provides Density(density, scale), LocalHavamaniaColors provides colors,
            LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
            ProvideResponsiveLayout { MaterialTheme { Box(Modifier.background(colors.background)) { content() } } }
        }
    }
    private fun image(name: String) {
        compose.mainClock.advanceTimeBy(2000)
        // Android RippleDrawable runs on real time rather than the Compose test clock.
        Thread.sleep(500)
        compose.waitForIdle()
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
