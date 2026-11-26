package com.havamania

internal data class TripDisplayMetrics(val averageTemperature: Double?, val rainProbability: Int?, val score: Int?)
private fun Double?.validDisplayTemperature() = this?.takeIf { it.isFinite() && it in -100.0..65.0 }
private fun Int?.validDisplayPercent() = this?.takeIf { it in 0..100 }

internal const val UNVERIFIED_TRIP_FORECAST_TEXT = "Tahminin hedef ve tarih eşleşmesi doğrulanamadı. Analizi yenileyin."

/** Current guide metrics require the same verified snapshot; old analysis cannot fill a gap. */
internal fun tripDisplayMetrics(plan: TravelPlan, now: java.time.Instant = java.time.Instant.now()): TripDisplayMetrics {
    val snapshot = plan.lastForecastSnapshot?.takeIf { tripForecastMatchesPlan(plan, it, now) }
        ?: return TripDisplayMetrics(null, null, null)
    return TripDisplayMetrics(snapshot.averageTemperature.validDisplayTemperature(),
        snapshot.precipitationProbability.validDisplayPercent(), snapshot.travelScore.validDisplayPercent())
}

internal fun tripDisplaySummary(plan: TravelPlan, now: java.time.Instant = java.time.Instant.now()): String {
    if (!tripForecastMatchesPlan(plan, plan.lastForecastSnapshot, now)) return UNVERIFIED_TRIP_FORECAST_TEXT
    plan.lastForecastSnapshot?.let { tripWeatherWarning(it)?.let { warning -> return warning } }
    val metrics = tripDisplayMetrics(plan, now)
    return buildList {
        add(metrics.averageTemperature?.let { "Dönem ortalaması: ${it.toInt()}°C." } ?: "Ortalama sıcaklık bilinmiyor.")
        add(metrics.rainProbability?.let { "Yağış olasılığı: %$it." } ?: "Yağış olasılığı bilinmiyor.")
    }.joinToString(" ")
}

internal fun tripGuidePacking(plan: TravelPlan, profile: PersonalizationProfile?, now: java.time.Instant = java.time.Instant.now()): String =
    if (tripForecastMatchesPlan(plan, plan.lastForecastSnapshot, now)) travelWeatherPacking(plan.lastForecastSnapshot, profile)
    else UNVERIFIED_TRIP_FORECAST_TEXT
