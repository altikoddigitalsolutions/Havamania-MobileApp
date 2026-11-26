package com.havamania
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
class TripForecastBindingDeviceTest {
 @Test fun bindingSurvivesDatabaseReopenAndCannotCrossAccounts() = runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val name="qa-forecast-binding-${UUID.randomUUID()}.db"
  var db=Room.databaseBuilder(context,WeatherDatabase::class.java,name).build()
  val now=Instant.parse("2026-11-26T12:00:00Z")
  val base=TravelPlan(id="qa-trip",userId="qa-A",city="Ankara",latitude=39.9,longitude=32.8,startDate=LocalDate.parse("2026-11-27"),endDate=LocalDate.parse("2026-11-28"))
  val plan=base.copy(lastForecastSnapshot=ForecastSnapshot(timezone="Europe/Istanbul",generatedAt=now.toEpochMilli(),maxTemp=20.0,binding=createTripForecastBinding(base,base.startDate,base.endDate)))
  try {
   db.weatherDao().insertTravelPlan(plan.toEntity()); db.close()
   db=Room.databaseBuilder(context,WeatherDatabase::class.java,name).build()
   val loaded=db.weatherDao().getTravelPlanById(plan.id,"qa-A")!!.toDomain()
   assertEquals(plan.lastForecastSnapshot,loaded.lastForecastSnapshot)
   assertTrue(tripForecastMatchesPlan(loaded,loaded.lastForecastSnapshot,now))
   assertNull(db.weatherDao().getTravelPlanById(plan.id,"qa-B"))
   assertFalse(tripForecastMatchesPlan(loaded.copy(userId="qa-B"),loaded.lastForecastSnapshot,now))
   assertFalse(tripForecastMatchesPlan(loaded.copy(city="Bursa"),loaded.lastForecastSnapshot,now))
  } finally { db.close(); check(context.deleteDatabase(name)) }
 }
}
