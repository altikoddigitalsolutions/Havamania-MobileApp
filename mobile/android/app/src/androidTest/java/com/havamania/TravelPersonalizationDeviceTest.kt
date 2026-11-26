package com.havamania

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.platform.app.InstrumentationRegistry
import com.havamania.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.util.UUID

class TravelPersonalizationDeviceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun persistedOptoutAndAccountIsolationAreReadAsOneSnapshot() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "travel-personal-${UUID.randomUUID()}.preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            var store = PreferenceDataStoreFactory.create(scope = scope) { file }
            store.edit {
                it[stringPreferencesKey("havamania:A:user_interests")] = "kamp,bulut_fotografciligi"
                it[booleanPreferencesKey("havamania:A:personalization_enabled")] = true
                it[stringPreferencesKey("havamania:B:user_interests")] = "cocuklar_icin"
                it[booleanPreferencesKey("havamania:B:personalization_enabled")] = true
            }
            assertEquals(setOf("kamp", "bulut_fotografciligi"), ThemeManager.getPersonalizationSnapshot(store, "A").first().travelInterests())
            assertEquals(setOf("cocuklar_icin"), ThemeManager.getPersonalizationSnapshot(store, "B").first().travelInterests())
            store.edit { it[booleanPreferencesKey("havamania:A:personalization_enabled")] = false }
            assertTrue(ThemeManager.getPersonalizationSnapshot(store, "A").first().travelInterests().isEmpty())
            scope.coroutineContext[Job]!!.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            store = PreferenceDataStoreFactory.create(scope = scope) { file }
            val reopened = ThemeManager.getPersonalizationSnapshot(store, "A").first()
            assertFalse(reopened.personalizationEnabled); assertTrue(reopened.selectedInterests.isEmpty())
            assertEquals(setOf("cocuklar_icin"), ThemeManager.getPersonalizationSnapshot(store, "B").first().travelInterests())
            assertFalse(ThemeManager.getPersonalizationSnapshot(store, "legacy").first().personalizationEnabled)
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin(); check(file.delete() || !file.exists()) }
    }
    @Test fun guideReactsToOptoutAndRejectsOtherOwnersPreferences() {
        val today = LocalDate.now()
        val plan = TravelPlan(id = "qa-guide", userId = "A", city = "İstanbul", tripType = TripType.CULTURE,
            startDate = today.plusDays(1), endDate = today.plusDays(2))
        var profile by mutableStateOf(PersonalizationProfile(uid = "A", selectedInterests = listOf("bulut_fotografciligi")))
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.LIGHT)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalTravelPersonalization provides profile,
                LocalTravelGuideRefresher provides { _, _, _ -> }) {
                ProvideResponsiveLayout { MaterialTheme {
                    Column(Modifier.width(320.dp).height(650.dp).background(colors.background).verticalScroll(rememberScrollState())) {
                        RichTravelGuideView(plan, today)
                    }
                } }
            }
        }
        compose.onNodeWithText("Galata'yı üç farklı açıdan fotoğrafla").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { profile = profile.copy(personalizationEnabled = false) }
        compose.onNodeWithText("Galata'yı üç farklı açıdan fotoğrafla").assertDoesNotExist()
        compose.onNodeWithText("Atlas Pasajı'nda sinema molası").assertExists()
        compose.runOnIdle { profile = profile.copy(uid = "B", personalizationEnabled = true) }
        compose.onNodeWithText("Galata'yı üç farklı açıdan fotoğrafla").assertDoesNotExist()
        compose.onNodeWithText("Atlas Pasajı'nda sinema molası").assertExists()
    }
    @Test fun guideShowsRetainedStateThenUpdatesAndUsesSnapshotPacking() {
        val today = LocalDate.now()
        var plan by mutableStateOf(TravelPlan(id = "qa-refresh", userId = "A", city = "İstanbul",
            startDate = today.plusDays(1), endDate = today.plusDays(2),
            weatherAnalysisStatus = TravelWeatherAnalysisStatus.WEATHER_PARTIAL_READY,
            lastForecastSnapshot = ForecastSnapshot(minTemp = -3.0, maxTemp = 5.0, weatherCode = 71),
            analyses = listOf(TravelWeatherAnalysis(averageTemperature = 35.0, summary = "Eski sıcak tahmin"))))
        plan = plan.copy(lastForecastSnapshot = plan.lastForecastSnapshot!!.copy(timezone = "Europe/Istanbul",
            binding = createTripForecastBinding(plan, plan.startDate, plan.endDate)))
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.LIGHT)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalTravelGuideRefresher provides { _, _, _ -> }) {
                ProvideResponsiveLayout { MaterialTheme {
                    Column(Modifier.width(320.dp).height(650.dp).verticalScroll(rememberScrollState())) {
                        RichTravelGuideView(plan, today)
                    }
                } }
            }
        }
        compose.onNodeWithText("Hava verisi eksik veya yenilenemedi", substring = true).assertExists()
        compose.onNodeWithText("35°C").assertDoesNotExist()
        compose.onNodeWithText("Soğuk veya donan yağış tahminine karşı", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("güneş koruması al", substring = true).assertDoesNotExist()
        compose.runOnIdle {
            plan = plan.copy(weatherAnalysisStatus = TravelWeatherAnalysisStatus.WEATHER_READY_ANALYSIS_READY,
                lastForecastSnapshot = ForecastSnapshot(minTemp = 18.0, maxTemp = 31.0, precipitationProbability = 10,
                    windSpeed = 5.0, weatherCode = 0, averageTemperature = 24.0, travelScore = 85, generatedAt = System.currentTimeMillis(), timezone = "Europe/Istanbul", binding = createTripForecastBinding(plan, plan.startDate, plan.endDate)))
        }
        compose.onNodeWithText("Hava verisi eksik veya yenilenemedi", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Soğuk veya donan yağış tahminine karşı", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Sıcaklık tahminine göre hafif", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("24°C").assertExists()
        compose.onNodeWithText("85/100").assertExists()
        compose.runOnIdle { plan = plan.copy(city = "Bursa") }
        compose.onNodeWithText("24°C").assertDoesNotExist()
        compose.onNodeWithText("85/100").assertDoesNotExist()
        compose.onNodeWithText("Sıcaklık tahminine göre hafif", substring = true).assertDoesNotExist()
        compose.onAllNodesWithText("eşleşmesi doğrulanamadı", substring = true)[0].performScrollTo().assertIsDisplayed()

    }
}
