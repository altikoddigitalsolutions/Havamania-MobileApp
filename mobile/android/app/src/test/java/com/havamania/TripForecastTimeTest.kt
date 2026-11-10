package com.havamania

import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class TripForecastTimeTest {
    private val now = Instant.parse("2026-10-27T15:10:00Z")

    @Test fun sameInstantHasDifferentDatesAtForecastLocations() {
        assertEquals(LocalDate.of(2026, 10, 28), tripForecastDate("Asia/Tokyo", now))
        assertEquals(LocalDate.of(2026, 10, 27), tripForecastDate("America/Los_Angeles", now))
        assertNull(validTripForecastZone("Invalid/Zone"))
    }

    @Test fun remainingForecastDaysUseDestinationDateAndStoreMetadata() {
        val tripStart = LocalDate.of(2026, 10, 27)
        val end = tripStart.plusDays(1)
        val daily = DailyDto(time = listOf("2026-10-28"), weatherCode = listOf(3), tempMin = listOf(10.0),
            tempMax = listOf(20.0), precipProbMax = listOf(10), windSpeedMax = listOf(10.0))
        assertNull(buildTripForecastSnapshot(daily, tripStart, end))
        val snapshot = buildTripForecastSnapshot(daily, maxOf(tripStart, tripForecastDate("Asia/Tokyo", now)), end,
            "Asia/Tokyo", now)!!
        assertEquals("Asia/Tokyo", snapshot.timezone)
        assertEquals(now.toEpochMilli(), snapshot.generatedAt)
        assertNull(buildTripForecastSnapshot(daily, end, end, "Invalid/Zone", now))
    }

    @Test fun previousDayLabelUsesForecastZoneRatherThanDeviceDate() {
        val generated = now.minusSeconds(20 * 60)
        val snapshot = ForecastSnapshot(generatedAt = generated.toEpochMilli(), timezone = "Asia/Tokyo")
        assertTrue(tripForecastTimeLabel(snapshot, now).contains("Önceki güne ait analiz"))
        assertTrue(tripForecastTimeLabel(snapshot, now).contains("27 Ekim 2026 23:50 (Asia/Tokyo)"))
        assertFalse(tripForecastTimeLabel(snapshot.copy(timezone = "America/Los_Angeles"), now).contains("Önceki güne ait"))
    }

    @Test fun legacyUnknownAndFutureTimestampsDoNotMasqueradeAsCurrentAnalysis() {
        val legacy = Json.decodeFromString(ForecastSnapshot.serializer(), """{"minTemp":10.0}""")
        assertEquals(0L, legacy.generatedAt)
        assertEquals("Analiz zamanı bilinmiyor.", tripForecastTimeLabel(legacy, now))
        val future = ForecastSnapshot(generatedAt = now.plusSeconds(3600).toEpochMilli(), timezone = "Asia/Tokyo")
        assertTrue(tripForecastTimeLabel(future, now).contains("cihaz saatinin ilerisinde"))
        assertTrue(tripForecastTimeLabel(future.copy(timezone = null), now).contains("Konum saat dilimi bilinmiyor"))
    }
}
