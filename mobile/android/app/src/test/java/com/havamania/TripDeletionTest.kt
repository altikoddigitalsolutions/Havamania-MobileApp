package com.havamania

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TripDeletionTest {
    @Test fun remoteFailurePreservesLocalTrip() = runBlocking {
        var removed = false
        try {
            deleteTripWithAcknowledgement({ error("offline") }, { removed = true })
            fail("Expected failure")
        } catch (_: IllegalStateException) { }
        assertFalse(removed)
    }
    @Test fun localRemovalFollowsRemoteAcknowledgement() = runBlocking {
        val events = mutableListOf<String>()
        deleteTripWithAcknowledgement({ events.add("remote") }, { events.add("local") })
        assertEquals(listOf("remote", "local"), events)
    }
}
