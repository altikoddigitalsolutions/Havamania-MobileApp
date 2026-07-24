package com.havamania.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Havamania Design System - Core Tokens
 */

/**
 * Premium Style Helpers
 */
@Composable
fun Modifier.havamaniaScreenBackground(): Modifier = this
    .fillMaxSize()
    .background(
        Brush.verticalGradient(
            colors = HavamaniaTheme.colors.gradientPrimary
        )
    )

@Composable
fun Modifier.havamaniaGlassCard(
    cornerRadius: Dp = HavamaniaTheme.styles.cardCornerRadius,
    blurAmount: Dp = HavamaniaTheme.styles.glassBlur
): Modifier = this
    .shadow(HavamaniaTheme.styles.elevation, RoundedCornerShape(cornerRadius))
    .clip(RoundedCornerShape(cornerRadius))
    .background(HavamaniaTheme.colors.surfaceGlass)
    .border(HavamaniaTheme.styles.cardBorderWidth, HavamaniaTheme.colors.border, RoundedCornerShape(cornerRadius))

@Composable
fun Modifier.havamaniaPrimaryButton(): Modifier = this
    .clip(RoundedCornerShape(16.dp))
    .background(HavamaniaTheme.colors.accent)

@Composable
fun Modifier.havamaniaChipStyle(): Modifier = this
    .clip(CircleShape)
    .background(HavamaniaTheme.colors.surfaceGlass)
    .border(1.dp, HavamaniaTheme.colors.border, CircleShape)

@Composable
fun Modifier.havamaniaBottomNavStyle(): Modifier = this
    .background(HavamaniaTheme.colors.surfaceGlass)
    .border(
        width = 1.dp,
        color = HavamaniaTheme.colors.border,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    )
    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp))

@Immutable
data class HavamaniaColors(
    val gradientPrimary: List<Color>,
    val gradientSecondary: List<Color>,
    val accent: Color,
    val onAccent: Color,
    val background: Color,
    val surface: Color,
    val surfaceGlass: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val divider: Color,
    val glow: Color,
    val error: Color,
    val success: Color,
    val warning: Color,
    val shadow: Color = Color.Black.copy(alpha = 0.2f),
    val buttonGradient: List<Color>? = null,
    val cardGradient: List<Color>? = null,
    val isDark: Boolean
)

@Immutable
data class HavamaniaStyles(
    val glassBlur: Dp = 16.dp,
    val cardCornerRadius: Dp = 24.dp, // Modern standard
    val cardBorderWidth: Dp = 1.dp,
    val elevation: Dp = 8.dp,

    // Standardized Spacing (M3)
    val spacingNone: Dp = 0.dp,
    val spacingExtraSmall: Dp = 4.dp,
    val spacingSmall: Dp = 8.dp,
    val spacingMedium: Dp = 16.dp,
    val spacingLarge: Dp = 24.dp,
    val spacingExtraLarge: Dp = 32.dp,
    val pagePadding: Dp = 20.dp,

    // Standardized Radius
    val radiusSmall: Dp = 12.dp,
    val radiusMedium: Dp = 20.dp,
    val radiusLarge: Dp = 24.dp,
    val radiusExtraLarge: Dp = 32.dp,
    val radiusFull: Dp = 9999.dp
)

/**
 * Global CompositionLocals
 */
val LocalHavamaniaColors = staticCompositionLocalOf<HavamaniaColors> {
    error("No HavamaniaColors provided")
}

val LocalHavamaniaStyles = staticCompositionLocalOf { HavamaniaStyles() }

/**
 * Easy access object
 */
object HavamaniaTheme {
    val colors: HavamaniaColors
        @androidx.compose.runtime.Composable
        @androidx.compose.runtime.ReadOnlyComposable
        get() = LocalHavamaniaColors.current

    val styles: HavamaniaStyles
        @androidx.compose.runtime.Composable
        @androidx.compose.runtime.ReadOnlyComposable
        get() = LocalHavamaniaStyles.current
}

/**
 * Theme Factory - Generates tokens based on ThemeType and Weather
 */
object ThemeFactory {
    fun createColors(type: AppTheme, weatherCode: Int? = null): HavamaniaColors {
        return when (type) {
            AppTheme.DARK -> HavamaniaColors(
                gradientPrimary = listOf(Color(0xFF0F172A), Color(0xFF020617)),
                gradientSecondary = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
                accent = Color(0xFF00C2FF),
                onAccent = Color.White,
                background = Color(0xFF020617),
                surface = Color(0xFF1E293B),
                surfaceGlass = Color(0xFF1E293B).copy(alpha = 0.7f),
                textPrimary = Color(0xFFEAF2FF),
                textSecondary = Color(0xFFA3B3C9),
                textMuted = Color(0xFF64748B),
                border = Color.White.copy(alpha = 0.08f),
                divider = Color.White.copy(alpha = 0.05f),
                glow = Color(0xFF00C2FF).copy(alpha = 0.15f),
                error = Color(0xFFEF4444),
                success = Color(0xFF10B981),
                warning = Color(0xFFF59E0B),
                shadow = Color.Black.copy(alpha = 0.4f),
                buttonGradient = listOf(Color(0xFF00C2FF), Color(0xFF0077FF)),
                isDark = true
            )
            AppTheme.LIGHT -> HavamaniaColors(
                gradientPrimary = listOf(Color(0xFFE0F2FE), Color(0xFFF8FAFC)),
                gradientSecondary = listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0)),
                accent = Color(0xFF0284C7),
                onAccent = Color.White,
                background = Color(0xFFF8FAFC),
                surface = Color.White,
                surfaceGlass = Color.White.copy(alpha = 0.85f),
                textPrimary = Color(0xFF0F172A),
                textSecondary = Color(0xFF475569),
                textMuted = Color(0xFF94A3B8),
                border = Color(0xFFE2E8F0),
                divider = Color(0xFFF1F5F9),
                glow = Color(0xFF38BDF8).copy(alpha = 0.1f),
                error = Color(0xFFDC2626),
                success = Color(0xFF059669),
                warning = Color(0xFFD97706),
                shadow = Color(0xFF0F172A).copy(alpha = 0.05f),
                buttonGradient = listOf(Color(0xFF0EA5E9), Color(0xFF0284C7)),
                isDark = false
            )
            AppTheme.SPRING -> HavamaniaColors(
                gradientPrimary = listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7)),
                gradientSecondary = listOf(Color(0xFFBBF7D0), Color(0xFF86EFAC)),
                accent = Color(0xFF16A34A),
                onAccent = Color.White,
                background = Color(0xFFF0FDF4),
                surface = Color.White,
                surfaceGlass = Color.White.copy(alpha = 0.8f),
                textPrimary = Color(0xFF14532D),
                textSecondary = Color(0xFF166534),
                textMuted = Color(0xFF34D399).copy(alpha = 0.7f),
                border = Color(0xFFBBF7D0).copy(alpha = 0.5f),
                divider = Color(0xFFDCFCE7),
                glow = Color(0xFF4ADE80).copy(alpha = 0.2f),
                error = Color(0xFFDC2626),
                success = Color(0xFF16A34A),
                warning = Color(0xFFF59E0B),
                shadow = Color(0xFF14532D).copy(alpha = 0.05f),
                buttonGradient = listOf(Color(0xFF4ADE80), Color(0xFF16A34A)),
                isDark = false
            )
            AppTheme.SUMMER -> HavamaniaColors(
                gradientPrimary = listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5)),
                gradientSecondary = listOf(Color(0xFFFFD8A8), Color(0xFFFDBA74)),
                accent = Color(0xFFEA580C),
                onAccent = Color.White,
                background = Color(0xFFFFF7ED),
                surface = Color.White,
                surfaceGlass = Color.White.copy(alpha = 0.8f),
                textPrimary = Color(0xFF431407),
                textSecondary = Color(0xFF7C2D12),
                textMuted = Color(0xFF9A3412).copy(alpha = 0.6f),
                border = Color(0xFFFFD8A8).copy(alpha = 0.5f),
                divider = Color(0xFFFFEDD5),
                glow = Color(0xFFFBBF24).copy(alpha = 0.25f),
                error = Color(0xFFDC2626),
                success = Color(0xFF059669),
                warning = Color(0xFFEA580C),
                shadow = Color(0xFF431407).copy(alpha = 0.05f),
                buttonGradient = listOf(Color(0xFFF97316), Color(0xFFEA580C)),
                isDark = false
            )
            AppTheme.AUTUMN -> HavamaniaColors(
                gradientPrimary = listOf(Color(0xFF451A03), Color(0xFF78350F)),
                gradientSecondary = listOf(Color(0xFF92400E), Color(0xFFB45309)),
                accent = Color(0xFFF97316),
                onAccent = Color.White,
                background = Color(0xFF451A03),
                surface = Color(0xFF78350F),
                surfaceGlass = Color(0xFF78350F).copy(alpha = 0.75f),
                textPrimary = Color(0xFFFFEDD5),
                textSecondary = Color(0xFFFED7AA),
                textMuted = Color(0xFFD97706).copy(alpha = 0.7f),
                border = Color.White.copy(alpha = 0.08f),
                divider = Color.White.copy(alpha = 0.05f),
                glow = Color(0xFFF59E0B).copy(alpha = 0.2f),
                error = Color(0xFFEF4444),
                success = Color(0xFF10B981),
                warning = Color(0xFFF97316),
                shadow = Color.Black.copy(alpha = 0.3f),
                buttonGradient = listOf(Color(0xFFFB923C), Color(0xFFF97316)),
                isDark = true
            )
            AppTheme.WINTER -> HavamaniaColors(
                gradientPrimary = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
                gradientSecondary = listOf(Color(0xFF334155), Color(0xFF1E293B)),
                accent = Color(0xFF60A5FA),
                onAccent = Color.White,
                background = Color(0xFF0F172A),
                surface = Color(0xFF1E293B),
                surfaceGlass = Color(0xFF1E293B).copy(alpha = 0.7f),
                textPrimary = Color(0xFFF1F5F9),
                textSecondary = Color(0xFFCBD5E1),
                textMuted = Color(0xFF94A3B8).copy(alpha = 0.7f),
                border = Color.White.copy(alpha = 0.08f),
                divider = Color.White.copy(alpha = 0.05f),
                glow = Color(0xFF3B82F6).copy(alpha = 0.2f),
                error = Color(0xFFEF4444),
                success = Color(0xFF10B981),
                warning = Color(0xFFF59E0B),
                shadow = Color.Black.copy(alpha = 0.4f),
                buttonGradient = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                isDark = true
            )
            AppTheme.AUTO -> createColors(ThemeManager.getSeasonalTheme(java.time.LocalDate.now().monthValue))
        }
    }
}
