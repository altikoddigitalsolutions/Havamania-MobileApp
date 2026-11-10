package com.havamania

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class CitySyncDeletionStorageDeviceTest {
    @Test fun lateWritesCannotRecreateDataAfterUidCleanup() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "city-deletion-${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var allowed = true
        try {
            val data = PreferenceDataStoreFactory.create(scope = scope) { file }
            val store = CitySyncStore(data) { allowed }
            val city = GeocodingResultDto(id = 1, name = "A")
            store.recordCityChange("a", city, false)
            store.recordDefaultCity("a", city)
            store.recordDefaultCity("b", city)
            val sent = store.pendingCityChanges("a").single()
            val default = store.pendingDefaultCity("a")!!
            allowed = false
            data.edit { prefs ->
                prefs.asMap().keys.filter { it.name.startsWith("havamania:a:") }.forEach { prefs.remove(it) }
            }
            store.recordCityChange("a", city, false)
            store.recordDefaultCity("a", city)
            store.acknowledge("a", sent)
            store.acknowledgeDefaultCity("a", default)
            store.recordFailure("a", sent, sent.copy(blockedReason = "REJECTED"), false)
            store.recordFailure("a", default, default.copy(blockedReason = "REJECTED"), true)
            store.releaseBlocks("a", false)
            store.mergeRemoteCities("a", listOf(city))
            store.mergeServerDefaultCity("a", city.name, city)
            assertNull(store.reserveAttempt("a", sent, false, 100))
            assertNull(store.reserveAttempt("a", default, true, 100))
            assertFalse(data.data.first().asMap().keys.any { it.name.startsWith("havamania:a:") })
            assertNotNull(store.pendingDefaultCity("b"))
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete() }
    }
}
