package com.havamania

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal data class NotificationTapRequest(val owner: String, val id: String, val epoch: Long) {
    fun isCurrent(currentOwner: String?, blocked: Boolean): Boolean =
        epoch == NotificationSessionEpoch.capture() && canOpenOwnedNotification(owner, currentOwner, blocked)
}

internal class NotificationTapViewModel : ViewModel() {
    var pending by mutableStateOf<NotificationTapRequest?>(null)
}

internal fun canNavigateFromNotification(route: String?): Boolean = when (route) {
    null, Routes.AUTH_WELCOME, Routes.LOGIN, Routes.REGISTER,
    Routes.FORGOT_PASSWORD, Routes.PERSONALIZATION -> false
    else -> true
}

internal fun notificationTapRequest(
    incoming: Intent, currentOwner: String?, blocked: Boolean, restoring: Boolean = false
): NotificationTapRequest? {
    // Android can redeliver the original launch intent on recreation. Pending state belongs to the ViewModel.
    if (restoring) return null
    val owner = incoming.getStringExtra(NOTIFICATION_OWNER) ?: return null
    val id = incoming.getStringExtra(NOTIFICATION_ID) ?: return null
    if (incoming.action?.startsWith(NOTIFICATION_ACTION_PREFIX) != true ||
        !canOpenOwnedNotification(owner, currentOwner, blocked) || !validNotificationTapId(id)) return null
    return NotificationTapRequest(owner, id, NotificationSessionEpoch.capture())
}

private fun validNotificationTapId(id: String) =
    id.isNotBlank() && id.length <= 1024 && id.none(Char::isISOControl)

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
    fun allowed() = request.isCurrent(currentOwner(), blocked())
    currentCoroutineContext().ensureActive()
    if (!allowed() || !validNotificationTapId(request.id)) return null
    val item = load(request.owner, request.id) ?: return null
    currentCoroutineContext().ensureActive()
    if (!allowed() || item.userId != request.owner || item.id != request.id) return null
    val destination = notificationDestination(item) ?: return null
    if (!item.isRead) markRead(request.owner, request.id)
    currentCoroutineContext().ensureActive()
    return destination.takeIf { allowed() }
}
