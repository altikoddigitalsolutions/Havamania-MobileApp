package com.havamania

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class TravelDailyIdentityDeviceTest {
    @Test fun concurrentReservationsAndReopenedDatabaseKeepOneDeliveryPerDestinationDay() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "qa-travel-daily-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, NotificationDatabase::class.java, name).build()
        val date = LocalDate.of(2026, 11, 26)
        val plan = TravelPlan(id = "qa-trip", userId = "A", city = "Tokyo", startDate = date, endDate = date.plusDays(2),
            lastForecastSnapshot = ForecastSnapshot(timezone = "Asia/Tokyo"))
        fun item(identity: TravelDailyIdentity, uid: String = "A") = NotificationItem(id = identity.notificationId,
            userId = uid, deduplicationKey = identity.deduplicationKey, eventAt = identity.eventAt)
        try {
            val identity = travelDailyIdentity(plan, Instant.parse("2026-11-25T23:59:59Z"))!!
            val dao = db.notificationDao()
            val results = (1..20).map { async(Dispatchers.IO) { dao.reserveForDelivery(item(identity)) } }.awaitAll()
            assertEquals(1, results.count { it > 0 })
            dao.completeDelivery("A", identity.notificationId)
            db.close()
            db = Room.databaseBuilder(context, NotificationDatabase::class.java, name).build()
            val reopened = db.notificationDao()
            val sameDay = travelDailyIdentity(plan, Instant.parse("2026-11-26T00:00:00Z"))!!
            assertEquals(identity, sameDay)
            assertEquals(0L, reopened.reserveForDelivery(item(sameDay)))
            assertEquals(1, reopened.getTotalCountFirst("A"))
            val nextDay = travelDailyIdentity(plan, Instant.parse("2026-11-26T15:00:00Z"))!!
            assertTrue(reopened.reserveForDelivery(item(nextDay)) > 0)
            reopened.completeDelivery("A", nextDay.notificationId)
            assertEquals(2, reopened.getTotalCountFirst("A"))
            val other = travelDailyIdentity(plan.copy(userId = "B"), Instant.parse("2026-11-25T23:59:59Z"))!!
            assertTrue(reopened.reserveForDelivery(item(other, "B")) > 0)
            reopened.completeDelivery("B", other.notificationId)
            assertEquals(1, reopened.getTotalCountFirst("B"))
        } finally { db.close(); check(context.deleteDatabase(name)) }
    }
}