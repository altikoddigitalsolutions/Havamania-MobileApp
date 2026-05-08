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
        val city = weatherData?.cityName ?: "bulunduğun şehir"
        val temp = weatherData?.temperature ?: "mevcut"
        val cond = weatherData?.condition?.lowercase() ?: "değişken"
        val precip = weatherData?.precipitationProbability ?: 0

        val baseHeader = "AI bağlantısında kısa bir sorun yaşadım ama mevcut hava verilerine göre yardımcı olayım. "

        return when {
            prompt.contains("giys") || prompt.contains("giyecek") || prompt.contains("ne giy") -> {
                val advice = if ((weatherData?.temperature?.filter { it.isDigit() || it == '-' }?.toIntOrNull() ?: 20) < 15) {
                    "Hava serin ($temp) olduğu için kalın bir mont veya katmanlı giyinmeni öneririm."
                } else {
                    "Hava $temp civarında. İnce bir ceket veya rahat bir tişört yeterli olabilir."
                }
                val rainAdvice = if (precip > 30) " Ayrıca yağış ihtimaline karşı şemsiyeni yanına almalısın." else ""
                baseHeader + "$city için hava şu an $cond. $advice$rainAdvice"
            }
            prompt.contains("aktivite") || prompt.contains("dışarı") || prompt.contains("çıkılır mı") -> {
                val advice = if (precip > 40 || (weatherData?.windSpeed ?: 0.0) > 30.0) {
                    "Hava koşulları ($cond) şu an dış mekan aktiviteleri için pek elverişli değil, kapalı alanları tercih edebilirsin."
                } else {
                    "Hava şu an $cond. Dışarı çıkmak ve yürüyüş yapmak için oldukça uygun bir zaman."
                }
                baseHeader + advice
            }
            prompt.contains("hafta sonu") -> {
                baseHeader + "Hafta sonu tahminini günlük verilerden kontrol ettim. Yağış ihtimali olan günlerde açık hava planlarına kapalı alan alternatifi eklemek iyi olur."
            }
            prompt.contains("seyahat") || prompt.contains("valiz") || prompt.contains("yolculuk") -> {
                baseHeader + "Yaklaşan seyahatlerin için hava değişken olabilir. Valizine ince katman, yağmurluk ve rahat ayakkabı eklemek iyi fikir."
            }
            prompt.contains("yağmur") || prompt.contains("yağış") -> {
                val advice = if (precip > 20) {
                    "Bugün %$precip oranında yağış ihtimali var. Yanına şemsiye almanı ve su geçirmez ayakkabı tercih etmeni öneririm."
                } else {
                    "Bugün için belirgin bir yağış beklenmiyor ama gökyüzü $cond görünüyor."
                }
                baseHeader + advice
            }
            else -> {
                baseHeader + "Şu an $city için hava $cond ve sıcaklık $temp. Detaylı analiz için daha sonra tekrar deneyebilir veya ana ekrandaki saatlik ve günlük tabloları inceleyebilirsin."
            }
        }
    }
}
