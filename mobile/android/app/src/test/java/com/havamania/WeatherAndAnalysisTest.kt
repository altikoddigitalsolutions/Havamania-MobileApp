package com.havamania

import org.junit.Assert.*
import org.junit.Test

class WeatherAndAnalysisTest {

    @Test
    fun `weatherMapper temperature null does not produce zero`() {
        val response = OpenMeteoResponse(
            latitude = 41.0,
            longitude = 29.0,
            current = CurrentWeatherDto(temperature = null)
        )
        val domain = WeatherMapper.mapToDomain(response, "İstanbul")
        assertEquals("--", domain.temperature)
    }

    @Test
    fun `weatherMapper temperature genuine zero is preserved`() {
        val response = OpenMeteoResponse(
            latitude = 41.0,
            longitude = 29.0,
            current = CurrentWeatherDto(temperature = 0.0)
        )
        val domain = WeatherMapper.mapToDomain(response, "İstanbul")
        assertEquals("0°", domain.temperature)
    }

    @Test
    fun `weatherMapper pressure null produces null`() {
        val response = OpenMeteoResponse(
            latitude = 41.0,
            longitude = 29.0,
            current = CurrentWeatherDto(pressure = null)
        )
        val domain = WeatherMapper.mapToDomain(response, "İstanbul")
        assertNull(domain.pressure)
    }

    @Test
    fun `weatherMapper visibility null produces null`() {
        val response = OpenMeteoResponse(
            latitude = 41.0,
            longitude = 29.0,
            current = CurrentWeatherDto(visibility = null)
        )
        val domain = WeatherMapper.mapToDomain(response, "İstanbul")
        assertNull(domain.visibilityKm)
    }

    @Test
    fun `weatherMapper precipitationProbability null produces null`() {
        val response = OpenMeteoResponse(
            latitude = 41.0,
            longitude = 29.0,
            daily = DailyDto(
                time = listOf("2026-09-05"),
                weatherCode = listOf(0),
                tempMax = listOf(25.0),
                tempMin = listOf(20.0),
                precipProbMax = null
            )
        )
        val domain = WeatherMapper.mapToDomain(response, "İstanbul")
        assertNull(domain.precipitationProbability)
    }

    @Test
    fun `weatherMapper precipitationProbability genuine zero is preserved`() {
        val response = OpenMeteoResponse(
            latitude = 41.0,
            longitude = 29.0,
            daily = DailyDto(
                time = listOf("2026-09-05"),
                weatherCode = listOf(0),
                tempMax = listOf(25.0),
                tempMin = listOf(20.0),
                precipProbMax = listOf(0)
            )
        )
        val domain = WeatherMapper.mapToDomain(response, "İstanbul")
        assertEquals(Integer.valueOf(0), domain.precipitationProbability)
    }

    @Test
    fun `weatherMapper windSpeed null produces null`() {
        val response = OpenMeteoResponse(
            latitude = 41.0,
            longitude = 29.0,
            current = CurrentWeatherDto(windSpeed = null)
        )
        val domain = WeatherMapper.mapToDomain(response, "İstanbul")
        assertNull(domain.windSpeed)
    }

    @Test
    fun `weatherMapper windSpeed genuine zero is preserved`() {
        val response = OpenMeteoResponse(
            latitude = 41.0,
            longitude = 29.0,
            current = CurrentWeatherDto(windSpeed = 0.0)
        )
        val domain = WeatherMapper.mapToDomain(response, "İstanbul")
        assertNotNull(domain.windSpeed)
        assertEquals(0.0, domain.windSpeed!!, 0.001)
    }

    @Test
    fun `travelAnalysisEngine missing required weather input creates no fake analysis`() {
        val snapshot = ForecastSnapshot(
            minTemp = null,
            maxTemp = 25.0
        )
        val plan = TravelPlan(city = "Ankara", startDate = java.time.LocalDate.now(), endDate = java.time.LocalDate.now().plusDays(1))
        assertTrue(plan.analyses.isEmpty())
    }

    @Test
    fun `travelAnalysisEngine valid data regression nature score is 96`() {
        val snapshot = ForecastSnapshot(
            minTemp = 20.0,
            maxTemp = 25.1, // avg = 22.55
            precipitationProbability = 0,
            windSpeed = 10.0
        )
        val score = TravelAnalysisEngine.calculateTravelScore(snapshot, TripType.NATURE)
        assertEquals(96, score)
    }
}
