package com.havamania

import java.time.LocalDate
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class TripTemperatureExtremesTest {
    @Test fun dailyExtremesSurviveWhileComfortAverageKeepsItsOriginalMeaning() {
        val day = LocalDate.of(2026, 11, 1)
        val data = DailyDto(time = listOf("2026-11-01", "2026-11-02", "2026-11-03"),
            weatherCode = listOf(0, 0, 0), tempMin = listOf(15.0, -5.0, 15.0), tempMax = listOf(25.0, 25.0, 40.0),
            precipProbMax = listOf(10, 10, 10), windSpeedMax = listOf(10.0, 10.0, 10.0))
        val result = buildTripForecastSnapshot(data, day, day.plusDays(2))!!
        assertEquals(-5.0, result.minTemp!!, 0.001)
        assertEquals(40.0, result.maxTemp!!, 0.001)
        assertEquals((20.0 + 10.0 + 27.5) / 3, result.averageTemperature!!, 0.001)
        val previousAverage = ForecastSnapshot(minTemp = (15.0 - 5.0 + 15.0) / 3,
            maxTemp = 30.0, precipitationProbability = 10, windSpeed = 10.0)
        assertEquals(TravelAnalysisEngine.calculateTravelScore(previousAverage, TripType.NATURE),
            TravelAnalysisEngine.calculateTravelScore(result, TripType.NATURE))
        assertTrue(tripWeatherWarning(result)!!.contains("-5,0°C"))
        assertTrue(tripWeatherWarning(result)!!.contains("40,0°C"))
    }

    @Test fun legacySnapshotDecodesWithoutInventingExtremes() {
        val legacy = Json.decodeFromString(ForecastSnapshot.serializer(), """{"minTemp":10.0,"maxTemp":25.0,"weatherCode":0}""")
        assertNull(legacy.averageTemperature)
        assertNull(tripTemperatureRange(legacy))
        val updated = legacy.copy(averageTemperature = 17.5)
        val decoded = Json.decodeFromString(ForecastSnapshot.serializer(), Json.encodeToString(ForecastSnapshot.serializer(), updated))
        assertEquals(updated, decoded)
    }

    @Test fun highComfortScoreCannotInventClearOrSafeWeatherInCardText() {
        val blank = TravelWeatherAnalysis(travelScore = 98, averageTemperature = 25.0)
        val summary = RecommendationEngine.getShortWeatherSummaryFromAnalysis(blank)
        assertFalse(summary.contains("açık"))
        assertFalse(summary.contains("elverişli"))
        assertTrue(summary.contains("Yağış olasılığı bilinmiyor"))
        assertEquals("Fırtına bekleniyor.", RecommendationEngine.getShortWeatherSummaryFromAnalysis(blank.copy(summary = "Fırtına bekleniyor.")))
    }
}
