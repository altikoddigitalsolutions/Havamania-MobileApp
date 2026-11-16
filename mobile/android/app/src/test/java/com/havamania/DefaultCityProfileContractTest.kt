package com.havamania

import org.junit.Assert.*
import org.junit.Test

class DefaultCityProfileContractTest {
    private val france = GeocodingResultDto(1, "Paris", 48.8, 2.3, countryCode = "FR")
    private val usa = france.copy(id = 2, latitude = 33.6, longitude = -95.5, countryCode = "US")
    private fun raw(city: GeocodingResultDto = france) = defaultCityProfileFields(city)["defaultCityLocation"] as Map<*, *>
    private fun rejected(value: Any?, name: String? = "Paris", cities: List<GeocodingResultDto> = emptyList()) {
        try { resolveDefaultCityProfile(name, value, cities); fail("Unsafe identity accepted") }
        catch (_: CityIdentityConflictException) { }
    }
    @Test fun companionDisambiguatesSameNameAcrossCountries() {
        assertEquals(france, resolveDefaultCityProfile("Paris", raw(), listOf(usa, france)))
        assertEquals("Paris", defaultCityProfileFields(france)["defaultCity"])
    }
    @Test fun unregisteredManualCoordinatesRoundTripWithoutSavedCity() {
        val manual = france.copy(id = 0)
        assertEquals(manual, resolveDefaultCityProfile("Paris", raw(manual), emptyList()))
    }
    @Test fun legacyNameOnlyRemainsSupported() {
        assertEquals(france, resolveDefaultCityProfile("Paris", null, listOf(france)))
        assertNull(resolveDefaultCityProfile(null, null, emptyList()))
        try { resolveDefaultCityProfile("Paris", null, listOf(france, usa)); fail("Ambiguous legacy name") }
        catch (_: DefaultCityNameAmbiguousException) { }
    }
    @Test fun missingCoordinatesUnknownVersionAndInvalidTypesFailClosed() {
        rejected(raw() - "latitude")
        rejected(raw() + ("longitude" to Double.NaN))
        rejected(raw() + ("latitude" to 91.0))
        rejected(raw() + ("version" to 2L))
        rejected(raw() + ("id" to 1.0))
        rejected(raw() + ("country_code" to 1L))
        rejected("Paris")
    }
    @Test fun staleCompanionAndSavedIdConflictFailClosed() {
        rejected(raw(), "Ankara")
        rejected(raw(), cities = listOf(france.copy(latitude = 40.0)))
    }
    @Test fun invalidOutgoingCoordinatesCannotBeWritten() {
        try { defaultCityProfileFields(france.copy(latitude = Double.NaN)); fail("Invalid coordinates") }
        catch (_: IllegalArgumentException) { }
    }
}
