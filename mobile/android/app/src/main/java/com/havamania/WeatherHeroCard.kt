package com.havamania

import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random

enum class VisualEffectType { NONE, SUN, MOON, RAIN, SNOW, THUNDER, FOG }

enum class WeatherThemeType { SUNNY, MOSTLY_SUNNY, PARTLY_CLOUDY, CLOUDY, RAINY, SNOWY, STORMY, FOGGY }

val SineEaseInOut = Easing { f -> ((1 - Math.cos(f * Math.PI)) / 2).toFloat() }

data class HeroThemeSpec(
    val themeType: WeatherThemeType,
    val dayPart: DayPhase,
    val gradientColors: List<Color>,
    val textColor: Color,
    val accentColor: Color,
    val icon: ImageVector,
    val effectType: VisualEffectType,
    val displayTitle: String,
    val cloudDensity: Int,
    val cloudColor: Color,
    val sunMoonPosition: Offset,
    val sunMoonSize: Float, // Added for dynamic sizing
    val overlayAlpha: Float
)

/**
 * RESTORED for backward compatibility with other components
 */
data class WeatherCardVisualSpec(
    val gradientColors: List<Color>,
    val overlayAlpha: Float,
    val textColor: Color,
    val accentColor: Color,
    val mainIcon: ImageVector,
    val cloudDensity: Int,
    val effectType: VisualEffectType,
    val sunMoonPosition: Offset,
    val isDark: Boolean,
    val cloudColor: Color = Color.White
)

object WeatherStyleResolver {
    @Composable
    fun resolveSpec(condition: WeatherCondition, phase: DayPhase, theme: AppTheme): WeatherCardVisualSpec {
        val isNight = phase == DayPhase.NIGHT
        val mainIcon = when (condition) {
            is WeatherCondition.Rain -> Icons.Rounded.WaterDrop
            is WeatherCondition.Snow -> Icons.Rounded.AcUnit
            is WeatherCondition.Thunderstorm -> Icons.Rounded.Thunderstorm
            is WeatherCondition.Fog -> Icons.Rounded.FilterDrama
            is WeatherCondition.Cloudy -> Icons.Rounded.Cloud
            is WeatherCondition.PartlyCloudy -> if (isNight) Icons.Rounded.CloudQueue else Icons.Rounded.WbCloudy
            else -> if (isNight) Icons.Rounded.NightsStay else Icons.Rounded.WbSunny
        }
        return WeatherCardVisualSpec(
            gradientColors = listOf(Color(0xFF64748B), Color(0xFF475569)),
            overlayAlpha = 0.1f,
            textColor = Color.White,
            accentColor = Color(0xFF7DD3FC),
            mainIcon = mainIcon,
            cloudDensity = 2,
            effectType = VisualEffectType.NONE,
            sunMoonPosition = Offset(0.82f, 0.22f),
            isDark = true
        )
    }
}

object WeatherHeroStyleManager {
    fun resolveWeatherHeroTheme(
        weatherCode: Int,
        conditionText: String,
        selectedTime: LocalTime,
        latitude: Double,
        longitude: Double,
        sunrise: LocalTime,
        sunset: LocalTime
    ): HeroThemeSpec {
        // 1. Resolve Day Part
        val dayPart = when {
            selectedTime.isBefore(sunrise) -> DayPhase.NIGHT
            !selectedTime.isBefore(sunrise) && selectedTime.isBefore(sunrise.plusHours(2)) -> DayPhase.MORNING
            !selectedTime.isBefore(sunrise.plusHours(2)) && selectedTime.isBefore(sunset.minusHours(2)) -> DayPhase.DAY
            !selectedTime.isBefore(sunset.minusHours(2)) && selectedTime.isBefore(sunset.plusHours(1)) -> DayPhase.EVENING
            else -> DayPhase.NIGHT
        }

        // 1.1 Calculate Dynamic Sun/Moon Position and Size
        val isDaytime = !selectedTime.isBefore(sunrise) && selectedTime.isBefore(sunset)

        val sunMoonPos = if (isDaytime) {
            // progress = (currentTime - sunrise) / (sunset - sunrise)
            val totalDayMinutes = java.time.Duration.between(sunrise, sunset).toMinutes().toFloat()
            val minutesFromSunrise = java.time.Duration.between(sunrise, selectedTime).toMinutes().toFloat()
            val t = (minutesFromSunrise / totalDayMinutes).coerceIn(0f, 1f)

            val x = 0.1f + t * 0.8f
            val y = 0.8f - (Math.sin(t * Math.PI).toFloat() * 0.65f)
            Offset(x, y)
        } else {
            // Moon path (approximate or fixed)
            Offset(0.80f, 0.25f)
        }

        val sunMoonSize = if (isDaytime) {
            val totalDayMinutes = java.time.Duration.between(sunrise, sunset).toMinutes().toFloat()
            val minutesFromSunrise = java.time.Duration.between(sunrise, selectedTime).toMinutes().toFloat()
            val t = (minutesFromSunrise / totalDayMinutes).coerceIn(0f, 1f)

            // Largest around solar noon (t=0.5)
            1.0f + (Math.sin(t * Math.PI).toFloat() * 0.4f)
        } else {
            1.3f // Night moon slightly larger
        }

        // 2. Resolve Theme Type (More granular)
        val themeType = when (weatherCode) {
            0 -> WeatherThemeType.SUNNY
            1 -> WeatherThemeType.MOSTLY_SUNNY
            2 -> WeatherThemeType.PARTLY_CLOUDY
            3 -> WeatherThemeType.CLOUDY
            45, 48 -> WeatherThemeType.FOGGY
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> WeatherThemeType.RAINY
            71, 73, 75, 77, 85, 86 -> WeatherThemeType.SNOWY
            95, 96, 99 -> WeatherThemeType.STORMY
            else -> WeatherThemeType.CLOUDY
        }

        // 3. Define Palettes - Premium & Atmospheric Identity
        val gradientColors = when (themeType) {
            WeatherThemeType.SUNNY -> when (dayPart) {
                DayPhase.MORNING -> listOf(Color(0xFFBAE6FD), Color(0xFFFFE0B2), Color(0xFFFFF9C4)) // Natural Peach/Yellow/Blue
                DayPhase.DAY -> listOf(Color(0xFF38BDF8), Color(0xFFFBBF24), Color(0xFFF59E0B)) // Blue to Gold
                DayPhase.EVENING -> listOf(Color(0xFFFFAD66), Color(0xFFF97316), Color(0xFFBF360C)) // Deeper Red/Orange
                DayPhase.NIGHT -> listOf(Color(0xFF0F172A), Color(0xFF020617), Color(0xFF1E293B))
            }
            WeatherThemeType.MOSTLY_SUNNY -> when (dayPart) {
                DayPhase.NIGHT -> listOf(Color(0xFF0F172A), Color(0xFF020617), Color(0xFF1E293B))
                DayPhase.EVENING -> listOf(Color(0xFFF97316), Color(0xFFEA580C), Color(0xFF431407))
                else -> listOf(Color(0xFF38BDF8), Color(0xFF7DD3FC), Color(0xFFFCD34D))
            }
            WeatherThemeType.PARTLY_CLOUDY -> when (dayPart) {
                DayPhase.NIGHT -> listOf(Color(0xFF0F172A), Color(0xFF020617), Color(0xFF1E1B4B))
                DayPhase.EVENING -> listOf(Color(0xFFF97316), Color(0xFFE85D75), Color(0xFF431407))
                else -> listOf(Color(0xFF2DD4BF), Color(0xFFBAE6FD), Color(0xFFFDE68A))
            }
            WeatherThemeType.CLOUDY -> when (dayPart) {
                DayPhase.NIGHT -> listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E293B))
                else -> listOf(Color(0xFF64748B), Color(0xFF94A3B8), Color(0xFFB4C6D1)) // Blue-Grey depth
            }
            WeatherThemeType.RAINY -> when (dayPart) {
                DayPhase.NIGHT -> listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E3A8A))
                else -> listOf(Color(0xFF1E3A8A), Color(0xFF475569), Color(0xFF64748B)) // Moody Rainy
            }
            WeatherThemeType.SNOWY -> listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFFBAE6FD))
            WeatherThemeType.STORMY -> listOf(Color(0xFF020617), Color(0xFF1E1B4B), Color(0xFF312E81))
            WeatherThemeType.FOGGY -> when (dayPart) {
                DayPhase.NIGHT -> listOf(Color(0xFF020617), Color(0xFF1E293B), Color(0xFF475569))
                DayPhase.MORNING -> listOf(Color(0xFFB4C6D1), Color(0xFFE9D5FF), Color(0xFFFFD1D1).copy(alpha = 0.4f))
                else -> listOf(Color(0xFF94A3B8), Color(0xFFB4C6D1), Color(0xFFE9D5FF))
            }
        }

        val isDark = dayPart == DayPhase.NIGHT || dayPart == DayPhase.EVENING || themeType == WeatherThemeType.STORMY
        val isFoggyDay = themeType == WeatherThemeType.FOGGY && (dayPart == DayPhase.DAY || dayPart == DayPhase.MORNING)
        val isFoggyMorning = themeType == WeatherThemeType.FOGGY && dayPart == DayPhase.MORNING

        val textColor = when {
            isDark -> Color.White.copy(alpha = 0.98f)
            isFoggyDay -> Color(0xFF1F2937) // Darker text for fog readability
            else -> Color(0xFF0F172A)
        }

        val accentColor = when (themeType) {
            WeatherThemeType.SUNNY -> if (dayPart == DayPhase.DAY) Color(0xFFFBBF24) else Color(0xFFFFD600)
            WeatherThemeType.MOSTLY_SUNNY -> Color(0xFFFCD34D)
            WeatherThemeType.PARTLY_CLOUDY -> Color(0xFF5EEAD4)
            WeatherThemeType.RAINY -> Color(0xFF60A5FA)
            WeatherThemeType.SNOWY -> Color(0xFF38BDF8)
            WeatherThemeType.STORMY -> Color(0xFFFACC15)
            WeatherThemeType.FOGGY -> Color(0xFF64748B)
            else -> if (isDark) Color(0xFF94A3B8) else Color(0xFF334155)
        }

        val icon = when (themeType) {
            WeatherThemeType.SUNNY, WeatherThemeType.MOSTLY_SUNNY -> if (dayPart == DayPhase.NIGHT) Icons.Rounded.NightsStay else Icons.Rounded.WbSunny
            WeatherThemeType.RAINY -> Icons.Rounded.WaterDrop
            WeatherThemeType.CLOUDY, WeatherThemeType.PARTLY_CLOUDY -> Icons.Rounded.Cloud
            WeatherThemeType.SNOWY -> Icons.Rounded.AcUnit
            WeatherThemeType.STORMY -> Icons.Rounded.Thunderstorm
            WeatherThemeType.FOGGY -> Icons.Rounded.FilterDrama
        }

        val effectType = when (themeType) {
            WeatherThemeType.SUNNY, WeatherThemeType.MOSTLY_SUNNY -> if (dayPart == DayPhase.NIGHT) VisualEffectType.MOON else VisualEffectType.SUN
            WeatherThemeType.RAINY -> VisualEffectType.RAIN
            WeatherThemeType.PARTLY_CLOUDY -> if (dayPart == DayPhase.NIGHT) VisualEffectType.MOON else VisualEffectType.SUN
            WeatherThemeType.SNOWY -> VisualEffectType.SNOW
            WeatherThemeType.STORMY -> VisualEffectType.THUNDER
            WeatherThemeType.FOGGY -> VisualEffectType.FOG
            else -> VisualEffectType.NONE
        }

        val cloudDensity = when (themeType) {
            WeatherThemeType.SUNNY -> 1 // Very few
            WeatherThemeType.MOSTLY_SUNNY -> 2 // 1-2 big
            WeatherThemeType.PARTLY_CLOUDY -> 4 // 2-4
            WeatherThemeType.CLOUDY -> 8 // Significant
            WeatherThemeType.RAINY, WeatherThemeType.STORMY -> 12 // Very dense
            WeatherThemeType.FOGGY -> 3
            WeatherThemeType.SNOWY -> 6
        }

        val cloudColor = when {
            dayPart == DayPhase.EVENING -> Color(0xFFFFD6A5).copy(alpha = 0.6f)
            dayPart == DayPhase.NIGHT -> Color(0xFF94A3B8).copy(alpha = 0.45f)
            themeType == WeatherThemeType.RAINY || themeType == WeatherThemeType.STORMY -> Color(0xFF475569).copy(alpha = 0.7f)
            themeType == WeatherThemeType.CLOUDY -> Color(0xFFCBD5E1).copy(alpha = 0.75f)
            else -> Color.White.copy(alpha = 0.75f)
        }

        val displayTitle = buildWeatherTitle(weatherCode, dayPart)

        return HeroThemeSpec(
            themeType = themeType,
            dayPart = dayPart,
            gradientColors = gradientColors,
            textColor = textColor,
            accentColor = accentColor,
            icon = icon,
            effectType = effectType,
            displayTitle = displayTitle,
            cloudDensity = cloudDensity,
            cloudColor = cloudColor,
            sunMoonPosition = sunMoonPos,
            sunMoonSize = sunMoonSize,
            overlayAlpha = if (isDark) 0.15f else 0.08f
        )
    }

    private fun buildWeatherTitle(code: Int, dayPart: DayPhase): String {
        val base = when (code) {
            0 -> "Güneşli"
            1 -> "Çoğunlukla Güneşli"
            2 -> "Parçalı Bulutlu"
            3 -> "Bulutlu"
            45, 48 -> "Sisli"
            51, 53, 55 -> "Hafif Yağmurlu"
            61, 63, 65 -> "Yağmurlu"
            80, 81, 82 -> "Sağanak Yağış"
            71, 73, 75, 77, 85, 86 -> "Kar Yağışlı"
            95, 96, 99 -> "Gök Gürültülü Sağanak"
            else -> "Bulutlu"
        }

        val suffix = when (dayPart) {
            DayPhase.MORNING -> if (code <= 1) "Açık Sabah" else "Sabah"
            DayPhase.EVENING -> if (code <= 1) "Açık Akşam" else "Akşam"
            DayPhase.NIGHT -> if (code <= 1) "Açık Gece" else "Gece"
            else -> ""
        }

        return if (suffix.isEmpty() || base.contains(suffix)) base else "$base $suffix"
    }
}

@Composable
fun WeatherHeroCard(
    cityName: String,
    temperature: String,
    conditionLabel: String,
    weatherCode: Int,
    isDay: Boolean,
    high: String,
    low: String,
    feelsLike: String,
    humidity: String,
    windSpeed: String,
    uvIndex: String,
    onCityClick: () -> Unit,
    onNotificationsClick: () -> Unit = {},
    unreadCount: Int = 0,
    modifier: Modifier = Modifier,
    districtName: String? = null,
    time: LocalTime = LocalTime.now(),
    latitude: Double = 41.0082,
    longitude: Double = 28.9784,
    sunriseTime: String? = null,
    sunsetTime: String? = null,
    parallaxOffset: Float = 0f,
    themeViewModel: com.havamania.ui.theme.ThemeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val sunrise = remember(sunriseTime) { try { LocalTime.parse(sunriseTime) } catch (e: Exception) { LocalTime.of(6, 30) } }
    val sunset = remember(sunsetTime) { try { LocalTime.parse(sunsetTime) } catch (e: Exception) { LocalTime.of(19, 30) } }

    val spec = remember(weatherCode, conditionLabel, time, sunrise, sunset, latitude, longitude) {
        WeatherHeroStyleManager.resolveWeatherHeroTheme(weatherCode, conditionLabel, time, latitude, longitude, sunrise, sunset)
    }

    val context = LocalContext.current
    val isReducedMotion = remember {
        try { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f) == 0f } catch (e: Exception) { false }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(targetValue = if (visible) 1f else 0.96f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(800))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .graphicsLayer {
                this.alpha = if (isReducedMotion) 1f else alpha
                this.scaleX = if (isReducedMotion) 1f else scale
                this.scaleY = if (isReducedMotion) 1f else scale
            }
            .clip(RoundedCornerShape(32.dp))
            .border(1.2.dp, Color.White.copy(0.12f), RoundedCornerShape(32.dp))
    ) {
        LiveBackgroundLayer(spec = spec)

        Box(modifier = Modifier.matchParentSize().clip(RoundedCornerShape(32.dp))) {
            WeatherEffectLayer(spec = spec, isAnimationEnabled = !isReducedMotion)
        }

        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = spec.overlayAlpha), Color.Transparent, Color.Black.copy(alpha = spec.overlayAlpha * 0.5f))
            )
        ))

        Box(modifier = Modifier.fillMaxSize().zIndex(100f)) {
            PremiumWeatherContent(
                cityName = cityName, districtName = districtName, conditionLabel = spec.displayTitle, temperature = temperature,
                feelsLike = feelsLike, humidity = humidity, windSpeed = windSpeed, uvIndex = uvIndex,
                spec = spec, unreadCount = unreadCount,
                onCityClick = onCityClick, onNotificationsClick = onNotificationsClick
            )
        }
    }
}

@Composable
fun LiveBackgroundLayer(spec: HeroThemeSpec) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val move by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(tween(40000, easing = LinearEasing), RepeatMode.Reverse),
        label = "bg_move"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val sunCenter = Offset(size.width * spec.sunMoonPosition.x, size.height * spec.sunMoonPosition.y)

        // Premium Background Brush with Vignette Effect
        val brush = Brush.radialGradient(
            0.0f to spec.gradientColors[0],
            0.6f to spec.gradientColors[1],
            1.0f to spec.gradientColors.last().copy(alpha = 0.9f),
            center = sunCenter,
            radius = size.width * 1.8f
        )

        drawRect(brush = brush)

        // Night Sky - Deeper Milky Way / Nebula
        if (spec.dayPart == DayPhase.NIGHT) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF5B21B6).copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.3f),
                    radius = size.width * 2f
                ),
                center = Offset(size.width * 0.2f, size.height * 0.3f),
                radius = size.width * 2f
            )
        }

        // Ambient glows - premium light scattering
        val scatteringColor = when (spec.dayPart) {
            DayPhase.EVENING -> Color(0xFFD84315) // Redder Evening
            DayPhase.NIGHT -> Color(0xFF334155)
            DayPhase.MORNING -> Color(0xFFFFCCBC)
            else -> Color(0xFFFFD166)
        }

        // Brighter Center Glow
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to scatteringColor.copy(0.1f),
                1.0f to Color.Transparent,
                center = sunCenter,
                radius = size.width * 1.5f
            ),
            center = sunCenter,
            radius = size.width * 1.5f
        )

        // Subtle vignette (edges darker)
        drawRect(
            brush = Brush.radialGradient(
                0.8f to Color.Transparent,
                1.0f to Color.Black.copy(0.12f),
                center = center,
                radius = size.width * 1.2f
            )
        )
    }
}

@Composable
fun WeatherEffectLayer(spec: HeroThemeSpec, isAnimationEnabled: Boolean) {
    if (!isAnimationEnabled) return

    Box(modifier = Modifier.fillMaxSize()) {
        if (spec.dayPart == DayPhase.NIGHT && (spec.themeType == WeatherThemeType.SUNNY || spec.themeType == WeatherThemeType.FOGGY)) {
            StarFieldEffect()
        }

        when (spec.effectType) {
            VisualEffectType.SUN -> PremiumSunEffect(spec)
            VisualEffectType.MOON -> PremiumMoonEffect(spec)
            VisualEffectType.RAIN -> HeroRainEffect(isStorm = false)
            VisualEffectType.THUNDER -> { HeroRainEffect(isStorm = true); LightningEffect() }
            VisualEffectType.SNOW -> SnowParticleEffect()
            VisualEffectType.FOG -> FogHazeEffect()
            else -> {}
        }

        if (spec.cloudDensity > 0) {
            HeroCloudDriftEffect(count = spec.cloudDensity, color = spec.cloudColor, dayPart = spec.dayPart)
        }
    }
}

@Composable
fun PremiumSunEffect(spec: HeroThemeSpec) {
    val infiniteTransition = rememberInfiniteTransition(label = "sun_fx")
    val drift by infiniteTransition.animateFloat(0.98f, 1.02f, infiniteRepeatable(tween(20000, easing = SineEaseInOut), RepeatMode.Reverse), label = "sun_drift")
    val energy by infiniteTransition.animateFloat(1f, 1.05f, infiniteRepeatable(tween(12000, easing = SineEaseInOut), RepeatMode.Reverse), label = "sun_energy")

    val coreColors = when (spec.dayPart) {
        DayPhase.EVENING -> listOf(Color(0xFFFFCC80), Color(0xFFFF7043), Color(0xFFD84315)) // Hotter Evening Sun
        DayPhase.MORNING -> listOf(Color(0xFFFFFDF0), Color(0xFFFFE0B2), Color(0xFFFFAB91))
        else -> listOf(Color(0xFFFFFFFF), Color(0xFFFFF3C4), Color(0xFFFCD34D))
    }

    val atmosphereColor = when (spec.dayPart) {
        DayPhase.EVENING -> Color(0xFFE64A19)
        DayPhase.MORNING -> Color(0xFFFFAB91)
        else -> Color(0xFFFBBF24)
    }

    val glowMultiplier = if (spec.dayPart == DayPhase.EVENING) 0.6f else 1.0f

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * spec.sunMoonPosition.x, size.height * spec.sunMoonPosition.y)
        val r = 36.dp.toPx() * spec.sunMoonSize * energy

        // 1. Lens Flare (Subtle)
        if (spec.dayPart == DayPhase.DAY) {
            val flareCenter = Offset(center.x - 100.dp.toPx(), center.y + 100.dp.toPx())
            drawCircle(color = Color.White.copy(alpha = 0.02f), radius = 12.dp.toPx(), center = flareCenter)
        }

        // 2. Halo Effect (Softer, multi-layered)
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color.White.copy(alpha = 0.04f * energy),
                0.8f to Color.White.copy(alpha = 0.01f * energy),
                1.0f to Color.Transparent,
                center = center,
                radius = r * 6f
            ),
            center = center,
            radius = r * 6f
        )

        // 3. Global Ambient Glow
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to atmosphereColor.copy(0.12f * glowMultiplier),
                1.0f to Color.Transparent,
                center = center,
                radius = 300.dp.toPx()
            ),
            center = center,
            radius = 300.dp.toPx()
        )

        // 4. Bloom
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to coreColors[1].copy(0.35f * glowMultiplier),
                1.0f to Color.Transparent,
                center = center,
                radius = r * 3.5f
            ),
            center = center,
            radius = r * 3.5f
        )

        // 5. Sun Disk
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to coreColors[0],
                0.5f to coreColors[1],
                1.0f to coreColors[2].copy(alpha = 0.8f),
                center = center,
                radius = r
            ),
            center = center,
            radius = r
        )
    }
}

@Composable
fun PremiumMoonEffect(spec: HeroThemeSpec) {
    val infiniteTransition = rememberInfiniteTransition(label = "moon")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(6000, easing = SineEaseInOut), RepeatMode.Reverse),
        label = "glow"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * spec.sunMoonPosition.x, size.height * spec.sunMoonPosition.y)
        val r = 26.dp.toPx() // Larger moon

        // Moon Glow (Enhanced)
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color(0xFFFDE68A).copy(0.25f * glow),
                1.0f to Color.Transparent,
                center = center,
                radius = 160.dp.toPx()
            ),
            center = center,
            radius = 160.dp.toPx()
        )

        val path = Path().apply {
            addOval(Rect(center.x - r, center.y - r, center.x + r, center.y + r))
        }
        val clipPath = Path().apply {
            addOval(Rect(center.x - r * 1.5f, center.y - r * 1.1f, center.x + r * 0.5f, center.y + r * 0.9f))
        }

        drawContext.canvas.save()
        drawContext.canvas.clipPath(clipPath, ClipOp.Difference)
        drawPath(path, Color(0xFFFDE68A))
        drawContext.canvas.restore()
    }
}



@Composable
fun HeroCloudDriftEffect(count: Int, color: Color, dayPart: DayPhase = DayPhase.DAY) {
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(130000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )

    // Fixed composition based on DayPhase for premium consistency
    val clouds = remember(count, dayPart) {
        List(count) { i ->
            val layer = when {
                i < count / 3 -> 0 // Back
                i < count * 2 / 3 -> 1 // Mid
                else -> 2 // Front
            }

            // Bias xRatio towards edges to keep center clear
            val isLeft = i % 2 == 0
            val xBase = if (isLeft) Random.nextFloat() * 0.35f else 0.65f + Random.nextFloat() * 0.35f

            // Adjust xRatio for morning sun (around 0.20f, 0.35f) to cover 25-35%
            val finalXBase = if (dayPart == DayPhase.MORNING && i == 0) 0.18f else xBase

            HeroCloudState(
                xRatio = finalXBase,
                yRatio = when (layer) {
                    0 -> 0.10f + (i * 0.05f)
                    1 -> 0.40f + (i * 0.03f)
                    else -> 0.65f - (i * 0.04f)
                },
                scale = when (layer) {
                    0 -> 0.6f + (i * 0.05f)
                    1 -> 0.9f + (i * 0.08f)
                    else -> 1.2f + (i * 0.12f)
                },
                speed = when (layer) {
                    0 -> 0.03f
                    1 -> 0.06f
                    else -> 0.11f
                },
                opacity = when (layer) {
                    0 -> 0.12f // Arka katman %10-15
                    1 -> 0.22f // Orta katman %20-25
                    else -> 0.32f // Ön katman %30-35
                },
                blur = when (layer) {
                    0 -> 6f
                    1 -> 3f
                    else -> 1f
                }
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        clouds.forEach { cloud ->
            val cw = 140.dp.toPx() * cloud.scale
            val totalSpan = size.width + cw * 2
            val x = ((cloud.xRatio + drift * cloud.speed) % 1.0f) * totalSpan - cw

            drawHeroCloud(
                Offset(x, size.height * cloud.yRatio),
                cloud.scale,
                color.copy(cloud.opacity)
            )
        }
    }
}

fun DrawScope.drawHeroCloud(center: Offset, scale: Float, color: Color) {
    val w = 120.dp.toPx() * scale
    val h = 65.dp.toPx() * scale

    // Premium Cumulus Path: Multiple small puffs for more volume and realism
    val path = Path().apply {
        moveTo(center.x - w * 0.4f, center.y + h * 0.2f)

        // Base
        cubicTo(
            center.x - w * 0.5f, center.y + h * 0.25f,
            center.x + w * 0.5f, center.y + h * 0.25f,
            center.x + w * 0.4f, center.y + h * 0.2f
        )

        // Cluster Puffs
        // Right side
        cubicTo(center.x + w * 0.55f, center.y + h * 0.1f, center.x + w * 0.5f, center.y - h * 0.2f, center.x + w * 0.25f, center.y - h * 0.15f)
        // Top right
        cubicTo(center.x + w * 0.3f, center.y - h * 0.45f, center.x + w * 0.1f, center.y - h * 0.5f, center.x, center.y - h * 0.3f)
        // Top left
        cubicTo(center.x - w * 0.1f, center.y - h * 0.5f, center.x - w * 0.3f, center.y - h * 0.45f, center.x - w * 0.25f, center.y - h * 0.15f)
        // Left side
        cubicTo(center.x - w * 0.5f, center.y - h * 0.2f, center.x - w * 0.55f, center.y + h * 0.1f, center.x - w * 0.4f, center.y + h * 0.2f)

        close()
    }
    drawPath(path, color)
}

private data class HeroCloudState(val xRatio: Float, val yRatio: Float, val scale: Float, val speed: Float, val opacity: Float, val blur: Float)

@Composable
fun HeroRainEffect(isStorm: Boolean) {
    val ry by rememberInfiniteTransition(label = "rain").animateFloat(
        initialValue = 0f,
        targetValue = 1800f, // Faster fall
        animationSpec = infiniteRepeatable(tween(if (isStorm) 500 else 900, easing = LinearEasing)),
        label = "rain_fall"
    )

    val rainDrops = remember(isStorm) {
        List(if (isStorm) 120 else 70) {
            HeroRainDrop(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat(),
                length = 25f + Random.nextFloat() * 45f, // Longer drops
                speed = 0.6f + Random.nextFloat() * 1.2f,
                alpha = 0.08f + Random.nextFloat() * 0.3f,
                strokeWidth = 0.4f + Random.nextFloat() * 0.8f, // Thinner drops
                angle = -3f + Random.nextFloat() * 6f // Variable angle
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        rainDrops.forEach { drop ->
            val x = drop.xRatio * size.width
            val y = (drop.yRatio * size.height + ry * drop.speed) % size.height

            drawLine(
                Color.White.copy(drop.alpha),
                Offset(x, y),
                Offset(x - 4f + drop.angle, y + drop.length),
                drop.strokeWidth.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

private data class HeroRainDrop(val xRatio: Float, val yRatio: Float, val length: Float, val speed: Float, val alpha: Float, val strokeWidth: Float, val angle: Float)

@Composable
fun LightningEffect() {
    val a by rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(keyframes { durationMillis = 6000; 0f at 0; 0f at 5000; 0.2f at 5050; 0f at 5100; 0.3f at 5150; 0f at 5300 }))
    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(a)))
}

@Composable
fun SnowParticleEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "snow")
    val sy by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "snow_fall"
    )
    val particles = remember {
        List(50) {
            Triple(Random.nextFloat(), Random.nextFloat(), 1.5f + Random.nextFloat() * 2.5f)
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val drift = (Math.sin(sy.toDouble() / 100 + p.first * 20) * 30).toFloat()
            val x = (p.first * size.width + drift) % size.width
            val y = (p.second * size.height + sy) % size.height

            drawCircle(
                Color.White.copy(0.6f),
                p.third.dp.toPx(),
                Offset(x, y)
            )
        }
    }
}

@Composable
fun StarFieldEffect() {
    val tw by rememberInfiniteTransition(label = "stars").animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(4000, easing = SineEaseInOut), RepeatMode.Reverse),
        label = "twinkle"
    )
    val stars = remember { List(120) { Triple(Random.nextFloat(), Random.nextFloat(), 0.4f + Random.nextFloat() * 0.8f) } }

    Canvas(modifier = Modifier.fillMaxSize()) {
        stars.forEachIndexed { i, s ->
            val alpha = (0.1f + (if (i % 4 == 0) tw else if (i % 4 == 1) 0.8f - tw else 0.4f) * 0.4f).coerceIn(0f, 1f)
            drawCircle(
                Color.White.copy(alpha = alpha * 0.5f),
                s.third.dp.toPx(),
                Offset(s.first * size.width, s.second * size.height)
            )
        }
    }
}

@Composable
fun FogHazeEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "fog")
    val d by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse),
        label = "fog_drift"
    )
    val op by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(12000, easing = SineEaseInOut), RepeatMode.Reverse),
        label = "fog_alpha"
    )

    val fogColor = Color(0xFFE9D5FF).copy(alpha = 0.5f) // Lavender tint

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Soft background haze
        drawRect(fogColor.copy(alpha = 0.06f * op))

        repeat(3) { i ->
            val xBase = -150f + d * (i + 1) * 0.15f
            // Fog layers moved away from center text
            val yPos = size.height * (if (i == 0) 0.1f else if (i == 1) 0.75f else 0.85f)
            val h = (50.dp + (25.dp * i)).toPx()

            drawRect(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        fogColor.copy(alpha = 0.04f * (i+1) * op),
                        Color.Transparent
                    )
                ),
                Offset(xBase, yPos),
                Size(size.width + 400f, h)
            )
        }
    }
}

@Composable
fun PremiumWeatherContent(
    cityName: String, districtName: String?, conditionLabel: String, temperature: String, feelsLike: String, humidity: String, windSpeed: String, uvIndex: String,
    spec: HeroThemeSpec, unreadCount: Int, onCityClick: () -> Unit, onNotificationsClick: () -> Unit
) {
    val textColor = spec.textColor
    val secondaryColor = textColor.copy(alpha = 0.65f)
    val isDark = spec.dayPart == DayPhase.NIGHT || spec.dayPart == DayPhase.EVENING || spec.themeType == WeatherThemeType.STORMY

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.clip(RoundedCornerShape(16.dp))
                .background(if (isDark) Color.White.copy(0.12f) else Color.Black.copy(0.06f))
                .clickable { onCityClick() }
                .padding(12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, null, tint = spec.accentColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text((districtName ?: cityName).uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = textColor))
                        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = secondaryColor, modifier = Modifier.size(16.dp))
                    }
                    Text("Konumu değiştir", style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = secondaryColor.copy(0.5f)))
                }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PremiumNotificationButton(unreadCount, isDark, textColor, onNotificationsClick)
                Spacer(Modifier.width(16.dp)) // Increased spacing
                Icon(spec.icon, null, tint = spec.accentColor, modifier = Modifier.size(34.dp))
            }
        }

        Spacer(Modifier.weight(0.3f)) // Balanced alignment

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(temperature, style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 100.sp,
                fontWeight = FontWeight.W100,
                letterSpacing = (-5).sp,
                color = textColor,
                shadow = Shadow(color = Color.Black.copy(alpha = if(isDark) 0.2f else 0.05f), offset = Offset(0f, 4f), blurRadius = 12f)
            ))
            Text(conditionLabel, style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                color = textColor,
                letterSpacing = 0.5.sp,
                lineHeight = 28.sp // Reduced effective spacing
            ))
            Text("Hissedilen $feelsLike", style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = secondaryColor,
                lineHeight = 20.sp // Reduced effective spacing
            ))
        }

        Spacer(Modifier.weight(0.8f)) // Balanced alignment

        // Glassmorphism Metrics Bar
        Surface(modifier = Modifier.fillMaxWidth().height(64.dp),
            color = if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.06f), // Increased contrast
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, if (isDark) Color.White.copy(0.15f) else Color.Black.copy(0.1f))) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val uvValue = if (spec.dayPart == DayPhase.NIGHT) "--" else "UV $uvIndex"

                listOf(Icons.Rounded.WaterDrop to humidity, Icons.Rounded.Air to windSpeed, Icons.Rounded.WbSunny to uvValue).forEach { (icon, valStr) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, tint = spec.accentColor, modifier = Modifier.size(20.dp)) // Larger icon
                        Spacer(Modifier.width(8.dp))
                        Text(valStr, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black), color = textColor)
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumNotificationButton(unreadCount: Int, isDark: Boolean, tint: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.size(48.dp).zIndex(120f).clip(CircleShape).background(Brush.verticalGradient(if (isDark) listOf(Color.White.copy(0.25f), Color.White.copy(0.1f)) else listOf(Color.Black.copy(0.12f), Color.Black.copy(0.05f)))).border(1.dp, if (isDark) Color.White.copy(0.3f) else Color.Black.copy(0.15f), CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Notifications, null, tint = tint, modifier = Modifier.size(22.dp))
        if (unreadCount > 0) Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(10.dp).clip(CircleShape).background(Color(0xFF3B82F6)).border(1.5.dp, if (isDark) Color(0xFF1E293B) else Color.White, CircleShape))
    }
}
