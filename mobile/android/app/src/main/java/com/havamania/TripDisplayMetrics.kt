package com.havamania

internal data class TripDisplayMetrics(val averageTemperature: Double?, val rainProbability: Int?, val score: Int?)
private fun Double?.validDisplayTemperature() = this?.takeIf { it.isFinite() && it in -100.0..65.0 }
private fun Int?.validDisplayPercent() = this?.takeIf { it in 0..100 }

/** Never fill gaps in a snapshot with measurements from a different analysis. */
internal fun tripDisplayMetrics(plan: TravelPlan): TripDisplayMetrics {
    val snapshot = plan.lastForecastSnapshot
    if (snapshot != null) return TripDisplayMetrics(snapshot.averageTemperature.validDisplayTemperature(),
        snapshot.precipitationProbability.validDisplayPercent(), snapshot.travelScore.validDisplayPercent())
    val analysis = plan.analyses.lastOrNull()
    return TripDisplayMetrics(analysis?.averageTemperature.validDisplayTemperature(),
        analysis?.rainRiskPercent.validDisplayPercent(), analysis?.travelScore.validDisplayPercent())
}

internal fun tripDisplaySummary(plan: TravelPlan): String {
    plan.lastForecastSnapshot?.let { tripWeatherWarning(it)?.let { warning -> return warning } }
    val metrics = tripDisplayMetrics(plan)
    if (plan.lastForecastSnapshot == null) {
        plan.analyses.lastOrNull()?.summary?.takeIf { it.isNotBlank() }?.let { return "Kayıtlı analiz: $it" }
    }
    return buildList {
        add(metrics.averageTemperature?.let { "Dönem ortalaması: ${it.toInt()}°C." } ?: "Ortalama sıcaklık bilinmiyor.")
        add(metrics.rainProbability?.let { "Yağış olasılığı: %$it." } ?: "Yağış olasılığı bilinmiyor.")
    }.joinToString(" ")
}