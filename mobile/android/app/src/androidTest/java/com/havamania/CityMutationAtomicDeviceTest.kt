package com.havamania

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class CityMutationAtomicDeviceTest {
    @Test fun concurrentDecisionsUseDiskAndRejectedEditsDoNotChangeQueue() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "atomic-cities-${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = CitySyncStore(PreferenceDataStoreFactory.create(scope = scope) { file })
            val a = GeocodingResultDto(id = 1, name = "A")
            val b = GeocodingResultDto(id = 2, name = "B")
            val c = GeocodingResultDto(id = 3, name = "C")
            val accepted = coroutineScope {
                (1..20).map { async(Dispatchers.IO) { store.recordCityChange("a", a, false) } }.awaitAll()
            }
            assertEquals(1, accepted.count { it })
            val initialQueue = store.pendingCityChanges("a")
            assertFalse(store.recordCityChange("a", a, true)) // Last city.
            assertFalse(store.recordCityChange("a", b, true)) // Missing city.
            assertEquals(initialQueue, store.pendingCityChanges("a"))
            assertTrue(store.recordCityChange("a", b, false))
            store.recordDefaultCity("a", b)
            val beforeRejectedDelete = store.pendingCityChanges("a")
            assertFalse(store.recordCityChange("a", b, true))
            assertEquals(beforeRejectedDelete, store.pendingCityChanges("a"))
            store.recordDefaultCity("a", c) // Manual/search default need not be a registered city.
            val removals = coroutineScope {
                listOf(a, b).map { city -> async(Dispatchers.IO) {
                    store.recordCityChange("a", city, true)
                } }.awaitAll()
            }
            assertEquals(1, removals.count { it })
            assertEquals(1, store.observeCities("a").first().size)
            assertTrue(store.observeCities("b").first().isEmpty())
            assertTrue(store.pendingCityChanges("b").isEmpty())
            assertTrue(store.recordCityChange("legacy", b, false))
            assertFalse(store.recordCityChange("legacy", GeocodingResultDto(id = 0), true))
            assertTrue(store.pendingCityChanges("legacy").isEmpty())
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete()
        }
    }
}
