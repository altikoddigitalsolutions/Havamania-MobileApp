package com.havamania

import com.havamania.ui.theme.AssistantTone
import java.util.Locale

/** Missing or malformed observations remain unknown; never substitute pleasant weather. */
internal fun ForecastSnapshot.validAdviceData(): ForecastSnapshot {
    var low = minTemp?.takeIf { it.isFinite() && it in -100.0..65.0 }
    var high = maxTemp?.takeIf { it.isFinite() && it in -100.0..65.0 }
    if (low != null && high != null && low > high) { low = null; high = null }
    return copy(minTemp = low, maxTemp = high,
        precipitationProbability = precipitationProbability?.takeIf { it in 0..100 },
        windSpeed = windSpeed?.takeIf { it.isFinite() && it in 0.0..500.0 },
        uvIndex = uvIndex?.takeIf { it.isFinite() && it in 0.0..25.0 })
}

private fun Double.adviceNumber() = String.format(Locale.forLanguageTag("tr-TR"), "%.1f", this)

internal fun travelWeatherAdvice(snapshot: ForecastSnapshot?, tone: AssistantTone, profile: PersonalizationProfile?): String {
    if (snapshot == null) return when (tone) {
        AssistantTone.SAMIMI -> "Bu seyahat için henüz hava tahmini bulunmuyor. Planını güncel tahmin geldikten sonra netleştirebilirsin."
        AssistantTone.KISA_NET -> "Hava tahmini bulunmuyor. Güncel tahmini kontrol edin."
        else -> "Bu seyahat için hava tahmini bulunmuyor; koşulların uygunluğu değerlendirilemiyor. Güncel tahminleri kontrol edin."
    }
    val data = snapshot.validAdviceData()
    val parts = mutableListOf<String>()
    // Keep rare hazards even if other measurements are absent. No optimistic suitability claim.
    tripWeatherWarning(data)?.let(parts::add)
    data.maxTemp?.let { parts += "Tahmini en yüksek sıcaklık: ${it.adviceNumber()}°C." }
    data.minTemp?.let { parts += "Tahmini en düşük sıcaklık: ${it.adviceNumber()}°C." }
    if (parts.isEmpty()) data.conditionSummary?.takeIf { it.isNotBlank() }?.let { parts += "Tahmin: $it." }
    data.precipitationProbability?.let { parts += "Yağış olasılığı: %$it." }
    data.windSpeed?.let { parts += "Rüzgar: ${WeatherUtils.formatWindWithLevel(it)}." }
    if ((data.windSpeed ?: 0.0) > 30) parts += "Kuvvetli rüzgara karşı açık hava planınızı gözden geçirin."
    data.uvIndex?.takeIf { it > 5 }?.let { parts += "Tahminde UV indeksi: $it. Güncel uyarıları kontrol edin." }
    val missing = buildList {
        if (data.maxTemp == null || data.minTemp == null) add("sıcaklık")
        if (data.precipitationProbability == null) add("yağış olasılığı")
        if (data.windSpeed == null) add("rüzgar")
    }
    if (missing.isNotEmpty()) parts += "Tahmin verisi eksik (${missing.joinToString(", ")}); koşulların uygunluğu kesinleştirilemiyor."
    val interests = profile.travelInterests()
    if (interests.any { it in setOf("kamp", "acik_hava", "outdoor", "dagcilik", "trekking") } &&
        ((data.precipitationProbability ?: 0) > 30 || (data.windSpeed ?: 0.0) > 25))
        parts += "Kamp ve yürüyüş için zemin ıslaklığına ve rüzgara dikkat et."
    if (interests.any { it.contains("bisiklet") || it == "kis_surusu" } && (data.windSpeed ?: 0.0) > 20)
        parts += "Rüzgar sürüşü zorlaştırabilir."
    if ("cocuklar_icin" in interests && data.maxTemp?.let { it < 15 } == true)
        parts += "Çocuklar için yedek katmanlar hazırla."
    return parts.joinToString(" ")
}

internal fun travelWeatherPacking(snapshot: ForecastSnapshot?, profile: PersonalizationProfile?): String {
    val data = snapshot?.validAdviceData()
    val items = mutableListOf<String>()
    val preparation = travelWeatherPreparation(data)
    if (WeatherPreparation.RAIN in preparation) items += "Yağış tahminine karşı yağmurluk ve su geçirmeyen ayakkabı hazırla."
    if (WeatherPreparation.COLD in preparation) items += "Soğuk veya donan yağış tahminine karşı sıcak tutan katmanlar ve kaymayan ayakkabı hazırla."
    if (WeatherPreparation.HEAT in preparation) items += "Sıcaklık tahminine göre hafif, nefes alan kıyafetler ve içme suyu hazırla."
    if (WeatherPreparation.WIND in preparation) items += "Rüzgara karşı koruyucu bir dış katman hazırla."
    if (WeatherPreparation.UV in preparation) items += "Yüksek UV tahminine karşı güneşten korunma ürünlerini hazırla."
    if (WeatherPreparation.TEMPERATURE_SWING in preparation) items += "Seyahat dönemindeki sıcaklık farkı fazla; hırka ve katmanlı kıyafet hazırla."
    if (data == null || data.maxTemp == null || data.minTemp == null || data.precipitationProbability == null || data.windSpeed == null)
        items += "Havaya göre valizi tamamlamak için eksik tahmin verilerinin güncellenmesini bekleyin."
    else if (items.isEmpty()) items += "Kıyafetlerini tahmindeki sıcaklık aralığına ve planladığın etkinliklere göre seç."
    personalizedTravelPacking(profile).takeIf { it.isNotBlank() }?.let(items::add)
    return items.joinToString(" ")
}
internal enum class WeatherPreparation { RAIN, COLD, HEAT, WIND, UV, TEMPERATURE_SWING }

/** One evidence rule set for guide prose and notification equipment. Unknown is never mild. */
internal fun travelWeatherPreparation(snapshot: ForecastSnapshot?): Set<WeatherPreparation> {
    val data = snapshot?.validAdviceData() ?: return emptySet()
    return buildSet {
        if (data.precipitationProbability?.let { it > 50 } == true || data.weatherCode in setOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99)) add(WeatherPreparation.RAIN)
        if (data.minTemp?.let { it <= 0 } == true || data.maxTemp?.let { it < 12 } == true || data.weatherCode in setOf(56, 57, 66, 67, 71, 73, 75, 77, 85, 86)) add(WeatherPreparation.COLD)
        if (data.maxTemp?.let { it > 28 } == true) add(WeatherPreparation.HEAT)
        if (data.windSpeed?.let { it > 30 } == true) add(WeatherPreparation.WIND)
        if (data.uvIndex?.let { it > 5 } == true) add(WeatherPreparation.UV)
        if (data.maxTemp != null && data.minTemp != null && data.maxTemp - data.minTemp > 10) add(WeatherPreparation.TEMPERATURE_SWING)
    }
}
