package com.havamania

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CitySyncClockDeviceTest {
    @Test fun clockAnchorsPersistAndRebootRebaseOccursOnlyOnce() = runBlocking {
        val actual = CitySyncClocks.capture(InstrumentationRegistry.getInstrumentation().targetContext)
        assertTrue(actual.elapsed >= 0)
        assertTrue(actual.boot.isNotBlank())
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "city-clock-${UUID.randomUUID()}.preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var clock = CitySyncClock(100, 10, "boot:1")
        try {
            var store = CitySyncStore(PreferenceDataStoreFactory.create(scope = scope) { file }, clock = { clock })
            store.recordDefaultCity("a", GeocodingResultDto(id = 1, name = "A"))
            val initial = store.pendingDefaultCity("a")!!
            val reserved = store.reserveAttempt("a", initial, true, clock.wall)!!
            assertEquals("boot:1", reserved.retryBoot)
            scope.coroutineContext[Job]!!.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            store = CitySyncStore(PreferenceDataStoreFactory.create(scope = scope) { file }, clock = { clock })
            assertEquals(reserved, store.pendingDefaultCity("a"))
            clock = CitySyncClock(1000000, 20, "boot:1")
            assertNull(store.reserveAttempt("a", reserved, true, clock.wall))
            clock = CitySyncClock(1, 20, "boot:2")
            val rebased = store.refreshRetryClock("a", reserved, true)!!
            assertEquals("boot:2", rebased.retryBoot)
            clock = CitySyncClock(1000000, 30, "boot:2")
            assertEquals(rebased, store.refreshRetryClock("a", rebased, true))
            assertEquals(CITY_SYNC_UNCERTAIN_SECONDS - 10, rebased.retrySecondsLeft(clock))
            clock = CitySyncClock(1, 20 + CITY_SYNC_UNCERTAIN_SECONDS, "boot:2")
            assertNotNull(store.reserveAttempt("a", rebased, true, clock.wall))
            assertNull(store.pendingDefaultCity("b"))
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete() }
    }
}
