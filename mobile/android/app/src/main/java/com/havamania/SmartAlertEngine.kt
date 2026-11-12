package com.havamania

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*

object SmartAlertEngine {

    fun generateAlerts(weather: WeatherData, config: SmartAlertConfig, uid: String = "legacy",
                       now: java.time.Instant = java.time.Instant.now()): List<SmartAlert> {
        if (weather.isStale || !weatherCacheIsFresh(weather.timestamp, now.toEpochMilli()) ||
            !cachedWeatherSameLocalDay(weather, now)) return emptyList()
        val alerts = mutableListOf<SmartAlert>()
        val todayStr = ForecastTime.localNow(weather.timezone, now).toLocalDate().toString()
        val city = weather.cityName

        // Helper to create unique key
        fun createKey(type: String) = "$uid|$city|$type|$todayStr"

        // 1. Rain Alert (Next 2 hours)
        if (config.rainEnabled) {
            val next2HoursRain = rainRiskInNextTwoHours(weather, now)
            if (next2HoursRain) {
                alerts.add(SmartAlert(
                    id = "rain",
                    title = "Yağmur Uyarısı",
                    description = "Önümüzdeki 2 saatlik tahminde yağış riski var.",
                    icon = Icons.Rounded.WaterDrop,
                    severity = AlertSeverity.WARNING,
                    deduplicationKey = createKey("rain")
                ))
            }
        }

        // 2. Wind Alert (> 40 km/h)
        if (config.windEnabled) {
            val speed = weather.windSpeed?.takeIf { it.isFinite() && it >= 0 } ?: 0.0
            if (speed > 40.0) {
                alerts.add(SmartAlert(
                    id = "wind",
                    title = "Kuvvetli Rüzgar",
                    description = "Bugün rüzgar şiddetli (${speed.toInt()} km/sa).",
                    icon = Icons.Rounded.Air,
                    severity = AlertSeverity.WARNING,
                    deduplicationKey = createKey("wind")
                ))
            }
        }

        // 3. Heat Alert (> 35°C)
        if (config.heatEnabled) {
            val temp = weather.temperature.trim().removeSuffix("°").toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0
            if (temp > 35.0) {
                alerts.add(SmartAlert(
                    id = "heat",
                    title = "Aşırı Sıcaklık",
                    description = "Sıcaklık 35°C üzerine çıktı. Bol su tüketin.",
                    icon = Icons.Rounded.WbSunny,
                    severity = AlertSeverity.CRITICAL,
                    deduplicationKey = createKey("heat")
                ))
            }
        }

        // 4. Frost Alert (< 0°C)
        if (config.frostEnabled) {
            val minTemp = weather.low.trim().removeSuffix("°").toDoubleOrNull()?.takeIf { it.isFinite() } ?: 5.0
            if (minTemp <= 0) {
                alerts.add(SmartAlert(
                    id = "frost",
                    title = "Don Riski",
                    description = "Günlük en düşük sıcaklık $minTemp°C; don riski için yerel uyarıları kontrol edin.",
                    icon = Icons.Rounded.AcUnit,
                    severity = AlertSeverity.WARNING,
                    deduplicationKey = createKey("frost")
                ))
            }
        }

        // ... (rest of alerts with deduplicationKey)
        if (config.fogEnabled && weather.weatherCode in listOf(45, 48)) {
            alerts.add(SmartAlert(
                id = "fog",
                title = "Sis Uyarısı",
                description = "Görüş mesafesi düşük olabilir.",
                icon = Icons.Rounded.Visibility,
                severity = AlertSeverity.INFO,
                deduplicationKey = createKey("fog")
            ))
        }

        if (config.stormEnabled && weather.weatherCode in listOf(95, 96, 99)) {
            alerts.add(SmartAlert(
                id = "storm",
                title = "Fırtına",
                description = "Gök gürültülü fırtına riski mevcut.",
                icon = Icons.Rounded.Thunderstorm,
                severity = AlertSeverity.CRITICAL,
                deduplicationKey = createKey("storm")
            ))
        }

        if (config.uvEnabled && (weather.uvIndex ?: 0) > 6) {
            alerts.add(SmartAlert(
                id = "uv",
                title = "UV Uyarısı",
                description = "Güneşin etkisi sert. Korunmayı unutmayın.",
                icon = Icons.Rounded.LightMode,
                severity = AlertSeverity.WARNING,
                deduplicationKey = createKey("uv")
            ))
        }

        return alerts
    }
}
