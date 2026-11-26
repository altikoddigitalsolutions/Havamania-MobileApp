package com.havamania

import android.app.Application
import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import com.havamania.ui.theme.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** Actual calendar, editor, ViewModel and Room; public guide refresh and geocoding are isolated. */
class TravelAutoGuideDeviceTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: WeatherDatabase
    private lateinit var model: TravelViewModel
    private val store = ViewModelStore()
    private var routeRequests = 0
    private val date = LocalDate.now().plusDays(20)
    private val destination = GeocodingResultDto(id = 34, name = "İstanbul", country = "Türkiye",
        latitude = 41.01, longitude = 28.97)
    private val original = TravelPlan(id = "focus-trip", userId = "legacy", city = "Ankara",
        latitude = 39.9, longitude = 32.8, startDate = date.plusDays(3), endDate = date.plusDays(3))

    @Before fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        assertEquals("QA must not target the Play installation", "com.havamania", app.packageName)
        assertNull("QA must not use an authenticated account", FirebaseAuth.getInstance().currentUser)
        database = Room.inMemoryDatabaseBuilder(app, WeatherDatabase::class.java).build()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            model = TravelViewModel(app, database, { listOf(destination) }, object : NetworkMonitor {
                override val isOnline = kotlinx.coroutines.flow.flowOf(false)
            })
            store.put("travel", model)
        }
        compose.waitUntil(10_000) { model.sessionVersion.value > 0 && !model.isLoading.value }
    }

    @After fun cleanup() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
        if (::database.isInitialized) database.close()
    }

    private fun seed(plan: TravelPlan) { runBlocking { database.weatherDao().saveLocalTrip(plan.toEntity()) } }

    private fun show(phone: Boolean, create: Boolean = true, restoration: StateRestorationTester? = null) {
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalTravelGuideRefresher provides { _, _, _ -> }) {
                ProvideResponsiveLayout {
                    val size = LocalWindowSize.current
                    CompositionLocalProvider(LocalWindowSize provides if (phone)
                        HavamaniaWindowSize(390, size.heightDp, false, true, false, false) else size) {
                        MaterialTheme {
                            Box(Modifier.width(if (phone) 390.dp else 800.dp).fillMaxHeight()) {
                                TravelPlannerScreen(model, initialCity = if (create) "İstanbul" else null,
                                    initialStartDate = date.toString(), focusId = original.id,
                                    onViewRoute = { routeRequests++ }, onBack = {})
                            }
                        }
                    }
                }
            }
        }
        if (restoration == null) compose.setContent(content) else restoration.setContent(content)
    }

    private fun submitNewTrip() {
        compose.waitUntil(10_000) { model.citySuggestions.value.isNotEmpty() }
        compose.onNodeWithTag("location_suggestion_34").performClick()
        compose.onNodeWithText("SEYAHATİ OLUŞTUR").performScrollTo().assertIsEnabled().performClick()
    }

    private fun verifyCreation(phone: Boolean, oldTripAlreadyLoaded: Boolean = true, archived: Boolean = false) {
        if (oldTripAlreadyLoaded) seed(original.copy(isArchived = archived))
        show(phone)
        submitNewTrip()
        compose.waitUntil(10_000) { model.plans.value.any { it.city == "İstanbul" } }
        val description = CityPersonalityProvider.getPersonality("İstanbul").description
        compose.waitUntil(10_000) { compose.onAllNodesWithText("HAVA ÖZETİ").fetchSemanticsNodes().isNotEmpty() }
        val guideText = compose.onNode(hasText(description) and hasAnyAncestor(hasTestTag("travel_guide")), useUnmergedTree = true)
        try { guideText.assertIsDisplayed() } catch (error: AssertionError) {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(
                InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                    "screencap -p /data/local/tmp/havamania-guide-visibility.png")
            ).use { it.readBytes() }
            throw AssertionError("Guide text bounds: ${guideText.fetchSemanticsNode().boundsInRoot}", error)
        }
        guideText.performTouchInput { click() }
        compose.runOnIdle { assertEquals("Reading the guide must not navigate to the route", 0, routeRequests) }
        runBlocking {
            val saved = database.weatherDao().getAllTravelPlans("legacy")
            assertEquals(if (oldTripAlreadyLoaded) 2 else 1, saved.size)
        }
        // A later Room emission must not reapply the old notification focus/date.
        seed(original.copy(updatedAt = System.currentTimeMillis()))
        compose.runOnIdle { model.loadPlans() }
        compose.waitUntil(10_000) { !model.isLoading.value }
        compose.onNode(hasText(description) and hasAnyAncestor(hasTestTag("travel_guide")), useUnmergedTree = true).assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("HAVA ÖZETİ").assertDoesNotExist()
        compose.onNodeWithText("Seyahat Planlayıcı").assertIsDisplayed()
    }

    @Test fun existingPhoneGuideOpensThenStaysClosedAfterReload() = verifyExistingGuide(true, false)
    @Test fun existingTabletGuideDoesNotOpenAnotherTripAfterCloseAndReload() = verifyExistingGuide(false, true)

    @Test fun phoneDateAndFilterRoundTripsRespectClosedGuide() = verifyDateAndFilters(true)
    @Test fun tabletDateAndFilterRoundTripsRespectClosedGuide() = verifyDateAndFilters(false)

    private fun verifyDateAndFilters(phone: Boolean) {
        seed(original)
        seed(original.copy(id = "past-local-trip", city = "Bursa",
            startDate = LocalDate.now().minusDays(4), endDate = LocalDate.now().minusDays(3)))
        seed(original.copy(id = "archived-local-trip", city = "İstanbul", isArchived = true))
        show(phone, create = false)
        compose.waitUntil(10_000) { compose.onAllNodesWithText("HAVA ÖZETİ").fetchSemanticsNodes().size == 1 }
        compose.onNodeWithText("Rehberi kapat").performScrollTo().performClick()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.forLanguageTag("tr-TR"))
        compose.onNodeWithContentDescription(original.startDate.plusDays(1).format(formatter))
            .performScrollTo().performClick()
        compose.onNodeWithText("HAVA ÖZETİ").assertDoesNotExist()
        compose.onNodeWithText("Bu tarih için planlanmış seyahat yok.").assertExists()
        compose.onNodeWithContentDescription(original.startDate.format(formatter)).performScrollTo().performClick()
        compose.onNodeWithText("Rehberi aç").assertExists()
        compose.onNodeWithText("HAVA ÖZETİ").assertDoesNotExist()
        for (filter in listOf("Geçmiş", "Arşiv", "Yaklaşanlar")) {
            compose.onNodeWithText(filter).performScrollTo().performClick()
            compose.onNodeWithText("HAVA ÖZETİ").assertDoesNotExist()
            compose.onNodeWithText("Rehberi aç").assertExists()
        }
        if (!phone) {
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(
                "mkdir -p /sdcard/Pictures/Havamania-QA/filters-20261127")).use { it.readBytes() }
            android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(
                "screencap -p /sdcard/Pictures/Havamania-QA/filters-20261127/tablet.png")).use { it.readBytes() }
        }
        compose.onNodeWithText("Rehberi aç").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("HAVA ÖZETİ").fetchSemanticsNodes().size == 1 }
    }

    @Test fun restoredPhoneScreenKeepsClosedGuide() = verifyRestoredGuide(true)
    @Test fun restoredTabletScreenKeepsClosedGuide() = verifyRestoredGuide(false)

    private fun verifyRestoredGuide(phone: Boolean) {
        seed(original)
        val restoration = StateRestorationTester(compose)
        show(phone, create = false, restoration = restoration)
        compose.waitUntil(10_000) { compose.onAllNodesWithText("HAVA ÖZETİ").fetchSemanticsNodes().size == 1 }
        compose.onNodeWithText("Rehberi kapat").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("HAVA ÖZETİ").assertDoesNotExist()
        compose.onNodeWithText("Rehberi aç").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("HAVA ÖZETİ").fetchSemanticsNodes().size == 1 }
        restoration.emulateSavedInstanceStateRestore()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("HAVA ÖZETİ").fetchSemanticsNodes().size == 1 }
    }

    private fun verifyExistingGuide(phone: Boolean, multiple: Boolean) {
        seed(original)
        if (multiple) seed(original.copy(id = "second-local-trip", city = "İstanbul"))
        show(phone, create = false)
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("HAVA ÖZETİ").fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithText("Rehberi kapat").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("HAVA ÖZETİ").assertDoesNotExist()
        seed(original.copy(updatedAt = System.currentTimeMillis()))
        compose.runOnIdle { model.loadPlans() }
        compose.waitUntil(10_000) { !model.isLoading.value && model.plans.value.size == if (multiple) 2 else 1 }
        compose.onNodeWithText("HAVA ÖZETİ").assertDoesNotExist()
        val openLabel = "Rehberi aç"
        compose.onAllNodesWithText(openLabel).onFirst().performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("HAVA ÖZETİ").fetchSemanticsNodes().size == 1 }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("HAVA ÖZETİ").assertDoesNotExist()
        compose.onNodeWithText("Seyahat Planlayıcı").assertIsDisplayed()
    }

    @Test fun newTripAutomaticallyShowsGuideOnPhoneAfterOldNotificationFocus() = verifyCreation(true)
    @Test fun newTripAutomaticallyShowsGuideOnTabletAfterOldNotificationFocus() = verifyCreation(false)
    @Test fun lateNotificationTargetCannotHideTheNewTripGuide() = verifyCreation(false, false)
    @Test fun savingFromArchivedFilterOpensTheNewUpcomingGuide() = verifyCreation(false, archived = true)

    @Test fun editingExistingTripAutomaticallyShowsGuide() {
        seed(original)
        show(false, create = false)
        compose.waitUntil(10_000) { model.plans.value.isNotEmpty() && !model.isLoading.value }
        compose.onNodeWithContentDescription("Seyahat işlemleri: Ankara").performScrollTo().performClick()
        compose.onNodeWithText("Düzenle").performClick()
        compose.onNodeWithText("DEĞİŞİKLİKLERİ KAYDET").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("HAVA ÖZETİ").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText(CityPersonalityProvider.getPersonality("Ankara").description) and
            hasAnyAncestor(hasTestTag("travel_guide"))).assertIsDisplayed()
        runBlocking { assertEquals(1, database.weatherDao().getAllTravelPlans("legacy").size) }
    }

    @Test fun duplicateTripDoesNotOpenGuide() {
        seed(original.copy(city = "İstanbul", startDate = date, endDate = date,
            latitude = destination.latitude, longitude = destination.longitude))
        show(false)
        submitNewTrip()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Bu şehir ve tarihler için zaten bir seyahatin bulunuyor.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Bu şehir ve tarihler için zaten bir seyahatin bulunuyor.").assertIsDisplayed()
        compose.onNodeWithText("HAVA ÖZETİ").assertDoesNotExist()
        runBlocking { assertEquals(1, database.weatherDao().getAllTravelPlans("legacy").size) }
    }

    @Test fun invalidOrForeignOwnedTripCannotTriggerSuccessOrPersist() = runBlocking {
        var completions = 0
        val plan = original.copy(city = "İstanbul", startDate = date, endDate = date)
        model.savePlan(plan.copy(userId = "another-owner")) { completions++ }?.join()
        model.savePlan(plan.copy(latitude = 0.0, longitude = 0.0)) { completions++ }?.join()
        model.savePlan(plan.copy(startDate = LocalDate.now().minusDays(2), endDate = LocalDate.now().minusDays(1))) { completions++ }?.join()
        database.weatherDao().enqueueTripDeletion(TripDeletionRequest("legacy", plan.id))
        model.savePlan(plan) { completions++ }?.join()
        val collision = plan.copy(id = "foreign-record", userId = "another-owner")
        seed(collision)
        model.savePlan(collision.copy(userId = "legacy")) { completions++ }?.join()
        assertEquals(0, completions)
        assertTrue(database.weatherDao().getAllTravelPlans("legacy").isEmpty())
        assertEquals("another-owner", database.weatherDao().getTravelPlanByIdRaw(collision.id)!!.userId)
    }
}
