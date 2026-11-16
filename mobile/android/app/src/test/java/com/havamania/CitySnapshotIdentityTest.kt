package com.havamania

import org.junit.Assert.*
import org.junit.Test

class CitySnapshotIdentityTest {
    private val a = GeocodingResultDto(1, "A", 40.0, 29.0, countryCode = "TR")
    private fun rejected(block: () -> Unit) {
        try { block(); fail("Expected identity conflict") } catch (_: CityIdentityConflictException) { }
    }
    @Test fun duplicateRemoteIdentityIsNotCollapsedArbitrarily() {
        rejected { mergeCityChanges(listOf(a, a.copy(latitude = 10.0)), emptyList()) }
    }
    @Test fun conflictingLocalAndQueuedIdentitiesAreRejected() {
        rejected { validateCitySnapshotMerge(listOf(a, a.copy(longitude = 10.0)), listOf(a), emptyList()) }
        rejected { validateCitySnapshotMerge(emptyList(), emptyList(), listOf(CitySyncChange(a, false), CitySyncChange(a.copy(latitude = 10.0), true))) }
    }
    @Test fun queuedDeleteCannotTargetDifferentCoordinatesButMetadataUpdateRemainsValid() {
        rejected { mergeCityChanges(listOf(a.copy(latitude = 10.0)), listOf(CitySyncChange(a, true))) }
        assertEquals(listOf(a), mergeCityChanges(listOf(a.copy(name = "Old name")), listOf(CitySyncChange(a, false))))
    }
    @Test fun exactDuplicateAndOrdinaryDeleteRemainValidAndErrorIsSpecific() {
        assertEquals(listOf(a), mergeCityChanges(listOf(a, a), emptyList()))
        assertTrue(mergeCityChanges(listOf(a), listOf(CitySyncChange(a, true))).isEmpty())
        val text = citySyncErrorText(CityIdentityConflictException())
        assertTrue(text.contains("kimlikleri çakışıyor"))
        assertTrue(text.contains("korundu"))
        assertFalse(text.contains("Bağlantınızı"))
    }
}