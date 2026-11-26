package com.havamania
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
class TripForecastBindingTest {
 @Test fun legacySnapshotCannotSupplyVerifiedPackingMeasurements() {
  val now=Instant.parse("2026-11-26T12:00:00Z")
  val plan=TravelPlan(city="Ankara",startDate=LocalDate.parse("2026-11-27"),endDate=LocalDate.parse("2026-11-28"),lastForecastSnapshot=ForecastSnapshot(timezone="Europe/Istanbul",generatedAt=now.toEpochMilli(),maxTemp=31.0,minTemp=20.0))
  val text=RecommendationEngine.generateAssistantFallbackReply("Valizime ne almalıyım?",null,travelPlans=listOf(plan),now=now)
  assertTrue(text,text.contains("eşleşmesi doğrulanamadı")); assertFalse(text.contains("31,0°C"))
 }

 private val now=Instant.parse("2026-11-26T12:00:00Z")
 private fun plan()=TravelPlan(id="trip-A",userId="A",city="Ankara",district="Çankaya",latitude=39.9,longitude=32.8,startDate=LocalDate.parse("2026-11-27"),endDate=LocalDate.parse("2026-11-28"))
 private fun snapshot(plan:TravelPlan)=ForecastSnapshot(timezone="Europe/Istanbul",generatedAt=now.toEpochMilli(),binding=createTripForecastBinding(plan,plan.startDate,plan.endDate))
 @Test fun producerRetainsPlanAndCompleteCoverage() {
  val plan=plan()
  val daily=DailyDto(time=listOf("2026-11-27","2026-11-28"),weatherCode=listOf(0,3),tempMin=listOf(10.0,11.0),tempMax=listOf(20.0,21.0),windSpeedMax=listOf(5.0,6.0),precipProbMax=listOf(10,20))
  val result=buildTripForecastSnapshot(daily,plan.startDate,plan.endDate,"Europe/Istanbul",now,plan)!!
  assertTrue(tripForecastMatchesPlan(plan,result,now)); assertEquals("A",result.binding!!.ownerUid)
  assertEquals("2026-11-27",result.binding!!.coveredStartDate)
 }
 @Test fun changedDestinationCannotReuseForecast() {
  val plan=plan(); val snapshot=snapshot(plan)
  for(changed in listOf(plan.copy(city="İstanbul"),plan.copy(district="Keçiören"),plan.copy(latitude=40.0),plan.copy(longitude=33.0)))
   assertFalse(tripForecastMatchesPlan(changed,snapshot,now))
 }
 @Test fun changedPlannedDatesCannotReuseForecast() {
  val plan=plan(); val snapshot=snapshot(plan)
  assertFalse(tripForecastMatchesPlan(plan.copy(startDate=plan.startDate.minusDays(1)),snapshot,now))
  assertFalse(tripForecastMatchesPlan(plan.copy(endDate=plan.endDate.plusDays(1)),snapshot,now))
 }
 @Test fun otherAccountOrTripCannotReuseForecast() {
  val plan=plan(); val snapshot=snapshot(plan)
  assertFalse(tripForecastMatchesPlan(plan.copy(userId="B"),snapshot,now))
  assertFalse(tripForecastMatchesPlan(plan.copy(id="trip-B"),snapshot,now))
 }
 @Test fun missingTailOrMalformedCoverageIsRejected() {
  val plan=plan(); val snapshot=snapshot(plan)
  assertFalse(tripForecastMatchesPlan(plan,snapshot.copy(binding=snapshot.binding!!.copy(coveredEndDate="2026-11-27")),now))
  assertFalse(tripForecastMatchesPlan(plan,snapshot.copy(binding=snapshot.binding!!.copy(coveredStartDate="invalid")),now))
 }
 @Test fun ongoingTripAllowsElapsedDaysButNotRemainingGap() {
  val plan=plan().copy(startDate=LocalDate.parse("2026-11-25"))
  val snapshot=snapshot(plan)
  assertTrue(tripForecastMatchesPlan(plan,snapshot,now))
  assertTrue(tripForecastMatchesPlan(plan,snapshot.copy(binding=createTripForecastBinding(plan,LocalDate.parse("2026-11-26"),plan.endDate)),now))
  assertFalse(tripForecastMatchesPlan(plan,snapshot.copy(binding=createTripForecastBinding(plan,LocalDate.parse("2026-11-27"),plan.endDate)),now))
 }
 @Test fun converterRetainsNewBindingAndReadsLegacySnapshot() {
  val converter=ChatTypeConverters(); val plan=plan(); val snapshot=snapshot(plan)
  val decoded=converter.toForecastSnapshot(converter.fromForecastSnapshot(snapshot))!!
  assertEquals(snapshot,decoded); assertTrue(tripForecastMatchesPlan(plan,decoded,now))
  val legacy=converter.toForecastSnapshot("""{"maxTemp":22.0}""")!!
  assertEquals(22.0,legacy.maxTemp!!,0.0); assertNull(legacy.binding)
 }
}
