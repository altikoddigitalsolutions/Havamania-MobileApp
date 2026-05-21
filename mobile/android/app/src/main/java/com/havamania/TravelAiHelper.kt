package com.havamania

import java.time.LocalDate

object TravelAiHelper {

    fun generateTravelAiSuggestion(
        city: String,
        tripType: TripType,
        forecastSnapshot: ForecastSnapshot?,
        previousSnapshot: ForecastSnapshot?,
        daysUntilTrip: Int,
        isPastTrip: Boolean = false,
        endDate: LocalDate? = null
    ): String {
        val sb = StringBuilder()

        if (isPastTrip) {
            val dateStr = endDate?.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM")) ?: "geçtiğimiz günlerde"
            return "$city seyahatin $dateStr'da tamamlandı. Bu seyahat geçmiş rotaların arasında saklanıyor. Dilersen notlarını güncelleyebilir, seyahati silebilir veya arşivde tutabilirsin. Geçmiş hava verisi bu seyahat için artık güncel öneri üretmek amacıyla kullanılmıyor."
        }

        if (daysUntilTrip > 15 || forecastSnapshot == null) {
            sb.append("Bu seyahat için güvenilir hava tahmini henüz erken. Seyahate 15 gün kala hava analizini başlatacağım. ")
            sb.append(getTripTypeAdvice(tripType, forecastSnapshot, daysUntilTrip))
            sb.append("\n\n").append(getCitySuggestion(city, tripType))
            return sb.toString().trim()
        }

        // 1. Comparison with previous snapshot
        if (previousSnapshot != null) {
            val comparison = generateComparisonText(previousSnapshot, forecastSnapshot)
            if (comparison.isNotBlank()) {
                sb.append(comparison).append(" ")
            }
        }

        // 2. Weather based advice
        val weatherAdvice = getWeatherAdvice(forecastSnapshot)
        sb.append(weatherAdvice).append(" ")

        // 3. Trip type based advice
        val tripAdvice = getTripTypeAdvice(tripType, forecastSnapshot, daysUntilTrip)
        sb.append(tripAdvice).append(" ")

        // 4. City specific sightseeing
        sb.append("\n\n").append(getCitySuggestion(city, tripType))

        return sb.toString().trim()
    }

    fun generateComparisonText(old: ForecastSnapshot, new: ForecastSnapshot): String {
        val changes = mutableListOf<String>()

        val precipDiff = (new.precipitationProbability ?: 0) - (old.precipitationProbability ?: 0)
        if (precipDiff >= 20) {
            changes.add("Yağmur ihtimali önceki analize göre %$precipDiff arttı (Toplam %${new.precipitationProbability}). Planlarını kapalı alanlara göre revize etmeni öneririm.")
        } else if (precipDiff <= -20) {
            changes.add("Yağmur ihtimali %${old.precipitationProbability}'den %${new.precipitationProbability}'ye düştü, hava düzeliyor.")
        }

        val tempDiff = (new.maxTemp ?: 0.0) - (old.maxTemp ?: 0.0)
        if (tempDiff >= 5) {
            changes.add("Hava beklediğimden daha sıcak olacak (${new.maxTemp?.toInt()}°).")
        } else if (tempDiff <= -5) {
            changes.add("Hava beklediğimden daha serin olacak (${new.maxTemp?.toInt()}°).")
        }

        if (changes.isEmpty()) {
            return "Hava tahmini önceki analizle benzer görünüyor."
        }

        return changes.joinToString(" ")
    }

    private fun getWeatherAdvice(snapshot: ForecastSnapshot): String {
        val precip = snapshot.precipitationProbability ?: 0
        val maxTemp = snapshot.maxTemp ?: 20.0
        val wind = snapshot.windSpeed ?: 0.0
        val uv = snapshot.uvIndex ?: 0.0

        return when {
            precip > 70 && maxTemp < 15 -> "Yağmur ve serin hava birlikte görünüyor. Kaygan zeminlere dikkat et, kapalı alan alternatifi planlamak iyi olabilir."
            precip > 50 -> "Yağmur ihtimali yüksek görünüyor. Şemsiyeni ve su geçirmez bir ayakkabıyı yanına almanı öneririm."
            maxTemp < 10 -> "Hava oldukça soğuk olabilir. Kalın kıyafetler ve bere/atkı almanı öneririm."
            maxTemp < 18 -> "Hava serin olabilir. Akşam saatleri için ince mont veya sweatshirt iyi olur."
            maxTemp > 30 -> "Sıcaklık yüksek görünüyor. Hafif kıyafet, bol su ve güneş koruması iyi olur."
            uv > 6 -> "Güneş güçlü olabilir. Güneş kremi, şapka ve bol su iyi fikir."
            wind > 35 -> "Rüzgar belirgin olabilir. Açık alan planlarında dikkatli olmanı öneririm."
            else -> "Hava durumu seyahat için oldukça elverişli görünüyor."
        }
    }

    private fun getTripTypeAdvice(type: TripType, snapshot: ForecastSnapshot?, daysUntil: Int): String {
        val hasForecast = snapshot != null
        val precip = snapshot?.precipitationProbability ?: 0
        val maxTemp = snapshot?.maxTemp ?: 20.0
        val minTemp = snapshot?.minTemp ?: 10.0
        val uv = snapshot?.uvIndex ?: 0.0
        val wind = snapshot?.windSpeed ?: 0.0

        return when (type) {
            TripType.CAMPING -> {
                if (!hasForecast) "Kamp için ekipmanlarını (uyku tulumu, çadır zemini) şimdiden kontrol etmelisin."
                else if (precip > 40) "Yağış ihtimali yüksekse çadır zemini ve su geçirmez ekipmanı mutlaka kontrol et."
                else if (minTemp < 10) "Gece sıcaklığı düşük olacağından kalın bir uyku tulumu ve termal katmanlar almanı öneririm."
                else "Kamp için hava oldukça uygun görünüyor, rüzgar durumuna göre çadır yerini seçebilirsin."
            }
            TripType.CULTURE -> {
                if (!hasForecast) "Kültür gezisi için müze kartını kontrol etmeyi ve yürüyüş ayakkabılarını hazırlamayı unutma."
                else if (precip > 30) "Yağmur ihtimali varsa kapalı müze ve tarihi çarşı rotalarını öne alabilirsin."
                else "Açık hava müzelerini ve antik kentleri gezmek için harika bir hava."
            }
            TripType.GASTRONOMY -> {
                if (!hasForecast) "Popüler yerel restoranlar için şimdiden rezervasyon yapmanı öneririm."
                else "Hava nasıl olursa olsun, akşam saatlerinde yerel lezzet duraklarını planına ekleyebilirsin."
            }
            TripType.BEACH -> {
                if (!hasForecast) "Güneş kremi ve deniz malzemelerini valizine eklemeyi unutma."
                else if (uv > 6) "UV endeksi yüksek görünüyor, şapka ve güneş kremi kullanımına dikkat etmelisin."
                else if (wind > 30) "Rüzgar deniz keyfini biraz etkileyebilir, korunaklı koyları tercih edebilirsin."
                else "Deniz ve güneşin tadını çıkarmak için ideal bir hava."
            }
            TripType.WINTER -> {
                if (!hasForecast) "Kayak takımlarını ve kışlık kıyafetlerini hazırlamaya başlayabilirsin."
                else if (maxTemp < 0) "Dondurucu soğuklara ve buzlanmaya karşı dikkatli ol, kış ekipmanlarını tam al."
                else "Kar durumunu takip ederek kış sporlarının tadını çıkarabilirsin."
            }
            TripType.PHOTOGRAPHY -> {
                if (!hasForecast) "Tripod ve yedek pillerini hazırlamayı unutma."
                else if (precip < 20 && uv < 5) "Bulutlu hava, yumuşak ışık ve dramatik fotoğraflar için harika bir fırsat sunabilir."
                else "Gün doğumu ve gün batımı saatlerini takip ederek en iyi kareleri yakalayabilirsin."
            }
            TripType.ROAD_TRIP -> {
                if (!hasForecast) "Aracının bakımını yaptırmayı ve rotanı önceden belirlemeyi unutma."
                else if (precip > 50) "Yağmur ihtimali artarsa uzun yolda takip mesafesini artır ve mola planı yap."
                else "Yolculuk için görüş mesafesi ve hava şartları oldukça uygun."
            }
            TripType.FAMILY -> {
                if (!hasForecast) "Çocuklar için esnek bir plan ve yanına alacağın atıştırmalıklar hayat kurtarabilir."
                else "Çocuklar için yedek kıyafet ve hem açık hem de kapalı alan alternatifleri planlamak iyi olur."
            }
            TripType.BUSINESS -> "İş seyahatinde ulaşım gecikmelerine karşı planına biraz esneklik payı bırakmanı öneririm."
            TripType.VACATION -> "Rahat ayakkabı ve hava durumuna uygun kıyafetler seyahat konforunu artıracaktır."
            TripType.NATURE -> "Doğa yürüyüşü için uygun ayakkabı ve katmanlı giyinme seyahatini kolaylaştıracaktır."
            TripType.ROMANTIC -> "Akşam yemeği ve gün batımı izleme noktaları için şimdiden plan yapabilirsin."
            TripType.ADVENTURE -> "Macera dolu bir gezi için güvenlik ekipmanlarını ve sigortanı kontrol etmeyi unutma."
            TripType.SHOPPING -> "Yerel pazarlar ve alışveriş merkezleri için valizinde boş yer bıraktığından emin ol."
            TripType.WEEKEND -> "Kısa süreli bu kaçamakta zamanı verimli kullanmak için rotanı önceden netleştir."
            TripType.HEALTH -> "Spa ve sağlık aktiviteleri için randevularını önceden almanı öneririm."
            TripType.EVENT -> "Etkinlik biletlerini ve giriş saatlerini tekrar kontrol etmeyi unutma."
            else -> "Seyahat tarihine yaklaştıkça valizini son bir kez gözden geçirmeyi unutma."
        }
    }

    fun getCitySuggestion(city: String, tripType: TripType): String {
        val c = city.lowercase()
        return when {
            c.contains("istanbul") -> "İstanbul'da Sultanahmet, Galata, Boğaz ve Kadıköy rotası her zaman klasiktir."
            c.contains("izmir") -> "İzmir'de Kordon, Alsancak, Efes ve Şirince'yi mutlaka görmelisin."
            c.contains("antalya") -> "Antalya'da Kaleiçi, Düden Şelalesi, Konyaaltı ve Side antik kenti harika noktalardır."
            c.contains("şanlıurfa") || c.contains("urfa") -> "Şanlıurfa'da Balıklıgöl, Göbeklitepe ve Halfeti tekne turunu planına eklemelisin."
            c.contains("batman") -> "Batman'da Hasankeyf ve Malabadi Köprüsü çevresini gezebilirsin."
            c.contains("çankırı") -> "Çankırı'da Ilgaz Dağı Milli Parkı'nı ve meşhur Çankırı Tuz Mağarası'nı mutlaka ziyaret et."
            c.contains("ankara") -> "Ankara'da Anıtkabir, Hamamönü ve Eymir Gölü huzurlu bir rota sunar."
            c.contains("trabzon") -> "Trabzon'da Uzungöl, Sümela Manastırı ve Boztepe'de çay keyfi yapmadan dönme."
            c.contains("muğla") -> "Muğla'da Bodrum, Fethiye, Marmaris ve Akyaka'nın eşsiz koylarını keşfetmelisin."
            c.contains("nevşehir") || c.contains("kapadokya") || c.contains("göreme") ->
                "Kapadokya'da Göreme, Uçhisar, Derinkuyu yeraltı şehri ve balon izleme noktalarını kaçırma."
            else -> "Şehir merkezindeki tarihi ve doğal noktaları seyahat tipine göre planına ekleyebilirsin."
        }
    }

    fun generateHistorySummary(plan: TravelPlan): TravelHistorySummary {
        val duration = java.time.temporal.ChronoUnit.DAYS.between(plan.startDate, plan.endDate).toInt() + 1
        val snapshot = plan.lastForecastSnapshot

        // Base values from snapshot or estimation
        val minT = snapshot?.minTemp?.toInt() ?: 12
        val maxT = snapshot?.maxTemp?.toInt() ?: 24
        val avgT = (minT + maxT) / 2

        // Estimation for distribution based on duration and weather condition summary
        val cond = snapshot?.conditionSummary?.lowercase() ?: "parçalı bulutlu"
        val prob = snapshot?.precipitationProbability ?: 20

        val rainy = if (cond.contains("yağmur") || prob > 50) (duration * 0.4).toInt().coerceAtLeast(1) else (duration * 0.1).toInt()
        val sunny = if (cond.contains("güneş") || cond.contains("açık")) (duration * 0.6).toInt().coerceAtLeast(1) else (duration * 0.3).toInt()
        val cloudy = (duration - rainy - sunny).coerceAtLeast(0)

        val comfort = when {
            rainy > duration / 2 -> 65
            maxT > 32 || minT < 5 -> 75
            else -> 88
        }

        val riskDay = if (rainy > 0) "Seyahatin orta dönemlerinde beklenen kuvvetli yağış en riskli gündü." else "Genel olarak stabil bir hava hakim olsa da rüzgar geçişleri dikkate değerdi."

        val summaryText = "${plan.city} seyahatin $duration gün sürdü. Bu rota ${getSeason(plan.startDate)} dönemine denk geldiği için genel olarak ${if(avgT > 20) "ılık ve keyifli" else "serin ve dengeli"}, ${if(rainy > 0) "ara ara yağış geçişleri içeren" else "açık havaya uygun"} bir hava profili gözlemlendi. ${plan.lastWeatherAnalysisText ?: ""}"

        val packingText = if (avgT > 22) {
            "İnce, pamuklu ve açık renkli kıyafetler konforun için en iyisiydi. Akşamları deniz esintisine karşı ince bir hırka yeterli olmuş olmalı."
        } else {
            "Katmanlı giyinme ve orta kalınlıkta bir ceket bu seyahatin kurtarıcısıydı. Mevsim geçişi nedeniyle kapalı ayakkabı tercihi doğru bir karardı."
        }

        val nextTripText = "Gelecek seyahatini yine bu tarihlerde planlayacaksan, ${if(rainy > 0) "yağış ihtimaline karşı daha esnek kapalı alan rotaları" else "açık hava etkinliklerini artıracak şekilde"} bir program oluşturmanı öneririm."

        return TravelHistorySummary(
            averageTemp = avgT,
            minTemp = minT,
            maxTemp = maxT,
            rainyDays = rainy,
            sunnyDays = sunny,
            cloudyDays = cloudy,
            riskDayText = riskDay,
            comfortScore = comfort,
            summaryText = summaryText,
            packingAdvice = packingText,
            nextTripAdvice = nextTripText,
            durationDays = duration
        )
    }

    private fun getSeason(date: java.time.LocalDate): String {
        return when (date.monthValue) {
            3, 4, 5 -> "ilkbahar"
            6, 7, 8 -> "yaz"
            9, 10, 11 -> "sonbahar"
            else -> "kış"
        }
    }
}
