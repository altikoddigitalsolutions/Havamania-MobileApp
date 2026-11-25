package com.havamania

/** Match route segments rather than prefixes so unrelated destinations never select a tab. */
internal fun bottomNavigationOwner(route: String?): String? = when (route?.substringBefore("?")?.substringBefore("/")) {
    Routes.WEATHER_ROOT, Routes.NOTIFICATION_CENTER -> Routes.WEATHER_ROOT
    Routes.CALENDAR_ROOT, "sub_route_weather" -> Routes.CALENDAR_ROOT
    Routes.AI_ROOT, Routes.AI_HISTORY, "sub_ai_history_detail" -> Routes.AI_ROOT
    Routes.PROFILE_ROOT, Routes.EDIT_PROFILE, Routes.CITIES, Routes.SETTINGS,
    Routes.SMART_ALERTS, Routes.PERSONALIZATION -> Routes.PROFILE_ROOT
    else -> null
}
