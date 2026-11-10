package com.havamania

import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class NotificationTrayDeviceTest {
    @Test fun postedNotificationDeliversInitialIntentAndReusesTheOpenActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as Application
        val manager = app.getSystemService(NotificationManager::class.java)
        val channel = "notification-tray-device-test-default"
        val events = LinkedBlockingQueue<Pair<Activity, Intent>>()
        val database = Room.inMemoryDatabaseBuilder(app, NotificationDatabase::class.java).build()
        val dao = database.notificationDao()
        val activities = mutableListOf<Activity>()
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity.javaClass != ComponentActivity::class.java) return
                activities += activity
                activity.intent.getStringExtra(NOTIFICATION_ID)?.let { events.add(activity to activity.intent) }
                (activity as ComponentActivity).addOnNewIntentListener { intent ->
                    intent.getStringExtra(NOTIFICATION_ID)?.let { events.add(activity to intent) }
                }
            }
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        }
        val tag = UUID.randomUUID().toString()
        val intents = mutableListOf<PendingIntent>()
        fun openPostedNotification(sequence: Int): Pair<Activity, Intent> {
            val title = "Havamania test ${tag.take(8)} $sequence"
            val item = NotificationItem(
                id = "$tag-$sequence",
                userId = "isolated-test-owner",
                message = "Güzergâhta tehlikeli hava koşulları bekleniyor.",
                category = NotificationCategory.TRAVEL,
                actionType = NotificationActionType.ROUTE_WEATHER,
                targetId = "İzmir/+?#"
            )
            runBlocking { dao.insert(item) }
            val intent = notificationTapIntent(app, item).setClass(app, ComponentActivity::class.java)
            val pending = PendingIntent.getActivity(app, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            intents += pending
            manager.notify(tag, sequence, Notification.Builder(app, channel)
                .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(item.message)
                .setContentIntent(pending).setAutoCancel(true).build())
            val deadline = SystemClock.uptimeMillis() + 5_000
            var active = manager.activeNotifications.firstOrNull { it.tag == tag && it.id == sequence }
            while (active == null && SystemClock.uptimeMillis() < deadline) {
                SystemClock.sleep(50)
                active = manager.activeNotifications.firstOrNull { it.tag == tag && it.id == sequence }
            }
            val posted = requireNotNull(active) {
                "Android did not post test notification $sequence"
            }
            assertEquals(item.message, posted.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
            posted.notification.contentIntent.send()
            val event = requireNotNull(events.poll(8, TimeUnit.SECONDS)) { "Notification intent was not delivered" }
            val request = requireNotNull(notificationTapRequest(event.second, item.userId, blocked = false))
            assertEquals(item.id, request.id)
            assertNull(notificationTapRequest(event.second, "other-owner", blocked = false))
            runBlocking {
                val destination = resolveNotificationTap(
                    request, { item.userId }, { false },
                    markRead = { uid, id -> dao.markAsRead(uid, listOf(id)) },
                    load = dao::getVisibleNotification
                )
                assertEquals(Routes.routeWeather(requireNotNull(item.targetId)), destination)
                assertTrue(requireNotNull(dao.getVisibleNotification(item.userId, item.id)).isRead)
            }
            return event
        }
        assertTrue("Notifications must be enabled on the test device", manager.areNotificationsEnabled())
        app.registerActivityLifecycleCallbacks(callbacks)
        try {
            manager.createNotificationChannel(NotificationChannel(channel, "Havamania test", NotificationManager.IMPORTANCE_DEFAULT).apply {
                setSound(null, null)
            })
            val cold = openPostedNotification(1)
            val warm = openPostedNotification(2)
            assertSame(cold.first, warm.first)
        } finally {
            database.close()
            manager.cancel(tag, 1)
            manager.cancel(tag, 2)
            intents.forEach(PendingIntent::cancel)
            manager.deleteNotificationChannel(channel)
            instrumentation.runOnMainSync {
                app.unregisterActivityLifecycleCallbacks(callbacks)
                activities.forEach(Activity::finish)
            }
        }
    }
}
