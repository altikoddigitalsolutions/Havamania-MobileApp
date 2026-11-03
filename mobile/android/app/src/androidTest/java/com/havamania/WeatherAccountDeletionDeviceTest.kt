package com.havamania

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class WeatherAccountDeletionDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun reopenedDatabaseRejectsLatePlansHistoryAndQueues() = runBlocking {
        val name = "weather-account-delete-${UUID.randomUUID()}"
        var db = Room.databaseBuilder(context, WeatherDatabase::class.java, name).build()
        try {
            var dao = db.weatherDao()
            for (uid in listOf("a", "b")) {
                dao.saveLocalTrip(TravelPlanEntity(id = "trip-$uid", userId = uid, city = "City"))
                dao.insertAiHistory(AiHistoryEntity(id = "history-$uid", userId = uid, title = "Saved"))
                dao.enqueueTripDeletion(TripDeletionRequest(uid, "removed-$uid"))
                dao.insertHistoryDeletion(HistoryDeletionRequest(uid, "removed-$uid"))
            }
            dao.deleteAccountData("a")
            db.close()
            db = Room.databaseBuilder(context, WeatherDatabase::class.java, name).build()
            dao = db.weatherDao()
            val trip = TravelPlanEntity(id = "late", userId = "a", city = "Late")
            dao.insertTravelPlan(trip); dao.saveLocalTrip(trip)
            assertNull(dao.saveTripAnalysisIfVersion(trip, 0))
            val history = AiHistoryEntity(id = "late", userId = "a", title = "Late")
            try { dao.insertAiHistory(history); fail("Expected blocked local history") }
            catch (_: IllegalStateException) { }
            dao.mergeAiHistory(history)
            dao.reconcileAiHistory("a", listOf(history), setOf("late"))
            dao.enqueueTripUpload(TripUploadRequest("a", "late", 1))
            dao.enqueueTripDeletion(TripDeletionRequest("a", "late"))
            dao.insertHistoryDeletion(HistoryDeletionRequest("a", "late"))
            dao.enqueueHistoryDeletion("a", null)
            dao.completeHistoryDeletion("a", "")
            dao.completeTripDeletion("a", "late")
            assertTrue(dao.getAllTravelPlans("a").isEmpty())
            assertTrue(dao.getAllAiHistory("a").isEmpty())
            assertTrue(dao.pendingTripUploads("a").isEmpty())
            assertTrue(dao.pendingTripDeletions("a").isEmpty())
            assertTrue(dao.getHistoryDeletions("a").isEmpty())
            assertEquals(1, dao.getAllTravelPlans("b").size)
            assertEquals(1, dao.getAllAiHistory("b").size)
            assertEquals(1, dao.pendingTripUploads("b").size)
            assertEquals(1, dao.pendingTripDeletions("b").size)
            assertEquals(1, dao.getHistoryDeletions("b").size)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun queuedLocalWritesCannotOutrunAccountCleanup() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WeatherDatabase::class.java).build()
        try {
            val dao = db.weatherDao()
            val started = CompletableDeferred<Unit>()
            val producer = launch(Dispatchers.Default) {
                repeat(100) { index ->
                    dao.saveLocalTrip(TravelPlanEntity(id = "trip-$index", userId = "a", city = "City"))
                    if (index == 0) started.complete(Unit)
                    yield()
                }
            }
            started.await(); dao.deleteAccountData("a"); producer.join()
            assertTrue(dao.getAllTravelPlans("a").isEmpty())
            assertTrue(dao.pendingTripUploads("a").isEmpty())
        } finally { db.close() }
    }

    @Test fun version22MigrationPreservesDataAndAddsAccountFence() = runBlocking {
        val name = "weather-v22-${UUID.randomUUID()}"
        var db = Room.databaseBuilder(context, WeatherDatabase::class.java, name).build()
        try {
            db.weatherDao().saveLocalTrip(TravelPlanEntity(id = "kept", userId = "a", city = "Ankara"))
            db.weatherDao().insertAiHistory(AiHistoryEntity(id = "history", userId = "a", title = "Keep"))
            db.close()
            val sqlite = android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath(name).path,
                null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE)
            try { sqlite.execSQL("DROP TABLE weather_account_deletions"); sqlite.version = 22 }
            finally { sqlite.close() }
            db = Room.databaseBuilder(context, WeatherDatabase::class.java, name)
                .addMigrations(WeatherDatabase.MIGRATION_22_23).build()
            val dao = db.weatherDao()
            assertEquals("Ankara", dao.getAllTravelPlans("a").single().city)
            assertEquals("Keep", dao.getAllAiHistory("a").single().title)
            assertEquals(1, dao.pendingTripUploads("a").size)
            dao.deleteAccountData("a")
            dao.saveLocalTrip(TravelPlanEntity(id = "late", userId = "a", city = "Late"))
            assertTrue(dao.getAllTravelPlans("a").isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
