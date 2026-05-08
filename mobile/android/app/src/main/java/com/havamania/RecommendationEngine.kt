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
        userInterests: Set<String> = emptySet()
    ): HavamaniaRecommendation {
        val todayDaily = weatherData.dailyForecast.firstOrNull { it.isToday } ?: weatherData.dailyForecast.firstOrNull()
        val tempMin = todayDaily?.minTemp ?: 10
        val tempMax = todayDaily?.maxTemp ?: 20
        val condition = weatherData.condition.lowercase()
        val city = weatherData.cityName

        val uvIndex = weatherData.details.find { it.title.contains("UV") }?.value?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        val windSpeed = weatherData.details.find { it.title.contains("Rüzgar") }?.value?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        val precipProb = weatherData.precipitationProbability ?: 0
        val humidity = weatherData.humidity ?: 50
        val visibility = weatherData.visibilityKm ?: 10.0

        val highlights = mutableListOf<String>()
        var primaryType = RecommendationType.GENERAL
        var priority = RecommendationPriority.LOW

        // --- CÜMLE 1: GENEL ÖZET ---
        val s1 = "Bugün $city’da hava genel olarak $condition ve sıcaklık $tempMin–$tempMax° aralığında görünüyor."

        // --- CÜMLE 2: PRATİK ÖNERİ ---
        val s2 = when {
            condition.contains("yağmur") || condition.contains("sağanak") || precipProb > 50 -> {
                primaryType = RecommendationType.WARNING
                priority = RecommendationPriority.HIGH
                highlights.add("şemsiye")
                listOf(
                    "Dışarı çıkarken şemsiyeni yanına alman ve su geçirmez bir katman tercih etmen çok iyi olur.",
                    "Yağış ihtimali yüksek olduğu için ayakkabı seçiminde su geçirmez modelleri tercih etmelisin.",
                    "Islanmamak için hazırlıklı olmanı ve planlarını kapalı mekanlara göre revize etmeni öneririm."
                ).random()
            }
            tempMax > 30 -> {
                primaryType = RecommendationType.HEALTH
                highlights.add("açık renk")
                "Hava oldukça sıcak olacağı için ince, pamuklu ve açık renkli kıyafetler seçmen konforunu artıracaktır."
            }
            tempMax < 14 -> {
                primaryType = RecommendationType.COMFORT
                highlights.add("kalın")
                "Hava serin seyredeceği için kalın bir mont veya katmanlı giysiler tercih ederek kendini koruyabilirsin."
            }
            tempMax < 20 -> {
                highlights.add("ince bir katman")
                "Hava ne çok sıcak ne çok soğuk; üzerine ince bir katman veya bir ceket alarak dışarı çıkmak en mantıklısı olacaktır."
            }
            else -> {
                "Hava koşulları dışarıda vakit geçirmek için oldukça ideal, rahat kıyafetlerinle günün tadını çıkarabilirsin."
            }
        }

        // --- CÜMLE 3: RİSK / EKSTRA / KİŞİSELLEŞTİRME ---
        val extraTips = mutableListOf<String>()

        // Hava riskleri
        if (uvIndex > 6) {
            extraTips.add("UV seviyesi yüksek olduğu için özellikle öğle saatlerinde güneş kremi ve şapka kullanmayı ihmal etme.")
            highlights.add("UV")
        }
        if (windSpeed > 25) {
            extraTips.add("Rüzgar belirgin olacağı için sahil ve yüksek bölgelerde rüzgar kesici bir üst giymen iyi fikir olabilir.")
            highlights.add("rüzgar")
        }
        if (visibility < 5.0) {
            extraTips.add("Görüş mesafesi düşük olabileceği için araç kullanırken takip mesafeni artırman ve dikkatli olman önemli.")
        }

        // İlgi alanları (Eğer risk mesajı yoksa veya çok uzun değilse)
        if (extraTips.size < 2) {
            if (userInterests.contains("Spor") || userInterests.contains("Koşu")) {
                extraTips.add(if (tempMax > 25) "Spor veya koşu planın varsa daha serin olan sabah veya akşam saatlerini seçmeni öneririm."
                              else "Bugünkü hava tablosu açık havada spor ve antrenman yapmak için oldukça elverişli görünüyor.")
            } else if (userInterests.contains("Fotoğrafçılık")) {
                extraTips.add("Işık geçişleri bugün fotoğraf çekmek için güzel fırsatlar sunabilir, kameranı yanına almayı unutma.")
            } else if (userInterests.contains("Çocuk") || userInterests.contains("Aile")) {
                extraTips.add("Çocuklarla dışarı çıkacaksanız, ani sıcaklık değişimlerine karşı hazırlıklı olup yanınıza yedek bir hırka alabilirsiniz.")
            } else if (userInterests.contains("Motosiklet") || userInterests.contains("Bisiklet")) {
                if (windSpeed > 20) extraTips.add("İki teker üzerinde olacaksan rüzgar hamlelerine karşı bugün her zamankinden daha dikkatli olmalısın.")
            }
        }

        val s3 = if (extraTips.isNotEmpty()) extraTips.random() else "Bugünkü genel tabloya göre planlarını yapabilir, keyifli bir gün geçirebilirsin!"

        val finalMessage = "$s1 $s2 $s3"

        return HavamaniaRecommendation(
            message = finalMessage,
            type = primaryType,
            highlightedWords = highlights.distinct(),
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
            return "AI bağlantısında kısa bir sorun yaşadım. Şu an güncel hava durumu verilerine de ulaşamıyorum. Lütfen internet bağlantını kontrol edip tekrar dene."
        }

        val city = weatherData.cityName
        val temp = weatherData.temperature
        val feelsLike = weatherData.feelsLike
        val cond = weatherData.condition.lowercase()
        val precip = weatherData.precipitationProbability ?: 0
        val wind = weatherData.windSpeed ?: 0.0
        val uv = weatherData.uvIndex ?: 0

        val baseHeader = "AI bağlantısında kısa bir sorun yaşadım ama mevcut hava verilerine göre yardımcı olayım. "
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
