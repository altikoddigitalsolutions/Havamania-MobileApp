package com.havamania

import com.havamania.ui.theme.AssistantTone
import java.time.LocalTime
import java.util.Locale
import kotlin.random.Random

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

    fun generateTravelRecommendation(
        plan: TravelPlan,
        forecastSnapshot: ForecastSnapshot?
    ): String {
        if (forecastSnapshot == null) return "${plan.city} seyahati için hazırlıklar başlasın! Hava durumu netleşince meteorolojik verileri senin için anlamlandıracağım."
        val cond = forecastSnapshot.conditionSummary?.lowercase(Locale("tr")) ?: "değişken"
        val maxT = forecastSnapshot.maxTemp?.toInt() ?: 20
        val rain = forecastSnapshot.precipitationProbability ?: 0
        val wind = forecastSnapshot.windSpeed ?: 0.0

        val base = "${plan.city} seyahatinde seni $cond bir hava bekliyor. Maksimum $maxT° sıcaklık öngörülüyor."
        val analysis = when {
            rain > 60 -> "Yağış ihtimali oldukça baskın görünüyor, bu durum şehir turu planlarını kapalı mekanlara kaydırmanı gerektirebilir."
            maxT > 30 -> "Yüksek sıcaklıklar gün içinde çabuk yorulmana neden olabilir; su tüketimini artırmalı ve öğle saatlerinde gölgeyi tercih etmelisin."
            wind > 30 -> "Belirgin rüzgar hızı, özellikle sahil kesimlerinde havayı olduğundan daha serin hissettirebilir."
            else -> "Hava koşulları genel seyahat konforu açısından oldukça ideal bir seviyede seyrediyor."
        }
        val advice = when {
            rain > 40 -> "Valizine mutlaka kompakt bir şemsiye ve su geçirmeyen bir ayakkabı eklemelisin."
            maxT < 12 -> "Hava serin olacağı için valizinde kalın bir mont ve atkı/bere ikilisine yer açman önemli."
            else -> "Rahat bir yürüyüş ayakkabısı ve katmanlı giysiler bu seyahatin olmazsa olmazı olacaktır."
        }
        return "$base $analysis $advice"
    }

    fun generateAssistantFallbackReply(
        userPrompt: String,
        weatherData: WeatherData?,
        aboutMe: String? = null,
        interests: Set<String> = emptySet(),
        tone: AssistantTone = AssistantTone.DENGELI
    ): String {
        val prompt = userPrompt.lowercase(Locale("tr"))
        if (weatherData == null) {
            return when (tone) {
                AssistantTone.SAMIMI -> "Şu an hava durumuna bakamıyorum canım, birazdan tekrar sorarsan kesin hallederiz!"
                AssistantTone.RESMI -> "Meteorolojik verilere şu an ulaşılamamaktadır. Lütfen daha sonra tekrar deneyiniz."
                AssistantTone.KISA_NET -> "Veri bağlantısı hatası."
                else -> "Hava durumu verilerine şu an erişilemiyor. Lütfen tekrar deneyin."
            }
        }

        val city = weatherData.cityName
        val temp = weatherData.temperature
        val cond = weatherData.condition.lowercase(Locale("tr"))
        val precip = weatherData.precipitationProbability ?: 0
        val wind = weatherData.windSpeed ?: 0.0
        val uv = weatherData.uvIndex ?: 0
        val humidity = weatherData.humidity ?: 50
        val tempVal = temp.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 20

        // Determination of suitability for various activities
        val isOutdoorSuitable = precip < 30 && wind < 30 && tempVal in 15..32
        val isRainy = precip > 40
        val isVeryHot = tempVal > 30
        val isVeryCold = tempVal < 10

        // 1. CONCLUSION (What the user wants to know first)
        val conclusion = when {
            prompt.contains("uygun") || prompt.contains("yapabilirim") || prompt.contains("çıkılır") || prompt.contains("olur mu") -> {
                if (isOutdoorSuitable) "Evet, bugün dışarı çıkmak ve aktiviteler için oldukça harika bir hava var!"
                else if (isRainy) "Bugün dışarıdaki planların için pek uygun bir hava görünmüyor, yağış ihtimali yüksek."
                else if (isVeryHot) "Dışarı çıkabilirsin ama hava oldukça sıcak, dikkatli olmanda fayda var."
                else "Dışarı çıkmak için koşullar biraz zorlayıcı olabilir."
            }
            prompt.contains("piknik") || prompt.contains("mangal") -> {
                if (isOutdoorSuitable && !isRainy) "Harika fikir! Piknik ve mangal için hava şu an çok müsait."
                else "Bugün piknik veya mangal planlarını ertelemek daha güvenli olabilir."
            }
            prompt.contains("koşu") || prompt.contains("spor") || prompt.contains("yürüyüş") || prompt.contains("bisiklet") -> {
                if (isOutdoorSuitable) "Spor ve yürüyüş için ideal bir hava. Performansın için harika bir gün!"
                else if (isVeryHot) "Spor yapabilirsin ama sıvı tüketimine ve güneşin dik gelmediği saatlere dikkat etmelisin."
                else "Hava koşulları açık havada spor yapmak için biraz sert olabilir."
            }
            prompt.contains("deniz") -> {
                if (isVeryHot && !isRainy && wind < 20) "Deniz keyfi için mükemmel bir gün! Suya atlamak için sabırsızlanıyor olmalısın."
                else "Deniz veya yüzme planı için hava biraz serin veya rüzgarlı olabilir."
            }
            prompt.contains("giys") || prompt.contains("kıyafet") || prompt.contains("şort") || prompt.contains("giy") -> {
                if (isVeryHot) "Kesinlikle şort ve ince tişört zamanı! Hava oldukça sıcak."
                else if (isVeryCold) "Sıkı giyinmelisin, dışarısı oldukça soğuk."
                else "Hafif bir ceket veya hırka alarak dengeli bir seçim yapabilirsin."
            }
            else -> "Bugün $city semalarında bizi ${cond.replaceFirstChar { it.uppercase() }} bir gökyüzü bekliyor."
        }

        // 2. EXPLANATION (The data behind the conclusion)
        val explanation = when (tone) {
            AssistantTone.SAMIMI -> "Şöyle ki; termometreler $temp dereceyi gösteriyor ve gökyüzü ${cond}. Nem oranı da %$humidity civarında, yani tam kıvamında!"
            AssistantTone.RESMI -> "$city bölgesinde sıcaklık $temp seviyesinde olup, atmosferik durum ${cond} olarak gözlemlenmektedir. Nem oranı %$humidity seviyesindedir."
            AssistantTone.KISA_NET -> "$temp, $cond. Nem: %$humidity."
            AssistantTone.DETAYLI_UZMAN -> "Meteorolojik analizlere göre $city'da $temp sıcaklık ve %$humidity bağıl nem ölçülmektedir. $wind km/sa hızındaki rüzgar ve $uv seviyesindeki UV indeksi günün karakterini belirliyor."
            else -> "Hava şu an $temp ve ${cond}. Nem oranı %$humidity, yağış ihtimali ise %$precip civarında seyrediyor."
        }

        // 3. SUGGESTIONS (Extra value)
        val suggestion = when {
            isRainy -> "Yanına mutlaka bir şemsiye almanı ve su geçirmeyen bir ayakkabı tercih etmeni öneririm."
            uv > 6 -> "Güneşin etkili olduğu saatlerde güneş koruyucu ve şapka kullanmayı ihmal etme."
            isVeryHot -> "Sıvı tüketimini artırıp mümkün olduğunca gölge alanlarda kalmaya çalışmalısın."
            isOutdoorSuitable -> "Bu güzel havanın tadını çıkarmak için kendine güzel bir yürüyüş rotası belirleyebilirsin!"
            else -> "Günün tadını çıkarmak için planlarını hava durumuna göre esnek tutmanda fayda var."
        }

        // 4. FORMATTING ACCORDING TO TONE
        return when (tone) {
            AssistantTone.SAMIMI -> {
                "Selam! 😊 $conclusion\n\n$explanation $suggestion Kendine çok iyi bak!"
            }
            AssistantTone.RESMI -> {
                "$conclusion\n\n$explanation\n\n$suggestion Bilgilerinize sunulur."
            }
            AssistantTone.KISA_NET -> {
                "$conclusion $explanation $suggestion"
            }
            AssistantTone.DETAYLI_UZMAN -> {
                "ATMOSFERİK ANALİZ RAPORU:\n$conclusion\n\n$explanation\n\nTEKNİK ÖNERİ: $suggestion"
            }
            else -> {
                "$conclusion\n\n$explanation\n\n💡 ÖNERİ: $suggestion"
            }
        }
    }
}
