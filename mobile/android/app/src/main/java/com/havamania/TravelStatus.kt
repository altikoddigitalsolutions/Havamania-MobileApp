package com.havamania

import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.Duration

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

    fun isWithinRouteWeatherWindow(departure: Instant, now: Instant): Boolean {
        val untilDeparture = Duration.between(now, departure)
        return !untilDeparture.isNegative && untilDeparture <= Duration.ofHours(48)
    }

    fun isRouteWeatherEligible(plan: TravelPlan, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val status = getStatus(plan.startDate, plan.endDate, now.atZone(zone).toLocalDate())
        if (status == TravelStatus.PAST) return false
        val departure = plan.departureDateTime?.atZone(zone)?.toInstant() ?: return false
        // Existing ongoing-trip access is retained; pre-departure access is exactly 48 elapsed hours.
        return status == TravelStatus.ONGOING || isWithinRouteWeatherWindow(departure, now)
    }
}
