package com.havamania

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class RouteForecastWireTest {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val payload = """{"timezone":"UTC","hourly":{"time":["2026-11-25T12:00","2026-11-25T13:00"],"temperature_2m":[20,null],"weather_code":[0,95],"precipitation_probability":[0,10],"wind_speed_10m":[5,80],"wind_gusts_10m":[8,90],"is_day":[1,null]}}"""
    @Test fun `null temperature JSON retains storm wind and neighboring sample`() {
        val data = json.decodeFromString(RouteForecastResponse.serializer(), payload).hourly!!
        val first = data.routeWeatherSample(0)!!; val second = data.routeWeatherSample(1)!!
        assertEquals(RouteRisk.OK, first.risk); assertEquals(RouteRisk.DANGER, second.risk)
        assertNull(second.temperatureC); assertTrue(second.hasIncompleteData)
        assertTrue(second.riskReason.orEmpty().contains("fırtına")); assertTrue(second.riskReason.orEmpty().contains("rüzgar"))
    }
    @Test fun `batch JSON null entries do not reject another response`() {
        val response = json.decodeFromString(ListSerializer(RouteForecastResponse.serializer()), "[$payload,$payload]")
        assertEquals(2, response.size)
        response.forEach { assertNotNull(it.hourly!!.routeWeatherSample(0)); assertEquals(RouteRisk.DANGER, it.hourly!!.routeWeatherSample(1)!!.risk) }
    }
}