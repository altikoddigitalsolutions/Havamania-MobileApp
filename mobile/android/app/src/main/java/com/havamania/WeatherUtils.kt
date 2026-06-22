package com.havamania

import java.util.Locale

object WeatherUtils {

    /**
     * Yağış ihtimali formatlamak için merkezi fonksiyon.
     * Asla "%Bilinmiyor" veya "%null" üretmez.
     */
    fun formatRainProbability(value: Any?): String {
        val raw = value?.toString()?.trim()
        if (raw.isNullOrBlank()) return "bilinmiyor"

        val normalized = raw.lowercase(Locale.ROOT)
        if (
            normalized == "bilinmiyor" ||
            normalized == "unknown" ||
            normalized == "undefined" ||
            normalized == "null" ||
            normalized == "n/a"
        ) {
            return "bilinmiyor"
        }

        // Eğer zaten % işareti varsa temizleyip tekrar ekleyelim
        val numberPart = raw.replace("%", "").trim()
        val number = numberPart.toIntOrNull()

        return if (number != null) {
            "%$number"
        } else {
            "bilinmiyor"
        }
    }

    /**
     * Fırtınalı durumlarda yağış riskini düşük göstermemek için tutarlılık kontrolü.
     */
    fun getPrecipitationRiskText(prob: Any?, sum: Double, weatherCode: Int): String {
        val formatted = formatRainProbability(prob)

        // Tutarlılık Kontrolü: Fırtınalı/Sağanak ise "Düşük" olamaz
        if (weatherCode >= 80) { // Sağanak, Fırtınalı, Gök gürültülü
            if (formatted == "bilinmiyor" || (prob is Number && prob.toDouble() < 70.0)) {
                return "Yüksek"
            }
        } else if (weatherCode >= 51) { // Yağmurlu
            if (formatted == "bilinmiyor" || (prob is Number && prob.toDouble() < 55.0)) {
                return "Orta"
            }
        }

        if (formatted != "bilinmiyor") {
            return formatted
        }

        if (sum > 0) {
            return when {
                sum > 10.0 -> "Yüksek"
                sum > 2.0 -> "Orta"
                else -> "Düşük"
            }
        }

        return "Bilinmiyor"
    }

    /**
     * Hava durumuna göre emoji döner.
     */
    fun getWeatherEmoji(code: Int): String {
        return when (code) {
            0 -> "☀️"
            1 -> "🌤️"
            2 -> "⛅"
            3 -> "☁️"
            45, 48 -> "🌫️"
            in 51..67, in 80..82 -> "🌧️"
            in 71..77, in 85..86 -> "❄️"
            in 95..99 -> "⛈️"
            else -> "☀️"
        }
    }

    /**
     * Rüzgar hızına göre sözel seviye döner.
     */
    fun getWindLevelText(speed: Double): String {
        return when {
            speed < 5.0 -> "Sakin"
            speed < 12.0 -> "Hafif"
            speed < 20.0 -> "Esintili"
            speed < 29.0 -> "Orta"
            speed < 39.0 -> "Sert"
            speed < 50.0 -> "Kuvvetli"
            speed < 62.0 -> "Çok Kuvvetli"
            speed < 75.0 -> "Fırtına"
            else -> "Şiddetli Fırtına"
        }
    }

    /**
     * Rüzgar bilgisini sayısal + sözel formatta döner.
     * Örnek: "18 km/s (Orta)"
     */
    fun formatWindWithLevel(speed: Double?): String {
        if (speed == null) return "Bilinmiyor"
        val level = getWindLevelText(speed)
        return "${speed.toInt()} km/s ($level)"
    }

    /**
     * Rüzgar yönü kısaltmasını uzun isme çevirir.
     */
    fun getFullWindDirection(label: String?): String {
        return when (label) {
            "K" -> "Kuzey"
            "KD" -> "Kuzeydoğu"
            "D" -> "Doğu"
            "GD" -> "Güneydoğu"
            "G" -> "Güney"
            "GB" -> "Güneybatı"
            "B" -> "Batı"
            "KB" -> "Kuzeybatı"
            else -> label ?: "Bilinmiyor"
        }
    }
}
