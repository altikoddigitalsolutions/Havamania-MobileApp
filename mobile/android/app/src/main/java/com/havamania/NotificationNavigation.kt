package com.havamania

import java.net.URLDecoder

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
    NotificationActionType.TRAVEL_DETAIL ->
        (item.targetId?.takeIf(::validTripId) ?: item.relatedTripId?.takeIf(::validTripId))
            ?.let(Routes::calendarTrip)
            ?: Routes.CALENDAR_ROOT
    NotificationActionType.WEEKLY_SUMMARY -> Routes.AI_ROOT
    NotificationActionType.APP_UPDATE -> Routes.PROFILE_ROOT
    NotificationActionType.NONE -> when (item.deepLinkTarget) {
        "havamania://app/weather" -> Routes.WEATHER_ROOT
        "havamania://app/calendar" -> Routes.CALENDAR_ROOT
        "havamania://app/ai" -> Routes.AI_ROOT
        "havamania://app/profile" -> Routes.PROFILE_ROOT
        else -> if (item.category == NotificationCategory.TRAVEL) legacyTripNotificationDestination(item.deepLinkTarget) else null
    }
}

/** Compatibility for local trip records created before typed actions; accept only one trip ID. */
private fun legacyTripNotificationDestination(target: String?): String? {
    if (target == null || target.length > 2000) return null
    val routePrefix = "havamania://app/route/"
    val calendarPrefix = "havamania://app/calendar?focusId="
    val route = target.startsWith(routePrefix)
    val raw = when {
        route -> target.removePrefix(routePrefix)
        target.startsWith(calendarPrefix) -> target.removePrefix(calendarPrefix)
        else -> return null
    }
    if (raw.isBlank() || raw.any { it in "/?#&" }) return null
    val id = try { URLDecoder.decode(raw.replace("+", "%2B"), "UTF-8") } catch (_: IllegalArgumentException) { return null }
    if (!validTripId(id)) return null
    return if (route) Routes.routeWeather(id) else Routes.calendarTrip(id)
}

private fun validTripId(id: String) = id.isNotBlank() && id.length <= 200 && id.none(Char::isISOControl)
