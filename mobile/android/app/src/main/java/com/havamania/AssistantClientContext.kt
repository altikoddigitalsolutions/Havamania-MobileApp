package com.havamania

import com.havamania.ui.theme.AssistantTone
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import kotlinx.serialization.Serializable

@Serializable
data class AssistantClientContext(val city: String? = null, val weather: String? = null, val trips: String? = null,
    val weather_data: AssistantWeatherSnapshot? = null)

@Serializable data class AssistantWeatherLocation(val name: String)
@Serializable data class AssistantCurrentWeather(val temp_c: Double? = null, val humidity_pct: Int? = null)
@Serializable data class AssistantForecastDay(val date: String, val min_c: Int? = null, val max_c: Int? = null,
    val precip_prob_pct: Int? = null)
@Serializable data class AssistantWeatherSnapshot(val observed_at: String, val timezone: String,
    val location: AssistantWeatherLocation? = null, val current: AssistantCurrentWeather? = null,
    val forecast: List<AssistantForecastDay> = emptyList(), val stale: Boolean = false)

internal fun assistantWeatherSnapshot(city: String?, data: WeatherData?, now: Long): AssistantWeatherSnapshot? {
    if (data == null || data.timestamp <= 0 || data.cityName.isBlank() || city != null && !city.equals(data.cityName, true)) return null
    val zone = runCatching { ZoneId.of(data.timezone) }.getOrNull() ?: return null
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val candidates = data.dailyForecast.filter { it.minTemp in -90..60 && it.maxTemp in -90..70 && it.minTemp <= it.maxTemp }
        .mapNotNull { row -> runCatching { LocalDate.parse(row.date) to row }.getOrNull() }
        .filter { !it.first.isBefore(today) && it.first.isBefore(today.plusDays(10)) }.groupBy { it.first }.filterValues { it.size == 1 }
        .values.map { it.single() }.sortedBy { it.first }
    val days = mutableListOf<AssistantForecastDay>()
    var previous: LocalDate? = null
    for ((date, row) in candidates) {
        if (days.size >= 10 || previous?.plusDays(1)?.let { it != date } == true) break
        days += AssistantForecastDay(date.toString(), row.minTemp, row.maxTemp,
            row.precipitationProbability?.takeIf { it in 0..100 })
        previous = date
    }
    val temperature = data.temperature.trim().removeSuffix("°").toDoubleOrNull()?.takeIf { it.isFinite() && it in -90.0..60.0 }
    return AssistantWeatherSnapshot(
        java.time.OffsetDateTime.ofInstant(Instant.ofEpochMilli(data.timestamp), zone).toString(), zone.id,
        AssistantWeatherLocation(data.cityName.take(100)),
        AssistantCurrentWeather(temperature, data.humidity?.takeIf { it in 0..100 }), days,
        data.isStale || now - data.timestamp > 3 * 60 * 60 * 1000L || data.timestamp - now > 10 * 60 * 1000L)
}

internal fun AssistantTone.platformTone(): String = when (this) {
    AssistantTone.SAMIMI -> "samimi"
    AssistantTone.RESMI -> "resmi"
    AssistantTone.KISA_NET -> "kisa"
    AssistantTone.DETAYLI_UZMAN -> "uzman"
    else -> "dengeli"
}

/** Snapshot only: no network lookup, notes, precise coordinates or profile health fields. */
internal fun assistantClientContext(city: String?, weather: WeatherData?, trips: List<TravelPlan>,
    personalization: Boolean, now: Long = System.currentTimeMillis()): AssistantClientContext {
    val selected = (city?.takeIf { it.isNotBlank() } ?: weather?.cityName)?.take(100)
    val fresh = weather != null && !weather.isStale && weather.timestamp > 0 &&
        now - weather.timestamp in 0..(30 * 60 * 1000L) &&
        (selected == null || selected.equals(weather.cityName, ignoreCase = true))
    val weatherText = if (fresh) {
        val zone = runCatching { ZoneId.of(weather.timezone).id }.getOrDefault("bilinmiyor")
        buildString {
            append("Konum: ${weather.cityName}. Uygulamanın edinme zamanı: ${Instant.ofEpochMilli(weather.timestamp)}. ")
            val temperature = weather.temperature.trim().removeSuffix("°").toDoubleOrNull()?.takeIf { it.isFinite() }
            append("Konum saat dilimi: $zone. ")
            append(if (temperature != null) "Sıcaklık: $temperature °C. " else "Anlık sıcaklık doğrulanamadı. ")
            append("Paylaşılan tahmin tarihleri: ")
            append(weather.dailyForecast.filter { it.minTemp <= it.maxTemp &&
                runCatching { LocalDate.parse(it.date) }.isSuccess }.take(10).joinToString("; ") { "${it.date}: ${it.minTemp}/${it.maxTemp} °C, kod ${it.weatherCode}" })
            append(". Paylaşılmayan tarihler için hava tahmini üretme.")
        }.take(2000)
    } else "Bu konum için doğrulanmış güncel hava verisi paylaşılmadı. Hava değeri veya tahmin üretme."
    val tripText = if (personalization) trips.sortedBy { it.startDate }.take(3).joinToString("; ") {
        "${it.city.take(100)}: ${it.startDate}–${it.endDate}"
    }.take(2000).ifBlank { null } else null
    val snapshot = assistantWeatherSnapshot(selected, weather, now)
    val text = if (snapshot != null && !fresh) "Hava bilgisi weather_data alanında veri zamanı ve eski veri işaretiyle paylaşıldı. Verilen tarihlerin dışında tahmin üretme." else weatherText
    return AssistantClientContext(selected, text, tripText, snapshot)
}
