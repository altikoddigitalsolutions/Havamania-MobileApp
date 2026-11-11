package com.havamania

import kotlin.math.roundToInt

internal data class WeatherHeroMetrics(val humidity: String, val wind: String, val uv: String)

/** Exact legacy labels only: wind chill/gust must never be presented as wind speed. */
internal fun weatherHeroMetrics(data: WeatherData): WeatherHeroMetrics {
    fun legacy(vararg labels: String): String = data.details.firstOrNull { item ->
        labels.any { it.equals(item.title.trim(), ignoreCase = true) }
    }?.value?.takeIf { it.isNotBlank() } ?: "Veri yok"
    val humidity = data.humidity?.let { if (it in 0..100) "%$it" else "Veri yok" }
        ?: legacy("Nem", "Bağıl nem")
    val wind = data.windSpeed?.let {
        if (it.isFinite() && it >= 0) "${it.roundToInt()} km/sa" else "Veri yok"
    } ?: legacy("Rüzgar", "Rüzgâr")
    val uv = data.uvIndex?.takeIf { it >= 0 }?.toString() ?: "Veri yok"
    return WeatherHeroMetrics(humidity, wind, uv)
}
