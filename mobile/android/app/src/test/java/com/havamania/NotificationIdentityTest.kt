package com.havamania

import org.junit.Assert.*
import org.junit.Test

class NotificationIdentityTest {
    @Test fun eventRecordIsStableAndSeparatedByOwnerKindAndEvent() {
        val id = notificationEventRecordId("a", "weather", "rain:today")
        assertEquals(id, notificationEventRecordId("a", "weather", "rain:today"))
        assertNotEquals(id, notificationEventRecordId("b", "weather", "rain:today"))
        assertNotEquals(id, notificationEventRecordId("a", "route", "rain:today"))
        assertNotEquals(id, notificationEventRecordId("a", "weather", "storm:today"))
        assertTrue(id.matches(Regex("event_[0-9a-f]{64}")))
    }

    @Test fun fieldBoundariesAndUnicodeDoNotAlias() {
        assertNotEquals(notificationEventRecordId("a", "bc", "d"), notificationEventRecordId("ab", "c", "d"))
        assertNotEquals(notificationEventRecordId("a:b", "c", "d"), notificationEventRecordId("a", "b:c", "d"))
        assertEquals(notificationEventRecordId("şehir", "hava", "yağış🌧"), notificationEventRecordId("şehir", "hava", "yağış🌧"))
    }

    @Test fun regeneratedRecordForSameEventKeepsTagButDifferentAlertDoesNot() {
        val old = NotificationItem(id = "old", userId = "a", deduplicationKey = "rain")
        assertEquals(notificationSystemTag(old), notificationSystemTag(old.copy(id = "new", title = "Updated")))
        assertNotEquals(notificationSystemTag(old), notificationSystemTag(old.copy(deduplicationKey = "uv")))
        assertNotEquals(notificationSystemTag(old), notificationSystemTag(old.copy(userId = "b")))
    }

    @Test fun javaHashCollisionAndFallbackKeyCannotReplaceAnotherEvent() {
        assertEquals("Aa".hashCode(), "BB".hashCode())
        val a = NotificationItem(id = "Aa", userId = "a")
        assertNotEquals(notificationSystemTag(a), notificationSystemTag(a.copy(id = "BB")))
        assertNotEquals(notificationSystemTag(a), notificationSystemTag(a.copy(deduplicationKey = "Aa")))
        assertEquals(notificationSystemTag(a), notificationSystemTag(a.copy(deduplicationKey = " ")))
    }
}
