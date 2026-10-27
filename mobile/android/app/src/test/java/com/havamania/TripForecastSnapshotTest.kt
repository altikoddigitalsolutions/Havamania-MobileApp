package com.havamania

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TripForecastSnapshotTest {
    private val date = LocalDate.of(2026, 11, 1)
    private val daily = DailyDto(time = listOf("2026-11-01"), weatherCode = listOf(0),
        tempMax = listOf(20.0), tempMin = listOf(10.0), windSpeedMax = listOf(12.0), precipProbMax = listOf(10))

    @Test fun missingOrUnknownCodeDoesNotBecomeClearWeather() {
        assertNull(buildTripForecastSnapshot(daily.copy(weatherCode = emptyList()), date, date))
        assertNull(buildTripForecastSnapshot(daily.copy(weatherCode = listOf(-1)), date, date))
        assertEquals(0, buildTripForecastSnapshot(daily, date, date)!!.weatherCode)
    }
    @Test fun partialCoverageAndDuplicateDatesCannotLookComplete() {
        assertNull(buildTripForecastSnapshot(daily, date, date.plusDays(1)))
        assertNull(buildTripForecastSnapshot(daily.copy(time = listOf(date.toString(), date.toString())), date, date.plusDays(1)))
        assertNull(buildTripForecastSnapshot(daily.copy(time = listOf("invalid")), date, date))
    }
    @Test fun invalidTemperaturesAndMissingRiskFieldsAreRejected() {
        assertNull(buildTripForecastSnapshot(daily.copy(tempMin = listOf(Double.NaN)), date, date))
        assertNull(buildTripForecastSnapshot(daily.copy(tempMax = listOf(Double.POSITIVE_INFINITY)), date, date))
        assertNull(buildTripForecastSnapshot(daily.copy(tempMin = listOf(30.0)), date, date))
        assertNull(buildTripForecastSnapshot(daily.copy(windSpeedMax = emptyList()), date, date))
        assertNull(buildTripForecastSnapshot(daily.copy(precipProbMax = null), date, date))
        assertNull(buildTripForecastSnapshot(daily.copy(precipProbMax = listOf(101)), date, date))
    }
    @Test fun completeCoverageAggregatesOnlyRequestedDays() {
        val data = DailyDto(time = listOf("2026-10-31", "2026-11-01", "2026-11-02"), weatherCode = listOf(99, 61, 61),
            tempMax = listOf(99.0, 20.0, 24.0), tempMin = listOf(90.0, 10.0, 14.0),
            windSpeedMax = listOf(100.0, 12.0, 22.0), precipProbMax = listOf(100, 10, 80))
        val result = buildTripForecastSnapshot(data, date, date.plusDays(1))!!
        assertEquals(10.0, result.minTemp!!, 0.001)
        assertEquals(24.0, result.maxTemp!!, 0.001)
        assertEquals(80, result.precipitationProbability)
        assertEquals(22.0, result.windSpeed!!, 0.001)
        assertEquals(61, result.weatherCode)
    }
    @Test fun tenDayPreparationBoundaryIsInclusiveAndRequiresNonPastTrip() {
        assertTrue(TravelStatusResolver.isTripAnalysisEligible(date.plusDays(10), date.plusDays(10), date))
        assertFalse(TravelStatusResolver.isTripAnalysisEligible(date.plusDays(11), date.plusDays(11), date))
        assertTrue(TravelStatusResolver.isTripAnalysisEligible(date.minusDays(1), date.plusDays(1), date))
        assertFalse(TravelStatusResolver.isTripAnalysisEligible(date.minusDays(2), date.minusDays(1), date))
        assertFalse(TravelStatusResolver.isTripAnalysisEligible(date.plusDays(1), date, date))
    }
}
