package com.havamania
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test
class AssistantLocationClockTest {
 private val now=Instant.parse("2026-11-13T12:00:00Z")
 private fun weather(zone:String)=WeatherData(cityName="Bursa",timezone=zone,temperature="20°",precipitationProbability=10,uvIndex=1,windSpeed=5.0,dailyForecast=listOf(
  DailyForecast("Saturday","2026-11-14",10,20,10,weatherCode=0),DailyForecast("Sunday","2026-11-15",10,22,20,weatherCode=0)))
 @Test fun missingZoneCannotResolveRelativeDates() {
  for(zone in listOf("","bad/zone")) for(prompt in listOf("bugün hava nasıl","yarın hava nasıl","hafta sonu hava nasıl")) {
   val reply=AiWeatherReply.datedReply(prompt,weather(zone),now)!!
   assertTrue(reply,reply.contains("saat dilimi")); assertFalse(reply.contains("günlük hava özeti"))
  }
 }
 @Test fun unknownZoneCannotRecommendWeatherOrClothes() {
  for(prompt in listOf("Bugün hava nasıl?","Yağmur var mı?","Bugün ne giymeliyim?","Hafta sonu hava nasıl?")) {
   val reply=RecommendationEngine.generateAssistantFallbackReply(prompt,weather("bad/zone"),now=now)
   assertTrue(reply,reply.contains("saat dilimi")); assertFalse(reply.contains("Cumartesi 20"))
  }
 }
 @Test fun weekendUsesInjectedLocationClock() {
  val reply=RecommendationEngine.generateAssistantFallbackReply("Hafta sonu hava nasıl?",weather("Europe/Istanbul"),now=now)
  assertTrue(reply,reply.contains("Cumartesi 20°")); assertTrue(reply,reply.contains("pazar 22°"))
 }
 @Test fun relativeTomorrowUsesLocationDayAfterUtcMidnightBoundary() {
  val reply=AiWeatherReply.datedReply("yarın hava nasıl",weather("Europe/Istanbul"),Instant.parse("2026-11-13T22:30:00Z"))!!
  assertTrue(reply,reply.contains("15 Kasım 2026")); assertTrue(reply,reply.contains("22°C"))
 }
 @Test fun noWeatherStillReturnsMissingDataMessage() {
  assertTrue(RecommendationEngine.generateAssistantFallbackReply("yarın hava nasıl",null,now=now).contains("erişilemiyor"))
 }
}
