package com.havamania

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class NotificationPresentationTest {
    @Test fun sameTextForDifferentTripsMustNotHideEitherEvent() {
        val a = NotificationItem(id = "a", title = "Uyarı", message = "Yağmur", targetId = "trip-a", createdAt = 1)
        val b = a.copy(id = "b", targetId = "trip-b", createdAt = 2)
        assertEquals(listOf("b", "a"), notificationDisplayOrder(listOf(a, b)).map { it.id })
    }
    @Test fun largeListPreservesAllEventsIncludingIdenticalText() {
        val rows = (0 until 10_000).map { NotificationItem(id = "$it", title = "Aynı", createdAt = it.toLong()) }
        val result = notificationDisplayOrder(rows)
        assertEquals(10_000, result.size)
        assertEquals("9999", result.first().id)
        assertEquals("0", result.last().id)
    }
    @Test fun relativeTimeUsesSharedClockAndMinuteBoundary() {
        val created = Instant.parse("2026-10-22T12:00:00Z")
        assertEquals("Az önce", NotificationDateFormatter.formatCreatedAt(created.toEpochMilli(), created.plusSeconds(59)))
        assertEquals("1 dk önce", NotificationDateFormatter.formatCreatedAt(created.toEpochMilli(), created.plusSeconds(60)))
    }
}
