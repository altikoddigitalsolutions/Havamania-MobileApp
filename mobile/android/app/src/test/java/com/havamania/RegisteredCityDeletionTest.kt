package com.havamania

import org.junit.Assert.*
import org.junit.Test

class RegisteredCityDeletionTest {
    private val a = GeocodingResultDto(1, "A", 40.0, 29.0, countryCode = "TR")
    private val b = GeocodingResultDto(2, "B", 41.0, 30.0, countryCode = "TR")
    @Test fun duplicateRowsDoNotMakeLastLocationDeletable() {
        assertFalse(canDeleteRegisteredCity(listOf(a, a.copy()), null, a))
    }
    @Test fun defaultMissingAndMismatchedCandidatesAreRejected() {
        assertFalse(canDeleteRegisteredCity(listOf(a, b), a, a))
        assertFalse(canDeleteRegisteredCity(listOf(a, b), null, a.copy(id = 3)))
        assertFalse(canDeleteRegisteredCity(listOf(a.copy(id = 0), b), null, a.copy(id = 0, latitude = 20.0)))
    }
    @Test fun distinctNonDefaultLocationCanBeRemoved() {
        assertTrue(canDeleteRegisteredCity(listOf(a, a.copy(), b), a, b))
    }
    @Test fun collidingLegacyQueueIdsCannotDeleteMultipleDifferentLocations() {
        val first = a.copy(id = 0)
        val second = b.copy(id = 0)
        assertFalse(canDeleteRegisteredCity(listOf(first, second), null, first))
        assertFalse(canDeleteRegisteredCity(listOf(first, second, a), null, second))
        assertFalse(canDeleteRegisteredCity(listOf(first, second, a), null, a))
        assertTrue(hasAmbiguousCityIds(listOf(first, second)))
    }
}