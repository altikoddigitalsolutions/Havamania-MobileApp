package com.havamania

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Akıllı Güzergâh Hava Durumu — Aşama 5 (hava + risk bağlama).
 *
 * Rota üzerindeki her nokta için (başlangıç, ara noktalar, varış) tahmini geçiş anına en
 * yakın saatlik Open-Meteo tahminini çeker ve WMO koduna göre bir sürüş riski atar.
 */

/** Bir güzergâh noktasının sürüş riski (marker rengi + panel özeti bu değerden gelir). */
enum class RouteRisk {
    /** Mevcut değerlendirilen alanlarda belirgin risk yok (yeşil). */
    OK,
    /** Dikkat — yağmur, sis, yüksek yağış ihtimali (amber). */
    CAUTION,
    /** Tehlikeli — kar, buzlanma, fırtına, şiddetli sağanak (kırmızı). */
    DANGER
}

/** Tek bir noktanın çözümlenmiş hava + risk verisi. */
data class WaypointWeather(
    val weatherCode: Int,
    val temperatureC: Double?,
    val apparentTempC: Double?,
    val precipProbability: Int?,
    val windSpeedKmh: Double?,
    val humidity: Int?,
    val risk: RouteRisk,
    /** Riski açıklayan kısa metin (OK ise null). */
    val riskReason: String?,
    val isDay: Boolean? = null,
    val windGustKmh: Double? = null,
    val hasIncompleteData: Boolean = false
)

/** Precautionary app risk policy; missing inputs cannot prove an OK route. */
object RouteRiskAssessor {
    fun assess(weatherCode: Int, precipProbability: Int?, temperatureC: Double?,
        windSpeedKmh: Double? = null, windGustKmh: Double? = null): Pair<RouteRisk, String?> {
        val temp = validRouteTemperature(temperatureC)
        val prob = precipProbability?.takeIf { it in 0..100 }
        val wind = validRouteWind(windSpeedKmh)
        val gust = validRouteWind(windGustKmh)
        val risks = mutableListOf<Pair<RouteRisk, String>>()
        when (weatherCode) {
            56, 57, 66, 67 -> risks += RouteRisk.DANGER to "Buzlanma riski (donan yağış)"
            71, 73, 75, 77, 85, 86 -> risks += RouteRisk.DANGER to "Kar yağışı"
            95, 96, 99 -> risks += RouteRisk.DANGER to "Gök gürültülü fırtına"
            82 -> risks += RouteRisk.DANGER to "Şiddetli sağanak"
            45, 48 -> risks += RouteRisk.CAUTION to "Sis, görüş mesafesi düşük"
            61, 63, 65, 80, 81 -> risks += RouteRisk.CAUTION to "Yağmur"
            51, 53, 55 -> risks += RouteRisk.CAUTION to "Çiseleyen yağmur"
        }
        if (temp != null && temp <= 0) risks += RouteRisk.DANGER to "Donma düzeyinde sıcaklık (${temp.toInt()}°C); buzlanma açısından dikkat"
        if (prob != null && prob >= 60) risks += RouteRisk.CAUTION to "Yüksek yağış ihtimali"
        wind?.takeIf { it >= ROUTE_WIND_CAUTION_KMH }?.let {
            risks += (if (it >= ROUTE_WIND_DANGER_KMH) RouteRisk.DANGER else RouteRisk.CAUTION) to "Kuvvetli rüzgar (${it.toInt()} km/sa)"
        }
        gust?.takeIf { it >= ROUTE_WIND_CAUTION_KMH }?.let {
            risks += (if (it >= ROUTE_WIND_DANGER_KMH) RouteRisk.DANGER else RouteRisk.CAUTION) to "Kuvvetli rüzgar hamlesi (${it.toInt()} km/sa)"
        }
        if (!WeatherUtils.isKnownCode(weatherCode) || temp == null || prob == null || wind == null || gust == null)
            risks += RouteRisk.CAUTION to "Hava verisi eksik; tüm sürüş riskleri değerlendirilemedi"
        if (risks.isEmpty()) return RouteRisk.OK to null
        return risks.maxBy { it.first.ordinal }.first to risks.map { it.second }.distinct().joinToString("; ")
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
                val resp = api.getRouteHourly(lat = point.latitude, lon = point.longitude, timezone = "UTC")
                val hourly = resp.hourly ?: return@withContext null
                val idx = hourly.indexNearest(etaEpochMillis, resp.timezone) ?: return@withContext null

                hourly.routeWeatherSample(idx)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                null
            }
        }

    /** Birden fazla nokta için hava durumunu tek seferde çeker (Open-Meteo toplu istek). */
    suspend fun weatherAtBatch(
        points: List<GeoPoint>,
        etas: List<Long?>
    ): List<WaypointWeather?> = withContext(Dispatchers.IO) {
        if (points.isEmpty()) return@withContext emptyList()
        if (points.size == 1) return@withContext listOf(weatherAt(points.single(), etas.firstOrNull()))
        try {
            val lats = points.map { it.latitude }
            val lons = points.map { it.longitude }

            val responses = api.getBatchRouteHourly(lats = lats.joinToString(","), lons = lons.joinToString(","), timezone = "UTC")

            points.indices.map { i ->
                val resp = responses.getOrNull(i) ?: return@map null
                val hourly = resp.hourly ?: return@map null
                val eta = etas.getOrNull(i)
                val idx = hourly.indexNearest(eta, resp.timezone) ?: return@map null

                hourly.routeWeatherSample(idx)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                Log.e("RouteWeather", "Batch fetch failed, falling back to individual", e)
}
            // Hata durumunda boş liste dönmek yerine bireysel deneme yapılabilir
            // ama genellikle bağlantı hatasıdır.
            points.map { null }
        }
    }

    /** Saatlik zaman dizisinde hedef ana en yakın indeksi bulur. */
    private fun RouteHourlyDto.indexNearest(etaEpochMillis: Long?, timezone: String?): Int? {
        if (time.isEmpty()) return null
        if (etaEpochMillis == null) return null
        val zone = timezone?.let(::validTripForecastZone) ?: return null
        // Open-Meteo timezone=auto → saatlik zamanlar noktanın yerel saatinde döner.
        val target = Instant.ofEpochMilli(etaEpochMillis).atZone(zone).toLocalDateTime()
        var earlierIndex = -1
        var laterIndex = -1
        var earlierDistance = Long.MAX_VALUE
        var laterDistance = Long.MAX_VALUE
        for (i in time.indices) {
            val t = try {
                LocalDateTime.parse(time[i])
            } catch (e: Exception) {
                continue
            }
            val offset = Duration.between(target, t).toMillis()
            if (offset == 0L) return i
            if (offset < 0 && -offset < earlierDistance) {
                earlierDistance = -offset
                earlierIndex = i
            }
            if (offset > 0 && offset < laterDistance) {
                laterDistance = offset
                laterIndex = i
            }
        }
        if (earlierIndex < 0 || laterIndex < 0) return null
        // A missing hourly sample must not be covered by a neighboring forecast.
        if (earlierDistance + laterDistance > 3_600_000) return null
        return if (earlierDistance <= laterDistance) earlierIndex else laterIndex
    }
}
