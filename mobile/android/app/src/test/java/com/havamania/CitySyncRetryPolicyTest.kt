package com.havamania

import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class CitySyncRetryPolicyTest {
    private val change = CitySyncChange(GeocodingResultDto(id = 1, name = "A"), false)
    @Test fun temporaryFailureBackoffIsBoundedAndKeepsRevision() {
        val first = citySyncAfterFailure(change, CitySyncFailure.TEMPORARY, 100) { 0 }
        assertEquals(130L, first.retryNotBefore)
        assertEquals(change.revision, first.revision)
        assertEquals(1, first.failedAttempts)
        val capped = citySyncAfterFailure(first.copy(failedAttempts = Int.MAX_VALUE), CitySyncFailure.TEMPORARY, 100) { it }
        assertEquals(2350L, capped.retryNotBefore)
        assertTrue(capped.failedAttempts <= 31)
    }
    @Test fun permanentFailureStoresOnlySafeCategory() {
        assertEquals(CitySyncFailure.TEMPORARY, citySyncFailure(IOException("private")))
        assertEquals(CitySyncFailure.UNKNOWN, citySyncFailure(IllegalStateException("private")))
        val rejected = citySyncAfterFailure(change, CitySyncFailure.REJECTED, 100)
        assertEquals("REJECTED", rejected.blockedReason)
        assertEquals(0L, rejected.retryNotBefore)
        assertFalse(Json.encodeToString(rejected).contains("private"))
    }
    @Test fun oldJournalIsReadableAndClockArithmeticCannotOverflow() {
        val old = "{\"city\":" + Json.encodeToString(change.city) + ",\"deleted\":false}"
        val restored = Json.decodeFromString<CitySyncChange>(old)
        assertEquals(0, restored.failedAttempts)
        assertNull(restored.blockedReason)
        assertEquals(Long.MAX_VALUE, citySyncAfterFailure(change, CitySyncFailure.TEMPORARY, Long.MAX_VALUE) { 0 }.retryNotBefore)
    }
    @Test fun coolingOrBlockedRecordsNeverCallNetwork() = runBlocking {
        for (pending in listOf(change.copy(retryNotBefore = 200), change.copy(blockedReason = "REJECTED"))) {
            try {
                CitySyncDispatcher.sendLatest("retry-policy", "a", { "a" }, { pending },
                    { fail("No network") }, { fail("No acknowledgement") }, now = { 100 })
                fail("Expected waiting state")
            } catch (_: CitySyncWaitingException) { }
        }
    }
}
