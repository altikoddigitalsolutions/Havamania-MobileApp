package com.havamania.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp

internal fun themeColorContrast(first: Color, second: Color): Float {
    val a = first.luminance(); val b = second.luminance()
    return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
}

/** Keep the preferred foreground when readable; otherwise choose an opaque readable alternative. */
internal fun themeFillContentColor(fill: Color, preferred: Color): Color =
    if (themeColorContrast(fill, preferred) >= 4.5f) preferred
    else listOf(Color.Black, Color.White).maxBy { themeColorContrast(fill, it) }

/** Preserve the palette hue, deepening/lightening only when text actions need separation from the page. */
internal fun themeActionColor(accent: Color, background: Color): Color {
    if (themeColorContrast(accent, background) >= 4.5f) return accent
    val target = if (background.luminance() > 0.5f) Color.Black else Color.White
    return (1..20).map { lerp(accent, target, it / 20f) }
        .firstOrNull { themeColorContrast(it, background) >= 4.5f } ?: target
}
internal fun materialColorsFor(colors: HavamaniaColors): ColorScheme {
    val primary = themeActionColor(colors.accent, colors.background)
    val secondary = themeActionColor(colors.info, colors.background)
    val tertiary = themeActionColor(colors.success, colors.background)
    val error = themeActionColor(colors.error, colors.background)
    val surface = colors.surface
    val container = colors.buttonSecondary.compositeOver(surface)
    val errorContainer = colors.error.copy(alpha = 0.12f).compositeOver(surface)
    val base = if (colors.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary, onPrimary = themeFillContentColor(primary, colors.onAccent),
        primaryContainer = container, onPrimaryContainer = colors.textPrimary,
        inversePrimary = themeActionColor(colors.accent, colors.textPrimary),
        secondary = secondary, onSecondary = themeFillContentColor(secondary, colors.onAccent),
        secondaryContainer = container, onSecondaryContainer = colors.textPrimary,
        tertiary = tertiary, onTertiary = themeFillContentColor(tertiary, colors.onAccent),
        tertiaryContainer = container, onTertiaryContainer = colors.textPrimary,
        background = colors.background, onBackground = colors.textPrimary,
        surface = surface, onSurface = colors.textPrimary,
        surfaceVariant = container, onSurfaceVariant = colors.textSecondary,
        surfaceTint = colors.accent,
        inverseSurface = colors.textPrimary, inverseOnSurface = colors.background,
        error = error, onError = themeFillContentColor(error, colors.onAccent),
        errorContainer = errorContainer, onErrorContainer = colors.textPrimary,
        outline = colors.textMuted.compositeOver(surface), outlineVariant = colors.border.compositeOver(surface),
        surfaceBright = surface, surfaceDim = colors.background,
        surfaceContainerLowest = colors.background, surfaceContainerLow = surface,
        surfaceContainer = surface, surfaceContainerHigh = container, surfaceContainerHighest = container
    )
}