package com.havamania

import com.havamania.ui.theme.AssistantTone
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object RecommendationEngine {

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

        val intro = when {
            rawCondition.contains("yağmur") || rawCondition.contains("sağanak") -> {
                primaryType = RecommendationType.WARNING
                priority = RecommendationPriority.HIGH
                "Bugün $city semaları hareketli. Yağış geçişleri günün temposunu belirleyecek. $tempMin°/$tempMax° sıcaklık aralığında, nemin etkisiyle hava olduğundan ağır hissedilebilir."
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
        if (uvIndex >= 6 || aboutMeLower.contains("güneş") || aboutMeLower.contains("cilt")) {
            analysisParts.add("UV indeksi $uvIndex seviyesinde. Hassas bir cildin varsa öğle saatlerinde doğrudan güneşten kaçınmalısın.")
            highlights.add("UV")
            if (uvIndex >= 6) priority = RecommendationPriority.MEDIUM
        }
        if (precipProb > 30) {
            analysisParts.add("Yağış ihtimali %$precipProb. Planlarında esnek olman, nemin (%$humidity) etkisiyle oluşabilecek ağır havaya hazırlıklı olman önemli.")
            highlights.add("yağış")
        }

        val suggestions = mutableListOf<String>()
        val clothingAdvice = when {
            tempMax > 28 -> "👕 İnce, pamuklu ve açık renkli kıyafetler gün boyu ferah kalmanı sağlayacaktır."
            tempMax < 14 -> "🧥 Hava serin; katmanlı giyinmek en mantıklısı. Kalın bir dış katman ve içine hafif parçalar seçebilirsin."
            precipProb > 40 -> "☂️ Sürpriz yağışlara karşı yanına mutlaka bir şemsiye veya su geçirmeyen bir ceket almalısın."
            else -> "🧥 Hafif bir ceketle dışarı çıkmak, değişen koşullara uyum sağlamanı kolaylaştırır."
        }
        suggestions.add(clothingAdvice)

        if (userInterests.contains("Spor") || userInterests.contains("Koşu")) {
            if (precipProb > 50 || windSpeed > 35) {
                suggestions.add("🏃‍♂️ Dışarısı spor için zorlayıcı. Antrenmanını bugün kapalı alana taşımaya ne dersin?")
            } else {
                suggestions.add("🏃‍♂️ Koşu için ideal! $windSpeed km/sa rüzgar, performansını destekleyecek taze bir hava sunuyor.")
            }
        }
        if (aboutMeLower.contains("çocuk") || aboutMeLower.contains("aile")) {
            if (precipProb < 20 && uvIndex < 6) suggestions.add("👨‍👩‍👧‍👦 Çocuklarla park veya bahçe planı yapmak için gökyüzü oldukça davetkar.")
        }
        if (userInterests.contains("Kamp") || userInterests.contains("Doğa")) {
            if (precipProb < 20 && windSpeed < 20) suggestions.add("🏕️ Doğa yürüyüşü veya kamp için koşullar harika. Sakin atmosferin tadını çıkarabilirsin.")
        }

        val analysisText = if (analysisParts.isNotEmpty()) analysisParts.joinToString("\n• ") else "Meteorolojik veriler bugün için oldukça dengeli bir tablo çiziyor."
        val suggestionsText = if (suggestions.isNotEmpty()) "\n\n💡 ÖNERİLER:\n• " + suggestions.joinToString("\n• ") else ""

        val finalMessage = "$intro\n\n• $analysisText$suggestionsText"

        return HavamaniaRecommendation(
            message = finalMessage,
            type = primaryType,
            highlightedWords = highlights.distinct(),
            priority = priority
        )
    }

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
                AssistantTone.KISA_NET -> "Veri bağlantısı hatası."
                else -> "Hava durumu verilerine şu an erişilemiyor. Lütfen tekrar deneyin."
            }
        }

        val intent = AiIntentParser.detectIntent(userPrompt)
        val city = weatherData.cityName
        val temp = weatherData.temperature
        val feelsLike = weatherData.feelsLike
        val cond = weatherData.condition.lowercase(Locale("tr"))
        val precip = weatherData.precipitationProbability ?: 0
        val windSpeed = weatherData.windSpeed ?: 0.0
        val uv = weatherData.uvIndex ?: 0
        val humidity = weatherData.humidity ?: 50

        val tempVal = temp.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 20
        val feelsLikeVal = feelsLike.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: tempVal

        return when (intent) {
            AiIntent.CLOTHING -> generateClothingReply(tempVal, feelsLikeVal, uv, precip, windSpeed, tone, interests)
            AiIntent.ACTIVITY -> generateActivityReply(city, tempVal, uv, precip, windSpeed, tone, interests)
            AiIntent.TRAVEL -> generateTravelReply(userPrompt, weatherData, travelPlans, tone)
            AiIntent.PACKING -> generatePackingReply(tempVal, precip, windSpeed, tone)
            AiIntent.CALENDAR -> generateCalendarReply(weatherData, travelPlans, tone)
            AiIntent.WEEKEND_FORECAST -> generateWeekendReply(weatherData, tone)
            AiIntent.TRIP_RISK -> generateTripRiskReply(weatherData, tone)
            AiIntent.TRIP_TIMING -> generateTripTimingReply(weatherData, tone)
            AiIntent.TRIP_ROUTE -> generateTripRouteReply(weatherData, tone)
            AiIntent.OUTDOOR_EVENT -> generateOutdoorEventReply(weatherData, tone)
            AiIntent.CHAT -> generateChatReply(tone)
            AiIntent.GENERAL_WEATHER -> generateGeneralWeatherReply(weatherData, tone)
        }
    }

    private fun generateClothingReply(temp: Int, feels: Int, uv: Int, precip: Int, wind: Double, tone: AssistantTone, interests: Set<String>): String {
        val upperItems = mutableListOf<String>()
        val lowerItems = mutableListOf<String>()
        val accessoryItems = mutableListOf<String>()

        when {
            temp > 28 -> {
                upperItems.add("İnce pamuklu tişört")
                lowerItems.add("Şort veya keten pantolon")
                accessoryItems.add("Güneş gözlüğü")
                if (uv > 5) accessoryItems.add("Şapka")
            }
            temp > 20 -> {
                upperItems.add("Polo yaka veya basic tişört")
                lowerItems.add("Hafif kot veya chino pantolon")
                if (wind > 20) accessoryItems.add("İnce bir hırka (rüzgar için)")
            }
            temp > 12 -> {
                upperItems.add("Sweatshirt veya uzun kollu gömlek")
                upperItems.add("Hafif ceket")
                lowerItems.add("Kot pantolon")
            }
            else -> {
                upperItems.add("Termal içlik")
                upperItems.add("Kalın kazak")
                upperItems.add("Kaşe kaban veya şişme mont")
                lowerItems.add("Kalın kanvas pantolon")
                accessoryItems.add("Atkı ve bere")
            }
        }

        if (precip > 30) accessoryItems.add("Şemsiye veya yağmurluk")

        return when (tone) {
            AssistantTone.SAMIMI -> {
                "Bugün hava $temp derece canım! 😊 Bence şöyle tatlı bir kombin yapabilirsin:\n" +
                "👕 Üst: ${upperItems.firstOrNull()}\n" +
                "👖 Alt: ${lowerItems.firstOrNull()}\n" +
                "🕶️ Aksesuar: ${accessoryItems.joinToString(", ")}\n" +
                "Dışarısı ${if(precip > 30) "biraz ıslak olabilir, hazırlıklı çık!" else "mis gibi, tadını çıkar!"}"
            }
            AssistantTone.RESMI -> {
                "Sıcaklık değeri $temp°C olarak ölçülmüştür. Önerilen kıyafet listesi:\n" +
                "- Üst Giyim: ${upperItems.joinToString(", ")}\n" +
                "- Alt Giyim: ${lowerItems.joinToString(", ")}\n" +
                "- Aksesuarlar: ${accessoryItems.joinToString(", ")}\n" +
                "Hava koşullarına uygun seçim yapmanız konforunuz açısından önemlidir."
            }
            AssistantTone.KISA_NET -> {
                "Giyim Önerisi ($temp°):\n" +
                "• ${upperItems.first()}\n" +
                "• ${lowerItems.first()}\n" +
                "• ${accessoryItems.joinToString(", ")}"
            }
            AssistantTone.DETAYLI_UZMAN -> {
                "Termal denge analizi: $temp°C sıcaklık ve $wind km/sa rüzgar hızı 'hissedilen' sıcaklığı $feels°C seviyesine çekiyor.\n" +
                "Öneri:\n" +
                "1. Baz Katman: Nem emici pamuklu ürünler.\n" +
                "2. Isı Katmanı: ${upperItems.filter { it.contains("kazak") || it.contains("sweat") }.joinToString() ?: "Hafif üstler"}.\n" +
                "3. Koruyucu Katman: ${if(wind > 15) "Rüzgar kesici ceket" else "Standart dış katman"}.\n" +
                "UV indeksinin $uv olması nedeniyle radyasyon koruması (gözlük/krem) teknik olarak zorunludur."
            }
            else -> "Sıcaklık $temp°C. Rahat bir gün için şunları giyebilirsin:\n" +
                    "- ${upperItems.joinToString()}\n" +
                    "- ${lowerItems.joinToString()}\n" +
                    "- ${accessoryItems.joinToString()}"
        }
    }

    private fun generateActivityReply(city: String, temp: Int, uv: Int, precip: Int, wind: Double, tone: AssistantTone, interests: Set<String>): String {
        val activities = mutableListOf<String>()

        // Suitability Scores (0-10)
        val runScore = if (precip > 40 || temp > 32 || temp < 5) 2 else 9
        val cycleScore = if (wind > 35 || precip > 30) 3 else 8
        val picnicScore = if (precip > 10 || temp < 18 || wind > 25) 1 else 10
        val campScore = if (precip > 20 || temp < 10 || wind > 30) 2 else 9
        val beachScore = if (temp < 25 || precip > 5) 0 else 10

        if (runScore > 7) activities.add("🏃 Koşu/Yürüyüş (İdeal)")
        if (cycleScore > 7) activities.add("🚲 Bisiklet (Uygun)")
        if (picnicScore > 7) activities.add("🧺 Piknik (Mükemmel)")
        if (beachScore > 7) activities.add("🏖️ Deniz/Plaj (Harika)")
        if (activities.isEmpty()) activities.add("☕ Kapalı alan aktiviteleri (Kafe, Müze, Sinema)")

        return when (tone) {
            AssistantTone.SAMIMI -> {
                if (picnicScore > 8) "Selam! Bugün dışarısı tam piknik havası, sepetini hazırla! 😊"
                else if (runScore > 8) "Hava koşulları harika, kısa bir yürüyüş sana çok iyi gelir dostum. 👟"
                else "Canım bugün hava biraz kaprisli 😕 Dışarıda yorulmak yerine güzel bir kahve eşliğinde kitap okumaya ne dersin?"
            }
            AssistantTone.RESMI -> {
                "Meteorolojik veriler ışığında $city bölgesi etkinlik analizi:\n" +
                "${activities.joinToString("\n")}\n" +
                "Dış mekan faaliyetlerinde $wind km/sa rüzgar hızının dikkate alınması tavsiye edilir."
            }
            AssistantTone.KISA_NET -> {
                "Aktivite Uygunluğu:\n" +
                activities.take(3).joinToString("\n")
            }
            AssistantTone.DETAYLI_UZMAN -> {
                "Biyometeorolojik Analiz:\n" +
                "- Koşu Endeksi: $runScore/10\n" +
                "- Statik Aktivite (Piknik): $picnicScore/10\n" +
                "- Isıl Konfor: ${if(temp in 18..24) "Optimal" else "Riskli"}\n" +
                "Analiz: Düşük rüzgar ($wind km/sa) ve $uv UV indeksi, aerobik aktiviteler için yüksek verimlilik sunmaktadır."
            }
            else -> "Bugün için önerilen aktiviteler:\n${activities.joinToString("\n")}"
        }
    }

    private fun generatePackingReply(temp: Int, precip: Int, wind: Double, tone: AssistantTone): String {
        val list = mutableListOf<String>()
        if (temp > 25) {
            list.add("3 adet kısa kollu tişört")
            list.add("Şort ve ince pantolon")
            list.add("Güneş kremi ve gözlük")
        } else if (temp > 15) {
            list.add("2 tişört, 1 sweatshirt")
            list.add("Hafif ceket")
            list.add("Rahat kot pantolon")
        } else {
            list.add("Kalın mont")
            list.add("Termal çorap")
            list.add("Kazak ve hırka")
        }

        if (precip > 20) list.add("Kompakt şemsiye")
        if (wind > 30) list.add("Rüzgarlık / Su geçirmez ceket")

        list.add("Powerbank ve şarj aletleri")
        list.add("Kişisel bakım kiti")

        val packingList = list.joinToString("\n") { "✓ $it" }

        return when (tone) {
            AssistantTone.SAMIMI -> "Valizine şunları kesin almalısın canım:\n\n$packingList\n\nŞimdiden iyi yolculuklar! 😘"
            AssistantTone.RESMI -> "Seyahatiniz öncesinde hazırlanması gereken valiz içeriği aşağıda sunulmuştur:\n\n$packingList\n\nVerimli bir yolculuk dileriz."
            AssistantTone.KISA_NET -> "Valiz Listesi:\n$packingList"
            AssistantTone.DETAYLI_UZMAN -> "Beklenen $temp°C sıcaklık ve %$precip yağış ihtimaline dayalı valiz listesi:\n\n$packingList\n\nBu seçimler biyometeorolojik konforu maksimize edecektir."
            else -> "Hava koşullarına göre valizine şunları koymanı öneririm:\n\n$packingList"
        }
    }

    private fun generateCalendarReply(weatherData: WeatherData, travelPlans: List<TravelPlan>, tone: AssistantTone): String {
        if (travelPlans.isEmpty()) {
            return when(tone) {
                AssistantTone.SAMIMI -> "Takviminde henüz bir seyahat veya etkinlik göremiyorum canım. Yeni bir plan eklediğinde hemen analiz ederim! 😊"
                AssistantTone.RESMI -> "Sistemde kayıtlı bir etkinlik veya seyahat planı bulunmamaktadır."
                AssistantTone.KISA_NET -> "Takvimde etkinlik bulunamadı."
                else -> "Takviminizde yaklaşan bir etkinlik bulunmuyor."
            }
        }

        val today = LocalDate.now()
        val analysisResults = travelPlans.take(3).map { plan ->
            val forecast = weatherData.dailyForecast.find { it.date == plan.startDate.toString() }
            val daysUntil = ChronoUnit.DAYS.between(today, plan.startDate).toInt()

            val riskScore = when {
                forecast == null -> -1
                forecast.weatherCode >= 95 -> 90
                forecast.weatherCode >= 80 -> 70
                forecast.weatherCode >= 51 -> 40
                forecast.maxTemp > 35 || forecast.maxTemp < 0 -> 30
                else -> 10
            }

            val status = when {
                riskScore == -1 && daysUntil > 15 -> "Uzak Tarih (Plan Hazır)"
                riskScore == -1 -> "Veri Bekleniyor"
                riskScore >= 70 -> "Yüksek Risk ⚠️"
                riskScore >= 40 -> "Orta Risk"
                else -> "Düşük Risk ✅"
            }

            val suggestion = if (riskScore == -1 && daysUntil > 15) {
                " (Hava durumu yaklaştığında analiz edilecektir)"
            } else if (riskScore >= 40) {
                val betterDay = weatherData.dailyForecast.find { it.weatherCode <= 3 && it.maxTemp in 15..28 }
                if (betterDay != null) " (Öneri: ${betterDay.day} gününe kaydırılabilir)" else ""
            } else ""

            "• ${plan.startDate}: ${plan.city} (${plan.tripType.label}) -> $status$suggestion"
        }

        val intro = "Takviminizdeki etkinliklerin meteorolojik risk analizi:"
        val resultList = analysisResults.joinToString("\n")

        val summary = when {
            analysisResults.any { it.contains("Yüksek Risk") } -> "Kritik: Bazı planlarınız hava koşulları nedeniyle yüksek risk taşıyor."
            analysisResults.any { it.contains("Orta Risk") } -> "Bilgi: Etkinliklerinizde yağış ihtimali nedeniyle esneklik gerekebilir."
            analysisResults.any { it.contains("Uzak Tarih") } -> "Not: Gelecek tarihler için şimdilik sadece plan kaydınız hazırlandı."
            else -> "Sonuç: Mevcut planlarınız için hava koşulları oldukça elverişli görünüyor."
        }

        return when (tone) {
            AssistantTone.SAMIMI -> "Canım takvimine baktım, durum şöyle:\n\n$resultList\n\n$summary"
            AssistantTone.RESMI -> "$intro\n$resultList\n\n$summary"
            AssistantTone.KISA_NET -> "Takvim Analizi:\n$resultList"
            AssistantTone.DETAYLI_UZMAN -> "SİNOOPTİK TAKVİM OPTİMİZASYONU:\n$resultList\n\n$summary Veriler istasyon bazlı günlük tahminlere dayanmaktadır."
            else -> "$intro\n$resultList\n\n$summary"
        }
    }

    private fun generateWeekendReply(weatherData: WeatherData, tone: AssistantTone): String {
        val sat = weatherData.dailyForecast.find { it.day.contains("Cumartesi", true) }
        val sun = weatherData.dailyForecast.find { it.day.contains("Pazar", true) }

        if (sat == null || sun == null) return "Hafta sonu verileri henüz tam netleşmedi."

        return when (tone) {
            AssistantTone.SAMIMI -> "Hafta sonu planları hazır mı? 😎\n\nCumartesi: ${sat.maxTemp}° ${if(sat.weatherCode > 50) "🌧️" else "☀️"}\nPazar: ${sun.maxTemp}° ${if(sun.weatherCode > 50) "🌧️" else "☀️"}\n\nBence Cumartesi dışarı çıkmak için daha keyifli! 😘"
            AssistantTone.RESMI -> "Hafta sonu tahmini:\n- Cumartesi: Maksimum ${sat.maxTemp}°C, Hava ${if(sat.weatherCode > 50) "yağışlı" else "açık"}.\n- Pazar: Maksimum ${sun.maxTemp}°C, Hava ${if(sun.weatherCode > 50) "yağışlı" else "açık"}."
            AssistantTone.KISA_NET -> "Cmt: ${sat.maxTemp}°, ${if(sat.weatherCode > 50) "Yağmur" else "Açık"}\nPaz: ${sun.maxTemp}°, ${if(sun.weatherCode > 50) "Yağmur" else "Açık"}"
            AssistantTone.DETAYLI_UZMAN -> "Hafta sonu projeksiyonu:\nCumartesi günü ${sat.maxTemp}°C ile termal stabilite korunurken, Pazar günü beklenen ${if(sun.weatherCode > 50) "yağış riski nedeniyle" else "yüksek basınç etkisiyle"} planlar ${if(sun.weatherCode > 50) "kapalı alanlara kaydırılmalıdır" else "açık havada yapılabilir"}."
            else -> "Hafta sonu özeti:\nCumartesi ${sat.maxTemp} derece, Pazar ${sun.maxTemp} derece. ${if(sat.maxTemp > sun.maxTemp) "Cumartesi" else "Pazar"} günü dış aktiviteler için daha uygun."
        }
    }

    private fun generateTripRiskReply(weatherData: WeatherData, tone: AssistantTone): String {
        val precip = weatherData.precipitationProbability ?: 0
        val wind = weatherData.windSpeed ?: 0.0
        val temp = weatherData.temperature.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 20

        val risks = mutableListOf<String>()
        if (precip > 60) risks.add("Şiddetli Yağış (%$precip)")
        if (wind > 45) risks.add("Fırtına ($wind km/sa)")
        if (temp > 35) risks.add("Aşırı Sıcaklık ($temp°C)")
        if (temp < 0) risks.add("Buzlanma ($temp°C)")

        return when (tone) {
            AssistantTone.SAMIMI -> {
                if (risks.isEmpty()) "Yolun açık olsun canım, hava gayet güvenli görünüyor! 😊"
                else "Yolda şunlarla karşılaşabilirsin tatlım: ${risks.joinToString()}. Hazırlıklı olsan iyi olur! ⚠️"
            }
            AssistantTone.RESMI -> {
                if (risks.isEmpty()) "Güzergah üzerinde herhangi bir meteorolojik risk tespit edilmemiştir."
                else "Öngörülen Risk Analizi: ${risks.joinToString()}. İlgili bölgelerde gerekli güvenlik önlemlerinin alınması tavsiye edilir."
            }
            AssistantTone.KISA_NET -> {
                if (risks.isEmpty()) "Risk: Yok."
                else "Riskler: ${risks.joinToString()}"
            }
            AssistantTone.DETAYLI_UZMAN -> {
                "Teknik Risk Analizi: Mevcut $wind km/sa rüzgar hızı ve $precip% yağış olasılığı, ${risks.joinToString().ifEmpty { "stabil bir seyir ortamı" }} oluşturmaktadır."
            }
            else -> "Seyahat risk analizi sonucu: ${risks.joinToString().ifEmpty { "Düşük risk seviyesi" }}."
        }
    }

    private fun generateOutdoorEventReply(weatherData: WeatherData, tone: AssistantTone): String {
        val cond = weatherData.weatherCode
        val precip = weatherData.precipitationProbability ?: 0
        val isSuitable = cond <= 3 && precip < 20

        return when (tone) {
            AssistantTone.SAMIMI -> if(isSuitable) "Etkinlik için hava efsane! 🤩 Kesinlikle kaçırma derim." else "Hava biraz bozacak gibi tatlım (%$precip yağış), kapalı alan seçeneğin olsa iyi olur. ☔"
            AssistantTone.RESMI -> if(isSuitable) "Dış mekan organizasyonu için meteorolojik koşullar uygundur." else "Hava muhalefeti (%$precip yağış) nedeniyle etkinliklerin kapalı alana alınması önerilmektedir."
            AssistantTone.KISA_NET -> if(isSuitable) "Etkinlik için uygun." else "Hava riskli (%$precip yağış), önlem al."
            else -> if(isSuitable) "Hava açık hava etkinlikleri için oldukça elverişli." else "Yağış riski (%$precip) nedeniyle açık hava organizasyonunuzu kapalı bir mekana kaydırmanızı tavsiye ederim."
        }
    }

    private fun generateChatReply(tone: AssistantTone): String {
        return when (tone) {
            AssistantTone.SAMIMI -> "Selam tatlım! 😊 Sana nasıl yardımcı olabilirim? Bugün harika görünüyorsun!"
            AssistantTone.RESMI -> "Merhabalar. Size nasıl yardımcı olabilirim? Hava durumu veya seyahat planlarınız hakkında bilgi alabilirsiniz."
            AssistantTone.KISA_NET -> "Merhaba, dinliyorum."
            AssistantTone.DETAYLI_UZMAN -> "Merhaba. Meteorolojik veri analizi ve seyahat optimizasyonu konularında size destek sağlamaya hazırım. Sorunuzu iletebilirsiniz."
            else -> "Merhaba! Size hava durumu ve planlarınız konusunda yardımcı olabilirim."
        }
    }

    private fun generateTravelReply(prompt: String, weatherData: WeatherData, travelPlans: List<TravelPlan>, tone: AssistantTone): String {
        val detectedCity = AiIntentParser.detectCity(prompt)
        val firstActivePlan = travelPlans.firstOrNull { !it.isArchived }

        val cityToMention = when {
            detectedCity != null -> detectedCity
            firstActivePlan != null -> firstActivePlan.city
            else -> null
        }

        if (cityToMention == null) {
            return when (tone) {
                AssistantTone.SAMIMI -> "Tabii tatlım! Nereye gitmeyi planlıyorsun? Şehri söylersen hemen yardımcı olabilirim. 😊"
                AssistantTone.RESMI -> "Seyahat planlamanıza yardımcı olabilirim. Lütfen varış noktanızı belirtiniz."
                AssistantTone.KISA_NET -> "Hangi şehir için plan yapalım?"
                else -> "Seyahat planlamanız için lütfen gitmek istediğiniz şehri belirtin."
            }
        }

        val temp = weatherData.temperature
        val cond = weatherData.condition.lowercase(Locale("tr"))

        val base = "✈️ $cityToMention seyahatiniz için $temp ve $cond bir hava öngörülüyor."
        val advice = " Yolculuk için en konforlu saatler 08:00-11:00 arasıdır. Valizinizde mutlaka rüzgar kesici bir parçaya yer açın."

        return base + advice
    }

    private fun generateGeneralWeatherReply(weatherData: WeatherData, tone: AssistantTone): String {
        val city = weatherData.cityName
        val temp = weatherData.temperature
        val cond = weatherData.condition.lowercase(Locale("tr"))
        val feels = weatherData.feelsLike

        return when (tone) {
            AssistantTone.SAMIMI -> "Bugün $city'de hava tam kıvamında! 🌤️ $temp derece ama hissedilen $feels. $cond bir gökyüzü eşliğinde günün tadını çıkarabilirsin canım!"
            AssistantTone.RESMI -> "$city bölgesi raporu: Hava $cond olup, sıcaklık $temp, hissedilen sıcaklık ise $feels seviyesindedir."
            AssistantTone.KISA_NET -> "$city: $temp ($cond). Hissedilen: $feels."
            AssistantTone.DETAYLI_UZMAN -> "ANALİZ: $city istasyonunda $cond bir hava hakim. $temp termometre değeri, %${weatherData.humidity ?: 50} nem ile $feels olarak hissediliyor. Koşullar dış aktiviteler için stabil seyrediyor."
            else -> "Bugün $city'de hava $cond. Sıcaklık $temp, hissedilen sıcaklık $feels. Genel olarak dengeli bir gün."
        }
    }

    private fun generateTripTimingReply(weatherData: WeatherData, tone: AssistantTone): String {
        return "🕒 Zamanlama: Yolculuk için en konforlu zaman dilimi 07:00 - 10:00 arasıdır. Bu saatlerde görüş mesafesi açık and sıcaklık dengelidir."
    }

    private fun generateTripRouteReply(weatherData: WeatherData, tone: AssistantTone): String {
        return "🛣️ Güzergah Analizi: Yol boyunca yağış beklenmiyor, asfalt kuru ve sürüş güvenliği açısından ideal bir ortam var."
    }

    fun generateTravelRecommendation(
        plan: TravelPlan,
        forecastSnapshot: ForecastSnapshot?
    ): String {
        if (forecastSnapshot == null) return "${plan.city} seyahati için hazırlıklar başlasın! Hava durumu netleştiğinde rotanı birlikte optimize edeceğiz."
        val cond = forecastSnapshot.conditionSummary?.lowercase(Locale("tr")) ?: "değişken"
        val maxT = forecastSnapshot.maxTemp?.toInt() ?: 20
        val rain = forecastSnapshot.precipitationProbability ?: 0
        val wind = forecastSnapshot.windSpeed ?: 0.0

        val base = "${plan.city} seyahatinde seni $cond bir hava bekliyor. Maksimum $maxT° sıcaklık öngörülüyor."
        val analysis = when {
            rain > 60 -> "Yağış ihtimali baskın olduğu için şehir turu planlarını kapalı mekanlara kaydırman daha güvenli olur."
            maxT > 30 -> "Yüksek sıcaklıklar nedeniyle gün içinde su tüketimini artırmalı ve öğle saatlerinde gölgeyi tercih etmelisin."
            wind > 30 -> "Belirgin rüzgar hızı, özellikle sahil kesimlerinde havayı olduğundan daha serin hissettirecektir."
            else -> "Hava koşulları genel seyahat konforu açısından ideal bir seviyede seyrediyor."
        }
        val advice = when {
            rain > 40 -> "Valizine mutlaka kompakt bir şemsiye ve su geçirmeyen bir ayakkabı ekle."
            maxT < 12 -> "Hava serin olacağı için valizinde kalın bir mont ve atkı/bere ikilisine yer aç."
            else -> "Rahat bir yürüyüş ayakkabısı ve katmanlı giysiler bu seyahatin olmazsa olmazı."
        }
        return "$base $analysis $advice"
    }
}
