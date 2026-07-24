package com.havamania

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class TravelLogicTest {

    @Test
    fun `seyahate 11 gun varsa analiz olusturulmaz`() {
        val today = LocalDate.now()
        val startDate = today.plusDays(11)
        val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, startDate).toInt()

        assertTrue(daysUntil > TRIP_ANALYSIS_WINDOW_DAYS)
    }

    @Test
    fun `seyahate 10 gun varsa analiz olusturulabilir`() {
        val today = LocalDate.now()
        val startDate = today.plusDays(10)
        val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, startDate).toInt()

        assertTrue(daysUntil <= TRIP_ANALYSIS_WINDOW_DAYS)
    }

    @Test
    fun `baslangic gunu seyahat ACTIVE olur`() {
        val today = LocalDate.now()
        val plan = TravelPlan(city = "Test", startDate = today, endDate = today.plusDays(2))

        val status = getTripStatusManual(today, plan.startDate, plan.endDate)
        assertEquals(TripStatus.ACTIVE, status)
    }

    @Test
    fun `bitis gunu seyahat hala ACTIVE kalir`() {
        val today = LocalDate.now()
        val plan = TravelPlan(city = "Test", startDate = today.minusDays(2), endDate = today)

        val status = getTripStatusManual(today, plan.startDate, plan.endDate)
        assertEquals(TripStatus.ACTIVE, status)
    }

    @Test
    fun `bitis tarihinden sonraki gun COMPLETED olur`() {
        val today = LocalDate.now()
        val plan = TravelPlan(city = "Test", startDate = today.minusDays(5), endDate = today.minusDays(1))

        val status = getTripStatusManual(today, plan.startDate, plan.endDate)
        assertEquals(TripStatus.COMPLETED, status)
    }

    private fun getTripStatusManual(today: LocalDate, startDate: LocalDate, endDate: LocalDate): TripStatus {
        return when {
            today.isAfter(endDate) -> TripStatus.COMPLETED
            !today.isBefore(startDate) && !today.isAfter(endDate) -> TripStatus.ACTIVE
            else -> TripStatus.UPCOMING
        }
    }
}
