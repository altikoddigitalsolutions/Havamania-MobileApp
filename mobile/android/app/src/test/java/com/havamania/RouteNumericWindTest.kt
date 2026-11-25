package com.havamania

import java.lang.reflect.Proxy
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RouteNumericWindTest {
    private val eta = Instant.parse("2026-11-25T12:00:00Z").toEpochMilli()
    private fun response(temp: Double = 20.0, code: Int = 0, wind: Double? = 5.0, prob: Int? = 0) = RouteForecastResponse(41.0, 29.0, timezone = "UTC",
        hourly = RouteHourlyDto(time = listOf("2026-11-25T12:00"), temperature = listOf(temp), weatherCode = listOf(code),
            precipitationProbability = prob?.let { listOf(it) }, windSpeed = wind?.let { listOf(it) }))
    private fun provider(value: RouteForecastResponse) = RouteWeatherProvider(Proxy.newProxyInstance(WeatherApiService::class.java.classLoader,
        arrayOf(WeatherApiService::class.java)) { _, method, args ->
            assertTrue(args!![2].toString().contains("wind_gusts_10m")); assertEquals("kmh", args[5])
            if (method.name == "getBatchRouteHourly") listOf(value, value) else value } as WeatherApiService)
    @Test fun `invalid temperature is unknown while known storm survives`() = runBlocking {
        val weather = provider(response(temp = Double.NaN, code = 95)).weatherAt(GeoPoint(41.0, 29.0), eta)
        assertNotNull(weather); assertNull(weather!!.temperatureC); assertEquals(RouteRisk.DANGER, weather.risk)
    }
    @Test fun `invalid optional fields cannot reach UI`() = runBlocking {
        val value = response(wind = Double.POSITIVE_INFINITY, prob = 101)
        val weather = provider(value.copy(hourly = value.hourly!!.copy(apparentTemperature = listOf(Double.NaN), humidity = listOf(-1))))
            .weatherAt(GeoPoint(41.0, 29.0), eta)!!
        assertNull(weather.windSpeedKmh); assertNull(weather.precipProbability); assertNull(weather.apparentTempC); assertNull(weather.humidity)
    }
    @Test fun `unknown code cannot produce green risk`() = runBlocking {
        assertNotEquals(RouteRisk.OK, provider(response(code = -1)).weatherAt(GeoPoint(41.0, 29.0), eta)!!.risk)
    }
    @Test fun `missing wind cannot be classified fully safe`() = runBlocking {
        val weather = provider(response(wind = null)).weatherAt(GeoPoint(41.0, 29.0), eta)!!
        assertNotEquals(RouteRisk.OK, weather.risk); assertTrue(weather.riskReason.orEmpty().contains("eksik"))
    }
    @Test fun `strong wind cannot be hidden by clear sky`() = runBlocking {
        val weather = provider(response(wind = 80.0)).weatherAt(GeoPoint(41.0, 29.0), eta)!!
        assertEquals(RouteRisk.DANGER, weather.risk); assertTrue(weather.riskReason.orEmpty().contains("rüzgar", true))
    }
    @Test fun `snow and wind reasons both survive`() = runBlocking {
        val weather = provider(response(code = 71, wind = 80.0)).weatherAt(GeoPoint(41.0, 29.0), eta)!!
        assertTrue(weather.riskReason.orEmpty().contains("Kar")); assertTrue(weather.riskReason.orEmpty().contains("rüzgar", true))
    }
    @Test fun `batch uses same numeric validation as single sample`() = runBlocking {
        val weather = provider(response(temp = Double.NaN, code = 61)).weatherAtBatch(listOf(GeoPoint(41.0, 29.0), GeoPoint(42.0, 30.0)), listOf(eta, eta))
        assertEquals(2, weather.size); weather.forEach { assertNotNull(it); assertNull(it!!.temperatureC) }
    }

    @Test fun `gust can raise danger while mean wind is low`() = runBlocking {
        val value = response(wind = 5.0)
        val weather = provider(value.copy(hourly = value.hourly!!.copy(windGusts = listOf(80.0)))).weatherAt(GeoPoint(41.0, 29.0), eta)!!
        assertEquals(RouteRisk.DANGER, weather.risk); assertTrue(weather.riskReason.orEmpty().contains("hamlesi"))
    }
    @Test fun `entirely unusable risk data returns missing forecast`() {
        val hourly = response(Double.NaN, -1, Double.NaN, 101).hourly!!.copy(windGusts = listOf(-1.0))
        assertNull(hourly.routeWeatherSample(0))
    }
    @Test fun `complete calm sample and genuine zero values survive`() {
        val value = response(wind = 0.0).hourly!!.copy(windGusts = listOf(0.0))
        val calm = value.routeWeatherSample(0)!!
        assertEquals(RouteRisk.OK, calm.risk); assertFalse(calm.hasIncompleteData)
        val zero = value.copy(temperature = listOf(0.0)).routeWeatherSample(0)!!
        assertEquals(0.0, zero.temperatureC!!, 0.0); assertEquals("0°", routeTemperatureText(zero.temperatureC))
    }
    @Test fun `wind and gust precaution boundaries are explicit`() {
        listOf(38.9 to RouteRisk.OK, 39.0 to RouteRisk.CAUTION, 61.9 to RouteRisk.CAUTION, 62.0 to RouteRisk.DANGER).forEach { (speed, expected) ->
            assertEquals(expected, RouteRiskAssessor.assess(0, 0, 20.0, speed, 0.0).first)
            assertEquals(expected, RouteRiskAssessor.assess(0, 0, 20.0, 0.0, speed).first)
        }
    }
    @Test fun `incomplete metadata cannot produce a full safe route summary`() {
        val sample = WaypointWeather(0, 20.0, null, 0, 5.0, 50, RouteRisk.OK, null, windGustKmh = 8.0, hasIncompleteData = true)
        val summary = travelRouteForecastSummary(listOf(sample))!!
        assertTrue(summary.contains("eksik")); assertFalse(summary.contains("önemli bir hava riski görülmedi"))
    }
    @Test fun `missing response timezone cannot invent UTC sample`() = runBlocking {
        assertNull(provider(response().copy(timezone = null)).weatherAt(GeoPoint(41.0, 29.0), eta))
    }
}
