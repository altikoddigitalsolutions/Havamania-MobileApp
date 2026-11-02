package com.havamania

import org.junit.Assert.*
import org.junit.Test

class CitySyncJournalTest {
    private val a = GeocodingResultDto(id = 1, name = "Ankara")
    private val b = GeocodingResultDto(id = 2, name = "Bursa")

    @Test fun staleSnapshotCannotUndoPendingAddOrDelete() {
        val changes = listOf(CitySyncChange(a, false), CitySyncChange(b, true))
        assertEquals(listOf(a), mergeCityChanges(listOf(b), changes))
        assertEquals(changes, unconfirmedCityChanges(listOf(b), changes))
    }
    @Test fun matchingServerSnapshotConfirmsDesiredValues() {
        val changes = listOf(CitySyncChange(a, false, acknowledged = true), CitySyncChange(b, true, acknowledged = true))
        assertTrue(unconfirmedCityChanges(listOf(a), changes).isEmpty())
    }
    @Test fun sameIdWithDifferentDataIsNotAcknowledged() {
        val change = CitySyncChange(a, false)
        assertEquals(listOf(change), unconfirmedCityChanges(listOf(a.copy(name = "old")), listOf(change)))
        assertEquals(listOf(a), mergeCityChanges(listOf(a.copy(name = "old")), listOf(change)))
    }
}
