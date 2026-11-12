package com.havamania

import org.junit.Assert.*
import org.junit.Test

class WeatherMissingDetailsTest {
    private fun details(current: CurrentWeatherDto? = null, daily: DailyDto? = null) =
        WeatherMapper.mapToDomain(OpenMeteoResponse(40.2, 29.1, current = current, daily = daily), "Bursa", now = java.time.Instant.parse("2026-11-12T12:00:00Z"))
            .details.associateBy { it.title }

    @Test fun `missing measurements do not create observations or low risk descriptions`() {
        val rows = details()
        assertEquals(11, rows.size)
        rows.values.forEach { assertEquals(it.title, "Veri yok", it.value) }
        assertEquals("Veri yok", rows.getValue("UV İndeksi").description)
        assertEquals("Veri yok", rows.getValue("Görüş").description)
        assertNull(rows.getValue("UV İndeksi").progress)
    }

    @Test fun `genuine zero measurements remain available`() {
        val rows = details(CurrentWeatherDto(apparentTemperature = 0.0, humidity = 0,
            windSpeed = 0.0, windGusts = 0.0, precipitation = 0.0, cloudCover = 0,
            visibility = 0.0, uvIndex = 0.0))
        assertEquals("0°", rows.getValue("Hissedilen").value)
        assertEquals("0 km/sa", rows.getValue("Rüzgar").value)
        assertEquals("0.0 mm", rows.getValue("Yağış Miktarı").value)
        assertEquals("0", rows.getValue("UV İndeksi").value)
        assertEquals("0 km", rows.getValue("Görüş").value)
        assertEquals(0f, rows.getValue("UV İndeksi").progress)
    }

    @Test fun `invalid measurements are unavailable without crashes`() {
        val rows = details(CurrentWeatherDto(apparentTemperature = Double.NaN, humidity = 101,
            windSpeed = -1.0, windGusts = Double.POSITIVE_INFINITY, precipitation = -2.0,
            cloudCover = -1, visibility = -1.0, pressure = 0.0, uvIndex = -1.0))
        rows.values.forEach { assertEquals(it.title, "Veri yok", it.value) }
    }

    @Test fun `daily UV fallback remains available and progress is bounded`() {
        val rows = details(daily = DailyDto(time = listOf("2026-11-12"), weatherCode = emptyList(),
            tempMax = emptyList(), tempMin = emptyList(), uvIndexMax = listOf(15.0)))
        assertEquals("15", rows.getValue("UV İndeksi").value)
        assertEquals(1f, rows.getValue("UV İndeksi").progress)
    }

    @Test fun `wind chill needs both temperature and wind speed`() {
        assertEquals("Veri yok", details(CurrentWeatherDto(windSpeed = 20.0)).getValue("Rüzgar Soğutma").value)
        assertEquals("Düşük", details(CurrentWeatherDto(temperature = 20.0, windSpeed = 20.0)).getValue("Rüzgar Soğutma").value)
        assertNotEquals("Veri yok", details(CurrentWeatherDto(temperature = 0.0, windSpeed = 20.0)).getValue("Rüzgar Soğutma").value)
    }
}
