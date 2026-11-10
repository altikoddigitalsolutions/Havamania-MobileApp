package com.havamania

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

/** Separate files and fake UIDs; never opens application preferences or Firebase. */
class CitySyncStoreDeviceTest {
    @Test fun restartPreservesChangesAndSeparatesAccounts() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "city-sync-${UUID.randomUUID()}.preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val a = GeocodingResultDto(id = 1, name = "Ankara")
        val b = GeocodingResultDto(id = 2, name = "Bursa")
        try {
            var data = PreferenceDataStoreFactory.create(scope = scope) { file }
            var journal = CitySyncStore(data)
            journal.mergeRemoteCities("a", listOf(b))
            journal.recordCityChange("a", a, false)
            journal.recordCityChange("b", b, false)
            scope.coroutineContext[Job]!!.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            data = PreferenceDataStoreFactory.create(scope = scope) { file }
            journal = CitySyncStore(data)
            assertEquals(a, journal.pendingCityChanges("a").single().city)
            val oldChange = journal.pendingCityChanges("a").single()
            journal.recordCityChange("a", a, true)
            journal.acknowledge("a", oldChange)
            assertFalse(journal.pendingCityChanges("a").single().acknowledged)
            journal.mergeRemoteCities("a", emptyList())
            assertEquals(1, journal.pendingCityChanges("a").size)
            journal.recordCityChange("a", a, false)

            assertEquals(b, journal.pendingCityChanges("b").single().city)
            assertEquals(listOf(a), journal.mergeRemoteCities("a", emptyList()))
            assertEquals(1, journal.pendingCityChanges("a").size)
            journal.acknowledge("a", journal.pendingCityChanges("a").single())
            journal.mergeRemoteCities("a", listOf(a))
            assertTrue(journal.pendingCityChanges("a").isEmpty())
            journal.mergeRemoteCities("a", listOf(a, b))
            journal.recordCityChange("a", a, true)
            assertTrue(journal.mergeRemoteCities("a", listOf(a)).isEmpty())
            assertTrue(journal.pendingCityChanges("a").single().deleted)
            journal.acknowledge("a", journal.pendingCityChanges("a").single())
            journal.mergeRemoteCities("a", emptyList())
            assertTrue(journal.pendingCityChanges("a").isEmpty())
            assertEquals(b, journal.pendingCityChanges("b").single().city)
            // Account deletion uses this same UID prefix; other users survive.
            data.edit { prefs ->
                prefs.asMap().keys.filter { it.name.startsWith("havamania:b:") }.forEach { prefs.remove(it) }
            }
            assertTrue(journal.pendingCityChanges("b").isEmpty())
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
            file.delete()
        }
    }
}
