package com.havamania.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.WeatherViewModel
import com.havamania.WeatherUiState
import java.time.LocalDateTime
import kotlinx.coroutines.delay

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W200,
        fontSize = 96.sp,
        letterSpacing = (-4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        letterSpacing = (-1).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 1.5.sp
    )
)

@Composable
fun HavamaniaTheme(
    themeViewModel: ThemeViewModel = viewModel(),
    weatherViewModel: WeatherViewModel = viewModel(),
    content: @Composable () -> Unit
) {
    val currentThemeSelection by themeViewModel.currentTheme.collectAsState()
    val weatherUiState by weatherViewModel.uiState.collectAsState()

    // Real-time time tracking for Auto Theme
    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(currentThemeSelection) {
        if (currentThemeSelection == AppTheme.AUTO) {
            while (true) {
                currentDateTime = LocalDateTime.now()
                kotlinx.coroutines.delay(60000) // Check every minute
            }
        }
    }

    val finalTheme = if (currentThemeSelection == AppTheme.AUTO) {
        val isDay = if (weatherUiState is WeatherUiState.Success) {
            val data = (weatherUiState as WeatherUiState.Success).data
            val sunrise = try { java.time.LocalTime.parse(data.sunriseTime) } catch (e: Exception) { java.time.LocalTime.of(6, 30) }
            val sunset = try { java.time.LocalTime.parse(data.sunsetTime) } catch (e: Exception) { java.time.LocalTime.of(19, 30) }
            val now = java.time.LocalTime.now()
            !now.isBefore(sunrise) && now.isBefore(sunset)
        } else {
            val hour = currentDateTime.hour
            hour in 6..18
        }
        ThemeManager.getAutoTheme(currentDateTime.monthValue, isDay)
    } else {
        currentThemeSelection
    }

    // Get base colors from factory
    val targetColors = ThemeFactory.createColors(finalTheme)

    // Premium Color Animations
    val animatedAccent = animateThemeColor(targetColors.accent)
    val animatedOnAccent = animateThemeColor(targetColors.onAccent)
    val animatedBackground = animateThemeColor(targetColors.background)
    val animatedSurface = animateThemeColor(targetColors.surface)
    val animatedSurfaceGlass = animateThemeColor(targetColors.surfaceGlass)
    val animatedTextPrimary = animateThemeColor(targetColors.textPrimary)
    val animatedTextSecondary = animateThemeColor(targetColors.textSecondary)
    val animatedTextMuted = animateThemeColor(targetColors.textMuted)
    val animatedBorder = animateThemeColor(targetColors.border)
    val animatedDivider = animateThemeColor(targetColors.divider)
    val animatedGlow = animateThemeColor(targetColors.glow)
    val animatedError = animateThemeColor(targetColors.error)
    val animatedSuccess = animateThemeColor(targetColors.success)
    val animatedWarning = animateThemeColor(targetColors.warning)

    // Manual animation for lists to avoid Composable in map issues
    val animatedGradientPrimary = listOf(
        animateThemeColor(targetColors.gradientPrimary[0]),
        animateThemeColor(targetColors.gradientPrimary.last())
    )
    val animatedGradientSecondary = listOf(
        animateThemeColor(targetColors.gradientSecondary[0]),
        animateThemeColor(targetColors.gradientSecondary.last())
    )

    val animatedColors = HavamaniaColors(
        gradientPrimary = animatedGradientPrimary,
        gradientSecondary = animatedGradientSecondary,
        accent = animatedAccent,
        onAccent = animatedOnAccent,
        background = animatedBackground,
        surface = animatedSurface,
        surfaceGlass = animatedSurfaceGlass,
        textPrimary = animatedTextPrimary,
        textSecondary = animatedTextSecondary,
        textMuted = animatedTextMuted,
        border = animatedBorder,
        divider = animatedDivider,
        glow = animatedGlow,
        error = animatedError,
        success = animatedSuccess,
        warning = animatedWarning,
        isDark = targetColors.isDark
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = animatedColors.gradientPrimary[0].toArgb()
            window.navigationBarColor = animatedColors.gradientPrimary.last().toArgb()

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !animatedColors.isDark
            insetsController.isAppearanceLightNavigationBars = !animatedColors.isDark
        }
    }

    CompositionLocalProvider(
        LocalHavamaniaColors provides animatedColors,
        LocalHavamaniaStyles provides HavamaniaStyles()
    ) {
        MaterialTheme(
            colorScheme = ThemeManager.getColorScheme(finalTheme),
            typography = AppTypography,
            content = content
        )
    }
}

@Composable
private fun animateThemeColor(target: Color): Color {
    return animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 800),
        label = "theme_color"
    ).value
}
