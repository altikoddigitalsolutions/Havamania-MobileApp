package com.havamania

import com.havamania.ui.theme.AssistantTone
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object RecommendationEngine {

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
        if (weatherData == null) return "Hava durumu verilerine şu an erişilemiyor."

        val intent = AiIntentParser.detectIntent(userPrompt)
        val today = LocalDate.now()
        val nextPlan = travelPlans
            .filter { !it.isArchived && !it.endDate.isAfter(today.plusDays(30)) && !it.endDate.isBefore(today) }
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
            AiIntent.CLOTHING -> generateClothingReply(targetCity, currentTemp, feelsLike, uv, wind, humidity, tone)
            AiIntent.ACTIVITY -> generateActivityReply(targetCity, currentTemp, uv, precip, wind, humidity, tone)
            AiIntent.WEEKEND_FORECAST -> generateWeekendReply(weatherData, tone)
            AiIntent.PACKING -> generatePackingReply(nextPlan, tone)
            AiIntent.GENERAL_WEATHER -> {
                if (userPrompt.contains("yağmur", ignoreCase = true) || userPrompt.contains("yağış", ignoreCase = true)) {
                    generateRainReply(targetCity, precip, weatherData, tone)
                } else {
                    generateGeneralWeatherReply(weatherData, tone)
                }
            }
            AiIntent.TRAVEL -> generateTravelReply(userPrompt, weatherData, travelPlans, tone)
            else -> generateGeneralWeatherReply(weatherData, tone)
        }
    }

    // 1) BUGÜN NE GİYMELİYİM?
    private fun generateClothingReply(city: String, temp: Int, feels: Int, uv: Int, wind: Double, hum: Int, tone: AssistantTone): String {
        val baseAdvice = when {
            temp > 28 || feels > 30 -> "Hava oldukça sıcak olduğu için terletmeyen, %100 pamuklu veya keten kumaşlar günü kurtaracaktır."
            temp > 20 -> "Hafif bir tişört ve altına ince bir pantolon ideal görünüyor; ne çok sıcak ne çok soğuk, tam kararında bir hava."
            temp > 12 -> "Sweatshirt veya uzun kollu ince gömleklerin üzerine hafif bir ceket alarak katmanlı bir kombin yapmanı öneririm."
            else -> "Hava oldukça sert; yünlü kazaklar, kalın pantolonlar ve mutlaka koruyucu bir mont giyerek vücut ısını korumalısın."
        }

        val uvAdvice = if (uv > 5) " UV seviyesi yüksek olduğu için dışarıda güneş gözlüğü ve şapka kullanmayı, açıkta kalan bölgelere güneş kremi sürmeyi ihmal etme." else ""
        val windAdvice = if (wind > 25) " Rüzgar hızı yüksek olduğu için hissettiğin soğukluk artabilir, rüzgar kesici bir üst tercih etmen konforunu artıracaktır." else ""
        val eveningAdvice = if (temp > 15) " Akşam saatlerinde hava biraz serinleyebilir, yanına ince bir hırka alman iyi olur." else " Gece sıcaklıklar iyice düşeceği için dışarıda kalacaksan hazırlıklı olmalısın."

        return when (tone) {
            AssistantTone.SAMIMI -> "$city'de bugün hava tam bir muamma canım! 😊 $baseAdvice$uvAdvice$windAdvice$eveningAdvice Güzel görünmeyi unutma!"
            AssistantTone.RESMI -> "$city lokasyonu için meteorolojik veriler ışığında şu giyim önerileri sunulmaktadır: $baseAdvice$uvAdvice$windAdvice$eveningAdvice Bilgilerinize sunulur."
            AssistantTone.DETAYLI_UZMAN -> "Meteorolojik Analiz ($city): Termal algı $feels°C ve bağıl nem %$hum seviyelerindedir. Bu parametreler altında $baseAdvice$uvAdvice$windAdvice$eveningAdvice Termal regülasyonunuzu bu doğrultuda optimize ediniz."
            AssistantTone.KISA_NET -> "$city: $temp° ($baseAdvice). UV: $uv. $uvAdvice"
            else -> "Bugün $city'de $baseAdvice$uvAdvice$windAdvice$eveningAdvice"
        }
    }

    // 2) HAFTA SONU HAVA NASIL?
    private fun generateWeekendReply(weather: WeatherData, tone: AssistantTone): String {
        val sat = weather.dailyForecast.find { it.day.contains("Cumartesi", true) }
        val sun = weather.dailyForecast.find { it.day.contains("Pazar", true) }

        if (sat == null || sun == null) return "Hafta sonu verileri henüz netleşmedi, ancak yakında burada olacak."

        val verdict = if (sat.maxTemp >= sun.maxTemp && sat.precipitationProbability < 20) "Cumartesi" else "Pazar"
        val advice = if (sat.precipitationProbability > 40 || sun.precipitationProbability > 40) "Kapalı alan etkinliklerine yönelmek daha güvenli olabilir." else "Piknik, sahil yürüyüşü veya açık hava kahvaltısı için harika bir fırsat."

        return when (tone) {
            AssistantTone.SAMIMI -> "Hafta sonu planların için harika haberlerim var canım! 😊 Cumartesi ${sat.maxTemp}°, Pazar ise ${sun.maxTemp}° görünüyor. Karşılaştırdığımda $verdict günü hava çok daha davetkar. Bence $advice"
            AssistantTone.RESMI -> "Hafta sonu projeksiyonu: Cumartesi günü maksimum ${sat.maxTemp}°C, Pazar günü ise ${sun.maxTemp}°C sıcaklık öngörülmektedir. Atmosferik koşullar değerlendirildiğinde $verdict gününün outdoor faaliyetler için daha elverişli olduğu saptanmıştır."
            AssistantTone.DETAYLI_UZMAN -> "48 Saatlik Hafta Sonu Analizi: Cumartesi günü troposferik stabilite hakimken (${sat.maxTemp}°C), Pazar günü sıcaklık ve rüzgar vektörlerinde değişim bekleniyor (${sun.maxTemp}°C). $verdict günü $advice Karşılaştırmalı analizimiz dış mekan planları için Cumartesi gününü işaret etmektedir."
            AssistantTone.KISA_NET -> "Cmt: ${sat.maxTemp}°, Paz: ${sun.maxTemp}°. $verdict günü daha uygun. $advice"
            else -> "Hafta sonu Cumartesi ${sat.maxTemp}°, Pazar ${sun.maxTemp}° civarında olacak. Genel değerlendirmeme göre $verdict günü dışarı çıkmak için çok daha uygun. $advice"
        }
    }

    // 3) DIŞARI ÇIKMAK İÇİN UYGUN MU?
    private fun generateActivityReply(city: String, temp: Int, uv: Int, precip: Int, wind: Double, hum: Int, tone: AssistantTone): String {
        val isGood = precip < 20 && wind < 30 && temp in 15..28
        val status = if (isGood) "oldukça uygun" else "biraz riskli"

        val details = mutableListOf<String>()
        if (precip > 30) details.add("yağış ihtimali (%$precip)")
        if (wind > 25) details.add("sert rüzgar ($wind km/sa)")
        if (uv > 6) details.add("yüksek UV radyasyonu")
        if (hum > 75) details.add("yüksek nem oranı")

        val reason = if (details.isNotEmpty()) "Ancak ${details.joinToString(", ")} nedeniyle dikkatli olmalısın." else "Şu an hiçbir engel görünmüyor."
        val timeHint = if (uv > 6) " Özellikle 11:00 ile 16:00 saatleri arasını kapalı alanlarda geçirmeni öneririm." else ""

        return when (tone) {
            AssistantTone.SAMIMI -> "Bugün $city'de dışarı çıkmak için hava $status canım! 😊 $reason$timeHint Şöyle güzel bir yürüyüş ruhuna iyi gelirdi."
            AssistantTone.RESMI -> "$city bölgesi için yapılan analizlerde dış mekan faaliyetlerinin icrası $status olarak değerlendirilmiştir. $reason$timeHint Tedbirli olmanız önerilir."
            AssistantTone.DETAYLI_UZMAN -> "Aktivite Uygunluk İndeksi ($city): Mevcut atmosferik parametreler (Yağış: %$precip, Rüzgar: $wind km/sa, UV: $uv) incelendiğinde durumun $status olduğu görülmektedir. $reason$timeHint Fizyolojik konfor açısından öğleden sonra saatleri risk içermektedir."
            AssistantTone.KISA_NET -> "Hava $status. $reason $timeHint"
            else -> "Bugün $city'de dışarı çıkmak $status. $reason$timeHint Planlarını bu verilere göre yapabilirsin."
        }
    }

    // 4) BUGÜN YAĞMUR YAĞACAK MI?
    private fun generateRainReply(city: String, precip: Int, weather: WeatherData, tone: AssistantTone): String {
        val hourlyPrecip = weather.hourlyForecast.take(12).filter { it.precipitationProbability > 30 }
        val hours = hourlyPrecip.joinToString(", ") { it.time.split("T").last().take(5) }

        val rainText = when {
            precip > 70 -> "Bugün yağmur kaçınılmaz görünüyor, hazırlıklı olmalısın."
            precip > 30 -> "Günün belli bölümlerinde yağış geçişleri olabilir, gökyüzü her an sürpriz yapabilir."
            else -> "Bugün yağmur beklemiyoruz, gökyüzü oldukça dost canlısı görünüyor."
        }

        val umbrellaAdvice = if (precip > 30) " Yanına mutlaka sağlam bir şemsiye ve su geçirmeyen bir ayakkabı almalısın." else " Şemsiye taşımana gerek yok, açık hava planlarını gönül rahatlığıyla yapabilirsin."
        val timeDetail = if (hourlyPrecip.isNotEmpty()) " Özellikle şu saatlere dikkat: $hours." else ""

        return when (tone) {
            AssistantTone.SAMIMI -> "Bugün $city'de yağmur ihtimali %$precip canım. $rainText$timeDetail$umbrellaAdvice Islanmanı hiç istemem! ☔"
            AssistantTone.RESMI -> "Günlük yağış projeksiyonu: %$precip presipitasyon olasılığı saptanmıştır. $rainText$timeDetail$umbrellaAdvice Bilgilerinize sunulur."
            AssistantTone.DETAYLI_UZMAN -> "Hidro-Meteorolojik Analiz: Bağıl nem ve bulut kapalılık oranı %$precip olasılıkla yağış formasyonuna işaret etmektedir. $rainText$timeDetail$umbrellaAdvice Veriler gün boyu stabil bir seyir izlemektedir."
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
            AssistantTone.SAMIMI -> "$city seyahatin için valizini ben hazırladım bile canım! ✨ Hava gündüz $temp°, gece ise $minTemp° civarında olacak. Şu listeyi unutma: $listStr. Şimdiden iyi yolculuklar! ✈️"
            AssistantTone.RESMI -> "$city seyahatiniz için hazırlanan ekipman listesi: Gündüz sıcaklığı $temp°C, gece $minTemp°C olarak öngörüldüğünden valizinizde $listStr bulunması tavsiye edilir."
            AssistantTone.DETAYLI_UZMAN -> "Destinasyon Lojistik Analizi ($city): Termal spektrum $minTemp°C ile $temp°C arasındadır. Presipitasyon riski %$precip seviyesindedir. Optimizasyon için önerilen materyaller: $listStr. Hazırlıklarınızı bu verilere göre tamamlayınız."
            AssistantTone.KISA_NET -> "$city Valizi ($temp°/$minTemp°): $listStr."
            else -> "$city seyahatin için valizine şunları almanı öneririm: $listStr. Gündüz hava $temp° iken akşam $minTemp° dereceye kadar düşebilir, hazırlıklı olmalısın."
        }
    }

    private fun generateGeneralWeatherReply(weather: WeatherData, tone: AssistantTone): String {
        val city = weather.cityName
        val temp = weather.temperature
        val cond = weather.condition.lowercase(Locale("tr"))
        val feels = weather.feelsLike

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
            AssistantTone.DETAYLI_UZMAN -> "$city destinasyonu için yapılan troposferik analizler, seyahat güvenliği ve konforu açısından pozitif sonuçlar vermektedir. Lojistik süreçleri başlatmanız tavsiye edilir."
            else -> "$city seyahatin için hava koşulları elverişli görünüyor. Yolculuk öncesi valiz hazırlığına başlamadan tekrar sorabilirsin."
        }
    }

    /**
     * Gelişmiş Seyahat Önerisi - Kart Görünümü İçin
     */
    fun generateTravelRecommendation(
        plan: TravelPlan,
        currentSnapshot: ForecastSnapshot?,
        previousSnapshot: ForecastSnapshot? = null,
        tone: AssistantTone = AssistantTone.DENGELI
    ): String {
        if (currentSnapshot == null) return "${plan.city} seyahati için hazırlıklar başlasın! ✈️"

        val cond = currentSnapshot.conditionSummary?.lowercase(Locale("tr")) ?: "değişken"
        val maxT = currentSnapshot.maxTemp?.toInt() ?: 20
        val rain = currentSnapshot.precipitationProbability ?: 0

        return when (tone) {
            AssistantTone.SAMIMI -> "${plan.city} seyahatin yaklaşıyor canım! Seni $maxT° sıcaklıkta $cond bir hava bekliyor. 😊"
            AssistantTone.RESMI -> "${plan.city} seyahatiniz yaklaşmaktadır. Tahmin edilen hava durumu $maxT°C ve $cond olarak bildirilmiştir."
            else -> "${plan.city} seyahatin yaklaşıyor! Seni $maxT° sıcaklıkta $cond bir hava bekliyor."
        }
    }
}
