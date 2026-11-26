package com.havamania
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
class PackingDestinationClockTest {
 private val now=Instant.parse("2026-11-13T22:30:00Z")
 private fun plan(city:String="Bursa",end:String="2026-11-15",zone:String="Europe/Istanbul",stamp:Long=now.minusSeconds(60).toEpochMilli()) : TravelPlan {
  val plan=TravelPlan(city=city,startDate=LocalDate.parse("2026-11-13"),endDate=LocalDate.parse(end))
  val start=maxOf(plan.startDate,now.atZone(java.time.ZoneId.of(zone)).toLocalDate())
  return plan.copy(lastForecastSnapshot=ForecastSnapshot(timezone=zone,generatedAt=stamp,minTemp=10.0,maxTemp=20.0,windSpeed=5.0,precipitationProbability=10,binding=createTripForecastBinding(plan,start,plan.endDate)))
 }

 private fun reply(plans:List<TravelPlan>,weather:WeatherData?=WeatherData(timezone="America/Los_Angeles"))=RecommendationEngine.generateAssistantFallbackReply("Valizime ne almalıyım?",weather,travelPlans=plans,now=now)
 @Test fun eachDestinationDateControlsPackingPlanSelection() {
  val text=reply(listOf(plan("Ankara",end="2026-11-13",zone="Asia/Tokyo"),plan(zone="Asia/Tokyo")))
  assertTrue(text,text.contains("Bursa")); assertFalse(text.contains("Ankara"))
 }
 @Test fun currentCityTimezoneIsNotRequiredForDestinationPacking() {
  val text=reply(listOf(plan()),WeatherData(timezone="bad/zone"))
  assertTrue(text,text.contains("Bursa")); assertTrue(text,text.contains("valiz",true))
 }
 @Test fun currentCityWeatherIsNotRequiredForDestinationPacking() {
  val text=reply(listOf(plan()),null); assertTrue(text,text.contains("Bursa")); assertTrue(text,text.contains("valiz",true))
 }
 @Test fun unknownAnalysisTimeIsExplicit() {
  val text=reply(listOf(plan(stamp=0)))
  assertTrue(text,text.contains("Analiz zamanı bilinmiyor")); assertTrue(text,text.contains("güncelliği doğrulanamadı"))
 }
 @Test fun olderLocalDayIsExplicit() {
  val text=reply(listOf(plan(stamp=now.minusSeconds(86400).toEpochMilli())))
  assertTrue(text,text.contains("Önceki güne ait analiz")); assertTrue(text,text.contains("güncelliği doğrulanamadı"))
 }
 @Test fun futureTimestampCannotAppearCurrent() {
  val text=reply(listOf(plan(stamp=now.plusSeconds(60).toEpochMilli())))
  assertTrue(text,text.contains("ilerisinde")); assertTrue(text,text.contains("güncelliği doğrulanamadı"))
 }
 @Test fun validLocalAnalysisTimeIsShown() {
  val text=reply(listOf(plan(stamp=Instant.parse("2026-11-13T22:00:00Z").toEpochMilli())))
  assertTrue(text,text.contains("14 Kasım 2026 01:00")); assertFalse(text.contains("güncelliği doğrulanamadı"))
 }
}
