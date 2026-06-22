package com.havamania

import com.havamania.ui.theme.AssistantTone
import java.time.LocalTime
import java.util.Locale
import kotlin.random.Random

object RecommendationEngine {

    /**
     * BUGÜN İÇİN DETAYLI, DOĞAL VE ANALİTİK ÖNERİ ÜRETİR.
     * Havamania Asistan'ın uzmanlık seviyesini yansıtır.
     */
    fun generateTodayRecommendation(
        weatherData: WeatherData,
        userInterests: Set<String> = emptySet()
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
        val visibility = weatherData.visibilityKm ?: 10.0
        val cloudCover = weatherData.cloudCover ?: 0

        val highlights = mutableListOf<String>()
        var primaryType = RecommendationType.GENERAL
        var priority = RecommendationPriority.LOW

        // 1. GENEL DURUM ÖZETİ VE YORUMU
        val intro = when {
            rawCondition.contains("yağmur") || rawCondition.contains("sağanak") -> {
                primaryType = RecommendationType.WARNING
                priority = RecommendationPriority.HIGH
                "Bugün $city’da gökyüzü oldukça hareketli. Yağış geçişleri günün temposunu belirleyecek gibi görünüyor. Sıcaklıklar $tempMin° ile $tempMax° arasında seyretse de nem oranının etkisiyle hava biraz daha ağır hissedilebilir."
            }
            rawCondition.contains("bulut") -> {
                "Bugün $city semalarında bulutlar hakimiyet kurmuş durumda. Güneşlenme süresi kısıtlı olsa da, sıcaklığın $tempMax° seviyelerine çıkması dışarıda vakit geçirmek için oldukça dengeli bir ortam yaratıyor."
            }
            rawCondition.contains("güneş") || rawCondition.contains("açık") -> {
                "Pırıl pırıl bir gün $city’da seni bekliyor. Gökyüzü tamamen açık ve güneş cömertliğini sergiliyor. $tempMax°'lik maksimum sıcaklık, mevsime uygun harika bir atmosfer sunuyor."
            }
            else -> "Bugün $city’da $rawCondition bir hava hakim. Termometreler $tempMin° ile $tempMax° arasında gidip gelirken, genel atmosfer planlarını yapmak için oldukça stabil görünüyor."
        }

        // 2. METEOROLOJİK ANALİZ (NEDEN-SONUÇ)
        val analysisParts = mutableListOf<String>()

        // Yağış Analizi
        if (precipProb > 30) {
            analysisParts.add("Yağış ihtimali %$precipProb seviyesinde olduğu için planlarında esnek olman önemli. Nem oranının %$humidity olması, yağışın etkisini daha belirgin kılabilir.")
            highlights.add("yağış")
        }

        // Rüzgar Analizi
        if (windSpeed > 20) {
            val windLevel = WeatherUtils.getWindLevelText(windSpeed)
            analysisParts.add("$windSpeed km/s hızındaki $windLevel rüzgar, özellikle açık alanlarda hissedilir bir serinlik yaratacaktır. Bu durum hissedilen sıcaklığın gerçek değerin biraz altında kalmasına neden olabilir.")
            highlights.add("rüzgar")
        }

        // UV Analizi
        if (uvIndex > 5) {
            analysisParts.add("UV indeksi $uvIndex seviyesinde. Bu, güneşin korumasız cilt üzerinde doğrudan etkisi olabileceği anlamına geliyor. Özellikle öğle saatlerinde cildini korumaya özen göstermelisin.")
            highlights.add("UV")
        }

        // 3. AKTİVİTE VE YAŞAM ÖNERİLERİ
        val suggestions = mutableListOf<String>()

        // Kıyafet Önerisi
        val clothingAdvice = when {
            tempMax > 28 -> "Hava oldukça sıcak; ince, pamuklu ve açık renkli kıyafetler gün boyu konforunu korumanı sağlayacaktır."
            tempMax < 14 -> "Havanın serinliği nedeniyle katmanlı giyinmek en mantıklısı. Kalın bir dış katman ve içine daha hafif parçalar tercih edebilirsin."
            precipProb > 40 -> "Yanına mutlaka su geçirmeyen bir katman veya bir şemsiye almanı öneririm, sürpriz yağışlar keyfini kaçırmasın."
            else -> "Hava ne çok sıcak ne çok soğuk. Üzerine hafif bir ceket alarak dışarı çıkmak, değişen koşullara uyum sağlamanı kolaylaştırır."
        }
        suggestions.add(clothingAdvice)

        // İlgi Alanına Göre Özelleşmiş Öneri
        if (userInterests.contains("Spor") || userInterests.contains("Koşu")) {
            if (precipProb > 50 || windSpeed > 35) {
                suggestions.add("Hava koşulları dışarıda spor yapmak için biraz zorlayıcı olabilir. Bugün antrenmanını kapalı bir spor salonunda yapmayı düşünebilirsin.")
            } else {
                suggestions.add("Açık havada koşu veya antrenman için ideal bir gün. Rüzgarın $windSpeed km/s olması, performansını destekleyecek taze bir hava sağlıyor.")
            }
        } else if (userInterests.contains("Fotoğrafçılık")) {
            if (cloudCover in 30..70) {
                suggestions.add("Bulutların gökyüzündeki dağılımı, fotoğrafların için harika bir yumuşak ışık sağlayacaktır. Işık geçişleri bugün fotoğraf çekmek için güzel fırsatlar sunabilir.")
            } else if (rawCondition.contains("sis")) {
                suggestions.add("Sisli atmosfer, dramatik ve mistik kareler yakalamak için sana eşsiz kompozisyonlar sunabilir.")
            }
        } else if (userInterests.contains("Kamp") || userInterests.contains("Doğa")) {
            if (precipProb < 20 && windSpeed < 20) {
                suggestions.add("Doğa yürüyüşü veya kamp planın varsa koşullar oldukça elverişli. Temiz havanın ve sakin atmosferin tadını çıkarabilirsin.")
            } else if (precipProb > 50) {
                suggestions.add("Doğa aktiviteleri için zemin ıslak ve kaygan olabilir. Eğer kamp yapacaksan su yalıtımına ekstra dikkat etmelisin.")
            }
        }

        val analysisText = if (analysisParts.isNotEmpty()) analysisParts.joinToString(" ") else "Meteorolojik veriler bugün için oldukça dengeli bir tablo çiziyor, beklenmedik bir hava olayı öngörülmüyor."
        val finalMessage = "$intro $analysisText\n\n${suggestions.joinToString(" ")}"

        return HavamaniaRecommendation(
            message = finalMessage,
            type = primaryType,
            highlightedWords = highlights.distinct(),
            priority = priority
        )
    }

    /**
     * SEYAHAT PLANLARINA ÖZEL ANALİTİK ANALİZ ÜRETİR.
     */
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

    /**
     * ASİSTAN YANITLARINI UZMAN DANIŞMAN TONUNDA ÜRETİR.
     */
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
                AssistantTone.SAMIMI -> "Şu an hava durumuna bakamıyorum canım, birazdan tekrar sorarsan kesin çözeriz!"
                AssistantTone.RESMI -> "Meteorolojik verilere şu an ulaşılamamaktadır. Lütfen daha sonra tekrar deneyiniz."
                AssistantTone.KISA_NET -> "Veri hatası. Tekrar dene."
                else -> "Şu an hava durumu verilerine tam erişim sağlayamıyorum. Birazdan tekrar denersen detaylı bir analiz sunabilirim."
            }
        }

        val city = weatherData.cityName
        val temp = weatherData.temperature
        val feels = weatherData.feelsLike
        val cond = weatherData.condition.lowercase(Locale("tr"))
        val precip = weatherData.precipitationProbability ?: 0
        val wind = weatherData.windSpeed ?: 0.0
        val windDirLabel = weatherData.windDirectionLabel ?: ""
        val windFullDir = WeatherUtils.getFullWindDirection(windDirLabel)
        val gust = weatherData.windGust ?: 0.0
        val humidity = weatherData.humidity ?: 50
        val uv = weatherData.uvIndex ?: 0
        val pressure = weatherData.pressure ?: 1013
        val cloud = weatherData.cloudCover ?: 0

        // UZMAN DANIŞMAN RAPORU YAPISI
        val header = when (tone) {
            AssistantTone.SAMIMI -> "Selam! Senin için $city havasını hemen inceledim:\n\n"
            AssistantTone.RESMI -> "$city bölgesine ait güncel meteorolojik analiz raporu aşağıda sunulmuştur:\n\n"
            AssistantTone.KISA_NET -> "$city Güncel Tablo:\n\n"
            AssistantTone.DETAYLI_UZMAN -> "$city için kapsamlı teknik atmosfer analizi ve uzman yorumu:\n\n"
            else -> "$city için hazırladığım atmosferik analiz raporu aşağıdadır:\n\n"
        }

        val dataSection = if (tone == AssistantTone.KISA_NET) {
            "• ${cond.replaceFirstChar { it.uppercase() }}, $temp\n" +
            "• Nem: %$humidity, Rüzgar: ${wind.toInt()} km/s\n\n"
        } else {
            "📊 GÜNCEL VERİLER:\n" +
            "• Gökyüzü: ${cond.replaceFirstChar { it.uppercase() }}\n" +
            "• Sıcaklık: $temp (Hissedilen: $feels)\n" +
            "• Nem: %$humidity\n" +
            "• Yağış İhtimali: %$precip\n" +
            "• Rüzgar: ${WeatherUtils.formatWindWithLevel(wind)}\n" +
            "• Hamleler: ${gust.toInt()} km/s\n" +
            "• Yön: $windFullDir ($windDirLabel)\n" +
            "• UV İndeksi: $uv\n" +
            "• Basınç: $pressure hPa\n" +
            "• Bulutluluk: %$cloud\n\n"
        }

        val interpretationLabel = when (tone) {
            AssistantTone.SAMIMI -> "🧐 DURUM ŞÖYLE:\n"
            AssistantTone.RESMI -> "🔍 METEOROLOJİK DEĞERLENDİRME:\n"
            AssistantTone.KISA_NET -> "📋 ÖZET:\n"
            else -> "🔍 ANALİZ VE YORUM:\n"
        }

        val interpretation = interpretationLabel + when {
            prompt.contains("giys") || prompt.contains("kıyafet") || prompt.contains("ne giy") -> {
                val tempVal = temp.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 20
                val coreAdvice = when {
                    tempVal < 10 -> "Hava oldukça soğuk, vücut ısısını korumak için kalın bir mont, bere ve atkı almanı öneririm. Katmanlı giyinmek, kapalı mekanlara girdiğinde konforunu artıracaktır."
                    tempVal < 18 -> "Hava serin ve taze bir karaktere sahip. Orta kalınlıkta bir ceket veya hırka giymek, gün boyu vücut ısınızı dengede tutacaktır."
                    tempVal < 25 -> "Hava ılık ve oldukça rahat; hafif bir ceket veya sweatshirt yeterli olur. Güneşin durumuna göre ceketini çıkarabileceğin bir kombin yapabilirsin."
                    else -> "Hava sıcak; cildinin nefes alabilmesi için ince, pamuklu ve ferah kıyafetler tercih etmelisin."
                }
                val extra = if (precip > 30) " Ayrıca yağış riskine karşı yanına bir şemsiye alman, planlarının aksamasını önleyecektir." else ""
                coreAdvice + extra
            }
            prompt.contains("aktivite") || prompt.contains("dışarı") || prompt.contains("spor") || prompt.contains("koşu") || prompt.contains("bisiklet") -> {
                val isBad = precip > 40 || wind > 35.0 || uv > 8
                if (isBad) {
                    "Mevcut meteorolojik koşullar (Yağış: %$precip, Rüzgar: ${wind.toInt()} km/s) dışarıda uzun süre vakit geçirmek veya efor sarf etmek için pek ideal değil. Fiziksel aktivitelerini kapalı alanlara taşıman, sağlığın ve konforun için daha doğru bir karar olabilir."
                } else {
                    "Gökyüzü durumu ve sıcaklık seviyesi, dışarıda aktif vakit geçirmek için oldukça davetkar. Rüzgarın ${wind.toInt()} km/s seviyesinde olması, özellikle bisiklet veya koşu gibi aktivitelerde ferahlatıcı bir etki yaratacaktır."
                }
            }
            prompt.contains("fotoğraf") -> {
                if (cloud in 30..80) {
                    "Bulutluluk oranının %$cloud olması, ışığın homojen dağılmasını sağlayarak dış mekan çekimlerinde harika bir 'softbox' etkisi yaratacaktır. Detayları yakalamak için mükemmel bir gün."
                } else {
                    "Işık koşulları oldukça sert olabilir. Eğer dış çekim yapacaksan, gölge ve ışık dengesine ekstra dikkat etmeni öneririm."
                }
            }
            prompt.contains("deniz") || prompt.contains("yüzme") || prompt.contains("tekne") -> {
                if (wind > 25.0) {
                    "Rüzgar hızı ${wind.toInt()} km/s seviyesine ulaştığı için deniz yüzeyinde çırpıntı oluşabilir. Deniz aktivitelerinde veya tekne yolculuklarında dalga boyuna karşı dikkatli olmalısın."
                } else {
                    "Deniz oldukça sakin görünüyor. Rüzgarın hafif esintisi, su kenarında vakit geçirmeyi oldukça keyifli kılacaktır."
                }
            }
            else -> {
                val tempVal = temp.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 20
                "Mevcut verileri incelediğimde, $city’da dengeli bir atmosfer görüyorum. Sıcaklık $temp civarında seyrederken, nemin %$humidity olması havanın kalitesini artırıyor. Rüzgar ${WeatherUtils.getWindLevelText(wind)} şiddetinde esiyor, bu da açık hava planlarını büyük ölçüde destekliyor."
            }
        }

        val suggestion = "\n\n💡 ÖNERİ VE TAVSİYE:\n" + when {
            precip > 50 -> "Bugün planlarını kapalı alanlara göre revize etmen sürprizlerden korunmanı sağlar."
            uv > 6 -> "Güneşin en dik geldiği 11:00 - 16:00 saatleri arasında yüksek faktörlü koruyucu kullanmayı ihmal etme."
            wind > 30 -> "Rüzgar hamleleri anlık dengeni bozabilir, yüksek ve açık alanlarda yürürken dikkatli ol."
            temp.contains("-") -> "Don riskine karşı su tesisatlarını kontrol etmeyi ve araç camların için önlem almayı unutma."
            else -> "Havanın tadını çıkarabileceğin bir yürüyüş rotası planlayarak güne enerji katabilirsin."
        }

        val closing = "\n\nHavamania Asistan olarak her zaman yanındayım. Başka bir detay veya farklı bir şehir hakkında sorun olursa sormaktan çekinme!"

        return header + dataSection + interpretation + suggestion + closing
    }
}
