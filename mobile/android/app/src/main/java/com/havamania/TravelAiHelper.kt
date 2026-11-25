package com.havamania

import com.havamania.ui.theme.AssistantTone
import java.time.LocalDate

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
        val interests = personalization.travelInterests()

        val windowDays = TRIP_ANALYSIS_WINDOW_DAYS

        val guide = LocalTravelGuides.find(city, district)
        val experiences = LocalTravelGuides.experiences(city, district, tripType, interests)
        val experienceAdvice = experiences.takeIf { it.isNotEmpty() }
            ?.joinToString("\n\n") { "${it.title}\n${it.description}" } ?: guide?.experience
        val destination = district?.takeIf { it.isNotBlank() }?.let { "$it, $city" } ?: city
        val missingGuide = "$destination için yer ve yemek adları doğrulanmış rehber henüz hazır değil."

        if (daysUntilTrip > windowDays) {
            return "HAVA ÖZETİ|Hava tahmini seyahate $windowDays gün kala hazırlanacak. [SEP] VALİZ TAVSİYESİ|Havaya göre valiz önerisi için tahmin bekleniyor. [SEP] MUTLAKA GÖR|${guide?.visit ?: missingGuide} [SEP] DENEMEDEN DÖNME|${guide?.food ?: missingGuide} [SEP] YEREL TAVSİYE|${experienceAdvice ?: missingGuide}"
        }

        val weatherBlock = travelWeatherAdvice(forecastSnapshot, tone, personalization)
        val packingBlock = travelWeatherPacking(forecastSnapshot, personalization)

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
        val oldPrecip = old.precipitationProbability?.takeIf { it in 0..100 }
        val newPrecip = new.precipitationProbability?.takeIf { it in 0..100 }
        if (oldPrecip != null && newPrecip != null && Math.abs(newPrecip - oldPrecip) >= 10) {
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
            changes.isEmpty() -> if (oldPrecip == null || newPrecip == null || old.travelScore == null || new.travelScore == null)
                "Karşılaştırma için bazı veriler eksik; hava koşullarının değişmediği söylenemez."
            else "Karşılaştırılan yağış olasılığı ve seyahat skorunda belirgin değişiklik yok."
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
