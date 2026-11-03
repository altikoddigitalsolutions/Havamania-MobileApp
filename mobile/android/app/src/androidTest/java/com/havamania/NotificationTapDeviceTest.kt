package com.havamania

import android.content.Intent
import android.os.Build
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NotificationTapDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun pendingIntentIdentityIsImmutableDistinctPerEventAndDoesNotClearAppTask() {
        val one = NotificationItem(id = "test-one", userId = "test-owner", deduplicationKey = "test-weather-one")
        val two = one.copy(id = "test-two", deduplicationKey = "test-weather-two")
        val otherOwner = one.copy(userId = "test-other")
        val intent = notificationTapIntent(context, one)
        assertEquals(WeatherPremiumActivity::class.java.name, intent.component?.className)
        assertEquals(one.userId, intent.getStringExtra(NOTIFICATION_OWNER))
        assertEquals(one.id, intent.getStringExtra(NOTIFICATION_ID))
        assertEquals(0, intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        val first = notificationTapPendingIntent(context, one)
        val second = notificationTapPendingIntent(context, two)
        val third = notificationTapPendingIntent(context, otherOwner)
        try {
            assertEquals(first, notificationTapPendingIntent(context, one.copy(title = "Updated")))
            assertNotEquals(first, second); assertNotEquals(first, third)
            if (Build.VERSION.SDK_INT >= 31) assertTrue(first.isImmutable)
            // Never send these PendingIntents: this test does not launch the app or post notifications.
        } finally { first.cancel(); second.cancel(); third.cancel() }
    }

    @Test fun tapRequiresOwnAcknowledgedRecordAndDeletedRecordCannotNavigate() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            val item = NotificationItem(id = "test", userId = "test-owner", actionType = NotificationActionType.WEATHER_HOME)
            val request = NotificationTapRequest(item.userId, item.id, NotificationSessionEpoch.capture())
            dao.reserveForDelivery(item)
            assertNull(resolveNotificationTap(request, { item.userId }, { false }, dao::getVisibleNotification))
            dao.completeDelivery(item.userId, item.id)
            assertEquals(Routes.WEATHER_ROOT, resolveNotificationTap(request, { item.userId }, { false }, dao::getVisibleNotification))
            assertNull(resolveNotificationTap(request, { "other" }, { false }, dao::getVisibleNotification))
            dao.delete(item.userId, listOf(item.id))
            assertNull(resolveNotificationTap(request, { item.userId }, { false }, dao::getVisibleNotification))
        } finally { db.close() }
    }
}
