package com.havamania

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class DefaultCityIdentityDeviceTest {
    private val france = GeocodingResultDto(1, "Paris", 48.8, 2.3, countryCode = "FR")
    private val usa = GeocodingResultDto(2, "Paris", 33.6, -95.5, countryCode = "US")
    @Test fun wrongSameNameCannotPruneDefaultFenceAndOldAckCannotUndoNewChoice() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "default-identity-" + UUID.randomUUID() + ".preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val data = PreferenceDataStoreFactory.create(scope = scope) { file }
            val store = CitySyncStore(data)
            store.recordDefaultCity("a", france)
            store.recordDefaultCity("b", usa)
            val first = store.pendingDefaultCity("a")!!
            store.acknowledgeDefaultCity("a", first)
            assertEquals(france, store.mergeServerDefaultCity("a", "Paris", usa))
            assertNotNull(store.pendingDefaultCity("a"))
            assertEquals(france, store.observeDefaultCity("a").first())
            store.mergeServerDefaultCity("a", "Paris", france)
            assertNull(store.pendingDefaultCity("a"))
            store.recordDefaultCity("a", usa)
            val latest = store.pendingDefaultCity("a")!!
            store.acknowledgeDefaultCity("a", first)
            store.mergeServerDefaultCity("a", "Paris", france)
            assertEquals(latest, store.pendingDefaultCity("a"))
            assertEquals(usa, store.observeDefaultCity("a").first())
            assertEquals(usa, store.observeDefaultCity("b").first())
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete() }
    }
    @Test fun ambiguousNameAndMismatchedResolvedNameLeavePreferencesUnchanged() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "default-ambiguous-" + UUID.randomUUID() + ".preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val data = PreferenceDataStoreFactory.create(scope = scope) { file }
            val store = CitySyncStore(data)
            store.recordDefaultCity("a", france)
            val before = data.data.first().asMap()
            try {
                val resolved = resolveNamedDefaultCity("Paris", listOf(france, usa))
                store.mergeServerDefaultCity("a", "Paris", resolved)
                fail("Ambiguous name expected")
            } catch (_: DefaultCityNameAmbiguousException) { }
            assertEquals(before, data.data.first().asMap())
            try { store.mergeServerDefaultCity("a", "Ankara", france); fail("Mismatched name expected") }
            catch (_: CityIdentityConflictException) { }
            assertEquals(before, data.data.first().asMap())
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete() }
    }
}