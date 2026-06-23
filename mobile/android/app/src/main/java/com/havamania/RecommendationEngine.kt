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
        val feelsLike = weatherData.feelsLike
        val cond = weatherData.condition.lowercase(Locale("tr"))
        val precip = WeatherUtils.formatRainProbability(weatherData.precipitationProbability)
        val windSpeed = weatherData.windSpeed ?: 0.0
        val windDir = WeatherUtils.getWindDirectionFromDegrees(weatherData.windDirectionDegrees)
        val uv = weatherData.uvIndex ?: 0
        val uvLabel = when {
            uv <= 2 -> "(Düşük)"
            uv <= 5 -> "(Orta)"
            uv <= 7 -> "(Yüksek)"
            else -> "(Çok Yüksek)"
        }
        val humidity = WeatherUtils.formatRainProbability(weatherData.humidity)
        val pressure = weatherData.pressure
        val visibility = weatherData.visibilityKm
        val cloudiness = weatherData.cloudCover
        val sunrise = weatherData.sunriseTime
        val sunset = weatherData.sunsetTime

        val tempVal = temp.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 20

        // Determination of suitability for various activities
        val isOutdoorSuitable = (weatherData.precipitationProbability ?: 0) < 30 && windSpeed < 30 && tempVal in 15..32
        val isRainy = (weatherData.precipitationProbability ?: 0) > 40
        val isVeryHot = tempVal > 30

        val basicData = """
            🌡️ Sıcaklık: $temp
            🤚 Hissedilen: $feelsLike
            💨 Rüzgar: $windSpeed km/sa
            🧭 Yön: $windDir
            💧 Nem: $humidity
            ☔ Yağış ihtimali: $precip
            ☀️ UV: $uv $uvLabel
        """.trimIndent()

        val expertData = """
            $basicData
            📈 Basınç: $pressure hPa
            👁️ Görüş mesafesi: $visibility km
            ☁️ Bulutluluk: %$cloudiness
            🌅 Gün doğumu: $sunrise
            🌇 Gün batımı: $sunset
        """.trimIndent()

        val intro = "Bugün $city'de hava ${cond}."

        return when (tone) {
            AssistantTone.SAMIMI -> {
                val comment = if (isOutdoorSuitable) "Harika bir gün seni bekliyor, tadını çıkar! 😊" else "Planlarını yaparken havaya dikkat et canım."
                "Selam! 😊 $intro\n\n$basicData\n\n$comment"
            }
            AssistantTone.RESMI -> {
                val comment = "Meteorolojik veriler doğrultusunda planlamalarınızı yapmanızı öneririz."
                "$intro\n\n$basicData\n\n$comment"
            }
            AssistantTone.DENGELI -> {
                val comment = if (isRainy) "Yağış ihtimaline karşı hazırlıklı olmanızı öneririm." else "Dış mekan aktiviteleri için uygun bir atmosfer hakim."
                "$intro\n\n$basicData\n\n$comment"
            }
            AssistantTone.KISA_NET -> {
                val summary = if (isVeryHot) "Sıcak hava dalgasına dikkat." else "Hava genel olarak stabil."
                "$intro\n\n$basicData\n\n$summary"
            }
            AssistantTone.DETAYLI_UZMAN -> {
                val analysis = "Meteorolojik analiz: UV indeksi ve nem dengesi, hissedilen sıcaklık üzerinde belirleyici bir etkiye sahip. Basınç değerleri atmosferik stabiliteyi koruyor."
                "KAPSAMLI METEOROLOJİK ANALİZ:\n$intro\n\n$expertData\n\n$analysis"
            }
        }
    }
}
