package com.havamania

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt

object WeatherMapper {

    fun mapToDomain(response: OpenMeteoResponse, cityName: String, districtName: String? = null,
                    now: java.time.Instant = java.time.Instant.now()): WeatherData {
        // Validate observations before any rounding, scoring or derived calculation.
        val current = response.current?.let { value -> value.copy(
            temperature = value.temperature?.takeIf { it.isFinite() },
            apparentTemperature = value.apparentTemperature?.takeIf { it.isFinite() },
            humidity = value.humidity?.takeIf { it in 0..100 },
            windSpeed = value.windSpeed?.takeIf { it.isFinite() && it >= 0 },
            windGusts = value.windGusts?.takeIf { it.isFinite() && it >= 0 },
            windDirection = value.windDirection?.takeIf { it.isFinite() && it in 0.0..360.0 },
            pressure = value.pressure?.takeIf { it.isFinite() && it > 0 },
            visibility = value.visibility?.takeIf { it.isFinite() && it >= 0 },
            dewPoint = value.dewPoint?.takeIf { it.isFinite() },
            precipitation = value.precipitation?.takeIf { it.isFinite() && it >= 0 },
            cloudCover = value.cloudCover?.takeIf { it in 0..100 },
            uvIndex = value.uvIndex?.takeIf { it.isFinite() && it >= 0 }
        ) }
        val allDaily = response.daily
        val today = ForecastTime.localNow(response.timezone, now).toLocalDate()
        val daily = allDaily?.let { source ->
            val indices = source.time.indices.filter { source.time[it] == today.toString() }
            val index = indices.singleOrNull() ?: return@let null
            source.copy(time = listOf(today.toString()),
                weatherCode = listOfNotNull(source.weatherCode.getOrNull(index)),
                tempMin = listOfNotNull(source.tempMin.getOrNull(index)),
                tempMax = listOfNotNull(source.tempMax.getOrNull(index)),
                precipProbMax = source.precipProbMax?.getOrNull(index)?.takeIf { it in 0..100 }?.let { listOf(it) },
                uvIndexMax = source.uvIndexMax?.getOrNull(index)?.takeIf { it.isFinite() && it >= 0 }?.let { listOf(it) },
                sunrise = listOfNotNull(source.sunrise.getOrNull(index)),
                sunset = listOfNotNull(source.sunset.getOrNull(index)))
        }
        val hourly = response.hourly

        val windDirLabel = current?.windDirection?.toInt()?.let { getWindDirectionLabel(it) }

        val windChillValue = current?.temperature?.let { temperature ->
            current.windSpeed?.let { speed -> calculateWindChill(temperature, speed) }
        }

        val suitability = calculateSuitability(current, daily)

        val todayIndex = daily?.time?.indexOf(today.toString()) ?: -1
        val (sunrise, sunset) = ForecastSun.validTimes(
            ForecastSun.timeForDate(daily?.sunrise?.getOrNull(todayIndex), today.toString()),
            ForecastSun.timeForDate(daily?.sunset?.getOrNull(todayIndex), today.toString()))
        val solarNoon = ForecastSun.daylightMidpoint(sunrise, sunset)
        val dailyMin = daily?.tempMin?.firstOrNull()?.takeIf { it.isFinite() }
        val dailyMax = daily?.tempMax?.firstOrNull()?.takeIf { it.isFinite() }
        val rangeValid = dailyMin == null || dailyMax == null || dailyMin <= dailyMax

        return WeatherData(
            cityName = cityName,
            districtName = districtName,
            latitude = response.latitude,
            longitude = response.longitude,
            temperature = current?.temperature?.let { "${it.roundToInt()}°" } ?: "--",
            condition = getWeatherCondition(current?.weatherCode ?: -1),
            weatherCode = current?.weatherCode ?: -1,
            isDay = current?.isDay == 1,
            high = dailyMax?.takeIf { rangeValid }?.let { "${it.roundToInt()}°" } ?: "--",
            low = dailyMin?.takeIf { rangeValid }?.let { "${it.roundToInt()}°" } ?: "--",
            feelsLike = current?.apparentTemperature?.let { "${it.roundToInt()}°" } ?: "--",
            sunriseTime = if (sunrise.isNullOrEmpty()) null else sunrise,
            sunsetTime = if (sunset.isNullOrEmpty()) null else sunset,
            timezone = response.timezone,
            solarNoon = if (solarNoon.isNullOrEmpty()) null else solarNoon,
            windSpeed = current?.windSpeed,
            windGust = current?.windGusts,
            windDirectionDegrees = current?.windDirection?.toInt(),
            windDirectionLabel = windDirLabel,
            windChill = windChillValue,
            dewPoint = current?.dewPoint,
            precipitationProbability = daily?.precipProbMax?.firstOrNull(),
            precipitationAmount = current?.precipitation,
            cloudCover = current?.cloudCover,
            visibilityKm = current?.visibility?.let { it / 1000.0 },
            humidity = current?.humidity,
            pressure = current?.pressure?.toInt(),
            uvIndex = current?.uvIndex?.toInt() ?: daily?.uvIndexMax?.firstOrNull()?.toInt(),
            weatherSuitabilityScore = suitability.score,
            weatherSuitabilityText = suitability.title,
            weatherSuitabilityDesc = suitability.description,
            hourlyForecast = mapHourly(hourly, allDaily, response.timezone, now),
            dailyForecast = mapDaily(allDaily, today),
            mappingVersion = 1,
            details = mapDetails(current, daily)
        )
    }

    private fun calculateWindChill(temp: Double, windSpeed: Double): Double? {
        if (temp > 10.0 || windSpeed <= 4.8) return null
        return 13.12 + 0.6215 * temp - 11.37 * Math.pow(windSpeed, 0.16) + 0.3965 * temp * Math.pow(windSpeed, 0.16)
    }

    data class SuitabilityResult(val score: Int, val title: String, val description: String)

    private fun calculateSuitability(current: CurrentWeatherDto?, daily: DailyDto?): SuitabilityResult {
        if (current == null) return SuitabilityResult(-1, "Veri yok", "Hava durumu verilerine ulaşılamıyor.")
        var score = 100
        val warnings = mutableListOf<String>()

        val precipProb = daily?.precipProbMax?.firstOrNull()?.takeIf { it in 0..100 }?.toDouble() ?: 0.0
        if ((current.precipitation ?: 0.0) > 0.1 || precipProb > 40.0) {
            score -= 30
            warnings.add("yağmur riski")
        }
        if ((current.windSpeed ?: 0.0) > 25.0) {
            score -= 20
            warnings.add("güçlü rüzgar")
        }
        val uv = listOfNotNull(current.uvIndex, daily?.uvIndexMax?.firstOrNull()?.takeIf { it.isFinite() && it >= 0 }).maxOrNull() ?: 0.0
        if (uv > 7.0) {
            score -= 15
            warnings.add("yüksek UV")
        }
        val visibility = current?.visibility
        if (visibility != null && visibility < 3000.0) {
            score -= 20
            warnings.add("düşük görüş")
        }
        val temp = current.temperature ?: 0.0
        if (temp > 35.0 || temp < 0.0) {
            score -= 10
            warnings.add("ekstrem sıcaklık")
        }

        val complete = current.weatherCode?.let { WeatherUtils.isKnownCode(it) } == true && current.temperature != null && current.windSpeed != null &&
            current.visibility != null && (current.uvIndex != null || daily?.uvIndexMax?.firstOrNull()?.let { it.isFinite() && it >= 0 } == true) &&
            daily?.precipProbMax?.firstOrNull()?.let { it in 0..100 } == true
        if (!complete) {
            val knownRisks = if (warnings.isEmpty()) "" else " Mevcut verilerde ${warnings.joinToString(", ")} var."
            return SuitabilityResult(-1, "Değerlendirme için veri eksik",
                "Eksik ölçümler nedeniyle açık hava uygunluğu değerlendirilemiyor.$knownRisks")
        }
        val title = when {
            score > 80 -> "Açık hava için uygun"
            score > 60 -> "Yürüyüş için uygun"
            score > 40 -> "Seyahat için dikkatli ol"
            else -> "Dışarı çıkmak için elverişsiz"
        }

        val description = if (warnings.isEmpty()) {
            "Hava koşulları şu an oldukça ideal görünüyor."
        } else {
            "Şu an ${warnings.joinToString(", ")} nedeniyle dikkatli olmalısınız."
        }

        return SuitabilityResult(score, title, description)
    }

    private fun getWindDirectionLabel(deg: Int): String {
        val directions = listOf("K", "KD", "D", "GD", "G", "GB", "B", "KB")
        return directions[((deg + 22.5) / 45).toInt() % 8]
    }

    private fun mapHourly(hourly: HourlyDto?, daily: DailyDto?, timezone: String, now: java.time.Instant): List<HourlyWeather> {
        if (hourly == null) return emptyList()
        val zone = try { java.time.ZoneId.of(timezone) } catch (_: Exception) { java.time.ZoneOffset.UTC }
        val currentHourStr = now.atZone(zone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00", Locale.US))

        val sunTimesByDate = mutableMapOf<String, Pair<LocalTime, LocalTime>>()
        if (daily != null) {
            val uniqueDates = daily.time.groupingBy { it }.eachCount()
            for (i in daily.time.indices) {
                val date = daily.time[i]
                if (uniqueDates[date] != 1) continue
                val rise = ForecastSun.timeForDate(daily.sunrise.getOrNull(i), date)?.let(LocalTime::parse)
                val set = ForecastSun.timeForDate(daily.sunset.getOrNull(i), date)?.let(LocalTime::parse)
                if (rise != null && set != null && set.isAfter(rise)) sunTimesByDate[date] = rise to set
            }
        }

        val mapped = hourly.time.indices.mapNotNull { i ->
            val code = hourly.weatherCode.getOrNull(i) ?: return@mapNotNull null
            val temperature = hourly.temperature.getOrNull(i) ?: return@mapNotNull null
            if (!temperature.isFinite()) return@mapNotNull null
            val fullTime = hourly.time[i]
            val parts = fullTime.split("T")
            if (parts.size != 2) return@mapNotNull null
            val datePart = parts[0]
            val hourLabelRaw = parts[1]

            val d = try { LocalDate.parse(datePart) } catch (e: Exception) { return@mapNotNull null }
            val hourLabel = if (hourLabelRaw == "00:00") {
                d.format(DateTimeFormatter.ofPattern("d MMM", Locale("tr")))
            } else {
                hourLabelRaw
            }
            val timeObj = try {
                LocalTime.parse(hourLabelRaw)
            } catch (e: Exception) {
                return@mapNotNull null
            }

            val sunTimes = sunTimesByDate[datePart]
            val isDay: Boolean? = if (sunTimes != null) {
                !timeObj.isBefore(sunTimes.first) && timeObj.isBefore(sunTimes.second)
            } else {
                null
            }

            HourlyWeather(
                time = hourLabel,
                fullTime = fullTime,
                iconName = getWeatherIconName(code),
                condition = getWeatherCondition(code),
                weatherCode = code,
                isDay = isDay,
                temp = "${temperature.roundToInt()}°",
                precipProb = hourly.precipitationProbability?.getOrNull(i)?.takeIf { it in 0..100 }?.let { "$it%" },
                precipitationProbability = hourly.precipitationProbability?.getOrNull(i)?.takeIf { it in 0..100 },
                isSelected = fullTime == currentHourStr
            )
        }
        return uniqueForecastEntries(mapped) { LocalDateTime.parse(it.fullTime) }
    }

    private fun mapDaily(daily: DailyDto?, today: LocalDate): List<DailyForecast> {
        if (daily == null) return emptyList()
        val mapped = daily.time.mapIndexedNotNull { index, time ->
            val code = daily.weatherCode.getOrNull(index) ?: return@mapIndexedNotNull null
            val min = daily.tempMin.getOrNull(index) ?: return@mapIndexedNotNull null
            val max = daily.tempMax.getOrNull(index) ?: return@mapIndexedNotNull null
            if (!min.isFinite() || !max.isFinite() || min > max) return@mapIndexedNotNull null
            try { LocalDate.parse(time) } catch (_: Exception) { return@mapIndexedNotNull null }
            val (rise, set) = ForecastSun.validTimes(ForecastSun.timeForDate(daily.sunrise.getOrNull(index), time),
                ForecastSun.timeForDate(daily.sunset.getOrNull(index), time))
            DailyForecast(
                day = getDayName(time),
                date = time,
                iconName = getWeatherIconName(code),
                weatherCode = code,
                minTemp = min.roundToInt(),
                maxTemp = max.roundToInt(),
                precipitationProbability = daily.precipProbMax?.getOrNull(index)?.takeIf { it in 0..100 },
                isToday = time == today.toString(),
                sunriseTime = rise,
                sunsetTime = set
            )
        }
        return uniqueForecastEntries(mapped) { LocalDate.parse(it.date) }
    }

    private fun getDayName(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = inputFormat.parse(dateStr)
            val outputFormat = SimpleDateFormat("d MMMM EEEE", Locale("tr", "TR"))
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun mapDetails(current: CurrentWeatherDto?, daily: DailyDto?): List<WeatherDetailData> {
        val precipProb = daily?.precipProbMax?.firstOrNull()?.takeIf { it in 0..100 }
        val humidity = current?.humidity?.takeIf { it in 0..100 }
        val uv = (current?.uvIndex ?: daily?.uvIndexMax?.firstOrNull())?.takeIf { it.isFinite() && it >= 0 }
        val visibility = current?.visibility?.takeIf { it.isFinite() && it >= 0 }
        val cloudCover = current?.cloudCover?.takeIf { it in 0..100 }
        val windGust = current?.windGusts?.takeIf { it.isFinite() && it >= 0 }
        val airTemp = current?.temperature?.takeIf { it.isFinite() }
        val windSpd = current?.windSpeed?.takeIf { it.isFinite() && it >= 0 }
        val windChill = if (airTemp != null && windSpd != null) calculateWindChill(airTemp, windSpd) else null
        val unavailable = "Veri yok"

        val isSnow = (current?.weatherCode ?: 0) in 71..77 || (current?.weatherCode ?: 0) in 85..86
        val precipLabel = if (precipProb == null) unavailable else if (isSnow) "Kar olasılığı: %$precipProb" else "Yağmur olasılığı: %$precipProb"

        return listOf(
            WeatherDetailData("Hissedilen", current?.apparentTemperature?.takeIf { it.isFinite() }?.let { "${it.roundToInt()}°" } ?: unavailable, "Rüzgar etkisi dahil", "Thermostat", "#FB7185"),
            WeatherDetailData("Rüzgar Soğutma", if (airTemp == null || windSpd == null) unavailable else windChill?.let { "${it.roundToInt()}°" } ?: "Düşük", if (airTemp == null || windSpd == null) unavailable else if (windChill != null) "Rüzgar etkisi dahil" else "Rüzgar etkisi düşük", "AcUnit", "#38BDF8"),
            WeatherDetailData("Yağış Olasılığı", precipProb?.let { WeatherUtils.formatRainProbability(it) } ?: unavailable, precipLabel, "WaterDrop", "#60A5FA"),
            WeatherDetailData("Yağış Miktarı", current?.precipitation?.takeIf { it.isFinite() && it >= 0 }?.let { "$it mm" } ?: unavailable, "Son 1 saat", "Umbrella", "#0EA5E9"),
            WeatherDetailData("Rüzgar", windSpd?.let { "${it.roundToInt()} km/sa" } ?: unavailable, "Anlık hız", "Air", "#34D399"),
            WeatherDetailData("Rüzgar Hamlesi", windGust?.let { "${it.roundToInt()} km/sa" } ?: unavailable, "Maksimum hız", "Cyclone", "#10B981"),
            WeatherDetailData("UV İndeksi", uv?.toInt()?.toString() ?: unavailable, uv?.let { getUVDescription(it) } ?: unavailable, "Sun", "#FBBF24", uv?.let { (it.toFloat() / 12f).coerceIn(0f, 1f) }),
            WeatherDetailData("Bulutluluk", cloudCover?.let { WeatherUtils.formatRainProbability(it) } ?: unavailable, "Gökyüzü kapalılığı", "Cloud", "#64748B"),
            WeatherDetailData("Görüş", visibility?.let { "${(it / 1000).toInt()} km" } ?: unavailable, visibility?.let { getVisibilityDescription(it) } ?: unavailable, "Visibility", "#10B981"),
            WeatherDetailData("Basınç", current?.pressure?.takeIf { it.isFinite() && it > 0 }?.let { "${it.toInt()} hPa" } ?: unavailable, "Yüzey basıncı", "Compress", "#A78BFA"),
            WeatherDetailData("Nem", humidity?.let { WeatherUtils.formatRainProbability(it) } ?: unavailable, "Bağıl nem oranı", "Opacity", "#06B6D4")
        )
    }

    private fun getUVDescription(uv: Double): String = when {
        uv <= 2 -> "Düşük risk"
        uv <= 5 -> "Orta risk"
        uv <= 7 -> "Yüksek risk"
        else -> "Çok yüksek risk"
    }

    private fun getVisibilityDescription(visibility: Double): String = when {
        visibility >= 10000 -> "Mükemmel görüş"
        visibility >= 5000 -> "İyi görüş"
        visibility >= 2000 -> "Orta görüş"
        else -> "Kısıtlı görüş"
    }

    private fun getMoonPhase(date: Date): MoonPhaseInfo {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val jd = 367.0 * year - floor(7.0 * (year + floor((month + 9.0) / 12.0)) / 4.0) + floor(275.0 * month / 9.0) + day + 1721013.5
        val cycle = (jd - 2451550.1) / 29.530588853
        val phase = cycle - floor(cycle)
        val illumination = (50.0 * (1.0 - cos(2.0 * Math.PI * phase))).roundToInt()
        val label = when {
            phase < 0.0625 || phase >= 0.9375 -> "Yeni Ay"
            phase < 0.1875 -> "Hilal"
            phase < 0.3125 -> "İlk Dördün"
            phase < 0.4375 -> "Şişen Ay"
            phase < 0.5625 -> "Dolunay"
            phase < 0.6875 -> "Küçülen Ay"
            phase < 0.8125 -> "Son Dördün"
            else -> "Eski Hilal"
        }
        return MoonPhaseInfo(label, illumination)
    }

    fun getMoonAndSunData(daily: DailyDto?): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        daily?.sunrise?.firstOrNull()?.let { list.add("Güneş Doğuşu" to it.substringAfter("T")) }
        daily?.sunset?.firstOrNull()?.let { list.add("Güneş Batışı" to it.substringAfter("T")) }
        val moon = getMoonPhase(Date())
        list.add("Ay Fazı" to moon.label)
        list.add("Aydınlık" to WeatherUtils.formatRainProbability(moon.illumination))
        return list
    }

    private data class MoonPhaseInfo(val label: String, val illumination: Int)

    fun getWeatherIconName(code: Int): String = when (code) {
        0, 1 -> "Sun"
        2, 3, 45, 48 -> "Cloudy"
        51, 53, 55, 61, 63, 65 -> "Rain"
        71, 73, 75, 77, 85, 86 -> "Snow"
        80, 81, 82, 95, 96, 99 -> "Thunderstorm"
        else -> "Cloudy"
    }

    fun getIconFromName(name: String): ImageVector = when (name) {
        "Sun", "WbSunny" -> Icons.Rounded.WbSunny
        "Cloudy", "Cloud" -> Icons.Rounded.Cloud
        "Rain" -> Icons.Rounded.WaterDrop
        "Snow", "AcUnit" -> Icons.Rounded.AcUnit
        "Thunderstorm" -> Icons.Rounded.Thunderstorm
        "Thermostat" -> Icons.Rounded.Thermostat
        "Air" -> Icons.Rounded.Air
        "Brightness3" -> Icons.Rounded.Brightness3
        "Compress" -> Icons.Rounded.Compress
        "WbTwilight" -> Icons.Rounded.WbTwilight
        "Visibility" -> Icons.Rounded.Visibility
        "WaterDrop" -> Icons.Rounded.WaterDrop
        "Umbrella" -> Icons.Rounded.Umbrella
        "Cyclone" -> Icons.Rounded.Cyclone
        "Opacity" -> Icons.Rounded.Opacity
        else -> Icons.Rounded.Cloud
    }

    fun getWeatherCondition(code: Int): String {
        return WeatherUtils.getWeatherDisplayName(
            weatherCode = code,
            currentTime = LocalDateTime.now(),
            sunrise = null, // Default behavior
            sunset = null
        )
    }

    /**
     * UI ekranında görünecek condition metnini günün saatine göre normalize eder.
     * Gece 22:00'da "Güneşli" yerine "Açık Gece" yazmasını sağlar.
     */
    fun getDisplayCondition(code: Int, phase: DayPhase, sunrise: LocalTime? = null, sunset: LocalTime? = null, time: LocalDateTime? = null): String {
        return WeatherUtils.getWeatherDisplayName(
            weatherCode = code,
            currentTime = time ?: LocalDateTime.now(),
            sunrise = sunrise,
            sunset = sunset
        )
    }

    fun getDayPhase(now: LocalDateTime, sunrise: LocalTime? = null, sunset: LocalTime? = null): DayPhase {
        val current = now.toLocalTime()
        if (sunrise == null || sunset == null) {
            return if (current.hour in 6..19) DayPhase.DAY else DayPhase.NIGHT
        }

        return when {
            // MORNING: sunrise ile sunrise + 2 saat arası
            !current.isBefore(sunrise) && current.isBefore(sunrise.plusHours(2)) -> DayPhase.MORNING
            // EVENING: sunset - 2 saat ile sunset + 1 saat arası
            !current.isBefore(sunset.minusHours(2)) && current.isBefore(sunset.plusHours(1)) -> DayPhase.EVENING
            // DAY: MORNING sonrası ve EVENING öncesi
            !current.isBefore(sunrise.plusHours(2)) && current.isBefore(sunset.minusHours(2)) -> DayPhase.DAY
            // NIGHT: Diğer tüm durumlar
            else -> DayPhase.NIGHT
        }
    }

    fun mapWeatherCodeToCondition(code: Int, isDay: Boolean? = null): WeatherCondition {
        return when (code) {
            0 -> when (isDay) {
                true -> WeatherCondition.Clear
                false -> WeatherCondition.NightClear
                else -> WeatherCondition.Cloudy
            }
            1 -> when (isDay) {
                true -> WeatherCondition.MostlySunny
                false -> WeatherCondition.NightClear
                else -> WeatherCondition.Cloudy
            }
            2 -> WeatherCondition.PartlyCloudy
            3 -> WeatherCondition.Cloudy
            45, 48 -> WeatherCondition.Fog
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> WeatherCondition.Rain
            71, 73, 75, 77, 85, 86 -> WeatherCondition.Snow
            95, 96, 99 -> WeatherCondition.Thunderstorm
            else -> WeatherCondition.Cloudy
        }
    }
}
