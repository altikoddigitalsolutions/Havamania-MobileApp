package com.havamania

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ForecastInvalidValuesTest {
    @Test fun `nonfinite daily temperatures do not crash or become observations`() {
        val value = WeatherMapper.mapToDomain(OpenMeteoResponse(40.0, 29.0, daily = DailyDto(
            time = listOf("2026-11-12", "2026-11-13", "2026-11-14"), weatherCode = listOf(0, 0, 0),
            tempMin = listOf(Double.NaN, 10.0, 5.0), tempMax = listOf(Double.POSITIVE_INFINITY, 8.0, 15.0)
        )), "Bursa")
        assertEquals("--", value.high)
        assertEquals("--", value.low)
        assertEquals("2026-11-14", value.dailyForecast.single().date)
    }
    @Test fun `invalid hourly values and malformed times are excluded while zero survives`() {
        val value = WeatherMapper.mapToDomain(OpenMeteoResponse(40.0, 29.0, hourly = HourlyDto(
            time = listOf("2026-11-12T10:00", "2026-11-12T11:99", "2026-11-12T12:00"),
            weatherCode = listOf(0, 0, 0), temperature = listOf(Double.NaN, 10.0, 0.0),
            precipitationProbability = listOf(0, 0, 101)
        )), "Bursa")
        val hour = value.hourlyForecast.single()
        assertEquals("0°", hour.temp)
        assertNull(hour.precipitationProbability)
        assertNull(hour.precipProb)
    }
    @Test fun `invalid date cannot be normalized into another day`() {
        val value = WeatherMapper.mapToDomain(OpenMeteoResponse(40.0, 29.0, daily = DailyDto(
            time = listOf("2026-02-30", "2026-11-12"), weatherCode = listOf(0, 0),
            tempMin = listOf(0.0, 0.0), tempMax = listOf(5.0, 5.0), precipProbMax = listOf(0, -1)
        )), "Bursa")
        assertEquals("2026-11-12", value.dailyForecast.single().date)
        assertNull(value.dailyForecast.single().precipitationProbability)
    }
    @Test fun `missing direction and temperature do not invent north or wind chill`() {
        val value = WeatherMapper.mapToDomain(OpenMeteoResponse(40.0, 29.0,
            current = CurrentWeatherDto(windSpeed = 20.0)), "Bursa")
        assertNull(value.windDirectionLabel)
        assertNull(value.windDirectionDegrees)
        assertNull(value.windChill)
    }
    @Test fun `travel summary never replaces missing or invalid maximum with twenty`() {
        val today = LocalDate.now()
        val plan = TravelPlan(city = "Bursa", startDate = today, endDate = today.plusDays(1))
        listOf(null, Double.NaN, Double.POSITIVE_INFINITY).forEach { max ->
            val text = RecommendationEngine.generateTravelRecommendation(plan, ForecastSnapshot(maxTemp = max))
            assertTrue(text, text.contains("verisi bulunmuyor"))
            assertFalse(text.contains("20°"))
        }
        val valid = RecommendationEngine.generateTravelRecommendation(plan, ForecastSnapshot(maxTemp = 0.0))
        assertTrue(valid, valid.contains("0°"))
    }
}
