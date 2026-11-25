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
        if (old.generatedAt > 0 && new.generatedAt > 0 && new.generatedAt < old.generatedAt)
            return "Yeni kabul edilen kayıt önceki kayıttan eski; değişim karşılaştırılamıyor."
        val oldScore = old.travelScore?.takeIf { it in 0..100 }
        val newScore = new.travelScore?.takeIf { it in 0..100 }
        val oldRain = old.precipitationProbability?.takeIf { it in 0..100 }
        val newRain = new.precipitationProbability?.takeIf { it in 0..100 }
        val parts = mutableListOf<String>()
        if (oldScore != null && newScore != null && kotlin.math.abs(newScore - oldScore) >= 5) {
            val direction = if (newScore > oldScore) "arttı" else "azaldı"
            parts += "Konfor skoru $oldScore/100 → $newScore/100 $direction."
        }
        if (oldRain != null && newRain != null && kotlin.math.abs(newRain - oldRain) >= 10) {
            val direction = if (newRain > oldRain) "arttı" else "azaldı"
            parts += "Yağış olasılığı %$oldRain → %$newRain $direction."
        }
        val incomplete = oldScore == null || newScore == null || oldRain == null || newRain == null
        if (incomplete) parts += "Karşılaştırma için bazı veriler eksik veya geçersiz; eksik alanların değişimi değerlendirilemiyor."
        else if (parts.isEmpty()) parts += "Karşılaştırılan yağış olasılığı ve konfor skorunda belirgin değişiklik yok."
        // A score change cannot replace a known snow, fog or storm warning.
        tripWeatherWarning(new)?.let(parts::add)
        val prefix = when (tone) {
            AssistantTone.SAMIMI -> "Tahmindeki değişim şöyle: "
            AssistantTone.RESMI -> "Kayıtlı tahminlerin karşılaştırması: "
            AssistantTone.DETAYLI_UZMAN -> "Sayısal tahmin karşılaştırması: "
            else -> ""
        }
        return prefix + parts.joinToString(" ")
    }

    fun generateHistorySummary(plan: TravelPlan, tone: AssistantTone = AssistantTone.DENGELI): TravelHistorySummary {
        val days = java.time.temporal.ChronoUnit.DAYS.between(plan.startDate, plan.endDate) + 1
        val duration = days.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        // Stored forecast revisions are not daily observations. Use one latest record, never mix gaps.
        val latest = plan.analyses.withIndex().maxWithOrNull(
            compareBy<IndexedValue<TravelWeatherAnalysis>> { it.value.createdAt.coerceAtLeast(0) }.thenBy { it.index })?.value
        val temperature = latest?.averageTemperature?.takeIf { it.isFinite() && it in -100.0..65.0 }?.toInt()
        val score = latest?.travelScore?.takeIf { it in 0..100 }
        val rain = latest?.rainRiskPercent?.takeIf { it in 0..100 }
        val rainText = rain?.let { "Planlama tahmininde yağış olasılığı %$it olarak kaydedilmişti." }
            ?: "Kaydedilen analizde yağış olasılığı bilgisi bulunmuyor."
        val temperatureText = temperature?.let { "Planlama tahmininde dönem ortalaması $it°C olarak kaydedilmişti." }
            ?: "Kaydedilen analizde geçerli sıcaklık bilgisi bulunmuyor."
        val dateText = if (duration == 0) "Seyahat tarih aralığı geçersiz." else when (tone) {
            AssistantTone.RESMI -> "${plan.city} seyahatinin kayıtlı süresi $duration gündür."
            AssistantTone.SAMIMI -> "${plan.city} seyahatinin kayıtlı süresi $duration gün."
            else -> "${plan.city} seyahati: $duration gün."
        }
        return TravelHistorySummary(
            averageTemp = temperature,
            minTemp = null,
            maxTemp = null,
            rainyDays = -1,
            sunnyDays = -1,
            cloudyDays = -1,
            riskDayText = if (latest == null) "Hava verisi arşivde bulunamadı." else "Kayıtlı planlama tahmini kullanılıyor; gerçekleşen hava ölçümü değildir.",
            comfortScore = score,
            summaryText = if (latest == null) "Bu seyahat için kayıtlı hava analizi bulunmuyor."
                else "$dateText $temperatureText $rainText Bu değerler gerçekleşen hava ölçümleri değildir.",
            packingAdvice = temperature?.let { "Kaydedilen tahmini dönem ortalaması: $it°C" }
                ?: "Geçmiş seyahat için sıcaklığa dayalı öneri sunulamıyor.",
            nextTripAdvice = "Gelecek planların için güncel tahminleri takip edebilirsin.",
            durationDays = duration
        )
    }
}
