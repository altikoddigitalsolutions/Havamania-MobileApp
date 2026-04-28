package com.havamania

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt

object WeatherMapper {

    fun mapToDomain(response: OpenMeteoResponse, cityName: String): WeatherData {
        val current = response.current
        val daily = response.daily
        val hourly = response.hourly

        val tempMax = daily?.tempMax?.firstOrNull()?.toInt() ?: current?.temperature?.toInt() ?: 17
        val tempMin = daily?.tempMin?.firstOrNull()?.toInt() ?: (current?.temperature?.toInt()?.minus(5)) ?: 3
        val feelsLike = current?.apparentTemperature?.toInt() ?: current?.temperature?.toInt() ?: 8

        return WeatherData(
            cityName = cityName,
            temperature = "${current?.temperature?.toInt() ?: 0}°",
            condition = getWeatherCondition(current?.weatherCode ?: 0),
            high = "${tempMax}°",
            low = "${tempMin}°",
            feelsLike = "${feelsLike}°",
            hourlyForecast = mapHourly(hourly),
            dailyForecast = mapDaily(daily),
            details = mapDetails(current, daily)
        )
    }

    private fun mapHourly(hourly: HourlyDto?): List<HourlyForecastData> {
        if (hourly == null) return emptyList()
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        return hourly.time.indices
            .filter { i -> i in currentHour..24 }
            .map { i ->
                val timeStr = hourly.time[i]
                var hourLabel = timeStr.split("T").last()
                if (hourLabel == "00:00" && i > currentHour) hourLabel = "24:00"

                HourlyForecastData(
                    time = hourLabel,
                    iconName = getWeatherIconName(hourly.weatherCode[i]),
                    temp = "${hourly.temperature[i].toInt()}°",
                    precipProb = hourly.precipitationProbability?.get(i)?.let { "$it%" },
                    isSelected = i == currentHour
                )
            }
    }

    private fun mapDaily(daily: DailyDto?): List<DailyForecastData> {
        if (daily == null) return emptyList()
        return daily.time.mapIndexed { index, time ->
            DailyForecastData(
                day = getDayName(time),
                iconName = getWeatherIconName(daily.weatherCode[index]),
                minTemp = daily.tempMin[index].toInt(),
                maxTemp = daily.tempMax[index].toInt(),
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
        val details = mutableListOf(
            WeatherDetailData("Hissedilen", "${current?.apparentTemperature?.toInt() ?: 0}°", "Rüzgar etkisi dahil", "Thermostat", "#FB7185"),
            WeatherDetailData("Nem", "%${current?.humidity ?: 0}", "Bağıl nem oranı", "WaterDrop", "#38BDF8", (current?.humidity ?: 0) / 100f),
            WeatherDetailData("Rüzgar", "${current?.windSpeed?.toInt() ?: 0} km/s", "Esinti hızı", "Air", "#34D399"),
            WeatherDetailData("Basınç", "${current?.pressure?.toInt() ?: 1013} hPa", "Yüzey basıncı", "Compress", "#A78BFA"),
            WeatherDetailData("UV İndeksi", "${daily?.uvIndexMax?.firstOrNull()?.toInt() ?: 0}", "Maksimum seviye", "Sun", "#FBBF24", (daily?.uvIndexMax?.firstOrNull() ?: 0.0).toFloat() / 12f),
            WeatherDetailData("Görüş", "${(current?.visibility?.div(1000))?.toInt() ?: 10} km", "Görüş mesafesi", "Visibility", "#10B981")
        )
        return details
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
        list.add("Aydınlık" to "%${moon.illumination}")
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
        "Sun" -> Icons.Rounded.WbSunny
        "Cloudy" -> Icons.Rounded.Cloud
        "Rain" -> Icons.Rounded.WaterDrop
        "Snow" -> Icons.Rounded.AcUnit
        "Thunderstorm" -> Icons.Rounded.Thunderstorm
        "Thermostat" -> Icons.Rounded.Thermostat
        "Air" -> Icons.Rounded.Air
        "Brightness3" -> Icons.Rounded.Brightness3
        "Compress" -> Icons.Rounded.Compress
        "WbTwilight" -> Icons.Rounded.WbTwilight
        "Visibility" -> Icons.Rounded.Visibility
        else -> Icons.Rounded.Cloud
    }

    private fun getWeatherCondition(code: Int): String = when (code) {
        0 -> "Güneşli"; 1 -> "Çoğunlukla Güneşli"; 2 -> "Parçalı Bulutlu"
        3 -> "Bulutlu"; 45, 48 -> "Sisli"; 51, 53, 55 -> "Hafif Yağmurlu"
        61, 63, 65 -> "Yağmurlu"; 71, 73, 75 -> "Karlı"
        80, 81, 82 -> "Sağanak Yağış"; 95, 96, 99 -> "Fırtınalı"; else -> "Bulutlu"
    }
}
