package com.havamania

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NotificationTapTest {
    private val item = NotificationItem(id = "event", userId = "a", actionType = NotificationActionType.WEATHER_HOME)
    private fun request() = NotificationTapRequest("a", item.id, NotificationSessionEpoch.capture())

    @Test fun ownVisibleWeatherItemResolvesFromStoredAction() = runBlocking {
        assertEquals(Routes.WEATHER_ROOT, resolveNotificationTap(request(), { "a" }, { false }) { uid, id ->
            assertEquals("a", uid); assertEquals(item.id, id); item
        })
    }

    @Test fun deniedSessionsNeverReadDisk() = runBlocking {
        for ((owner, blocked) in listOf(null to false, "b" to false, "a" to true)) {
            assertNull(resolveNotificationTap(request(), { owner }, { blocked }) { _, _ -> fail("No read"); item })
        }
        val stale = request(); NotificationSessionEpoch.changed()
        assertNull(resolveNotificationTap(stale, { "a" }, { false }) { _, _ -> fail("Old epoch"); item })
    }

    @Test fun missingOrWrongIdentityCannotOpenAnotherRecord() = runBlocking {
        for (record in listOf(null, item.copy(userId = "b"), item.copy(id = "another"))) {
            assertNull(resolveNotificationTap(request(), { "a" }, { false }) { _, _ -> record })
        }
    }

    @Test fun accountSwitchAndRoundTripDuringReadAreRejected() = runBlocking {
        for (roundTrip in listOf(false, true)) {
            var owner = "a"
            assertNull(resolveNotificationTap(request(), { owner }, { false }) { _, _ ->
                owner = "b"; NotificationSessionEpoch.changed()
                if (roundTrip) { owner = "a"; NotificationSessionEpoch.changed() }
                item
            })
        }
    }

    @Test fun invalidIdsAvoidDiskAndCancellationIsNotConvertedToNavigation() = runBlocking {
        for (id in listOf("", "\ninvalid", "x".repeat(1025))) {
            assertNull(resolveNotificationTap(request().copy(id = id), { "a" }, { false }) { _, _ -> fail("Invalid ID"); item })
        }
        assertTrue(runCatching {
            resolveNotificationTap(request(), { "a" }, { false }) { _, _ -> throw CancellationException("cancel") }
        }.exceptionOrNull() is CancellationException)
    }

    @Test fun legacyTripTargetIsCanonicalizedAndCannotInjectArgumentsOrAnotherRoute() {
        val trip = NotificationItem(category = NotificationCategory.TRAVEL,
            deepLinkTarget = "havamania://app/route/trip%20%26city%3Dother%2Fid")
        assertEquals(Routes.routeWeather("trip%20%26city%3Dother%2Fid"), notificationDestination(trip))
        assertEquals("${Routes.CALENDAR_ROOT}?focusId=trip%2Bid",
            notificationDestination(trip.copy(deepLinkTarget = "havamania://app/calendar?focusId=trip%2Bid")))
        for (target in listOf("havamania://app/route/a/b", "havamania://app/route/a?x=b",
            "havamania://app/calendar?focusId=a&city=b", "havamania://app/route/%0A",
            "havamania://app/route/%broken", "https://example.invalid/route/a")) {
            assertNull(notificationDestination(trip.copy(deepLinkTarget = target)))
        }
        assertNull(notificationDestination(trip.copy(category = NotificationCategory.GENERAL)))
    }
}
