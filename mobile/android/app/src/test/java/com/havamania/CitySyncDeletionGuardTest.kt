package com.havamania

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CitySyncDeletionGuardTest {
    private val change = CitySyncChange(GeocodingResultDto(id = 1, name = "A"), false)
    @Test fun deletionBeforeAttemptPreventsReadAndNetwork() = runBlocking {
        CitySyncDispatcher.sendLatest("delete-start", "a", { "a" }, { fail("No read"); null },
            { fail("No send") }, { fail("No acknowledgement") }, isAllowed = { false })
    }
    @Test fun deletionDuringReservationPreventsNetwork() = runBlocking {
        var allowed = true
        CitySyncDispatcher.sendLatest("delete-reserve", "a", { "a" }, { change },
            { fail("No send") }, { fail("No acknowledgement") },
            reserve = { current, _ -> allowed = false; current }, isAllowed = { allowed })
    }
    @Test fun deletionDuringRemoteSendSuppressesAcknowledgementAndFailureWrites() = runBlocking {
        for (fails in listOf(false, true)) {
            var allowed = true
            try {
                CitySyncDispatcher.sendLatest("delete-return-$fails", "a", { "a" }, { change },
                    { allowed = false; if (fails) throw java.io.IOException("test") },
                    { fail("No acknowledgement after deletion") },
                    recordFailure = { _, _ -> fail("No failure write after deletion") }, isAllowed = { allowed })
            } catch (_: java.io.IOException) { assertTrue(fails) }
        }
    }
}
