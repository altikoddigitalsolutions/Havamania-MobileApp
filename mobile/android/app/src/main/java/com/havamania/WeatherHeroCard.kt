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

enum class WeatherThemeType { SUNNY, RAINY, CLOUDY, SNOWY, STORMY, FOGGY }

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
        // 1. Resolve Day Part - STRICT METEOROLOGICAL RULES
        val dayPart = when {
            selectedTime.isBefore(sunrise) -> DayPhase.NIGHT
            !selectedTime.isBefore(sunrise) && selectedTime.isBefore(sunrise.plusHours(2)) -> DayPhase.MORNING
            !selectedTime.isBefore(sunrise.plusHours(2)) && selectedTime.isBefore(sunset.minusHours(2)) -> DayPhase.DAY
            !selectedTime.isBefore(sunset.minusHours(2)) && selectedTime.isBefore(sunset.plusHours(1)) -> DayPhase.EVENING
            else -> DayPhase.NIGHT
        }

        // 2. Resolve Theme Type
        val themeType = when (weatherCode) {
            0, 1 -> WeatherThemeType.SUNNY
            2, 3 -> WeatherThemeType.CLOUDY
            45, 48 -> WeatherThemeType.FOGGY
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> WeatherThemeType.RAINY
            71, 73, 75, 77, 85, 86 -> WeatherThemeType.SNOWY
            95, 96, 99 -> WeatherThemeType.STORMY
            else -> WeatherThemeType.CLOUDY
        }

        // 3. Define Palettes - Adhering to the 6 core rules
        val gradientColors = when (themeType) {
            WeatherThemeType.SUNNY -> when (dayPart) {
                DayPhase.MORNING -> listOf(Color(0xFFFFD1D1), Color(0xFFFFE4E1), Color(0xFFBAE6FD))
                DayPhase.DAY -> listOf(Color(0xFF0EA5E9), Color(0xFF38BDF8), Color(0xFFFFD166))
                DayPhase.EVENING -> listOf(Color(0xFFF97316), Color(0xFFEF4444), Color(0xFF7C2D12))
                DayPhase.NIGHT -> listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF020617))
            }
            WeatherThemeType.RAINY -> when (dayPart) {
                DayPhase.NIGHT -> listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
                else -> listOf(Color(0xFF475569), Color(0xFF64748B), Color(0xFF94A3B8))
            }
            WeatherThemeType.CLOUDY -> when (dayPart) {
                DayPhase.NIGHT -> listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF0F172A))
                else -> listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1), Color(0xFFE2E8F0))
            }
            WeatherThemeType.SNOWY -> listOf(Color(0xFFE0F2FE), Color(0xFFF1F5F9), Color(0xFFBAE6FD))
            WeatherThemeType.STORMY -> listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4C1D95))
            WeatherThemeType.FOGGY -> when (dayPart) {
                DayPhase.NIGHT -> listOf(Color(0xFF0F172A), Color(0xFF334155), Color(0xFF1E293B))
                DayPhase.MORNING -> listOf(Color(0xFFD8E6F2), Color(0xFFFFD1D1).copy(alpha = 0.3f), Color(0xFFBAE6FD))
                else -> listOf(Color(0xFFF3F4F6), Color(0xFFE5E7EB), Color(0xFFD1D5DB)) // Süt beyazı, açık gri, mavi-gri
            }
        }

        val isDark = dayPart == DayPhase.NIGHT || dayPart == DayPhase.EVENING || themeType == WeatherThemeType.STORMY
        val isFoggyDay = themeType == WeatherThemeType.FOGGY && (dayPart == DayPhase.DAY || dayPart == DayPhase.MORNING)

        val textColor = when {
            isFoggyDay -> Color(0xFF334155) // Lacivert/gri tonu sisli gün için
            isDark -> Color.White
            else -> Color(0xFF1F2937)
        }

        val accentColor = when (themeType) {
            WeatherThemeType.SUNNY -> if (dayPart == DayPhase.DAY) Color(0xFFFBBF24) else Color(0xFFFFD600)
            WeatherThemeType.RAINY -> Color(0xFF60A5FA)
            WeatherThemeType.SNOWY -> Color(0xFF38BDF8)
            WeatherThemeType.STORMY -> Color(0xFFFACC15)
            WeatherThemeType.FOGGY -> if (isFoggyDay) Color(0xFF64748B) else Color(0xFF94A3B8)
            else -> if (isDark) Color(0xFF94A3B8) else Color(0xFF4B5563)
        }

        val icon = when (themeType) {
            WeatherThemeType.SUNNY -> if (dayPart == DayPhase.NIGHT) Icons.Rounded.NightsStay else Icons.Rounded.WbSunny
            WeatherThemeType.RAINY -> Icons.Rounded.WaterDrop
            WeatherThemeType.CLOUDY -> {
                if (weatherCode == 2) {
                    if (dayPart == DayPhase.NIGHT) Icons.Rounded.NightsStay else Icons.Rounded.WbSunny
                } else Icons.Rounded.Cloud
            }
            WeatherThemeType.SNOWY -> Icons.Rounded.AcUnit
            WeatherThemeType.STORMY -> Icons.Rounded.Thunderstorm
            WeatherThemeType.FOGGY -> Icons.Rounded.FilterDrama
        }

        val effectType = when (themeType) {
            WeatherThemeType.SUNNY -> if (dayPart == DayPhase.NIGHT) VisualEffectType.MOON else VisualEffectType.SUN
            WeatherThemeType.RAINY -> VisualEffectType.RAIN
            WeatherThemeType.CLOUDY -> if (weatherCode == 2) {
                if (dayPart == DayPhase.NIGHT) VisualEffectType.MOON else VisualEffectType.SUN
            } else VisualEffectType.NONE
            WeatherThemeType.SNOWY -> VisualEffectType.SNOW
            WeatherThemeType.STORMY -> VisualEffectType.THUNDER
            WeatherThemeType.FOGGY -> VisualEffectType.FOG
        }

        val cloudDensity = when (themeType) {
            WeatherThemeType.SUNNY -> if (weatherCode == 1) 1 else 0
            WeatherThemeType.CLOUDY -> if (weatherCode == 3) 8 else 3
            WeatherThemeType.RAINY, WeatherThemeType.STORMY -> 6
            WeatherThemeType.FOGGY -> 2
            WeatherThemeType.SNOWY -> 4
        }

        val cloudColor = when {
            dayPart == DayPhase.EVENING -> Color(0xFFFFD6A5).copy(alpha = 0.5f)
            dayPart == DayPhase.NIGHT -> Color(0xFF475569).copy(alpha = 0.4f)
            themeType == WeatherThemeType.RAINY || themeType == WeatherThemeType.STORMY -> Color(0xFF64748B).copy(alpha = 0.6f)
            themeType == WeatherThemeType.FOGGY -> Color.White.copy(alpha = 0.4f)
            else -> Color.White.copy(alpha = 0.7f)
        }

        val displayTitle = buildWeatherTitle(conditionText, dayPart)

        val sunMoonPos = when (dayPart) {
            DayPhase.MORNING -> Offset(0.20f, 0.35f)
            DayPhase.DAY -> Offset(0.80f, 0.20f)
            DayPhase.EVENING -> Offset(0.85f, 0.50f)
            DayPhase.NIGHT -> Offset(0.82f, 0.25f)
        }

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
            overlayAlpha = if (isDark) 0.15f else 0.05f
        )
    }

    private fun buildWeatherTitle(condition: String, dayPart: DayPhase): String {
        val cleanCondition = condition
            .replace(" Gece", "")
            .replace(" Sabah", "")
            .replace(" Akşam", "")
            .replace(" Gündüz", "")
            .replace(" gece", "")
            .replace(" sabah", "")
            .replace(" akşam", "")
            .replace(" gündüz", "")
            .trim()

        val suffix = when (dayPart) {
            DayPhase.MORNING -> "Sabah"
            DayPhase.DAY -> ""
            DayPhase.EVENING -> "Akşam"
            DayPhase.NIGHT -> "Gece"
        }

        return if (suffix.isEmpty()) cleanCondition else "$cleanCondition $suffix"
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

        drawRect(brush = Brush.linearGradient(spec.gradientColors, start = Offset(move, -move), end = Offset(size.width - move, size.height + move)))

        // Ambient glows - premium light scattering
        val scatteringColor = when (spec.dayPart) {
            DayPhase.EVENING -> Color(0xFFFF7A3D)
            DayPhase.NIGHT -> Color(0xFF1E1B4B)
            DayPhase.MORNING -> Color(0xFFFFD1D1)
            else -> Color(0xFFFFD166)
        }

        drawCircle(
            brush = Brush.radialGradient(
                0.0f to scatteringColor.copy(0.15f),
                1.0f to Color.Transparent,
                center = sunCenter,
                radius = size.width * 1.5f
            ),
            center = sunCenter,
            radius = size.width * 1.5f
        )

        // Subtle vignette
        drawRect(
            brush = Brush.radialGradient(
                0.0f to Color.Transparent,
                1.0f to Color.Black.copy(0.12f),
                center = center,
                radius = size.width
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
            HeroCloudDriftEffect(count = spec.cloudDensity, color = spec.cloudColor)
        }
    }
}

@Composable
fun PremiumSunEffect(spec: HeroThemeSpec) {
    val infiniteTransition = rememberInfiniteTransition(label = "sun_fx")
    val drift by infiniteTransition.animateFloat(0.99f, 1.01f, infiniteRepeatable(tween(20000, easing = SineEaseInOut), RepeatMode.Reverse), label = "sun_drift")
    val energy by infiniteTransition.animateFloat(1f, 1.02f, infiniteRepeatable(tween(15000, easing = SineEaseInOut), RepeatMode.Reverse), label = "sun_energy")

    val coreColors = when (spec.dayPart) {
        DayPhase.EVENING -> listOf(Color(0xFFFFF9C4), Color(0xFFFFB74D), Color(0xFFF97316))
        DayPhase.MORNING -> listOf(Color(0xFFFFFDF0), Color(0xFFFFE4E1), Color(0xFFFFC0CB))
        else -> listOf(Color(0xFFFFFDF0), Color(0xFFFFF3C4), Color(0xFFFFD166))
    }

    val atmosphereColor = when (spec.dayPart) {
        DayPhase.EVENING -> Color(0xFFE85D75)
        DayPhase.MORNING -> Color(0xFFFFB6C1)
        else -> Color(0xFFFFB703)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * spec.sunMoonPosition.x, size.height * spec.sunMoonPosition.y)
        val r = (if (spec.dayPart != DayPhase.DAY) 38.dp else 28.dp).toPx() * energy

        drawCircle(brush = Brush.radialGradient(0.0f to atmosphereColor.copy(0.1f * drift), 1.0f to Color.Transparent, center = center, radius = 200.dp.toPx()), center = center, radius = 200.dp.toPx())
        drawCircle(brush = Brush.radialGradient(0.0f to coreColors[0], 0.5f to coreColors[1].copy(0.7f), 1.0f to Color.Transparent, center = center, radius = r * 1.3f), center = center, radius = r * 1.3f, alpha = 0.85f)
    }
}

@Composable
fun PremiumMoonEffect(spec: HeroThemeSpec) {
    val infiniteTransition = rememberInfiniteTransition()
    val glow by infiniteTransition.animateFloat(0.8f, 1.1f, infiniteRepeatable(tween(6000, easing = SineEaseInOut), RepeatMode.Reverse))

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * spec.sunMoonPosition.x, size.height * spec.sunMoonPosition.y)
        val r = 20.dp.toPx()

        drawCircle(brush = Brush.radialGradient(0.0f to Color(0xFFFDE68A).copy(0.12f * glow), 1.0f to Color.Transparent, center = center, radius = 120.dp.toPx()), center = center, radius = 120.dp.toPx())

        val path = Path().apply {
            addOval(Rect(center.x - r, center.y - r, center.x + r, center.y + r))
        }
        val clipPath = Path().apply {
            addOval(Rect(center.x - r * 1.6f, center.y - r * 1.2f, center.x + r * 0.4f, center.y + r * 0.8f))
        }

        drawContext.canvas.save()
        drawContext.canvas.clipPath(clipPath, ClipOp.Difference)
        drawPath(path, Color(0xFFFDE68A))
        drawContext.canvas.restore()
    }
}

fun DrawScope.drawHeroCloud(center: Offset, scale: Float, color: Color) {
    val w = 150.dp.toPx() * scale
    val h = 80.dp.toPx() * scale
    val path = Path().apply {
        moveTo(center.x - w * 0.4f, center.y + h * 0.25f)
        cubicTo(center.x - w * 0.65f, center.y + h * 0.1f, center.x - w * 0.6f, center.y - h * 0.35f, center.x - w * 0.25f, center.y - h * 0.25f)
        cubicTo(center.x - w * 0.1f, center.y - h * 0.75f, center.x + w * 0.3f, center.y - h * 0.7f, center.x + w * 0.35f, center.y - h * 0.2f)
        cubicTo(center.x + w * 0.65f, center.y - h * 0.1f, center.x + w * 0.7f, center.y + h * 0.35f, center.x + w * 0.4f, center.y + h * 0.3f)
        close()
    }
    drawPath(path, color)
}

@Composable
fun HeroCloudDriftEffect(count: Int, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(50000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )

    val clouds = remember(count) {
        List(count) { i ->
            HeroCloudState(
                xRatio = Random.nextFloat(),
                yRatio = (i.toFloat() / count) * 0.4f + 0.05f,
                scale = 0.6f + Random.nextFloat() * 0.8f,
                speed = 0.15f + Random.nextFloat() * 0.35f,
                opacity = 0.15f + Random.nextFloat() * 0.25f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        clouds.forEach { cloud ->
            val cw = 160.dp.toPx() * cloud.scale
            val totalSpan = size.width + cw * 2
            val x = ((cloud.xRatio + drift * cloud.speed) % 1.0f) * totalSpan - cw

            drawHeroCloud(Offset(x, size.height * cloud.yRatio), cloud.scale, color.copy(cloud.opacity))
        }
    }
}

private data class HeroCloudState(val xRatio: Float, val yRatio: Float, val scale: Float, val speed: Float, val opacity: Float)

@Composable
fun HeroRainEffect(isStorm: Boolean) {
    val ry by rememberInfiniteTransition(label = "rain").animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(if (isStorm) 800 else 1400, easing = LinearEasing)),
        label = "rain_fall"
    )

    val rainDrops = remember(isStorm) {
        List(if (isStorm) 60 else 40) {
            HeroRainDrop(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat(),
                length = 15f + Random.nextFloat() * 15f,
                speed = 0.8f + Random.nextFloat() * 0.4f,
                alpha = 0.15f + Random.nextFloat() * 0.25f,
                isForeground = Random.nextBoolean()
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        rainDrops.forEach { drop ->
            val x = drop.xRatio * size.width
            val y = (drop.yRatio * size.height + ry * drop.speed) % size.height
            val stroke = if (drop.isForeground) 1.2.dp.toPx() else 0.8.dp.toPx()

            drawLine(
                Color.White.copy(drop.alpha),
                Offset(x, y),
                Offset(x - 2f, y + drop.length),
                stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

private data class HeroRainDrop(val xRatio: Float, val yRatio: Float, val length: Float, val speed: Float, val alpha: Float, val isForeground: Boolean)

@Composable
fun LightningEffect() {
    val a by rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(keyframes { durationMillis = 6000; 0f at 0; 0f at 5000; 0.2f at 5050; 0f at 5100; 0.3f at 5150; 0f at 5300 }))
    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(a)))
}

@Composable
fun SnowParticleEffect() {
    val sy by rememberInfiniteTransition().animateFloat(0f, 1000f, infiniteRepeatable(tween(5000, easing = LinearEasing)))
    val p = remember { List(45) { Offset(Random.nextFloat(), Random.nextFloat()) } }
    Canvas(modifier = Modifier.fillMaxSize()) {
        p.forEach { dr ->
            drawCircle(
                Color.White.copy(0.5f),
                2.5.dp.toPx(),
                Offset(dr.x * size.width + (Math.sin(sy.toDouble() / 80 + dr.x * 15) * 25).toFloat(), (dr.y * size.height + sy) % size.height)
            )
        }
    }
}

@Composable
fun StarFieldEffect() {
    val tw by rememberInfiniteTransition().animateFloat(0.3f, 1f, infiniteRepeatable(tween(2500), RepeatMode.Reverse))
    val stars = remember { List(60) { Offset(Random.nextFloat(), Random.nextFloat()) } }
    Canvas(modifier = Modifier.fillMaxSize()) {
        stars.forEachIndexed { i, s ->
            drawCircle(
                Color.White.copy(0.5f * (if (i % 2 == 0) tw else 1.3f - tw)),
                1.2.dp.toPx(),
                Offset(s.x * size.width, s.y * size.height)
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
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "fog_drift"
    )
    val op by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(8000, easing = SineEaseInOut), RepeatMode.Reverse),
        label = "fog_alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Soft background haze
        drawRect(Color.White.copy(alpha = 0.15f * op))

        repeat(4) { i ->
            val xBase = -150f + d * (i + 1) * 0.3f
            val yPos = size.height * (0.2f + i * 0.2f)
            val h = (40.dp + (20.dp * i)).toPx()

            drawRect(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.08f * (i+1) * op),
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
                Spacer(Modifier.width(12.dp))
                Icon(spec.icon, null, tint = spec.accentColor, modifier = Modifier.size(34.dp))
            }
        }

        Spacer(Modifier.weight(0.4f))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(temperature, style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 100.sp,
                fontWeight = FontWeight.W100,
                letterSpacing = (-5).sp,
                color = textColor,
                shadow = Shadow(color = Color.Black.copy(alpha = if(isDark) 0.2f else 0.05f), offset = Offset(0f, 4f), blurRadius = 12f)
            ))
            Text(conditionLabel, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = textColor, letterSpacing = 0.5.sp))
            Text("Hissedilen $feelsLike", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = secondaryColor))
        }

        Spacer(Modifier.weight(1f))

        // Glassmorphism Metrics Bar
        Surface(modifier = Modifier.fillMaxWidth().height(64.dp),
            color = if (isDark) Color.White.copy(0.08f) else Color.Black.copy(0.04f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(0.8.dp, if (isDark) Color.White.copy(0.12f) else Color.Black.copy(0.08f))) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                listOf(Icons.Rounded.WaterDrop to humidity, Icons.Rounded.Air to windSpeed, Icons.Rounded.WbSunny to "UV $uvIndex").forEach { (icon, valStr) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, tint = spec.accentColor.copy(0.8f), modifier = Modifier.size(18.dp))
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
