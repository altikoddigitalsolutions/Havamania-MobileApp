package com.havamania

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class NotificationRecoveryFailureDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun realAckFailureIsRepairedWithoutSecondPostOrProcessRestart() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java).build()
        try {
            val recovery = NotificationDeliveryRecovery()
            val dao = db.notificationDao()
            val item = NotificationItem(id = "ack", userId = "a", deduplicationKey = "event")
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_ack BEFORE DELETE ON notification_pending_deliveries BEGIN SELECT RAISE(ABORT, 'synthetic ack failure'); END")
            var posts = 0
            suspend fun attempt() = deliverLocalNotification(item, { dao.reserveForDelivery(it) }, dao::delete,
                complete = { dao.completeDelivery(it.userId, it.id) }, recovery = recovery) { posts++; true }
            assertTrue(runCatching { attempt() }.isFailure)
            assertTrue(dao.hasPendingDelivery(item.id))
            assertTrue(dao.getAllNotifications("a").first().isEmpty())
            assertEquals(0, dao.getUnreadCount("a").first())
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_ack")
            assertEquals(LocalNotificationDelivery.ALREADY_RECORDED, attempt())
            assertEquals(1, posts); assertFalse(dao.hasPendingDelivery(item.id))
            assertEquals(listOf(item.id), dao.getAllNotifications("a").first().map { it.id })
            assertEquals(1, dao.getUnreadCount("a").first())
        } finally { db.close() }
    }

    @Test fun realDeleteFailureIsRepairedBeforeReservingRetryAndDoesNotTouchOtherOwner() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java).build()
        try {
            val recovery = NotificationDeliveryRecovery()
            val dao = db.notificationDao()
            dao.insert(NotificationItem(id = "keep", userId = "b", title = "Preserve"))
            val item = NotificationItem(id = "failed", userId = "a", deduplicationKey = "event")
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_delete BEFORE DELETE ON notifications WHEN OLD.userId = 'a' BEGIN SELECT RAISE(ABORT, 'synthetic cleanup failure'); END")
            assertTrue(runCatching {
                deliverLocalNotification(item, { dao.reserveForDelivery(it) }, dao::delete,
                    complete = { dao.completeDelivery(it.userId, it.id) }, recovery = recovery) { false }
            }.isFailure)
            assertTrue(dao.hasPendingDelivery(item.id))
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_delete")
            var posts = 0
            val retry = item.copy(id = "retried")
            assertEquals(LocalNotificationDelivery.POSTED,
                deliverLocalNotification(retry, { dao.reserveForDelivery(it) }, dao::delete,
                    complete = { dao.completeDelivery(it.userId, it.id) }, recovery = recovery) { posts++; true })
            assertNull(dao.notificationOwner(item.id)); assertFalse(dao.hasPendingDelivery(retry.id))
            assertEquals(1, posts); assertEquals(1, dao.getTotalCountFirst("a"))
            assertEquals("Preserve", dao.getAllNotifications("b").first().single().title)
        } finally { db.close() }
    }
}
