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

    fun mapToDomain(response: OpenMeteoResponse, cityName: String, districtName: String? = null): WeatherData {
        val current = response.current
        val daily = response.daily
        val hourly = response.hourly

        val tempMax = daily?.tempMax?.firstOrNull()?.roundToInt() ?: 0
        val tempMin = daily?.tempMin?.firstOrNull()?.roundToInt() ?: 0
        val feelsLike = current?.apparentTemperature?.roundToInt() ?: 0

        val windDir = current?.windDirection?.toInt() ?: 0
        val windDirLabel = getWindDirectionLabel(windDir)

        val airTemp = current?.temperature ?: 0.0
        val windSpd = current?.windSpeed ?: 0.0
        val windChillValue = calculateWindChill(airTemp, windSpd)

        val suitability = calculateSuitability(current, daily)

        // Sunrise/Sunset/SolarNoon
        val sunriseISO = daily?.sunrise?.firstOrNull()
        val sunsetISO = daily?.sunset?.firstOrNull()
        val sunrise = sunriseISO?.split("T")?.last()
        val sunset = sunsetISO?.split("T")?.last()
        var solarNoon: String? = null

        // Solar noon fallback calculation
        if (sunrise != null && sunset != null) {
            solarNoon = calculateSolarNoon(sunrise, sunset)
        }

        val sunriseLocalTime = try { LocalTime.parse(sunrise) } catch (e: Exception) { LocalTime.of(6, 30) }
        val sunsetLocalTime = try { LocalTime.parse(sunset) } catch (e: Exception) { LocalTime.of(19, 30) }

        return WeatherData(
            cityName = cityName,
            districtName = districtName,
            latitude = response.latitude,
            longitude = response.longitude,
            temperature = "${current?.temperature?.roundToInt() ?: 0}°",
            condition = getWeatherCondition(current?.weatherCode ?: 0),
            weatherCode = current?.weatherCode ?: 0,
            isDay = current?.isDay == 1,
            high = "${tempMax}°",
            low = "${tempMin}°",
            feelsLike = "${feelsLike}°",
            sunriseTime = if (sunrise.isNullOrEmpty()) null else sunrise,
            sunsetTime = if (sunset.isNullOrEmpty()) null else sunset,
            timezone = response.timezone,
            solarNoon = if (solarNoon.isNullOrEmpty()) null else solarNoon,
            windSpeed = current?.windSpeed ?: 0.0,
            windGust = current?.windGusts ?: 0.0,
            windDirectionDegrees = current?.windDirection?.toInt() ?: 0,
            windDirectionLabel = windDirLabel,
            windChill = windChillValue,
            dewPoint = current?.dewPoint ?: 0.0,
            precipitationProbability = daily?.precipProbMax?.firstOrNull() ?: 0,
            precipitationAmount = current?.precipitation ?: 0.0,
            cloudCover = current?.cloudCover ?: 0,
            visibilityKm = (current?.visibility ?: 10000.0) / 1000.0,
            humidity = current?.humidity ?: 0,
            pressure = current?.pressure?.toInt() ?: 1013,
            uvIndex = current?.uvIndex?.toInt() ?: daily?.uvIndexMax?.firstOrNull()?.toInt() ?: 0,
            weatherSuitabilityScore = suitability.score,
            weatherSuitabilityText = suitability.title,
            weatherSuitabilityDesc = suitability.description,
            hourlyForecast = mapHourly(hourly, sunriseLocalTime, sunsetLocalTime),
            dailyForecast = mapDaily(daily),
            details = mapDetails(current, daily)
        )
    }

    private fun calculateSolarNoon(sunrise: String, sunset: String): String? {
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.US)
            val riseDate = sdf.parse(sunrise)
            val setDate = sdf.parse(sunset)
            if (riseDate != null && setDate != null) {
                val midTime = (riseDate.time + setDate.time) / 2
                sdf.format(Date(midTime))
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateWindChill(temp: Double, windSpeed: Double): Double? {
        if (temp > 10.0 || windSpeed <= 4.8) return null
        return 13.12 + 0.6215 * temp - 11.37 * Math.pow(windSpeed, 0.16) + 0.3965 * temp * Math.pow(windSpeed, 0.16)
    }

    data class SuitabilityResult(val score: Int, val title: String, val description: String)

    private fun calculateSuitability(current: CurrentWeatherDto?, daily: DailyDto?): SuitabilityResult {
        if (current == null) return SuitabilityResult(100, "Veri yok", "Hava durumu verilerine ulaşılamıyor.")
        var score = 100
        val warnings = mutableListOf<String>()

        val precipProb = daily?.precipProbMax?.firstOrNull()?.toDouble() ?: 0.0
        if ((current.precipitation ?: 0.0) > 0.1 || precipProb > 40.0) {
            score -= 30
            warnings.add("yağmur riski")
        }
        if ((current.windSpeed ?: 0.0) > 25.0) {
            score -= 20
            warnings.add("güçlü rüzgar")
        }
        val uv = daily?.uvIndexMax?.firstOrNull() ?: 0.0
        if (uv > 7.0) {
            score -= 15
            warnings.add("yüksek UV")
        }
        val visibility = current.visibility ?: 10000.0
        if (visibility < 3000.0) {
            score -= 20
            warnings.add("düşük görüş")
        }
        val temp = current.temperature ?: 0.0
        if (temp > 35.0 || temp < 0.0) {
            score -= 10
            warnings.add("ekstrem sıcaklık")
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

    private fun mapHourly(hourly: HourlyDto?, sunrise: LocalTime, sunset: LocalTime): List<HourlyWeather> {
        if (hourly == null) return emptyList()
        val calendar = Calendar.getInstance()
        val currentHourStr = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.US).format(calendar.time)

        return hourly.time.indices.map { i ->
            val fullTime = hourly.time[i]
            val hourLabelRaw = fullTime.split("T").last()
            val hourLabel = if (hourLabelRaw == "00:00") {
                val d = try { LocalDate.parse(fullTime.split("T").first()) } catch (e: Exception) { LocalDate.now() }
                d.format(DateTimeFormatter.ofPattern("d MMM", Locale("tr")))
            } else {
                hourLabelRaw
            }
            val timeObj = try {
                LocalTime.parse(hourLabelRaw)
            } catch (e: Exception) {
                try {
                    LocalTime.of(hourLabelRaw.split(":").first().toInt(), 0)
                } catch (e2: Exception) {
                    LocalTime.of(0, 0)
                }
            }

            HourlyWeather(
                time = hourLabel,
                fullTime = fullTime,
                iconName = getWeatherIconName(hourly.weatherCode[i]),
                condition = getWeatherCondition(hourly.weatherCode[i]),
                weatherCode = hourly.weatherCode[i],
                isDay = !timeObj.isBefore(sunrise) && timeObj.isBefore(sunset),
                temp = "${hourly.temperature[i].toInt()}°",
                precipProb = hourly.precipitationProbability?.get(i)?.let { "$it%" },
                precipitationProbability = hourly.precipitationProbability?.get(i),
                isSelected = fullTime == currentHourStr
            )
        }
    }

    private fun mapDaily(daily: DailyDto?): List<DailyForecast> {
        if (daily == null) return emptyList()
        return daily.time.mapIndexed { index, time ->
            DailyForecast(
                day = getDayName(time),
                date = time,
                iconName = getWeatherIconName(daily.weatherCode[index]),
                weatherCode = daily.weatherCode[index],
                minTemp = daily.tempMin[index].toInt(),
                maxTemp = daily.tempMax[index].toInt(),
                precipitationProbability = daily.precipProbMax?.getOrNull(index),
                isToday = index == 0
            )
        }
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
        val precipProb = daily?.precipProbMax?.firstOrNull() ?: 0
        val humidity = current?.humidity ?: 0
        val uv = daily?.uvIndexMax?.firstOrNull() ?: 0.0
        val visibility = current?.visibility ?: 10000.0
        val cloudCover = current?.cloudCover ?: 0
        val windGust = current?.windGusts ?: 0.0
        val airTemp = current?.temperature ?: 0.0
        val windSpd = current?.windSpeed ?: 0.0
        val windChill = calculateWindChill(airTemp, windSpd)

        val isSnow = (current?.weatherCode ?: 0) in 71..77 || (current?.weatherCode ?: 0) in 85..86
        val precipLabel = if (isSnow) "Kar olasılığı: %$precipProb" else "Yağmur olasılığı: %$precipProb"

        return listOf(
            WeatherDetailData("Hissedilen", "${current?.apparentTemperature?.roundToInt() ?: 0}°", "Rüzgar etkisi dahil", "Thermostat", "#FB7185"),
            WeatherDetailData("Rüzgar Soğutma", if (windChill != null) "${windChill.roundToInt()}°" else "Düşük", if (windChill != null) "Rüzgar etkisi dahil" else "Rüzgar etkisi düşük", "AcUnit", "#38BDF8"),
            WeatherDetailData("Yağış Olasılığı", WeatherUtils.formatRainProbability(precipProb), precipLabel, "WaterDrop", "#60A5FA"),
            WeatherDetailData("Yağış Miktarı", "${current?.precipitation ?: 0.0} mm", "Son 1 saat", "Umbrella", "#0EA5E9"),
            WeatherDetailData("Rüzgar", "${windSpd.roundToInt()} km/sa", "Anlık hız", "Air", "#34D399"),
            WeatherDetailData("Rüzgar Hamlesi", "${windGust.roundToInt()} km/sa", "Maksimum hız", "Cyclone", "#10B981"),
            WeatherDetailData("UV İndeksi", "${uv.toInt()}", getUVDescription(uv), "Sun", "#FBBF24", uv.toFloat() / 12f),
            WeatherDetailData("Bulutluluk", WeatherUtils.formatRainProbability(cloudCover), "Gökyüzü kapalılığı", "Cloud", "#64748B"),
            WeatherDetailData("Görüş", "${(visibility / 1000).toInt()} km", getVisibilityDescription(visibility), "Visibility", "#10B981"),
            WeatherDetailData("Basınç", "${current?.pressure?.toInt() ?: 1013} hPa", "Yüzey basıncı", "Compress", "#A78BFA"),
            WeatherDetailData("Nem", WeatherUtils.formatRainProbability(humidity), "Bağıl nem oranı", "Opacity", "#06B6D4")
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
        if (daily != null && daily.sunrise.isNotEmpty()) {
            list.add("Güneş Doğuşu" to daily.sunrise.first().split("T").last())
            list.add("Güneş Batışı" to daily.sunset.first().split("T").last())
        }
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

    fun getDayPhase(now: LocalDateTime, sunrise: LocalTime, sunset: LocalTime): DayPhase {
        val current = now.toLocalTime()

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

    fun mapWeatherCodeToCondition(code: Int, isDay: Boolean): WeatherCondition {
        return when (code) {
            0 -> if (isDay) WeatherCondition.Clear else WeatherCondition.NightClear
            1 -> if (isDay) WeatherCondition.MostlySunny else WeatherCondition.NightClear
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
