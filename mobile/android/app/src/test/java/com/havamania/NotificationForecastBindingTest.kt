package com.havamania
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
class NotificationForecastBindingTest {
 private val now=Instant.parse("2026-11-26T12:00:00Z")
 private fun payload(plan:TravelPlan,days:Int)=buildTravelNotificationData(plan,days,now)
 private fun base()=TravelPlan(id="trip-A",userId="A",city="Ankara",latitude=39.9,longitude=32.8,startDate=LocalDate.parse("2026-11-27"),endDate=LocalDate.parse("2026-11-28"),weatherSummary="Eski iyimser analiz")
 private fun bound(plan:TravelPlan)=ForecastSnapshot(timezone="Europe/Istanbul",minTemp=10.0,maxTemp=20.0,precipitationProbability=80,windSpeed=50.0,conditionSummary="Yağmurlu",binding=createTripForecastBinding(plan,plan.startDate,plan.endDate))
 private fun assertHidden(plan:TravelPlan) {
  val data=payload(plan,1)
  assertNull(data.minTemp); assertNull(data.maxTemp); assertNull(data.rainProbability); assertNull(data.weatherSummary)
  assertNull(data.previousAnalysisSummary); assertNull(data.comparisonText); assertTrue(data.recommendedItems.isEmpty())
 }
 @Test fun legacySnapshotCannotPopulateNotification() {
  assertHidden(base().copy(lastForecastSnapshot=ForecastSnapshot(maxTemp=35.0,weatherCode=61,precipitationProbability=90)))
 }
 @Test fun otherDestinationCannotPopulateNotification() {
  val base=base(); assertHidden(base.copy(city="Bursa",lastForecastSnapshot=bound(base)))
 }
 @Test fun changedDatesCannotPopulateNotification() {
  val base=base(); assertHidden(base.copy(endDate=base.endDate.plusDays(1),lastForecastSnapshot=bound(base)))
 }
 @Test fun otherOwnerCannotPopulateNotification() {
  val base=base(); assertHidden(base.copy(userId="B",lastForecastSnapshot=bound(base)))
 }
 @Test fun unmatchedPreviousSnapshotCannotProduceComparison() {
  val base=base(); val data=payload(base.copy(lastForecastSnapshot=bound(base),previousForecastSnapshot=ForecastSnapshot(maxTemp=15.0,precipitationProbability=0)),1)
  assertNull(data.comparisonText); assertNull(data.previousAnalysisSummary)
 }
 @Test fun validBoundDataRetainsWeatherAndEquipment() {
  val base=base(); val data=payload(base.copy(lastForecastSnapshot=bound(base)),1)
  assertEquals(20.0,data.maxTemp!!,0.0); assertEquals(80,data.rainProbability)
  assertTrue(data.recommendedItems.contains("Yağmurluk")); assertTrue(data.recommendedItems.contains("Rüzgarlık"))
 }
 @Test fun rejectedSnapshotCannotAppearAsWeatherInText() {
  val base=base();val plan=base.copy(city="Bursa",lastForecastSnapshot=bound(base))
  val text=buildTravelNotificationText(plan,1,false,payload(plan,1),LocalDate.parse("2026-11-26")).second
  assertTrue(text,text.contains("eksik")); assertFalse(text.contains("Yağmurlu")); assertFalse(text.contains("20°C"))
 }
}
