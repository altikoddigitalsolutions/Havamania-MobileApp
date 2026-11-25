package com.havamania

/** Provider solar evidence belongs to the sampled location/hour, never the device clock. */
internal fun routeWeatherDisplayName(code: Int, isDay: Boolean?): String =
    if (WeatherUtils.isKnownCode(code)) WeatherUtils.describeWeatherPhase(code, isDay) else "Hava tahmini"
internal fun routeWeatherEmoji(code: Int, isDay: Boolean?): String = when {
    !WeatherUtils.isKnownCode(code) -> "🌡️"
    code == 0 && isDay == false -> "🌙"
    code == 0 && isDay == null -> "🌡️"
    code in 1..2 && isDay != true -> "☁️"
    else -> WeatherUtils.getWeatherEmoji(code)
}
