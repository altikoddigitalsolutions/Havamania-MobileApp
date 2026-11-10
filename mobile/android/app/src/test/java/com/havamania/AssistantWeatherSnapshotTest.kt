package com.havamania

import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class AssistantWeatherSnapshotTest {
    private val now = Instant.parse("2026-10-30T11:30:00Z").toEpochMilli()
    private fun weather() = WeatherData(cityName = "Ankara", latitude = 39.9, longitude = 32.8,
        timezone = "Europe/Istanbul", timestamp = now, temperature = "18°", humidity = 60,
        dailyForecast = listOf(DailyForecast("Fri", "2026-10-30",10,19), DailyForecast("Sat", "2026-10-31",9,17)))
    @Test fun schemaCarriesOriginalFetchTimeAndNeverCoordinatesOrInventedSource() {
        val data = assistantClientContext("Ankara", weather(), emptyList(), false, now).weather_data!!
        assertEquals(now, OffsetDateTime.parse(data.observed_at).toInstant().toEpochMilli())
        assertEquals("Europe/Istanbul", data.timezone)
        assertEquals(18.0, data.current!!.temp_c!!, 0.01)
        val text = Json.encodeToString(data)
        assertFalse(text.contains("39.9")); assertFalse(text.contains("latitude")); assertFalse(text.contains("MGM"))
    }
    @Test fun staleDataIsMarkedButMissingTimeWrongCityAndUnknownZoneAreOmitted() {
        assertTrue(assistantWeatherSnapshot("Ankara", weather().copy(isStale = true), now)!!.stale)
        assertTrue(assistantWeatherSnapshot("Ankara", weather(), now+4*60*60*1000L)!!.stale)
        assertTrue(assistantWeatherSnapshot("Ankara", weather().copy(timestamp = now+11*60*1000L), now)!!.stale)
        assertNull(assistantWeatherSnapshot("İzmir", weather(), now))
        assertNull(assistantWeatherSnapshot("Ankara", weather().copy(timezone="Unknown/City"), now))
        assertNull(assistantWeatherSnapshot("Ankara", weather().copy(timestamp=0), now))
    }
    @Test fun forecastCannotContainGapsDuplicatesOrDatesOutsideTenDayWindow() {
        val rows = listOf(DailyForecast("Fri","2026-10-30",10,19), DailyForecast("Sun","2026-11-01",9,17))
        assertEquals(listOf("2026-10-30"), assistantWeatherSnapshot("Ankara",weather().copy(dailyForecast=rows),now)!!.forecast.map { it.date })
        val duplicate = rows + rows.first()
        val snapshot = assistantWeatherSnapshot("Ankara",weather().copy(dailyForecast=duplicate),now)!!
        assertEquals(listOf("2026-11-01"), snapshot.forecast.map { it.date })
        assertTrue(assistantWeatherSnapshot("Ankara",weather().copy(dailyForecast=listOf(DailyForecast("Future","2026-12-01",1,2))),now)!!.forecast.isEmpty())
    }
    @Test fun platformErrorsRemainDistinctErrorStates() {
        assertEquals(AssistantResult.AccountDeleted, assistantHttpFailure(409,"""{"error":"user_deleted"}"""))
        assertEquals(AssistantResult.RequestInProgress, assistantHttpFailure(409,"""{"error":"request_in_progress"}"""))
        assertEquals(AssistantResult.RequestConflict, assistantHttpFailure(422,"""{"error":"request_conflict"}"""))
    }
}
