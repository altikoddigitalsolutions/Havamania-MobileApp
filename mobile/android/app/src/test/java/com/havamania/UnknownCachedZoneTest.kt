package com.havamania
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
class UnknownCachedZoneTest {
 private val now=Instant.parse("2026-11-26T12:30:00Z")
 private fun data(zone:String)=WeatherData(timezone=zone,timestamp=now.minusSeconds(60).toEpochMilli(),mappingVersion=1,high="20°",low="10°",weatherSuitabilityScore=100,dailyForecast=listOf(DailyForecast("Today","2026-11-26",10,20,isToday=true)),hourlyForecast=listOf(HourlyWeather("12:00","2026-11-26T12:00","15°",isSelected=true)))
 @Test fun unknownZoneCannotValidateCachedLocalDay() {
  for(zone in listOf("","bad/zone")) {
   val value=data(zone)
   assertFalse(cachedWeatherSameLocalDay(value,now))
   val shown=presentCachedWeather(value,now)
   assertTrue(shown.isStale); assertEquals(-1,shown.weatherSuitabilityScore); assertEquals("--",shown.high)
  }
 }
 @Test fun unknownZoneCannotRecreateTodayOrCurrentHours() {
  val value=data("")
  val days=ForecastTime.dailyFromToday(value,now)
  assertEquals("2026-11-26",days.single().date); assertFalse(days.single().isToday)
  assertTrue(ForecastTime.hoursForDate(value,LocalDate.parse("2026-11-26"),now).isEmpty())
 }
 @Test fun cachedSelectionFlagsAreRemovedWithoutZone() {
  val value=normalizeCachedForecastLists(data("bad/zone"))
  assertFalse(value.dailyForecast.single().isToday)
  assertFalse(value.hourlyForecast.single().isSelected)
 }
 @Test fun explicitUtcStillValidatesFreshLocalDay() {
  assertTrue(cachedWeatherSameLocalDay(data("UTC"),now))
  assertFalse(presentCachedWeather(data("UTC"),now).isStale)
 }
}
