package com.havamania

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class NotificationInboxRaceDeviceTest {
    @Test fun clearingInboxDuringPreparationBlocksLatePostAndAllowsNextAttempt() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            val repo = NotificationRepository(dao)
            val item = NotificationItem(id = "pending", userId = "a")
            val prepared = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val epoch = NotificationSessionEpoch.capture()
            var posts = 0
            suspend fun attempt() = deliverLocalNotification(item, { dao.reserveForDelivery(it) }, dao::delete,
                complete = { dao.completeDelivery(it.userId, it.id) }) {
                prepared.complete(Unit)
                release.await()
                postNotificationIfCurrent("a", epoch, { "a" }, { false },
                    stillReserved = { dao.ownsPendingDelivery("a", item.id) }) { posts++ }
            }
            dao.insert(NotificationItem(id = "keep", userId = "b"))
            val first = async { attempt() }
            withTimeout(5000) { prepared.await() }
            repo.deleteAll("a")
            release.complete(Unit)
            assertEquals(LocalNotificationDelivery.SKIPPED, first.await())
            assertEquals(0, posts); assertEquals(0, dao.getTotalCountFirst("a"))
            assertEquals(1, dao.getTotalCountFirst("b"))
            assertEquals(LocalNotificationDelivery.POSTED, attempt())
            assertEquals(1, posts); assertNotNull(dao.getVisibleNotification("a", item.id))
        } finally { db.close() }
    }

    @Test fun clearCannotSlipBetweenFinalCheckAndPost() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            val item = NotificationItem(id = "pending", userId = "a")
            dao.reserveForDelivery(item)
            val checked = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val trace = mutableListOf<String>()
            val posting = async {
                postNotificationIfCurrent("a", NotificationSessionEpoch.capture(), { "a" }, { false },
                    stillReserved = {
                        val owned = dao.ownsPendingDelivery("a", item.id)
                        checked.complete(Unit); release.await(); owned
                    }) { trace.add("post") }
            }
            withTimeout(5000) { checked.await() }
            val clearing = launch(start = CoroutineStart.UNDISPATCHED) {
                NotificationRepository(dao).delete("a", listOf(item.id)); trace.add("clear")
            }
            assertFalse(clearing.isCompleted)
            release.complete(Unit); assertTrue(posting.await()); clearing.join()
            assertEquals(listOf("post", "clear"), trace)
            assertFalse(dao.ownsPendingDelivery("a", item.id))
        } finally { db.close() }
    }
}
