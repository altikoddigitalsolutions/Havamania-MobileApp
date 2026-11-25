package com.havamania
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test
class UnknownForecastZoneTest {
 @Test fun missingZoneCannotClaimTodayOrCurrentHour() {
  val response=OpenMeteoResponse(0.0,0.0,daily=DailyDto(time=listOf("2026-11-26"),weatherCode=listOf(0),tempMin=listOf(10.0),tempMax=listOf(20.0)),hourly=HourlyDto(time=listOf("2026-11-26T12:00"),temperature=listOf(15.0),weatherCode=listOf(0)))
  val data=WeatherMapper.mapToDomain(response,"Test",now=Instant.parse("2026-11-26T12:30:00Z"))
  assertFalse(data.dailyForecast.single().isToday)
  assertFalse(data.hourlyForecast.single().isSelected)
  assertEquals("",data.timezone)
 }
 @Test fun invalidZoneCannotClaimToday() {
  val response=OpenMeteoResponse(0.0,0.0,timezone="invalid/zone",daily=DailyDto(time=listOf("2026-11-26"),weatherCode=listOf(0),tempMin=listOf(10.0),tempMax=listOf(20.0)))
  assertFalse(WeatherMapper.mapToDomain(response,"Test",now=Instant.parse("2026-11-26T12:30:00Z")).dailyForecast.single().isToday)
 }
}
