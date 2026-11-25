package com.havamania

// App precaution bands aligned with existing wind descriptions; not official road closure thresholds.
internal const val ROUTE_WIND_CAUTION_KMH = 39.0
internal const val ROUTE_WIND_DANGER_KMH = 62.0
internal fun validRouteTemperature(value: Double?) = value?.takeIf { it.isFinite() && it in -100.0..65.0 }
internal fun validRouteWind(value: Double?) = value?.takeIf { it.isFinite() && it in 0.0..500.0 }
internal fun routeTemperatureText(value: Double?, unit: String = "°"): String =
    validRouteTemperature(value)?.let { "${it.toInt()}$unit" } ?: "Bilgi yok"

/** One decoder for single and batch: preserve known hazards without inventing missing measurements. */
internal fun RouteHourlyDto.routeWeatherSample(index: Int): WaypointWeather? {
    val code = weatherCode.getOrNull(index)?.takeIf(WeatherUtils::isKnownCode)
    val temp = validRouteTemperature(temperature.getOrNull(index))
    val prob = precipitationProbability?.getOrNull(index)?.takeIf { it in 0..100 }
    val wind = validRouteWind(windSpeed?.getOrNull(index))
    val gust = validRouteWind(windGusts?.getOrNull(index))
    if (code == null && temp == null && prob == null && wind == null && gust == null) return null
    val feels = apparentTemperature?.getOrNull(index)?.takeIf { it.isFinite() && it in -120.0..85.0 }
    val hum = humidity?.getOrNull(index)?.takeIf { it in 0..100 }
    val (risk, reason) = RouteRiskAssessor.assess(code ?: -1, prob, temp, wind, gust)
    return WaypointWeather(code ?: -1, temp, feels, prob, wind, hum, risk, reason,
        isDay?.getOrNull(index)?.let { when (it) { 0 -> false; 1 -> true; else -> null } }, gust,
        code == null || temp == null || prob == null || wind == null || gust == null)
}