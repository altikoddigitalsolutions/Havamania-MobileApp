package com.havamania

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class LocalNotificationDeliveryDeviceTest {
    @Test fun concurrentSameKeyReservesOneRecordAndKeepsOwnersSeparate() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,
            NotificationDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            val results = (1..30).map { n -> async(Dispatchers.IO) {
                dao.reserveForDelivery(NotificationItem(id = "a-$n", userId = "a", deduplicationKey = "same"))
            } }.awaitAll()
            assertEquals(1, results.count { it > 0 }); assertEquals(29, results.count { it == -1L })
            assertEquals(1, dao.getTotalCountFirst("a"))
            assertTrue(dao.reserveForDelivery(NotificationItem(id = "b", userId = "b", deduplicationKey = "same")) > 0)
            assertEquals(-1L, dao.reserveForDelivery(NotificationItem(id = "b", userId = "a")))
            dao.deleteAccountData("a")
            assertEquals(-1L, dao.reserveForDelivery(NotificationItem(id = "late", userId = "a")))
            assertEquals(1, dao.getTotalCountFirst("b"))
        } finally { db.close() }
    }

    @Test fun failedDeliveryReleasesDedupAndSuccessfulRetryPreservesReadState() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,
            NotificationDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            val item = NotificationItem(id = "first", userId = "a", deduplicationKey = "weather")
            assertEquals(LocalNotificationDelivery.SKIPPED,
                deliverLocalNotification(item, { dao.reserveForDelivery(it) }, dao::delete, complete = { dao.completeDelivery(it.userId, it.id) }) { false })
            assertFalse(dao.existsWithKey("a", "weather"))
            assertEquals(LocalNotificationDelivery.POSTED,
                deliverLocalNotification(item, { dao.reserveForDelivery(it) }, dao::delete, complete = { dao.completeDelivery(it.userId, it.id) }) { true })
            dao.markAsRead("a", listOf(item.id))
            assertEquals(LocalNotificationDelivery.ALREADY_RECORDED,
                deliverLocalNotification(item.copy(id = "second"), { dao.reserveForDelivery(it) }, dao::delete, complete = { dao.completeDelivery(it.userId, it.id) }) {
                    fail("No duplicate post"); true
                })
            assertEquals(1, dao.getTotalCountFirst("a"))
            assertTrue(dao.getAllNotificationsFirst("a").single().isRead)
        } finally { db.close() }
    }
}
