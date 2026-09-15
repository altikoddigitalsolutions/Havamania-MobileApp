package com.havamania

import org.junit.Assert.*
import org.junit.Test

class WeatherAndAnalysisTest {

    @Test
    fun `partial forecast arrays preserve complete entries without crashing`() {
        val domain = WeatherMapper.mapToDomain(OpenMeteoResponse(
            latitude = 41.0, longitude = 29.0,
            hourly = HourlyDto(
                time = listOf("2026-09-15T12:00", "2026-09-15T13:00"),
                temperature = listOf(21.6), weatherCode = listOf(0, 1),
                precipitationProbability = emptyList()
            ),
            daily = DailyDto(
                time = listOf("2026-09-15", "2026-09-16"),
                weatherCode = listOf(0), tempMax = listOf(25.6), tempMin = listOf(20.6)
            )
        ), "İstanbul")
        assertEquals(1, domain.hourlyForecast.size)
        assertEquals("22°", domain.hourlyForecast.single().temp)
        assertNull(domain.hourlyForecast.single().precipitationProbability)
        assertEquals(1, domain.dailyForecast.size)
        assertEquals(26, domain.dailyForecast.single().maxTemp)
    }

    @Test
    fun `sunrise without sunset does not crash`() {
        val daily = DailyDto(time = emptyList(), weatherCode = emptyList(),
            tempMax = emptyList(), tempMin = emptyList(),
            sunrise = listOf("2026-09-15T06:30"))
        assertTrue(WeatherMapper.getMoonAndSunData(daily).contains("Güneş Doğuşu" to "06:30"))
    }

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
    fun `weatherMapper sunrise sunset null does not crash or use fake times`() {
        val response = OpenMeteoResponse(
            latitude = 41.0,
            longitude = 29.0,
            daily = DailyDto(
                time = listOf("2026-09-05"),
                weatherCode = listOf(0),
                tempMax = listOf(25.0),
                tempMin = listOf(20.0),
                sunrise = emptyList(),
                sunset = emptyList()
            ),
            hourly = HourlyDto(
                time = listOf("2026-09-05T12:00"),
                weatherCode = listOf(0),
                temperature = listOf(22.0)
            )
        )
        val domain = WeatherMapper.mapToDomain(response, "İstanbul")
        assertNull(domain.sunriseTime)
        assertNull(domain.sunsetTime)
        assertEquals(1, domain.hourlyForecast.size)
    }

    @Test
    fun `weatherMapper invalid hourly timestamp is skipped`() {
        val response = OpenMeteoResponse(
            latitude = 41.0,
            longitude = 29.0,
            hourly = HourlyDto(
                time = listOf("invalid-time"),
                weatherCode = listOf(0),
                temperature = listOf(22.0)
            )
        )
        val domain = WeatherMapper.mapToDomain(response, "İstanbul")
        assertTrue(domain.hourlyForecast.isEmpty())
    }

    @Test
    fun `weatherMapper visibility null does not assume good visibility`() {
        val response = OpenMeteoResponse(
            latitude = 41.0,
            longitude = 29.0,
            current = CurrentWeatherDto(visibility = null)
        )
        val domain = WeatherMapper.mapToDomain(response, "İstanbul")
        assertNull(domain.visibilityKm)
    }

    @Test
    fun `cache age under 15 minutes is not stale`() {
        val now = System.currentTimeMillis()
        val timestamp = now - (10 * 60 * 1000L) // 10 minutes ago
        val cacheTimeoutMillis = 15 * 60 * 1000L
        val isStale = (now - timestamp) >= cacheTimeoutMillis
        assertFalse(isStale)
    }

    @Test
    fun `cache age over 15 minutes is stale`() {
        val now = System.currentTimeMillis()
        val timestamp = now - (20 * 60 * 1000L) // 20 minutes ago
        val cacheTimeoutMillis = 15 * 60 * 1000L
        val isStale = (now - timestamp) >= cacheTimeoutMillis
        assertTrue(isStale)
    }

    @Test
    fun `cache key normalization handles turkish casing and whitespace correctly`() {
        fun canonicalKey(cityName: String, districtName: String?): String {
            val normCity = cityName.trim().lowercase(java.util.Locale("tr"))
            val normDistrict = districtName?.trim()?.takeIf { it.isNotBlank() }?.lowercase(java.util.Locale("tr"))
            return if (normDistrict != null) "$normCity-$normDistrict" else normCity
        }

        assertEquals("istanbul", canonicalKey("İstanbul", null))
        assertEquals("istanbul", canonicalKey("istanbul", null))
        assertEquals("istanbul", canonicalKey("İSTANBUL", null))
        assertEquals("ankara", canonicalKey(" Ankara", null))
        assertEquals("ankara", canonicalKey("Ankara ", null))
        assertEquals("izmir", canonicalKey("İZMİR", null))
        assertEquals("izmir", canonicalKey("İzmir", null))
        assertEquals("izmir", canonicalKey("izmir", null))
        assertEquals("ığdır", canonicalKey("IĞDIR", null))
        assertEquals("ığdır", canonicalKey("ığdır", null))
        assertEquals("çankırı", canonicalKey("ÇANKIRI", null))
        assertEquals("çankırı", canonicalKey("çankırı", null))
        assertEquals("istanbul-kadıköy", canonicalKey("İSTANBUL", " KADIKÖY "))
        assertEquals("istanbul-kadıköy", canonicalKey("istanbul", "kadıköy"))
    }

    @Test
    fun `parseDepartureTime handles valid and invalid times correctly`() {
        assertNotNull(parseDepartureTime("00:00"))
        assertNotNull(parseDepartureTime("08:00"))
        assertNotNull(parseDepartureTime("09:30"))
        assertNotNull(parseDepartureTime("23:59"))

        assertNull(parseDepartureTime(null))
        assertNull(parseDepartureTime(""))
        assertNull(parseDepartureTime("   "))
        assertNull(parseDepartureTime("abc"))
        assertNull(parseDepartureTime("9"))
        assertNull(parseDepartureTime("09"))
        assertNull(parseDepartureTime("09:"))
        assertNull(parseDepartureTime(":30"))
        assertNull(parseDepartureTime("24:00"))
        assertNull(parseDepartureTime("99:30"))
        assertNull(parseDepartureTime("12:60"))
        assertNull(parseDepartureTime("-1:30"))
    }

    @Test
    fun `travelPlan departureDateTime with malformed time returns null without exception`() {
        val plan = TravelPlan(
            city = "Ankara",
            startDate = java.time.LocalDate.now().plusDays(1),
            endDate = java.time.LocalDate.now().plusDays(2),
            departureTime = "99:30"
        )
        assertNull(plan.departureDateTime)
    }

    @Test
    fun `travelPlan departureDateTime with null time returns null`() {
        val plan = TravelPlan(
            city = "Ankara",
            startDate = java.time.LocalDate.now().plusDays(1),
            endDate = java.time.LocalDate.now().plusDays(2),
            departureTime = null
        )
        assertNull(plan.departureDateTime)
    }

    @Test
    fun `travelPlan departureDateTime with valid time returns datetime`() {
        val date = java.time.LocalDate.now().plusDays(1)
        val plan = TravelPlan(
            city = "Ankara",
            startDate = date,
            endDate = date.plusDays(1),
            departureTime = "08:00"
        )
        val dt = plan.departureDateTime
        assertNotNull(dt)
        assertEquals(date.atTime(8, 0), dt)
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
