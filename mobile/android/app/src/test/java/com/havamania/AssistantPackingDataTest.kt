package com.havamania
import java.time.Instant
import java.time.LocalDate
import com.havamania.ui.theme.AssistantTone
import org.junit.Assert.*
import org.junit.Test
class AssistantPackingDataTest {
 private val day=LocalDate.of(2026,11,27)
 private fun reply(snapshot:ForecastSnapshot?,tone:AssistantTone=AssistantTone.DENGELI):String {
  val now=Instant.parse("2026-11-26T12:00:00Z")
  val plan=TravelPlan(city="Ankara",startDate=day,endDate=day.plusDays(2))
  val bound=plan.copy(lastForecastSnapshot=snapshot?.copy(timezone="Europe/Istanbul",generatedAt=now.toEpochMilli(),binding=createTripForecastBinding(plan,day,day.plusDays(2))))
  return RecommendationEngine.generateAssistantFallbackReply("Valizime ne almalıyım?",WeatherData(cityName="Bursa",timezone="Europe/Istanbul"),tone=tone,travelPlans=listOf(bound),now=now)
 }

 @Test fun noSnapshotDoesNotInventWeatherInAnyTone() {
  for(tone in AssistantTone.entries) {
   val text=reply(null,tone)
   assertTrue(text,text.contains("eksik")); assertFalse(text.contains("20°")); assertFalse(text.contains("15°")); assertFalse(text.contains("%0"))
  }
 }
 @Test fun invalidValuesStayUnknown() {
  val text=reply(ForecastSnapshot(minTemp=Double.NEGATIVE_INFINITY,maxTemp=Double.NaN,precipitationProbability=101,windSpeed=Double.POSITIVE_INFINITY))
  assertTrue(text,text.contains("eksik")); listOf("NaN","Infinity","%101","2147483647").forEach { assertFalse(text,text.contains(it)) }
 }
 @Test fun rainCodeWithoutChanceStillNeedsRainGear() {
  val text=reply(ForecastSnapshot(weatherCode=61))
  assertTrue(text,text.contains("yağmurluk")); assertTrue(text,text.contains("eksik")); assertFalse(text.contains("%0"))
 }
 @Test fun heatAloneCannotInventUvAdvice() {
  val text=reply(ForecastSnapshot(maxTemp=35.0))
  assertTrue(text,text.contains("hafif",true)); assertFalse(text.contains("güneş kremi",true)); assertFalse(text.contains("şapka",true))
 }
 @Test fun concurrentHazardsUseSharedPackingAdvice() {
  val snapshot=ForecastSnapshot(minTemp=-2.0,maxTemp=10.0,precipitationProbability=80,windSpeed=50.0,uvIndex=8.0,weatherCode=71)
  val text=reply(snapshot)
  assertTrue(text,text.contains(travelWeatherPacking(snapshot,null)))
  listOf("kaymayan","Rüzgara","UV").forEach { assertTrue(text,text.contains(it)) }
 }
 @Test fun periodMinimumIsNotClaimedAsNightTemperature() {
  val text=reply(ForecastSnapshot(minTemp=7.0,maxTemp=20.0,precipitationProbability=10,windSpeed=5.0))
  assertTrue(text,text.contains("en düşük sıcaklık")); assertFalse(text.contains("gece",true)); assertFalse(text.contains("akşam",true))
 }
 @Test fun realZeroMeasurementsArePreserved() {
  val text=reply(ForecastSnapshot(minTemp=0.0,maxTemp=0.0,precipitationProbability=0,windSpeed=0.0))
  assertTrue(text,text.contains("%0")); assertTrue(text,text.contains("0,0°C")); assertFalse(text.contains("eksik"))
 }
}
