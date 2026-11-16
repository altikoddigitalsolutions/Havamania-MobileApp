package com.havamania

import org.junit.Assert.*
import org.junit.Test

class DefaultCityIdentityTest {
    private val paris = GeocodingResultDto(1, "Paris", 48.8, 2.3, countryCode = "FR")
    @Test fun sameNameAcrossCountriesIsNeverResolvedByFirstRow() {
        try {
            resolveNamedDefaultCity("Paris", listOf(paris, paris.copy(id = 2, latitude = 33.6, longitude = -95.5, countryCode = "US")))
            fail("Ambiguous name")
        } catch (_: DefaultCityNameAmbiguousException) { }
    }
    @Test fun exactDuplicatesAndNormalizedUniqueNamesRemainResolvable() {
        assertEquals(paris, resolveNamedDefaultCity(" PARIS ", listOf(paris, paris.copy())))
        assertNull(resolveNamedDefaultCity(null, listOf(paris)))
        assertNull(resolveNamedDefaultCity("Ankara", listOf(paris)))
    }
    @Test fun acknowledgedNameAloneCannotConfirmOtherLocationOrUnknownIdentity() {
        val change = CitySyncChange(paris, false, acknowledged = true)
        assertFalse(canConfirmDefaultCity(change, "Paris", null))
        assertFalse(canConfirmDefaultCity(change, "Paris", paris.copy(id = 2)))
        assertFalse(canConfirmDefaultCity(change, "Paris", paris.copy(longitude = 10.0)))
        assertFalse(canConfirmDefaultCity(change.copy(acknowledged = false), "Paris", paris))
        assertTrue(canConfirmDefaultCity(change, "Paris", paris))
    }
    @Test fun ambiguityHasActionableMessageWithoutClaimingSuccessfulSync() {
        val text = citySyncErrorText(DefaultCityNameAmbiguousException())
        assertTrue(text.contains("birden fazla"))
        assertTrue(text.contains("korundu"))
        assertFalse(text.contains("eşitleme tamamlandı"))
    }
}