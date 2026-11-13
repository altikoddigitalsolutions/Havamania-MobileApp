package com.havamania

import org.junit.Assert.*
import org.junit.Test

class LocationSuggestionPresentationTest {
    @Test fun countryIsVisibleWithoutDuplicateRegionOrLeadingComma() {
        val row = GeocodingResultDto(name = "Balıkesir", country = "Türkiye", admin1 = "Balıkesir")
        assertEquals("Türkiye", locationSupportingText(row))
        assertEquals("Türkiye", locationSupportingText(row.copy(admin1 = "")))
        assertEquals("Ankara · Türkiye", locationSupportingText(row.copy(name = "Bala", admin1 = "Ankara")))
    }
    @Test fun missingCountryFallsBackToCodeThenExplicitUnknown() {
        assertEquals("TR", locationSupportingText(GeocodingResultDto(countryCode = "tr")))
        assertEquals("Ülke bilgisi yok", locationSupportingText(GeocodingResultDto()))
    }
    @Test fun locationsWithoutProviderIdsAreNotCollapsedAcrossCountries() {
        val first = GeocodingResultDto(name = "Paris", latitude = 48.8, longitude = 2.3, countryCode = "FR")
        assertNotEquals(locationSuggestionKey(first), locationSuggestionKey(first.copy(countryCode = "US", latitude = 33.6)))
        assertEquals(locationSuggestionKey(first), locationSuggestionKey(first.copy()))
    }
    @Test fun providerIdKeepsIdentityWhenTranslatedNameChanges() {
        val row = GeocodingResultDto(id = 2, name = "Balıkesir", countryCode = "TR")
        assertEquals(locationSuggestionKey(row), locationSuggestionKey(row.copy(name = "Balikesir")))
    }
}