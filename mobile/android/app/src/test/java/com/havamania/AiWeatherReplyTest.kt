package com.havamania

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class AiWeatherReplyTest {
    private val today = LocalDate.parse("2026-10-22")
    private fun weather(date: String = "2026-10-23", code: Int = 0) = WeatherData(
        cityName = "İstanbul", timezone = "Europe/Istanbul",
        dailyForecast = listOf(DailyForecast("Day", date, 10, 20, weatherCode = code)))

    @Test fun tomorrowIsResolvedAtForecastLocationAcrossUtcMidnight() {
        val reply = AiWeatherReply.datedReply("yarın hava nasıl", weather("2026-10-24"), Instant.parse("2026-10-22T22:30:00Z"))!!
        assertTrue(reply.contains("24 Ekim 2026"))
        assertTrue(reply.contains("10°C / 20°C"))
    }

    @Test fun dailySummaryDoesNotChangeBetweenDayAndNight() {
        val data = weather()
        val day = AiWeatherReply.datedReply("23 Ekim 2026", data, Instant.parse("2026-10-22T10:00:00Z"))
        val night = AiWeatherReply.datedReply("23 Ekim 2026", data, Instant.parse("2026-10-22T20:00:00Z"))
        assertEquals(day, night)
        assertTrue(day!!.contains("günlük hava özeti: açık"))
        assertFalse(day.contains("Gece"))
    }

    @Test fun missingDayDoesNotSubstituteCurrentWeatherOrPromiseCoverage() {
        val reply = AiWeatherReply.datedReply("25 Ekim 2026", weather(), Instant.parse("2026-10-22T10:00:00Z"))!!
        assertTrue(reply.contains("verisi bulunmuyor"))
        assertFalse(reply.contains("10 gün"))
        assertFalse(reply.contains("°C"))
    }

    @Test fun staleForecastIsExplicitAndInvalidProbabilityIsOmitted() {
        val data = weather().let { it.copy(isStale = true, dailyForecast = it.dailyForecast.map { day -> day.copy(precipitationProbability = 140) }) }
        val reply = AiWeatherReply.datedReply("yarın", data, Instant.parse("2026-10-22T10:00:00Z"))!!
        assertTrue(reply.contains("eski önbellek"))
        assertFalse(reply.contains("%140"))
    }

    @Test fun dayAfterTomorrowAndYearBoundaryAreCorrect() {
        assertEquals(today.plusDays(2), AiIntentParser.detectDate("yarından sonra", today))
        assertEquals(LocalDate.parse("2027-01-01"), AiIntentParser.detectDate("yarın", LocalDate.parse("2026-12-31")))
    }

    @Test fun explicitYearIsPreservedIncludingPastDatesAndLeapDay() {
        assertEquals(LocalDate.parse("2025-10-23"), AiIntentParser.detectDate("23 Ekim 2025", today))
        assertEquals(LocalDate.parse("2028-02-29"), AiIntentParser.detectDate("29 Şubat 2028", today))
        assertNull(AiIntentParser.detectDate("29 Şubat 2027", today))
    }

    @Test fun severeAndUnknownCodesAreNotCalledSunny() {
        assertEquals("donan yağmurlu", AiWeatherReply.dailyCondition(67))
        assertEquals("dolu ve gök gürültülü fırtınalı", AiWeatherReply.dailyCondition(99))
        assertEquals("bilinmiyor", AiWeatherReply.dailyCondition(-1))
    }
}
