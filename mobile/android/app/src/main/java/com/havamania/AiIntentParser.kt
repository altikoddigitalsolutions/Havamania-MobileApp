package com.havamania

import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    fun detectCity(text: String): String? {
        val lowerText = text.lowercase(Locale("tr"))
        return cities.find { city ->
            lowerText.contains(city.lowercase(Locale("tr")))
        }
    }

    fun detectDate(text: String): LocalDate? {
        val lowerText = text.lowercase(Locale("tr"))
        val today = LocalDate.now()

        if (lowerText.contains("bugün")) return today
        if (lowerText.contains("yarın")) return today.plusDays(1)

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
            val monthStr = match.groupValues[2]
            val month = turkishMonths[monthStr]
            if (day != null && month != null) {
                return try {
                    var date = LocalDate.of(today.year, month, day)
                    if (date.isBefore(today.minusMonths(6))) {
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

    fun isWeatherQuery(text: String): Boolean {
        val lowerText = text.lowercase(Locale("tr"))
        val keywords = listOf("hava", "yağmur", "sıcaklık", "derece", "nasıl", "güneş", "kar")
        return keywords.any { lowerText.contains(it) }
    }
}
