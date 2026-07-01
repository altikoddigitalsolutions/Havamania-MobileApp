package com.havamania

import com.havamania.ui.theme.AssistantTone
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object RecommendationEngine {

    /**
     * Günlük hava durumu önerisi oluşturur - Kişiselleştirme ve Sağlık Hassasiyeti eklendi.
     */
    fun generateTodayRecommendation(
        weatherData: WeatherData,
        userInterests: Set<String> = emptySet(),
        aboutMe: String? = null,
        tone: AssistantTone = AssistantTone.DENGELI
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

        val isNight = !weatherData.isDay

        // 1. TONA GÖRE GİRİŞ VE ANA MESAJ OLUŞTURMA
        val finalMessage = when (tone) {
            AssistantTone.SAMIMI -> buildSamimiRecommendation(city, rawCondition, isNight, tempMin, tempMax, uvIndex, humidity, precipProb, windSpeed, userInterests, aboutMeLower)
            AssistantTone.RESMI -> buildResmiRecommendation(city, rawCondition, isNight, tempMin, tempMax, uvIndex, humidity, precipProb, windSpeed, userInterests, aboutMeLower)
            AssistantTone.KISA_NET -> buildKisaNetRecommendation(city, rawCondition, isNight, tempMin, tempMax, uvIndex, humidity, precipProb, windSpeed, userInterests, aboutMeLower)
            AssistantTone.DETAYLI_UZMAN -> buildUzmanRecommendation(city, rawCondition, isNight, tempMin, tempMax, uvIndex, humidity, precipProb, windSpeed, userInterests, aboutMeLower)
            else -> buildDengeliRecommendation(city, rawCondition, isNight, tempMin, tempMax, uvIndex, humidity, precipProb, windSpeed, userInterests, aboutMeLower)
        }

        // Highlightları ve önceliği belirle (Teknik metinlerden bağımsız genel mantık)
        if (uvIndex >= 6) { highlights.add("UV"); if (uvIndex >= 8) priority = RecommendationPriority.HIGH }
        if (humidity > 75) highlights.add("nem")
        if (precipProb > 40) highlights.add("yağış")
        if (windSpeed > 30) highlights.add("rüzgar")

        return HavamaniaRecommendation(
            message = finalMessage,
            type = primaryType,
            highlightedWords = highlights.distinct(),
            priority = priority
        )
    }

    private fun buildSamimiRecommendation(city: String, cond: String, isNight: Boolean, min: Int, max: Int, uv: Int, hum: Int, precip: Int, wind: Double, interests: Set<String>, bio: String): String {
        val intro = when {
            cond.contains("yağmur") || cond.contains("sağanak") -> if (isNight) "Gece yağmuru $city semalarını serinletiyor canım. 🌧️" else "Bugün $city'da şemsiyeler başrolde, yağmura hazır mısın? ☂️"
            cond.contains("kar") -> if (isNight) "Bembeyaz bir gece seni bekliyor canım, karın tadını çıkar! ❄️" else "Dışarısı tam bir masal diyarı gibi, karın tadını çıkar canım! ☃️"
            cond.contains("bulut") -> if (isNight) "$city'da bulutlu bir gece, yıldızları görmek zor olabilir tatlım." else "Gökyüzü biraz dertli bugün, bulutlar $city'da hakimiyeti kurmuş."
            cond.contains("güneş") || cond.contains("açık") -> if (isNight) "Yıldızlar bu gece $city üzerinde pırıl pırıl parlıyor canım. ✨" else "Güneş bugün tüm enerjisini harcıyor, harika bir gün seni bekliyor! ☀️"
            else -> if (isNight) "$city'da sakin bir gece hakim canım." else "Bugün $city semalarında $cond bir hava var."
        }
        val tempStr = "Hava $min°/$max° arası, bence tam gezmelik!"
        val advice = mutableListOf<String>()
        if (uv > 6 && !isNight) advice.add("güneş kremini sakın unutma")
        if (precip > 30) advice.add("yanına bir şemsiye alırsan iyi olur")
        if (max > 28) advice.add("ince pamuklu bir şeyler giymeni öneririm")

        val bioAdvice = if (bio.contains("çocuk") || bio.contains("aile")) " Sevdiklerinle vakit geçirmek için de bence harika bir zaman!" else ""
        val prefix = if (advice.isNotEmpty()) " Bence " else ""

        return "$intro $tempStr$prefix${advice.joinToString(", ", postfix = ".")}$bioAdvice"
    }

    private fun buildResmiRecommendation(city: String, cond: String, isNight: Boolean, min: Int, max: Int, uv: Int, hum: Int, precip: Int, wind: Double, interests: Set<String>, bio: String): String {
        val status = if (isNight) "gece saatlerinde" else "gün içerisinde"
        val intro = "$city bölgesinde $status $cond hava koşullarının hakim olması öngörülmektedir."
        val temp = "Sıcaklık değerleri minimum $min°C, maksimum $max°C olarak ölçülmüştür."
        val measures = mutableListOf<String>()
        if (uv > 6 && !isNight) measures.add("yüksek UV radyasyonuna karşı koruyucu ekipman kullanımı önerilir")
        if (precip > 30) measures.add("olası presipitasyona karşı tedbir alınması uygun olacaktır")
        if (max > 28) measures.add("hafif ve açık renkli tekstil ürünlerinin tercihi konforunuzu artıracaktır")

        return "$intro $temp ${measures.joinToString(". ", postfix = ".", transform = { it.replaceFirstChar { c -> c.uppercase() } })}"
    }

    private fun buildDengeliRecommendation(city: String, cond: String, isNight: Boolean, min: Int, max: Int, uv: Int, hum: Int, precip: Int, wind: Double, interests: Set<String>, bio: String): String {
        val intro = if (isNight) "$city'de $cond ve sakin bir gece etkisini sürdürüyor." else "$city'de bugün $cond bir hava bekleniyor."
        val temp = "$min°/$max° sıcaklık aralığında, günün tadını çıkarabilirsin."
        val tips = mutableListOf<String>()
        if (uv > 6 && !isNight) tips.add("Güneş kremini ihmal etme")
        if (precip > 30) tips.add("Yanına şemsiye alabilirsin")
        if (max > 28) tips.add("İnce ve pamuklu kıyafetler giyebilirsin")

        return "$intro $temp ${tips.joinToString(". ")}"
    }

    private fun buildKisaNetRecommendation(city: String, cond: String, isNight: Boolean, min: Int, max: Int, uv: Int, hum: Int, precip: Int, wind: Double, interests: Set<String>, bio: String): String {
        val parts = mutableListOf<String>()
        parts.add("$city: $cond ($min°/$max°)")
        if (uv > 6 && !isNight) parts.add("UV Yüksek")
        if (precip > 30) parts.add("Yağış riski")
        if (max > 28) parts.add("İnce giyin, bol su iç")
        return parts.joinToString(". ")
    }

    private fun buildUzmanRecommendation(city: String, cond: String, isNight: Boolean, min: Int, max: Int, uv: Int, hum: Int, precip: Int, wind: Double, interests: Set<String>, bio: String): String {
        val timeSpan = if (isNight) "Gece periyodunda" else "Gündüz periyodunda"
        val intro = "Meteorolojik Analiz Raporu ($city): $timeSpan $cond atmosferik olaylar ve troposferik hareketlilik gözlemlenmektedir."
        val detail = "Sıcaklık spektrumu $min°C ile $max°C aralığındadır. %$hum bağıl nem oranı, evaporatif soğumayı kısıtlayarak termal algıyı (hissedilen sıcaklık) yukarı yönlü manipüle edebilir."
        val windInfo = if (wind > 5) " Rüzgar vektörü anlık $wind km/sa hıza ulaşarak sirkülasyon sağlamaktadır." else " Atmosferik stabilite nedeniyle rüzgar hızı nominal değerlerin altındadır ($wind km/sa); serinletici etkisi kısıtlıdır."
        val analysis = mutableListOf<String>()
        if (uv > 6 && !isNight) analysis.add("UV indeksi $uv (Yüksek) seviyesine ulaşacağından, fotodermatolojik koruma protokolleri (güneş koruyucu, şapka) önceliklendirilmelidir")
        if (precip > 40) analysis.add("Hidro-meteorolojik veriler %$precip presipitasyon olasılığı verdiğinden, dış mekan operasyonlarında mobilite kısıtlamaları öngörülmelidir")

        return "$intro $detail$windInfo ${analysis.joinToString(". ", postfix = ". ")}"
    }

    /**
     * AI Asistan Yanıtları - Kişiselleştirme ve Tonlama
     */
    fun generateAssistantFallbackReply(
        userPrompt: String,
        weatherData: WeatherData?,
        aboutMe: String? = null,
        interests: Set<String> = emptySet(),
        tone: AssistantTone = AssistantTone.DENGELI,
        travelPlans: List<TravelPlan> = emptyList()
    ): String {
        if (weatherData == null) return "Hava durumu verilerine şu an erişilemiyor."

        val intent = AiIntentParser.detectIntent(userPrompt)
        val today = LocalDate.now()
        val nextPlan = travelPlans
            .filter { !it.isArchived && !it.endDate.isBefore(today) }
            .minByOrNull { it.startDate }

        // Şehir tespiti
        val promptCity = AiIntentParser.detectCity(userPrompt)

        // Mantıksal ayrım: Seyahatle ilgili bir niyet varsa ve seyahat planı varsa onu kullan
        // Ama kullanıcı spesifik bir şehir sormuşsa (örn: "İstanbul hava nasıl") onu önceliklendir.
        val isTravelIntent = intent == AiIntent.TRAVEL || intent == AiIntent.PACKING || intent == AiIntent.TRIP_RISK

        if (isTravelIntent && promptCity == null && nextPlan == null) {
            return when (tone) {
                AssistantTone.SAMIMI -> "Henüz planlanmış bir seyahatin bulunmuyor canım. İstersen hemen yeni bir seyahat planlayabiliriz! 😊"
                AssistantTone.RESMI -> "Kayıtlı aktif bir seyahat planınız bulunmamaktadır. Yeni bir seyahat planı oluşturulması önerilir."
                AssistantTone.KISA_NET -> "Planlı seyahat yok. Yeni plan oluşturabiliriz."
                else -> "Henüz planlanmış bir seyahatin bulunmuyor. İstersen yeni bir seyahat planlayabiliriz."
            }
        }

        val targetCity: String
        val targetTemp: Int
        val targetFeels: Int
        val targetUv: Int
        val targetPrecip: Int
        val targetWind: Double

        if (isTravelIntent && promptCity == null && nextPlan != null) {
            // Seyahat planı bağlamını kullan
            val snapshot = nextPlan.lastForecastSnapshot
            targetCity = nextPlan.city
            targetTemp = snapshot?.maxTemp?.toInt() ?: 20
            targetFeels = snapshot?.feelsLike?.toInt() ?: targetTemp
            targetUv = snapshot?.uvIndex?.toInt() ?: 0
            targetPrecip = snapshot?.precipitationProbability ?: 0
            targetWind = snapshot?.windSpeed ?: 0.0
        } else if (promptCity != null && AiIntentParser.normalizeTurkish(promptCity) != AiIntentParser.normalizeTurkish(weatherData.cityName)) {
            // Başka bir şehir sorulmuş (Seyahat planı olsun olmasın)
            targetCity = promptCity
            // Detaylı veri yoksa varsayılan veya weatherData'dan benzer veri (basitleştirme için)
            targetTemp = 20; targetFeels = 20; targetUv = 4; targetPrecip = 0; targetWind = 10.0
        } else {
            // Mevcut konum bağlamını kullan
            targetCity = weatherData.cityName
            targetTemp = weatherData.temperature.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 20
            targetFeels = weatherData.feelsLike.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: targetTemp
            targetUv = weatherData.uvIndex ?: 0
            targetPrecip = weatherData.precipitationProbability ?: 0
            targetWind = weatherData.windSpeed ?: 0.0
        }

        return when (intent) {
            AiIntent.CLOTHING -> generateClothingReply(targetTemp, targetFeels, targetUv, targetPrecip, targetWind, tone, interests)
            AiIntent.ACTIVITY -> generateActivityReply(targetCity, targetTemp, targetUv, targetPrecip, targetWind, tone, interests)
            AiIntent.TRAVEL -> generateTravelReply(userPrompt, weatherData, travelPlans, tone)
            AiIntent.PACKING -> generatePackingReply(targetTemp, targetPrecip, targetWind, tone)
            AiIntent.CALENDAR -> generateCalendarReply(weatherData, travelPlans, tone)
            AiIntent.WEEKEND_FORECAST -> generateWeekendReply(weatherData, tone)
            AiIntent.TRIP_RISK -> generateTripRiskReply(weatherData, tone) // Bu fonksiyona da context eklenmeli
            AiIntent.GENERAL_WEATHER -> generateGeneralWeatherReply(weatherData, tone)
            else -> generateGeneralWeatherReply(weatherData, tone)
        }
    }

    private fun generateClothingReply(temp: Int, feels: Int, uv: Int, precip: Int, wind: Double, tone: AssistantTone, interests: Set<String>): String {
        val upperItems = mutableListOf<String>()
        val lowerItems = mutableListOf<String>()
        val accessoryItems = mutableListOf<String>()

        when {
            temp > 28 -> {
                upperItems.add("ince pamuklu tişört")
                lowerItems.add("şort veya keten pantolon")
                accessoryItems.add("güneş gözlüğü")
                if (uv > 5) accessoryItems.add("şapka")
            }
            temp > 20 -> {
                upperItems.add("polo yaka tişört")
                lowerItems.add("hafif kot veya chino pantolon")
                if (wind > 20) accessoryItems.add("ince hırka")
            }
            temp > 12 -> {
                upperItems.add("sweatshirt veya uzun kollu gömlek")
                upperItems.add("hafif ceket")
                lowerItems.add("kot pantolon")
            }
            else -> {
                upperItems.add("termal içlik")
                upperItems.add("kalın kazak")
                upperItems.add("kışlık mont")
                lowerItems.add("kalın pantolon")
                accessoryItems.add("atkı ve bere")
            }
        }

        if (precip > 30) accessoryItems.add("şemsiye")

        return when (tone) {
            AssistantTone.SAMIMI -> "Sıcaklık $temp derece canım, hissedilen ise $feels. 😊 Bence şöyle rahat bir şeyler seçebilirsin: Üstte ${upperItems.joinToString(", ")}, altta ${lowerItems.joinToString(", ")}. ${if(accessoryItems.isNotEmpty()) "Aksesuar olarak da ${accessoryItems.joinToString(", ")} harika olur!" else ""}"
            AssistantTone.RESMI -> "Güncel sıcaklık $temp°C (hissedilen $feels°C) olarak bildirilmiştir. Konforunuz açısından şu giyim kombinasyonu önerilir: Üst bölümde ${upperItems.joinToString(", ")}, alt bölümde ${lowerItems.joinToString(", ")}. ${if(accessoryItems.isNotEmpty()) "Ek olarak ${accessoryItems.joinToString(", ")} kullanımı uygun olacaktır." else ""}"
            AssistantTone.KISA_NET -> "$temp° (His: $feels°). Öneri: ${upperItems.firstOrNull() ?: ""}, ${lowerItems.firstOrNull() ?: ""}${if(accessoryItems.isNotEmpty()) ", " + accessoryItems.firstOrNull() else ""}."
            AssistantTone.DETAYLI_UZMAN -> "Meteorolojik verilere göre ortam sıcaklığı $temp°C, termal algı ise $feels°C düzeyindedir. Vücut ısısı regülasyonu için ${upperItems.joinToString(", ")} ve ${lowerItems.joinToString(", ")} entegrasyonu tavsiye edilir. $precip% presipitasyon riski nedeniyle ${accessoryItems.joinToString(", ")} fonksiyonel bir tercih olacaktır."
            else -> "Hava $temp° (hissedilen $feels°). Şunları giyebilirsin: ${upperItems.joinToString(", ")}, ${lowerItems.joinToString(", ")} ve ${accessoryItems.joinToString(", ")}."
        }
    }

    private fun generateActivityReply(city: String, temp: Int, uv: Int, precip: Int, wind: Double, tone: AssistantTone, interests: Set<String>): String {
        val activities = mutableListOf<String>()

        if (precip < 20 && temp in 18..28) activities.add("piknik")
        if (wind < 25 && precip < 10) activities.add("bisiklet sürüşü")
        if (temp in 15..25 && wind < 30) activities.add("açık hava koşusu")
        if (temp > 25 && uv < 8) activities.add("yüzme / plaj")
        if (activities.isEmpty()) activities.add("kapalı alan aktiviteleri")

        return when (tone) {
            AssistantTone.SAMIMI -> "Bugün $city harika canım! 😊 Şöyle bir plan yapmaya ne dersin: ${activities.joinToString(", ")}. Bence çok keyifli olur!"
            AssistantTone.RESMI -> "$city bölgesi için meteorolojik koşullar dahilinde şu faaliyetler icra edilebilir: ${activities.joinToString(", ")}. Bilgilerinize sunulur."
            AssistantTone.KISA_NET -> "$city Aktiviteler: ${activities.joinToString(", ")}."
            AssistantTone.DETAYLI_UZMAN -> "Atmosferik stabilite ve termal konfor indeksine göre $city lokasyonunda ${activities.joinToString(", ")} faaliyetleri için optimal koşullar oluşmuştur. Rüzgarın $wind km/sa olması ${activities.firstOrNull() ?: ""} için avantajlıdır."
            else -> "Bugün $city'de şunları yapabilirsin: ${activities.joinToString(", ")}."
        }
    }

    private fun generatePackingReply(temp: Int, precip: Int, wind: Double, tone: AssistantTone): String {
        val items = mutableListOf<String>()
        if (temp > 25) items.add("güneş kremi, şort, tişört")
        else if (temp > 15) items.add("ceket, kot pantolon, spor ayakkabı")
        else items.add("mont, kazak, kalın çorap")

        if (precip > 20) items.add("şemsiye veya yağmurluk")
        items.add("powerbank, kişisel bakım ürünleri")

        return when (tone) {
            AssistantTone.SAMIMI -> "Valizini hazırlarken bence şunları unutma canım: ${items.joinToString(", ")}. Şimdiden iyi yolculuklar! ✈️"
            AssistantTone.RESMI -> "Seyahat planlamanız doğrultusunda şu ekipmanların valizinizde bulunması tavsiye edilir: ${items.joinToString(", ")}."
            AssistantTone.KISA_NET -> "Valiz listesi: ${items.joinToString(", ")}."
            AssistantTone.DETAYLI_UZMAN -> "Destinasyon verileri ışığında valiz optimizasyonu için; ${items.joinToString(", ")} materyallerinin envanterde bulundurulması elzemdir."
            else -> "Valizine şunları eklemeni öneririm: ${items.joinToString(", ")}."
        }
    }

    private fun generateCalendarReply(weatherData: WeatherData, travelPlans: List<TravelPlan>, tone: AssistantTone): String {
        if (travelPlans.isEmpty()) return "Takviminizde henüz planlı bir etkinlik bulunmuyor."
        val result = travelPlans.take(3).joinToString(", ") { "${it.startDate}: ${it.city}" }
        return when (tone) {
            AssistantTone.SAMIMI -> "Takvimine baktım canım, yakında şunlar var: $result. Heyecanlı mısın? 😊"
            AssistantTone.RESMI -> "Kayıtlı seyahat planlarınız şu şekildedir: $result. Bilgilerinize sunulur."
            AssistantTone.KISA_NET -> "Planlar: $result."
            AssistantTone.DETAYLI_UZMAN -> "Mevcut seyahat projeksiyonu şu takvimsel verileri içermektedir: $result. Lojistik hazırlıkların bu tarihlere göre revize edilmesi önerilir."
            else -> "Yaklaşan seyahat planlarınız:\n$result"
        }
    }

    private fun generateGeneralWeatherReply(weatherData: WeatherData, tone: AssistantTone): String {
        val city = weatherData.cityName
        val temp = weatherData.temperature
        val cond = weatherData.condition.lowercase(Locale("tr"))
        val feels = weatherData.feelsLike

        return when (tone) {
            AssistantTone.SAMIMI -> "Selam canım! 😊 $city'de hava şu an $temp, ama nemden dolayı $feels gibi hissediliyor. Gökyüzü de $cond, tam gezmelik!"
            AssistantTone.RESMI -> "Sayın kullanıcımız, $city lokasyonunda anlık sıcaklık $temp, hissedilen sıcaklık $feels düzeyindedir. Gökyüzü $cond olarak gözlemlenmektedir."
            AssistantTone.KISA_NET -> "$city: $temp ($cond). Hissedilen: $feels."
            AssistantTone.DETAYLI_UZMAN -> "$city meteoroloji istasyonundan alınan son verilere göre; sıcaklık $temp, termal algı $feels düzeyindedir. Atmosferik durum $cond olarak raporlanmıştır. Rüzgar ve nem dengesi stabil seyretmektedir."
            else -> "$city'de hava $temp ve $cond. Hissedilen sıcaklık ise $feels."
        }
    }

    private fun generateTravelReply(prompt: String, weatherData: WeatherData, travelPlans: List<TravelPlan>, tone: AssistantTone): String {
        val detectedCity = AiIntentParser.detectCity(prompt)
        val today = LocalDate.now()

        // Takvimdeki en yakın ve aktif seyahati bul
        val nextPlan = travelPlans
            .filter { !it.isArchived && !it.endDate.isBefore(today) }
            .minByOrNull { it.startDate }

        val city = detectedCity ?: nextPlan?.city

        if (city == null) {
            return when (tone) {
                AssistantTone.SAMIMI -> "Henüz planlanmış bir seyahatin bulunmuyor canım. İstersen hemen yeni bir seyahat planlayabiliriz! 😊"
                AssistantTone.RESMI -> "Kayıtlı aktif bir seyahat planınız bulunmamaktadır. Yeni bir seyahat planı oluşturulması önerilir."
                AssistantTone.KISA_NET -> "Planlı seyahat yok. Yeni plan oluşturabiliriz."
                else -> "Henüz planlanmış bir seyahatin bulunmuyor. İstersen yeni bir seyahat planlayabiliriz."
            }
        }

        // Eğer kullanıcı mevcut konumunu sormuşsa ama takvimde oraya bir seyahat YOKSA
        // ve bu bir TRAVEL intent ise (promptta seyahat kelimesi geçiyor vs)
        // Mevcut konumu "seyahat" olarak değerlendirmemek için kontrol:
        if (detectedCity == null && nextPlan == null) {
             // Bu durumda yukarıdaki city == null zaten yakalar.
        }

        return when (tone) {
            AssistantTone.SAMIMI -> "$city seyahatin için hava süper görünüyor canım! 😊 Valiz hazırlığı için bana her zaman sorabilirsin."
            AssistantTone.RESMI -> "$city istikametine yapılacak seyahatler için atmosferik koşullar elverişlidir. Seyahat öncesi valiz hazırlığı hususunda tarafımızdan teknik destek alabilirsiniz."
            AssistantTone.KISA_NET -> "$city seyahati için hava uygun. Hazırlıklara başlayabilirsin."
            AssistantTone.DETAYLI_UZMAN -> "$city destinasyonu için yapılan troposferik analizler, seyahat güvenliği ve konforu açısından pozitif sonuçlar vermektedir. Lojistik süreçleri başlatmanız tavsiye edilir."
            else -> "$city seyahatin için hava koşulları elverişli görünüyor. Yolculuk öncesi valiz hazırlığına başlamadan tekrar sorabilirsin."
        }
    }

    private fun generateTripRiskReply(weatherData: WeatherData, tone: AssistantTone): String {
        val precip = weatherData.precipitationProbability ?: 0
        return when (tone) {
            AssistantTone.SAMIMI -> if (precip > 50) "⚠️ Dikkat canım, yolda biraz yağmur olabilir, yavaş git emi?" else "✅ Yol tertemiz, gönül rahatlığıyla çıkabilirsin!"
            AssistantTone.RESMI -> if (precip > 50) "⚠️ Dikkat: Güzergah üzerinde yağış riski bulunmaktadır. Güvenli sürüş kurallarına riayet ediniz." else "✅ Seyahat güzergahı üzerinde herhangi bir risk tespit edilmemiştir."
            AssistantTone.KISA_NET -> if (precip > 50) "Yağış riski var. Dikkatli sür." else "Yol açık. Risk yok."
            AssistantTone.DETAYLI_UZMAN -> if (precip > 50) "Analiz raporu: Rota üzerinde %$precip olasılıkla presipitasyon öngörülmektedir. Fren mesafesi ve görüş mesafesi parametrelerini optimize ediniz." else "Sismik ve atmosferik veriler yol güvenliği için ideal seviyededir. Operasyonel bir engel bulunmamaktadır."
            else -> if (precip > 50) "⚠️ Dikkat: Yağış riski var. Sürüş güvenliğine dikkat edin." else "✅ Seyahat rotası üzerinde şu an için bir risk görünmüyor."
        }
    }

    private fun generateWeekendReply(weatherData: WeatherData, tone: AssistantTone): String {
        val sat = weatherData.dailyForecast.find { it.day.contains("Cumartesi", true) }
        val sun = weatherData.dailyForecast.find { it.day.contains("Pazar", true) }
        if (sat == null || sun == null) return "Hafta sonu verileri henüz netleşmedi."

        return when (tone) {
            AssistantTone.SAMIMI -> "Hafta sonu planın varsa süper! 😊 Cmt ${sat.maxTemp}°, Pazar ${sun.maxTemp}° olacak. Bence şimdiden planını yap!"
            AssistantTone.RESMI -> "Hafta sonu meteorolojik projeksiyonu: Cumartesi günü en yüksek sıcaklık ${sat.maxTemp}°C, Pazar günü ise ${sun.maxTemp}°C olarak öngörülmektedir."
            AssistantTone.KISA_NET -> "Hafta sonu: Cmt ${sat.maxTemp}°, Paz ${sun.maxTemp}°."
            AssistantTone.DETAYLI_UZMAN -> "Hafta sonu periyodu için 48 saatlik tahmin simülasyonu: Cumartesi günü termal zirve ${sat.maxTemp}°C, Pazar günü ise ${sun.maxTemp}°C düzeyindedir. Aktivite planlaması bu dalgalanmaya göre stabilize edilmelidir."
            else -> "Hafta sonu tahmini:\nCumartesi: ${sat.maxTemp}°\nPazar: ${sun.maxTemp}°"
        }
    }

    /**
     * Gelişmiş Seyahat Önerisi - Karşılaştırma ve Detaylı Analiz
     */
    fun generateTravelRecommendation(
        plan: TravelPlan,
        currentSnapshot: ForecastSnapshot?,
        previousSnapshot: ForecastSnapshot? = null,
        tone: AssistantTone = AssistantTone.DENGELI
    ): String {
        if (currentSnapshot == null) {
            return when (tone) {
                AssistantTone.SAMIMI -> "${plan.city} seyahatin için heyecan dorukta! ✈️ Hazırlıklara şimdiden başla canım."
                AssistantTone.RESMI -> "${plan.city} seyahati planlamanız onaylanmıştır. Hazırlık sürecine başlanması önerilir."
                AssistantTone.KISA_NET -> "${plan.city} hazırlıkları başlasın."
                AssistantTone.DETAYLI_UZMAN -> "${plan.city} destinasyonu için lojistik planlama evresine geçilmesi meteorolojik açıdan tavsiye edilir."
                else -> "${plan.city} seyahati için hazırlıklar başlasın! ✈️"
            }
        }

        val cond = currentSnapshot.conditionSummary?.lowercase(Locale("tr")) ?: "değişken"
        val maxT = currentSnapshot.maxTemp?.toInt() ?: 20
        val rain = currentSnapshot.precipitationProbability ?: 0

        return when (tone) {
            AssistantTone.SAMIMI -> {
                var msg = "${plan.city} seyahatin yaklaşıyor canım! Seni $maxT° sıcaklıkta $cond bir hava bekliyor. 😊"
                if (rain > 50) msg += "\n☔ Bence yanına bir şemsiye almalısın, ıslanmanı istemem!"
                else if (maxT > 30) msg += "\n☀️ Güneş kremini sakın unutma, hava baya sıcak olacak."
                msg + "\n🎒 Valizine powerbank eklemeyi de unutma sakın!"
            }
            AssistantTone.RESMI -> {
                var msg = "${plan.city} seyahatiniz yaklaşmaktadır. Tahmin edilen hava durumu $maxT°C ve $cond olarak bildirilmiştir."
                if (rain > 50) msg += "\n☔ Yağış ihtimaline karşı teknik önlem (şemsiye/yağmurluk) alınması önerilir."
                msg + "\n🎒 Gerekli ekipmanların (şarj cihazı/powerbank) valizinizde bulunması önem arz etmektedir."
            }
            AssistantTone.KISA_NET -> {
                var msg = "${plan.city}: $maxT°, $cond."
                if (rain > 50) msg += " Yağış riski: Şemsiye al."
                msg + " Valiz: Powerbank ekle."
            }
            AssistantTone.DETAYLI_UZMAN -> {
                var msg = "Analiz Raporu: ${plan.city} destinasyonunda troposferik veriler $maxT°C ve $cond koşulları öngörmektedir."
                if (rain > 50) msg += "\n☔ Presipitasyon olasılığı %$rain düzeyinde olduğundan, dış mekan mobilite planlarının revize edilmesi önerilir."
                msg + "\n🎒 Enerji yönetimi için powerbank gibi yardımcı donanımların envantere eklenmesi teknik tavsiyemizdir."
            }
            else -> {
                var msg = "${plan.city} seyahatin yaklaşıyor! Seni $maxT° sıcaklıkta $cond bir hava bekliyor."
                if (rain > 50) msg += "\n☔ Yağış ihtimali yüksek. Yanına şemsiye almanı öneririz."
                msg + "\n🎒 Valizine powerbank eklemeyi unutma."
            }
        }
    }
}
