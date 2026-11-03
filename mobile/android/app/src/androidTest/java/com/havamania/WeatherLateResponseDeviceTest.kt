package com.havamania

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import java.lang.reflect.Proxy
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import org.junit.Assert.*
import org.junit.Test

class WeatherLateResponseDeviceTest {
    private fun api(fetch: suspend () -> OpenMeteoResponse): WeatherApiService {
        val unused = Proxy.newProxyInstance(WeatherApiService::class.java.classLoader,
            arrayOf(WeatherApiService::class.java)) { _, _, _ -> throw AssertionError("Unexpected API call") } as WeatherApiService
        return object : WeatherApiService by unused {
            override suspend fun getFullWeather(lat: Double, lon: Double, current: String, hourly: String,
                daily: String, timezone: String, days: Int): OpenMeteoResponse = fetch()
        }
    }
    private fun response() = OpenMeteoResponse(latitude = 41.0, longitude = 29.0,
        current = CurrentWeatherDto(temperature = 20.0))

    @Test fun lateSuccessCannotRepopulateCacheOrVisibleWeatherAfterCleanup() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, WeatherDatabase::class.java).build()
        try {
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var allowed = true
            val repo = WeatherRepository(api({ entered.complete(Unit); release.await(); response() }), db.weatherDao(), { allowed })
            val emitted = mutableListOf<WeatherData>()
            val request = launch { repo.getWeatherData(41.0, 29.0, "City").toList(emitted) }
            entered.await(); allowed = false; repo.clearForAccountDeletion()
            allowed = true // Recovery finishes before the old response arrives.
            release.complete(Unit); request.join()
            assertTrue(emitted.isEmpty()); assertNull(repo.currentWeatherState.value)
            assertNull(db.weatherDao().getCachedWeather(weatherCacheKey("City", null, 41.0, 29.0)))
            assertEquals(1, repo.getWeatherData(41.0, 29.0, "New").toList().size)
            assertNotNull(repo.currentWeatherState.value)
        } finally { db.close() }
    }

    @Test fun cleanupWaitsForAlreadyStartedCacheWriteThenRemovesIt() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, WeatherDatabase::class.java).build()
        try {
            val real = db.weatherDao()
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var allowed = true
            val slow = object : WeatherDao by real {
                override suspend fun insertWeather(weather: WeatherCacheEntity) {
                    entered.complete(Unit); release.await(); real.insertWeather(weather)
                }
            }
            val repo = WeatherRepository(api({ response() }), slow, { allowed })
            val emitted = mutableListOf<WeatherData>()
            val request = launch { repo.getWeatherData(41.0, 29.0, "Old").toList(emitted) }
            entered.await(); allowed = false
            val cleanup = async(start = CoroutineStart.UNDISPATCHED) { repo.clearForAccountDeletion() }
            assertFalse(cleanup.isCompleted)
            release.complete(Unit); cleanup.await(); request.join()
            assertTrue(emitted.isEmpty()); assertNull(repo.currentWeatherState.value)
            assertNull(real.getCachedWeather(weatherCacheKey("Old", null, 41.0, 29.0)))
        } finally { db.close() }
    }

    @Test fun flowCreatedBeforeCleanupCannotStartAfterRecovery() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, WeatherDatabase::class.java).build()
        try {
            var calls = 0
            val repo = WeatherRepository(api({ calls++; response() }), db.weatherDao())
            val old = repo.getWeatherData(41.0, 29.0, "Old")
            repo.clearForAccountDeletion()
            assertTrue(old.toList().isEmpty()); assertEquals(0, calls)
            assertEquals(1, repo.getWeatherData(41.0, 29.0, "New").toList().size)
        } finally { db.close() }
    }

    @Test fun pendingDeletionBlocksCacheReadAndLateNetworkError() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, WeatherDatabase::class.java).build()
        try {
            var allowed = true
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var calls = 0
            val repo = WeatherRepository(api({ calls++; entered.complete(Unit); release.await(); throw IOException("test") }),
                db.weatherDao(), { allowed })
            val request = launch { assertTrue(repo.getWeatherData(41.0, 29.0, "Old").toList().isEmpty()) }
            entered.await(); allowed = false; repo.clearForAccountDeletion()
            release.complete(Unit); request.join()
            assertTrue(repo.getWeatherData(41.0, 29.0, "Blocked").toList().isEmpty())
            assertEquals(1, calls)
        } finally { db.close() }
    }
}
