package com.havamania

import java.time.Duration
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
    private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMMM HH:mm", turkishLocale)
    private val fullDateFormatter = DateTimeFormatter.ofPattern("d MMMM EEEE • HH:mm", turkishLocale)

    /**
     * Verilen timestamp değerini kullanıcı dostu bir tarih-saat formatına çevirir.
     * Gelecek zamanlar için (Hava olayı zamanı).
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

    /**
     * Bildirimin oluşturulma zamanını relative formatta döner.
     * Geçmiş zamanlar için (Gönderim zamanı).
     * Örnekler:
     * - Az önce
     * - 15 dk önce
     * - 2 saat önce
     * - Dün 14:05
     * - 19 Haziran 14:05
     */
    fun formatCreatedAt(timestamp: Long): String {
        val now = Instant.now()
        val createdAt = Instant.ofEpochMilli(timestamp)
        val duration = Duration.between(createdAt, now)

        val localCreatedAt = LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault())
        val localNow = LocalDateTime.ofInstant(now, ZoneId.systemDefault())

        return when {
            duration.toMinutes() < 1 -> "Az önce"
            duration.toMinutes() < 60 -> "${duration.toMinutes()} dk önce"
            duration.toHours() < 24 && localCreatedAt.dayOfYear == localNow.dayOfYear -> "${duration.toHours()} saat önce"
            localCreatedAt.toLocalDate() == localNow.toLocalDate().minusDays(1) -> "Dün ${localCreatedAt.format(timeFormatter)}"
            else -> localCreatedAt.format(dayMonthFormatter)
        }
    }
}
