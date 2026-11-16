package com.havamania

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class RegisteredCityDeletionDeviceTest {
    @Test fun duplicateDiskRowsCannotBypassLastLocationProtection() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "duplicate-delete-" + UUID.randomUUID() + ".preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val data = PreferenceDataStoreFactory.create(scope = scope) { file }
            val city = GeocodingResultDto(id = 1, name = "Bursa")
            data.edit { it[stringPreferencesKey("havamania:a:registered_cities")] = Json.encodeToString(listOf(city, city)) }
            val store = CitySyncStore(data)
            assertFalse(store.recordCityChange("a", city, deleted = true))
            assertTrue(store.pendingCityChanges("a").isEmpty())
            assertEquals(listOf(city), store.observeCities("a").first())
            val first = city.copy(id = 0, name = "Paris", latitude = 48.8, longitude = 2.3)
            val second = city.copy(id = 0, name = "Paris", latitude = 33.6, longitude = -95.5)
            val conflicting = listOf(first, second, city)
            data.edit { it[stringPreferencesKey("havamania:a:registered_cities")] = Json.encodeToString(conflicting) }
            assertFalse(store.recordCityChange("a", city, deleted = true))
            assertFalse(store.recordCityChange("a", city.copy(id = 3, name = "New"), deleted = false))
            assertEquals(conflicting, store.observeCities("a").first())
            assertTrue(store.pendingCityChanges("a").isEmpty())
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete() }
    }
}