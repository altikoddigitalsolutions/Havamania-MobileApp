package com.havamania.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

/**
 * Tasarım Sistemi Sabitleri
 */
object HavamaniaDesign {
    val CardCornerRadius = 28.dp
    val SmallCardCornerRadius = 20.dp
    val SpacingMedium = 16.dp
    val SpacingLarge = 24.dp
    val CardBorderWidth = 1.dp
    val CardAlpha = 0.6f
}

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
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
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
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

    val finalTheme = if (currentThemeSelection == AppTheme.AUTO) {
        val weatherData = (weatherUiState as? com.havamania.WeatherUiState.Success)?.data
        ThemeManager.getAutoTheme(weatherData)
    } else {
        currentThemeSelection
    }

    val targetColorScheme = ThemeManager.getColorScheme(finalTheme)

    // PREMIUM GEÇİŞ ANİMASYONU
    val animatedColorScheme = targetColorScheme.copy(
        primary = animateColor(targetColorScheme.primary),
        onPrimary = animateColor(targetColorScheme.onPrimary),
        primaryContainer = animateColor(targetColorScheme.primaryContainer),
        onPrimaryContainer = animateColor(targetColorScheme.onPrimaryContainer),
        secondary = animateColor(targetColorScheme.secondary),
        onSecondary = animateColor(targetColorScheme.onSecondary),
        secondaryContainer = animateColor(targetColorScheme.secondaryContainer),
        onSecondaryContainer = animateColor(targetColorScheme.onSecondaryContainer),
        tertiary = animateColor(targetColorScheme.tertiary),
        onTertiary = animateColor(targetColorScheme.onTertiary),
        tertiaryContainer = animateColor(targetColorScheme.tertiaryContainer),
        onTertiaryContainer = animateColor(targetColorScheme.onTertiaryContainer),
        background = animateColor(targetColorScheme.background),
        onBackground = animateColor(targetColorScheme.onBackground),
        surface = animateColor(targetColorScheme.surface),
        onSurface = animateColor(targetColorScheme.onSurface),
        surfaceVariant = animateColor(targetColorScheme.surfaceVariant),
        onSurfaceVariant = animateColor(targetColorScheme.onSurfaceVariant),
        outline = animateColor(targetColorScheme.outline),
        error = animateColor(targetColorScheme.error),
        onError = animateColor(targetColorScheme.onError)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = animatedColorScheme.background.toArgb()
            window.navigationBarColor = animatedColorScheme.background.toArgb()

            val isDark = finalTheme == AppTheme.DARK
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = AppTypography,
        content = content
    )
}

@Composable
private fun animateColor(targetColor: Color) =
    animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 600),
        label = "color_transition"
    ).value
