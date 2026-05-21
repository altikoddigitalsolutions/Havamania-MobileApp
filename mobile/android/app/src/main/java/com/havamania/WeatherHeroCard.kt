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

private const val TAG = "WeatherHeroCard"

enum class VisualEffectType { NONE, SUN, MOON, RAIN, SNOW, THUNDER, FOG, WIND }

data class WeatherCardVisualSpec(
    val gradientColors: List<Color>,
    val overlayAlpha: Float,
    val textColor: Color,
    val accentColor: Color,
    val mainIcon: ImageVector,
    val cloudDensity: Int,
    val effectType: VisualEffectType,
    val sunMoonPosition: Offset, // Normalized 0..1
    val isDark: Boolean,
    val cloudColor: Color = Color.White,
    val isEvening: Boolean = false,
    val isNight: Boolean = false,
    val isDawn: Boolean = false
)

object WeatherStyleResolver {
    @Composable
    fun resolveSpec(condition: WeatherCondition, phase: DayPhase, theme: AppTheme, windSpeed: Float = 0f): WeatherCardVisualSpec {
        val isNight = phase == DayPhase.NIGHT
        val isEvening = phase == DayPhase.EVENING
        val isDawn = phase == DayPhase.DAWN
        val isDay = phase == DayPhase.DAY

        // 1. Base Colors by Condition & Phase - Refined for premium contrast
        val baseColors = when {
            isEvening -> when (condition) {
                is WeatherCondition.Rain, is WeatherCondition.Thunderstorm -> listOf(Color(0xFF334155), Color(0xFF1E293B), Color(0xFFF97316), Color(0xFF7C2D12))
                else -> listOf(Color(0xFFFDBA3B), Color(0xFFF97316), Color(0xFFEF5D60), Color(0xFF7C2D12))
            }
            isNight -> listOf(Color(0xFF07111F), Color(0xFF0F1B33), Color(0xFF1E1B4B), Color(0xFF111827))
            isDawn -> when (condition) {
                is WeatherCondition.Clear, is WeatherCondition.MostlySunny -> listOf(Color(0xFFFFD1D1), Color(0xFFD0E1FF), Color(0xFFBAE6FD))
                else -> listOf(Color(0xFFD8E6F2), Color(0xFFAFC1D2), Color(0xFF7F95AA))
            }
            else -> when (condition) { // DAY
                is WeatherCondition.Clear, is WeatherCondition.MostlySunny -> listOf(Color(0xFF0EA5E9), Color(0xFF06B6D4), Color(0xFFFFD166))
                is WeatherCondition.Rain, is WeatherCondition.Thunderstorm -> listOf(Color(0xFF64748B), Color(0xFF708090), Color(0xFF475569)) // Mavi-Gri Premium Gündüz Fırtınası
                is WeatherCondition.Cloudy, is WeatherCondition.PartlyCloudy -> listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1), Color(0xFFE2E8F0))
                is WeatherCondition.Snow -> listOf(Color(0xFFE0F2FE), Color(0xFFF1F5F9), Color(0xFFDDD6FE))
                is WeatherCondition.Fog -> listOf(Color(0xFF94A3B8), Color(0xFFA9B5C1), Color(0xFFD1D9E1)) // Sisli Premium Gradient
                else -> listOf(Color(0xFF0EA5E9), Color(0xFF38BDF8), Color(0xFF7DD3FC))
            }
        }

        // 2. Effect Mapping - Fixed Priorities
        val effectType = when {
            windSpeed > 35f && condition !is WeatherCondition.Rain && condition !is WeatherCondition.Thunderstorm -> VisualEffectType.WIND
            condition is WeatherCondition.Rain -> VisualEffectType.RAIN
            condition is WeatherCondition.Thunderstorm -> VisualEffectType.THUNDER
            condition is WeatherCondition.Snow -> VisualEffectType.SNOW
            condition is WeatherCondition.Fog -> VisualEffectType.FOG
            condition is WeatherCondition.Clear -> if (isNight) VisualEffectType.MOON else VisualEffectType.SUN
            condition is WeatherCondition.MostlySunny || condition is WeatherCondition.PartlyCloudy -> if (isNight) VisualEffectType.MOON else VisualEffectType.SUN
            else -> VisualEffectType.NONE
        }

        // 3. Density & Focus - Balanced for initial screen load
        val cloudDensity = when (condition) {
            is WeatherCondition.Clear -> 0
            is WeatherCondition.MostlySunny -> 2
            is WeatherCondition.PartlyCloudy -> 4
            is WeatherCondition.Cloudy -> 8
            is WeatherCondition.Rain, is WeatherCondition.Thunderstorm -> 6
            is WeatherCondition.Fog -> 4
            else -> 2
        }

        val sunMoonPos = when {
            isEvening -> Offset(0.82f, 0.48f)
            isDawn -> Offset(0.18f, 0.38f)
            isNight -> Offset(0.85f, 0.25f)
            else -> Offset(0.82f, 0.22f)
        }

        // Text Color Logic: Improved for visibility in rainy day/snow
        val isBrightBackground = (isDay || isDawn) && (condition is WeatherCondition.Clear || condition is WeatherCondition.MostlySunny || condition is WeatherCondition.Snow)
        val isDark = !isBrightBackground

        val textColor = if (isDark) Color.White else Color(0xFF0F172A)
        val accentColor = when {
            isEvening -> Color(0xFFFFD600)
            isNight -> Color(0xFFFDE68A)
            condition is WeatherCondition.Thunderstorm -> Color(0xFFFACC15)
            condition is WeatherCondition.Snow -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
            else -> if (isDark) Color(0xFF7DD3FC) else Color(0xFF0284C7)
        }

        val cloudColor = when {
            isEvening -> Color(0xFFFFD6A5).copy(alpha = 0.6f)
            isNight -> Color(0xFF475569).copy(alpha = 0.5f)
            condition is WeatherCondition.Rain || condition is WeatherCondition.Thunderstorm -> Color(0xFF94A3B8).copy(alpha = 0.7f)
            else -> Color.White.copy(alpha = 0.8f)
        }

        val mainIcon = when (condition) {
            is WeatherCondition.Rain -> Icons.Rounded.WaterDrop
            is WeatherCondition.Snow -> Icons.Rounded.AcUnit
            is WeatherCondition.Thunderstorm -> Icons.Rounded.Thunderstorm
            is WeatherCondition.Fog -> Icons.Rounded.FilterDrama
            is WeatherCondition.Cloudy -> Icons.Rounded.Cloud
            is WeatherCondition.PartlyCloudy -> if (isNight) Icons.Rounded.CloudQueue else Icons.Rounded.WbCloudy
            else -> if (isNight) Icons.Rounded.NightsStay else Icons.Rounded.WbSunny
        }

        return applyPolish(
            WeatherCardVisualSpec(
                gradientColors = baseColors,
                overlayAlpha = if (isEvening) 0.12f else if (isNight) 0.18f else 0.02f,
                textColor = textColor,
                accentColor = accentColor,
                mainIcon = mainIcon,
                cloudDensity = cloudDensity,
                effectType = effectType,
                sunMoonPosition = sunMoonPos,
                isDark = isDark,
                cloudColor = cloudColor,
                isEvening = isEvening,
                isNight = isNight,
                isDawn = isDawn
            ), theme
        )
    }

    private fun applyPolish(spec: WeatherCardVisualSpec, theme: AppTheme): WeatherCardVisualSpec {
        val (polishColor, accentTweak) = when (theme) {
            AppTheme.SPRING_DAY, AppTheme.SPRING_NIGHT -> Color(0xFFD1FAE5) to Color(0xFF10B981)
            AppTheme.SUMMER_DAY, AppTheme.SUMMER_NIGHT -> Color(0xFFFFF7ED) to Color(0xFFF59E0B)
            AppTheme.AUTUMN_DAY, AppTheme.AUTUMN_NIGHT -> Color(0xFFFEF3C7) to Color(0xFFD97706)
            AppTheme.WINTER_DAY, AppTheme.WINTER_NIGHT -> Color(0xFFEFF6FF) to Color(0xFF3B82F6)
            else -> null to null
        }
        return if (polishColor != null) {
            spec.copy(
                gradientColors = spec.gradientColors.map { it.lerp(polishColor, 0.08f) },
                accentColor = if (accentTweak != null) spec.accentColor.lerp(accentTweak, 0.15f) else spec.accentColor
            )
        } else spec
    }

    private fun Color.lerp(other: Color, fraction: Float): Color = Color(
        red + (other.red - red) * fraction,
        green + (other.green - green) * fraction,
        blue + (other.blue - blue) * fraction,
        alpha + (other.alpha - alpha) * fraction
    )
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
    sunriseTime: String? = null,
    sunsetTime: String? = null,
    parallaxOffset: Float = 0f,
    themeViewModel: com.havamania.ui.theme.ThemeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val currentTheme by themeViewModel.currentTheme.collectAsState()

    val phase = remember(time, sunriseTime, sunsetTime) {
        val sunrise = try { LocalTime.parse(sunriseTime) } catch (e: Exception) { LocalTime.of(6, 30) }
        val sunset = try { LocalTime.parse(sunsetTime) } catch (e: Exception) { LocalTime.of(19, 30) }
        val dummyDate = LocalDate.now()
        WeatherMapper.getDayPhase(LocalDateTime.of(dummyDate, time), sunrise, sunset)
    }

    val condition = remember(weatherCode, isDay) { WeatherMapper.mapWeatherCodeToCondition(weatherCode, isDay) }
    val windSpeedValue = remember(windSpeed) { try { windSpeed.filter { it.isDigit() || it == '.' }.toFloat() } catch(e: Exception) { 0f } }
    val spec = WeatherStyleResolver.resolveSpec(condition, phase, currentTheme, windSpeedValue)

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
            .border(1.5.dp, Color.White.copy(0.15f), RoundedCornerShape(32.dp))
    ) {
        LiveBackgroundLayer(spec = spec)

        Box(modifier = Modifier.matchParentSize().clip(RoundedCornerShape(32.dp))) {
            WeatherEffectLayer(spec = spec, isAnimationEnabled = !isReducedMotion)
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(spec.overlayAlpha)))

        Box(modifier = Modifier.fillMaxSize().zIndex(100f)) {
            PremiumWeatherContent(
                cityName = cityName, districtName = districtName, conditionLabel = conditionLabel, temperature = temperature,
                feelsLike = feelsLike, humidity = humidity, windSpeed = windSpeed, uvIndex = uvIndex,
                spec = spec, unreadCount = unreadCount,
                onCityClick = onCityClick, onNotificationsClick = onNotificationsClick
            )
        }
    }
}

@Composable
fun LiveBackgroundLayer(spec: WeatherCardVisualSpec) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val move by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing), RepeatMode.Reverse),
        label = "bg_move"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val sunCenter = Offset(size.width * spec.sunMoonPosition.x, size.height * spec.sunMoonPosition.y)

        if (spec.isEvening && spec.gradientColors.size >= 4) {
            drawRect(
                brush = Brush.linearGradient(
                    0.0f to spec.gradientColors[2],
                    0.5f to spec.gradientColors[1],
                    0.8f to spec.gradientColors[0],
                    1.0f to spec.gradientColors[3],
                    start = Offset(size.width, 0f),
                    end = Offset(0f, size.height)
                )
            )
        } else {
            drawRect(brush = Brush.linearGradient(spec.gradientColors, start = Offset(move, -move), end = Offset(size.width - move, size.height + move)))
        }

        // Ambient glows - more dramatic
        val scatteringColor = if (spec.isEvening) Color(0xFFFF7A3D) else if (spec.isNight) Color(0xFF1E1B4B) else if (spec.isDawn) Color(0xFFFFD1D1) else Color(0xFFFFD166)
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to scatteringColor.copy(0.18f),
                1.0f to Color.Transparent,
                center = sunCenter,
                radius = size.width * 1.6f
            ),
            center = sunCenter,
            radius = size.width * 1.6f
        )
    }
}

@Composable
fun WeatherEffectLayer(spec: WeatherCardVisualSpec, isAnimationEnabled: Boolean) {
    if (!isAnimationEnabled) return

    Box(modifier = Modifier.fillMaxSize()) {
        if (spec.isNight && spec.effectType != VisualEffectType.RAIN && spec.effectType != VisualEffectType.THUNDER) {
            StarFieldEffect()
        }

        when (spec.effectType) {
            VisualEffectType.SUN -> PremiumSunEffect(spec)
            VisualEffectType.MOON -> PremiumMoonEffect(spec)
            VisualEffectType.RAIN -> RainEffect()
            VisualEffectType.THUNDER -> { RainEffect(); LightningEffect() }
            VisualEffectType.SNOW -> SnowParticleEffect()
            VisualEffectType.FOG -> FogHazeEffect()
            VisualEffectType.WIND -> PremiumWindEffect()
            else -> {}
        }

        if (spec.cloudDensity > 0) {
            CloudDriftEffect(count = spec.cloudDensity, color = spec.cloudColor)
        }
    }
}

@Composable
fun PremiumWindEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "wind")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "wind_drift"
    )
    val lines = remember { List(12) { Offset(Random.nextFloat(), Random.nextFloat()) } }

    Canvas(modifier = Modifier.fillMaxSize()) {
        lines.forEach { line ->
            val x = ((line.x + drift) % 1f) * size.width
            val y = line.y * size.height
            val length = 80.dp.toPx()

            drawLine(
                Brush.horizontalGradient(listOf(Color.White.copy(0f), Color.White.copy(0.2f), Color.White.copy(0f))),
                Offset(x, y),
                Offset(x + length, y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun PremiumSunEffect(spec: WeatherCardVisualSpec) {
    val infiniteTransition = rememberInfiniteTransition(label = "sun_fx")
    val drift by infiniteTransition.animateFloat(0.999f, 1.001f, infiniteRepeatable(tween(30000, easing = SineEaseInOut), RepeatMode.Reverse), label = "sun_drift")
    val energy by infiniteTransition.animateFloat(1f, 1.015f, infiniteRepeatable(tween(20000, easing = SineEaseInOut), RepeatMode.Reverse), label = "sun_energy")

    val isEvening = spec.isEvening
    val isDawn = spec.isDawn

    val sunRadiusBase = if (isEvening || isDawn) 46.dp else 36.dp
    val coreColors = when {
        isEvening -> listOf(Color(0xFFFFF9C4), Color(0xFFFFB74D), Color(0xFFF97316))
        isDawn -> listOf(Color(0xFFFFFDF0), Color(0xFFFFE4E1), Color(0xFFFFC0CB))
        else -> listOf(Color(0xFFFFFDF0), Color(0xFFFFF3C4), Color(0xFFFFD166))
    }
    val atmosphereColor = when {
        isEvening -> Color(0xFFE85D75)
        isDawn -> Color(0xFFFFB6C1)
        else -> Color(0xFFFFB703)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * spec.sunMoonPosition.x, size.height * spec.sunMoonPosition.y)
        val r = sunRadiusBase.toPx() * energy
        drawCircle(brush = Brush.radialGradient(0.0f to atmosphereColor.copy(0.18f * drift), 1.0f to Color.Transparent, center = center, radius = 280.dp.toPx()), center = center, radius = 280.dp.toPx())
        drawCircle(brush = Brush.radialGradient(0.0f to coreColors[0], 0.45f to coreColors[1].copy(0.9f), 1.0f to Color.Transparent, center = center, radius = r * 1.3f), center = center, radius = r * 1.3f, alpha = 0.95f)
    }
}

@Composable
fun PremiumMoonEffect(spec: WeatherCardVisualSpec) {
    val infiniteTransition = rememberInfiniteTransition()
    val glow by infiniteTransition.animateFloat(0.7f, 1.2f, infiniteRepeatable(tween(5000, easing = SineEaseInOut), RepeatMode.Reverse))

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * spec.sunMoonPosition.x, size.height * spec.sunMoonPosition.y)
        val r = 26.dp.toPx()

        drawCircle(brush = Brush.radialGradient(0.0f to Color(0xFFFDE68A).copy(0.18f * glow), 1.0f to Color.Transparent, center = center, radius = 120.dp.toPx()), center = center, radius = 120.dp.toPx())

        val path = Path().apply {
            addOval(Rect(center.x - r, center.y - r, center.x + r, center.y + r))
        }
        val clipPath = Path().apply {
            addOval(Rect(center.x - r * 1.65f, center.y - r * 1.25f, center.x + r * 0.35f, center.y + r * 0.75f))
        }

        drawContext.canvas.save()
        drawContext.canvas.clipPath(clipPath, ClipOp.Difference)
        drawPath(path, Color(0xFFFDE68A))
        drawContext.canvas.restore()
    }
}

val SineEaseInOut = Easing { f -> ((1 - Math.cos(f * Math.PI)) / 2).toFloat() }

fun DrawScope.drawCloud(center: Offset, scale: Float, color: Color) {
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
fun CloudDriftEffect(count: Int, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(45000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )

    // Distributed layers system: start with clouds already in place
    val clouds = remember(count) {
        List(count) { i ->
            CloudState(
                x = (i.toFloat() / count) + (Random.nextFloat() * 0.1f), // Better distribution spread
                y = (i % 3) * 0.12f + 0.05f + (Random.nextFloat() * 0.05f), // Layered Y distribution
                scale = 0.7f + Random.nextFloat() * 1.0f,
                speed = 0.25f + Random.nextFloat() * 0.5f, // Slightly slower for more premium feel
                opacity = if (i % 2 == 0) 0.35f else 0.22f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        clouds.forEach { cloud ->
            val cw = 180.dp.toPx() * cloud.scale
            val totalSpan = size.width + cw * 2

            // Seamless looping: start at cloud.x, then offset by drift*speed
            var xPos = (cloud.x + drift * cloud.speed) % 1f
            val x = xPos * totalSpan - cw

            drawCloud(Offset(x, size.height * cloud.y), cloud.scale, color.copy(cloud.opacity))
        }
    }
}

private data class CloudState(val x: Float, val y: Float, val scale: Float, val speed: Float, val opacity: Float)

@Composable
fun RainEffect() {
    val ry by rememberInfiniteTransition(label = "rain").animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "rain_fall"
    )
    val p = remember { List(40) { Offset(Random.nextFloat(), Random.nextFloat()) } }
    Canvas(modifier = Modifier.fillMaxSize()) {
        p.forEach { dr ->
            val x = dr.x * size.width
            val y = (dr.y * size.height + ry) % size.height
            drawLine(
                Color.White.copy(0.35f),
                Offset(x, y),
                Offset(x - 3f, y + 20f),
                1.5f
            )
        }
    }
}

@Composable
fun LightningEffect() {
    val a by rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(keyframes { durationMillis = 6000; 0f at 0; 0f at 5000; 0.2f at 5050; 0f at 5100; 0.3f at 5150; 0f at 5300 }))
    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(a)))
}

@Composable
fun SnowParticleEffect() {
    val sy by rememberInfiniteTransition().animateFloat(0f, 1000f, infiniteRepeatable(tween(5000, easing = LinearEasing)))
    val p = remember { List(45) { Offset(Random.nextFloat(), Random.nextFloat()) } }
    Canvas(modifier = Modifier.fillMaxSize()) { p.forEach { dr -> drawCircle(Color.White.copy(0.5f), 2.5.dp.toPx(), Offset(dr.x * size.width + (Math.sin(sy.toDouble() / 80 + dr.x * 15) * 25).toFloat(), (dr.y * size.height + sy) % size.height)) } }
}

@Composable
fun StarFieldEffect() {
    val tw by rememberInfiniteTransition().animateFloat(0.3f, 1f, infiniteRepeatable(tween(2500), RepeatMode.Reverse))
    val stars = remember { List(60) { Offset(Random.nextFloat(), Random.nextFloat()) } }
    Canvas(modifier = Modifier.fillMaxSize()) { stars.forEachIndexed { i, s -> drawCircle(Color.White.copy(0.5f * (if (i % 2 == 0) tw else 1.3f - tw)), 1.2.dp.toPx(), Offset(s.x * size.width, s.y * size.height)) } }
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
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(8000, easing = SineEaseInOut), RepeatMode.Reverse),
        label = "fog_alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // 1. Background haze layer - Base for depth
        drawRect(Color.White.copy(alpha = 0.18f))

        // 2. Horizontal fog bands - Multi-layered
        repeat(5) { i ->
            val xBase = -150f + d * (i + 1) * 0.4f
            val yPos = size.height * (0.15f + i * 0.18f)
            val h = (50.dp + (25.dp * i)).toPx()

            drawRect(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.12f * (i+1) * op),
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
    spec: WeatherCardVisualSpec, unreadCount: Int, onCityClick: () -> Unit, onNotificationsClick: () -> Unit
) {
    val textColor = spec.textColor
    val secondaryColor = textColor.copy(alpha = 0.7f)
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (spec.isDark) Color.White.copy(0.12f) else Color.Black.copy(0.06f)).clickable { onCityClick() }.padding(12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, null, tint = spec.accentColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text((districtName ?: cityName).uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = textColor)); Icon(Icons.Rounded.KeyboardArrowDown, null, tint = secondaryColor, modifier = Modifier.size(16.dp)) }
                    Text("Konumu değiştir", style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = secondaryColor.copy(0.5f)))
                }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PremiumNotificationButton(unreadCount, spec.isDark, textColor, onNotificationsClick)
                Spacer(Modifier.width(12.dp)); Icon(spec.mainIcon, null, tint = spec.accentColor, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.weight(0.5f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(temperature, style = MaterialTheme.typography.displayLarge.copy(fontSize = 110.sp, fontWeight = FontWeight.W100, letterSpacing = (-6).sp, color = textColor))
            Text(conditionLabel, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = textColor))
            Text("Hissedilen $feelsLike", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = secondaryColor))
        }
        Spacer(Modifier.weight(1f))
        Surface(modifier = Modifier.fillMaxWidth().height(64.dp), color = if (spec.isDark) Color.White.copy(0.1f) else Color.Black.copy(0.05f), shape = RoundedCornerShape(24.dp), border = BorderStroke(0.5.dp, if (spec.isDark) Color.White.copy(0.15f) else Color.Black.copy(0.08f))) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                listOf(Icons.Rounded.WaterDrop to humidity, Icons.Rounded.Air to windSpeed, Icons.Rounded.WbSunny to "UV $uvIndex").forEach { (icon, valStr) ->
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = textColor.copy(0.6f), modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(valStr, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold), color = textColor) }
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
