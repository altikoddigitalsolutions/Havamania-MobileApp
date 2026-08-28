package com.havamania

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

enum class TravelStatus {
    PAST,
    ONGOING,
    UPCOMING
}

/**
 * Merkezi seyahat durum motoru.
 * Uygulama genelinde tarih bazlı sınıflandırma buradan yapılmalıdır.
 */
object TravelStatusResolver {

    fun getStatus(startDate: LocalDate, endDate: LocalDate, today: LocalDate = LocalDate.now()): TravelStatus {
        return when {
            endDate.isBefore(today) -> TravelStatus.PAST
            !startDate.isAfter(today) && !endDate.isBefore(today) -> TravelStatus.ONGOING
            else -> TravelStatus.UPCOMING
        }
    }

    fun isRouteWeatherEligible(plan: TravelPlan, now: LocalDateTime = LocalDateTime.now()): Boolean {
        val today = now.toLocalDate()
        val status = getStatus(plan.startDate, plan.endDate, today)

        if (status == TravelStatus.PAST) return false

        val departure = plan.departureDateTime ?: return false
        val hoursUntil = ChronoUnit.HOURS.between(now, departure)

        return (status == TravelStatus.ONGOING) || (hoursUntil in 0..48)
    }
}
