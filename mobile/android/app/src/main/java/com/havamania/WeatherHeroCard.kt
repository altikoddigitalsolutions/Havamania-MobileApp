package com.havamania

import android.provider.Settings
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.AppTheme
import java.time.LocalTime
import kotlin.random.Random

private const val TAG = "WeatherHeroCard"
private const val SunnyDebugMode = false // Premium Sun Disk Debug Mode

enum class SunVariant {
    CLEAR_DAY, MOSTLY_SUNNY, PARTLY_CLOUDY, EVENING
}

data class WeatherCardStyle(
    val gradientColors: List<Color>,
    val lightOverlay: Color,
    val accentColor: Color,
    val icon: ImageVector,
    val isDark: Boolean,
    val styleName: String
)

object WeatherStyleResolver {
    @Composable
    fun resolve(condition: WeatherCondition, phase: DayPhase, theme: AppTheme, isDay: Boolean): WeatherCardStyle {
        if (phase == DayPhase.SUNSET) {
            return applyThemePolish(resolveSunsetStyle(condition, isDay), theme)
        }
        val style = when (condition) {
            is WeatherCondition.Clear, is WeatherCondition.NightClear -> resolveSunnyStyle(phase, isDay)
            is WeatherCondition.MostlySunny -> resolveMostlySunnyStyle(phase, isDay)
            is WeatherCondition.PartlyCloudy -> resolvePartlyCloudyStyle(phase, isDay)
            is WeatherCondition.Cloudy -> resolveCloudyStyle(phase)
            is WeatherCondition.Rain -> resolveRainStyle(phase)
            is WeatherCondition.Thunderstorm -> resolveThunderStyle(phase)
            is WeatherCondition.Snow -> resolveSnowStyle(phase)
            is WeatherCondition.Fog -> resolveFogStyle(phase)
        }
        return applyThemePolish(style, theme)
    }

    private fun resolveSunsetStyle(condition: WeatherCondition, isDay: Boolean): WeatherCardStyle {
        val colors = listOf(
            Color(0xFFF9B233), // Amber
            Color(0xFFF97316), // Orange
            Color(0xFFE85D75), // Pink/Coral
            Color(0xFF7C2D12)  // Dark Warm Depth
        )
        val icon = when (condition) {
            is WeatherCondition.Rain -> Icons.Rounded.WaterDrop
            is WeatherCondition.Snow -> Icons.Rounded.AcUnit
            is WeatherCondition.Thunderstorm -> Icons.Rounded.Thunderstorm
            is WeatherCondition.Fog -> Icons.Rounded.FilterDrama
            is WeatherCondition.Cloudy -> Icons.Rounded.Cloud
            is WeatherCondition.PartlyCloudy -> if (isDay) Icons.Rounded.WbCloudy else Icons.Rounded.CloudQueue
            else -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
        }
        return WeatherCardStyle(
            gradientColors = colors,
            lightOverlay = Color.Black.copy(0.15f),
            accentColor = Color.White,
            icon = icon,
            isDark = true,
            styleName = "SUNSET_FORCED"
        )
    }

    private fun resolveCloudyStyle(phase: DayPhase): WeatherCardStyle {
        val baseColors = when (phase) {
            DayPhase.SUNRISE -> listOf(Color(0xFFD8E6F2), Color(0xFFAFC1D2), Color(0xFF7F95AA))
            DayPhase.DAY -> listOf(Color(0xFFC4D5E4), Color(0xFF9BAFC3), Color(0xFF6F849A))
            DayPhase.SUNSET -> return resolveSunsetStyle(WeatherCondition.Cloudy, true)
            DayPhase.NIGHT -> listOf(Color(0xFF1B2432), Color(0xFF263447), Color(0xFF33485F))
        }
        val isDark = phase == DayPhase.NIGHT
        return WeatherCardStyle(baseColors, if (isDark) Color.Transparent else Color.White.copy(0.15f), if (isDark) Color(0xFFA7B5C5) else Color(0xFF475569), Icons.Rounded.Cloud, isDark, "CLOUDY_$phase")
    }

    private fun resolveSunnyStyle(phase: DayPhase, isDay: Boolean): WeatherCardStyle {
        val icon = if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
        return when (phase) {
            DayPhase.SUNRISE -> WeatherCardStyle(listOf(Color(0xFFFFD1D1), Color(0xFFD0E1FF), Color(0xFFBAE6FD)), Color(0xFFFFF9C4).copy(0.2f), Color(0xFFD84315), icon, false, "SUNNY_SUNRISE")
            DayPhase.DAY -> WeatherCardStyle(listOf(Color(0xFF0EA5E9), Color(0xFF38BDF8), Color(0xFF7DD3FC)), Color.White.copy(0.1f), Color(0xFFFFD600), icon, false, "SUNNY_DAY")
            DayPhase.SUNSET -> return resolveSunsetStyle(WeatherCondition.Clear, isDay)
            DayPhase.NIGHT -> WeatherCardStyle(listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155)), Color.Transparent, Color(0xFFFDE68A), icon, true, "SUNNY_NIGHT")
        }
    }

    private fun resolveMostlySunnyStyle(phase: DayPhase, isDay: Boolean): WeatherCardStyle = resolveSunnyStyle(phase, isDay).copy(styleName = "MOSTLY_SUNNY_$phase")

    private fun resolvePartlyCloudyStyle(phase: DayPhase, isDay: Boolean): WeatherCardStyle {
        val isDark = phase == DayPhase.NIGHT || phase == DayPhase.SUNSET
        val icon = if (isDay) Icons.Rounded.WbCloudy else Icons.Rounded.CloudQueue
        return WeatherCardStyle(if (isDark) listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF475569)) else listOf(Color(0xFFBAE6FD), Color(0xFF7DD3FC), Color(0xFFE0F2FE)), if (isDark) Color.Transparent else Color.White.copy(0.15f), Color(0xFFF1F5F9), icon, isDark, "PARTLY_CLOUDY_$phase")
    }

    private fun resolveRainStyle(phase: DayPhase): WeatherCardStyle {
        val isDark = phase == DayPhase.NIGHT || phase == DayPhase.SUNSET
        return WeatherCardStyle(if (isDark) listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E293B)) else listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF475569)), Color.White.copy(0.05f), Color(0xFF60A5FA), Icons.Rounded.WaterDrop, true, "RAIN_$phase")
    }

    private fun resolveThunderStyle(phase: DayPhase): WeatherCardStyle = WeatherCardStyle(listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4338CA)), Color(0xFFC084FC).copy(0.1f), Color(0xFFFACC15), Icons.Rounded.Thunderstorm, true, "THUNDER_$phase")

    private fun resolveSnowStyle(phase: DayPhase): WeatherCardStyle = WeatherCardStyle(listOf(Color(0xFFE0F2FE), Color(0xFFF1F5F9), Color(0xFFFFFFFF)), Color.White.copy(0.2f), Color(0xFF38BDF8), Icons.Rounded.AcUnit, false, "SNOW_$phase")

    private fun resolveFogStyle(phase: DayPhase): WeatherCardStyle = WeatherCardStyle(listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1), Color(0xFFE2E8F0)), Color.White.copy(0.2f), Color(0xFF475569), Icons.Rounded.FilterDrama, false, "FOG_$phase")

    private fun applyThemePolish(style: WeatherCardStyle, theme: AppTheme): WeatherCardStyle {
        val (polishColor, accentTweak) = when (theme) {
            AppTheme.SPRING_DAY, AppTheme.SPRING_NIGHT -> Color(0xFFD1FAE5) to Color(0xFF10B981)
            AppTheme.SUMMER_DAY, AppTheme.SUMMER_NIGHT -> Color(0xFFFFF7ED) to Color(0xFFF59E0B)
            AppTheme.AUTUMN_DAY, AppTheme.AUTUMN_NIGHT -> Color(0xFFFEF3C7) to Color(0xFFD97706)
            AppTheme.WINTER_DAY, AppTheme.WINTER_NIGHT -> Color(0xFFEFF6FF) to Color(0xFF3B82F6)
            else -> null to null
        }
        return if (polishColor != null) {
            style.copy(gradientColors = style.gradientColors.map { it.lerp(polishColor, 0.08f) }, accentColor = if (accentTweak != null) style.accentColor.lerp(accentTweak, 0.15f) else style.accentColor)
        } else style
    }

    private fun Color.lerp(other: Color, fraction: Float): Color = Color(red + (other.red - red) * fraction, green + (other.green - green) * fraction, blue + (other.blue - blue) * fraction, alpha + (other.alpha - alpha) * fraction)
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
        WeatherMapper.getDayPhase(java.time.LocalDateTime.now(), sunrise, sunset)
    }

    val condition = remember(weatherCode, isDay) { WeatherMapper.mapWeatherCodeToCondition(weatherCode, isDay) }
    val style = WeatherStyleResolver.resolve(condition, phase, currentTheme, isDay)

    val isSunnyScene = SunnyDebugMode || condition is WeatherCondition.Clear || condition is WeatherCondition.MostlySunny || (condition is WeatherCondition.PartlyCloudy && isDay)

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
        val sunVariant = if (isSunnyScene && phase != DayPhase.NIGHT) {
            when {
                phase == DayPhase.SUNSET -> SunVariant.EVENING
                condition is WeatherCondition.MostlySunny -> SunVariant.MOSTLY_SUNNY
                condition is WeatherCondition.PartlyCloudy -> SunVariant.PARTLY_CLOUDY
                else -> SunVariant.CLEAR_DAY
            }
        } else null

        LiveBackgroundLayer(colors = style.gradientColors, isReducedMotion = isReducedMotion, sunVariant = sunVariant)

        Box(modifier = Modifier.matchParentSize().clip(RoundedCornerShape(32.dp))) {
            WeatherEffectLayer(condition, phase, weatherCode, !isReducedMotion, parallaxOffset)
        }

        Box(modifier = Modifier.fillMaxSize().background(style.lightOverlay))

        Box(modifier = Modifier.fillMaxSize().zIndex(100f)) {
            PremiumWeatherContent(
                cityName = cityName, districtName = districtName, conditionLabel = conditionLabel, temperature = temperature,
                feelsLike = feelsLike, humidity = humidity, windSpeed = windSpeed, uvIndex = uvIndex,
                style = style, isSunnyScene = isSunnyScene, unreadCount = unreadCount,
                onCityClick = onCityClick, onNotificationsClick = onNotificationsClick
            )
        }
    }
}

@Composable
fun LiveBackgroundLayer(colors: List<Color>, isReducedMotion: Boolean, sunVariant: SunVariant? = null) {
    val infiniteTransition = rememberInfiniteTransition()
    val move by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 100f, animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing), RepeatMode.Reverse))

    Canvas(modifier = Modifier.fillMaxSize()) {
        val sunCenter = Offset(size.width * 0.82f, size.height * (if (sunVariant == SunVariant.EVENING) 0.42f else 0.23f))

        if (sunVariant == SunVariant.EVENING && colors.size >= 4) {
            // Special linear gradient distribution for sunset
            drawRect(
                brush = Brush.linearGradient(
                    0.0f to colors[2], // Sağ Üst: Pembe
                    0.5f to colors[1], // Orta: Turuncu
                    0.8f to colors[0], // Sol Alt: Amber
                    1.0f to colors[3], // Sağ Alt: Koyu Sıcak
                    start = Offset(size.width, 0f),
                    end = Offset(0f, size.height)
                )
            )
        } else {
            drawRect(brush = Brush.linearGradient(colors, start = Offset(move, -move), end = Offset(size.width - move, size.height + move)))
        }

        if (sunVariant != null) {
            val scatteringBase = if (sunVariant == SunVariant.EVENING) Color(0xFFFF7A3D) else Color(0xFFFFD166)
            drawCircle(brush = Brush.radialGradient(0.0f to scatteringBase.copy(0.12f), 0.5f to scatteringBase.copy(0.04f), 1.0f to Color.Transparent, center = sunCenter, radius = size.width * 1.6f), center = sunCenter, radius = size.width * 1.6f)
            drawCircle(brush = Brush.radialGradient(0.0f to scatteringBase.copy(0.08f), 1.0f to Color.Transparent, center = sunCenter, radius = size.width * 0.8f), center = sunCenter, radius = size.width * 0.8f)
        }
    }
}

@Composable
fun WeatherEffectLayer(condition: WeatherCondition, phase: DayPhase, weatherCode: Int, isAnimationEnabled: Boolean, parallaxOffset: Float) {
    if (!isAnimationEnabled) return
    val isSunnyScene = SunnyDebugMode || condition is WeatherCondition.Clear || condition is WeatherCondition.MostlySunny || (condition is WeatherCondition.PartlyCloudy && phase != DayPhase.NIGHT)
    val isSunset = phase == DayPhase.SUNSET

    Box(modifier = Modifier.fillMaxSize()) {
        if (isSunnyScene && phase != DayPhase.NIGHT) {
            val variant = when { phase == DayPhase.SUNSET -> SunVariant.EVENING; condition is WeatherCondition.MostlySunny -> SunVariant.MOSTLY_SUNNY; condition is WeatherCondition.PartlyCloudy -> SunVariant.PARTLY_CLOUDY; else -> SunVariant.CLEAR_DAY }
            PremiumSunEffect(variant)
            val cloudColor = if (isSunset) Color(0xFFFFD6A5) else Color.White
            if (condition is WeatherCondition.MostlySunny) CloudDriftEffect(2, 0.2f, cloudColor) else if (condition is WeatherCondition.PartlyCloudy) CloudDriftEffect(3, 0.32f, cloudColor)
        } else {
            val cloudColor = if (isSunset) Color(0xFFFFD6A5) else if (phase == DayPhase.NIGHT) Color(0xFF64748B) else Color(0xFFD1D5DB)
            when (condition) {
                is WeatherCondition.Clear, is WeatherCondition.NightClear, is WeatherCondition.MostlySunny -> if (phase == DayPhase.NIGHT) StarFieldEffect()
                is WeatherCondition.PartlyCloudy -> if (phase == DayPhase.NIGHT) { StarFieldEffect(); CloudDriftEffect(2, 0.25f, Color(0xFF94A3B8)) }
                is WeatherCondition.Cloudy -> CloudDriftEffect(if (phase == DayPhase.NIGHT) 4 else 6, if (phase == DayPhase.NIGHT) 0.35f else 0.42f, cloudColor)
                is WeatherCondition.Rain -> RainEffect()
                is WeatherCondition.Thunderstorm -> { RainEffect(); LightningEffect() }
                is WeatherCondition.Snow -> SnowParticleEffect()
                is WeatherCondition.Fog -> FogHazeEffect()
            }
        }
    }
}

@Composable
fun PremiumSunEffect(variant: SunVariant) {
    val infiniteTransition = rememberInfiniteTransition()
    val drift by infiniteTransition.animateFloat(0.999f, 1.001f, infiniteRepeatable(tween(30000, easing = SineEaseInOut), RepeatMode.Reverse))
    val energy by infiniteTransition.animateFloat(1f, 1.012f, infiniteRepeatable(tween(20000, easing = SineEaseInOut), RepeatMode.Reverse))

    val sunRadiusBase = if (variant == SunVariant.EVENING) 42.dp else 36.dp
    val coreColors = if (variant == SunVariant.EVENING) listOf(Color(0xFFFFF9C4), Color(0xFFFFB74D), Color(0xFFF97316)) else listOf(Color(0xFFFFFDF0), Color(0xFFFFF3C4), Color(0xFFFFD166))
    val atmosphereColor = if (variant == SunVariant.EVENING) Color(0xFFE85D75) else Color(0xFFFFB703)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * 0.82f, size.height * (if (variant == SunVariant.EVENING) 0.42f else 0.23f))
        val r = sunRadiusBase.toPx() * energy
        drawCircle(brush = Brush.radialGradient(0.0f to atmosphereColor.copy(0.15f * drift), 1.0f to Color.Transparent, center = center, radius = 350.dp.toPx()), center = center, radius = 350.dp.toPx())
        drawCircle(brush = Brush.radialGradient(0.0f to coreColors[0], 0.4f to coreColors[1].copy(0.95f), 1.0f to Color.Transparent, center = center, radius = r * 1.2f), center = center, radius = r * 1.2f, alpha = if (variant == SunVariant.PARTLY_CLOUDY) 0.5f else 0.9f)
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
fun CloudDriftEffect(count: Int, opacity: Float, color: Color = Color.White) {
    val drift by rememberInfiniteTransition().animateFloat(-80f, 80f, infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Reverse))
    val clouds = remember(count) { List(count) { Triple(Offset(Random.nextFloat(), Random.nextFloat() * 0.35f + 0.05f), 0.8f + Random.nextFloat() * 0.7f, 0.5f + Random.nextFloat() * 1.0f) } }
    Canvas(modifier = Modifier.fillMaxSize()) {
        clouds.forEach { (pos, scale, speed) ->
            val cw = 160.dp.toPx() * scale
            drawCloud(Offset((size.width * pos.x + drift * speed * 1.5f) % (size.width + cw * 2) - cw, size.height * pos.y), scale, color.copy(opacity))
        }
    }
}

@Composable
fun RainEffect() {
    val ry by rememberInfiniteTransition().animateFloat(0f, 1000f, infiniteRepeatable(tween(1500, easing = LinearEasing)))
    val p = remember { List(35) { Offset(Random.nextFloat(), Random.nextFloat()) } }
    Canvas(modifier = Modifier.fillMaxSize()) { p.forEach { dr -> drawLine(Color.White.copy(0.25f), Offset(dr.x * size.width, (dr.y * size.height + ry) % size.height), Offset(dr.x * size.width - 4f, (dr.y * size.height + ry) % size.height + 18f), 1.5f) } }
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
    val d by rememberInfiniteTransition().animateFloat(0f, 200f, infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse))
    Canvas(modifier = Modifier.fillMaxSize()) { repeat(3) { i -> drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(0.12f), Color.Transparent)), Offset(-100f + d * (i + 1) * 0.25f, size.height * (0.65f + i * 0.08f)), Size(size.width + 200f, 35.dp.toPx())) } }
}

@Composable
fun PremiumWeatherContent(
    cityName: String, districtName: String?, conditionLabel: String, temperature: String, feelsLike: String, humidity: String, windSpeed: String, uvIndex: String,
    style: WeatherCardStyle, isSunnyScene: Boolean, unreadCount: Int, onCityClick: () -> Unit, onNotificationsClick: () -> Unit
) {
    val textColor = if (style.isDark) Color.White else Color(0xFF0F172A)
    val secondaryColor = textColor.copy(alpha = 0.7f)
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (style.isDark) Color.White.copy(0.12f) else Color.Black.copy(0.06f)).clickable { onCityClick() }.padding(12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, null, tint = style.accentColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text((districtName ?: cityName).uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = textColor)); Icon(Icons.Rounded.KeyboardArrowDown, null, tint = secondaryColor, modifier = Modifier.size(16.dp)) }
                    Text("Konumu değiştir", style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = secondaryColor.copy(0.5f)))
                }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PremiumNotificationButton(unreadCount, style.isDark, textColor, onNotificationsClick)
                if (!isSunnyScene) { Spacer(Modifier.width(12.dp)); Icon(style.icon, null, tint = style.accentColor, modifier = Modifier.size(36.dp)) }
            }
        }
        Spacer(Modifier.weight(0.5f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(temperature, style = MaterialTheme.typography.displayLarge.copy(fontSize = 110.sp, fontWeight = FontWeight.W100, letterSpacing = (-6).sp, color = textColor))
            Text(conditionLabel, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = textColor))
            Text("Hissedilen $feelsLike", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = secondaryColor))
        }
        Spacer(Modifier.weight(1f))
        Surface(modifier = Modifier.fillMaxWidth().height(64.dp), color = if (style.isDark) Color.White.copy(0.1f) else Color.Black.copy(0.05f), shape = RoundedCornerShape(24.dp), border = BorderStroke(0.5.dp, if (style.isDark) Color.White.copy(0.15f) else Color.Black.copy(0.08f))) {
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
