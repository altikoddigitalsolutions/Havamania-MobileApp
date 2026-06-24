package com.havamania

import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class AiIntent {
    CLOTHING,
    ACTIVITY,
    TRAVEL,
    PACKING,
    CALENDAR,
    WEEKEND_FORECAST,
    TRIP_RISK,
    TRIP_TIMING,
    TRIP_ROUTE,
    GENERAL_WEATHER,
    OUTDOOR_EVENT,
    CHAT
}

object AiIntentParser {
    private val turkishMonths = mapOf(
        "ocak" to Month.JANUARY, "şubat" to Month.FEBRUARY, "mart" to Month.MARCH,
        "nisan" to Month.APRIL, "mayıs" to Month.MAY, "haziran" to Month.JUNE,
        "temmuz" to Month.JULY, "ağustos" to Month.AUGUST, "eylül" to Month.SEPTEMBER,
        "ekim" to Month.OCTOBER, "kasım" to Month.NOVEMBER, "aralık" to Month.DECEMBER
    )

    private val cities = listOf(
        "Adana", "Adıyaman", "Afyonkarahisar", "Ağrı", "Amasya", "Ankara", "Antalya", "Artvin",
        "Aydın", "Balıkesir", "Bilecik", "Bingöl", "Bitlis", "Bolu", "Burdur", "Bursa", "Çanakkale",
        "Çankırı", "Çorum", "Denizli", "Diyarbakır", "Edirne", "Elazığ", "Erzincan", "Erzurum",
        "Eskişehir", "Gaziantep", "Giresun", "Gümüşhane", "Hakkari", "Hatay", "Isparta", "Mersin",
        "İstanbul", "İzmir", "Kars", "Kastamonu", "Kayseri", "Kırklareli", "Kırşehir", "Kocaeli",
        "Konya", "Kütahya", "Malatya", "Manisa", "Kahramanmaraş", "Mardin", "Muğla", "Muş",
        "Nevşehir", "Niğde", "Ordu", "Rize", "Sakarya", "Samsun", "Siirt", "Sinop", "Sivas",
        "Tekirdağ", "Tokat", "Trabzon", "Tunceli", "Şanlıurfa", "Uşak", "Van", "Yozgat", "Zonguldak",
        "Aksaray", "Bayburt", "Karaman", "Kırıkkale", "Batman", "Şırnak", "Bartın", "Ardahan",
        "Iğdır", "Yalova", "Karabük", "Kilis", "Osmaniye", "Düzce"
    )

    fun normalizeTurkish(text: String): String {
        return text.lowercase(Locale("tr"))
            .replace('ı', 'i')
            .replace('ç', 'c')
            .replace('ğ', 'g')
            .replace('ö', 'o')
            .replace('ş', 's')
            .replace('ü', 'u')
            .trim()
    }

    fun detectCity(text: String): String? {
        val normalizedInput = normalizeTurkish(text)

        return cities
            .filter { city ->
                val normalizedCity = normalizeTurkish(city)
                // Use word boundaries to ensure we match the whole city name
                val regex = Regex("\\b${Regex.escape(normalizedCity)}\\b", RegexOption.IGNORE_CASE)
                regex.containsMatchIn(normalizedInput)
            }
            .maxByOrNull { it.length }
    }

    fun detectDate(text: String): LocalDate? {
        val lowerText = text.lowercase(Locale("tr"))
        val today = LocalDate.now()

        if (lowerText.contains("bugün")) return today
        if (lowerText.contains("yarın")) return today.plusDays(1)
        if (lowerText.contains("ertesi gün")) return today.plusDays(1)

        // Hafta sonu check
        if (lowerText.contains("hafta sonu")) {
            var date = today
            while (date.dayOfWeek.value != 6) { // Saturday
                date = date.plusDays(1)
            }
            return date
        }

        // Specific date parsing (e.g., "24 Haziran")
        val regex = Regex("(\\d{1,2})\\s+([a-zA-ZşŞğĞüÜıİöÖçÇ]+)")
        val match = regex.find(lowerText)
        if (match != null) {
            val day = match.groupValues[1].toIntOrNull()
            val monthStr = match.groupValues[2].lowercase(Locale("tr"))
            val month = turkishMonths[monthStr]
            if (day != null && month != null) {
                return try {
                    var date = LocalDate.of(today.year, month, day)
                    // Eğer tarih geçtiyse ve çok eskiyse (örn 6 aydan fazla), gelecek yıl olarak düşün
                    if (date.isBefore(today.minusDays(7))) {
                        date = date.plusYears(1)
                    }
                    date
                } catch (e: Exception) {
                    null
                }
            }
        }

        return null
    }

    fun detectIntent(text: String): AiIntent {
        val lower = normalizeTurkish(text)

        return when {
            lower.contains("selam") || lower.contains("merhaba") || lower.contains("nasilssin") ||
                    lower.contains("kimsin") -> AiIntent.CHAT

            lower.contains("takvim") || lower.contains("ajanda") || lower.contains("etkinlikler") ||
                    lower.contains("optimize") || lower.contains("program") -> AiIntent.CALENDAR

            lower.contains("dugun") || lower.contains("konser") || lower.contains("mac") ||
                    lower.contains("organizasyon") || lower.contains("etkinlik") -> AiIntent.OUTDOOR_EVENT

            lower.contains("valiz") || lower.contains("canta") || lower.contains("yanima ne") ||
                    lower.contains("hazirla") || lower.contains("koymaliyim") -> AiIntent.PACKING

            lower.contains("giysi") || lower.contains("kiyafet") || lower.contains("giy") ||
                    lower.contains("ustume") || lower.contains("giymeliyim") || lower.contains("kombin") -> AiIntent.CLOTHING

            lower.contains("disari") || lower.contains("uygun") || lower.contains("yapabilirim") ||
                    lower.contains("piknik") || lower.contains("kosu") || lower.contains("bisiklet") ||
                    lower.contains("kamp") || lower.contains("mangal") || lower.contains("yuruyus") ||
                    lower.contains("spor") || lower.contains("aktivite") || lower.contains("deniz") ||
                    lower.contains("yuzmek") -> AiIntent.ACTIVITY

            lower.contains("risk") || lower.contains("tehlike") || lower.contains("sorun") ||
                    lower.contains("uyari") || lower.contains("guvenli") -> AiIntent.TRIP_RISK

            lower.contains("saat") || lower.contains("zaman") || lower.contains("ne zaman") -> AiIntent.TRIP_TIMING

            lower.contains("rota") || lower.contains("yol") || lower.contains("guzergah") -> AiIntent.TRIP_ROUTE

            lower.contains("seyahat") || lower.contains("gezi") || lower.contains("gitmek") ||
                    lower.contains("gidiyorum") || lower.contains("yolculuk") || lower.contains("tatil") -> AiIntent.TRAVEL

            lower.contains("hafta sonu") || lower.contains("cumartesi") || lower.contains("pazar") -> AiIntent.WEEKEND_FORECAST

            else -> AiIntent.GENERAL_WEATHER
        }
    }

    fun isWeatherQuery(text: String): Boolean {
        val normalizedInput = normalizeTurkish(text)
        val weatherKeywords = listOf(
            "hava", "yagmur", "yagis", "sicaklik", "derece", "nasil",
            "gunes", "kar", "firtina", "bulut", "nem", "ruzgar", "tahmin", "raporu"
        )
        val activityKeywords = listOf(
            "disari", "uygun", "yapabilirim", "piknik", "kosu", "bisiklet",
            "kamp", "mangal", "yuruyus", "cocuk", "deniz", "sort", "giysi",
            "kiyafet", "giy", "seyahat", "gezi", "rota"
        )

        return weatherKeywords.any { normalizedInput.contains(it) } ||
               activityKeywords.any { normalizedInput.contains(it) } ||
               (detectCity(text) != null)
    }
}
