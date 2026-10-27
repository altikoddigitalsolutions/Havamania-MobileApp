package com.havamania

import java.util.Locale

internal fun tripWeatherCodePriority(code: Int): Int = when (code) {
    56, 57, 66, 67, 71, 73, 75, 77, 82, 85, 86, 95, 96, 99 -> 2
    45, 48, 51, 53, 55, 61, 63, 65, 80, 81 -> 1
    else -> 0
}

internal fun tripWeatherRiskLabel(code: Int): String? = when (code) {
    56, 57, 66, 67 -> "Donan yağış ve buzlanma"
    71, 73, 75, 77, 85, 86 -> "Kar yağışı"
    95, 96, 99 -> "Gök gürültülü fırtına"
    82 -> "Şiddetli sağanak"
    45, 48 -> "Sis"
    61, 63, 65, 80, 81 -> "Yağmur"
    51, 53, 55 -> "Çiseleme"
    else -> null
}

internal fun orderedTripWeatherCodes(codes: List<Int>): List<Int> {
    val counts = codes.groupingBy { it }.eachCount()
    return counts.keys.sortedWith(compareByDescending<Int> { tripWeatherCodePriority(it) }
        .thenByDescending { counts.getValue(it) }.thenByDescending { it })
}

internal fun tripTemperatureRange(snapshot: ForecastSnapshot): String? {
    // Older snapshots stored averaged daily minima/maxima; don't relabel those as extremes.
    if (snapshot.averageTemperature == null) return null
    val min = snapshot.minTemp?.takeIf { it.isFinite() } ?: return null
    val max = snapshot.maxTemp?.takeIf { it.isFinite() } ?: return null
    val locale = Locale.forLanguageTag("tr-TR")
    return "Dönem aralığı: en düşük ${String.format(locale, "%.1f", min)}°C, en yüksek ${String.format(locale, "%.1f", max)}°C."
}

internal fun tripWeatherWarning(snapshot: ForecastSnapshot): String? {
    val label = snapshot.weatherCode?.let(::tripWeatherRiskLabel)
    val range = tripTemperatureRange(snapshot)
    val temperatureExtreme = range != null && ((snapshot.minTemp ?: 1.0) <= 0 || (snapshot.maxTemp ?: 0.0) > 30)
    if (label == null && !temperatureExtreme) return null
    val parts = mutableListOf<String>()
    if (label != null) {
        val description = snapshot.conditionSummary?.takeIf { it.isNotBlank() } ?: label
        parts += "Seyahat döneminde ${description.lowercase(Locale.forLanguageTag("tr-TR"))} tahmin ediliyor."
    }
    if (range != null) parts += range
    parts += "Güncel tahminleri ve resmi uyarıları kontrol edin."
    return parts.joinToString(" ")
}
