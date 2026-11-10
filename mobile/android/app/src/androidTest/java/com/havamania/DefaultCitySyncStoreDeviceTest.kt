package com.havamania

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.channels.Channel
import com.havamania.ui.theme.LocationMode
import org.junit.Assert.*
import org.junit.Test

/** Isolated preferences files; no production account or network access. */
class DefaultCitySyncStoreDeviceTest {
    @Test fun defaultSelectionSurvivesRestartAndRejectsStaleAcknowledgement() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "default-city-${UUID.randomUUID()}.preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val ankara = GeocodingResultDto(id = 1, name = "Ankara")
        val bursa = GeocodingResultDto(id = 2, name = "Bursa")
        try {
            var preferences = PreferenceDataStoreFactory.create(scope = scope) { file }
            preferences.edit {
                it[weatherLocationModeKey("a")] = "AUTO"
                it[weatherLocationModeKey("b")] = "AUTO"
            }
            var store = CitySyncStore(preferences)
            val changes = Channel<Pair<LocationMode, GeocodingResultDto?>>(Channel.UNLIMITED)
            val observedStore = store
            val observer = launch { observedStore.observeWeatherLocation("a").collect { changes.send(it) } }
            assertEquals(LocationMode.AUTO to null, withTimeout(5_000) { changes.receive() })
            store.recordDefaultCity("a", ankara, useManualLocation = true)
            assertEquals(LocationMode.MANUAL to ankara, withTimeout(5_000) { changes.receive() })
            observer.cancelAndJoin()
            store.recordDefaultCity("b", bursa)
            val first = store.pendingDefaultCity("a")!!
            scope.coroutineContext[Job]!!.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            preferences = PreferenceDataStoreFactory.create(scope = scope) { file }
            store = CitySyncStore(preferences)
            assertEquals(ankara, store.observeDefaultCity("a").first())
            val restored = preferences.data.first()
            assertEquals("MANUAL", restored[weatherLocationModeKey("a")])
            assertEquals("AUTO", restored[weatherLocationModeKey("b")])
            assertEquals(first, store.pendingDefaultCity("a"))
            assertEquals(ankara, store.mergeServerDefaultCity("a", "Bursa", bursa))
            store.recordDefaultCity("a", bursa)
            store.acknowledgeDefaultCity("a", first)
            assertFalse(store.pendingDefaultCity("a")!!.acknowledged)
            assertEquals(bursa, store.mergeServerDefaultCity("a", "Bursa", bursa))
            assertNotNull(store.pendingDefaultCity("a")) // Snapshot alone cannot acknowledge a write.
            store.acknowledgeDefaultCity("a", store.pendingDefaultCity("a")!!)
            assertEquals(bursa, store.mergeServerDefaultCity("a", "Ankara", ankara))
            assertNotNull(store.pendingDefaultCity("a"))
            store.mergeServerDefaultCity("a", "Bursa", bursa)
            assertNull(store.pendingDefaultCity("a"))
            assertEquals(bursa, store.pendingDefaultCity("b")!!.city)
            // A remote name arriving before its city can be resolved later.
            assertNull(store.mergeServerDefaultCity("a", "Ankara", null))
            assertEquals(ankara, store.mergeServerDefaultCity("a", "Ankara", ankara))
            store.recordDefaultCity("legacy", ankara)
            assertNull(store.pendingDefaultCity("legacy"))
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
            file.delete()
        }
    }
}
