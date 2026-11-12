package com.havamania

import org.junit.Assert.*
import org.junit.Test

class WeatherSuitabilityCompletenessTest {
    private val daily = DailyDto(time = listOf("2026-11-12"), weatherCode = emptyList(),
        tempMax = emptyList(), tempMin = emptyList(), precipProbMax = listOf(0), uvIndexMax = listOf(1.0))
    private val current = CurrentWeatherDto(weatherCode = 0, temperature = 20.0, windSpeed = 5.0, visibility = 10000.0, uvIndex = 1.0)
    private fun map(value: CurrentWeatherDto?, days: DailyDto? = daily) =
        WeatherMapper.mapToDomain(OpenMeteoResponse(40.2, 29.1, current = value, daily = days), "Bursa", now = java.time.Instant.parse("2026-11-12T12:00:00Z"))

    @Test fun `missing observations never imply perfect suitability`() {
        listOf(map(null), map(CurrentWeatherDto()), map(current.copy(windSpeed = null)),
            map(current, daily.copy(precipProbMax = listOf(101)))).forEach {
            assertEquals(-1, it.weatherSuitabilityScore)
            assertFalse(it.weatherSuitabilityText.contains("uygun"))
            assertFalse(it.weatherSuitabilityDesc.contains("ideal"))
        }
    }
    @Test fun `known risks survive partial data`() {
        val value = map(current.copy(temperature = null, windSpeed = 40.0, uvIndex = 9.0))
        assertEquals(-1, value.weatherSuitabilityScore)
        assertTrue(value.weatherSuitabilityDesc.contains("rüzgar"))
        assertTrue(value.weatherSuitabilityDesc.contains("UV"))
    }
    @Test fun `complete mild observations retain suitability and zero values count`() {
        assertEquals(100, map(current).weatherSuitabilityScore)
        assertEquals(100, map(current.copy(temperature = 0.0, windSpeed = 0.0, uvIndex = 0.0)).weatherSuitabilityScore)
    }
    @Test fun `partial recommendation reports gaps without invented temperature or clothing`() {
        val reply = RecommendationEngine.generateTodayRecommendation(WeatherData(cityName = "Bursa", temperature = "--")).message
        assertTrue(reply.contains("eksik"))
        assertFalse(reply.contains("20°C"))
        assertFalse(reply.contains("elverişli"))
        assertFalse(reply.contains("GİYSİ"))
    }
    @Test fun `partial recommendation retains observed risk and stale record is explicit`() {
        val value = WeatherData(cityName = "Bursa", temperature = "--", windSpeed = 40.0, uvIndex = 9)
        val reply = RecommendationEngine.generateTodayRecommendation(value).message
        assertTrue(reply.contains("güçlü rüzgâr"))
        assertTrue(reply.contains("yüksek UV"))
        val stale = RecommendationEngine.generateTodayRecommendation(value.copy(isStale = true)).message
        assertTrue(stale.contains("güncel değil"))
    }
    @Test fun `cold temperature alone never claims fresh snow`() {
        val reply = RecommendationEngine.generateTodayRecommendation(WeatherData(cityName = "Bursa", temperature = "2°", weatherCode = 0,
            windSpeed = 5.0, precipitationProbability = 0, uvIndex = 1), userInterests = setOf("Kayak")).message
        assertFalse(reply.contains("taze kar"))
        assertTrue(reply.contains("kar kalınlığını"))
    }
}
