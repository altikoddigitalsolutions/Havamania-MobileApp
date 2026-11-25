package com.havamania

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

internal fun buildTravelNotificationText(
    plan: TravelPlan,
    daysLeft: Int,
    isOngoing: Boolean,
    data: TravelNotificationData,
    today: LocalDate
): Pair<String, String> {
    val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("tr-TR"))
    val dateRange = "${plan.startDate.format(formatter)} - ${plan.endDate.format(formatter)}"
    val fields = ForecastSnapshot(minTemp = data.minTemp, maxTemp = data.maxTemp).validAdviceData()
    val condition = data.weatherSummary?.trim()?.takeIf { it.isNotEmpty() }
    val temperatures = when {
        fields.minTemp != null && fields.maxTemp != null -> "Kaydedilen tahmini sıcaklıklar: ${fields.minTemp.toInt()}°C–${fields.maxTemp.toInt()}°C."
        fields.maxTemp != null -> "Kaydedilen üst sıcaklık tahmini: ${fields.maxTemp.toInt()}°C. Alt sıcaklık bilgisi eksik."
        fields.minTemp != null -> "Kaydedilen alt sıcaklık tahmini: ${fields.minTemp.toInt()}°C. Üst sıcaklık bilgisi eksik."
        else -> "Sıcaklık bilgisi eksik."
    }
    val weather = if (condition == null && fields.minTemp == null && fields.maxTemp == null)
        "Seyahat dönemi için hava verisi eksik; güncel tahmini kontrol et."
    else listOf(condition?.let { "Seyahat dönemi tahmini: $it." } ?: "Seyahat dönemi tahmini.", temperatures).joinToString(" ")
    val intro = when {
        isOngoing -> {
            val count = (ChronoUnit.DAYS.between(plan.startDate, today) + 1).coerceIn(1, Int.MAX_VALUE.toLong())
            "Seyahatin devam ediyor (Gün: $count)."
        }
        daysLeft == 0 -> "Seyahatin bugün başlıyor."
        daysLeft == 1 -> "Seyahatin yarın başlıyor."
        daysLeft > 1 -> "Seyahatine $daysLeft gün kaldı."
        else -> "Kayıtlı seyahat tahminini inceleyebilirsin."
    }
    return "Seyahat Güncellemesi" to "${plan.city} • $dateRange: $intro $weather"
}