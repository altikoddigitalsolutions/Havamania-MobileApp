package com.havamania

import com.havamania.ui.theme.AssistantTone
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class RecommendationTripStatus {
    UPCOMING_LOCKED,
    UPCOMING_ACTIVE,
    ONGOING,
    PAST
}

object RecommendationEngine {

    fun getTripStatus(today: LocalDate, startDate: LocalDate, endDate: LocalDate): RecommendationTripStatus {
        return when {
            today.isAfter(endDate) -> RecommendationTripStatus.PAST
            !today.isBefore(startDate) && !today.isAfter(endDate) -> RecommendationTripStatus.ONGOING
            else -> {
                val daysUntil = ChronoUnit.DAYS.between(today, startDate)
                if (daysUntil > TRIP_ANALYSIS_WINDOW_DAYS) RecommendationTripStatus.UPCOMING_LOCKED else RecommendationTripStatus.UPCOMING_ACTIVE
            }
        }
    }

    /**
     * Günlük hava durumu önerisi oluşturur - Ana ekran kartı için.
     */
    fun generateTodayRecommendation(
        weatherData: WeatherData,
        userInterests: Set<String> = emptySet(),
        aboutMe: String? = null,
        tone: AssistantTone = AssistantTone.DENGELI,
        personalization: PersonalizationProfile? = null
    ): HavamaniaRecommendation {
        val message = generateStructuredWeatherReply(weatherData, tone, userInterests, aboutMe, personalization)

        val uv = weatherData.uvIndex ?: 0
        val precip = weatherData.precipitationProbability ?: 0
        val wind = weatherData.windSpeed ?: 10.0

        val highlights = mutableListOf<String>()
        if (uv >= 6) highlights.add("UV")
        if (precip > 40) highlights.add("yağış")
        if (wind > 30) highlights.add("rüzgar")
        if (userInterests.contains("uv_hassasiyeti") && uv >= 4) highlights.add("hassasiyet")
        if (userInterests.contains("kamp") && (precip > 20 || tempToFloat(weatherData.temperature) < 10f)) highlights.add("kamp")

        return HavamaniaRecommendation(
            message = message,
            type = RecommendationType.GENERAL,
            highlightedWords = highlights.distinct(),
            priority = if (uv >= 8 || precip > 70 || (userInterests.contains("uv_hassasiyeti") && uv >= 6)) RecommendationPriority.HIGH else RecommendationPriority.LOW
        )
    }

    private fun tempToFloat(tempStr: String): Float {
        return tempStr.filter { it.isDigit() || it == '-' }.toFloatOrNull() ?: 20f
    }

    private fun generateStructuredWeatherReply(
        weather: WeatherData,
        tone: AssistantTone,
        interests: Set<String>,
        aboutMe: String?,
        personalization: PersonalizationProfile? = null
    ): String {
        val city = weather.cityName
        val tempValue = weather.temperature.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 20
        val cond = weather.condition.lowercase(Locale("tr"))
        val uv = weather.uvIndex ?: 0
        val precip = weather.precipitationProbability ?: 0
        val wind = weather.windSpeed ?: 0.0

        val prefs = personalization?.weatherPreferences ?: WeatherPreferences()

        val sb = StringBuilder()

        // 1. Hava Özeti
        sb.append("Bugün $city'de hava $tempValue°C ve $cond. ")

        // 2. Günlük Yaşam Etkisi
        if (precip > 50 || (prefs.rainSensitive && precip > 20)) {
            sb.append("Yağış ihtimali gününüzü etkileyebilir, dışarıda uzun süre kalacaksanız dikkatli olmanızda fayda var. ")
        } else if (tempValue > 32 || (prefs.likesCool && tempValue > 25)) {
            sb.append("Sıcak hava nedeniyle öğle saatlerinde serin yerleri tercih etmenizi öneririm. ")
        } else {
            sb.append("Hava genel olarak günlük planlarınız için oldukça elverişli görünüyor. ")
        }

        sb.append("[SEP]")

        // 3. Kıyafet Önerisi
        val clothing = when {
            tempValue > 25 -> "İnce ve pamuklu kıyafetler"
            tempValue > 15 -> "Hafif bir ceket veya sweatshirt"
            else -> "Kalın bir mont ve koruyucu kıyafetler"
        }
        sb.append("👕 GİYSİ| $clothing bugün en konforlu seçim olacaktır. ")

        // 4. Aktivite & Kişiselleştirme
        if (interests.any { it.contains("koşu", true) } && tempValue in 10..24 && precip < 20) {
            sb.append("[SEP]🏃 KOŞU| Koşu için harika bir hava! Sabah saatlerini değerlendirebilirsin. ")
        } else if (interests.any { it.contains("kamp", true) } && precip > 30) {
            sb.append("[SEP]⛺ KAMP| Kamp planların varsa zemin ıslaklığına ve yağış geçişlerine dikkat etmelisin. ")
        } else if (interests.contains("Kayak") || interests.contains("Kış Sporları")) {
             if (tempValue < 5) sb.append("[SEP]❄️ SPOR| Kış sporları için taze kar ve soğuk hava oldukça uygun görünüyor.")
        }

        // 5. Risk Uyarısı
        if (uv >= 6 || (interests.contains("uv_hassasiyeti") && uv >= 4) || (prefs.uvSensitive && uv >= 4)) {
            sb.append("[SEP]⚠️ RİSK| UV indeksi ($uv) yüksek! Güneş kremi ve gözlük kullanmayı ihmal etme.")
        } else if (wind > 35 || (prefs.windSensitive && wind > 20)) {
            sb.append("[SEP]⚠️ RİSK| Sert rüzgar uyarısı! Dışarıda savrulabilecek eşyalara dikkat.")
        }

        return sb.toString().trim()
    }

    /**
     * AI Asistan Yanıtları - Yeni Kurallara Göre Optimize Edildi
     */
    fun generateAssistantFallbackReply(
        userPrompt: String,
        weatherData: WeatherData?,
        aboutMe: String? = null,
        interests: Set<String> = emptySet(),
        tone: AssistantTone = AssistantTone.DENGELI,
        travelPlans: List<TravelPlan> = emptyList()
    ): String {
        if (weatherData == null) return "Hava durumu verilerine şu an erişilemiyor. Lütfen kısa süre sonra tekrar dene."

        val intent = AiIntentParser.detectIntent(userPrompt)
        val today = LocalDate.now()
        val nextPlan = travelPlans
            .filter { !it.isArchived && !it.endDate.isBefore(today) }
            .minByOrNull { it.startDate }

        val promptCity = AiIntentParser.detectCity(userPrompt)

        // Context Selection
        val targetCity: String = promptCity ?: weatherData.cityName
        val currentTemp = weatherData.temperature.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 20
        val feelsLike = weatherData.feelsLike.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: currentTemp
        val uv = weatherData.uvIndex ?: 0
        val humidity = weatherData.humidity ?: 50
        val wind = weatherData.windSpeed ?: 10.0
        val precip = weatherData.precipitationProbability ?: 0

        return when (intent) {
            AiIntent.CLOTHING -> generateClothingReply(targetCity, currentTemp, feelsLike, uv, wind, humidity, tone, interests)
            AiIntent.ACTIVITY -> generateActivityReply(targetCity, currentTemp, uv, precip, wind, humidity, tone, interests)
            AiIntent.WEEKEND_FORECAST -> generateWeekendReply(weatherData, tone, interests)
            AiIntent.PACKING -> generatePackingReply(nextPlan, tone)
            AiIntent.GENERAL_WEATHER -> {
                if (userPrompt.contains("yağmur", ignoreCase = true) || userPrompt.contains("yağış", ignoreCase = true)) {
                    generateRainReply(targetCity, precip, weatherData, tone)
                } else {
                    generateStructuredWeatherReply(weatherData, tone, interests, aboutMe)
                }
            }
            AiIntent.TRAVEL -> generateTravelReply(userPrompt, weatherData, travelPlans, tone)
            else -> generateStructuredWeatherReply(weatherData, tone, interests, aboutMe)
        }
    }

    // 1) BUGÜN NE GİYMELİYİM?
    private fun generateClothingReply(city: String, temp: Int, feels: Int, uv: Int, wind: Double, hum: Int, tone: AssistantTone, interests: Set<String>): String {
        val baseAdvice = when {
            temp > 28 || feels > 30 -> "Hava oldukça sıcak olduğu için terletmeyen, %100 pamuklu veya keten kumaşlar günü kurtaracaktır."
            temp > 20 -> "Hafif bir tişört ve altına ince bir pantolon ideal görünüyor; ne çok sıcak ne çok soğuk, tam kararında bir hava."
            temp > 12 -> "Sweatshirt veya uzun kollu ince gömleklerin üzerine hafif bir ceket alarak katmanlı bir kombin yapmanı öneririm."
            else -> "Hava oldukça sert; yünlü kazaklar, kalın pantolonlar ve mutlaka koruyucu bir mont giyerek vücut ısısını korumalısın."
        }

        val uvAdvice = if (uv > 5 || (interests.contains("uv_hassasiyeti") && uv > 3)) " UV seviyesi riskli olduğu için dışarıda güneş gözlüğü ve şapka kullanmayı, güneş kremi sürmeyi ihmal etme." else ""
        val windAdvice = if (wind > 25) " Rüzgar hızı yüksek olduğu için hissettiğin soğukluk artabilir, rüzgar kesici bir üst tercih etmen konforunu artıracaktır." else ""

        // Personalization
        val interestAdvice = if (interests.contains("motorsiklet")) " Motor sürüşü yapacaksan rüzgar direncine karşı korunaklı bir mont seçmen çok önemli." else ""

        return when (tone) {
            AssistantTone.SAMIMI -> "$city'de bugün hava tam bir muamma canım! 😊 $baseAdvice$uvAdvice$windAdvice$interestAdvice Güzel görünmeyi unutma!"
            AssistantTone.RESMI -> "$city lokasyonu için meteorolojik veriler ışığında şu giyim önerileri sunulmaktadır: $baseAdvice$uvAdvice$windAdvice$interestAdvice Bilgilerinize sunulur."
            AssistantTone.DETAYLI_UZMAN -> "Meteorolojik Analiz ($city): Termal algı $feels°C ve bağıl nem %$hum seviyelerindedir. Bu parametreler altında $baseAdvice$uvAdvice$windAdvice$interestAdvice Termal regülasyonunuzu bu doğrultuda optimize ediniz."
            AssistantTone.KISA_NET -> "$city: $temp° ($baseAdvice). UV: $uv. $uvAdvice"
            else -> "Bugün $city'de $baseAdvice$uvAdvice$windAdvice$interestAdvice"
        }
    }

    // 2) HAFTA SONU HAVA NASIL?
    private fun generateWeekendReply(weather: WeatherData, tone: AssistantTone, interests: Set<String>): String {
        val sat = weather.dailyForecast.find { it.day.contains("Cumartesi", true) }
        val sun = weather.dailyForecast.find { it.day.contains("Pazar", true) }

        if (sat == null || sun == null) return "Hafta sonu verileri henüz netleşmedi, ancak yakında burada olacak."

        val verdict = if (sat.maxTemp >= sun.maxTemp && (sat.precipitationProbability ?: 0) < 20) "Cumartesi" else "Pazar"

        // Personalization for activity
        val activity = when {
            interests.contains("kamp") -> "doğa kampı"
            interests.contains("kayak") || interests.contains("snowboard") -> "kış sporları"
            interests.contains("trekking") -> "doğa yürüyüşü"
            interests.contains("balikcilik") -> "balıkçılık"
            interests.contains("acik_hava") -> "açık hava etkinliği"
            else -> "sahil yürüyüşü veya kısa bir tur"
        }

        val advice = if ((sat.precipitationProbability ?: 0) > 40 || (sun.precipitationProbability ?: 0) > 40) "Kapalı alan etkinliklerine yönelmek daha güvenli olabilir." else "$activity için harika bir fırsat."

        return when (tone) {
            AssistantTone.SAMIMI -> "Hafta sonu planların için harika haberlerim var canım! 😊 Cumartesi ${sat.maxTemp}°, Pazar ise ${sun.maxTemp}° görünüyor. Karşılaştırdığımda $verdict günü hava çok daha davetkar. Bence $advice"
            AssistantTone.RESMI -> "Hafta sonu projeksiyonu: Cumartesi günü maksimum ${sat.maxTemp}°C, Pazar günü ise ${sun.maxTemp}°C sıcaklık öngörülmektedir. Atmosferik koşullar değerlendirildiğinde $verdict gününün outdoor faaliyetler için daha elverişli olduğu saptanmıştır. $advice"
            AssistantTone.DETAYLI_UZMAN -> "48 Saatlik Hafta Sonu Analizi: Cumartesi günü troposferik stabilite hakimken (${sat.maxTemp}°C), Pazar günü sıcaklık ve rüzgar vektörlerinde değişim bekleniyor (${sun.maxTemp}°C). $verdict günü $advice Karşılaştırmalı analizimiz dış mekan planları için Cumartesi gününü işaret etmektedir."
            AssistantTone.KISA_NET -> "Cmt: ${sat.maxTemp}°, Paz: ${sun.maxTemp}°. $verdict günü daha uygun. $advice"
            else -> "Hafta sonu Cumartesi ${sat.maxTemp}°, Pazar ${sun.maxTemp}° civarında olacak. Genel değerlendirmeme göre $verdict günü dışarı çıkmak için çok daha uygun. $advice"
        }
    }

    // 3) DIŞARI ÇIKMAK İÇİN UYGUN MU?
    private fun generateActivityReply(city: String, temp: Int, uv: Int, precip: Int, wind: Double, hum: Int, tone: AssistantTone, interests: Set<String>): String {
        val isGood = precip < 25 && wind < 35 && temp in 12..30
        val status = if (isGood) "oldukça uygun" else "biraz riskli"

        val details = mutableListOf<String>()
        if (precip > 30) details.add("yağış ihtimali (%$precip)")
        if (wind > 30) details.add("sert rüzgar ($wind km/sa)")
        if (uv > 7) details.add("yüksek UV radyasyonu")
        if (hum > 80) details.add("yüksek nem oranı")

        val reason = if (details.isNotEmpty()) "Ancak ${details.joinToString(", ")} nedeniyle dikkatli olmalısın." else "Şu an hiçbir engel görünmüyor."

        // Personalization
        val suggestion = when {
            interests.contains("trekking") && isGood -> " Trekking parkurları seni bekliyor."
            interests.contains("bulut_fotografciligi") && hum > 60 -> " Bulut oluşumları fotoğraf çekimi için harika kareler sunabilir."
            interests.contains("cocuklar_icin") && isGood -> " Çocuklarla park keyfi yapmak için ideal bir gün."
            else -> ""
        }

        return when (tone) {
            AssistantTone.SAMIMI -> "Bugün $city'de dışarı çıkmak için hava $status canım! 😊 $reason$suggestion Şöyle güzel bir yürüyüş ruhuna iyi gelirdi."
            AssistantTone.RESMI -> "$city bölgesi için yapılan analizlerde dış mekan faaliyetlerinin icrası $status olarak değerlendirilmiştir. $reason$suggestion Tedbirli olmanız önerilir."
            AssistantTone.DETAYLI_UZMAN -> "Aktivite Uygunluk İndeksi ($city): Mevcut atmosferik parametreler (Yağış: %$precip, Rüzgar: $wind km/sa, UV: $uv) incelendiğinde durumun $status olduğu görülmektedir. $reason$suggestion"
            AssistantTone.KISA_NET -> "Hava $status. $reason $suggestion"
            else -> "Bugün $city'de dışarı çıkmak $status. $reason$suggestion Planlarını bu verilere göre yapabilirsin."
        }
    }

    // 4) BUGÜN YAĞMUR YAĞACAK MI?
    private fun generateRainReply(city: String, precip: Int, weather: WeatherData, tone: AssistantTone): String {
        val hourlyPrecip = weather.hourlyForecast.take(12).filter { (it.precipitationProbability ?: 0) > 30 }
        val hours = hourlyPrecip.joinToString(", ") { it.time.split("T").last().take(5) }

        val rainText = when {
            precip > 70 -> "Bugün yağmur kaçınılmaz görünüyor, hazırlıklı olmalısın."
            precip > 30 -> "Günün belli bölümlerinde yağış geçişleri olabilir, gökyüzü her an sürpriz yapabilir."
            else -> "Bugün yağmur beklemiyoruz, gökyüzü oldukça dost canlısı görünüyor."
        }

        val umbrellaAdvice = if (precip > 30) " Yanına mutlaka sağlam bir şemsiye and su geçirmeyen bir ayakkabı almalısın." else " Şemsiye taşımana gerek yok, açık hava planlarını gönül rahatlığıyla yapabilirsin."
        val timeDetail = if (hourlyPrecip.isNotEmpty()) " Özellikle şu saatlere dikkat: $hours." else ""

        return when (tone) {
            AssistantTone.SAMIMI -> "Bugün $city'de yağmur ihtimali %$precip canım. $rainText$timeDetail$umbrellaAdvice Islanmanı hiç istemem! ☔"
            AssistantTone.RESMI -> "Günlük yağış projeksiyonu: %$precip presipitasyon olasılığı saptanmıştır. $rainText$timeDetail$umbrellaAdvice Bilgilerinize sunulur."
            AssistantTone.DETAYLI_UZMAN -> "Hidro-Meteorolojik Analiz: Bağıl nem and bulut kapalılık oranı %$precip olasılıkla yağış formasyonuna işaret etmektedir. $rainText$timeDetail$umbrellaAdvice Veriler gün boyu stabil bir seyir izlemektedir."
            AssistantTone.KISA_NET -> "Yağış ihtimali: %$precip. $rainText $timeDetail"
            else -> "Bugün $city'de yağmur ihtimali %$precip seviyesinde. $rainText$timeDetail$umbrellaAdvice"
        }
    }

    // 5) VALİZİME NE ALMALIYIM?
    private fun generatePackingReply(plan: TravelPlan?, tone: AssistantTone): String {
        if (plan == null) {
            return when (tone) {
                AssistantTone.SAMIMI -> "Valizine bakmak isterdim ama henüz kayıtlı bir seyahatin yok canım! 😊 Takvim bölümünden hemen bir rota oluştur, en iyi listeyi hazırlayalım."
                AssistantTone.RESMI -> "Sistemde kayıtlı yaklaşan bir seyahat planı bulunamamıştır. Valiz önerisi oluşturulabilmesi için lütfen Takvim üzerinden seyahat detayı giriniz."
                AssistantTone.DETAYLI_UZMAN -> "Envanter Analizi Hatası: Projeksiyon yapılacak aktif bir seyahat rotası mevcut değildir. Analiz için destinasyon verisi gerekmektedir."
                AssistantTone.KISA_NET -> "Kayıtlı seyahat yok. Önce takvime plan ekle."
                else -> "Asistan şu an yaklaşan bir seyahat planı bulamadı. Valiz önerisi verebilmem için önce Takvim'den bir seyahat oluşturmalısın."
            }
        }

        val today = LocalDate.now()
        val status = getTripStatus(today, plan.startDate, plan.endDate)

        if (status == RecommendationTripStatus.UPCOMING_LOCKED) {
            return "${plan.city} seyahatin için valiz hazırlığına biraz daha vakit var canım! ✨ Hava tahminleri netleştiğinde (seyahate $TRIP_ANALYSIS_WINDOW_DAYS gün kala) sana en uygun listeyi burada sunacağım."
        }

        val city = plan.city
        val snapshot = plan.lastForecastSnapshot
        val temp = snapshot?.maxTemp?.toInt() ?: 20
        val precip = snapshot?.precipitationProbability ?: 0
        val minTemp = snapshot?.minTemp?.toInt() ?: 15

        val items = mutableListOf<String>()
        if (temp > 25) items.addAll(listOf("şort", "ince tişört", "güneş kremi", "şapka"))
        else if (temp > 15) items.addAll(listOf("kot pantolon", "hafif ceket", "spor ayakkabı"))
        else items.addAll(listOf("kalın mont", "yünlü kazak", "atkı", "termal çorap"))

        if (precip > 30) items.add("şemsiye veya yağmurluk")
        if (temp - minTemp > 10) items.add("gece serinliği için hırka")
        items.add("powerbank")

        val listStr = items.joinToString(", ")

        return when (tone) {
            AssistantTone.SAMIMI -> "$city seyahatin için valizini ben hazırladım bile canım! ✨ Sıcaklık $temp°, gece ise $minTemp° civarında olacak. Şu listeyi unutma: $listStr. Şimdiden iyi yolculuklar! ✈️"
            AssistantTone.RESMI -> "$city seyahatiniz için hazırlanan ekipman listesi: Maksimum sıcaklık $temp°C, gece $minTemp°C olarak öngörüldüğünden valizinizde $listStr bulunması tavsiye edilir."
            AssistantTone.DETAYLI_UZMAN -> "Destinasyon Lojistik Analizi ($city): Termal spektrum $minTemp°C ile $temp°C arasındadır. Presipitasyon riski %$precip seviyesindedir. Optimizasyon için önerilen materyaller: $listStr. Hazırlıklarınızı bu verilere göre tamamlayınız."
            AssistantTone.KISA_NET -> "$city Valizi ($temp°/$minTemp°): $listStr."
            else -> "$city seyahatin için valizine şunları almanı öneririm: $listStr. Hava $temp° iken akşam $minTemp° dereceara kadar düşebilir, hazırlıklı olmalısın."
        }
    }

    private fun generateGeneralWeatherReply(weather: WeatherData, tone: AssistantTone, interests: Set<String>, aboutMe: String?): String {
        val city = weather.cityName
        val temp = weather.temperature
        val cond = weather.condition.lowercase(Locale("tr"))
        val feels = weather.feelsLike
        val uv = weather.uvIndex ?: 0
        val hum = weather.humidity ?: 0
        val wind = weather.windSpeed ?: 0.0
        val precip = weather.precipitationProbability ?: 0

        val advice = when {
            precip > 40 -> "Yağış ihtimali yüksek olduğu için dışarı çıkarken tedbirli olmalısın."
            temp.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 20 > 32 -> "Hava oldukça sıcak, bol su tüketmeyi ve gölgede kalmayı unutma."
            wind > 35 -> "Sert rüzgar dışarıda vakit geçirmeyi zorlaştırabilir."
            else -> "Hava genel olarak outdoor aktiviteler için elverişli görünüyor."
        }

        val uvText = if (uv > 5 || (interests.contains("uv_hassasiyeti") && uv > 3)) {
            "UV indeksi $uv ile riskli seviyede, güneş koruması şart."
        } else {
            "Güneşin tadını çıkarabilirsin, UV riski düşük."
        }

        // Bio/Interest personalization
        val extraAdvice = when {
            aboutMe?.lowercase()?.contains("çocuk") == true || interests.contains("cocuklar_icin") ->
                " Çocuklarla dışarı çıkacaksan yanına yedek kıyafet ve güneş koruyucu almanı öneririm."
            interests.contains("migren") && (hum > 75 || uv > 7) ->
                " Hava koşulları migreni tetikleyebilir, bugün kendine ekstra dikkat etmelisin."
            interests.contains("motorsiklet") && wind > 25 ->
                " Yüksek rüzgar hızı sürüş güvenliğini etkileyebilir, ekipmanlarını kontrol etmelisin."
            else -> ""
        }

        return when (tone) {
            AssistantTone.SAMIMI -> "Selam canım! 😊 $city'de hava şu an $temp, ama nemden dolayı $feels gibi hissediliyor. Gökyüzü de $cond, tam gezmelik! $advice $uvText$extraAdvice"
            AssistantTone.RESMI -> "Sayın kullanıcımız, $city lokasyonunda anlık sıcaklık $temp, hissedilen sıcaklık $feels düzeyindedir. Gökyüzü $cond olarak gözlemlenmektedir. $advice $uvText$extraAdvice"
            AssistantTone.KISA_NET -> "$city: $temp ($cond). His: $feels. $advice"
            AssistantTone.DETAYLI_UZMAN -> "$city meteoroloji istasyonundan alınan son verilere göre; sıcaklık $temp, termal algı $feels düzeyindedir. Atmosferik durum $cond olarak raporlanmıştır. Rüzgar hızı $wind km/sa, nem %$hum ve yağış riski %$precip düzeyindedir. $advice $uvText$extraAdvice"
            else -> "$city'de hava $temp ve $cond. Hissedilen sıcaklık ise $feels. $advice $uvText$extraAdvice"
        }
    }

    private fun generateTravelReply(prompt: String, weatherData: WeatherData, travelPlans: List<TravelPlan>, tone: AssistantTone): String {
        val detectedCity = AiIntentParser.detectCity(prompt)
        val today = LocalDate.now()
        val nextPlan = travelPlans.filter { !it.isArchived && !it.endDate.isBefore(today) }.minByOrNull { it.startDate }

        val city = detectedCity ?: nextPlan?.city

        if (city == null) {
            return when (tone) {
                AssistantTone.SAMIMI -> "Henüz planlanmış bir seyahatin bulunmuyor canım. İstersen hemen yeni bir seyahat planlayabiliriz! 😊"
                AssistantTone.RESMI -> "Kayıtlı aktif bir seyahat planınız bulunmamaktadır. Yeni bir seyahat planı oluşturulması önerilir."
                AssistantTone.KISA_NET -> "Planlı seyahat yok. Yeni plan oluşturabiliriz."
                else -> "Henüz planlanmış bir seyahatin bulunmuyor. İstersen yeni bir seyahat planlayabiliriz."
            }
        }

        return when (tone) {
            AssistantTone.SAMIMI -> "$city seyahatin için hava süper görünüyor canım! 😊 Valiz hazırlığı için bana her zaman sorabilirsin."
            AssistantTone.RESMI -> "$city istikametine yapılacak seyahatler için atmosferik koşullar elverişlidir. Seyahat öncesi valiz hazırlığı hususunda tarafımızdan teknik destek alabilirsiniz."
            AssistantTone.KISA_NET -> "$city seyahati için hava uygun. Hazırlıklara başlayabilirsin."
            AssistantTone.DETAYLI_UZMAN -> "$city destinasyonu için yapılan troposferik analizler, seyahat güvenliği and konforu açısından pozitif sonuçlar vermektedir. Lojistik süreçleri başlatmanız tavsiye edilir."
            else -> "$city seyahatin için hava koşulları elverişli görünüyor. Yolculuk öncesi valiz hazırlığına başlamadan tekrar sorabilirsin."
        }
    }

    /**
     * Gelişmiş Seyahat Önerisi - Kart Görünümü İçin (Issue #4 & #3)
     */
    fun generateTravelRecommendation(
        plan: TravelPlan,
        currentSnapshot: ForecastSnapshot?,
        previousSnapshot: ForecastSnapshot? = null,
        tone: AssistantTone = AssistantTone.DENGELI
    ): String {
        val today = LocalDate.now()
        val status = getTripStatus(today, plan.startDate, plan.endDate)

        if (status == RecommendationTripStatus.UPCOMING_LOCKED) {
            return "Detaylı seyahat önerileri yakında hazır olacak. Hava tahminleri seyahat tarihiniz yaklaştıkça daha güvenilir hale gelir."
        }

        if (currentSnapshot == null) return "${plan.city} seyahati için hazırlıklar başlasın! ✈️ Hava verilerini güncellemek için dokunabilirsin."

        val cond = currentSnapshot.conditionSummary?.lowercase(Locale("tr")) ?: "değişken"
        val maxT = currentSnapshot.maxTemp?.toInt() ?: 20

        if (status == RecommendationTripStatus.ONGOING) {
            val daysIntoTrip = ChronoUnit.DAYS.between(plan.startDate, today) + 1
            return when (tone) {
                AssistantTone.SAMIMI -> "${plan.city} seyahatin şu anda devam ediyor canım! 😊 Bugün seyahatinin $daysIntoTrip. günü. Hava $maxT° ve $cond. Keyfini çıkar!"
                AssistantTone.RESMI -> "${plan.city} seyahatiniz devam etmektedir (Gün: $daysIntoTrip). Güncel hava durumu $maxT°C ve $cond olarak raporlanmıştır."
                else -> "${plan.city} seyahatin şu anda devam ediyor. Bugün seyahatinin $daysIntoTrip. günü. Bugünkü hava durumuna göre planını güncelleyebilirsin."
            }
        }

        return when (tone) {
            AssistantTone.SAMIMI -> "${plan.city} seyahatin yaklaşıyor canım! Seni $maxT° sıcaklıkta $cond bir hava bekliyor. Şimdiden hazırlıklara başla derim! 😊"
            AssistantTone.RESMI -> "${plan.city} istikametine yapacağınız seyahat yaklaşmaktadır. Tahmin edilen hava durumu $maxT°C ve $cond olarak bildirilmiştir."
            else -> "${plan.city} seyahatin yaklaşıyor! Seni $maxT° sıcaklıkta $cond bir hava bekliyor. İyi yolculuklar!"
        }
    }
}
