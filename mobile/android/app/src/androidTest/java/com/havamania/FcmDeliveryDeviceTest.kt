package com.havamania

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class FcmDeliveryDeviceTest {
    private val event = FcmEnvelope("d3667c20-5d15-42f3-a448-2ae58774b96b", "Synthetic", "Local only", 2000)

    @Test fun skippedDeliveryCanRetryAndSuccessDeduplicatesPerOwner() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            dao.insert(NotificationItem(id = "other", userId = "b", title = "Keep"))
            assertFalse(deliverFcmRecord("a", event, { true }, dao::insertIncoming, dao::delete) { false })
            assertEquals(0, dao.getTotalCountFirst("a")); assertEquals(1, dao.getTotalCountFirst("b"))
            var posts = 0
            assertTrue(deliverFcmRecord("a", event, { true }, dao::insertIncoming, dao::delete) { posts++; true })
            assertFalse(deliverFcmRecord("a", event, { true }, dao::insertIncoming, dao::delete) { posts++; true })
            assertEquals(1, posts); assertEquals(1, dao.getTotalCountFirst("a"))
            dao.deleteAccountData("a")
            assertFalse(deliverFcmRecord("a", event, { true }, dao::insertIncoming, dao::delete) { fail("deleted owner"); true })
        } finally { db.close() }
    }

    @Test fun cancelledPreparationCleansRoomRecordDespiteCancelledJob() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            val entered = CompletableDeferred<Unit>()
            val never = CompletableDeferred<Boolean>()
            val task = launch {
                deliverFcmRecord("a", event, { true }, dao::insertIncoming, dao::delete) {
                    entered.complete(Unit); never.await()
                }
            }
            entered.await(); assertEquals(1, dao.getTotalCountFirst("a"))
            task.cancelAndJoin()
            assertEquals(0, dao.getTotalCountFirst("a"))
            assertTrue(deliverFcmRecord("a", event, { true }, dao::insertIncoming, dao::delete) { true })
        } finally { db.close() }
    }
}
