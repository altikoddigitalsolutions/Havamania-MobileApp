package com.havamania

object Routes {
    // Auth Screens
    const val AUTH_WELCOME = "auth_welcome"
    const val LOGIN = "auth_login"
    const val REGISTER = "auth_register"
    const val FORGOT_PASSWORD = "auth_forgot_password"
    const val KVKK = "auth_kvkk"
    const val PRIVACY_POLICY = "auth_privacy_policy"
    const val TERMS_OF_USE = "auth_terms_of_use"

    // Root Containers
    const val ROOT_AUTH = "root_auth"
    const val ROOT_MAIN = "root_main"

    // Root Tabs
    const val WEATHER_ROOT = "root_weather"
    const val CALENDAR_ROOT = "root_calendar"
    const val AI_ROOT = "root_ai?conversationId={conversationId}"
    const val PROFILE_ROOT = "root_profile"

    // Sub Screens
    const val CITIES = "sub_cities"
    const val AI_HISTORY = "sub_ai_history"
    const val AI_HISTORY_DETAIL = "sub_ai_history_detail/{itemId}"
    const val EDIT_PROFILE = "sub_edit_profile"
    const val SETTINGS = "sub_settings"
    const val PERSONALIZATION = "sub_personalization"
    const val NOTIFICATION_CENTER = "sub_notification_center"
    const val SMART_ALERTS = "sub_smart_alerts"
    const val LEGAL_WEBVIEW = "sub_legal_webview/{title}/{url}"
}
