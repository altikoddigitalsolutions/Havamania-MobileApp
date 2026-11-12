package com.havamania

import com.havamania.ui.theme.AssistantTone
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class RecommendationTripStatus {
    UPCOMING_LOCKED,
    UPCOMING_ACTIVE,
    TODAY,
    ONGOING,
    PAST
}

object RecommendationEngine {

    fun getTripStatus(today: LocalDate, startDate: LocalDate, endDate: LocalDate): RecommendationTripStatus {
        val status = TravelStatusResolver.getStatus(startDate, endDate, today)
        return when (status) {
            TravelStatus.PAST -> RecommendationTripStatus.PAST
            TravelStatus.ONGOING -> if (today == startDate) RecommendationTripStatus.TODAY else RecommendationTripStatus.ONGOING
            TravelStatus.UPCOMING -> {
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
        incompleteTodayReply(weather)?.let { return it }
        val tempValue = weather.temperature.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 20
        val cond = weather.condition.lowercase(Locale("tr"))
        val uv = weather.uvIndex ?: 0
        val precip = weather.precipitationProbability ?: 0
        val wind = weather.windSpeed ?: 0.0

        val prefs = personalization?.weatherPreferences ?: WeatherPreferences()

        val sb = StringBuilder()

        // 1. Hava Özeti
        sb.append("Bugün $city için hava $tempValue°C ve $cond. ")

        // 2. Günlük Yaşam Etkisi
        if (precip > 50 || (prefs.rainSensitive && precip > 20)) {
            sb.append("Yağış ihtimali gününüzü etkileyebilir, dışarıda uzun süre kalacaksanız dikkatli olmanızda fayda var. ")
        } else if (tempValue > 32 || (prefs.likesCool && tempValue > 25)) {
            sb.append("Sıcak hava nedeniyle öğle saatlerinde serin yerleri tercih etmenizi öneririm. ")
        } else {
            if (wind > 25 || uv >= 6 || tempValue < 0 || weather.isStale) {
                sb.append("Planınızı mevcut hava uyarılarını dikkate alarak yapın. ")
            } else {
                sb.append("Hava genel olarak günlük planlarınız için elverişli görünüyor. ")
            }
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
             if (tempValue < 5) sb.append("[SEP]❄️ SPOR| Kış sporları planlıyorsan pist durumunu, kar kalınlığını ve yerel uyarıları ayrıca kontrol et.")
        }

        // 5. Risk Uyarısı
        if (uv >= 6 || (interests.contains("uv_hassasiyeti") && uv >= 4) || (prefs.uvSensitive && uv >= 4)) {
            sb.append("[SEP]⚠️ RİSK| UV indeksi ($uv) yüksek! Güneş kremi ve gözlük kullanmayı ihmal etme.")
        } else if (wind > 35 || (prefs.windSensitive && wind > 20)) {
            sb.append("[SEP]⚠️ RİSK| Sert rüzgar uyarısı! Dışarıda savrulabilecek eşyalara dikkat.")
        }

        return sb.toString().trim()
    }

    private fun incompleteTodayReply(weather: WeatherData): String? {
        val temperature = weather.temperature.removeSuffix("°").trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        val wind = weather.windSpeed?.takeIf { it.isFinite() && it >= 0 }
        val rain = weather.precipitationProbability?.takeIf { it in 0..100 }
        val uv = weather.uvIndex?.takeIf { it >= 0 }
        if (temperature != null && wind != null && rain != null && uv != null && !weather.isStale && WeatherUtils.isKnownCode(weather.weatherCode)) return null
        val observations = buildList {
            temperature?.let { add("sıcaklık ${weather.temperature}") }
            wind?.let { add("rüzgâr $it km/sa") }
            rain?.let { add("yağış olasılığı %$it") }
            uv?.let { add("UV indeksi $it") }
        }
        val risks = buildList {
            if (wind != null && wind > 25) add("güçlü rüzgâr")
            if (rain != null && rain > 40) add("yağış riski")
            if (uv != null && uv >= 6) add("yüksek UV")
            if (temperature != null && (temperature < 0 || temperature > 35)) add("ekstrem sıcaklık")
        }
        return buildString {
            append("${weather.cityName} için ")
            append(if (weather.isStale) "hava verileri güncel değil. " else "hava verileri eksik. ")
            if (observations.isNotEmpty()) append("Mevcut kayıtta ${observations.joinToString(", ")}. ")
            if (risks.isNotEmpty()) append("Dikkat: ${risks.joinToString(", ")}. ")
            append("Açık hava ve kıyafet önerisi için güncel ölçümleri kontrol edin.")
        }
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
        val targetDate = AiIntentParser.detectDate(userPrompt)
        val localWeatherIntent = intent != AiIntent.PACKING && intent != AiIntent.TRAVEL
        if (localWeatherIntent && promptCity != null && !promptCity.equals(weatherData.cityName, ignoreCase = true)) {
            return "$promptCity için doğrulanmış hava verisi bulunmuyor. Mevcut veriler ${weatherData.cityName} için; önce ilgili şehri seçin."
        }
        if (localWeatherIntent && intent != AiIntent.WEEKEND_FORECAST && targetDate != null &&
            targetDate != ForecastTime.localNow(weatherData.timezone, java.time.Instant.now()).toLocalDate()) {
            return "$targetDate için saatlik ölçümlerle doğrulanmış bir yanıt hazırlayamıyorum. Takvimde ilgili günün tahminini kontrol edin."
        }
        val rainQuestion = intent == AiIntent.GENERAL_WEATHER &&
            (userPrompt.contains("yağmur", true) || userPrompt.contains("yağış", true))
        if (localWeatherIntent && intent != AiIntent.WEEKEND_FORECAST) {
            if (rainQuestion) {
                if (weatherData.isStale || weatherData.precipitationProbability?.let { it in 0..100 } != true)
                    return "${weatherData.cityName} için güncel yağış olasılığı bulunmuyor. Eksik veri yağmur yağmayacağı anlamına gelmez."
            } else {
                incompleteTodayReply(weatherData)?.let { return it }
                if ((intent == AiIntent.CLOTHING || intent == AiIntent.ACTIVITY) &&
                    (weatherData.humidity?.let { it in 0..100 } != true ||
                        weatherData.feelsLike.removeSuffix("°").toDoubleOrNull()?.isFinite() != true)) {
                    return "${weatherData.cityName} için hissedilen sıcaklık veya nem ölçümü eksik. Kesin kıyafet ve aktivite önerisi için güncel ölçümleri kontrol edin."
                }
            }
        }
        // Context Selection
        val targetCity: String = promptCity ?: weatherData.cityName
        val currentTemp = weatherData.temperature.removeSuffix("°").trim().toDoubleOrNull()?.toInt() ?: 20
        val feelsLike = weatherData.feelsLike.removeSuffix("°").toDoubleOrNull()?.toInt() ?: currentTemp
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
            AssistantTone.SAMIMI -> "$city için bugün hava tam bir muamma canım! 😊 $baseAdvice$uvAdvice$windAdvice$interestAdvice Güzel görünmeyi unutma!"
            AssistantTone.RESMI -> "$city lokasyonu için meteorolojik veriler ışığında şu giyim önerileri sunulmaktadır: $baseAdvice$uvAdvice$windAdvice$interestAdvice Bilgilerinize sunulur."
            AssistantTone.DETAYLI_UZMAN -> "Meteorolojik Analiz ($city): Termal algı $feels°C ve bağıl nem %$hum seviyelerindedir. Bu parametreler altında $baseAdvice$uvAdvice$windAdvice$interestAdvice Termal regülasyonunuzu bu doğrultuda optimize ediniz."
            AssistantTone.KISA_NET -> "$city: $temp° ($baseAdvice). UV: $uv. $uvAdvice"
            else -> "Bugün $city için $baseAdvice$uvAdvice$windAdvice$interestAdvice"
        }
    }

    // 2) HAFTA SONU HAVA NASIL?
    private fun generateWeekendReply(weather: WeatherData, tone: AssistantTone, interests: Set<String>): String =
        weekendWeatherReply(weather, ForecastTime.localNow(weather.timezone, java.time.Instant.now()).toLocalDate())

    // 3) DIŞARI ÇIKMAK İÇİN UYGUN MU?
    private fun generateActivityReply(city: String, temp: Int, uv: Int, precip: Int, wind: Double, hum: Int, tone: AssistantTone, interests: Set<String>): String {
        val isGood = precip < 25 && wind < 35 && temp in 12..30 && uv <= 7 && hum <= 80
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
            AssistantTone.SAMIMI -> "Bugün $city için dışarı çıkmak için hava $status canım! 😊 $reason$suggestion Şöyle güzel bir yürüyüş ruhuna iyi gelirdi."
            AssistantTone.RESMI -> "$city bölgesi için yapılan analizlerde dış mekan faaliyetlerinin icrası $status olarak değerlendirilmiştir. $reason$suggestion Tedbirli olmanız önerilir."
            AssistantTone.DETAYLI_UZMAN -> "Aktivite Uygunluk İndeksi ($city): Mevcut atmosferik parametreler (Yağış: %$precip, Rüzgar: $wind km/sa, UV: $uv) incelendiğinde durumun $status olduğu görülmektedir. $reason$suggestion"
            AssistantTone.KISA_NET -> "Hava $status. $reason $suggestion"
            else -> "Bugün $city için dışarı çıkmak $status. $reason$suggestion Planlarını bu verilere göre yapabilirsin."
        }
    }

    // 4) BUGÜN YAĞMUR YAĞACAK MI?
    private fun generateRainReply(city: String, precip: Int, weather: WeatherData, tone: AssistantTone): String {
        val hourlyPrecip = weather.hourlyForecast.take(12).filter { (it.precipitationProbability ?: 0) > 30 }
        val hours = hourlyPrecip.joinToString(", ") { it.time.split("T").last().take(5) }

        val rainText = when {
            precip > 70 -> "Yağış olasılığı yüksek; yağmura hazırlıklı olun."
            precip > 30 -> "Günün belli bölümlerinde yağış geçişleri olabilir, gökyüzü her an sürpriz yapabilir."
            else -> "Yağış olasılığı düşük; bu, yağmur yağmayacağı garantisi değildir."
        }

        val umbrellaAdvice = if (precip > 30) " Yanına şemsiye ve su geçirmeyen ayakkabı almayı değerlendirebilirsin." else " Dışarı çıkmadan önce saatlik tahmini yeniden kontrol et."
        val timeDetail = if (hourlyPrecip.isNotEmpty()) " Özellikle şu saatlere dikkat: $hours." else ""

        return when (tone) {
            AssistantTone.SAMIMI -> "Bugün $city için yağmur ihtimali %$precip canım. $rainText$timeDetail$umbrellaAdvice Islanmanı hiç istemem! ☔"
            AssistantTone.RESMI -> "Günlük yağış projeksiyonu: %$precip presipitasyon olasılığı saptanmıştır. $rainText$timeDetail$umbrellaAdvice Bilgilerinize sunulur."
            AssistantTone.DETAYLI_UZMAN -> "Günlük yağış olasılığı %$precip. $rainText$timeDetail$umbrellaAdvice"
            AssistantTone.KISA_NET -> "Yağış ihtimali: %$precip. $rainText $timeDetail"
            else -> "Bugün $city için yağmur ihtimali %$precip seviyesinde. $rainText$timeDetail$umbrellaAdvice"
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
            else -> "$city seyahatin için valizine şunları almanı öneririm: $listStr. Hava $temp° iken akşam $minTemp° dereceye kadar düşebilir, hazırlıklı olmalısın."
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

        return "$city seyahati için şehir ve tarih aralığıyla eşleşen güzergâh analizi gerekir. Mevcut ${weatherData.cityName} hava kaydı yolculuk koşullarını doğrulamaz. Takvimden seyahatin güncel hava analizini açın."
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
        val maxT = currentSnapshot.maxTemp?.takeIf { it.isFinite() }?.toInt()
            ?: return "${plan.city} seyahati için en yüksek sıcaklık verisi bulunmuyor. Güncel seyahat analizini kontrol edin."

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

    /**
     * P3.2: Deterministic short summary for the insight area.
     */
    fun getShortWeatherSummary(weather: WeatherData): String {
        val temp = weather.temperature.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 20
        val condition = weather.condition.lowercase()
        val precip = weather.precipitationProbability ?: 0
        val wind = weather.windSpeed ?: 0.0

        return when {
            precip > 70 -> "Kuvvetli yağış bekleniyor, şemsiyesiz çıkmayın."
            precip > 30 -> "Günün ilerleyen saatlerinde yağış görülebilir."
            temp > 32 -> "Hava oldukça sıcak, bol su tüketmeyi unutmayın."
            temp < 5 -> "Soğuk bir gün, sıkı giyinmenizi öneririz."
            wind > 40 -> "Sert rüzgârlara karşı tedbirli olun."
            condition.contains("bulut") -> "Bugün hava yer yer bulutlu seyredecek."
            condition.contains("açık") || condition.contains("güneş") -> "Güneşli ve açık bir gökyüzü sizi bekliyor."
            else -> "Bugün hava $condition, sıcaklık $temp°."
        }
    }

    /**
     * P4.1.1: Historical analysis summary formatter.
     */
    fun getShortWeatherSummaryFromAnalysis(analysis: TravelWeatherAnalysis, isOngoing: Boolean = false): String {
        if (analysis.summary.isNotBlank()) return analysis.summary
        val prefix = if (isOngoing) "Devam eden seyahat tahmini" else "Seyahat tahmini"
        val rain = analysis.rainRiskPercent?.let { " Yağış olasılığı: %$it." } ?: " Yağış olasılığı bilinmiyor."
        return "$prefix: dönem ortalaması ${analysis.averageTemperature.toInt()}°C.$rain"
    }
}
