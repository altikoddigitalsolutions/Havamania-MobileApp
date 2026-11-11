package com.havamania

import com.havamania.ui.theme.AssistantTone
import java.time.LocalDate
import java.util.Locale

object TravelAiHelper {

    fun generateTravelAiSuggestion(
        city: String,
        tripType: TripType,
        forecastSnapshot: ForecastSnapshot?,
        previousSnapshot: ForecastSnapshot?,
        daysUntilTrip: Int,
        isPastTrip: Boolean = false,
        endDate: LocalDate? = null,
        tone: AssistantTone = AssistantTone.DENGELI,
        personalization: PersonalizationProfile? = null,
        district: String? = null
    ): String {
        if (isPastTrip) {
            val dateStr = endDate?.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM")) ?: "geçtiğimiz günlerde"
            return when (tone) {
                AssistantTone.SAMIMI -> "HAVA ÖZETİ|Bu seyahatin $dateStr tarihinde bitti bile! Anıların arasında yerini aldı. [SEP] VALİZ TAVSİYESİ|Geçmiş seyahat için öneriye gerek yok canım. [SEP] MUTLAKA GÖR|Eski fotoğraflarına bakıp iç geçirebilirsin! [SEP] DENEMEDEN DÖNME|Bir sonraki rotan için iştahını sakla. [SEP] YEREL TAVSİYE|Yeni maceralarda görüşürüz!"
                AssistantTone.RESMI -> "HAVA ÖZETİ|Söz konusu seyahat $dateStr tarihinde tamamlanmıştır. Veriler arşivlenmiştir. [SEP] VALİZ TAVSİYESİ|Geçmiş dönem için tavsiye sunulmamaktadır. [SEP] MUTLAKA GÖR|Şehir hatıralarınızı inceleyebilirsiniz. [SEP] DENEMEDEN DÖNME|Gelecek rotalarınız için planlama yapabilirsiniz. [SEP] YEREL TAVSİYE|İyi yolculuklar dileriz."
                AssistantTone.KISA_NET -> "HAVA ÖZETİ|Tamamlandı ($dateStr). [SEP] VALİZ TAVSİYESİ|Gerekli değil. [SEP] MUTLAKA GÖR|Arşivleri incele. [SEP] DENEMEDEN DÖNME|Yeni plan yap. [SEP] YEREL TAVSİYE|Kayıtlı."
                else -> "HAVA ÖZETİ|Bu seyahat $dateStr tarihinde tamamlandı. Geçmiş rotaların arasında güvenle saklanıyor. [SEP] VALİZ TAVSİYESİ|Geçmiş seyahat verisi için valiz önerisi sunulmuyor. [SEP] MUTLAKA GÖR|Şehirdeki anılarını tazelemek için eski fotoğraflarına göz atabilirsin. [SEP] DENEMEDEN DÖNME|Bir sonraki rotan için yerel lezzet duraklarını şimdiden keşfetmeye başla. [SEP] YEREL TAVSİYE|Seyahat geçmişin, gelecek planların için harika bir rehber olacak."
            }
        }

        // Kişiselleştirme Katmanı (Personalization Layer)
        val interests = personalization?.selectedInterests ?: emptyList()
        val isPersonalizationActive = personalization?.personalizationEnabled ?: true

        val windowDays = TRIP_ANALYSIS_WINDOW_DAYS

        val guide = LocalTravelGuides.find(city, district)
        val experiences = LocalTravelGuides.experiences(city, district, tripType)
        val experienceAdvice = experiences.takeIf { it.isNotEmpty() }
            ?.joinToString("\n\n") { "${it.title}\n${it.description}" } ?: guide?.experience
        val destination = district?.takeIf { it.isNotBlank() }?.let { "$it, $city" } ?: city
        val missingGuide = "$destination için yer ve yemek adları doğrulanmış rehber henüz hazır değil."

        if (daysUntilTrip > windowDays) {
            return "HAVA ÖZETİ|Hava tahmini seyahate $windowDays gün kala hazırlanacak. [SEP] VALİZ TAVSİYESİ|Havaya göre valiz önerisi için tahmin bekleniyor. [SEP] MUTLAKA GÖR|${guide?.visit ?: missingGuide} [SEP] DENEMEDEN DÖNME|${guide?.food ?: missingGuide} [SEP] YEREL TAVSİYE|${experienceAdvice ?: missingGuide}"
        }

        var weatherBlock = ""
        if (forecastSnapshot != null) {
            val precip = forecastSnapshot.precipitationProbability ?: 0
            val maxTemp = forecastSnapshot.maxTemp?.toInt() ?: 20
            val windSpeed = forecastSnapshot.windSpeed ?: 0.0
            val code = forecastSnapshot.weatherCode ?: 0
            val cond = (forecastSnapshot.conditionSummary ?: "Açık").lowercase(Locale("tr"))

            val formattedPrecip = WeatherUtils.formatRainProbability(precip)
            val windText = WeatherUtils.formatWindWithLevel(windSpeed)

            weatherBlock = when (tone) {
                AssistantTone.SAMIMI -> {
                    when {
                        code >= 95 -> "Hava biraz huysuz, fırtına çıkabilir! Rüzgar $windText civarında, dışarı çıkarken iki kere düşün canım."
                        code >= 80 -> "Sağanak yağmur geliyor, ıslanmaya hazır ol! Şemsiyeni sakın unutma tatlım."
                        precip > 60 -> "Gökyüzü biraz ağlamaklı, $formattedPrecip yağmur ihtimali var. Hazırlıklı ol!"
                        maxTemp > 30 -> "Sıcaklık $maxTemp°C civarında olacak. Güncel hava koşullarını takip etmeni öneririm."
                        maxTemp < 10 -> "Hava biraz sert, buz gibi olacak. Sıkı giyinmeyi unutma!"
                        else -> "Tam gezmelik harika bir hava! $maxTemp° derece ve $cond gökyüzü seni bekliyor."
                    }
                }
                AssistantTone.RESMI -> {
                    when {
                        code >= 95 -> "İlgili tarihlerde fırtına riski ve $windText hızında rüzgar öngörülmektedir. Tedbirli olunması tavsiye edilir."
                        code >= 80 -> "Kuvvetli sağanak yağış beklenmektedir. Olumsuz hava şartlarına karşı hazırlıklı olunmalıdır."
                        precip > 60 -> "Hava durumunun yağışlı geçeceği (%$precip) tahmin edilmektedir. Şemsiye bulundurulması önerilir."
                        maxTemp > 30 -> "Sıcaklık değerlerinin $maxTemp°C seviyelerinde seyretmesi beklenmektedir."
                        maxTemp < 10 -> "Düşük sıcaklık değerleri ($maxTemp°) ve serin hava koşulları hakim olacaktır."
                        else -> "Meteorolojik koşullar seyahat için elverişli olup, sıcaklık $maxTemp° civarında seyrecektir."
                    }
                }
                AssistantTone.KISA_NET -> {
                    when {
                        code >= 95 -> "Fırtına riski, rüzgar $windText. Dikkatli ol."
                        code >= 80 -> "Sağanak yağışlı. Şemsiye al."
                        precip > 60 -> "Yağmurlu (%$precip). Önlem al."
                        maxTemp > 30 -> "Sıcak ($maxTemp°C)."
                        maxTemp < 10 -> "Soğuk ($maxTemp°). Kalın giyin."
                        else -> "Hava uygun ($maxTemp°, $cond)."
                    }
                }
                AssistantTone.DETAYLI_UZMAN -> {
                    when {
                        code >= 95 -> "Troposferik instabilite: Fırtına olasılığı mevcuttur. Rüzgar vektörü $windText seviyesindedir."
                        code >= 80 -> "Presipitasyon zirvesi: Kuvvetli sağanak geçişleri öngörülmektedir. Termal konfor düşük."
                        precip > 60 -> "Atmosferik nem doygunluğu: $formattedPrecip olasılıkla yağış beklenmektedir. Görüş mesafesi etkilenebilir."
                        maxTemp > 30 -> "Tahmin edilen en yüksek sıcaklık $maxTemp°C. Güncel hava koşulları takip edilmelidir."
                        maxTemp < 10 -> "Hipotermi riski: Sıcaklık $maxTemp°C civarındadır. İzolasyon sağlayan katmanlar önerilir."
                        else -> "Meteorolojik stabilite: Hava $maxTemp°C ve $cond durumdadır. Aktivite parametreleri için ideal."
                    }
                }
                else -> { // DENGELİ
                    when {
                        code >= 95 -> "Hava fırtınalı görünüyor, yağış riski yüksek. Rüzgar $windText hızına ulaşabilir. Yaklaşık $maxTemp° civarında olacak, tedbirli olmalısın."
                        code >= 80 -> "Hava sağanak yağışlı görünüyor, yağış riski yüksek. Yaklaşık $maxTemp° civarında olacak, şemsiyeni sakın unutma."
                        precip > 60 -> "Gideceğin tarihlerde gökyüzü biraz ağlamaklı görünüyor, $formattedPrecip yağmur ihtimali var. Hava yaklaşık $maxTemp° civarında olacak, şemsiyeni sakın unutma."
                        maxTemp > 30 -> "Sıcaklığın $maxTemp°C civarında olması bekleniyor. Diğer hava koşullarını güncel tahminden takip edebilirsin."
                        maxTemp < 10 -> "Hava biraz sert ve serin olacak, termometreler $maxTemp° civarında gezecek. Kalın bir şeyler almanı öneririm."
                        windSpeed > 30 -> "Hava oldukça rüzgarlı ($windText) olacak. $maxTemp° sıcaklıkta bile esinti üşütebilir, rüzgar kesici bir şeyler almanı öneririm."
                        else -> "Tam gezmelik, harika bir hava seni bekliyor! $maxTemp° derece ve $cond gökyüzü seyahatine ayrı bir keyif katacak. Yağmur ihtimali $formattedPrecip."
                    }
                }
            }

            // A rare hazardous day must not turn into an optimistic summary in any tone.
            tripWeatherWarning(forecastSnapshot)?.let { weatherBlock = it }

            forecastSnapshot.uvIndex?.takeIf { it.isFinite() && it > 5 }?.let { uv ->
                weatherBlock += " Tahminde UV indeksi: $uv. Güncel uyarıları kontrol edin."
            }

            // Kişiselleştirilmiş Ek Bilgi (Personalized Extra Info)
            if (isPersonalizationActive) {
                if (interests.any { it.contains("kamp", true) || it.contains("outdoor", true) } && (precip > 30 || windSpeed > 25)) {
                    weatherBlock += " 🏕️ Kamp için zemin ıslaklığına ve rüzgar şiddetine dikkat etmeni öneririm."
                }
                if (interests.any { it.contains("bisiklet", true) || it.contains("kis_surusu", true) } && windSpeed > 20) {
                    weatherBlock += " 🚲 Rüzgar hızı sürüşü biraz zorlaştırabilir."
                }
                if (interests.contains("cocuklar_icin") && maxTemp < 15) {
                    weatherBlock += " 👶 Minik gezginler için yedek katmanlar almayı unutma."
                }
            }
        } else {
            weatherBlock = when (tone) {
                AssistantTone.SAMIMI -> "Şu an tam veriye bakamadım ama mevsime göre hazırlan canım. Keyifli geçeceğine eminim!"
                AssistantTone.RESMI -> "Veri erişimi kısıtlılığı nedeniyle mevsim normallerine göre planlama yapılması önerilmektedir."
                AssistantTone.KISA_NET -> "Veri yok. Mevsimlik plan yap."
                else -> "Şu an hava servisine ulaşılamadı ama mevsim normallerine göre plan yapabilirsin. Genel olarak seyahat için elverişli ve güzel bir dönem."
            }
        }

        var packingBlock = ""
        if (forecastSnapshot != null) {
            val precip = forecastSnapshot.precipitationProbability ?: 0
            val maxTemp = forecastSnapshot.maxTemp?.toInt() ?: 20
            val minTemp = forecastSnapshot.minTemp?.toInt() ?: 15

            val basePacking = when {
                precip > 50 -> "Hava yağışlı geçeceği için şık bir yağmurluk ve su geçirmeyen bir ayakkabı hayat kurtarır. Valizine mutlaka sağlam bir şemsiye eklemelisin."
                maxTemp > 28 -> "Hava oldukça sıcak geçecek; pamuklu ve ferah kıyafetler seçmeni öneririm. Güneş gözlüğün ve yüksek koruyucu kremin olmazsa olmazın."
                maxTemp < 12 -> "Hava sert; kalın bir mont, atkı ve bere ikilisini valizine eklemelisin. Kat kat giyinmek seni gün boyu soğuktan koruyacaktır."
                else -> "Hafif bir ceket veya sweatshirt yanına almak mantıklı olur. Akşam serinliği için tedarikli olmakta fayda var."
            }

            packingBlock = basePacking

            // Kişiselleştirilmiş Valiz Önerisi
            if (isPersonalizationActive) {
                if (interests.any { it.contains("kamp", true) }) packingBlock += " ⛺ Uyku tulumu ve matını kontrol etmeyi unutma."
                if (interests.any { it.contains("koşu", true) }) packingBlock += " 🏃 En rahat koşu ayakkabılarını yanına almayı ihmal etme."
                if (interests.any { it.contains("fotoğraf", true) }) packingBlock += " 📷 Yedek piller ve lens temizleme kiti hayat kurtarabilir."
                if (maxTemp - minTemp > 10) packingBlock += " 🧥 Seyahat dönemindeki sıcaklık farkı fazla, hırka almanı öneririm."
            }
        } else {
            packingBlock = "Rahat bir yürüyüş ayakkabısı ve her ihtimale karşı akşamları serinleyebilecek havaya karşı hafif bir ceket valizinin olmazsa olmazı."
        }

        return "HAVA ÖZETİ|$weatherBlock [SEP] VALİZ TAVSİYESİ|$packingBlock [SEP] MUTLAKA GÖR|${guide?.visit ?: missingGuide} [SEP] DENEMEDEN DÖNME|${guide?.food ?: missingGuide} [SEP] YEREL TAVSİYE|${experienceAdvice ?: missingGuide}"
    }

    fun getCityDescription(city: String): String =
        LocalTravelGuides.find(city, null)?.description ?: city

    fun generateComparisonText(old: ForecastSnapshot, new: ForecastSnapshot, tone: AssistantTone = AssistantTone.DENGELI): String {
        val changes = mutableListOf<String>()

        // Helper to format based on tone
        fun formatDiff(text: String, samimi: String, resmi: String, kisa: String, uzman: String): String {
            return when(tone) {
                AssistantTone.SAMIMI -> samimi
                AssistantTone.RESMI -> resmi
                AssistantTone.KISA_NET -> kisa
                AssistantTone.DETAYLI_UZMAN -> uzman
                else -> text
            }
        }

        // 1. Skor Karşılaştırması
        if (old.travelScore != null && new.travelScore != null) {
            val scoreDiff = new.travelScore - old.travelScore
            if (Math.abs(scoreDiff) >= 5) {
                if (scoreDiff > 0) {
                    changes.add(formatDiff(
                        "Seyahat skoru iyileşiyor.",
                        "Seyahat şansın artıyor canım, koşullar daha iyi!",
                        "Seyahat uygunluk katsayısı pozitif yönde güncellenmiştir.",
                        "Skor arttı.",
                        "Termal stabilite indeksi %${old.travelScore}'den %${new.travelScore}'e yükselerek seyahat optimizasyonunu artırmıştır."
                    ))
                } else {
                    changes.add(formatDiff(
                        "Seyahat skoru düşüyor.",
                        "Hava biraz bozuyor sanki, planlarına dikkat et tatlım.",
                        "Meteorolojik riskler nedeniyle seyahat skoru düşürülmüştür.",
                        "Skor düştü.",
                        "Atmosferik perturbasyonlar nedeniyle seyahat skoru %${new.travelScore}'e gerilemiştir."
                    ))
                }
            }
        }

        // 2. Yağış Karşılaştırması
        val oldPrecip = old.precipitationProbability ?: 0
        val newPrecip = new.precipitationProbability ?: 0
        if (Math.abs(newPrecip - oldPrecip) >= 10) {
            if (newPrecip > oldPrecip) {
                changes.add(formatDiff(
                    "Yağmur riski arttı.",
                    "Daha çok yağmur yağacak gibi, şemsiyeni hazırla canım.",
                    "Yağış olasılığı artış göstermiştir.",
                    "Yağış arttı.",
                    "Presipitasyon gradyanı %$oldPrecip'ten %$newPrecip'e yükselmiştir."
                ))
            } else {
                changes.add(formatDiff(
                    "Yağış ihtimali azaldı.",
                    "Hava açıyor canım, yağmur ihtimali düştü!",
                    "Yağış beklentisi minimize edilmiştir.",
                    "Yağış azaldı.",
                    "Bulut kapalılık oranı ve presipitasyon riski azalma eğilimindedir."
                ))
            }
        }

        return when {
            changes.isEmpty() -> formatDiff("Büyük bir değişiklik yok.", "Hava aynı canım, sürpriz yok.", "Hava koşulları stabil seyretmektedir.", "Değişim yok.", "Meteorolojik parametrelerde anlamlı bir sapma gözlemlenmemiştir.")
            changes.size == 1 -> changes.first()
            else -> changes.joinToString(" ")
        }
    }

    fun generateHistorySummary(plan: TravelPlan, tone: AssistantTone = AssistantTone.DENGELI): TravelHistorySummary {
        val duration = java.time.temporal.ChronoUnit.DAYS.between(plan.startDate, plan.endDate).toInt() + 1

        // Use the actual historical analyses list if available
        // PROOF: This comes from the 'analyses' field of the TravelPlan which is persisted in Room/Firestore.
        val historicalData = plan.analyses
        val hasData = historicalData.isNotEmpty() && historicalData.any { it.summary.isNotBlank() }

        if (!hasData) {
            return TravelHistorySummary(
                averageTemp = null,
                minTemp = null,
                maxTemp = null,
                rainyDays = -1,
                sunnyDays = -1,
                cloudyDays = -1,
                riskDayText = "Hava verisi arşivde bulunamadı.",
                comfortScore = 0,
                summaryText = "Bu seyahat için kayıtlı hava analizi bulunmuyor.",
                packingAdvice = "Geçmiş seyahat için öneri sunulmuyor.",
                nextTripAdvice = "Gelecek planların için ana ekrandaki tahminleri takip edebilirsin.",
                durationDays = duration
            )
        }

        // Derive summary from the LATEST recorded analysis for this trip in retrospective past tense
        val latestRecorded = historicalData.last()
        val avgT = latestRecorded.averageTemperature.toInt()
        val avgScore = latestRecorded.travelScore
        val rainRisk = latestRecorded.rainRiskPercent

        val weatherDesc = when {
            avgT > 28 -> "sıcak"
            avgT < 10 -> "serin ve soğuk"
            else -> "ılıman"
        }

        val rainText = when {
            rainRisk == null -> "Kaydedilen analizde yağış olasılığı bilgisi bulunmuyor."
            rainRisk > 20 -> "Planlama tahmininde yağış riski %$rainRisk olarak öngörülüyordu."
            else -> "Planlama tahmininde yağış riski düşüktü."
        }

        val summaryText = when(tone) {
            AssistantTone.SAMIMI -> "${plan.city} seyahatin $duration gün sürdü canım. Planlama sırasında kaydedilen hava tahmininde hava genel olarak $weatherDesc idi ve ortalama sıcaklık ${avgT}°C civarında öngörülüyordu. $rainText"
            AssistantTone.RESMI -> "${plan.city} seyahati $duration günlük periyotta gerçekleşmiştir. Planlama evresindeki tahmin verilerine göre hava koşulları $weatherDesc öngörülmüş, sıcaklık ortalaması ${avgT}°C olarak hesaplanmıştır. $rainText"
            else -> "${plan.city} seyahatin $duration gün sürdü. Planlama sırasında kaydedilen hava tahmininde hava genel olarak $weatherDesc idi ve ortalama sıcaklık ${avgT}°C civarında öngörülüyordu. $rainText"
        }

        return TravelHistorySummary(
            averageTemp = avgT,
            minTemp = historicalData.map { it.averageTemperature }.minOrNull()?.toInt() ?: avgT,
            maxTemp = historicalData.map { it.averageTemperature }.maxOrNull()?.toInt() ?: avgT,
            rainyDays = -1,
            sunnyDays = -1,
            cloudyDays = -1,
            riskDayText = "Arşivlenen analiz verileri kullanılıyor.",
            comfortScore = avgScore,
            summaryText = summaryText,
            packingAdvice = "Kaydedilen ortalama sıcaklık: $avgT°C",
            nextTripAdvice = "Seyahat geçmişin, gelecek planların için bir rehber niteliğindedir.",
            durationDays = duration
        )
    }
}
