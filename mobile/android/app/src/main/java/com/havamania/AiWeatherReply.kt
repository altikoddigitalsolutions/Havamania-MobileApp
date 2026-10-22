package com.havamania

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object AiWeatherReply {
    // Daily codes summarize a whole day; they must not inherit the device's current night/day.
    fun dailyCondition(code: Int): String = when (code) {
        0 -> "açık"
        1 -> "çoğunlukla açık"
        2 -> "parçalı bulutlu"
        3 -> "kapalı"
        45, 48 -> "sisli"
        51, 53, 55 -> "çiseleyen yağmurlu"
        56, 57 -> "donan çisentili"
        61, 63, 65 -> "yağmurlu"
        66, 67 -> "donan yağmurlu"
        71, 73, 75, 77 -> "karlı"
        80, 81, 82 -> "sağanak yağışlı"
        85, 86 -> "kar sağanaklı"
        95 -> "gök gürültülü fırtınalı"
        96, 99 -> "dolu ve gök gürültülü fırtınalı"
        else -> "bilinmiyor"
    }

    fun datedReply(prompt: String, data: WeatherData, now: Instant = Instant.now()): String? {
        val today = ForecastTime.localNow(data.timezone, now).toLocalDate()
        val date = AiIntentParser.detectDate(prompt, today) ?: return null
        val label = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("tr")))
        val day = data.dailyForecast.firstOrNull { it.date == date.toString() }
            ?: return "$label tarihi için ${data.cityName} şehrine ait tahmin verisi bulunmuyor."
        val stale = if (data.isStale) "Bu yanıt eski önbellek verisine dayanıyor; güncel tahmin doğrulanamadı. " else ""
        return stale + "$label tarihinde ${data.cityName} şehrinde günlük hava özeti: ${dailyCondition(day.weatherCode)}. " +
            "Sıcaklık ${day.minTemp}°C / ${day.maxTemp}°C civarında bekleniyor." +
            (day.precipitationProbability?.takeIf { it in 0..100 }?.let { " Yağış ihtimali %$it." } ?: "")
    }
}
