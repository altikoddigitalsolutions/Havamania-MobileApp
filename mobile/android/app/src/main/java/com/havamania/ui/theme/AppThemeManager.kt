package com.havamania.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.havamania.WeatherData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppTheme(val title: String) {
    AUTO("Otomatik"), LIGHT("Açık Tema"), DARK("Koyu Tema"), SPRING("İlkbahar"), SUMMER("Yaz"), AUTUMN("Sonbahar"), WINTER("Kış")
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Veri tipine göre dinamik renkleri yöneten yardımcı sınıf
 */
object WeatherDataColors {
    // Tema bağımsız temel renkler (Semantik)
    private val HumidityMavi = Color(0xFF38BDF8)
    private val UVTuruncu = Color(0xFFFBBF24)
    private val WindGri = Color(0xFF94A3B8)
    private val PressureMor = Color(0xFFA78BFA)
    private val FeelsKirmizi = Color(0xFFFB7185)

    /**
     * Seçili temaya göre veri tipinin vurgu rengini optimize eder
     */
    fun getAccentColor(type: String, isDark: Boolean): Color {
        val baseColor = when (type.lowercase()) {
            "nem", "humidity" -> HumidityMavi
            "uv", "uv indeksi" -> UVTuruncu
            "rüzgar", "wind" -> WindGri
            "basınç", "pressure" -> PressureMor
            "hissedilen", "feels_like" -> FeelsKirmizi
            else -> Color.Gray
        }

        // Açık temada renkleri biraz daha doygun/koyu yapıyoruz ki beyaz üzerinde okunsun
        return if (!isDark) {
            when (baseColor) {
                HumidityMavi -> Color(0xFF0284C7)
                UVTuruncu -> Color(0xFFD97706)
                PressureMor -> Color(0xFF7C3AED)
                FeelsKirmizi -> Color(0xFFE11D48)
                else -> baseColor
            }
        } else {
            baseColor
        }
    }
}

data class ThemeVisuals(
    val heroGradient: Brush,
    val aiAccent: Color,
    val progressGradient: Brush,
    val selectionAlpha: Float = 0.15f
)

object ThemeManager {
    private val THEME_KEY = stringPreferencesKey("app_theme")

    suspend fun saveTheme(context: Context, theme: AppTheme) = context.dataStore.edit { it[THEME_KEY] = theme.name }

    fun getTheme(context: Context): Flow<AppTheme> = context.dataStore.data.map {
        AppTheme.valueOf(it[THEME_KEY] ?: AppTheme.DARK.name)
    }

    fun getAutoTheme(weatherData: WeatherData?): AppTheme {
        if (weatherData == null) return AppTheme.DARK
        val condition = weatherData.condition.lowercase()
        return when {
            condition.contains("karlı") -> AppTheme.WINTER
            condition.contains("güneşli") || condition.contains("açık") -> AppTheme.SUMMER
            condition.contains("yağmurlu") -> AppTheme.DARK
            condition.contains("bulutlu") -> AppTheme.AUTUMN
            else -> AppTheme.SPRING
        }
    }

    fun getColorScheme(theme: AppTheme): ColorScheme {
        return when (theme) {
            AppTheme.DARK -> darkColorScheme(
                primary = Color(0xFF00C2FF),
                secondary = Color(0xFF38BDF8),
                background = Color(0xFF0B0F14),
                surface = Color(0xFF121821),
                surfaceVariant = Color(0xFF1A2230),
                onBackground = Color(0xFFEAF2FF),
                onSurface = Color(0xFFEAF2FF),
                onSurfaceVariant = Color(0xFFA3B3C9),
                outline = Color(0xFF30363D)
            )
            AppTheme.LIGHT -> lightColorScheme(
                primary = Color(0xFF0077FF),
                secondary = Color(0xFF38BDF8),
                background = Color(0xFFF5F7FA),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFEEF2F7),
                onBackground = Color(0xFF1A1F2B),
                onSurface = Color(0xFF1A1F2B),
                onSurfaceVariant = Color(0xFF6B7280),
                outline = Color(0xFFE2E8F0)
            )
            AppTheme.SPRING -> lightColorScheme(
                primary = Color(0xFF4ADE80),
                secondary = Color(0xFFF472B6),
                background = Color(0xFFF0FFF4),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFE6F9ED),
                onBackground = Color(0xFF1F2937),
                onSurface = Color(0xFF1F2937),
                onSurfaceVariant = Color(0xFF6B7280)
            )
            AppTheme.SUMMER -> lightColorScheme(
                primary = Color(0xFF00B4D8),
                secondary = Color(0xFFFFD60A),
                background = Color(0xFFE0F7FF),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFD6F4FF),
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A),
                onSurfaceVariant = Color(0xFF475569)
            )
            AppTheme.AUTUMN -> lightColorScheme(
                primary = Color(0xFFF97316),
                secondary = Color(0xFFA16207),
                background = Color(0xFFFFF7ED),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFFFEAD5),
                onBackground = Color(0xFF3F2A1D),
                onSurface = Color(0xFF3F2A1D),
                onSurfaceVariant = Color(0xFF7C5A3C)
            )
            AppTheme.WINTER -> lightColorScheme(
                primary = Color(0xFF60A5FA),
                secondary = Color(0xFF1E3A8A),
                background = Color(0xFFF1F5F9),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFE2E8F0),
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A),
                onSurfaceVariant = Color(0xFF64748B)
            )
            AppTheme.AUTO -> getColorScheme(AppTheme.DARK)
        }
    }

    fun getVisuals(theme: AppTheme): ThemeVisuals {
        return when (theme) {
            AppTheme.DARK -> ThemeVisuals(
                heroGradient = Brush.verticalGradient(listOf(Color(0xFF00C2FF).copy(0.15f), Color(0xFF0B0F14))),
                aiAccent = Color(0xFFA78BFA),
                progressGradient = Brush.horizontalGradient(listOf(Color(0xFF00C2FF), Color(0xFFA78BFA)))
            )
            AppTheme.LIGHT -> ThemeVisuals(
                heroGradient = Brush.verticalGradient(listOf(Color(0xFF0077FF).copy(0.1f), Color(0xFFF5F7FA))),
                aiAccent = Color(0xFF0077FF),
                progressGradient = Brush.horizontalGradient(listOf(Color(0xFF0077FF), Color(0xFF38BDF8)))
            )
            AppTheme.SPRING -> ThemeVisuals(
                heroGradient = Brush.verticalGradient(listOf(Color(0xFF4ADE80).copy(0.15f), Color(0xFFF0FFF4))),
                aiAccent = Color(0xFFF472B6),
                progressGradient = Brush.horizontalGradient(listOf(Color(0xFF4ADE80), Color(0xFFF472B6)))
            )
            AppTheme.SUMMER -> ThemeVisuals(
                heroGradient = Brush.verticalGradient(listOf(Color(0xFF00B4D8).copy(0.15f), Color(0xFFE0F7FF))),
                aiAccent = Color(0xFFFFD60A),
                progressGradient = Brush.horizontalGradient(listOf(Color(0xFF00B4D8), Color(0xFFFFD60A)))
            )
            AppTheme.AUTUMN -> ThemeVisuals(
                heroGradient = Brush.verticalGradient(listOf(Color(0xFFF97316).copy(0.15f), Color(0xFFFFF7ED))),
                aiAccent = Color(0xFFA16207),
                progressGradient = Brush.horizontalGradient(listOf(Color(0xFFF97316), Color(0xFFA16207)))
            )
            AppTheme.WINTER -> ThemeVisuals(
                heroGradient = Brush.verticalGradient(listOf(Color(0xFF60A5FA).copy(0.15f), Color(0xFFF1F5F9))),
                aiAccent = Color(0xFF1E3A8A),
                progressGradient = Brush.horizontalGradient(listOf(Color(0xFF60A5FA), Color(0xFF1E3A8A)))
            )
            AppTheme.AUTO -> getVisuals(AppTheme.DARK)
        }
    }
}
