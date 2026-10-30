package com.havamania

import org.junit.Assert.*
import org.junit.Test

class NotificationNavigationTest {
    @Test fun accountSwitchLogoutLegacyAndDeletionCannotOpenOldNotification() {
        assertTrue(canOpenOwnedNotification("a", "a", false))
        assertFalse(canOpenOwnedNotification("a", "b", false))
        assertFalse(canOpenOwnedNotification("a", null, false))
        assertFalse(canOpenOwnedNotification("a", "a", true))
        assertFalse(canOpenOwnedNotification(null, "a", false))
        assertFalse(canOpenOwnedNotification("", "", false))
        assertFalse(canOpenOwnedNotification("legacy", "legacy", false))
    }

    @Test fun tripIdentifierCannotInjectAnotherNavigationArgument() {
        val item = NotificationItem(actionType = NotificationActionType.TRAVEL_DETAIL,
            targetId = "trip &city=Another/City?date=2026-12-01")
        assertEquals("${Routes.CALENDAR_ROOT}?focusId=trip%20%26city%3DAnother%2FCity%3Fdate%3D2026-12-01",
            notificationDestination(item))
        assertEquals(Routes.CALENDAR_ROOT, notificationDestination(item.copy(targetId = "\ninvalid")))
        assertEquals(Routes.CALENDAR_ROOT, notificationDestination(item.copy(targetId = "x".repeat(201))))
    }

    @Test fun fallbackCannotNavigateToArbitraryExternalOrInternalRoutes() {
        listOf("https://example.invalid", "havamania://evil/calendar", "auth_login",
            "havamania://app/route/private-trip", "havamania://app/calendar?focusId=private",
            "intent://app/weather", null).forEach { target ->
            assertNull(notificationDestination(NotificationItem(deepLinkTarget = target)))
        }
        assertEquals(Routes.WEATHER_ROOT, notificationDestination(NotificationItem(deepLinkTarget = "havamania://app/weather")))
        assertEquals(Routes.CALENDAR_ROOT, notificationDestination(NotificationItem(actionType = NotificationActionType.TRAVEL_CALENDAR)))
        assertEquals(Routes.AI_ROOT, notificationDestination(NotificationItem(actionType = NotificationActionType.WEEKLY_SUMMARY)))
        assertEquals(Routes.PROFILE_ROOT, notificationDestination(NotificationItem(actionType = NotificationActionType.APP_UPDATE)))
    }
}
