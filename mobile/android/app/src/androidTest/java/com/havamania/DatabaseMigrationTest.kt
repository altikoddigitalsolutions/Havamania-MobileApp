package com.havamania

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Uses isolated DB names; never opens the user's weather/notification databases. */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun oldDatabase(name: String, version: Int, create: (SupportSQLiteDatabase) -> Unit) {
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(db: SupportSQLiteDatabase) = create(db)
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        helper.writableDatabase
        helper.close()
    }

    @Test fun notificationVersion4PreservesExistingMessages() = runBlocking {
        val name = "migration_test_notifications"
        oldDatabase(name, 4) { db ->
            db.execSQL("""CREATE TABLE notifications (
                id TEXT NOT NULL PRIMARY KEY, userId TEXT NOT NULL, title TEXT NOT NULL,
                message TEXT NOT NULL, category TEXT NOT NULL, createdAt INTEGER NOT NULL,
                eventAt INTEGER, isRead INTEGER NOT NULL, actionType TEXT NOT NULL,
                targetId TEXT, deepLinkTarget TEXT, relatedTripId TEXT, actionLabel TEXT, travelData TEXT)""")
            db.execSQL("""INSERT INTO notifications (id,userId,title,message,category,createdAt,isRead,actionType)
                VALUES ('old','owner','Title','Message','TRAVEL',123,0,'NONE')""")
        }
        val database = Room.databaseBuilder(context, NotificationDatabase::class.java, name)
            .addMigrations(NotificationDatabase.MIGRATION_4_5).build()
        try {
            val rows = database.notificationDao().getAllNotifications("owner").first()
            assertEquals(1, rows.size)
            assertEquals("Message", rows.single().message)
            assertEquals("NORMAL", rows.single().severity)
            assertNull(rows.single().deduplicationKey)
            database.notificationDao().deleteAll("other")
            assertEquals(1, database.notificationDao().getTotalCountFirst("owner"))
            database.notificationDao().deleteAll("owner")
            assertEquals(0, database.notificationDao().getTotalCountFirst("owner"))
            assertTrue(NotificationRepository(database.notificationDao()).getAllNotificationsFlow("legacy").first().isEmpty())
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun weatherVersion11MigratesAndDeletesOnlyRequestedOwner() = runBlocking {
        val name = "migration_test_weather"
        oldDatabase(name, 11) { db ->
            db.execSQL("CREATE TABLE weather_cache (cityName TEXT NOT NULL PRIMARY KEY, jsonData TEXT NOT NULL, timestamp INTEGER NOT NULL)")
            db.execSQL("""CREATE TABLE ai_history (id TEXT NOT NULL PRIMARY KEY, userId TEXT NOT NULL,
                title TEXT NOT NULL, summary TEXT NOT NULL, messages TEXT NOT NULL, cityName TEXT, timestamp INTEGER NOT NULL)""")
            db.execSQL("""CREATE TABLE travel_plans (
                id TEXT NOT NULL PRIMARY KEY, userId TEXT NOT NULL, city TEXT NOT NULL,
                latitude REAL NOT NULL, longitude REAL NOT NULL, tripType TEXT NOT NULL,
                startDate INTEGER NOT NULL, endDate INTEGER NOT NULL, createdAt INTEGER NOT NULL,
                weatherSummary TEXT, aiSuggestion TEXT, userNote TEXT, userRating INTEGER,
                lastWeatherAnalysisText TEXT, lastWeatherAnalysisDate INTEGER, lastForecastSnapshot TEXT,
                previousForecastSnapshot TEXT, nextAnalysisEligibleDate INTEGER, weatherAnalysisStatus TEXT NOT NULL,
                isArchived INTEGER NOT NULL DEFAULT 0, analyses TEXT NOT NULL,
                lastDailyNotificationDate TEXT, isDemo INTEGER NOT NULL DEFAULT 0)""")
            for (uid in listOf("owner", "other")) {
                db.execSQL("""INSERT INTO travel_plans
                    (id,userId,city,latitude,longitude,tripType,startDate,endDate,createdAt,weatherAnalysisStatus,analyses)
                    VALUES (?,?,'Istanbul',41,29,'OTHER',0,0,123,'TOO_EARLY','[]')""", arrayOf(uid, uid))
                db.execSQL("INSERT INTO ai_history VALUES (?,?,'Title','Summary','[]',NULL,456)", arrayOf(uid, uid))
            }
        }
        val database = Room.databaseBuilder(context, WeatherDatabase::class.java, name)
            .addMigrations(WeatherDatabase.MIGRATION_11_13, WeatherDatabase.MIGRATION_13_14,
                WeatherDatabase.MIGRATION_14_15, WeatherDatabase.MIGRATION_15_16,
                WeatherDatabase.MIGRATION_16_17, WeatherDatabase.MIGRATION_17_18, WeatherDatabase.MIGRATION_18_19, WeatherDatabase.MIGRATION_19_20).build()
        try {
            val dao = database.weatherDao()
            val plan = dao.getAllTravelPlans("owner").single()
            assertEquals(123L, plan.updatedAt)
            assertEquals("WAITING_FOR_WINDOW", plan.weatherAnalysisStatus)
            assertEquals(456L, dao.getAllAiHistory("owner").single().updatedAt)
            assertNull(dao.getAllAiHistory("owner").single().syncedUpdatedAt)
            assertFalse(dao.getAllAiHistory("owner").single().pendingUpload)
            dao.reconcileAiHistory("owner", emptyList(), emptySet())
            assertEquals(1, dao.getAllAiHistory("owner").size) // unknown migrated row must survive
            dao.clearAllTravelPlans("owner")
            dao.clearAllAiHistory("owner")
            assertTrue(dao.getAllTravelPlans("owner").isEmpty())
            assertTrue(dao.getAllAiHistory("owner").isEmpty())
            assertEquals(1, dao.getAllTravelPlans("other").size)
            assertEquals(1, dao.getAllAiHistory("other").size)
            // Coordinate-based cache rows stay bounded, including repeated GPS updates.
            repeat(110) { dao.insertWeather(WeatherCacheEntity("key-$it", "{}", it.toLong())) }
            assertNull(dao.getCachedWeather("key-0"))
            assertNotNull(dao.getCachedWeather("key-109"))
            database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM weather_cache").use {
                assertTrue(it.moveToFirst())
                assertEquals(100, it.getInt(0))
            }
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
    @Test fun confirmedHistoryIsReconciledAfterDatabaseReopenWithoutDeletingPendingEdits() = runBlocking {
        val name = "history_reconciliation_test"
        context.deleteDatabase(name)
        var database = Room.databaseBuilder(context, WeatherDatabase::class.java, name).build()
        try {
            var dao = database.weatherDao()
            val mirror = AiHistoryEntity(id = "mirror", userId = "A", updatedAt = 100)
            val edited = mirror.copy(id = "edited")
            val malformed = mirror.copy(id = "malformed")
            dao.reconcileAiHistory("A", listOf(mirror, edited, malformed), setOf("mirror", "edited", "malformed"))
            dao.insertAiHistory(mirror.copy(id = "pending"))
            dao.insertAiHistory(edited.copy(updatedAt = 200))
            dao.insertAiHistory(mirror.copy(id = "other", userId = "B"))
            database.close()
            database = Room.databaseBuilder(context, WeatherDatabase::class.java, name).build()
            dao = database.weatherDao()
            assertEquals(100L, dao.getAiHistoryItem("mirror", "A")?.syncedUpdatedAt)
            // Presence is taken from raw document IDs, even if a document could not be decoded.
            dao.reconcileAiHistory("A", emptyList(), setOf("malformed"))
            assertNull(dao.getAiHistoryItem("mirror", "A"))
            assertNotNull(dao.getAiHistoryItem("pending", "A"))
            assertEquals(200L, dao.getAiHistoryItem("edited", "A")?.updatedAt)
            assertNotNull(dao.getAiHistoryItem("malformed", "A"))
            assertNotNull(dao.getAiHistoryItem("other", "B"))
            // A stale server response must not acknowledge a newer local edit.
            dao.reconcileAiHistory("A", listOf(edited), setOf("edited"))
            assertNull(dao.getAiHistoryItem("edited", "A")?.syncedUpdatedAt)
            dao.reconcileAiHistory("A", emptyList(), emptySet())
            assertNotNull(dao.getAiHistoryItem("edited", "A"))
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun pendingHistorySurvivesReopenAndOnlyMatchingVersionIsAcknowledged() = runBlocking {
        val name = "history_outbox_test"
        context.deleteDatabase(name)
        var database = Room.databaseBuilder(context, WeatherDatabase::class.java, name).build()
        try {
            val first = AiHistoryEntity(id = "stable-id", userId = "A", updatedAt = 100, pendingUpload = true)
            database.weatherDao().insertAiHistory(first)
            database.close()
            database = Room.databaseBuilder(context, WeatherDatabase::class.java, name).build()
            val dao = database.weatherDao()
            assertEquals("stable-id", dao.getPendingAiHistory("A").single().id)
            assertTrue(dao.getPendingAiHistory("B").isEmpty())
            dao.reconcileAiHistory("A", emptyList(), emptySet())
            assertEquals(1, dao.getPendingAiHistory("A").size)
            dao.insertAiHistory(first.copy(updatedAt = 101))
            dao.acknowledgeAiHistory("stable-id", "A", 100)
            assertEquals(1, dao.getPendingAiHistory("A").size)
            dao.acknowledgeAiHistory("stable-id", "B", 101)
            assertEquals(1, dao.getPendingAiHistory("A").size)
            dao.acknowledgeAiHistory("stable-id", "A", 101)
            assertTrue(dao.getPendingAiHistory("A").isEmpty())
            assertEquals(101L, dao.getAiHistoryItem("stable-id", "A")?.syncedUpdatedAt)
            dao.insertAiHistory(first.copy(updatedAt = 102))
            dao.stopAiHistoryUpload("A", "stable-id")
            assertTrue(dao.getPendingAiHistory("A").isEmpty())
            assertNotNull(dao.getAiHistoryItem("stable-id", "A"))
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun deletionRequestSurvivesRestartAndBlocksResurrection() = runBlocking {
        val name = "history_deletion_resume_test"
        context.deleteDatabase(name)
        var database = Room.databaseBuilder(context, WeatherDatabase::class.java, name).build()
        try {
            val row = AiHistoryEntity(id = "delete-me", userId = "A", updatedAt = 1, pendingUpload = true)
            database.weatherDao().insertAiHistory(row)
            database.weatherDao().enqueueHistoryDeletion("A", row.id)
            database.close()
            database = Room.databaseBuilder(context, WeatherDatabase::class.java, name).build()
            val dao = database.weatherDao()
            assertEquals(row.id, dao.getHistoryDeletions("A").single().recordId)
            assertTrue(dao.getHistoryDeletions("B").isEmpty())
            assertTrue(dao.getPendingAiHistory("A").isEmpty())
            dao.mergeAiHistory(row.copy(title = "stale snapshot", updatedAt = 2))
            assertEquals(1L, dao.getAiHistoryItem(row.id, "A")?.updatedAt)
            try { dao.insertAiHistory(row.copy(updatedAt = 3)); fail("Pending deletion must reject save") }
            catch (_: IllegalStateException) { }
            dao.completeHistoryDeletion("A", row.id)
            dao.completeHistoryDeletion("A", row.id) // retry is safe
            assertNull(dao.getAiHistoryItem(row.id, "A"))
            assertTrue(dao.getHistoryDeletions("A").isEmpty())
            dao.mergeAiHistory(row.copy(updatedAt = 4))
            assertNull(dao.getAiHistoryItem(row.id, "A")) // late snapshot cannot resurrect an acknowledged delete
            dao.enqueueHistoryDeletion("A", null)
            assertTrue(dao.hasHistoryDeletion("A", "new-id"))
            assertFalse(dao.hasHistoryDeletion("B", "new-id"))
            dao.completeHistoryDeletion("A", "")
            assertFalse(dao.hasHistoryDeletion("A", "new-id"))
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

}
