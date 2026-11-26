package com.havamania
import java.time.LocalDate
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test
class TripStoredDateTest {
 private fun crossing(writeZone:String,readZone:String,date:String) {
  val prior=TimeZone.getDefault()
  try {
   TimeZone.setDefault(TimeZone.getTimeZone(writeZone))
   val day=LocalDate.parse(date);val plan=TravelPlan(city="Ankara",startDate=day,endDate=day.plusDays(2))
   val entity=plan.toEntity()
   TimeZone.setDefault(TimeZone.getTimeZone(readZone))
   val loaded=entity.toDomain()
   assertEquals(plan.startDate,loaded.startDate);assertEquals(plan.endDate,loaded.endDate)
  } finally { TimeZone.setDefault(prior) }
 }
 @Test fun istanbulToLosAngelesRetainsDates() { crossing("Europe/Istanbul","America/Los_Angeles","2026-11-26") }
 @Test fun oppositeDateLineZonesRetainDates() { crossing("Pacific/Kiritimati","Pacific/Honolulu","2026-12-31") }
 @Test fun daylightSavingBoundaryRetainsDates() { crossing("America/New_York","America/Los_Angeles","2026-03-08") }
 @Test fun legacyBindingCanRecoverOriginalCalendarDates() {
  val prior=TimeZone.getDefault()
  try {
   val day=LocalDate.parse("2026-11-26");val plan=TravelPlan(city="Ankara",startDate=day,endDate=day.plusDays(1))
   val binding=createTripForecastBinding(plan,day,plan.endDate)
   val stored=TravelPlanEntity(id=plan.id,userId=plan.userId,city=plan.city,startDate=day.atStartOfDay(java.time.ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli(),endDate=plan.endDate.atStartOfDay(java.time.ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli(),lastForecastSnapshot=ForecastSnapshot(binding=binding))
   TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
   assertEquals(day,stored.toDomain().startDate)
  } finally { TimeZone.setDefault(prior) }
 }
 @Test fun frozenLegacyDatesDoNotChangeAgain() {
  val day=LocalDate.parse("2026-11-26");val zone=java.time.ZoneId.of("Europe/Istanbul")
  val row=TravelPlanEntity(startDate=day.atStartOfDay(zone).toInstant().toEpochMilli(),endDate=day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
  val stable=freezeTripDates(row,zone)
  assertEquals(day,storedTripDates(stable,java.time.ZoneId.of("America/Los_Angeles")).first)
  assertEquals(row.startDate,stable.startDate)
 }
 @Test fun explicitCalendarDatesWinOverLegacyEpoch() {
  val row=TravelPlanEntity(startDate=0,endDate=0,startDateIso="2026-12-31",endDateIso="2027-01-02")
  assertEquals(LocalDate.parse("2026-12-31"),row.toDomain().startDate)
  assertEquals(LocalDate.parse("2027-01-02"),row.toDomain().endDate)
 }
 private fun legacyRow(): TravelPlanEntity {
  val zone=java.time.ZoneId.of("Europe/Istanbul");val day=LocalDate.parse("2026-11-26")
  return TravelPlanEntity(id="trip",userId="A",startDate=day.atStartOfDay(zone).toInstant().toEpochMilli(),endDate=day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
 }
 @Test fun replayRetainsPreviouslyFrozenCalendarDates() {
  val row=legacyRow();val previous=freezeTripDates(row,java.time.ZoneId.of("Europe/Istanbul"))
  val replay=freezeTripDates(row,java.time.ZoneId.of("America/Los_Angeles"),previous)
  assertEquals("2026-11-26",replay.startDateIso);assertEquals(previous.endDateIso,replay.endDateIso)
 }
 @Test fun changedLegacyTimestampsAreNotLockedToPreviousDates() {
  val row=legacyRow();val previous=freezeTripDates(row,java.time.ZoneId.of("Europe/Istanbul"))
  val edited=row.copy(startDate=java.time.Instant.parse("2026-11-27T12:00:00Z").toEpochMilli(),endDate=java.time.Instant.parse("2026-11-28T12:00:00Z").toEpochMilli())
  assertEquals("2026-11-27",freezeTripDates(edited,java.time.ZoneId.of("America/Los_Angeles"),previous).startDateIso)
 }
 @Test fun anotherOwnerCannotDonateFrozenDates() {
  val row=legacyRow();val previous=freezeTripDates(row,java.time.ZoneId.of("Europe/Istanbul"))
  assertEquals("2026-11-25",freezeTripDates(row.copy(userId="B"),java.time.ZoneId.of("America/Los_Angeles"),previous).startDateIso)
 }
}
