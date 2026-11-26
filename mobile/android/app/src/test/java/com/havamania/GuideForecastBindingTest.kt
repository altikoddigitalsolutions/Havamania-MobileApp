package com.havamania
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
class GuideForecastBindingTest {
 private val now=java.time.Instant.parse("2026-11-26T12:00:00Z")
 private fun metrics(plan:TravelPlan)=tripDisplayMetrics(plan,now)
 private fun summary(plan:TravelPlan)=tripDisplaySummary(plan,now)
 private fun plan()=TravelPlan(id="trip-A",userId="A",city="Ankara",startDate=LocalDate.parse("2026-11-27"),endDate=LocalDate.parse("2026-11-28"),analyses=listOf(TravelWeatherAnalysis(averageTemperature=35.0,rainRiskPercent=0,travelScore=99,summary="Eski iyi hava")))
 private fun bound(plan:TravelPlan)=ForecastSnapshot(timezone="Europe/Istanbul",averageTemperature=20.0,precipitationProbability=80,travelScore=25,weatherCode=71,binding=createTripForecastBinding(plan,plan.startDate,plan.endDate))
 private fun assertHidden(plan:TravelPlan) {
  assertEquals(TripDisplayMetrics(null,null,null),metrics(plan))
  val text=summary(plan)
  assertTrue(text,text.contains("eşleşmesi doğrulanamadı")); assertFalse(text.contains("Eski iyi")); assertFalse(text.contains("kar yağışı"))
 }
 @Test fun legacySnapshotCannotBecomeVerifiedGuideMetrics() {
  assertHidden(plan().copy(lastForecastSnapshot=ForecastSnapshot(averageTemperature=20.0,precipitationProbability=80,travelScore=25)))
 }
 @Test fun otherDestinationCannotBecomeGuideMetrics() {
  val base=plan();assertHidden(base.copy(city="Bursa",lastForecastSnapshot=bound(base)))
 }
 @Test fun changedDatesCannotBecomeGuideMetrics() {
  val base=plan();assertHidden(base.copy(endDate=base.endDate.plusDays(1),lastForecastSnapshot=bound(base)))
 }
 @Test fun otherOwnerCannotBecomeGuideMetrics() {
  val base=plan();assertHidden(base.copy(userId="B",lastForecastSnapshot=bound(base)))
 }
 @Test fun missingSnapshotCannotBorrowUnboundOldAnalysis() { assertHidden(plan()) }
 @Test fun validBindingStillShowsSnapshotMetricsAndWarning() {
  val base=plan();val value=base.copy(lastForecastSnapshot=bound(base))
  assertEquals(TripDisplayMetrics(20.0,80,25),metrics(value))
  assertTrue(summary(value).contains("kar yağışı"))
 }
 @Test fun packingAdviceRequiresTheSameBinding() {
  val base=plan();val valid=base.copy(lastForecastSnapshot=bound(base))
  assertTrue(tripGuidePacking(valid,null,now).contains("kaymayan"))
  val text=tripGuidePacking(valid.copy(city="Bursa"),null,now)
  assertTrue(text.contains("eşleşmesi doğrulanamadı"));assertFalse(text.contains("kaymayan"))
 }
}
