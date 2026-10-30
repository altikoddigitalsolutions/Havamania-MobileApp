package com.havamania

import java.net.URLEncoder

internal const val NOTIFICATION_OWNER = "notification_owner_uid"
internal const val NOTIFICATION_ID = "notification_id"
internal const val NOTIFICATION_ACTION_PREFIX = "com.havamania.notification."

internal fun canOpenOwnedNotification(owner: String?, currentUid: String?, deletionPending: Boolean): Boolean =
    !deletionPending && !owner.isNullOrBlank() && owner != "legacy" && owner == currentUid

internal fun notificationDestination(item: NotificationItem): String? = when (item.actionType) {
    NotificationActionType.WEATHER_HOME, NotificationActionType.HOURLY_FORECAST,
    NotificationActionType.DAILY_FORECAST, NotificationActionType.UV_DETAIL,
    NotificationActionType.WEATHER_ALERT -> Routes.WEATHER_ROOT
    NotificationActionType.TRAVEL_CALENDAR -> Routes.CALENDAR_ROOT
    NotificationActionType.TRAVEL_DETAIL -> item.targetId?.takeIf {
        it.isNotBlank() && it.length <= 200 && it.none(Char::isISOControl)
    }?.let { "${Routes.CALENDAR_ROOT}?focusId=${URLEncoder.encode(it, "UTF-8").replace("+", "%20")}" }
        ?: Routes.CALENDAR_ROOT
    NotificationActionType.WEEKLY_SUMMARY -> Routes.AI_ROOT
    NotificationActionType.APP_UPDATE -> Routes.PROFILE_ROOT
    NotificationActionType.NONE -> when (item.deepLinkTarget) {
        "havamania://app/weather" -> Routes.WEATHER_ROOT
        "havamania://app/calendar" -> Routes.CALENDAR_ROOT
        "havamania://app/ai" -> Routes.AI_ROOT
        "havamania://app/profile" -> Routes.PROFILE_ROOT
        else -> null
    }
}
