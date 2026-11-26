package com.havamania
import java.time.Instant
import java.time.LocalDate
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test
class WeatherForecastSelectionTest {
 private val now=Instant.parse("2026-11-26T22:30:00Z")
 private val day=DailyForecast("Tomorrow","2026-11-27",10,20,weatherCode=0)
 @Test fun unknownZoneCannotAutomaticallyChooseDatedForecast() {
  for(zone in listOf("","bad/zone")) {
   val selection=initialWeatherForecastSelection(WeatherData(timezone=zone,dailyForecast=listOf(day)),now)
   assertNull(selection.date); assertNull(selection.day); assertNull(selection.hour)
  }
 }
 @Test fun locationDayWinsOverDeviceAndUtc() {
  val prior=TimeZone.getDefault()
  try {
   TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
   val hour=HourlyWeather("02:00","2026-11-27T02:00","12°")
   val selection=initialWeatherForecastSelection(WeatherData(timezone="Europe/Istanbul",dailyForecast=listOf(day),hourlyForecast=listOf(hour)),now)
   assertEquals(LocalDate.parse("2026-11-27"),selection.date); assertTrue(selection.day!!.isToday); assertEquals(hour,selection.hour)
  } finally { TimeZone.setDefault(prior) }
 }
 @Test fun noDailyRowsCanStillSelectKnownLocationHour() {
  val hour=HourlyWeather("23:00","2026-11-26T23:00","12°")
  val selection=initialWeatherForecastSelection(WeatherData(timezone="UTC",hourlyForecast=listOf(hour)),now)
  assertEquals(LocalDate.parse("2026-11-26"),selection.date); assertNull(selection.day); assertEquals(hour,selection.hour)
 }
 @Test fun dailyDescriptionDoesNotInheritCurrentNight() {
  assertEquals("Açık",weatherSelectionCondition(WeatherData(condition="Açık gece"),null,day))
 }
 @Test fun selectedHourUsesOwnPhaseAndCurrentUsesMappedDescription() {
  val data=WeatherData(condition="Güneşli")
  assertEquals("Güneşli",weatherSelectionCondition(data,null,null))
  val hour=HourlyWeather("23:00","2026-11-26T23:00","12°",weatherCode=0,isDay=false)
  assertEquals("Açık gece",weatherSelectionCondition(data,hour,day))
  assertEquals("Açık",weatherSelectionCondition(data,hour.copy(isDay=null),day))
 }
 @Test fun updateLabelCannotInventUtcForUnknownZone() {
  assertEquals("Saat dilimi doğrulanamadı",weatherUpdatedTimeLabel(WeatherData(timezone="",timestamp=now.toEpochMilli())))
  assertTrue(weatherUpdatedTimeLabel(WeatherData(timezone="Europe/Istanbul",timestamp=now.toEpochMilli())).contains("27 Kas 2026, 01:30"))
 }
}
