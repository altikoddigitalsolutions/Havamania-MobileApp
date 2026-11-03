package com.havamania

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class NotificationDeletionDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun deletedOwnerCannotBeRepopulatedAfterDatabaseReopens() = runBlocking {
        val name = "notification-deletion-${UUID.randomUUID()}"
        var db = Room.databaseBuilder(context, NotificationDatabase::class.java, name).build()
        try {
            val a = NotificationItem(id = "a", userId = "owner-a", title = "A")
            val b = NotificationItem(id = "b", userId = "owner-b", title = "B")
            db.notificationDao().insert(a); db.notificationDao().insert(b)
            db.notificationDao().deleteAccountData("owner-a")
            db.close()
            db = Room.databaseBuilder(context, NotificationDatabase::class.java, name).build()
            val dao = db.notificationDao()
            dao.insert(a.copy(title = "late"))
            assertEquals(-1L, dao.insertIncoming(a.copy(id = "late")))
            dao.update(a.copy(title = "late"))
            assertEquals(0, dao.getTotalCountFirst("owner-a"))
            assertEquals("B", dao.getAllNotifications("owner-b").first().single().title)
            assertTrue(dao.isDeletedAccountHash(accountOwnerHash("owner-a")))
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun crossOwnerWritesCannotOverwriteAndInboxClearDoesNotBlockFutureMessages() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            dao.insert(NotificationItem(id = "shared", userId = "b", title = "Keep"))
            dao.insert(NotificationItem(id = "shared", userId = "a", title = "Replace"))
            dao.update(NotificationItem(id = "shared", userId = "a", title = "Replace"))
            assertEquals(-1L, dao.insertIncoming(NotificationItem(id = "shared", userId = "a")))
            assertEquals("Keep", dao.getAllNotifications("b").first().single().title)
            dao.deleteAll("b")
            dao.insert(NotificationItem(id = "new", userId = "b"))
            assertEquals(1, dao.getTotalCountFirst("b"))
        } finally { db.close() }
    }

    @Test fun version5MigratesWithoutLosingMessagesAndEnforcesNewGuard() = runBlocking {
        val name = "notification-v5-${UUID.randomUUID()}"
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name).callback(object : SupportSQLiteOpenHelper.Callback(5) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""CREATE TABLE notifications (
                        id TEXT NOT NULL PRIMARY KEY, userId TEXT NOT NULL, title TEXT NOT NULL,
                        message TEXT NOT NULL, category TEXT NOT NULL, createdAt INTEGER NOT NULL,
                        eventAt INTEGER, isRead INTEGER NOT NULL, actionType TEXT NOT NULL,
                        targetId TEXT, deepLinkTarget TEXT, relatedTripId TEXT, actionLabel TEXT,
                        travelData TEXT, deduplicationKey TEXT, severity TEXT NOT NULL DEFAULT 'NORMAL')""")
                    db.execSQL("""INSERT INTO notifications (id,userId,title,message,category,createdAt,isRead,actionType)
                        VALUES ('old','a','Old','Preserve','GENERAL',1,0,'NONE')""")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build())
        helper.writableDatabase; helper.close()
        val db = Room.databaseBuilder(context, NotificationDatabase::class.java, name)
            .addMigrations(NotificationDatabase.MIGRATION_5_6).build()
        try {
            val dao = db.notificationDao()
            assertEquals("Preserve", dao.getAllNotifications("a").first().single().message)
            dao.deleteAccountData("a")
            dao.insert(NotificationItem(id = "late", userId = "a"))
            assertEquals(0, dao.getTotalCountFirst("a"))
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
