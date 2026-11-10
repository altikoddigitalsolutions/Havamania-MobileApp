package com.havamania

import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.junit.Assert.*
import org.junit.Test

class NotificationInboxRaceDeviceTest {
    @Test fun retryAfterInjectedSqliteFailureReadsSavedNotifications() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            dao.insert(NotificationItem(id = "saved", userId = "a"))
            dao.insert(NotificationItem(id = "other-account", userId = "b"))
            val failure = SQLiteException("Simulated read error")
            var attempts = 0
            val flakyDao = object : NotificationDao by dao {
                override fun getAllNotifications(uid: String) = flow {
                    if (++attempts == 1) throw failure
                    emitAll(dao.getAllNotifications(uid))
                }
            }
            val repo = NotificationRepository(flakyDao)
            val first = withTimeout(5_000) { repo.getAllNotificationsFlow("a").inboxReadResults().first() }
            assertSame(failure, first.exceptionOrNull())
            val retry = withTimeout(5_000) { repo.getAllNotificationsFlow("a").inboxReadResults().first() }
            assertEquals(listOf("saved"), retry.getOrThrow().map { it.id })
            assertEquals(2, attempts)
        } finally {
            db.close()
        }
    }
    @Test fun rejectedSecondOpenLeavesUnreadCountUnchanged() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            val repo = NotificationRepository(dao)
            val first = NotificationItem(id = "first", userId = "a", actionType = NotificationActionType.WEATHER_HOME)
            val second = first.copy(id = "second")
            dao.insert(first)
            dao.insert(second)
            assertEquals(2, repo.getUnreadCount("a").first())
            var listActive = true
            val open: (String?) -> Boolean = { route ->
                (listActive && route == Routes.WEATHER_ROOT).also { if (it) listActive = false }
            }
            openNotificationAndMarkRead(first, open) { repo.markAsRead(it.userId, listOf(it.id)) }
            openNotificationAndMarkRead(second, open) { repo.markAsRead(it.userId, listOf(it.id)) }
            assertEquals(1, repo.getUnreadCount("a").first())
            assertTrue(dao.getVisibleNotification("a", "first")?.isRead == true)
            assertTrue(dao.getVisibleNotification("a", "second")?.isRead == false)
        } finally {
            db.close()
        }
    }

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
