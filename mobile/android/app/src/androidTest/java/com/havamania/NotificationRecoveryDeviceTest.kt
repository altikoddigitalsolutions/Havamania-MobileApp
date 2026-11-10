package com.havamania

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NotificationRecoveryDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun pending(db: NotificationDatabase, uid: String): Int = db.openHelper.writableDatabase.query(
        "SELECT COUNT(*) FROM notification_pending_deliveries WHERE userId = ?", arrayOf(uid)
    ).use { it.moveToFirst(); it.getInt(0) }

    @Test fun recoveredEventKeepsSystemIdentityWithoutReplacingAnotherAlert() = runBlocking {
        val name = "notification-identity-${UUID.randomUUID()}"
        var db = Room.databaseBuilder(context, NotificationDatabase::class.java, name).build()
        try {
            val rain = NotificationItem(id = notificationEventRecordId("a", "weather", "rain"),
                userId = "a", deduplicationKey = "rain", title = "Old rain")
            val storm = NotificationItem(id = notificationEventRecordId("a", "weather", "storm"),
                userId = "a", deduplicationKey = "storm", title = "Storm")
            // Fake OS sink models only the (tag,id) contract; no real notification is posted.
            val visible = mutableMapOf<Pair<String, Int>, String>()
            db.notificationDao().reserveForDelivery(rain, "old-process")
            visible[notificationSystemTag(rain) to 0] = rain.title
            db.notificationDao().reserveForDelivery(storm, "old-process")
            visible[notificationSystemTag(storm) to 0] = storm.title
            db.notificationDao().completeDelivery("a", storm.id, "old-process")
            db.close()
            db = Room.databaseBuilder(context, NotificationDatabase::class.java, name).build()
            db.notificationDao().recoverInterruptedDeliveries("a", "new-process")
            val retried = rain.copy(title = "Fresh rain")
            assertTrue(db.notificationDao().reserveForDelivery(retried, "new-process") > 0)
            visible[notificationSystemTag(retried) to 0] = retried.title
            db.notificationDao().completeDelivery("a", retried.id, "new-process")
            assertEquals(2, visible.size)
            assertEquals("Fresh rain", visible[notificationSystemTag(rain) to 0])
            assertEquals("Storm", visible[notificationSystemTag(storm) to 0])
            assertEquals(2, db.notificationDao().getTotalCountFirst("a"))
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun redeliveredFcmReclaimsInterruptedEventAndAcknowledgesSuccess() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            val event = FcmEnvelope("d3667c20-5d15-42f3-a448-2ae58774b96b", "Synthetic", "Local", 2000)
            val item = NotificationItem(id = "fcm_a_${event.eventId}", userId = "a")
            dao.reserveForDelivery(item, "old-process")
            var posts = 0
            suspend fun attempt(): Boolean = deliverFcmRecord("a", event, { true },
                { dao.reserveForDelivery(it).let { result -> if (result > 0) result else -1L } }, dao::delete,
                complete = { dao.completeDelivery(it.userId, it.id) }) { posts++; true }
            assertTrue(attempt()); assertEquals(0, pending(db, "a"))
            assertFalse(attempt()); assertEquals(1, posts)
            dao.recoverInterruptedDeliveries("a", "another-process")
            assertEquals("a", dao.notificationOwner(item.id))
        } finally { db.close() }
    }

    @Test fun reopeningRecoversOnlyInterruptedOwnerAndPreservesAcknowledgedAndCurrentAttempts() = runBlocking {
        val name = "notification-recovery-${UUID.randomUUID()}"
        var db = Room.databaseBuilder(context, NotificationDatabase::class.java, name).build()
        try {
            val a = NotificationItem(id = "a-pending", userId = "a", deduplicationKey = "a-event")
            val done = NotificationItem(id = "a-done", userId = "a", isRead = true)
            db.notificationDao().reserveForDelivery(a, "old-process")
            db.notificationDao().reserveForDelivery(done, "old-process")
            db.notificationDao().completeDelivery("a", done.id, "old-process")
            db.notificationDao().reserveForDelivery(NotificationItem(id = "b-pending", userId = "b"), "old-process")
            db.close()
            db = Room.databaseBuilder(context, NotificationDatabase::class.java, name).build()
            val dao = db.notificationDao()
            dao.recoverInterruptedDeliveries("a", "new-process")
            assertNull(dao.notificationOwner(a.id)); assertEquals("b", dao.notificationOwner("b-pending"))
            assertEquals("a", dao.notificationOwner(done.id)); assertTrue(dao.getAllNotificationsFirst("a").single().isRead)
            assertEquals(0, pending(db, "a")); assertEquals(1, pending(db, "b"))
            assertTrue(dao.reserveForDelivery(a, "new-process") > 0)
            dao.recoverInterruptedDeliveries("a", "new-process")
            assertEquals("a", dao.notificationOwner(a.id))
            assertEquals(-1L, dao.reserveForDelivery(a, "new-process"))
            assertEquals(-1L, dao.reserveForDelivery(a.copy(id = "same-event-new-id"), "new-process"))
            dao.completeDelivery("a", a.id, "old-process") // A stale acknowledgement cannot complete the new attempt.
            assertEquals(1, pending(db, "a"))
            dao.completeDelivery("a", a.id, "new-process")
            dao.recoverInterruptedDeliveries("a", "third-process")
            assertEquals("a", dao.notificationOwner(a.id)); assertEquals(0, pending(db, "a"))
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun accountAndInboxCleanupCascadeToPendingJournal() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            dao.reserveForDelivery(NotificationItem(id = "a", userId = "a"))
            dao.reserveForDelivery(NotificationItem(id = "b", userId = "b"))
            dao.deleteAccountData("a")
            assertEquals(0, pending(db, "a")); assertEquals(1, pending(db, "b"))
            assertEquals(-1L, dao.reserveForDelivery(NotificationItem(id = "late", userId = "a")))
            dao.deleteAll("b"); assertEquals(0, pending(db, "b"))
        } finally { db.close() }
    }

    @Test fun journalWriteFailureRollsBackInboxReservation() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java).build()
        try {
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_pending BEFORE INSERT ON notification_pending_deliveries BEGIN SELECT RAISE(ABORT, 'synthetic journal failure'); END")
            assertTrue(runCatching {
                db.notificationDao().reserveForDelivery(NotificationItem(id = "a", userId = "a"))
            }.isFailure)
            assertEquals(0, db.notificationDao().getTotalCountFirst("a")); assertEquals(0, pending(db, "a"))
        } finally { db.close() }
    }

    @Test fun version6MigrationPreservesLegacyMessagesAndAddsRecoverableReservations() = runBlocking {
        val name = "notification-v6-${UUID.randomUUID()}"
        var db = Room.databaseBuilder(context, NotificationDatabase::class.java, name).build()
        try {
            db.notificationDao().insert(NotificationItem(id = "old", userId = "a", title = "Preserve", isRead = true))
            // Version 6 has the same parent/guard schemas, without the new child table.
            db.openHelper.writableDatabase.execSQL("DROP TABLE notification_pending_deliveries")
            db.openHelper.writableDatabase.execSQL("DELETE FROM room_master_table")
            db.openHelper.writableDatabase.version = 6
            db.close()
            db = Room.databaseBuilder(context, NotificationDatabase::class.java, name)
                .addMigrations(NotificationDatabase.MIGRATION_6_7).build()
            val dao = db.notificationDao()
            assertEquals("Preserve", dao.getAllNotificationsFirst("a").single().title)
            assertTrue(dao.getAllNotificationsFirst("a").single().isRead)
            assertEquals(0, pending(db, "a"))
            dao.reserveForDelivery(NotificationItem(id = "pending", userId = "a"), "old-process")
            dao.recoverInterruptedDeliveries("a", "new-process")
            assertEquals(1, dao.getTotalCountFirst("a"))
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
