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

        val uv = weatherData.uvIndex?.takeIf { it in 0..25 } ?: 0
        val precip = weatherData.precipitationProbability?.takeIf { it in 0..100 } ?: 0
        val wind = weatherData.windSpeed?.takeIf { it.isFinite() && it in 0.0..500.0 } ?: 0.0

        val highlights = mutableListOf<String>()
        if (uv >= 6) highlights.add("UV")
        if (precip > 40) highlights.add("yağış")
        if (wind > 30) highlights.add("rüzgar")
        if (userInterests.contains("uv_hassasiyeti") && uv >= 4) highlights.add("hassasiyet")
        if (userInterests.contains("kamp") && (precip > 20 || measuredTemperature(weatherData.temperature)?.let { it < 10.0 } == true)) highlights.add("kamp")

        return HavamaniaRecommendation(
            message = message,
            type = RecommendationType.GENERAL,
            highlightedWords = highlights.distinct(),
            priority = if (tripWeatherCodePriority(weatherData.weatherCode) >= 2 || uv >= 8 || precip > 70 || (userInterests.contains("uv_hassasiyeti") && uv >= 6)) RecommendationPriority.HIGH else RecommendationPriority.LOW
        )
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
        val tempValue = requireNotNull(measuredTemperature(weather.temperature))
        val cond = weather.condition.lowercase(Locale("tr"))
        val uv = weather.uvIndex ?: 0
        val precip = weather.precipitationProbability ?: 0
        val wind = weather.windSpeed ?: 0.0

        val prefs = personalization?.weatherPreferences ?: WeatherPreferences()

        val sb = StringBuilder()

        // 1. Hava Özeti
        sb.append("Bugün $city için hava ${temperatureNumber(tempValue)}°C ve $cond. ")

        // 2. Günlük Yaşam Etkisi
        val codedRisk = tripWeatherRiskLabel(weather.weatherCode)
        if (codedRisk != null) {
            sb.append("$codedRisk. Planınızı güncel yerel uyarılara göre değerlendirin. ")
        } else if (precip > 50 || (prefs.rainSensitive && precip > 20)) {
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
        if (interests.any { it.contains("koşu", true) } && tempValue in 10.0..24.0 && precip < 20) {
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
        val temperature = measuredTemperature(weather.temperature)
        val wind = weather.windSpeed?.takeIf { it.isFinite() && it in 0.0..500.0 }
        val rain = weather.precipitationProbability?.takeIf { it in 0..100 }
        val uv = weather.uvIndex?.takeIf { it in 0..25 }
        if (temperature != null && wind != null && rain != null && uv != null && !weather.isStale && WeatherUtils.isKnownCode(weather.weatherCode)) return null
        val observations = buildList {
            temperature?.let { add("sıcaklık ${temperatureNumber(it)}°C") }
            wind?.let { add("rüzgâr $it km/sa") }
            rain?.let { add("yağış olasılığı %$it") }
            uv?.let { add("UV indeksi $it") }
        }
        val risks = buildList {
            tripWeatherRiskLabel(weather.weatherCode)?.let(::add)
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
        travelPlans: List<TravelPlan> = emptyList(),
        now: java.time.Instant = java.time.Instant.now()
    ): String {
        val intent = AiIntentParser.detectIntent(userPrompt)
        if (intent == AiIntent.PACKING) {
            val selected = selectTravelAdvicePlan(travelPlans, now)
            if (selected.needsTimezone) return "Seyahatlerin konum saat dilimi bilgisi eksik. Valiz hazırlığı için Takvim'den ilgili planın analizini yenileyin."
            val plan = selected.plan ?: return "Yaklaşan kayıtlı seyahat bulunmuyor. Valiz hazırlığı için Takvim'den bir plan seçin."
            return generatePackingReply(plan, tone, requireNotNull(selected.today), now)
        }
        if (weatherData == null) return "Hava durumu verilerine şu an erişilemiyor. Lütfen kısa süre sonra tekrar dene."
        val today = ForecastTime.knownZone(weatherData.timezone)?.let { now.atZone(it).toLocalDate() }

        val promptCity = AiIntentParser.detectCity(userPrompt)
        val targetDate = today?.let { AiIntentParser.detectDate(userPrompt, it) }
        val localWeatherIntent = intent != AiIntent.PACKING && intent != AiIntent.TRAVEL
        if (localWeatherIntent && promptCity != null && !promptCity.equals(weatherData.cityName, ignoreCase = true)) {
            return "$promptCity için doğrulanmış hava verisi bulunmuyor. Mevcut veriler ${weatherData.cityName} için; önce ilgili şehri seçin."
        }
        if (today == null && intent != AiIntent.TRAVEL) return AiWeatherReply.locationTimeUnavailable(weatherData.cityName)
        if (localWeatherIntent && intent != AiIntent.WEEKEND_FORECAST && targetDate != null &&
            targetDate != today) {
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
                        measuredTemperature(weatherData.feelsLike, -120.0..85.0) == null)) {
                    return "${weatherData.cityName} için hissedilen sıcaklık veya nem ölçümü eksik. Kesin kıyafet ve aktivite önerisi için güncel ölçümleri kontrol edin."
                }
            }
        }
        if (intent == AiIntent.ACTIVITY && tripWeatherCodePriority(weatherData.weatherCode) >= 2) {
            val risk = tripWeatherRiskLabel(weatherData.weatherCode)?.lowercase(Locale.forLanguageTag("tr-TR")) ?: "olumsuz hava koşulları"
            return "${weatherData.cityName} için $risk kaydı var. Açık hava etkinlikleri açısından risk işareti bulunuyor; güncel yerel uyarıları kontrol edin."
        }
        // Context Selection
        val targetCity: String = promptCity ?: weatherData.cityName
        val currentTemp = measuredTemperature(weatherData.temperature)
        val feelsLike = measuredTemperature(weatherData.feelsLike, -120.0..85.0)
        val uv = weatherData.uvIndex ?: 0
        val humidity = weatherData.humidity ?: 50
        val wind = weatherData.windSpeed ?: 10.0
        val precip = weatherData.precipitationProbability ?: 0

        return when (intent) {
            AiIntent.CLOTHING -> generateClothingReply(targetCity, requireNotNull(currentTemp), requireNotNull(feelsLike), uv, wind, humidity, tone, interests)
            AiIntent.ACTIVITY -> generateActivityReply(targetCity, requireNotNull(currentTemp), uv, precip, wind, humidity, tone, interests)
            AiIntent.WEEKEND_FORECAST -> weekendWeatherReply(weatherData, requireNotNull(today))
            AiIntent.GENERAL_WEATHER -> {
                if (userPrompt.contains("yağmur", ignoreCase = true) || userPrompt.contains("yağış", ignoreCase = true)) {
                    generateRainReply(targetCity, precip, weatherData, tone)
                } else {
                    generateStructuredWeatherReply(weatherData, tone, interests, aboutMe)
                }
            }
            AiIntent.TRAVEL -> generateTravelReply(userPrompt, weatherData, travelPlans, tone, now)
            else -> generateStructuredWeatherReply(weatherData, tone, interests, aboutMe)
        }
    }

    // 1) BUGÜN NE GİYMELİYİM?
    private fun generateClothingReply(city: String, temp: Double, feels: Double, uv: Int, wind: Double, hum: Int, tone: AssistantTone, interests: Set<String>): String {
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
            AssistantTone.DETAYLI_UZMAN -> "Meteorolojik Analiz ($city): Termal algı ${temperatureNumber(feels)}°C ve bağıl nem %$hum seviyelerindedir. Bu parametreler altında $baseAdvice$uvAdvice$windAdvice$interestAdvice Termal regülasyonunuzu bu doğrultuda optimize ediniz."
            AssistantTone.KISA_NET -> "$city: ${temperatureNumber(temp)}° ($baseAdvice). UV: $uv. $uvAdvice"
            else -> "Bugün $city için $baseAdvice$uvAdvice$windAdvice$interestAdvice"
        }
    }

    // 3) DIŞARI ÇIKMAK İÇİN UYGUN MU?
    private fun generateActivityReply(city: String, temp: Double, uv: Int, precip: Int, wind: Double, hum: Int, tone: AssistantTone, interests: Set<String>): String {
        val isGood = precip < 25 && wind < 35 && temp in 12.0..30.0 && uv <= 7 && hum <= 80
        val status = if (isGood) "oldukça uygun" else "biraz riskli"

        val details = mutableListOf<String>()
        if (temp !in 12.0..30.0) details.add("sıcaklık (${temperatureNumber(temp)}°C)")
        if (precip >= 25) details.add("yağış ihtimali (%$precip)")
        if (wind > 30) details.add("sert rüzgar ($wind km/sa)")
        if (uv > 7) details.add("yüksek UV radyasyonu")
        if (hum > 80) details.add("yüksek nem oranı")

        val reason = if (details.isNotEmpty()) "Ancak ${details.joinToString(", ")} nedeniyle dikkatli olmalısın." else "Yerel koşulları ve güncel uyarıları ayrıca kontrol edin."

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
    private fun generatePackingReply(plan: TravelPlan, tone: AssistantTone, today: LocalDate, now: java.time.Instant): String {
        val status = getTripStatus(today, plan.startDate, plan.endDate)

        if (status == RecommendationTripStatus.UPCOMING_LOCKED) {
            return "${plan.city} seyahatin için valiz hazırlığına biraz daha vakit var canım! ✨ Hava tahminleri netleştiğinde (seyahate $TRIP_ANALYSIS_WINDOW_DAYS gün kala) sana en uygun listeyi burada sunacağım."
        }

        if (!tripForecastMatchesPlan(plan, plan.lastForecastSnapshot, now))
            return "${plan.city} için tahminin hedef ve tarih eşleşmesi doğrulanamadı. Valiz önerisinde kullanılacak doğrulanmış hava verisi eksik; Takvim'den analizi yenileyin."
        val snapshot = plan.lastForecastSnapshot?.validAdviceData()
        val generated = snapshot?.generatedAt?.takeIf { it > 0 }?.let(java.time.Instant::ofEpochMilli)
        val zone = snapshot?.timezone?.let(::validTripForecastZone)
        val unverifiedTime = generated == null || generated.isAfter(now) || zone == null || generated.atZone(zone).toLocalDate() != today

        fun number(value: Double) = String.format(Locale.forLanguageTag("tr-TR"), "%.1f", value)
        val measurements = buildList {
            snapshot?.maxTemp?.let { add("Seyahat dönemi için tahmini en yüksek sıcaklık: ${number(it)}°C.") }
            snapshot?.minTemp?.let { add("Seyahat dönemi için tahmini en düşük sıcaklık: ${number(it)}°C.") }
            snapshot?.precipitationProbability?.let { add("Yağış olasılığı: %$it.") }
        }
        val title = when (tone) {
            AssistantTone.SAMIMI -> "${plan.city} seyahatin için valizi birlikte hazırlayalım."
            AssistantTone.KISA_NET -> "${plan.city} için valiz hazırlığı."
            else -> "${plan.city} seyahati için valiz hazırlığı."
        }
        return buildList {
            add(title)
            add(tripForecastTimeLabel(snapshot, now))
            if (unverifiedTime) add("Tahminin güncelliği doğrulanamadı. Analizi yenileyin; aşağıdaki hazırlıklar kayıtlı tahmine dayanır.")

            if (measurements.isNotEmpty()) add("Kayıtlı seyahat tahminine göre:")
            addAll(measurements)
            add(travelWeatherPacking(snapshot, null))
            add("Seyahatten önce güncel tahmini ve yerel uyarıları kontrol edin.")
        }.joinToString(" ")
    }

    private fun generateTravelReply(prompt: String, weatherData: WeatherData, travelPlans: List<TravelPlan>, tone: AssistantTone, now: java.time.Instant): String {
        val detectedCity = AiIntentParser.detectCity(prompt)
        val selection = if (detectedCity == null) selectTravelAdvicePlan(travelPlans, now) else null
        if (selection?.needsTimezone == true)
            return "Kayıtlı seyahatlerin konum saat dilimi doğrulanamadı. Yaklaşan seyahati seçebilmek için Takvim'den ilgili planın analizini yenileyin."
        val nextPlan = selection?.plan

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
        tone: AssistantTone = AssistantTone.DENGELI,
        now: java.time.Instant = java.time.Instant.now()
    ): String {
        if (plan.endDate.isBefore(plan.startDate)) return "Seyahat tarih aralığı geçersiz. Takvim'den planı kontrol edin."
        val snapshot = currentSnapshot ?: plan.lastForecastSnapshot
        val zone = snapshot?.timezone?.let(::validTripForecastZone)
            ?: return "${plan.city} seyahatinin konum saat dilimi doğrulanamadı. Seyahat durumunu doğrulamak için hava analizini yenileyin."
        val today = now.atZone(zone).toLocalDate()
        val status = getTripStatus(today, plan.startDate, plan.endDate)
        if (status == RecommendationTripStatus.PAST)
            return "${plan.city} seyahati tamamlandı. Kayıtlı dönem analizini geçmiş seyahatlerden inceleyebilirsiniz."
        if (status == RecommendationTripStatus.UPCOMING_LOCKED)
            return "Detaylı seyahat önerileri yakında hazır olacak. Hava tahminleri seyahat tarihiniz yaklaştıkça daha güvenilir hale gelir."
        val heading = when (status) {
            RecommendationTripStatus.TODAY, RecommendationTripStatus.ONGOING ->
                "${plan.city} seyahati devam ediyor (seyahatin ${ChronoUnit.DAYS.between(plan.startDate, today) + 1}. günü)."
            else -> "${plan.city} seyahati yaklaşıyor."
        }
        if (currentSnapshot == null) return "$heading Bu seyahat için yeni hava tahmini bulunmuyor. Analizi yenileyin."
        return "$heading Kayıtlı seyahat dönemi tahmini: ${travelWeatherAdvice(currentSnapshot, tone, null)}"
    }

    /**
     * P3.2: Deterministic short summary for the insight area.
     */
    fun getShortWeatherSummary(weather: WeatherData): String {
        incompleteTodayReply(weather)?.let { return it }
        val temp = requireNotNull(measuredTemperature(weather.temperature))
        val condition = weather.condition.lowercase(Locale.forLanguageTag("tr-TR"))
        val precip = requireNotNull(weather.precipitationProbability)
        val wind = requireNotNull(weather.windSpeed)
        tripWeatherRiskLabel(weather.weatherCode)?.let { return "$it. Güncel yerel uyarıları kontrol edin." }
        return when {
            precip > 70 -> "Kuvvetli yağış bekleniyor, şemsiyesiz çıkmayın."
            precip > 30 -> "Günün ilerleyen saatlerinde yağış görülebilir."
            temp > 32 -> "Hava oldukça sıcak, bol su tüketmeyi unutmayın."
            temp < 5 -> "Soğuk bir gün, sıkı giyinmenizi öneririz."
            wind > 40 -> "Sert rüzgârlara karşı tedbirli olun."
            condition.contains("bulut") -> "Hava bulutlu görünüyor; güncel tahmini kontrol edin."
            condition.contains("açık") || condition.contains("güneş") -> "Hava açık görünüyor; güncel koşulları kontrol edin."
            else -> "Kayıtlı hava: $condition, sıcaklık ${temperatureNumber(temp)}°C."
        }
    }

    /**
     * P4.1.1: Historical analysis summary formatter.
     */
    fun getShortWeatherSummaryFromAnalysis(analysis: TravelWeatherAnalysis, isOngoing: Boolean = false): String {
        if (analysis.summary.isNotBlank()) return analysis.summary
        val prefix = if (isOngoing) "Devam eden seyahat tahmini" else "Seyahat tahmini"
        val rain = analysis.rainRiskPercent?.takeIf { it in 0..100 }?.let { " Yağış olasılığı: %$it." }
            ?: " Yağış olasılığı bilinmiyor."
        val temperature = analysis.averageTemperature.takeIf { it.isFinite() && it in -100.0..65.0 }
            ?.let { "dönem ortalaması ${it.toInt()}°C." } ?: "Ortalama sıcaklık bilinmiyor."
        return "$prefix: $temperature$rain"
    }
}
