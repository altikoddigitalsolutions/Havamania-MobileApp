package com.havamania

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CitySyncPreflightDeviceTest {
    @Test fun failureAfterDiskCommitPreventsNetworkAndFenceSurvivesReopen() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "preflight-${UUID.randomUUID()}.preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val base = PreferenceDataStoreFactory.create(scope = scope) { file }
            var store = CitySyncStore(base)
            store.recordDefaultCity("a", GeocodingResultDto(id = 1, name = "A"))
            val sent = store.pendingDefaultCity("a")!!
            val failAfterCommit = object : DataStore<Preferences> {
                override val data = base.data
                override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                    base.updateData(transform)
                    throw IOException("injected after commit")
                }
            }
            try {
                CitySyncDispatcher.sendLatest("device-preflight", "a", { "a" }, { sent },
                    { fail("No network after failed preflight") }, { fail("No acknowledgement") }, now = { 100 },
                    reserve = { change, now -> CitySyncStore(failAfterCommit).reserveAttempt("a", change, true, now) })
                fail("Expected injected error")
            } catch (_: IOException) { }
            scope.coroutineContext[Job]!!.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            store = CitySyncStore(PreferenceDataStoreFactory.create(scope = scope) { file })
            val restored = store.pendingDefaultCity("a")!!
            assertEquals(100 + CITY_SYNC_UNCERTAIN_SECONDS, restored.retryNotBefore)
            assertFalse(restored.acknowledged)
            assertNull(store.reserveAttempt("a", restored, true, 101))
            assertNull(store.reserveAttempt("b", restored, true, Long.MAX_VALUE))
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete() }
    }
}
