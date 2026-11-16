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

class RegisteredCityRepairDeviceTest {
    @Test fun exactRepairSurvivesReopenAndPreservesDefaultQueueAndOtherAccount() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "city-repair-" + UUID.randomUUID() + ".preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var reopenedScope: CoroutineScope? = null
        val key = stringPreferencesKey("havamania:a:registered_cities")
        val other = stringPreferencesKey("havamania:b:registered_cities")
        val default = stringPreferencesKey("havamania:a:default_city")
        val queue = stringPreferencesKey("havamania:a:city_sync_changes")
        val a = GeocodingResultDto(1, "A")
        val b = GeocodingResultDto(2, "B")
        val otherRaw = Json.encodeToString(listOf(b, b))
        val defaultRaw = Json.encodeToString(a)
        val queueRaw = Json.encodeToString(listOf(CitySyncChange(a, false, revision = "preserve")))
        try {
            val data = PreferenceDataStoreFactory.create(scope = scope) { file }
            data.edit { it[key] = Json.encodeToString(listOf(a, a, b)); it[other] = otherRaw; it[default] = defaultRaw; it[queue] = queueRaw }
            val store = CitySyncStore(data)
            assertEquals(listOf(a, b), store.observeCities("a").first()) // Production onStart repair.
            assertEquals(CityRepairStatus.UNCHANGED, store.repairExactCityDuplicates("a"))
            val prefs = data.data.first()
            assertEquals(otherRaw, prefs[other]); assertEquals(defaultRaw, prefs[default]); assertEquals(queueRaw, prefs[queue])
            scope.coroutineContext[Job]!!.cancelAndJoin()
            val nextScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            reopenedScope = nextScope
            val reopened = PreferenceDataStoreFactory.create(scope = nextScope) { file }
            assertEquals(Json.encodeToString(listOf(a, b)), reopened.data.first()[key])
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
            reopenedScope?.coroutineContext?.get(Job)?.cancelAndJoin()
            file.delete()
        }
    }
    @Test fun blockedConflictingAndCorruptListsAreNotOverwritten() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "city-repair-conflict-" + UUID.randomUUID() + ".preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val key = stringPreferencesKey("havamania:a:registered_cities")
        var allowed = false
        try {
            val data = PreferenceDataStoreFactory.create(scope = scope) { file }
            val a = GeocodingResultDto(1, "A", 40.0, 29.0)
            val raw = Json.encodeToString(listOf(a, a.copy(latitude = 10.0)))
            data.edit { it[key] = raw }
            val store = CitySyncStore(data, writesAllowed = { allowed })
            assertEquals(CityRepairStatus.BLOCKED, store.repairExactCityDuplicates("a"))
            allowed = true
            assertEquals(CityRepairStatus.CONFLICT, store.repairExactCityDuplicates("a"))
            assertEquals(raw, data.data.first()[key])
            data.edit { it[key] = "broken-json" }
            assertEquals(CityRepairStatus.INVALID, store.repairExactCityDuplicates("a"))
            assertEquals("broken-json", data.data.first()[key])
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete() }
    }
}