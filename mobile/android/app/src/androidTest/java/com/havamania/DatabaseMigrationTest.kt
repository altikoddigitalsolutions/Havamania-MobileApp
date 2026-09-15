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
                WeatherDatabase.MIGRATION_16_17).build()
        try {
            val dao = database.weatherDao()
            val plan = dao.getAllTravelPlans("owner").single()
            assertEquals(123L, plan.updatedAt)
            assertEquals("WAITING_FOR_WINDOW", plan.weatherAnalysisStatus)
            assertEquals(456L, dao.getAllAiHistory("owner").single().updatedAt)
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
}
