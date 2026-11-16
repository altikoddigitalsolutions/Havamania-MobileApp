package com.havamania

import org.junit.Assert.*
import org.junit.Test

class RegisteredCityRepairTest {
    private val a = GeocodingResultDto(1, "A", 40.0, 29.0, countryCode = "TR")
    @Test fun identicalDuplicatesCompactInOriginalOrder() {
        val b = a.copy(id = 2, name = "B")
        val result = planRegisteredCityRepair(listOf(a, b, a.copy(), b.copy()))
        assertEquals(CityRepairStatus.COMPACTED, result.status)
        assertEquals(listOf(a, b), result.cities)
    }
    @Test fun distinctCoordinatesWithSamePositiveIdRemainUntouched() {
        val raw = listOf(a, a.copy(latitude = 10.0))
        assertEquals(CityRepairStatus.CONFLICT, planRegisteredCityRepair(raw).status)
        assertEquals(raw, planRegisteredCityRepair(raw).cities)
        assertFalse(canDeleteRegisteredCity(raw, null, a))
    }
    @Test fun zeroIdConflictIsNotGuessedOrDropped() {
        val raw = listOf(a.copy(id = 0), a.copy(id = 0, longitude = 10.0))
        assertEquals(CityRepairStatus.CONFLICT, planRegisteredCityRepair(raw).status)
        assertEquals(raw, planRegisteredCityRepair(raw).cities)
    }
    @Test fun repairIsIdempotentAndEmptyListRemainsEmpty() {
        val compacted = planRegisteredCityRepair(listOf(a, a)).cities
        assertEquals(CityRepairStatus.UNCHANGED, planRegisteredCityRepair(compacted).status)
        assertEquals(CityRepairStatus.UNCHANGED, planRegisteredCityRepair(emptyList()).status)
    }
}