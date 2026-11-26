package com.havamania
import java.time.Instant
import java.time.LocalDate
import com.havamania.ui.theme.AssistantTone
import org.junit.Assert.*
import org.junit.Test
class TravelRecommendationClockTest {
 private val now=Instant.parse("2026-11-13T22:30:00Z")
 private fun plan(city:String="Bursa",start:String="2026-11-13",end:String="2026-11-15",zone:String?="Europe/Istanbul")=TravelPlan(city=city,startDate=LocalDate.parse(start),endDate=LocalDate.parse(end),lastForecastSnapshot=ForecastSnapshot(timezone=zone,maxTemp=20.0,minTemp=10.0))
 private fun card(plan:TravelPlan=plan(),tone:AssistantTone=AssistantTone.DENGELI,snapshot:ForecastSnapshot?=plan.lastForecastSnapshot)=RecommendationEngine.generateTravelRecommendation(plan,snapshot,tone=tone,now=now)
 @Test fun unknownTargetZoneCannotClaimTripStatus() {
  val text=card(plan(zone=null)); assertTrue(text,text.contains("saat dilimi")); assertFalse(text.contains("yaklaşıyor"))
 }
 @Test fun targetLocalDayDefinesTripDay() {
  val text=card(); assertTrue(text,text.contains("2. gün")); assertTrue(text,text.contains("devam ediyor"))
 }
 @Test fun periodForecastIsNeverCurrentObservationInAnyTone() {
  for(tone in AssistantTone.entries) {
   val text=card(tone=tone)
   assertTrue(text,text.contains("dönem")); assertFalse(text.contains("Bugünkü hava")); assertFalse(text.contains("Güncel hava durumu"))
  }
 }
 @Test fun completedTripCannotBeCalledUpcoming() {
  val text=card(plan(end="2026-11-13")); assertTrue(text,text.contains("tamamlandı")); assertFalse(text.contains("yaklaşıyor"))
 }
 @Test fun invalidTemperatureCannotReachPeriodSummary() {
  val text=card(snapshot=ForecastSnapshot(timezone="Europe/Istanbul",maxTemp=900.0))
  assertFalse(text.contains("900")); assertTrue(text,text.contains("eksik"))
 }
 @Test fun fallbackChoosesPlanUsingEachDestinationDay() {
  val plans=listOf(plan("Ankara",end="2026-11-13",zone="Asia/Tokyo"),plan(start="2026-11-14",zone="Asia/Tokyo"))
  val text=RecommendationEngine.generateAssistantFallbackReply("Seyahat öner",WeatherData(cityName="İstanbul",timezone="America/Los_Angeles"),travelPlans=plans,now=now)
  assertTrue(text,text.contains("Bursa seyahati")); assertFalse(text.contains("Ankara seyahati"))
 }
 @Test fun unresolvedPlanZoneIsNotReportedAsNoPlan() {
  val text=RecommendationEngine.generateAssistantFallbackReply("Seyahat öner",WeatherData(timezone="UTC"),travelPlans=listOf(plan(zone=null)),now=now)
  assertTrue(text,text.contains("saat dilimi")); assertFalse(text.contains("planlanmış bir seyahatin bulunmuyor"))
 }
}
