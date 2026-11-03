package com.havamania

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class NotificationVisibilityDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun acknowledgementAlonePublishesListAndBadgeWithoutMarkingUnseenItemRead() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java).build()
        val collectors = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val dao = db.notificationDao()
            val initialList = CompletableDeferred<Unit>()
            val initialBadge = CompletableDeferred<Unit>()
            val visible = CompletableDeferred<List<NotificationItem>>()
            val unread = CompletableDeferred<Int>()
            collectors.launch {
                dao.getAllNotifications("a").collect {
                    if (it.isEmpty()) initialList.complete(Unit)
                    else visible.complete(it)
                }
            }
            collectors.launch {
                dao.getUnreadCount("a").collect {
                    if (it == 0) initialBadge.complete(Unit)
                    else unread.complete(it)
                }
            }
            withTimeout(5000) { initialList.await(); initialBadge.await() }
            val item = NotificationItem(id = "pending", userId = "a")
            assertTrue(dao.reserveForDelivery(item) > 0)
            dao.insert(NotificationItem(id = "b", userId = "b"))
            assertTrue(dao.getAllNotifications("a").first().isEmpty())
            assertEquals(0, dao.getUnreadCount("a").first())
            dao.markAsRead("a", listOf(item.id))
            dao.markAllAsRead("a")
            assertFalse(dao.getAllNotificationsFirst("a").single().isRead)
            assertFalse(visible.isCompleted); assertFalse(unread.isCompleted)
            // Only the journal changes here: both Room flows must observe that table too.
            dao.completeDelivery("a", item.id)
            assertEquals(listOf(item.id), withTimeout(5000) { visible.await() }.map { it.id })
            assertEquals(1, withTimeout(5000) { unread.await() })
            assertEquals(1, dao.getUnreadCount("b").first())
            dao.markAllAsRead("a")
            assertEquals(0, dao.getUnreadCount("a").first())
            assertEquals(1, dao.getUnreadCount("b").first())
        } finally { collectors.coroutineContext[Job]!!.cancelAndJoin(); db.close() }
    }

    @Test fun cancelledAndSkippedDeliveryNeverExposePreparingRecord() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            val item = NotificationItem(id = "attempt", userId = "a")
            val entered = CompletableDeferred<Unit>()
            val never = CompletableDeferred<Boolean>()
            val attempt = launch {
                deliverLocalNotification(item, { dao.reserveForDelivery(it) }, dao::delete,
                    complete = { dao.completeDelivery(it.userId, it.id) }) {
                    entered.complete(Unit); never.await()
                }
            }
            withTimeout(5000) { entered.await() }
            assertEquals(1, dao.getTotalCountFirst("a")) // Durable reservation exists, UI stays empty.
            assertTrue(dao.getAllNotifications("a").first().isEmpty())
            assertEquals(0, dao.getUnreadCount("a").first())
            attempt.cancelAndJoin()
            assertEquals(0, dao.getTotalCountFirst("a"))
            assertEquals(LocalNotificationDelivery.SKIPPED, deliverLocalNotification(item,
                { dao.reserveForDelivery(it) }, dao::delete,
                complete = { dao.completeDelivery(it.userId, it.id) }) { false })
            assertTrue(dao.getAllNotifications("a").first().isEmpty())
            assertEquals(0, dao.getUnreadCount("a").first())
        } finally { db.close() }
    }

    @Test fun reopeningHidesInterruptedItemAndKeepsLegacyVisibleAndReadActionsScoped() = runBlocking {
        val name = "notification-visible-${UUID.randomUUID()}"
        var db = Room.databaseBuilder(context, NotificationDatabase::class.java, name).build()
        try {
            db.notificationDao().insert(NotificationItem(id = "legacy", userId = "a", isRead = true))
            db.notificationDao().reserveForDelivery(NotificationItem(id = "pending", userId = "a", isRead = true), "old")
            db.close()
            db = Room.databaseBuilder(context, NotificationDatabase::class.java, name).build()
            val dao = db.notificationDao()
            assertEquals(listOf("legacy"), dao.getAllNotifications("a").first().map { it.id })
            assertEquals(0, dao.getUnreadCount("a").first())
            dao.markAsUnread("a", listOf("legacy", "pending"))
            assertEquals(1, dao.getUnreadCount("a").first())
            dao.completeDelivery("a", "pending", "old")
            val rows = dao.getAllNotifications("a").first().associateBy { it.id }
            assertTrue(rows.getValue("pending").isRead)
            assertFalse(rows.getValue("legacy").isRead)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
