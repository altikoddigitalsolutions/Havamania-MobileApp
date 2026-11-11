package com.havamania

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.platform.app.InstrumentationRegistry
import com.havamania.ui.theme.LocationMode
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import org.junit.Assert.*
import org.junit.Test

/** Isolated preference files; never accesses real users, account settings or Firebase. */
class AutomaticWeatherCityCacheDeviceTest {
    private val city = GeocodingResultDto(34, "İstanbul", 41.01, 28.97, "Türkiye", "TR", "İstanbul")

    @Test fun restartAndAccountIsolationKeepManualDefaultAndDoNotRetriggerLocationObserver() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "auto-city-${UUID.randomUUID()}.preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var observer: Job? = null
        try {
            var prefs = PreferenceDataStoreFactory.create(scope = scope) { file }
            prefs.edit { it[weatherLocationModeKey("a")] = "AUTO"; it[weatherLocationModeKey("b")] = "AUTO" }
            var journal = CitySyncStore(prefs)
            val manual = city.copy(id = 6, name = "Ankara", admin1 = "Ankara")
            journal.recordDefaultCity("a", manual)
            journal.acknowledgeDefaultCity("a", journal.pendingDefaultCity("a")!!)
            journal.mergeServerDefaultCity("a", manual.name, manual)
            val changes = Channel<Pair<LocationMode, GeocodingResultDto?>>(Channel.UNLIMITED)
            val observed = journal
            observer = launch { observed.observeWeatherLocation("a").collect { changes.send(it) } }
            assertEquals(LocationMode.AUTO to manual, withTimeout(5_000) { changes.receive() })
            assertTrue(AutomaticWeatherCityCache(prefs, clock = { 1000 }).write("a", city) { true })
            assertNull(journal.pendingDefaultCity("a"))
            assertEquals(manual, journal.observeDefaultCity("a").first())
            val nextManual = manual.copy(name = "Bursa")
            journal.recordDefaultCity("a", nextManual)
            assertEquals(LocationMode.AUTO to nextManual, withTimeout(5_000) { changes.receive() })
            observer.cancelAndJoin()
            scope.coroutineContext[Job]!!.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            prefs = PreferenceDataStoreFactory.create(scope = scope) { file }
            val cache = AutomaticWeatherCityCache(prefs, clock = { 2000 })
            assertEquals(city, cache.read("a"))
            assertNull(cache.read("b"))
            journal = CitySyncStore(prefs)
            assertEquals(nextManual, journal.observeDefaultCity("a").first())
        } finally { observer?.cancelAndJoin(); scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete() }
    }

    @Test fun manualModeOldOwnerDeletionAndInvalidCoordinatesCannotOverwriteCache() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "auto-city-${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val prefs = PreferenceDataStoreFactory.create(scope = scope) { file }
            prefs.edit { it[weatherLocationModeKey("a")] = "AUTO" }
            var allowed = true
            val cache = AutomaticWeatherCityCache(prefs, { allowed }, { 1000 })
            assertTrue(cache.write("a", city) { true })
            assertFalse(cache.write("a", city.copy(name = "Bursa")) { false })
            allowed = false
            assertFalse(cache.write("a", city.copy(name = "Bursa")) { true })
            allowed = true
            assertFalse(cache.write("a", city.copy(latitude = 0.0, longitude = 0.0)) { true })
            prefs.edit { it[weatherLocationModeKey("a")] = "MANUAL" }
            assertFalse(cache.write("a", city.copy(name = "Bursa")) { true })
            assertNull(cache.read("a"))
            prefs.edit { it[weatherLocationModeKey("a")] = "AUTO" }
            assertEquals(city, cache.read("a"))
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete() }
    }

    @Test fun expiredCorruptFutureAndDeletedAccountRecordsAreNotReused() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "auto-city-${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val prefs = PreferenceDataStoreFactory.create(scope = scope) { file }
            prefs.edit { it[weatherLocationModeKey("a")] = "AUTO"; it[weatherLocationModeKey("b")] = "AUTO" }
            var now = 1000L
            val cache = AutomaticWeatherCityCache(prefs, clock = { now })
            assertTrue(cache.write("a", city) { true })
            now += 24 * 60 * 60_000L
            assertNull(cache.read("a"))
            val key = stringPreferencesKey("havamania:a:last_automatic_weather_city")
            assertNull(prefs.data.first()[key])
            prefs.edit { it[key] = "broken JSON" }
            assertNull(cache.read("a")); assertNull(prefs.data.first()[key])
            assertTrue(cache.write("a", city) { true })
            now -= 100
            assertNull(cache.read("a"))
            assertTrue(cache.write("a", city) { true }); assertTrue(cache.write("b", city) { true })
            prefs.edit { data -> data.asMap().keys.filter { it.name.startsWith("havamania:a:") }.forEach { data.remove(it) } }
            assertNull(cache.read("a")); assertEquals(city, cache.read("b"))
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete() }
    }
}
