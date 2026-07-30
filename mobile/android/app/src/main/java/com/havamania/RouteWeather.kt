package com.havamania

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs

/**
 * Akıllı Güzergâh Hava Durumu — Aşama 5 (hava + risk bağlama).
 *
 * Rota üzerindeki her nokta için (başlangıç, ara noktalar, varış) tahmini geçiş anına en
 * yakın saatlik Open-Meteo tahminini çeker ve WMO koduna göre bir sürüş riski atar.
 */

/** Bir güzergâh noktasının sürüş riski (marker rengi + panel özeti bu değerden gelir). */
enum class RouteRisk {
    /** Uygun — hava sürüşü etkilemiyor (yeşil). */
    OK,
    /** Dikkat — yağmur, sis, yüksek yağış ihtimali (amber). */
    CAUTION,
    /** Tehlikeli — kar, buzlanma, fırtına, şiddetli sağanak (kırmızı). */
    DANGER
}

/** Tek bir noktanın çözümlenmiş hava + risk verisi. */
data class WaypointWeather(
    val weatherCode: Int,
    val temperatureC: Double,
    val apparentTempC: Double?,
    val precipProbability: Int?,
    val windSpeedKmh: Double?,
    val humidity: Int?,
    val risk: RouteRisk,
    /** Riski açıklayan kısa metin (OK ise null). */
    val riskReason: String?
)

/** WMO hava koduna + sıcaklığa + yağış ihtimaline göre sürüş riski üretir. */
object RouteRiskAssessor {
    fun assess(weatherCode: Int, precipProbability: Int?, temperatureC: Double): Pair<RouteRisk, String?> =
        when {
            // Buzlanan yağmur / çiseleme
            weatherCode in listOf(56, 57, 66, 67) -> RouteRisk.DANGER to "Buzlanma riski (donan yağış)"
            // Kar
            weatherCode in listOf(71, 73, 75, 77, 85, 86) -> RouteRisk.DANGER to "Kar yağışı"
            // Gök gürültülü fırtına
            weatherCode in listOf(95, 96, 99) -> RouteRisk.DANGER to "Gök gürültülü fırtına"
            // Şiddetli sağanak
            weatherCode == 82 -> RouteRisk.DANGER to "Şiddetli sağanak"
            // Donma noktası — yol buzlanması
            temperatureC <= 0.0 -> RouteRisk.DANGER to "Buzlanma (sıcaklık ${temperatureC.toInt()}°C)"

            // Sis — görüş
            weatherCode in listOf(45, 48) -> RouteRisk.CAUTION to "Sis, görüş mesafesi düşük"
            // Yağmur / sağanak
            weatherCode in listOf(61, 63, 65, 80, 81) -> RouteRisk.CAUTION to "Yağmur"
            // Çiseleme
            weatherCode in listOf(51, 53, 55) -> RouteRisk.CAUTION to "Çiseleyen yağmur"
            // Yüksek yağış ihtimali (kod hafif ama olasılık yüksek)
            (precipProbability ?: 0) >= 60 -> RouteRisk.CAUTION to "Yüksek yağış ihtimali"

            else -> RouteRisk.OK to null
        }
}

/** ETA'ya en yakın saatlik tahmini getiren sağlayıcı (Open-Meteo, key gerektirmez). */
class RouteWeatherProvider(
    private val api: WeatherApiService = NetworkModule.apiService
) {
    /**
     * [point] için, verilen tahmini geçiş anına ([etaEpochMillis]) en yakın saatlik tahmini
     * çözer. Hata / veri yoksa null döner (çağıran taraf noktayı "veri yok" olarak gösterir).
     */
    suspend fun weatherAt(point: GeoPoint, etaEpochMillis: Long?): WaypointWeather? =
        withContext(Dispatchers.IO) {
            try {
                val resp = api.getRouteHourly(lat = point.latitude, lon = point.longitude)
                val hourly = resp.hourly ?: return@withContext null
                val idx = hourly.indexNearest(etaEpochMillis, resp.timezone) ?: return@withContext null
                val code = hourly.weatherCode.getOrNull(idx) ?: return@withContext null
                val temp = hourly.temperature.getOrNull(idx) ?: return@withContext null
                val feels = hourly.apparentTemperature?.getOrNull(idx)
                val prob = hourly.precipitationProbability?.getOrNull(idx)
                val wind = hourly.windSpeed?.getOrNull(idx)
                val hum = hourly.humidity?.getOrNull(idx)
                val (risk, reason) = RouteRiskAssessor.assess(code, prob, temp)
                WaypointWeather(code, temp, feels, prob, wind, hum, risk, reason)
            } catch (e: Exception) {
                null
            }
        }

    /** Saatlik zaman dizisinde hedef ana en yakın indeksi bulur. */
    private fun HourlyDto.indexNearest(etaEpochMillis: Long?, timezone: String): Int? {
        if (time.isEmpty()) return null
        if (etaEpochMillis == null) return 0
        val zone = try {
            ZoneId.of(timezone)
        } catch (e: Exception) {
            ZoneId.systemDefault()
        }
        // Open-Meteo timezone=auto → saatlik zamanlar noktanın yerel saatinde döner.
        val target = Instant.ofEpochMilli(etaEpochMillis).atZone(zone).toLocalDateTime()
        var bestIdx = 0
        var bestDiff = Long.MAX_VALUE
        for (i in time.indices) {
            val t = try {
                LocalDateTime.parse(time[i])
            } catch (e: Exception) {
                continue
            }
            val diff = abs(Duration.between(t, target).toMinutes())
            if (diff < bestDiff) {
                bestDiff = diff
                bestIdx = i
            }
        }
        return bestIdx
    }
}
