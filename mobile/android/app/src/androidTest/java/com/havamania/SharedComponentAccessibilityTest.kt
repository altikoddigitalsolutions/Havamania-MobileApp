package com.havamania

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
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

    @Test fun tripSyncRetryFollowsConnectionAndRunningState() {
        var online by mutableStateOf(false)
        var running by mutableStateOf(false)
        var retries = 0
        compose.setContent {
            TestTheme { TripSyncStatusPanel(1, 1, running, online, null, null) { retries++ } }
        }
        compose.onNodeWithText("Bağlantı bekleniyor").assertIsNotEnabled()
        compose.runOnIdle { online = true }
        compose.onNodeWithText("Yeniden dene").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, retries); running = true }
        compose.onNodeWithText("Eşitleniyor…").assertIsNotEnabled()
    }

    @Test fun tripSyncErrorRemainsReachableWithLargeTextAndClearsOnSuccess() {
        var error by mutableStateOf<String?>("Seyahat değişiklikleri gönderilemedi.")
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                TestTheme {
                    androidx.compose.foundation.layout.Column(
                        Modifier.width(320.dp).height(420.dp)
                            .then(Modifier.verticalScroll(rememberScrollState()))
                    ) { TripSyncStatusPanel(0, 0, false, true, error, null) { error = null } }
                }
            }
        }
        compose.onNodeWithText("Seyahat değişiklikleri gönderilemedi.").assertIsDisplayed()
        compose.onNodeWithText("Yeniden dene").performScrollTo().performClick()
        compose.onNodeWithText("Yeniden dene").assertDoesNotExist()
        compose.onNodeWithText("Seyahat değişiklikleri gönderilemedi.").assertDoesNotExist()
    }

    @Test fun calendarFollowsDistantSelectionAndUpdatedToday() {
        var today by mutableStateOf(java.time.LocalDate.of(2026, 10, 23))
        var selection by mutableStateOf(today)
        compose.setContent {
            TestTheme {
                Box(Modifier.width(320.dp)) {
                    TravelCalendarStripe(selection, { selection = it }, emptySet(), today = today)
                }
            }
        }
        compose.onNodeWithContentDescription("23 Ekim 2026").assertIsDisplayed().assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Bugün"))
        compose.runOnIdle { selection = java.time.LocalDate.of(2030, 2, 12) }
        compose.onNodeWithContentDescription("12 Şubat 2030").assertIsDisplayed().assertIsSelected()
        compose.runOnIdle { today = java.time.LocalDate.of(2030, 2, 12) }
        compose.onNodeWithContentDescription("12 Şubat 2030")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Bugün"))
        compose.runOnIdle { selection = java.time.LocalDate.of(2020, 1, 2) }
        compose.onNodeWithContentDescription("2 Ocak 2020").assertIsDisplayed().assertIsSelected()
    }

    @Test fun calendarUsesTurkishUppercaseMonth() {
        compose.setContent {
            TestTheme {
                TravelCalendarStripe(java.time.LocalDate.of(2026, 10, 23), {}, emptySet())
            }
        }
        compose.onNodeWithText("EKİM 2026").assertIsDisplayed()
    }

    @Test fun searchFeedbackDistinguishesLoadingEmptyAndRetry() {
        var status by mutableStateOf(LocationSearchStatus.LOADING)
        var retries = 0
        compose.setContent { TestTheme { LocationSearchFeedback(status) { retries++ } } }
        compose.onNodeWithText("Konumlar aranıyor…").assertIsDisplayed()
        compose.onNodeWithText("Yeniden dene").assertDoesNotExist()
        compose.runOnIdle { status = LocationSearchStatus.EMPTY }
        compose.onNodeWithText("Konum bulunamadı. Farklı bir ad deneyin.").assertIsDisplayed()
        compose.runOnIdle { status = LocationSearchStatus.ERROR }
        compose.onNodeWithText("Yeniden dene").performClick()
        compose.runOnIdle { assertEquals(1, retries); status = LocationSearchStatus.RESULTS }
        compose.onNodeWithText("Yeniden dene").assertDoesNotExist()
    }

    @Test fun citySearchRetryIsReachableAtLargeFontInSmallViewport() {
        var retries = 0
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                TestTheme {
                    Box(Modifier.width(320.dp).height(300.dp)) {
                        CityManagementContent("Ankara", {}, emptyList(), emptyList(), null,
                            {}, {}, {}, searchStatus = LocationSearchStatus.ERROR, onRetry = { retries++ })
                    }
                }
            }
        }
        compose.onNodeWithText("Yeniden dene").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun weatherSearchRetryWithKeyboardAndLargeFont() {
        var retries = 0
        var rootView: android.view.View? = null
        var keyboard: androidx.compose.ui.platform.SoftwareKeyboardController? = null
        compose.setContent {
            val localDensity = LocalDensity.current
            val view = androidx.compose.ui.platform.LocalView.current
            val controller = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            SideEffect { rootView = view; keyboard = controller }
            val density = localDensity.density
            var query by remember { mutableStateOf("Ankara") }
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                TestTheme {
                    Box(Modifier.width(320.dp).height(420.dp)) {
                        WeatherCitySearchContent(query, { query = it }, emptyList(),
                            LocationSearchStatus.ERROR, { retries++ }, onSelect = {})
                    }
                }
            }
        }
        compose.onNode(hasSetTextAction()).performClick().performTextInput("a")
        compose.runOnIdle { keyboard?.show() }
        compose.waitUntil(5_000) {
            rootView?.rootWindowInsets?.isVisible(android.view.WindowInsets.Type.ime()) == true
        }
        compose.onNodeWithText("Yeniden dene").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

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
