package com.havamania

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CityDocumentMutationTest {
    private val city = GeocodingResultDto(1, "A", 40.0, 29.0, countryCode = "TR")
    private fun rejected(block: () -> Unit) {
        try { block(); fail("Conflict expected") } catch (_: CityIdentityConflictException) { }
    }
    @Test fun mismatchedRemoteDocumentCannotBeDeletedOrOverwritten() {
        var writes = 0
        for (deleted in listOf(true, false)) {
            rejected { stageCityDocumentMutation(CitySyncChange(city, deleted), { true },
                { city.copy(latitude = 10.0) }, { writes++ }, { writes++ }) }
        }
        assertEquals(0, writes)
    }
    @Test fun validCreateUpdateAndAlreadyAbsentDeleteAreSupported() {
        var writes = 0; var deletes = 0
        assertTrue(stageCityDocumentMutation(CitySyncChange(city, false), { true }, { null }, { writes++ }, { deletes++ }))
        assertTrue(stageCityDocumentMutation(CitySyncChange(city, false), { true }, { city.copy(name = "Old") }, { writes++ }, { deletes++ }))
        assertTrue(stageCityDocumentMutation(CitySyncChange(city, true), { true }, { null }, { writes++ }, { deletes++ }))
        assertEquals(2, writes); assertEquals(0, deletes)
    }
    @Test fun accountOrDeletionGateChangeDuringReadStopsStaging() {
        var current = true; var writes = 0
        assertFalse(stageCityDocumentMutation(CitySyncChange(city, true), { current },
            { current = false; city }, { writes++ }, { writes++ }))
        assertEquals(0, writes)
    }
    @Test fun sdkRetryRevalidatesNewDocumentInsteadOfReusingFirstRead() {
        var currentDocument = city
        var staged = false
        stageCityDocumentMutation(CitySyncChange(city, true), { true }, { currentDocument }, {}, { staged = true })
        assertTrue(staged)
        staged = false // The first SDK transaction conflicted and did not commit.
        currentDocument = city.copy(id = 2)
        rejected { stageCityDocumentMutation(CitySyncChange(city, true), { true }, { currentDocument }, {}, { staged = true }) }
        assertFalse(staged)
        assertFalse(stageCityDocumentMutation(CitySyncChange(city, true), { false }, { fail("No read"); null }, {}, { staged = true }))
    }
    @Test fun duplicateQueuedIdsCannotSelectArbitraryRevision() {
        val change = CitySyncChange(city, false)
        rejected { singleCitySyncChange(listOf(change, change.copy(revision = "other")), city.id) }
        assertEquals(change, singleCitySyncChange(listOf(change), city.id))
    }
    @Test fun wrappedSdkConflictRemainsBlockedAndHasSpecificMessage() {
        val wrapped = IllegalStateException("SDK wrapper", CityIdentityConflictException())
        assertEquals(CitySyncFailure.UNKNOWN, citySyncFailure(wrapped))
        assertTrue(citySyncErrorText(wrapped).contains("kimlikleri çakışıyor"))
    }
    @Test fun dispatcherDoesNotAcknowledgeIdentityConflict() = runBlocking {
        var pending = CitySyncChange(city, true)
        var acknowledged = false; var writes = 0
        try {
            CitySyncDispatcher.sendLatest("identity-test", "A", { "A" }, read = { pending },
                send = { change -> stageCityDocumentMutation(change, { true }, { city.copy(longitude = 10.0) }, { writes++ }, { writes++ }) },
                acknowledge = { acknowledged = true },
                recordFailure = { sent, error -> pending = sent.copy(blockedReason = citySyncFailure(error).name) })
            fail("Conflict expected")
        } catch (_: CityIdentityConflictException) { }
        assertFalse(acknowledged)
        assertEquals(0, writes)
        assertEquals("UNKNOWN", pending.blockedReason)
    }
}