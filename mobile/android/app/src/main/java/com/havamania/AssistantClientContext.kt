package com.havamania

import com.havamania.ui.theme.AssistantTone
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import kotlinx.serialization.Serializable

@Serializable
data class AssistantClientContext(val city: String? = null, val weather: String? = null, val trips: String? = null)

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
    return AssistantClientContext(selected, weatherText, tripText)
}
