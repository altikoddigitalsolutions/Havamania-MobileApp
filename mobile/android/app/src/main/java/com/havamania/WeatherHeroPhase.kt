package com.havamania

import java.time.LocalTime
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*

internal fun heroDayPhase(time: LocalTime?, rise: LocalTime?, set: LocalTime?, isDay: Boolean?): DayPhase? {
    val validSolar = rise != null && set != null && set.isAfter(rise)
    val day = isDay ?: if (time != null && rise != null && set != null && validSolar)
        !time.isBefore(rise) && time.isBefore(set) else return null
    if (!day) return DayPhase.NIGHT
    if (time == null || rise == null || set == null || !validSolar || time.isBefore(rise) || !time.isBefore(set)) return DayPhase.DAY
    return when {
        java.time.Duration.between(rise, time).toMinutes() < 120 -> DayPhase.MORNING
        java.time.Duration.between(time, set).toMinutes() < 120 -> DayPhase.EVENING
        else -> DayPhase.DAY
    }
}

internal fun heroDaylightProgress(time: LocalTime?, rise: LocalTime?, set: LocalTime?): Float? {
    if (time == null || rise == null || set == null || !set.isAfter(rise) || time.isBefore(rise) || !time.isBefore(set)) return null
    return (java.time.Duration.between(rise, time).seconds.toFloat() / java.time.Duration.between(rise, set).seconds.toFloat()).coerceIn(0f, 1f)
}

internal fun heroWeatherTheme(code: Int): WeatherThemeType = when (code) {
    0 -> WeatherThemeType.SUNNY
    1 -> WeatherThemeType.MOSTLY_SUNNY
    2 -> WeatherThemeType.PARTLY_CLOUDY
    3 -> WeatherThemeType.CLOUDY
    45, 48 -> WeatherThemeType.FOGGY
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherThemeType.RAINY
    71, 73, 75, 77, 85, 86 -> WeatherThemeType.SNOWY
    95, 96, 99 -> WeatherThemeType.STORMY
    else -> WeatherThemeType.CLOUDY
}

internal fun neutralHeroTheme(code: Int): HeroThemeSpec {
    val theme = heroWeatherTheme(code)
    val known = WeatherUtils.isKnownCode(code)
    val icon = if (!known) Icons.Rounded.HelpOutline else when (theme) {
        WeatherThemeType.SUNNY, WeatherThemeType.MOSTLY_SUNNY, WeatherThemeType.PARTLY_CLOUDY -> Icons.Rounded.Thermostat
        WeatherThemeType.RAINY -> Icons.Rounded.WaterDrop
        WeatherThemeType.SNOWY -> Icons.Rounded.AcUnit
        WeatherThemeType.STORMY -> Icons.Rounded.Thunderstorm
        else -> Icons.Rounded.Cloud
    }
    val effect = when (theme) {
        WeatherThemeType.RAINY -> VisualEffectType.RAIN
        WeatherThemeType.SNOWY -> VisualEffectType.SNOW
        WeatherThemeType.STORMY -> VisualEffectType.THUNDER
        WeatherThemeType.FOGGY -> VisualEffectType.FOG
        else -> VisualEffectType.NONE
    }
    return HeroThemeSpec(theme, DayPhase.DAY, listOf(Color(0xFF334155), Color(0xFF1E293B)),
        Color.White, Color(0xFF7DD3FC), icon, effect, WeatherUtils.describeWeatherPhase(code, null),
        if (known && theme !in setOf(WeatherThemeType.SUNNY, WeatherThemeType.MOSTLY_SUNNY)) 3 else 0,
        Color(0xFFCBD5E1).copy(alpha = 0.6f), Offset(0.8f, 0.25f), 1f, 0.1f, phaseKnown = false)
}

/** Functional icons share the readable foreground on bright daytime palettes. */
internal fun heroForegroundIconColor(spec: HeroThemeSpec): Color =
    if (spec.phaseKnown && spec.dayPart in setOf(DayPhase.MORNING, DayPhase.DAY) &&
        spec.themeType in setOf(WeatherThemeType.SUNNY, WeatherThemeType.MOSTLY_SUNNY, WeatherThemeType.PARTLY_CLOUDY, WeatherThemeType.SNOWY, WeatherThemeType.FOGGY))
        spec.textColor else spec.accentColor
