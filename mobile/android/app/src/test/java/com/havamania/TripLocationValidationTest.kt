package com.havamania

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TripLocationValidationTest {
    @Test fun unresolvedMissingAndInvalidCoordinatesAreRejected() {
        assertFalse(hasResolvedTripCoordinates(0.0, 0.0))
        assertFalse(hasResolvedTripCoordinates(null, 32.8))
        assertFalse(hasResolvedTripCoordinates(Double.NaN, 32.8))
        assertFalse(hasResolvedTripCoordinates(39.9, Double.POSITIVE_INFINITY))
        assertFalse(hasResolvedTripCoordinates(91.0, 32.8))
        assertFalse(hasResolvedTripCoordinates(39.9, -181.0))
    }
    @Test fun resolvedLocationsIncludingSingleZeroAxisAreAccepted() {
        assertTrue(hasResolvedTripCoordinates(39.9, 32.8))
        assertTrue(hasResolvedTripCoordinates(0.0, 32.8))
        assertTrue(hasResolvedTripCoordinates(39.9, 0.0))
    }
    @Test fun namedOriginMustHaveResolvedCoordinatesButEmptyOriginIsOptional() {
        val date = LocalDate.of(2026, 11, 1)
        val plan = TravelPlan(city = "Ankara", latitude = 39.9, longitude = 32.8, startDate = date, endDate = date)
        assertNull(tripLocationError(plan))
        assertNotNull(tripLocationError(plan.copy(latitude = 0.0, longitude = 0.0)))
        assertNotNull(tripLocationError(plan.copy(originCity = "İstanbul")))
        assertNull(tripLocationError(plan.copy(originCity = "İstanbul", originLatitude = 41.0, originLongitude = 29.0)))
    }
}
