package com.havamania
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
class TripStoredDateDeviceTest {
 @Test fun newTripRetainsDatesAfterTimezoneChangeAndReopen() = runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val name="qa-trip-date-${UUID.randomUUID()}.db";val prior=TimeZone.getDefault()
  var db=Room.databaseBuilder(context,WeatherDatabase::class.java,name).build()
  try {
   TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
   val day=LocalDate.parse("2026-12-31");val plan=TravelPlan(id="trip",userId="A",city="Ankara",startDate=day,endDate=day.plusDays(2))
   db.weatherDao().insertTravelPlan(plan.toEntity());db.close()
   TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
   db=Room.databaseBuilder(context,WeatherDatabase::class.java,name).build()
   val row=db.weatherDao().getTravelPlanById("trip","A")!!
   assertEquals("2026-12-31",row.startDateIso);assertEquals(plan.startDate,row.toDomain().startDate);assertEquals(plan.endDate,row.toDomain().endDate)
  } finally { db.close();TimeZone.setDefault(prior);check(context.deleteDatabase(name)) }
 }
 @Test fun version23MigrationFreezesLegacyDatesAndPreservesUploadRevision() = runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val name="qa-trip-date-v23-${UUID.randomUUID()}.db";val prior=TimeZone.getDefault()
  var db=Room.databaseBuilder(context,WeatherDatabase::class.java,name).build()
  try {
   TimeZone.setDefault(TimeZone.getTimeZone("Europe/Istanbul"))
   val day=LocalDate.parse("2026-11-26");val zone=ZoneId.of("Europe/Istanbul")
   db.weatherDao().saveLocalTrip(TravelPlanEntity(id="trip",userId="A",city="Ankara",startDate=day.atStartOfDay(zone).toInstant().toEpochMilli(),endDate=day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),userNote="Keep note"))
   val before=db.weatherDao().getTravelPlanById("trip","A")!!.updatedAt
   val queued=db.weatherDao().pendingTripUploads("A").single()
   db.close()
   val sqlite=android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath(name).path,null,android.database.sqlite.SQLiteDatabase.OPEN_READWRITE)
   try { stripTripDateIsoColumnsForLegacyFixture(sqlite);sqlite.version=23 } finally { sqlite.close() }
   db=Room.databaseBuilder(context,WeatherDatabase::class.java,name).addMigrations(WeatherDatabase.MIGRATION_23_24).build()
   val migrated=db.weatherDao().getTravelPlanById("trip","A")!!
   assertEquals("2026-11-26",migrated.startDateIso);assertEquals(before,migrated.updatedAt);assertEquals("Keep note",migrated.userNote)
   assertEquals(queued,db.weatherDao().pendingTripUploads("A").single());db.close()
   TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
   db=Room.databaseBuilder(context,WeatherDatabase::class.java,name).build()
   assertEquals(day,db.weatherDao().getTravelPlanById("trip","A")!!.toDomain().startDate)
   assertNull(db.weatherDao().getTravelPlanById("trip","B"))
  } finally { db.close();TimeZone.setDefault(prior);check(context.deleteDatabase(name)) }
 }
 @Test fun repeatedLegacyCloudRecordCannotShiftFrozenDates() = runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val name="qa-trip-date-replay-${UUID.randomUUID()}.db";val prior=TimeZone.getDefault()
  val db=Room.databaseBuilder(context,WeatherDatabase::class.java,name).build()
  try {
   val day=LocalDate.parse("2026-11-26");val zone=ZoneId.of("Europe/Istanbul")
   val legacy=TravelPlanEntity(id="trip",userId="A",city="Ankara",startDate=day.atStartOfDay(zone).toInstant().toEpochMilli(),endDate=day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
   TimeZone.setDefault(TimeZone.getTimeZone("Europe/Istanbul"))
   db.weatherDao().insertTravelPlan(legacy)
   assertEquals(day,db.weatherDao().getTravelPlanById("trip","A")!!.toDomain().startDate)
   TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
   db.weatherDao().insertTravelPlan(legacy)
   assertEquals(day,db.weatherDao().getTravelPlanById("trip","A")!!.toDomain().startDate)
  } finally { db.close();TimeZone.setDefault(prior);check(context.deleteDatabase(name)) }
 }
}
