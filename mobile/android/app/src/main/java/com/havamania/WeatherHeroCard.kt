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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.AppTheme
import java.time.LocalTime
import kotlin.random.Random

private const val TAG = "WeatherHeroCard"

// ── Shared Models & Enums ───────────────────────────────────────────────────

enum class TimeOfDay { MORNING, DAY, EVENING, NIGHT }

sealed class WeatherCondition {
    object Clear : WeatherCondition()
    object PartlyCloudy : WeatherCondition()
    object Cloudy : WeatherCondition()
    object Rain : WeatherCondition()
    object Thunderstorm : WeatherCondition()
    object Snow : WeatherCondition()
    object Fog : WeatherCondition()
    object NightClear : WeatherCondition()
}

enum class WeatherEffectType { NONE, SUNNY, CLOUDY, RAINY, NIGHT, SNOW, FOG, THUNDER }

data class WeatherCardStyle(
    val gradientColors: List<Color>,
    val accentColor: Color,
    val icon: ImageVector,
    val effectType: WeatherEffectType,
    val isDark: Boolean,
    val styleName: String
)

// ── Shared Logic & Mappers ────────────────────────────────────────────────────

object WeatherStyleMapper {
    fun getCondition(code: Int, isDay: Boolean): WeatherCondition {
        return when (code) {
            0 -> if (isDay) WeatherCondition.Clear else WeatherCondition.NightClear
            1, 2 -> WeatherCondition.PartlyCloudy
            3 -> WeatherCondition.Cloudy
            45, 48 -> WeatherCondition.Fog
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> WeatherCondition.Rain
            71, 73, 75, 77, 85, 86 -> WeatherCondition.Snow
            95, 96, 99 -> WeatherCondition.Thunderstorm
            else -> WeatherCondition.Cloudy
        }
    }
}

fun resolveTimeOfDay(hour: Int): TimeOfDay {
    return when (hour) {
        in 6..10 -> TimeOfDay.MORNING
        in 11..16 -> TimeOfDay.DAY
        in 17..18 -> TimeOfDay.EVENING
        else -> TimeOfDay.NIGHT
    }
}

// ── Style Resolver ───────────────────────────────────────────────────────────

object WeatherStyleResolver {
    @Composable
    fun resolve(
        condition: WeatherCondition,
        timeOfDay: TimeOfDay,
        theme: AppTheme
    ): WeatherCardStyle {
        return when (condition) {
            is WeatherCondition.Clear, is WeatherCondition.NightClear -> resolveSunnyStyle(timeOfDay, theme)
            is WeatherCondition.PartlyCloudy -> resolvePartlyCloudyStyle(timeOfDay, theme)
            is WeatherCondition.Cloudy -> resolveCloudyStyle(timeOfDay, theme)
            is WeatherCondition.Thunderstorm -> resolveThunderStyle(timeOfDay, theme)
            is WeatherCondition.Rain -> resolveRainStyle(timeOfDay, theme)
            is WeatherCondition.Snow -> resolveSnowStyle(timeOfDay, theme)
            is WeatherCondition.Fog -> resolveFogStyle(timeOfDay, theme)
            else -> resolveDefaultStyle(theme)
        }
    }

    private fun resolveCloudyStyle(time: TimeOfDay, theme: AppTheme): WeatherCardStyle {
        val baseColors = when (time) {
            TimeOfDay.MORNING -> listOf(Color(0xFFD8E6F2), Color(0xFFAFC1D2), Color(0xFF7F95AA))
            TimeOfDay.DAY -> listOf(Color(0xFFC4D5E4), Color(0xFF9BAFC3), Color(0xFF6F849A))
            TimeOfDay.EVENING -> listOf(Color(0xFFB7AFC8), Color(0xFF8F8FA8), Color(0xFF5F7185))
            TimeOfDay.NIGHT -> listOf(Color(0xFF1B2432), Color(0xFF263447), Color(0xFF33485F))
        }

        val colors = applyThemeAtmosphere(baseColors, theme, time)
        val isDark = time == TimeOfDay.NIGHT || time == TimeOfDay.EVENING

        return WeatherCardStyle(
            gradientColors = colors,
            accentColor = if (isDark) Color(0xFFA7B5C5) else Color(0xFFF1F5F9),
            icon = Icons.Rounded.Cloud,
            effectType = WeatherEffectType.CLOUDY,
            isDark = isDark,
            styleName = "CLOUDY_$time"
        )
    }

    private fun resolveSunnyStyle(time: TimeOfDay, theme: AppTheme): WeatherCardStyle {
        val baseColors = when (time) {
            TimeOfDay.MORNING -> listOf(Color(0xFFFFADAD), Color(0xFFFFD1D1), Color(0xFFD0E1FF)) // Peach to Light Blue
            TimeOfDay.DAY -> listOf(Color(0xFF0EA5E9), Color(0xFF38BDF8), Color(0xFF7DD3FC)) // Sky Blue
            TimeOfDay.EVENING -> listOf(Color(0xFFF97316), Color(0xFFFB923C), Color(0xFFFCD34D)) // Amber/Coral
            TimeOfDay.NIGHT -> listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
        }

        val colors = applyThemeAtmosphere(baseColors, theme, time)
        val isDark = time == TimeOfDay.NIGHT || time == TimeOfDay.EVENING

        return WeatherCardStyle(
            gradientColors = colors,
            accentColor = when (time) {
                TimeOfDay.DAY -> Color(0xFFFFD600)
                TimeOfDay.EVENING -> Color(0xFFFDE68A)
                else -> Color(0xFFF1F5F9)
            },
            icon = if (time == TimeOfDay.NIGHT) Icons.Rounded.NightsStay else Icons.Rounded.WbSunny,
            effectType = if (time == TimeOfDay.NIGHT) WeatherEffectType.NIGHT else WeatherEffectType.SUNNY,
            isDark = isDark,
            styleName = "SUNNY_$time"
        )
    }

    private fun resolvePartlyCloudyStyle(time: TimeOfDay, theme: AppTheme): WeatherCardStyle {
        val baseColors = when (time) {
            TimeOfDay.MORNING -> listOf(Color(0xFFD8E6F2), Color(0xFFBAE6FD), Color(0xFF7DD3FC))
            TimeOfDay.DAY -> listOf(Color(0xFF7DD3FC), Color(0xFFBAE6FD), Color(0xFFE0F2FE))
            TimeOfDay.EVENING -> listOf(Color(0xFFB7AFC8), Color(0xFFA78BFA), Color(0xFFF472B6))
            TimeOfDay.NIGHT -> listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF475569))
        }

        val colors = applyThemeAtmosphere(baseColors, theme, time)
        val isDark = time == TimeOfDay.NIGHT || time == TimeOfDay.EVENING

        return WeatherCardStyle(
            gradientColors = colors,
            accentColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFFFFD600).copy(alpha = 0.8f),
            icon = if (time == TimeOfDay.NIGHT) Icons.Rounded.CloudQueue else Icons.Rounded.WbCloudy,
            effectType = WeatherEffectType.CLOUDY,
            isDark = isDark,
            styleName = "PARTLY_CLOUDY_$time"
        )
    }

    private fun resolveRainStyle(time: TimeOfDay, theme: AppTheme): WeatherCardStyle {
        val isNight = time == TimeOfDay.NIGHT
        val baseColors = if (isNight) {
            listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E293B))
        } else {
            listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF475569))
        }

        val colors = applyThemeAtmosphere(baseColors, theme, time)

        return WeatherCardStyle(
            gradientColors = colors,
            accentColor = Color(0xFF60A5FA),
            icon = Icons.Rounded.WaterDrop,
            effectType = WeatherEffectType.RAINY,
            isDark = true,
            styleName = "RAIN_$time"
        )
    }

    private fun resolveThunderStyle(time: TimeOfDay, theme: AppTheme): WeatherCardStyle {
        val baseColors = listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF1E293B))
        val colors = applyThemeAtmosphere(baseColors, theme, time)

        return WeatherCardStyle(
            gradientColors = colors,
            accentColor = Color(0xFFC084FC),
            icon = Icons.Rounded.Thunderstorm,
            effectType = WeatherEffectType.THUNDER,
            isDark = true,
            styleName = "THUNDER_$time"
        )
    }

    private fun resolveSnowStyle(time: TimeOfDay, theme: AppTheme): WeatherCardStyle {
        val isNight = time == TimeOfDay.NIGHT
        val baseColors = if (isNight) {
            listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF33485F))
        } else {
            listOf(Color(0xFFE0F2FE), Color(0xFFF1F5F9), Color(0xFFFFFFFF))
        }

        val colors = applyThemeAtmosphere(baseColors, theme, time)

        return WeatherCardStyle(
            gradientColors = colors,
            accentColor = if (isNight) Color(0xFF94A3B8) else Color(0xFF0EA5E9),
            icon = Icons.Rounded.AcUnit,
            effectType = WeatherEffectType.SNOW,
            isDark = isNight,
            styleName = "SNOW_$time"
        )
    }

    private fun resolveFogStyle(time: TimeOfDay, theme: AppTheme): WeatherCardStyle {
        val isNight = time == TimeOfDay.NIGHT
        val baseColors = if (isNight) {
            listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
        } else {
            listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1), Color(0xFFE2E8F0))
        }

        val colors = applyThemeAtmosphere(baseColors, theme, time)

        return WeatherCardStyle(
            gradientColors = colors,
            accentColor = Color(0xFFF1F5F9),
            icon = Icons.Rounded.FilterDrama,
            effectType = WeatherEffectType.FOG,
            isDark = true,
            styleName = "FOG_$time"
        )
    }

    private fun resolveDefaultStyle(theme: AppTheme): WeatherCardStyle {
        return WeatherCardStyle(
            gradientColors = listOf(Color(0xFF475569), Color(0xFF64748B)),
            accentColor = Color.White,
            icon = Icons.Rounded.WbCloudy,
            effectType = WeatherEffectType.NONE,
            isDark = true,
            styleName = "DEFAULT"
        )
    }

    private fun applyThemeAtmosphere(colors: List<Color>, theme: AppTheme, time: TimeOfDay): List<Color> {
        return colors.map { color ->
            when (theme) {
                AppTheme.SPRING -> color.lerp(Color(0xFFD1FAE5), 0.1f) // Fresh green hint
                AppTheme.SUMMER -> color.lerp(Color(0xFFFFF7ED), 0.08f) // Warm hint
                AppTheme.AUTUMN -> color.lerp(Color(0xFFFFF7ED), 0.12f).lerp(Color(0xFF78350F), 0.05f) // Amber hint
                AppTheme.WINTER -> color.lerp(Color(0xFFF1F5F9), 0.15f) // Icy hint
                else -> color
            }
        }
    }

    private fun Color.lerp(other: Color, fraction: Float): Color {
        return Color(
            red = red + (other.red - red) * fraction,
            green = green + (other.green - green) * fraction,
            blue = blue + (other.blue - blue) * fraction,
            alpha = alpha + (other.alpha - alpha) * fraction
        )
    }
}

// ── Main Component ───────────────────────────────────────────────────────────

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
    modifier: Modifier = Modifier,
    districtName: String? = null,
    time: LocalTime = LocalTime.now(),
    parallaxOffset: Float = 0f,
    themeViewModel: com.havamania.ui.theme.ThemeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val currentTheme by themeViewModel.currentTheme.collectAsState()
    val condition = remember(weatherCode, isDay) { WeatherStyleMapper.getCondition(weatherCode, isDay) }
    val timeOfDay = remember(time) { resolveTimeOfDay(time.hour) }
    val style = WeatherStyleResolver.resolve(condition, timeOfDay, currentTheme)

    // Log the current hour and resolved style for debugging
    LaunchedEffect(condition, time.hour, currentTheme, style) {
        Log.d("WeatherCardStyle", "condition=$condition, hour=${time.hour}, timeOfDay=$timeOfDay, theme=$currentTheme, styleName=${style.styleName}")
    }

    val context = LocalContext.current
    val isReducedMotion = remember {
        try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f) == 0f
        } catch (e: Exception) { false }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alphaAnim by animateFloatAsState(if (visible) 1f else 0f, tween(500), label = "alpha")
    val scaleAnim by animateFloatAsState(if (visible) 1f else 0.97f, tween(500), label = "scale")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .graphicsLayer {
                alpha = if (isReducedMotion) 1f else alphaAnim
                scaleX = if (isReducedMotion) 1f else scaleAnim
                scaleY = if (isReducedMotion) 1f else scaleAnim
            }
            .clip(RoundedCornerShape(32.dp))
    ) {
        // Layer 1: Background
        WeatherCardBackground(style.gradientColors, isReducedMotion)

        // Layer 2: Weather Effect Layer
        WeatherEffectLayer(style.effectType, style.accentColor, isReducedMotion)

        // Layer 3: Subtle border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 0.5.dp,
                    color = if (style.isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(32.dp)
                )
        )

        // Layer 4: Content
        WeatherHeroContent(
            cityName = cityName,
            districtName = districtName,
            temperature = temperature,
            conditionLabel = conditionLabel,
            high = high,
            low = low,
            feelsLike = feelsLike,
            humidity = humidity,
            windSpeed = windSpeed,
            uvIndex = uvIndex,
            style = style,
            onCityClick = onCityClick,
            isReducedMotion = isReducedMotion
        )
    }
}

@Composable
fun WeatherCardBackground(colors: List<Color>, isReducedMotion: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val offset by if (isReducedMotion) remember { mutableStateOf(0f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse),
            label = "offset"
        )
    }

    val brush = remember(colors, offset) {
        Brush.linearGradient(
            colors = colors,
            start = Offset(0f, 200f * offset),
            end = Offset(1200f, 1000f - (200f * offset))
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(brush))
}

@Composable
fun WeatherEffectLayer(type: WeatherEffectType, accent: Color, isReducedMotion: Boolean) {
    when (type) {
        WeatherEffectType.SUNNY -> SunGlowEffect(accent, isReducedMotion)
        WeatherEffectType.RAINY -> RainEffect(isReducedMotion)
        WeatherEffectType.CLOUDY -> CloudHazeEffect(isReducedMotion)
        WeatherEffectType.NIGHT -> StarFieldEffect(isReducedMotion)
        WeatherEffectType.SNOW -> SnowEffect(isReducedMotion)
        WeatherEffectType.FOG -> FogHazeEffect(isReducedMotion)
        WeatherEffectType.THUNDER -> ThunderEffect(accent, isReducedMotion)
        else -> Unit
    }
}

@Composable
fun SunGlowEffect(accent: Color, isReducedMotion: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "sun")
    val pulse by if (isReducedMotion) remember { mutableStateOf(0.1f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0.05f, targetValue = 0.12f,
            animationSpec = infiniteRepeatable(tween(8000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulse"
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Small directional light from top right edge
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = pulse), Color.Transparent),
                center = Offset(size.width * 0.95f, size.height * 0.05f),
                radius = size.width * 0.5f
            ),
            radius = size.width * 0.5f,
            center = Offset(size.width * 0.95f, size.height * 0.05f)
        )
    }
}

@Composable
fun RainEffect(isReducedMotion: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "rain")
    val progress by if (isReducedMotion) remember { mutableStateOf(0f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
            label = "prog"
        )
    }
    val drops = remember { List(35) { Offset(Random.nextFloat(), Random.nextFloat()) } }

    Canvas(modifier = Modifier.fillMaxSize()) {
        drops.forEach { drop ->
            val x = drop.x * size.width
            val y = if (isReducedMotion) drop.y * size.height else ((drop.y + progress) % 1f) * size.height
            drawLine(
                color = Color.White.copy(alpha = 0.12f),
                start = Offset(x, y),
                end = Offset(x + 1.2.dp.toPx(), y + 12.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
fun CloudHazeEffect(isReducedMotion: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")
    val drift by if (isReducedMotion) remember { mutableStateOf(0f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(45000, easing = LinearEasing), RepeatMode.Restart),
            label = "drift"
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val hazeOpacity = 0.06f
        val hazeColor = Color.White.copy(alpha = hazeOpacity)
        val xOffset = drift * size.width

        // Subtle haze in upper/side regions - no large center circles
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(hazeColor, Color.Transparent),
                startY = 0f,
                endY = size.height * 0.4f
            )
        )

        // Directional soft light from top right
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(size.width * 0.9f, size.height * 0.1f),
                radius = size.width * 0.4f
            ),
            center = Offset(size.width * 0.9f, size.height * 0.1f),
            radius = size.width * 0.4f
        )
    }
}

@Composable
fun StarFieldEffect(isReducedMotion: Boolean) {
    val stars = remember { List(15) { Offset(Random.nextFloat(), Random.nextFloat() * 0.6f) } }

    Canvas(modifier = Modifier.fillMaxSize()) {
        stars.forEach { star ->
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = 0.8.dp.toPx(),
                center = Offset(star.x * size.width, star.y * size.height)
            )
        }
    }
}

@Composable
fun SnowEffect(isReducedMotion: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "snow")
    val progress by if (isReducedMotion) remember { mutableStateOf(0f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
            label = "snow_prog"
        )
    }
    val flakes = remember { List(25) { Offset(Random.nextFloat(), Random.nextFloat()) } }

    Canvas(modifier = Modifier.fillMaxSize()) {
        flakes.forEach { flake ->
            val x = flake.x * size.width
            val y = if (isReducedMotion) flake.y * size.height else ((flake.y + progress) % 1f) * size.height
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = 1.5.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun FogHazeEffect(isReducedMotion: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                startY = 0f,
                endY = size.height * 0.6f
            )
        )
    }
}

@Composable
fun ThunderEffect(accent: Color, isReducedMotion: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "thunder")
    val flash by if (isReducedMotion) remember { mutableStateOf(0f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 10000
                    0f at 0
                    0.4f at 8000
                    0f at 8100
                    0.2f at 8500
                    0f at 8600
                    0f at 10000
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "flash"
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (flash > 0) {
            drawRect(
                color = accent.copy(alpha = flash * 0.15f)
            )
        }
    }
}

@Composable
fun WeatherHeroContent(
    cityName: String,
    temperature: String,
    conditionLabel: String,
    high: String,
    low: String,
    feelsLike: String,
    humidity: String,
    windSpeed: String,
    uvIndex: String,
    style: WeatherCardStyle,
    onCityClick: () -> Unit,
    isReducedMotion: Boolean,
    districtName: String? = null
) {
    val textColor = if (style.isDark) Color.White else Color(0xFF0F172A)
    val secondaryColor = textColor.copy(alpha = 0.7f)

    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatOffset by if (isReducedMotion) remember { mutableStateOf(0f) } else {
        infiniteTransition.animateFloat(
            initialValue = -1.2f, targetValue = 1.2.dp.value,
            animationSpec = infiniteRepeatable(tween(5000, easing = SineEaseInOut), RepeatMode.Reverse),
            label = "y"
        )
    }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.clickable { onCityClick() }) {
                Text(
                    text = (districtName ?: cityName).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = secondaryColor
                    )
                )
                if (districtName != null) {
                    Text(
                        text = cityName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = secondaryColor.copy(alpha = 0.5f)
                        )
                    )
                }
                Text(
                    conditionLabel,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                )
            }
            Box(modifier = Modifier.offset(y = floatOffset.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(36.dp).blur(12.dp).background(style.accentColor.copy(alpha = 0.15f), CircleShape))
                Icon(style.icon, null, tint = style.accentColor, modifier = Modifier.size(30.dp))
            }
        }

        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(temperature, style = MaterialTheme.typography.displayLarge.copy(fontSize = 100.sp, fontWeight = FontWeight.W100, letterSpacing = (-4).sp, color = textColor))
            Text("Hissedilen $feelsLike", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = secondaryColor))
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(54.dp),
            color = if (style.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 0.5.dp,
                color = if (style.isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)
            )
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                HeroInfoItem(Icons.Rounded.WaterDrop, humidity, textColor)
                HeroInfoItem(Icons.Rounded.Air, windSpeed, textColor)
                HeroInfoItem(Icons.Rounded.WbSunny, "UV $uvIndex", textColor)
            }
        }
    }
}

@Composable
fun HeroInfoItem(icon: ImageVector, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = color))
    }
}

val SineEaseInOut = androidx.compose.animation.core.Easing { fraction ->
    (-(Math.cos(Math.PI * fraction) - 1) / 2).toFloat()
}
