package com.havamania

import android.app.NotificationManager
import android.os.Build

internal fun notificationChannelPolicyAllowsPosting(
    sdk: Int, importance: Int?, groupBlocked: Boolean
): Boolean = sdk < 26 || (importance != null && importance > 0 && (sdk < 28 || !groupBlocked))

/** Only reads policy. Never creates replacement IDs or changes the user's channel settings. */
internal fun notificationChannelAllowsPosting(manager: NotificationManager, channelId: String): Boolean {
    if (Build.VERSION.SDK_INT < 26) return true
    val channel = manager.getNotificationChannel(channelId) ?: return false
    val groupBlocked = if (Build.VERSION.SDK_INT >= 28 && channel.group != null) {
        val group = manager.getNotificationChannelGroup(channel.group) ?: return false
        group.isBlocked
    } else false
    return notificationChannelPolicyAllowsPosting(Build.VERSION.SDK_INT, channel.importance, groupBlocked)
}
