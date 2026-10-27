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

    @Test fun travelFormSaveRemainsReachableWithLargeTextAfterTyping() {
        var saved: TravelPlan? = null
        val date = java.time.LocalDate.now().plusDays(2)
        val plan = TravelPlan(city = "Ankara", startDate = date, endDate = date, latitude = 39.9, longitude = 32.8)
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                TestTheme {
                    TravelPlanForm(plan, emptyList(), emptyList(), LocationSearchStatus.IDLE,
                        LocationSearchStatus.IDLE, false, {}, {}, {}, { saved = it })
                }
            }
        }
        compose.onNode(hasSetTextAction()).performScrollTo().performClick().performTextInput("test")
        compose.onNodeWithText("DEĞİŞİKLİKLERİ KAYDET").performScrollTo().assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performScrollTo().performTextClearance()
        compose.onNodeWithText("DEĞİŞİKLİKLERİ KAYDET").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("Ankara", saved?.city); assertEquals(null, saved?.originLatitude) }
    }

    @Test fun clearingExistingOriginAlsoClearsCoordinates() {
        var saved: TravelPlan? = null
        val date = java.time.LocalDate.now().plusDays(2)
        val plan = TravelPlan(city = "Ankara", startDate = date, endDate = date,
            latitude = 39.9, longitude = 32.8, originCity = "İstanbul", originLatitude = 41.0, originLongitude = 29.0)
        compose.setContent {
            TestTheme { TravelPlanForm(plan, emptyList(), emptyList(), LocationSearchStatus.IDLE,
                LocationSearchStatus.IDLE, false, {}, {}, {}, { saved = it }) }
        }
        compose.onAllNodesWithContentDescription("Konumu değiştir")[1].performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNodeWithText("DEĞİŞİKLİKLERİ KAYDET").performScrollTo().performClick()
        compose.runOnIdle {
            org.junit.Assert.assertNotNull(saved)
            assertEquals(null, saved?.originCity)
            assertEquals(null, saved?.originLatitude)
            assertEquals(null, saved?.originLongitude)
        }
    }

    @Test fun unlockingDestinationRequiresNewSelection() {
        val date = java.time.LocalDate.now().plusDays(2)
        val plan = TravelPlan(city = "Ankara", startDate = date, endDate = date, latitude = 39.9, longitude = 32.8)
        compose.setContent {
            TestTheme {
                TravelPlanForm(plan, emptyList(), emptyList(), LocationSearchStatus.IDLE,
                    LocationSearchStatus.IDLE, false, {}, {}, {}, {})
            }
        }
        compose.onNodeWithContentDescription("Konumu değiştir").performClick()
        compose.onNodeWithText("DEĞİŞİKLİKLERİ KAYDET").performScrollTo().assertIsNotEnabled()
    }

    @Test fun travelFormRestoresSelectedDestinationAndCoordinates() {
        val restoration = StateRestorationTester(compose)
        var saved: TravelPlan? = null
        val suggestion = GeocodingResultDto(id = 123, name = "Ankara", latitude = 39.9, longitude = 32.8, country = "Türkiye")
        restoration.setContent {
            TestTheme {
                TravelPlanForm(null, listOf(suggestion), emptyList(), LocationSearchStatus.RESULTS,
                    LocationSearchStatus.IDLE, false, {}, {}, {}, { saved = it })
            }
        }
        compose.onNodeWithText("Ankara").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Ankara").assertIsDisplayed()
        compose.onNodeWithText("SEYAHATİ OLUŞTUR").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle {
            org.junit.Assert.assertNotNull(saved)
            assertEquals("Ankara", saved?.city)
            assertEquals(39.9, saved!!.latitude, 0.00001)
            assertEquals(32.8, saved!!.longitude, 0.00001)
        }
    }

    @Test fun narrowLargeFontTravelDatesStackVertically() {
        val date = java.time.LocalDate.of(2026, 10, 30)
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                TestTheme {
                    Box(Modifier.width(280.dp)) {
                        TravelDateFields(date, date.plusDays(1), {}, {},
                            java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.forLanguageTag("tr-TR")))
                    }
                }
            }
        }
        val start = compose.onNodeWithText("30 Ekim 2026").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val end = compose.onNodeWithText("31 Ekim 2026").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(end.top >= start.bottom)
    }

    @Test fun restoredEditorKeepsOpenTripAndDraftButAccountChangeClosesIt() {
        val restoration = StateRestorationTester(compose)
        var owner by mutableStateOf("A:1")
        var editorState: MutableState<TravelEditorContext>? = null
        val date = java.time.LocalDate.now().plusDays(2)
        val plan = TravelPlan(id = "trip-123", city = "Ankara", startDate = date, endDate = date,
            latitude = 39.9, longitude = 32.8)
        restoration.setContent {
            val state = rememberTravelEditor(owner)
            SideEffect { editorState = state }
            if (state.value.open) {
                TestTheme {
                    TravelPlanForm(plan, emptyList(), emptyList(), LocationSearchStatus.IDLE,
                        LocationSearchStatus.IDLE, false, {}, {}, {}, {})
                }
            }
        }
        compose.runOnIdle { editorState!!.value = editorState!!.value.edit(plan.id) }
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("Korunan kalkış taslağı")
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            org.junit.Assert.assertTrue(editorState!!.value.open)
            assertEquals("trip-123", editorState!!.value.editId)
        }
        compose.onNode(hasSetTextAction()).performScrollTo().assertTextContains("Korunan kalkış taslağı")
        compose.runOnIdle { owner = "B:2" }
        compose.onNodeWithText("DEĞİŞİKLİKLERİ KAYDET").assertDoesNotExist()
        compose.runOnIdle { org.junit.Assert.assertFalse(editorState!!.value.open) }
    }

    @Test fun prefilledEditorRestoresStableIdentityAndConsumedLaunch() {
        val restoration = StateRestorationTester(compose)
        var editorState: MutableState<TravelEditorContext>? = null
        restoration.setContent {
            val state = rememberTravelEditor("A:1")
            SideEffect { editorState = state }
        }
        compose.runOnIdle {
            editorState!!.value = TravelEditorContext("A:1", open = true, initialHandled = true,
                prefillCity = "Ankara", prefillType = "VACATION", prefillDate = "2026-11-01", prefillId = "stable-id")
        }
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            val restored = editorState!!.value
            assertEquals("stable-id", restored.prefill()!!.id)
            assertEquals("Ankara", restored.prefill()!!.city)
            org.junit.Assert.assertTrue(restored.initialHandled)
            org.junit.Assert.assertFalse(restored.close().open)
            org.junit.Assert.assertTrue(restored.close().initialHandled)
        }
    }

    @Test fun prefilledCityNeedsSelectionAndSavesChosenCoordinates() {
        var saved: TravelPlan? = null
        val date = java.time.LocalDate.now().plusDays(2)
        val prefill = TravelPlan(id = "prefill-id", city = "Ankara", startDate = date, endDate = date)
        val suggestion = GeocodingResultDto(id = 123, name = "Ankara", latitude = 39.9, longitude = 32.8, country = "Türkiye")
        compose.setContent {
            TestTheme { TravelPlanForm(prefill, listOf(suggestion), emptyList(), LocationSearchStatus.RESULTS,
                LocationSearchStatus.IDLE, false, {}, {}, {}, { saved = it }, requireDestinationSelection = true) }
        }
        compose.onNodeWithText("SEYAHATİ OLUŞTUR").performScrollTo().assertIsNotEnabled()
        compose.onNode(hasSetTextAction() and hasText("Ankara")).performScrollTo()
        // The text field and suggestion both contain the name; click the selectable row.
        compose.onNode(hasText("Ankara") and hasClickAction() and !hasSetTextAction()).performScrollTo().performClick()
        compose.onNodeWithText("SEYAHATİ OLUŞTUR").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle {
            org.junit.Assert.assertNotNull(saved)
            assertEquals("prefill-id", saved!!.id)
            assertEquals(39.9, saved!!.latitude, 0.00001)
            assertEquals(32.8, saved!!.longitude, 0.00001)
        }
    }

    @Test fun editorWaitsForOwnedTripAndNeverFallsBackToCreation() {
        val date = java.time.LocalDate.now().plusDays(2)
        val owned = TravelPlan(id = "trip-id", userId = "A", city = "Ankara", startDate = date, endDate = date)
        var plans by mutableStateOf(emptyList<TravelPlan>())
        var loading by mutableStateOf(true)
        var retries = 0
        compose.setContent {
            TestTheme {
                TravelEditorHost(TravelEditorContext("A", open = true, editId = "trip-id"), plans, "A", loading,
                    { retries++ }, {}) { plan, _ -> androidx.compose.material3.Text("Düzenlenen: ${plan?.city}") }
            }
        }
        compose.onNodeWithText("Seyahat yükleniyor…").assertIsDisplayed()
        compose.onNodeWithText("Yeniden dene").assertIsNotEnabled()
        compose.runOnIdle { loading = false; plans = listOf(owned.copy(userId = "B", city = "Başka hesap")) }
        compose.onNodeWithText("Seyahat bulunamadı veya henüz yüklenemedi.").assertIsDisplayed()
        compose.onNodeWithText("Düzenlenen: Başka hesap").assertDoesNotExist()
        compose.onNodeWithText("Yeniden dene").performClick()
        compose.runOnIdle { assertEquals(1, retries); plans = plans + owned }
        compose.onNodeWithText("Düzenlenen: Ankara").assertIsDisplayed()
        compose.runOnIdle { plans = emptyList() }
        compose.onNodeWithText("Düzenlenen: null").assertDoesNotExist()
        compose.onNodeWithText("Seyahat bulunamadı veya henüz yüklenemedi.").assertIsDisplayed()
    }

    @Test fun editorIgnoresContextFromDifferentOwner() {
        compose.setContent {
            TestTheme {
                TravelEditorHost(TravelEditorContext("A", open = true), emptyList(), "B", false, {}, {}) { _, _ ->
                    androidx.compose.material3.Text("Eski form")
                }
            }
        }
        compose.onNodeWithText("Eski form").assertDoesNotExist()
    }

    @Test fun localTripRetryRemainsAccessibleAndTracksLoadingAcrossThemes() {
        var error by mutableStateOf<String?>("Seyahatler cihazdan yüklenemedi. Yeniden deneyin.")
        var loading by mutableStateOf(false)
        var theme by mutableStateOf(AppTheme.DARK)
        var retries = 0
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                TestTheme(theme) {
                    androidx.compose.foundation.layout.Column(
                        Modifier.width(280.dp).height(260.dp).verticalScroll(rememberScrollState())
                    ) {
                        TripLoadStatusPanel(error, loading) { retries++; error = null; loading = true }
                    }
                }
            }
        }
        compose.onNodeWithText("Seyahatleri yeniden yükle").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Yükleniyor…").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(1, retries)
            theme = AppTheme.LIGHT
            loading = false
            error = "Seyahatler cihazdan yüklenemedi. Yeniden deneyin."
        }
        compose.onNodeWithText("Seyahatleri yeniden yükle").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(2, retries); loading = false; error = null }
        compose.onNodeWithText("Seyahatleri yeniden yükle").assertDoesNotExist()
        compose.onNodeWithText("Seyahatler yükleniyor…").assertDoesNotExist()
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
private fun TestTheme(theme: AppTheme = AppTheme.DARK, content: @Composable () -> Unit) {
    val colors = ThemeFactory.createColors(theme)
    CompositionLocalProvider(
        LocalHavamaniaColors provides colors,
        LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)
    ) { ProvideResponsiveLayout { MaterialTheme(content = content) } }
}
