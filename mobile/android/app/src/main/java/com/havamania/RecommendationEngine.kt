package com.havamania

import java.time.LocalTime
import kotlin.random.Random

object RecommendationEngine {

    /**
     * BUGÜN İÇİN DETAYLI, DOĞAL VE SOHBET TADINDA ÖNERİ ÜRETİR.
     * Yaklaşık 180-320 karakter arası, 3 cümlelik yapı.
     */
    fun generateTodayRecommendation(
        weatherData: WeatherData,
        userInterests: Set<String> = emptySet(),
        selectedHour: LocalTime? = null
    ): HavamaniaRecommendation {
        val now = selectedHour ?: LocalTime.now()
        val hour = now.hour

        val todayDaily = weatherData.dailyForecast.firstOrNull { it.isToday } ?: weatherData.dailyForecast.firstOrNull()
        val tempMin = todayDaily?.minTemp ?: 10
        val tempMax = todayDaily?.maxTemp ?: 20

        val hourlyData = weatherData.hourlyForecast.find {
            it.time.startsWith(String.format("%02d", hour))
        }

        val currentTemp = hourlyData?.temp?.filter { it.isDigit() || it == '-' }?.toIntOrNull() ?: tempMax
        val rawCondition = (hourlyData?.condition ?: weatherData.condition).lowercase()

        val condition = when {
            rawCondition.contains("yağmur") || rawCondition.contains("sağanak") -> "yağmurlu"
            rawCondition.contains("bulut") && rawCondition.contains("parça") -> "parçalı bulutlu"
            rawCondition.contains("bulut") -> "bulutlu"
            rawCondition.contains("güneş") || rawCondition.contains("açık") -> "açık"
            else -> rawCondition
        }

        val uvIndex = weatherData.uvIndex ?: 0
        val windSpeed = weatherData.windSpeed ?: 0.0
        val precipProb = hourlyData?.precipProb?.filter { it.isDigit() }?.toIntOrNull() ?: weatherData.precipitationProbability ?: 0
        val visibility = weatherData.visibilityKm ?: 10.0

        val highlights = mutableListOf<String>()
        var primaryType = RecommendationType.GENERAL
        var priority = RecommendationPriority.LOW

        // --- Contextual Recommendation Generation (Turkish) ---
        val message = when {
            // Priority 1: Heavy Weather/Warnings
            precipProb > 45 -> {
                primaryType = RecommendationType.WARNING
                priority = RecommendationPriority.HIGH
                highlights.add("şemsiye")
                if (hour in 18..23) "Bu akşam yağmur bekleniyor. Gece planların için yanına bir şemsiye alman ve su geçirmez katmanlar giymen akıllıca olacaktır."
                else "Önümüzdeki saatlerde yağış bekleniyor. Şemsiyeni unutma ve kuru kalmak için kapalı alan aktivitelerini tercih et."
            }
            windSpeed > 30 -> {
                primaryType = RecommendationType.WARNING
                priority = RecommendationPriority.MEDIUM
                highlights.add("rüzgar")
                "Sert rüzgarlar etkisini artırıyor. Hava olduğundan daha soğuk hissedilebilir, bu yüzden dışarı çıkarken rüzgar kesici bir ceket giymeni öneririm."
            }
            uvIndex > 6 && hour in 11..16 -> {
                primaryType = RecommendationType.HEALTH
                priority = RecommendationPriority.HIGH
                highlights.add("güneş koruması")
                "Şu an UV indeksi oldukça yüksek. Dışarıda vakit geçireceksen güneş kremi kullanmalı, şapka takmalı ve bol su tüketmelisiniz."
            }

            // Priority 2: Time-based lifestyle suggestions
            hour in 5..8 -> {
                primaryType = RecommendationType.OUTDOOR
                highlights.add("sabah yürüyüşü")
                if (condition == "açık") "Harika ve taze bir sabah. Güne taze hava alarak başlamak için canlandırıcı bir sabah yürüyüşü veya erken egzersiz için mükemmel bir zaman."
                else "Sabah havası taze ve sakin. Bulutlara rağmen hafif bir ceketle yapacağın yürüyüş güne güzel bir başlangıç yapmanı sağlayacaktır."
            }
            hour in 9..12 -> {
                primaryType = RecommendationType.COMFORT
                highlights.add("dışarı")
                "Hava dışarıda vakit geçirmek için oldukça konforlu. İşlerini halletmek veya terasta bir sabah kahvesi molası vermek için harika bir zaman."
            }
            hour in 18..21 -> {
                primaryType = RecommendationType.GENERAL
                highlights.add("akşam")
                if (currentTemp > 18) "Altın saatler beraberinde hoş bir sıcaklık getiriyor. Akşam yürüyüşü veya arkadaşlarınla dışarıda bir akşam yemeği için ideal bir zaman."
                else "Akşam havası tatlı bir şekilde serinliyor. Gün batımı atmosferinin tadını çıkarırken hafif bir kazak seni rahat ettirecektir."
            }
            hour in 22..23 || hour in 0..4 -> {
                primaryType = RecommendationType.COMFORT
                highlights.add("serin")
                "Gece çökerken sıcaklıklar düşüyor. Eğer hala dışarıdaysan, serin gece havasında hafif bir ceket seni sıcak tutacaktır."
            }

            // Default based on temp
            currentTemp > 28 -> "Bugün hava oldukça sıcak. Gün boyu serin ve rahat kalmak için keten veya pamuklu gibi nefes alan kumaşları tercih edin."
            currentTemp < 12 -> "Hava soğuk tarafta seyrediyor. Kalın bir palto ve belki bir atkı ile katmanlı giyinmek vücut ısınızı korumanıza yardımcı olur."
            else -> "Hava oldukça ılıman. Gerektiğinde ekleyip çıkarabileceğiniz hafif bir katmanla her türlü küçük değişikliğe hazır olabilirsiniz."
        }

        return HavamaniaRecommendation(
            message = message,
            type = primaryType,
            highlightedWords = highlights,
            priority = priority
        )
    }

    /**
     * SEYAHAT PLANLARINA ÖZEL ANALİZ VE ÖNERİ ÜRETİR.
     */
    fun generateTravelRecommendation(
        plan: TravelPlan,
        forecastSnapshot: ForecastSnapshot?
    ): String {
        return if (forecastSnapshot != null) {
            "${plan.city} seyahati yaklaşıyor. Seyahat gününde hava ${forecastSnapshot.conditionSummary} görünüyor. Valizine uygun kıyafetler eklemeyi unutma."
        } else {
            "${plan.city} seyahati için hazırlıklar başlasın! Hava durumu netleşince sana özel önerilerimi paylaşacağım."
        }
    }

    /**
     * AI BAĞLANTISI KOPARSA YEREL VERİLERLE CEVAP ÜRETİR.
     */
    fun generateAssistantFallbackReply(
        userPrompt: String,
        weatherData: WeatherData?,
        aboutMe: String? = null,
        interests: Set<String> = emptySet()
    ): String {
        val prompt = userPrompt.lowercase()

        if (weatherData == null) {
            return "Hava durumu bilgilerini şu an tam olarak alamadım. Lütfen daha sonra tekrar dene."
        }

        val city = weatherData.cityName
        val temp = weatherData.temperature
        val feelsLike = weatherData.feelsLike
        val cond = weatherData.condition.lowercase()
        val precip = weatherData.precipitationProbability ?: 0
        val wind = weatherData.windSpeed ?: 0.0
        val uv = weatherData.uvIndex ?: 0

        val baseHeader = "Son güncellenen hava verisine göre yardımcı olayım: "
        val currentStatus = "$city’da şu an hava $cond, sıcaklık $temp ve hissedilen $feelsLike. "

        return when {
            prompt.contains("giys") || prompt.contains("giyecek") || prompt.contains("ne giy") || prompt.contains("kıyafet") -> {
                val tempVal = temp.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 20
                val advice = when {
                    tempVal < 10 -> "Hava oldukça soğuk, kalın bir mont, bere ve atkı almanı öneririm."
                    tempVal < 18 -> "Hava serin, orta kalınlıkta bir ceket veya hırka giymek iyi olacaktır."
                    tempVal < 25 -> "Hava ılık ve rahat; hafif bir ceket veya sweatshirt yeterli olur."
                    else -> "Hava sıcak; ince, pamuklu ve ferah kıyafetler tercih etmelisin."
                }
                val rainAdvice = if (precip > 30) " Ayrıca %$precip yağış ihtimali var, şemsiyeni sakın unutma." else ""
                val uvAdvice = if (uv > 5) " UV indeksi $uv olduğu için güneş koruyucu kullanmanı öneririm." else ""
                baseHeader + currentStatus + advice + rainAdvice + uvAdvice
            }
            prompt.contains("aktivite") || prompt.contains("dışarı") || prompt.contains("çıkılır mı") || prompt.contains("spor") -> {
                val isBadWeather = precip > 40 || wind > 35.0 || uv > 8
                val advice = if (isBadWeather) {
                    "Şu anki koşullar ($cond, rüzgar $wind km/s) dışarıda uzun süre vakit geçirmek için pek ideal değil. Kapalı alan aktivitelerini değerlendirebilirsin."
                } else {
                    "Hava durumu dışarı çıkmak, yürüyüş yapmak veya hafif tempolu bir spor için oldukça elverişli görünüyor."
                }
                baseHeader + currentStatus + advice
            }
            prompt.contains("yağmur") || prompt.contains("yağış") || prompt.contains("şemsiye") -> {
                val advice = if (precip > 20) {
                    "Bugün $city için %$precip oranında bir yağış ihtimali bulunuyor. Tedbirli olup şemsiye taşıman akıllıca olur."
                } else {
                    "Bugün için belirgin bir yağış beklenmiyor, gökyüzü genellikle $cond."
                }
                baseHeader + currentStatus + advice
            }
            prompt.contains("rüzgar") || prompt.contains("fırtına") -> {
                val advice = if (wind > 20.0) {
                    "Rüzgar hızı $wind km/s seviyesinde. Dışarıdayken biraz esintili hissedebilirsin, rüzgar kesici bir şeyler giymek konforunu artırır."
                } else {
                    "Rüzgar şu an oldukça hafif ($wind km/s), hava sakin görünüyor."
                }
                baseHeader + currentStatus + advice
            }
            prompt.contains("uv") || prompt.contains("güneş") || prompt.contains("krem") -> {
                val advice = if (uv > 4) {
                    "UV indeksi $uv seviyesinde, yani güneş etkisi belirgin. Özellikle öğle saatlerinde güneş kremi ve şapka kullanmanı öneririm."
                } else {
                    "Güneşin yakıcı etkisi şu an düşük (UV: $uv), ancak yine de gözlerini korumak için güneş gözlüğü takabilirsin."
                }
                baseHeader + currentStatus + advice
            }
            prompt.contains("hafta sonu") || prompt.contains("tahmin") -> {
                baseHeader + "Hafta sonu detaylarına AI erişimim şu an kısıtlı ama genel tabloya göre planlarına kapalı alan alternatifleri eklemeni öneririm."
            }
            else -> {
                val extraInfo = "Yağış ihtimali %$precip, rüzgar $wind km/s ve UV indeksi $uv seviyesinde."
                baseHeader + currentStatus + extraInfo + " Başka bir konuda sorunuz olursa buradayım!"
            }
        }
    }
}
