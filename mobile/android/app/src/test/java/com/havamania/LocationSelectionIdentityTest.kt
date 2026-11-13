package com.havamania

import org.junit.Assert.*
import org.junit.Test

class LocationSelectionIdentityTest {
    private val paris = GeocodingResultDto(name = "Paris", latitude = 48.8, longitude = 2.3, country = "Fransa")
    @Test fun sameNameAtOtherCoordinatesIsNotCurrentLocation() {
        assertTrue(matchesWeatherSelection(48.8, 2.3, "Paris", null, paris))
        assertFalse(matchesWeatherSelection(33.6, -95.5, "Paris", null, paris))
    }
    @Test fun missingIdsDoNotMakeDifferentLocationsBothDefault() {
        assertTrue(sameLocationRecord(paris, paris.copy()))
        assertFalse(sameLocationRecord(paris, paris.copy(latitude = 33.6, longitude = -95.5, country = "ABD")))
        assertFalse(sameLocationRecord(paris, null))
    }
    @Test fun providerIdRemainsStableAcrossNameTranslation() {
        assertTrue(sameLocationRecord(paris.copy(id = 7), paris.copy(id = 7, name = "Translated")))
        assertFalse(sameLocationRecord(paris.copy(id = 7), paris.copy(id = 8)))
    }
    @Test fun districtAndInvalidCoordinatesDoNotProduceFalseSelection() {
        assertFalse(matchesWeatherSelection(Double.NaN, 2.3, "Paris", null, paris))
        assertFalse(matchesWeatherSelection(91.0, 2.3, "Paris", null, paris))
        val district = paris.copy(name = "Semt", admin1 = "Paris")
        assertFalse(matchesWeatherSelection(48.8, 2.3, "Paris", null, district))
        assertTrue(matchesWeatherSelection(48.8, 2.3, "PARIS", "Semt", district))
    }
}