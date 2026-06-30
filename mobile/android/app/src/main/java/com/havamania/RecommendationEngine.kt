package com.havamania

import com.havamania.ui.theme.AssistantTone
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object RecommendationEngine {

    /**
     * Günlük hava durumu önerisi oluşturur - Kişiselleştirme ve Sağlık Hassasiyeti eklendi.
     */
    fun generateTodayRecommendation(
        weatherData: WeatherData,
        userInterests: Set<String> = emptySet(),
        aboutMe: String? = null
    ): HavamaniaRecommendation {
        val todayDaily = weatherData.dailyForecast.firstOrNull { it.isToday } ?: weatherData.dailyForecast.firstOrNull()
        val tempMin = todayDaily?.minTemp ?: 10
        val tempMax = todayDaily?.maxTemp ?: 20

        val rawCondition = weatherData.condition.lowercase(Locale("tr"))
        val city = weatherData.cityName
        val uvIndex = weatherData.uvIndex ?: 0
        val windSpeed = weatherData.windSpeed ?: 0.0
        val precipProb = weatherData.precipitationProbability ?: 0
        val humidity = weatherData.humidity ?: 50
        val aboutMeLower = aboutMe?.lowercase(Locale("tr")) ?: ""

        val highlights = mutableListOf<String>()
        var primaryType = RecommendationType.GENERAL
        var priority = RecommendationPriority.LOW

        // 1. GİRİŞ (Hava Durumu Özeti)
        val intro = when {
            rawCondition.contains("yağmur") || rawCondition.contains("sağanak") -> {
                primaryType = RecommendationType.WARNING
                priority = RecommendationPriority.HIGH
                "Bugün $city semaları hareketli. Yağış geçişleri günün temposunu belirleyecek. $tempMin°/$tempMax° sıcaklık aralığında, nemin etkisiyle hava olduğundan ağır hissedilebilir."
            }
            rawCondition.contains("kar") -> {
                primaryType = RecommendationType.WARNING
                priority = RecommendationPriority.CRITICAL
                "Dışarısı masalsı ama soğuk! ❄️ $city'da kar yağışı etkisini gösterecek. Sıcaklık $tempMin° civarında, buzlanmaya karşı dikkatli olmalısın."
            }
            rawCondition.contains("bulut") -> {
                "Bugün $city'da bulutlar hakim. Güneş kısıtlı olsa da $tempMax° sıcaklık dışarıda vakit geçirmek için oldukça dengeli bir ortam sunuyor."
            }
            rawCondition.contains("güneş") || rawCondition.contains("açık") -> {
                "Pırıl pırıl bir gün $city’da seni bekliyor. Gökyüzü tamamen açık. $tempMax°'lik maksimum sıcaklık, mevsime uygun harika bir atmosfer yaratıyor."
            }
            else -> "Bugün $city’da $rawCondition bir hava hakim. $tempMin° ile $tempMax° arasında seyreden termometreler, planlarını yapmak için oldukça stabil görünüyor."
        }

        val analysisParts = mutableListOf<String>()

        // 2. SAĞLIK VE YAŞAM ETKİSİ
        if (uvIndex >= 6 || aboutMeLower.contains("güneş") || aboutMeLower.contains("cilt") || userInterests.contains("Sağlık")) {
            val uvAdvice = if (uvIndex >= 8) "UV çok yüksek! 11:00-16:00 arası dışarıda kalmamaya çalış." else "Güneş koruyucunu ihmal etme."
            analysisParts.add("UV indeksi $uvIndex seviyesinde. $uvAdvice")
            highlights.add("UV")
            if (uvIndex >= 6) priority = maxOf(priority, RecommendationPriority.MEDIUM)
        }

        if (humidity > 70 && tempMax > 25) {
            analysisParts.add("Yüksek nem (%$humidity) nedeniyle hava daha bunaltıcı hissedilebilir. Sıvı tüketimine dikkat!")
            highlights.add("nem")
        }

        if (precipProb > 30) {
            analysisParts.add("Yağış ihtimali %$precipProb. Planlarında esnek olman önemli.")
            highlights.add("yağış")
        }

        // 3. ÖNERİLER (Giyim, Aktivite, İlgi Alanları)
        val suggestions = mutableListOf<String>()

        // Giyim Önerisi
        val clothingAdvice = when {
            tempMax > 28 -> "👕 İnce, pamuklu ve açık renkli kıyafetler seçmelisin."
            tempMax < 14 -> "🧥 Hava serin; katmanlı giyinmek (layering) en iyi korumayı sağlar."
            precipProb > 40 -> "☂️ Sürpriz yağışlara karşı yanına mutlaka bir şemsiye almalısın."
            else -> "🧥 Hafif bir ceketle dışarı çıkmak, değişen koşullara uyum sağlamanı kolaylaştırır."
        }
        suggestions.add(clothingAdvice)

        // İlgi Alanına Göre Kişiselleştirme
        if (userInterests.contains("Spor") || userInterests.contains("Koşu")) {
            if (precipProb > 50 || windSpeed > 35) {
                suggestions.add("🏃‍♂️ Dışarısı spor için zorlayıcı. Antrenmanını bugün kapalı alana taşımaya ne dersin?")
            } else if (tempMax in 15..24) {
                suggestions.add("🏃‍♂️ Koşu için mükemmel bir hava! Performansın için koşullar çok uygun.")
            }
        }

        if (userInterests.contains("Kayak") || userInterests.contains("Kış Sporları")) {
            if (rawCondition.contains("kar") && tempMax < 2) {
                suggestions.add("⛷️ Pistler seni çağırıyor! Kar kalitesi ve sıcaklık kış sporları için harika.")
            }
        }

        if (aboutMeLower.contains("çocuk") || aboutMeLower.contains("aile")) {
            if (precipProb < 20 && uvIndex < 6 && tempMax > 18) {
                suggestions.add("👨‍👩‍👧‍👦 Çocuklarla dışarıda vakit geçirmek için harika bir gün.")
            }
        }

        if (userInterests.contains("Kamp") || userInterests.contains("Doğa") || userInterests.contains("Outdoor")) {
            if (precipProb < 20 && windSpeed < 25) {
                suggestions.add("🏕️ Doğa yürüyüşü için koşullar çok davetkar. Taze havanın tadını çıkar.")
            }
        }

        val analysisSection = if (analysisParts.isNotEmpty()) "\n\n⚠️ DİKKAT:\n• " + analysisParts.joinToString("\n• ") else ""
        val suggestionsSection = if (suggestions.isNotEmpty()) "\n\n💡 ÖNERİLER:\n• " + suggestions.joinToString("\n• ") else ""

        val finalMessage = "$intro$analysisSection$suggestionsSection"

        return HavamaniaRecommendation(
            message = finalMessage,
            type = primaryType,
            highlightedWords = highlights.distinct(),
            priority = priority
        )
    }

    /**
     * AI Asistan Yanıtları - Kişiselleştirme ve Tonlama
     */
    fun generateAssistantFallbackReply(
        userPrompt: String,
        weatherData: WeatherData?,
        aboutMe: String? = null,
        interests: Set<String> = emptySet(),
        tone: AssistantTone = AssistantTone.DENGELI,
        travelPlans: List<TravelPlan> = emptyList()
    ): String {
        if (weatherData == null) {
            return when (tone) {
                AssistantTone.SAMIMI -> "Şu an hava durumuna bakamıyorum canım, birazdan tekrar sorarsan kesin hallederiz!"
                AssistantTone.RESMI -> "Meteorolojik verilere şu an ulaşılamamaktadır. Lütfen daha sonra tekrar deneyiniz."
                AssistantTone.KISA_NET -> "Bağlantı hatası. Veri çekilemedi."
                else -> "Hava durumu verilerine şu an erişilemiyor. Lütfen tekrar deneyin."
            }
        }

        val intent = AiIntentParser.detectIntent(userPrompt)
        val city = weatherData.cityName
        val temp = weatherData.temperature
        val feelsLike = weatherData.feelsLike

        val tempVal = temp.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 20
        val feelsLikeVal = feelsLike.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: tempVal
        val uv = weatherData.uvIndex ?: 0
        val precip = weatherData.precipitationProbability ?: 0
        val windSpeed = weatherData.windSpeed ?: 0.0

        return when (intent) {
            AiIntent.CLOTHING -> generateClothingReply(tempVal, feelsLikeVal, uv, precip, windSpeed, tone, interests)
            AiIntent.ACTIVITY -> generateActivityReply(city, tempVal, uv, precip, windSpeed, tone, interests)
            AiIntent.TRAVEL -> generateTravelReply(userPrompt, weatherData, travelPlans, tone)
            AiIntent.PACKING -> generatePackingReply(tempVal, precip, windSpeed, tone)
            AiIntent.CALENDAR -> generateCalendarReply(weatherData, travelPlans, tone)
            AiIntent.WEEKEND_FORECAST -> generateWeekendReply(weatherData, tone)
            AiIntent.TRIP_RISK -> generateTripRiskReply(weatherData, tone)
            AiIntent.GENERAL_WEATHER -> generateGeneralWeatherReply(weatherData, tone)
            else -> generateGeneralWeatherReply(weatherData, tone)
        }
    }

    private fun generateClothingReply(temp: Int, feels: Int, uv: Int, precip: Int, wind: Double, tone: AssistantTone, interests: Set<String>): String {
        val upperItems = mutableListOf<String>()
        val lowerItems = mutableListOf<String>()
        val accessoryItems = mutableListOf<String>()

        when {
            temp > 28 -> {
                upperItems.add("ince pamuklu tişört")
                lowerItems.add("şort veya keten pantolon")
                accessoryItems.add("güneş gözlüğü")
                if (uv > 5) accessoryItems.add("şapka")
            }
            temp > 20 -> {
                upperItems.add("polo yaka tişört")
                lowerItems.add("hafif kot veya chino pantolon")
                if (wind > 20) accessoryItems.add("ince hırka")
            }
            temp > 12 -> {
                upperItems.add("sweatshirt veya uzun kollu gömlek")
                upperItems.add("hafif ceket")
                lowerItems.add("kot pantolon")
            }
            else -> {
                upperItems.add("termal içlik")
                upperItems.add("kalın kazak")
                upperItems.add("kışlık mont")
                lowerItems.add("kalın pantolon")
                accessoryItems.add("atkı ve bere")
            }
        }

        if (precip > 30) accessoryItems.add("şemsiye")

        val result = "Sıcaklık $temp° (hissedilen $feels°). Kombin önerim:\n• Üst: ${upperItems.joinToString(", ")}\n• Alt: ${lowerItems.joinToString(", ")}\n• Aksesuar: ${accessoryItems.joinToString(", ")}"

        return when (tone) {
            AssistantTone.SAMIMI -> "Hava $temp derece canım! 😊 Şöyle rahat bir şeyler giyebilirsin:\n" + result
            AssistantTone.RESMI -> "Güncel sıcaklık $temp°C'dir. Konforunuz için şu giyim unsurları önerilir:\n" + result
            AssistantTone.KISA_NET -> "Giyim ($temp°):\n" + result
            else -> result
        }
    }

    private fun generateActivityReply(city: String, temp: Int, uv: Int, precip: Int, wind: Double, tone: AssistantTone, interests: Set<String>): String {
        val activities = mutableListOf<String>()

        if (precip < 20 && temp in 18..28) activities.add("🧺 Piknik")
        if (wind < 25 && precip < 10) activities.add("🚲 Bisiklet sürüşü")
        if (temp in 15..25 && wind < 30) activities.add("🏃 Açık hava koşusu")
        if (temp > 25 && uv < 8) activities.add("🏊 Yüzme / Plaj")
        if (activities.isEmpty()) activities.add("☕ Kapalı alan aktiviteleri (Sinema, Müze)")

        val list = activities.joinToString("\n• ")
        return when (tone) {
            AssistantTone.SAMIMI -> "Bugün $city'de canın ne yapmak istiyor? 😊 Şunlar harika olur:\n• $list"
            AssistantTone.RESMI -> "$city bölgesi için uygun faaliyet analizi:\n• $list"
            else -> "Bugün için önerilen aktiviteler:\n• $list"
        }
    }

    private fun generatePackingReply(temp: Int, precip: Int, wind: Double, tone: AssistantTone): String {
        val items = mutableListOf<String>()
        if (temp > 25) items.add("Güneş kremi, şort, tişört")
        else if (temp > 15) items.add("Ceket, kot pantolon, spor ayakkabı")
        else items.add("Mont, kazak, kalın çorap")

        if (precip > 20) items.add("Şemsiye veya yağmurluk")
        items.add("Powerbank, kişisel bakım ürünleri")

        val list = items.joinToString("\n• ")
        return "Valizine şunları eklemeni öneririm:\n• $list"
    }

    private fun generateCalendarReply(weatherData: WeatherData, travelPlans: List<TravelPlan>, tone: AssistantTone): String {
        if (travelPlans.isEmpty()) return "Takviminizde henüz planlı bir etkinlik bulunmuyor."
        val result = travelPlans.take(3).joinToString("\n") { "• ${it.startDate}: ${it.city} seyahati" }
        return "Yaklaşan seyahat planlarınız:\n$result"
    }

    private fun generateGeneralWeatherReply(weatherData: WeatherData, tone: AssistantTone): String {
        val city = weatherData.cityName
        val temp = weatherData.temperature
        val cond = weatherData.condition.lowercase(Locale("tr"))
        val feels = weatherData.feelsLike

        return when (tone) {
            AssistantTone.SAMIMI -> "Canım, $city'de hava şu an $temp derece ve $cond. 🌤️ Ama nemden dolayı $feels gibi hissediliyor. Güzel bir gün geçir! 😊"
            AssistantTone.RESMI -> "$city raporu: Sıcaklık $temp, hissedilen $feels. Gökyüzü $cond."
            AssistantTone.KISA_NET -> "$city: $temp ($cond). His: $feels."
            else -> "$city'de hava $temp derece ve $cond. Hissedilen sıcaklık ise $feels."
        }
    }

    private fun generateTravelReply(prompt: String, weatherData: WeatherData, travelPlans: List<TravelPlan>, tone: AssistantTone): String {
        val detectedCity = AiIntentParser.detectCity(prompt)
        val city = detectedCity ?: weatherData.cityName
        return "$city seyahatin için hava koşulları elverişli görünüyor. Yolculuk öncesi valiz hazırlığına başlamadan tekrar sorabilirsin."
    }

    private fun generateWeekendReply(weatherData: WeatherData, tone: AssistantTone): String {
        val sat = weatherData.dailyForecast.find { it.day.contains("Cumartesi", true) }
        val sun = weatherData.dailyForecast.find { it.day.contains("Pazar", true) }
        if (sat == null || sun == null) return "Hafta sonu verileri henüz netleşmedi."
        return "Hafta sonu tahmini:\nCumartesi: ${sat.maxTemp}°\nPazar: ${sun.maxTemp}°"
    }

    private fun generateTripRiskReply(weatherData: WeatherData, tone: AssistantTone): String {
        val precip = weatherData.precipitationProbability ?: 0
        if (precip > 50) return "⚠️ Dikkat: Güzergah üzerinde yağış riski var. Sürüş güvenliğine dikkat edin."
        return "✅ Seyahat rotası üzerinde şu an için bir risk görünmüyor."
    }

    /**
     * Gelişmiş Seyahat Önerisi - Karşılaştırma ve Detaylı Analiz
     */
    fun generateTravelRecommendation(
        plan: TravelPlan,
        currentSnapshot: ForecastSnapshot?,
        previousSnapshot: ForecastSnapshot? = null
    ): String {
        if (currentSnapshot == null) return "${plan.city} seyahati için hazırlıklar başlasın! ✈️"

        val cond = currentSnapshot.conditionSummary?.lowercase(Locale("tr")) ?: "değişken"
        val maxT = currentSnapshot.maxTemp?.toInt() ?: 20
        val rain = currentSnapshot.precipitationProbability ?: 0

        var message = "${plan.city} seyahatin yaklaşıyor! Seni $maxT° sıcaklıkta $cond bir hava bekliyor."

        // 1. Karşılaştırmalı Analiz (Trend)
        if (previousSnapshot != null) {
            val prevMaxT = previousSnapshot.maxTemp?.toInt() ?: maxT
            val tempDiff = maxT - prevMaxT
            val rainDiff = rain - (previousSnapshot.precipitationProbability ?: rain)

            val trendText = when {
                tempDiff > 2 -> "\n📈 Önceki analize göre hava $tempDiff° ısınıyor."
                tempDiff < -2 -> "\n📉 Önceki analize göre hava ${-tempDiff}° soğuyor."
                else -> ""
            }
            message += trendText

            if (rainDiff > 15) message += " Yağış ihtimali artmış görünüyor, hazırlıklı ol! 🌧️"
        }

        // 2. Yaşam Etkisi ve Tavsiyeler
        message += when {
            rain > 50 -> "\n☔ Yağış ihtimali yüksek. Şehir turu için müze ve kapalı çarşı gibi noktaları önceliklendirmen iyi olur."
            maxT > 30 -> "\n☀️ Oldukça sıcak bir hava. Valizine mutlaka güneş koruyucu ve şapka ekle."
            maxT < 10 -> "\n❄️ Hava serin. Kalın bir mont ve termal katmanlar konforun için şart."
            else -> "\n✨ Hava gezmek için harika! Rahat bir spor ayakkabı ile şehrin tadını çıkar."
        }

        // 3. Valiz / Eşya Tavsiyesi
        val items = mutableListOf<String>()
        if (rain > 30) items.add("şemsiye")
        if (maxT < 15) items.add("hafif ceket")
        if (maxT > 25) items.add("güneş gözlüğü")
        items.add("powerbank")

        if (items.isNotEmpty()) {
            message += "\n\n🎒 Valiz İpucu: Yanına mutlaka ${items.joinToString(", ")} almalısın."
        }

        return message
    }
}
