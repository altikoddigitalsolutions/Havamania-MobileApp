package com.havamania

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.room.Room
import android.content.Context
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripDeletionPersistenceTest {
    @Test fun pendingDeletionSurvivesReopenAndRemainsAccountScoped() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "trip-deletion-test-${java.util.UUID.randomUUID()}"
        fun open() = Room.databaseBuilder(context, WeatherDatabase::class.java, name).build()
        var database = open()
        try {
            database.weatherDao().enqueueTripDeletion(TripDeletionRequest("A", "trip"))
            database.close()
            database = open()
            val dao = database.weatherDao()
            assertEquals(1, dao.pendingTripDeletions("A").size)
            assertEquals(1, dao.pendingTripDeletionCount("A").first())
            assertEquals(0, dao.pendingTripDeletionCount("B").first())
            assertTrue(dao.pendingTripDeletions("B").isEmpty())
            dao.completeTripDeletion("A", "trip")
            assertTrue(dao.pendingTripDeletions("A").isEmpty())
            assertEquals(0, dao.pendingTripDeletionCount("A").first())
            assertTrue(dao.hasTripDeletion("A", "trip"))
            dao.clearTripDeletions("B")
            assertTrue(dao.hasTripDeletion("A", "trip"))
            dao.clearTripDeletions("A")
            assertFalse(dao.hasTripDeletion("A", "trip"))
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun staleSnapshotCannotRestorePendingOrCompletedDeletion() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, WeatherDatabase::class.java).build()
        try {
            val dao = database.weatherDao()
            val trip = TravelPlanEntity(id = "trip", userId = "A", city = "Ankara")
            dao.insertTravelPlan(trip)
            dao.enqueueTripDeletion(TripDeletionRequest("A", "trip"))
            dao.insertTravelPlan(trip.copy(city = "Stale"))
            assertEquals("Ankara", dao.getTravelPlanByIdRaw("trip")?.city)
            dao.completeTripDeletion("A", "trip")
            dao.insertTravelPlan(trip)
            assertNull(dao.getTravelPlanByIdRaw("trip"))
            dao.insertTravelPlan(trip.copy(id = "other", userId = "B"))
            assertEquals("B", dao.getTravelPlanByIdRaw("other")?.userId)
        } finally { database.close() }
    }

    @Test fun version20MigrationPreservesTripsAndCreatesDeletionQueue() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "trip-migration-test-${java.util.UUID.randomUUID()}"
        var database = Room.databaseBuilder(context, WeatherDatabase::class.java, name).build()
        try {
            database.weatherDao().insertTravelPlan(TravelPlanEntity(id = "kept", userId = "A", city = "İzmir"))
            database.close()
            // v21 only adds trip_deletions. Remove it to construct the v20 schema fixture.
            val sqlite = android.database.sqlite.SQLiteDatabase.openDatabase(
                context.getDatabasePath(name).path, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE)
            try {
                sqlite.execSQL("DROP TABLE trip_deletions")
                sqlite.execSQL("DROP TABLE trip_uploads")
                sqlite.version = 20
            } finally { sqlite.close() }
            database = Room.databaseBuilder(context, WeatherDatabase::class.java, name)
                .addMigrations(WeatherDatabase.MIGRATION_20_21, WeatherDatabase.MIGRATION_21_22).build()
            val dao = database.weatherDao()
            assertEquals("İzmir", dao.getTravelPlanByIdRaw("kept")?.city)
            dao.enqueueTripDeletion(TripDeletionRequest("A", "kept"))
            assertEquals(1, dao.pendingTripDeletions("A").size)
            assertEquals(1, dao.pendingTripDeletionCount("A").first())
            assertEquals(0, dao.pendingTripDeletionCount("B").first())
            assertTrue(dao.pendingTripDeletions("B").isEmpty())
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun pendingUploadProtectsLocalEditsAndOldAcknowledgements() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "trip-upload-test-${java.util.UUID.randomUUID()}"
        fun open() = Room.databaseBuilder(context, WeatherDatabase::class.java, name).build()
        var database = open()
        try {
            var dao = database.weatherDao()
            val trip = TravelPlanEntity(id = "queued", userId = "A", city = "Ankara", updatedAt = 10)
            dao.saveLocalTrip(trip)
            database.close()
            database = open()
            dao = database.weatherDao()
            assertEquals(1, dao.pendingTripUploads("A").size)
            assertTrue(dao.pendingTripUploads("B").isEmpty())
            dao.insertTravelPlan(trip.copy(city = "Old remote"))
            assertEquals("Ankara", dao.getTravelPlanByIdRaw("queued")?.city)
            dao.saveLocalTrip(trip.copy(city = "İzmir"))
            dao.acknowledgeTripUpload("A", "queued", 10)
            assertEquals(11L, dao.pendingTripUploadVersion("A", "queued"))
            dao.enqueueTripDeletion(TripDeletionRequest("A", "queued"))
            dao.completeTripDeletion("A", "queued")
            assertTrue(dao.pendingTripUploads("A").isEmpty())
            assertNull(dao.getTravelPlanByIdRaw("queued"))
        } finally { database.close(); context.deleteDatabase(name) }
    }
}
