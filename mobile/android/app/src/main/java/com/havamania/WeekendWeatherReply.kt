package com.havamania

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Compare actual dates, never localized display labels or unrelated weekends. */
internal fun weekendWeatherReply(weather: WeatherData, today: LocalDate): String {
    val saturday = if (today.dayOfWeek == DayOfWeek.SUNDAY) today.minusDays(1)
        else today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
    val sunday = saturday.plusDays(1)
    fun forecast(date: LocalDate) = weather.dailyForecast.singleOrNull { it.date == date.toString() }
    val sat = forecast(saturday)
    val sun = forecast(sunday)
    if (weather.isStale) return "Hafta sonu tahmini güncel değil. Güncel veriyi kontrol edin."
    if (today.dayOfWeek == DayOfWeek.SUNDAY) {
        return if (sun == null) "Bugünkü pazar tahmini bulunmuyor; geçmiş cumartesiyle karşılaştırma yapılamıyor."
        else "Bugünkü pazar kaydında en yüksek sıcaklık ${sun.maxTemp}°. Hafta sonunun kalan günü için saatlik tahmini ve yerel uyarıları kontrol edin."
    }
    if (sat == null || sun == null) return "Bu hafta sonunun iki günü için eşleşen tahmin bulunmuyor. Gün seçmek için veri yetersiz."
    val satRain = sat.precipitationProbability?.takeIf { it in 0..100 }
    val sunRain = sun.precipitationProbability?.takeIf { it in 0..100 }
    if (satRain == null || sunRain == null) return "Hafta sonu için yağış olasılıkları eksik. Sıcaklık tek başına uygun gün seçmek için yeterli değil."
    val summary = "Cumartesi ${sat.maxTemp}° ve yağış olasılığı %$satRain; pazar ${sun.maxTemp}° ve yağış olasılığı %$sunRain. "
    fun risk(day: DailyForecast, rain: Int) = rain > 40 || day.minTemp < 0 || day.maxTemp > 35 ||
        day.weatherCode in setOf(45, 48, 65, 66, 67, 75, 77, 82, 85, 86, 95, 96, 99)
    val satRisk = risk(sat, satRain)
    val sunRisk = risk(sun, sunRain)
    val verdict = when {
        satRisk && sunRisk -> "İki günde de dikkat gerektiren koşullar var; açık hava için uygun gün öneremiyorum."
        satRisk -> "Cumartesi için risk işaretleri var. Pazarın saatlik tahminini değerlendirin."
        sunRisk -> "Pazar için risk işaretleri var. Cumartesinin saatlik tahminini değerlendirin."
        satRain < sunRain -> "Cumartesi yağış olasılığı daha düşük."
        sunRain < satRain -> "Pazar yağış olasılığı daha düşük."
        else -> "Yağış olasılıkları eşit; bu verilerle bir günü üstün seçemiyorum."
    }
    return summary + verdict + " Rüzgâr, UV ve yerel uyarıları ayrıca kontrol edin."
}
