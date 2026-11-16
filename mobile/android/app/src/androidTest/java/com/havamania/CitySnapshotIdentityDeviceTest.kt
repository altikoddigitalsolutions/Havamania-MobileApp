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

class CitySnapshotIdentityDeviceTest {
    private val a = GeocodingResultDto(1, "A", 40.0, 29.0, countryCode = "TR")
    private val b = GeocodingResultDto(2, "B", 41.0, 30.0, countryCode = "TR")
    @Test fun invalidRemoteOrLocalSnapshotCannotOverwriteDiskOrPruneQueue() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "snapshot-conflict-" + UUID.randomUUID() + ".preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val data = PreferenceDataStoreFactory.create(scope = scope) { file }
            val store = CitySyncStore(data)
            store.mergeRemoteCities("a", listOf(a, b))
            store.recordCityChange("a", a.copy(id = 3, name = "C"), false)
            store.recordDefaultCity("a", a)
            store.mergeRemoteCities("b", listOf(b))
            var before = data.data.first().asMap()
            try { store.mergeRemoteCities("a", listOf(a, a.copy(latitude = 10.0))); fail("Conflict expected") }
            catch (_: CityIdentityConflictException) { }
            assertEquals(before, data.data.first().asMap())
            data.edit { it[stringPreferencesKey("havamania:a:registered_cities")] = Json.encodeToString(listOf(a, a.copy(latitude = 10.0))) }
            before = data.data.first().asMap()
            try { store.mergeRemoteCities("a", listOf(a, b)); fail("Local conflict expected") }
            catch (_: CityIdentityConflictException) { }
            assertEquals(before, data.data.first().asMap())
            assertEquals(listOf(b), store.observeCities("b").first())
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete() }
    }
    @Test fun pendingDeleteCannotEraseDifferentRemoteLocationSharingId() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "snapshot-delete-" + UUID.randomUUID() + ".preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val data = PreferenceDataStoreFactory.create(scope = scope) { file }
            val store = CitySyncStore(data)
            store.mergeRemoteCities("a", listOf(a, b))
            store.recordDefaultCity("a", b)
            assertTrue(store.recordCityChange("a", a, true))
            val before = data.data.first().asMap()
            try { store.mergeRemoteCities("a", listOf(a.copy(latitude = 10.0), b)); fail("Delete identity conflict expected") }
            catch (_: CityIdentityConflictException) { }
            assertEquals(before, data.data.first().asMap())
            assertTrue(store.pendingCityChanges("a").single().deleted)
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete() }
    }
}