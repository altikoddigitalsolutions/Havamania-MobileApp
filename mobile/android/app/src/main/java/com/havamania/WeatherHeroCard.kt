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
import androidx.compose.ui.geometry.CornerRadius
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
    val textColor: Color,
    val accentColor: Color,
    val mainIcon: ImageVector,
    val cloudDensity: Int,
    val effectType: VisualEffectType,
    val sunMoonPosition: Offset,
    val isDark: Boolean,
    val cloudColor: Color = Color.White,
    val phase: DayPhase = DayPhase.DAY,
    val sunOpacity: Float = 1.0f,
    val rainIntensity: Float = 1.0f,
    val atmosphereProgress: Float = 0.5f
)

object WeatherStyleResolver {
    @Composable
    fun resolveSpec(
        weatherCode: Int,
        isDay: Boolean,
        phase: DayPhase,
        altitude: Float,
        tempValue: Float,
        theme: AppTheme
    ): WeatherCardVisualSpec {
        val condition = remember(weatherCode, isDay) { WeatherMapper.mapWeatherCodeToCondition(weatherCode, isDay) }
        val progress = ((altitude + 18f) / (90f + 18f)).coerceIn(0f, 1f)

        // 1. Unified Dynamic Gradients - AAA Cinematic Realism
        val baseColors = when (phase) {
            DayPhase.DAWN -> {
                if (condition is WeatherCondition.Overcast || condition is WeatherCondition.Cloudy || condition is WeatherCondition.PartlyCloudy) {
                    listOf(Color(0xFF334155), Color(0xFF475569), Color(0xFFFBCFE8)) // Misty Dawn
                } else {
                    listOf(Color(0xFF2D1B69), Color(0xFFDB2777), Color(0xFFF59E0B))
                }
            }
            DayPhase.SUNSET, DayPhase.GOLDEN_HOUR, DayPhase.DUSK -> {
                if (condition is WeatherCondition.Overcast || condition is WeatherCondition.Cloudy || condition is WeatherCondition.PartlyCloudy) {
                    listOf(Color(0xFF2C3E50), Color(0xFF4A5568), Color(0xFFFED7AA))
                } else {
                    listOf(Color(0xFF1E293B), Color(0xFF7C2D12), Color(0xFFF97316))
                }
            }
            DayPhase.MORNING, DayPhase.DAY -> {
                when {
                    condition is WeatherCondition.Rain || condition is WeatherCondition.Thunderstorm ->
                        listOf(Color(0xFF2D3748), Color(0xFF4A5568), Color(0xFF718096))
                    condition is WeatherCondition.Snow ->
                        listOf(Color(0xFFE2E8F0), Color(0xFFF8FAFC), Color(0xFFCBD5E1))
                    condition is WeatherCondition.Fog ->
                        listOf(Color(0xFF718096), Color(0xFFA0AEC0), Color(0xFFCBD5E1))
                    condition is WeatherCondition.PartlyCloudy ->
                        listOf(Color(0xFF4A5568), Color(0xFF718096), Color(0xFFA0AEC0))
                    condition is WeatherCondition.Overcast || condition is WeatherCondition.Cloudy ->
                        listOf(Color(0xFF5E6B7D), Color(0xFF6E7B8B), Color(0xFF4B5565))
                    else -> {
                        if (tempValue > 28f) listOf(Color(0xFF0284C7), Color(0xFF0EA5E9), Color(0xFFFDE047))
                        else listOf(Color(0xFF0369A1), Color(0xFF0EA5E9), Color(0xFF7DD3FC))
                    }
                }
            }
            DayPhase.NIGHT, DayPhase.BLUE_HOUR, DayPhase.TWILIGHT -> {
                if (condition is WeatherCondition.Overcast || condition is WeatherCondition.Cloudy || condition is WeatherCondition.PartlyCloudy || condition is WeatherCondition.Fog || condition is WeatherCondition.Rain) {
                    listOf(Color(0xFF080C14), Color(0xFF1E293B), Color(0xFF334155))
                } else {
                    listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E1B4B))
                }
            }
            else -> listOf(Color(0xFF0EA5E9), Color(0xFF38BDF8), Color(0xFFBAE6FD))
        }

        val effectType = when {
            condition is WeatherCondition.Thunderstorm -> VisualEffectType.THUNDER
            condition is WeatherCondition.Rain -> VisualEffectType.RAIN
            condition is WeatherCondition.Snow -> VisualEffectType.SNOW
            condition is WeatherCondition.Fog -> VisualEffectType.FOG
            altitude <= 0f -> VisualEffectType.MOON
            else -> VisualEffectType.SUN
        }

        val cloudDensity = when (condition) {
            is WeatherCondition.Clear, is WeatherCondition.NightClear -> 0
            is WeatherCondition.MostlySunny -> 1
            is WeatherCondition.PartlyCloudy -> 2
            is WeatherCondition.Cloudy -> 4
            is WeatherCondition.Overcast -> 8
            else -> 3
        }

        val rainIntensity = when (condition) {
            is WeatherCondition.Thunderstorm -> 1.5f
            is WeatherCondition.Rain -> 0.8f
            else -> 0.4f
        }

        val sunY = 0.7f - ((altitude.coerceAtLeast(0f) / 90f) * 0.55f)
        val sunMoonPos = Offset(0.85f, sunY)

        val isNight = altitude <= 0
        val isDark = isNight || condition is WeatherCondition.Rain || condition is WeatherCondition.Thunderstorm || condition is WeatherCondition.Overcast
        val textColor = if (isDark || altitude < 10) Color.White else Color(0xFF0F172A)

        val accentColor = if (isNight) Color(0xFF93C5FD) else Color(0xFFFDE047)

        val iconName = WeatherMapper.getWeatherIconName(weatherCode, !isNight)
        val mainIcon = WeatherMapper.getIconFromName(iconName)

        return WeatherCardVisualSpec(
            gradientColors = baseColors,
            textColor = textColor,
            accentColor = accentColor,
            mainIcon = mainIcon,
            cloudDensity = cloudDensity,
            effectType = effectType,
            sunMoonPosition = sunMoonPos,
            isDark = isDark,
            cloudColor = Color.White,
            phase = phase,
            sunOpacity = (altitude / 10f).coerceIn(0.1f, 1.0f),
            rainIntensity = rainIntensity,
            atmosphereProgress = progress
        )
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
    fullTime: String? = null,
    sunriseTime: String? = null,
    sunsetTime: String? = null,
    parallaxOffset: Float = 0f,
    solarAltitude: Float = 45f,
    themeViewModel: com.havamania.ui.theme.ThemeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val currentTheme by themeViewModel.currentTheme.collectAsState()

    val phase = remember(time, fullTime, sunriseTime, sunsetTime) {
        val sunrise = try { LocalTime.parse(sunriseTime) } catch (e: Exception) { LocalTime.of(6, 30) }
        val sunset = try { LocalTime.parse(sunsetTime) } catch (e: Exception) { LocalTime.of(19, 30) }
        val dateTime = if (fullTime != null) {
            try { LocalDateTime.parse(fullTime) } catch (e: Exception) { LocalDateTime.now().with(time) }
        } else {
            LocalDateTime.now().with(time)
        }
        WeatherMapper.getDayPhase(dateTime, sunrise, sunset)
    }

    val tempVal = remember(temperature) { temperature.filter { it.isDigit() || it == '-' }.toFloatOrNull() ?: 20f }
    val spec = WeatherStyleResolver.resolveSpec(weatherCode, isDay, phase, solarAltitude, tempVal, currentTheme)

    val context = LocalContext.current
    val isReducedMotion = remember {
        try { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f) == 0f } catch (e: Exception) { false }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(targetValue = if (visible) 1f else 0.98f, animationSpec = tween(1000))
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(1200))

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
            .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(32.dp))
    ) {
        Crossfade(targetState = spec, animationSpec = tween(1500), label = "hero_bg") { targetSpec ->
            LiveBackgroundLayer(spec = targetSpec)
        }

        Box(modifier = Modifier.matchParentSize().clip(RoundedCornerShape(32.dp))) {
            WeatherEffectLayer(spec = spec, isAnimationEnabled = !isReducedMotion)
        }

        Box(modifier = Modifier.fillMaxSize().zIndex(100f)) {
            PremiumWeatherContent(
                cityName = cityName, districtName = districtName, conditionLabel = conditionLabel, temperature = temperature,
                feelsLike = feelsLike, humidity = humidity, windSpeed = windSpeed, uvIndex = uvIndex,
                spec = spec, unreadCount = unreadCount, weatherIcon = spec.mainIcon,
                onCityClick = onCityClick, onNotificationsClick = onNotificationsClick
            )
        }
    }
}

@Composable
fun LiveBackgroundLayer(spec: WeatherCardVisualSpec) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val pulse by infiniteTransition.animateFloat(0.02f, 0.06f, infiniteRepeatable(tween(12000, easing = SineEaseInOut), RepeatMode.Reverse))

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sunCenter = Offset(size.width * spec.sunMoonPosition.x, size.height * spec.sunMoonPosition.y)

            // 1. Base Sky Gradient
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to spec.gradientColors.first(),
                    0.6f to spec.gradientColors.getOrElse(1) { spec.gradientColors.first() },
                    1.0f to spec.gradientColors.last()
                )
            )

            // 2. Horizon Glow & Atmospheric Scattering
            val horizonColor = when (spec.phase) {
                DayPhase.DAWN -> Color(0xFFFBCFE8).copy(0.12f)
                DayPhase.GOLDEN_HOUR, DayPhase.SUNSET, DayPhase.DUSK -> Color(0xFFFED7AA).copy(0.15f)
                DayPhase.NIGHT, DayPhase.BLUE_HOUR, DayPhase.TWILIGHT, DayPhase.EVENING -> Color(0xFF1E293B).copy(0.08f)
                else -> Color.White.copy(0.05f)
            }

            drawRect(
                brush = Brush.verticalGradient(0.7f to Color.Transparent, 1.0f to horizonColor)
            )

            // 3. Volumetric Ambient Lighting (Animated)
            drawCircle(
                brush = Brush.radialGradient(0.0f to Color.White.copy(pulse), 1.0f to Color.Transparent, center = sunCenter, radius = size.width * 1.5f),
                center = sunCenter, radius = size.width * 1.5f, blendMode = BlendMode.Overlay
            )

            // 4. Cinematic Vignette
            drawRect(
                brush = Brush.radialGradient(0.0f to Color.Transparent, 0.7f to Color.Transparent, 1.0f to Color.Black.copy(0.12f), center = center, radius = size.width * 1.2f)
            )
        }
    }
}

@Composable
fun PremiumWeatherContent(
    cityName: String, districtName: String?, conditionLabel: String, temperature: String, feelsLike: String, humidity: String, windSpeed: String, uvIndex: String,
    spec: WeatherCardVisualSpec, unreadCount: Int, weatherIcon: ImageVector, onCityClick: () -> Unit, onNotificationsClick: () -> Unit
) {
    val textColor = spec.textColor
    val secondaryColor = textColor.copy(alpha = 0.7f)
    val isNight = spec.isDark && spec.effectType == VisualEffectType.MOON

    Column(modifier = Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.clip(RoundedCornerShape(12.dp))
                .background(if (spec.isDark) Color.White.copy(0.12f) else Color.Black.copy(0.06f))
                .clickable { onCityClick() }
                .padding(10.dp, 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, null, tint = spec.accentColor.copy(0.9f), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(8.dp))
                Text((districtName ?: cityName).uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = textColor))
                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = secondaryColor, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PremiumNotificationButton(unreadCount, spec.isDark, textColor, onNotificationsClick)
                Spacer(Modifier.width(14.dp))
                Icon(weatherIcon, null, tint = spec.accentColor, modifier = Modifier.size(30.dp))
            }
        }
        Spacer(Modifier.weight(0.6f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(temperature, style = MaterialTheme.typography.displayLarge.copy(fontSize = 105.sp, fontWeight = FontWeight.W100, letterSpacing = (-4).sp, color = textColor))
            Text(conditionLabel.uppercase(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.5.sp, color = textColor.copy(0.9f)))
            Text("HİSSEDİLEN $feelsLike", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp, color = secondaryColor))
        }
        Spacer(Modifier.weight(1f))

        // GLASSMORPHISM INFO BAR
        Surface(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            color = Color.White.copy(alpha = 0.08f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val uvVal = if (isNight) "0" else uvIndex
                val uvIcon = if (isNight) Icons.Rounded.Brightness3 else Icons.Rounded.WbSunny

                listOf(Icons.Rounded.WaterDrop to humidity, Icons.Rounded.Air to windSpeed, uvIcon to "UV $uvVal").forEach { (icon, valStr) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, tint = textColor.copy(0.5f), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(valStr, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold), color = textColor)
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumNotificationButton(unreadCount: Int, isDark: Boolean, tint: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.05f)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Notifications, null, tint = tint.copy(0.85f), modifier = Modifier.size(20.dp))
        if (unreadCount > 0) Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(9.dp).clip(CircleShape).background(Color(0xFF3B82F6)).border(1.5.dp, if (isDark) Color(0xFF1E293B) else Color.White, CircleShape))
    }
}
