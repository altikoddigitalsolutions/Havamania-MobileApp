package com.havamania

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Separate in-memory database and fake API. No production account or cloud data. */
class WeatherCacheClearStorageTest {
    @Test fun clearWeatherPreservesBothOwnersTripsHistoryAndSyncQueues() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WeatherDatabase::class.java).build()
        try {
            val dao = database.weatherDao()
            val api = Proxy.newProxyInstance(WeatherApiService::class.java.classLoader,
                arrayOf(WeatherApiService::class.java)) { _, _, _ ->
                throw AssertionError("Cache cleanup must not call weather services")
            } as WeatherApiService
            val repository = WeatherRepository(apiService = api, weatherDao = dao)
            val owners = listOf("local-a", "local-b")
            for (uid in owners) {
                dao.insertWeather(WeatherCacheEntity("weather-$uid", "{}"))
                dao.insertTravelPlanRaw(TravelPlanEntity(id = "trip-$uid", userId = uid,
                    city = "Ankara", tripType = "OTHER", userNote = "Saved note"))
                dao.insertAiHistoryRaw(AiHistoryEntity(id = "history-$uid", userId = uid, title = "Saved conversation"))
                dao.enqueueTripUpload(TripUploadRequest(uid, "trip-$uid", 10))
                dao.enqueueTripDeletion(TripDeletionRequest(uid, "deleted-$uid"))
            }
            val plans = owners.associateWith { dao.getAllTravelPlans(it) }
            val history = owners.associateWith { dao.getAllAiHistory(it) }
            val clearer = WeatherCacheClearer { repository.clearAllCachedWeather() }
            clearer.clear()
            assertEquals(WeatherCacheClearState.SUCCESS, clearer.state.value)
            for (uid in owners) {
                assertNull(dao.getCachedWeather("weather-$uid"))
                assertEquals(plans[uid], dao.getAllTravelPlans(uid))
                assertEquals(history[uid], dao.getAllAiHistory(uid))
                assertEquals(listOf(TripUploadRequest(uid, "trip-$uid", 10)), dao.pendingTripUploads(uid))
                assertEquals(listOf(TripDeletionRequest(uid, "deleted-$uid")), dao.pendingTripDeletions(uid))
            }
            clearer.clear() // Already empty cache is also safe.
            assertEquals(WeatherCacheClearState.SUCCESS, clearer.state.value)
            assertEquals(plans["local-a"], dao.getAllTravelPlans("local-a"))
        } finally { database.close() }
    }
}
