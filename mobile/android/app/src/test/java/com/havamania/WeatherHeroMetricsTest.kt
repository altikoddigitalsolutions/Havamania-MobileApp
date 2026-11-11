package com.havamania

import org.junit.Assert.*
import org.junit.Test

class WeatherHeroMetricsTest {
    @Test fun windSpeedUsesItsNumericFieldRatherThanChillOrStaleDisplayText() {
        val data = WeatherData(humidity = 64, windSpeed = 12.6, uvIndex = 3, details = listOf(
            WeatherDetailData("Rüzgar Soğutma", "-2°", ""),
            WeatherDetailData("Rüzgar", "99 km/sa", ""), WeatherDetailData("Nem", "%99", "")))
        assertEquals(WeatherHeroMetrics("%64", "13 km/sa", "3"), weatherHeroMetrics(data))
    }
    @Test fun legacyDetailsSelectExactSpeedAndHumidityWithoutMistakingOtherWindMetrics() {
        val data = WeatherData(details = listOf(WeatherDetailData("Rüzgar Soğutma", "Düşük", ""),
            WeatherDetailData("Rüzgar Hamlesi", "45 km/sa", ""),
            WeatherDetailData("Rüzgâr", "17 km/sa", ""), WeatherDetailData("Nem", "%70", "")))
        assertEquals(WeatherHeroMetrics("%70", "17 km/sa", "Veri yok"), weatherHeroMetrics(data))
    }
    @Test fun missingMeasurementsAreNotReplacedWithInventedNumbers() {
        assertEquals(WeatherHeroMetrics("Veri yok", "Veri yok", "Veri yok"), weatherHeroMetrics(WeatherData()))
        assertEquals("Veri yok", weatherHeroMetrics(WeatherData(details = listOf(
            WeatherDetailData("Rüzgar Soğutma", "Düşük", "")))).wind)
    }
    @Test fun zeroMeasurementsAreRealValuesAndInvalidMeasurementsAreUnavailable() {
        assertEquals(WeatherHeroMetrics("%0", "0 km/sa", "0"),
            weatherHeroMetrics(WeatherData(humidity = 0, windSpeed = 0.0, uvIndex = 0)))
        assertEquals(WeatherHeroMetrics("Veri yok", "Veri yok", "Veri yok"),
            weatherHeroMetrics(WeatherData(humidity = -1, windSpeed = Double.NaN, uvIndex = -1)))
    }
}
