package com.havamania

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal data class NotificationTapRequest(val owner: String, val id: String, val epoch: Long)

internal fun notificationTapIntent(context: Context, item: NotificationItem): Intent =
    Intent(Intent.ACTION_VIEW, Uri.parse("havamania://app/notifications"), context, WeatherPremiumActivity::class.java).apply {
        action = NOTIFICATION_ACTION_PREFIX + notificationSystemTag(item)
        putExtra(NOTIFICATION_OWNER, item.userId)
        putExtra(NOTIFICATION_ID, item.id)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }

internal fun notificationTapPendingIntent(context: Context, item: NotificationItem): PendingIntent =
    PendingIntent.getActivity(context, 0, notificationTapIntent(context, item),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

internal suspend fun resolveNotificationTap(
    request: NotificationTapRequest, currentOwner: () -> String?, blocked: () -> Boolean,
    markRead: suspend (String, String) -> Unit = { _, _ -> },
    load: suspend (String, String) -> NotificationItem?
): String? {
    fun allowed() = request.epoch == NotificationSessionEpoch.capture() &&
        canOpenOwnedNotification(request.owner, currentOwner(), blocked())
    currentCoroutineContext().ensureActive()
    if (!allowed() || request.id.isBlank() || request.id.length > 1024 || request.id.any(Char::isISOControl)) return null
    val item = load(request.owner, request.id) ?: return null
    currentCoroutineContext().ensureActive()
    if (!allowed() || item.userId != request.owner || item.id != request.id) return null
    val destination = notificationDestination(item) ?: return null
    if (!item.isRead) markRead(request.owner, request.id)
    currentCoroutineContext().ensureActive()
    return destination.takeIf { allowed() }
}
