package com.havamania

import java.time.Instant
import java.time.LocalDate

internal fun invalidateChangedTripForecast(previous: TravelPlan, edited: TravelPlan): TravelPlan {
    val forecastChanged = previous.city != edited.city || previous.district != edited.district ||
        previous.latitude != edited.latitude || previous.longitude != edited.longitude ||
        previous.startDate != edited.startDate || previous.endDate != edited.endDate || previous.tripType != edited.tripType
    val routeChanged = forecastChanged || previous.originCity != edited.originCity ||
        previous.originDistrict != edited.originDistrict || previous.originLatitude != edited.originLatitude ||
        previous.originLongitude != edited.originLongitude || previous.departureTime != edited.departureTime
    var result = edited
    if (forecastChanged) result = result.copy(
        weatherSummary = null, packingAdvice = null, mustSee = null, foodAdvice = null, localAdvice = null,
        aiSuggestion = null, comfortScore = null, lastAnalysisAt = null, isAnalyzing = false,
        lastForecastSnapshot = null, previousForecastSnapshot = null, analyses = emptyList(),
        weatherAnalysisStatus = TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW
    )
    if (routeChanged) result = result.copy(routeWeatherSummary = null, lastRouteAnalysisAt = null)
    return result
}

/** The destination's known timezone controls analysis dates; legacy/unknown zones retain the explicit fallback. */
internal fun tripAnalysisToday(plan: TravelPlan, fallback: LocalDate, now: Instant = Instant.now()): LocalDate =
    plan.lastForecastSnapshot?.timezone?.let(::validTripForecastZone)?.let { now.atZone(it).toLocalDate() } ?: fallback
