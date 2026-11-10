package com.havamania

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TripForecastInvalidationTest {
    private val date = LocalDate.of(2026, 11, 1)
    private val previous = TravelPlan(id = "trip", userId = "A", city = "Ankara", latitude = 39.9, longitude = 32.8,
        startDate = date, endDate = date.plusDays(1), userNote = "Kullanıcı notu", userRating = 4,
        aiSuggestion = "Eski öneri", weatherSummary = "Eski hava", routeWeatherSummary = "Eski rota",
        lastAnalysisAt = 100, lastRouteAnalysisAt = 100, comfortScore = 96,
        lastForecastSnapshot = ForecastSnapshot(timezone = "Europe/Istanbul"),
        previousForecastSnapshot = ForecastSnapshot(), analyses = listOf(TravelWeatherAnalysis()),
        weatherAnalysisStatus = TravelWeatherAnalysisStatus.WEATHER_READY_ANALYSIS_READY)

    @Test fun destinationAndPeriodChangesInvalidateDerivedDataButKeepUserContent() {
        listOf(previous.copy(city = "Tokyo", latitude = 35.7, longitude = 139.7),
            previous.copy(endDate = date.plusDays(2)), previous.copy(tripType = TripType.CAMPING)).forEach { edited ->
            val result = invalidateChangedTripForecast(previous, edited)
            assertNull(result.lastForecastSnapshot)
            assertNull(result.previousForecastSnapshot)
            assertNull(result.aiSuggestion)
            assertNull(result.routeWeatherSummary)
            assertTrue(result.analyses.isEmpty())
            assertEquals("Kullanıcı notu", result.userNote)
            assertEquals(4, result.userRating)
            assertEquals("trip", result.id)
            assertEquals("A", result.userId)
        }
    }
    @Test fun originOrDepartureChangesOnlyInvalidateRouteData() {
        listOf(previous.copy(originCity = "Bursa"), previous.copy(departureTime = "12:00")).forEach { edited ->
            val result = invalidateChangedTripForecast(previous, edited)
            assertNull(result.routeWeatherSummary)
            assertNull(result.lastRouteAnalysisAt)
            assertEquals(previous.lastForecastSnapshot, result.lastForecastSnapshot)
            assertEquals(previous.analyses, result.analyses)
        }
    }
    @Test fun noContextChangePreservesForecastAndKnownDestinationDateIgnoresDeviceFallback() {
        assertEquals(previous, invalidateChangedTripForecast(previous, previous))
        val now = Instant.parse("2026-10-27T15:10:00Z")
        val tokyo = previous.copy(lastForecastSnapshot = ForecastSnapshot(timezone = "Asia/Tokyo"))
        assertEquals(LocalDate.of(2026, 10, 28), tripAnalysisToday(tokyo, LocalDate.of(2026, 10, 27), now))
        val legacy = previous.copy(lastForecastSnapshot = null)
        assertEquals(LocalDate.of(2026, 10, 27), tripAnalysisToday(legacy, LocalDate.of(2026, 10, 27), now))
    }
}
