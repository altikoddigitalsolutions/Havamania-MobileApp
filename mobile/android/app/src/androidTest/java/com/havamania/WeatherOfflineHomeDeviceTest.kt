package com.havamania

import android.app.Application
import android.location.Location
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import com.havamania.ui.theme.*
import java.io.File
import java.io.IOException
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Actual HomeScreen + ViewModel + persisted city + Room. Network failure is injected, no live calls. */
class WeatherOfflineHomeDeviceTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: WeatherDatabase
    private lateinit var file: File
    private val prefsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val models = ViewModelStore()
    private lateinit var model: WeatherViewModel
    private val calls = AtomicInteger()
    private val city = GeocodingResultDto(16, "Bursa", 40.2, 29.1, "Türkiye", "TR", "Bursa")

    @Before fun setup() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        assertEquals("com.havamania", app.packageName)
        assertNull(FirebaseAuth.getInstance().currentUser)
        db = Room.inMemoryDatabaseBuilder(app, WeatherDatabase::class.java).build()
        file = File(app.cacheDir, "offline-home-${UUID.randomUUID()}.preferences_pb")
        val prefs = PreferenceDataStoreFactory.create(scope = prefsScope) { file }
        prefs.edit { it[weatherLocationModeKey("legacy")] = "AUTO" }
        val cache = AutomaticWeatherCityCache(prefs)
        assertTrue(cache.write("legacy", city) { true })
        val timestamp = System.currentTimeMillis() - 8 * 60 * 60_000L
        // Legacy payload intentionally omits the default city/coordinates and optional metrics.
        val data = WeatherData(temperature = "18°", feelsLike = "Veri yok", timezone = "Europe/Istanbul", timestamp = timestamp)
        db.weatherDao().insertWeather(WeatherCacheEntity(weatherCacheKey("Bursa", null, city.latitude, city.longitude),
            Json.encodeToString(data), timestamp))
        val unused = Proxy.newProxyInstance(WeatherApiService::class.java.classLoader,
            arrayOf(WeatherApiService::class.java)) { _, _, _ -> throw AssertionError("Unexpected API operation") } as WeatherApiService
        val api = object : WeatherApiService by unused {
            override suspend fun getFullWeather(lat: Double, lon: Double, current: String, hourly: String, daily: String,
                timezone: String, days: Int): OpenMeteoResponse {
                assertEquals(city.latitude, lat, 0.00001); assertEquals(city.longitude, lon, 0.00001)
                calls.incrementAndGet(); throw IOException("Offline fixture")
            }
        }
        val repository = WeatherRepository(api, db.weatherDao(), currentOwner = { "legacy" })
        val noPosition = object : LocationTracker {
            override suspend fun getCurrentLocation(): Location? = null
            override suspend fun getCurrentCity(): GeocodingResultDto? = null
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            model = WeatherViewModel(app, repository, object : NetworkMonitor { override val isOnline = flowOf(false) },
                { noPosition }, { flowOf(LocationMode.AUTO to null) }, cache)
            models.put("weather", model)
        }
    }

    @After fun cleanup() = runBlocking {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { models.clear() }
        prefsScope.coroutineContext[Job]!!.cancelAndJoin()
        if (::db.isInitialized) db.close()
        if (::file.isInitialized) file.delete()
    }

    @Test fun coldOfflineOpenUsesLastAutomaticCityAndShowsItsOrigin() {
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                ProvideResponsiveLayout { MaterialTheme { HomeScreen(viewModel = model) } }
            }
        }
        compose.waitUntil(15_000) { model.uiState.value is WeatherUiState.Success && !model.locating.value && !model.isOnline.value }
        try {
            compose.waitUntil(5_000) { runCatching {
                compose.onNodeWithText("BURSA").assertIsDisplayed(); true
            }.getOrDefault(false) }
        } catch (error: Throwable) {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(
                InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                    "screencap -p /data/local/tmp/havamania-offline-home.png")
            ).use { it.readBytes() }
            val bounds = runCatching { compose.onNodeWithText("BURSA", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot }.getOrNull()
            throw AssertionError("Offline city bounds=$bounds", error)
        }
        compose.onNodeWithText("BURSA").assertIsDisplayed()
        compose.onNodeWithText("%65").assertDoesNotExist()
        compose.onNodeWithText("12 km/sa").assertDoesNotExist()
        compose.onNodeWithText("UV 0").assertDoesNotExist()
        compose.onNodeWithText("Çevrimdışı hava verisi", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Mevcut konum doğrulanamadı. Kayıtlı şehrin hava durumu gösteriliyor.").assertIsDisplayed()
        compose.onNodeWithText("BURSA").performClick()
        compose.waitUntil(5_000) { runCatching {
            compose.onNodeWithText("Mevcut konumumu kullan").assertIsDisplayed(); true
        }.getOrDefault(false) }
        compose.onNodeWithText("Mevcut konumumu kullan").assertIsDisplayed().assertIsEnabled()
        compose.runOnIdle {
            assertTrue(calls.get() > 0)
            assertEquals("Bursa", (model.uiState.value as WeatherUiState.Success).data.cityName)
            assertEquals(city.latitude, (model.uiState.value as WeatherUiState.Success).data.latitude!!, 0.00001)
            assertEquals(city.longitude, (model.uiState.value as WeatherUiState.Success).data.longitude!!, 0.00001)
            assertTrue((model.uiState.value as WeatherUiState.Success).data.isStale)
        }
    }
}
