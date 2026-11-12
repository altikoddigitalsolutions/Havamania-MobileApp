package com.havamania

/** Old serialized derived scores cannot be trusted after mapping rules change. */
internal fun normalizeCachedWeatherAssessment(data: WeatherData): WeatherData =
    if (data.mappingVersion >= 1) data else data.copy(
        weatherSuitabilityScore = -1,
        weatherSuitabilityText = "Değerlendirme güncellenmeli",
        weatherSuitabilityDesc = "Kayıtlı hava ölçümleri korunuyor. Uygunluk değerlendirmesi için veriyi yenileyin."
    )

internal fun cachedWeatherSameLocalDay(data: WeatherData, now: java.time.Instant): Boolean =
    ForecastTime.localNow(data.timezone, java.time.Instant.ofEpochMilli(data.timestamp)).toLocalDate() ==
        ForecastTime.localNow(data.timezone, now).toLocalDate()

internal fun presentCachedWeather(data: WeatherData, now: java.time.Instant): WeatherData {
    val normalized = normalizeCachedWeatherAssessment(data)
    val sameDay = cachedWeatherSameLocalDay(data, now)
    if (sameDay && !data.isStale) return normalized
    return normalized.copy(
        high = if (sameDay) normalized.high else "--",
        low = if (sameDay) normalized.low else "--",
        precipitationProbability = if (sameDay) normalized.precipitationProbability else null,
        uvIndex = if (sameDay) normalized.uvIndex else null,
        sunriseTime = if (sameDay) normalized.sunriseTime else null,
        sunsetTime = if (sameDay) normalized.sunsetTime else null,
        solarNoon = if (sameDay) normalized.solarNoon else null,
        details = if (sameDay) normalized.details else emptyList(),
        weatherSuitabilityScore = -1,
        weatherSuitabilityText = "Değerlendirme güncellenmeli",
        weatherSuitabilityDesc = "Kayıtlı hava verileri güncel değil. Güncel koşullar için veriyi yenileyin.",
        isStale = true
    )
}

internal fun ageVisibleWeather(data: WeatherData, now: java.time.Instant): WeatherData =
    presentCachedWeather(data.copy(isStale = data.isStale || !weatherCacheIsFresh(data.timestamp, now.toEpochMilli())), now)
