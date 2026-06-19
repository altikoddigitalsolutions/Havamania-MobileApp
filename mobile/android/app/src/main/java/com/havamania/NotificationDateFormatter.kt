package com.havamania

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Bildirimlerde tarih ve saat formatlaması için merkezi yardımcı sınıf.
 */
object NotificationDateFormatter {
    private val turkishLocale = Locale("tr", "TR")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", turkishLocale)
    private val fullDateFormatter = DateTimeFormatter.ofPattern("d MMMM EEEE • HH:mm", turkishLocale)

    /**
     * Verilen timestamp değerini kullanıcı dostu bir tarih-saat formatına çevirir.
     * Örnekler:
     * - Bugün 14:30
     * - Yarın 09:00
     * - 21 Haziran Cumartesi • 16:00
     */
    fun formatEventTime(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0) return ""

        return try {
            val eventDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
            )
            val eventDate = eventDateTime.toLocalDate()
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)

            when {
                eventDate == today -> "Bugün ${eventDateTime.format(timeFormatter)}"
                eventDate == tomorrow -> "Yarın ${eventDateTime.format(timeFormatter)}"
                else -> eventDateTime.format(fullDateFormatter)
            }
        } catch (e: Exception) {
            ""
        }
    }
}
