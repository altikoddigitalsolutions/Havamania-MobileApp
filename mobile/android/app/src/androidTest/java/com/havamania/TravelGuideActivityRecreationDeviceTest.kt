package com.havamania

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TravelGuideActivityRecreationDeviceTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun phoneActualRecreationKeepsGuideDateAndFilter() = verify(true)
    @Test fun tabletActualRecreationKeepsGuideDateAndFilter() = verify(false)
    private fun verify(phone: Boolean) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        assertEquals("com.havamania", app.packageName)
        assertNull(FirebaseAuth.getInstance().currentUser)
        val database = Room.inMemoryDatabaseBuilder(app, WeatherDatabase::class.java).build()
        val store = ViewModelStore()
        var scenario: ActivityScenario<TravelGuideRecreationTestActivity>? = null
        val date = LocalDate.now().plusDays(20)
        val plan = TravelPlan(id = "recreate-trip", userId = "legacy", city = "Ankara",
            latitude = 39.9, longitude = 32.8, startDate = date, endDate = date)
        try {
            runBlocking {
                database.weatherDao().saveLocalTrip(plan.toEntity())
                database.weatherDao().saveLocalTrip(plan.copy(id = "past-recreate", city = "Bursa",
                    startDate = LocalDate.now().minusDays(5), endDate = LocalDate.now().minusDays(4)).toEntity())
            }
            lateinit var model: TravelViewModel
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                model = TravelViewModel(app, database, { emptyList() }, object : NetworkMonitor {
                    override val isOnline = kotlinx.coroutines.flow.flowOf(false)
                })
                store.put("travel", model)
            }
            TravelGuideRecreationFixture.inputs = TravelGuideRecreationFixture.Inputs(model, plan.id, phone)
            val opened = ActivityScenario.launch(TravelGuideRecreationTestActivity::class.java)
            scenario = opened
            compose.waitUntil(10_000) { compose.onAllNodesWithText("HAVA ÖZETİ").fetchSemanticsNodes().size == 1 }
            compose.onNodeWithText("Rehberi kapat").performScrollTo().performClick()
            opened.recreate()
            compose.onNodeWithText("Rehberi aç").assertExists()
            compose.onNodeWithText("HAVA ÖZETİ").assertDoesNotExist()
            compose.onNodeWithText("Rehberi aç").performScrollTo().performClick()
            opened.recreate()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("HAVA ÖZETİ").fetchSemanticsNodes().size == 1 }
            compose.onNodeWithText("Rehberi kapat").performScrollTo().performClick()
            val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("tr-TR"))
            val next = date.plusDays(1).format(formatter)
            compose.onNodeWithContentDescription(next).performScrollTo().performClick()
            opened.recreate()
            compose.onNodeWithContentDescription(next).assertIsSelected()
            compose.onNodeWithText("Bu tarih için planlanmış seyahat yok.").assertExists()
            compose.onNodeWithText("Geçmiş").performScrollTo().performClick()
            opened.recreate()
            compose.onNodeWithContentDescription("Seyahat işlemleri: Bursa").assertExists()
            compose.onNodeWithText("HAVA ÖZETİ").assertDoesNotExist()
            compose.onNodeWithText("Rehberi aç").performScrollTo().performClick()
            opened.recreate()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("HAVA ÖZETİ").fetchSemanticsNodes().size == 1 }
            compose.onNodeWithContentDescription("Seyahat işlemleri: Bursa").performScrollTo().performClick()
            compose.onNodeWithText("Sil").performClick()
            compose.onNodeWithText("Seyahati Sil?").assertIsDisplayed()
            compose.onNodeWithText("Sil").performClick()
            compose.waitUntil(10_000) { model.plans.value.none { it.id == "past-recreate" } }
            compose.onNodeWithText("HAVA ÖZETİ").assertDoesNotExist()
            compose.onNodeWithContentDescription("Seyahat işlemleri: Bursa").assertDoesNotExist()
            runBlocking { assertNull(database.weatherDao().getTravelPlanByIdRaw("past-recreate")) }
            opened.recreate()
            compose.onNodeWithText("HAVA ÖZETİ").assertDoesNotExist()
            compose.onNodeWithText("Geçmiş seyahat bulunmuyor").assertExists()
            runBlocking { assertNotNull(database.weatherDao().getTravelPlanByIdRaw(plan.id)) }
        } finally {
            scenario?.close()
            TravelGuideRecreationFixture.inputs = null
            InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
            database.close()
        }
    }
}
