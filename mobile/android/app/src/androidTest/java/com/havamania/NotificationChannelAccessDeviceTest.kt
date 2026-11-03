package com.havamania

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import org.junit.Assume.assumeTrue
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class NotificationChannelAccessDeviceTest {
    @Test fun realChannelPolicyRejectsMissingAndDisabledButAllowsSilentChannel() {
        assumeTrue(Build.VERSION.SDK_INT >= 26)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val prefix = "test-channel-${UUID.randomUUID()}"
        val blocked = "$prefix-disabled"
        val silent = "$prefix-silent"
        try {
            assertFalse(notificationChannelAllowsPosting(manager, "$prefix-missing"))
            manager.createNotificationChannel(NotificationChannel(blocked, "Isolated test", NotificationManager.IMPORTANCE_NONE))
            manager.createNotificationChannel(NotificationChannel(silent, "Isolated test", NotificationManager.IMPORTANCE_MIN))
            assertFalse(notificationChannelAllowsPosting(manager, blocked))
            assertTrue(notificationChannelAllowsPosting(manager, silent))
            // Re-registering a disabled channel must not enable it or bypass the guard.
            manager.createNotificationChannel(NotificationChannel(blocked, "Isolated test", NotificationManager.IMPORTANCE_HIGH))
            assertFalse(notificationChannelAllowsPosting(manager, blocked))
        } finally {
            manager.deleteNotificationChannel(blocked); manager.deleteNotificationChannel(silent)
        }
    }

    @Test fun groupedChannelIsReadableAndDeletedGroupCannotPermitPosting() {
        assumeTrue(Build.VERSION.SDK_INT >= 26)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val group = "test-group-${UUID.randomUUID()}"
        val channel = "$group-channel"
        try {
            manager.createNotificationChannelGroup(NotificationChannelGroup(group, "Isolated test group"))
            manager.createNotificationChannel(NotificationChannel(channel, "Isolated test", NotificationManager.IMPORTANCE_LOW).apply {
                this.group = group
            })
            assertTrue(notificationChannelAllowsPosting(manager, channel))
            manager.deleteNotificationChannelGroup(group)
            assertFalse(notificationChannelAllowsPosting(manager, channel))
        } finally {
            manager.deleteNotificationChannel(channel); manager.deleteNotificationChannelGroup(group)
        }
    }
}
