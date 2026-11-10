package com.havamania

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun validTripForecastZone(timezone: String): ZoneId? = runCatching { ZoneId.of(timezone) }.getOrNull()

internal fun tripForecastDate(timezone: String, now: Instant): LocalDate =
    now.atZone(requireNotNull(validTripForecastZone(timezone)) { "Tahmin saat dilimi geçersiz." }).toLocalDate()

internal fun tripForecastTimeLabel(snapshot: ForecastSnapshot?, now: Instant = Instant.now()): String {
    if (snapshot == null || snapshot.generatedAt <= 0L) return "Analiz zamanı bilinmiyor."
    val generated = runCatching { Instant.ofEpochMilli(snapshot.generatedAt) }.getOrNull()
        ?: return "Analiz zamanı bilinmiyor."
    val suppliedZone = snapshot.timezone?.let(::validTripForecastZone)
    val zone = suppliedZone ?: ZoneOffset.UTC
    val stamp = generated.atZone(zone).format(DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm", Locale.forLanguageTag("tr-TR")))
    val prefix = when {
        generated.isAfter(now) -> "Analiz zamanı cihaz saatinin ilerisinde. "
        suppliedZone != null && generated.atZone(zone).toLocalDate().isBefore(now.atZone(zone).toLocalDate()) -> "Önceki güne ait analiz. "
        else -> ""
    }
    val unknown = if (suppliedZone == null) " Konum saat dilimi bilinmiyor." else ""
    return "${prefix}Analiz: $stamp (${zone.id}).$unknown"
}
