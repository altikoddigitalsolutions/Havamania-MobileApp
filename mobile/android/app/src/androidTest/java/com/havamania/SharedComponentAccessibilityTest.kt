package com.havamania

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Isolated components; no production activity, ViewModel, login or remote writes. */
class SharedComponentAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sharedButtonPreservesTurkishSentenceCase() {
        compose.setContent { TestTheme { HavamaniaPrimaryButton("İzinleri yönet", {}) } }
        compose.onNodeWithText("İzinleri yönet").assertIsDisplayed()
    }

    @Test fun citiesRemainReachableBelowSuggestionsAtLargeFont() {
        val saved = GeocodingResultDto(id = 99, name = "Uzun kayıtlı konum", country = "Türkiye")
        val suggestions = (1L..5L).map { GeocodingResultDto(id = it, name = "Uzun arama sonucu $it", country = "Türkiye") }
        var selected = false
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                TestTheme {
                    Box(Modifier.width(320.dp).height(420.dp)) {
                        CityManagementContent("Uzun", {}, suggestions, listOf(saved), null,
                            {}, {}, { selected = true })
                    }
                }
            }
        }
        compose.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Uzun kayıtlı konum varsayılan konum yap"))
        compose.onNodeWithContentDescription("Uzun kayıtlı konum varsayılan konum yap").assertIsDisplayed().performClick()
        compose.runOnIdle { org.junit.Assert.assertTrue(selected) }
    }

    @Test fun aboutMeDraftSurvivesSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            TestTheme { Box(Modifier.width(320.dp).height(420.dp)) { PremiumAboutMeContent("Original") {} } }
        }
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("Korunan taslak")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasSetTextAction()).performScrollTo().assertTextContains("Korunan taslak")
    }

    @Test fun aboutMeSaveRemainsReachableWithLargeTextAndKeepsDraftOnFailure() {
        var saved = ""
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                TestTheme {
                    Box(Modifier.width(320.dp).height(420.dp)) {
                        PremiumAboutMeContent("Taslak", errorMessage = "Kayıt onayı alınamadı") { saved = it }
                    }
                }
            }
        }
        compose.onNodeWithText("PROFİLİ GÜNCELLE").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("Taslak", saved) }
        compose.onNodeWithText("Kayıt onayı alınamadı").performScrollTo().assertIsDisplayed()
    }

    @Test fun authFieldsKeepLabelsAndPasswordToggleAnnouncesAction() {
        compose.setContent {
            TestTheme {
                androidx.compose.foundation.layout.Column {
                    HavamaniaAuthField("user@example.test", {}, "E-posta", Icons.Rounded.Email)
                    HavamaniaAuthPasswordField("secret", {}, "Şifre")
                }
            }
        }
        compose.onNodeWithText("E-posta", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Şifre", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Şifreyi göster").performClick()
        compose.onNodeWithContentDescription("Şifreyi gizle").assertIsDisplayed()
    }

    @Test fun sendingMessageHasAccessibleDisabledSendAction() {
        compose.setContent {
            TestTheme { ChatInput({}, true, HavamaniaTheme.colors, HavamaniaStyles()) }
        }
        compose.onNodeWithContentDescription("Mesaj gönder").assertIsNotEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Yanıt bekleniyor"))
    }

    @Test fun sunTimesStackOnNarrowScreenWithLargeText() {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                TestTheme { Box(Modifier.width(260.dp)) { SunTimesCard("06:15", "12:30", "18:45") } }
            }
        }
        val rise = compose.onNodeWithText("Gün Doğumu").assertIsDisplayed().getUnclippedBoundsInRoot()
        val middle = compose.onNodeWithText("Gündüz ortası").assertIsDisplayed().getUnclippedBoundsInRoot()
        val set = compose.onNodeWithText("Gün Batımı").assertIsDisplayed().getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue(rise.bottom < middle.top && middle.bottom < set.top)
    }

    @Test fun loadingButtonKeepsAccessibleNameAndRejectsClick() {
        var clicks = 0
        compose.setContent {
            TestTheme {
                HavamaniaPrimaryButton("Kaydet", { clicks++ }, isLoading = true)
            }
        }
        compose.onNodeWithContentDescription("Kaydet")
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "İşlem sürüyor"))
            .performClick()
        compose.runOnIdle { assertEquals(0, clicks) }
    }

    @Test fun filledTextFieldRetainsItsLabel() {
        compose.setContent {
            TestTheme { HavamaniaTextField("Ada", {}, placeholder = "Ad Soyad") }
        }
        compose.onNodeWithText("Ad Soyad", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Ada", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun largeFontButtonGrowsInsteadOfClippingToFixedHeight() {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                TestTheme {
                    HavamaniaPrimaryButton("DEĞİŞİKLİKLERİ KAYDET", {}, modifier = Modifier.width(180.dp))
                }
            }
        }
        compose.onNodeWithText("DEĞİŞİKLİKLERİ KAYDET").assertIsDisplayed()
            .assertHeightIsAtLeast(56.dp)
    }
}

@Composable
private fun TestTheme(content: @Composable () -> Unit) {
    val colors = ThemeFactory.createColors(AppTheme.DARK)
    CompositionLocalProvider(
        LocalHavamaniaColors provides colors,
        LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)
    ) { ProvideResponsiveLayout { MaterialTheme(content = content) } }
}
