package com.havamania.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.havamania.WeatherData
import com.havamania.GeocodingResultDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppTheme(val title: String) {
    AUTO("Otomatik"),
    LIGHT("Açık Tema"),
    DARK("Koyu Tema"),
    SPRING_DAY("İlkbahar Gündüz"),
    SPRING_NIGHT("İlkbahar Gece"),
    SUMMER_DAY("Yaz Gündüz"),
    SUMMER_NIGHT("Yaz Gece"),
    AUTUMN_DAY("Sonbahar Gündüz"),
    AUTUMN_NIGHT("Sonbahar Gece"),
    WINTER_DAY("Kış Gündüz"),
    WINTER_NIGHT("Kış Gece")
}

enum class TemperatureUnit(val symbol: String, val title: String) {
    CELSIUS("°C", "Celsius"), FAHRENHEIT("°F", "Fahrenheit")
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

object ThemeManager {
    private val THEME_KEY = stringPreferencesKey("app_theme")
    private val LIVE_EFFECTS_KEY = booleanPreferencesKey("live_effects")
    private val ANIMATIONS_KEY = booleanPreferencesKey("animations_enabled")
    private val TILT_EFFECT_KEY = booleanPreferencesKey("tilt_effect_enabled")
    private val EFFECT_INTENSITY_KEY = stringPreferencesKey("effect_intensity")

    // Yeni Ayarlar
    private val TEMP_UNIT_KEY = stringPreferencesKey("temp_unit")
    private val LANGUAGE_KEY = stringPreferencesKey("language")
    private val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")

    // Profil Ayarları
    private val USER_NAME_KEY = stringPreferencesKey("user_name")
    private val USER_BIO_KEY = stringPreferencesKey("user_bio")
    private val USER_IMAGE_URI_KEY = stringPreferencesKey("user_image_uri")
    private val USER_INTERESTS_KEY = stringPreferencesKey("user_interests")
    private val USER_ABOUT_ME_KEY = stringPreferencesKey("user_about_me")

    // Şehir Ayarları
    private val REGISTERED_CITIES_KEY = stringPreferencesKey("registered_cities")
    private val DEFAULT_CITY_KEY = stringPreferencesKey("default_city")

    suspend fun saveTheme(context: Context, theme: AppTheme) = context.dataStore.edit { it[THEME_KEY] = theme.name }

    fun getTheme(context: Context): Flow<AppTheme> = context.dataStore.data.map {
        val themeName = it[THEME_KEY] ?: AppTheme.DARK.name
        try {
            AppTheme.valueOf(themeName)
        } catch (e: Exception) {
            // Migration for old theme names
            when (themeName) {
                "SPRING" -> AppTheme.SPRING_DAY
                "SUMMER" -> AppTheme.SUMMER_DAY
                "AUTUMN" -> AppTheme.AUTUMN_DAY
                "WINTER" -> AppTheme.WINTER_DAY
                else -> AppTheme.DARK
            }
        }
    }

    suspend fun saveTempUnit(context: Context, unit: TemperatureUnit) = context.dataStore.edit { it[TEMP_UNIT_KEY] = unit.name }

    fun getTempUnit(context: Context): Flow<TemperatureUnit> = context.dataStore.data.map {
        TemperatureUnit.valueOf(it[TEMP_UNIT_KEY] ?: TemperatureUnit.CELSIUS.name)
    }

    suspend fun saveLanguage(context: Context, lang: String) = context.dataStore.edit { it[LANGUAGE_KEY] = lang }

    fun getLanguage(context: Context): Flow<String> = context.dataStore.data.map {
        it[LANGUAGE_KEY] ?: "TR"
    }

    suspend fun saveNotificationsEnabled(context: Context, enabled: Boolean) = context.dataStore.edit { it[NOTIFICATIONS_KEY] = enabled }

    fun getNotificationsEnabled(context: Context): Flow<Boolean> = context.dataStore.data.map {
        it[NOTIFICATIONS_KEY] ?: true
    }

    suspend fun saveUserName(context: Context, name: String) = context.dataStore.edit { it[USER_NAME_KEY] = name }
    fun getUserName(context: Context): Flow<String> = context.dataStore.data.map { it[USER_NAME_KEY] ?: "Gezgin" }

    suspend fun saveUserBio(context: Context, bio: String) = context.dataStore.edit { it[USER_BIO_KEY] = bio }
    fun getUserBio(context: Context): Flow<String> = context.dataStore.data.map { it[USER_BIO_KEY] ?: "Hava durumu meraklısı 🌤️" }

    suspend fun saveUserImageUri(context: Context, uri: String?) = context.dataStore.edit {
        if (uri == null) it.remove(USER_IMAGE_URI_KEY) else it[USER_IMAGE_URI_KEY] = uri
    }
    fun getUserImageUri(context: Context): Flow<String?> = context.dataStore.data.map { it[USER_IMAGE_URI_KEY] }

    suspend fun saveUserInterests(context: Context, interests: Set<String>) = context.dataStore.edit {
        it[USER_INTERESTS_KEY] = interests.joinToString(",")
    }
    fun getUserInterests(context: Context): Flow<Set<String>> = context.dataStore.data.map {
        val str = it[USER_INTERESTS_KEY] ?: "Kamp,Yürüyüş,Seyahat"
        if (str.isEmpty()) emptySet() else str.split(",").toSet()
    }

    suspend fun saveUserAboutMe(context: Context, aboutMe: String) = context.dataStore.edit {
        it[USER_ABOUT_ME_KEY] = aboutMe
    }
    fun getUserAboutMe(context: Context): Flow<String> = context.dataStore.data.map {
        it[USER_ABOUT_ME_KEY] ?: ""
    }

    suspend fun saveRegisteredCities(context: Context, cities: List<GeocodingResultDto>) = context.dataStore.edit {
        it[REGISTERED_CITIES_KEY] = Json.encodeToString(cities)
    }
    fun getRegisteredCities(context: Context): Flow<List<GeocodingResultDto>> = context.dataStore.data.map {
        val str = it[REGISTERED_CITIES_KEY] ?: ""
        if (str.isEmpty()) {
            listOf(
                GeocodingResultDto(0, "İstanbul", 41.0082, 28.9784, "Turkey", "TR", "İstanbul")
            )
        } else {
            try { Json.decodeFromString(str) } catch(e: Exception) { emptyList() }
        }
    }

    suspend fun saveDefaultCity(context: Context, city: GeocodingResultDto) = context.dataStore.edit {
        it[DEFAULT_CITY_KEY] = Json.encodeToString(city)
    }
    fun getDefaultCity(context: Context): Flow<GeocodingResultDto> = context.dataStore.data.map {
        val str = it[DEFAULT_CITY_KEY] ?: ""
        if (str.isEmpty()) {
            GeocodingResultDto(0, "İstanbul", 41.0082, 28.9784, "Turkey", "TR", "İstanbul")
        } else {
            try { Json.decodeFromString(str) } catch(e: Exception) {
                GeocodingResultDto(0, "İstanbul", 41.0082, 28.9784, "Turkey", "TR", "İstanbul")
            }
        }
    }

    suspend fun saveLiveEffects(context: Context, enabled: Boolean) = context.dataStore.edit { it[LIVE_EFFECTS_KEY] = enabled }

    fun getLiveEffects(context: Context): Flow<Boolean> = context.dataStore.data.map {
        it[LIVE_EFFECTS_KEY] ?: true
    }

    suspend fun saveAnimationsEnabled(context: Context, enabled: Boolean) = context.dataStore.edit { it[ANIMATIONS_KEY] = enabled }

    fun getAnimationsEnabled(context: Context): Flow<Boolean> = context.dataStore.data.map {
        it[ANIMATIONS_KEY] ?: true
    }

    suspend fun saveTiltEffectEnabled(context: Context, enabled: Boolean) = context.dataStore.edit { it[TILT_EFFECT_KEY] = enabled }

    fun getTiltEffectEnabled(context: Context): Flow<Boolean> = context.dataStore.data.map {
        it[TILT_EFFECT_KEY] ?: true
    }

    suspend fun saveEffectIntensity(context: Context, intensity: String) = context.dataStore.edit { it[EFFECT_INTENSITY_KEY] = intensity }

    fun getEffectIntensity(context: Context): Flow<String> = context.dataStore.data.map {
        it[EFFECT_INTENSITY_KEY] ?: "MEDIUM"
    }

    suspend fun clearRegisteredCities(context: Context) = context.dataStore.edit {
        val default = GeocodingResultDto(0, "İstanbul", 41.0082, 28.9784, "Turkey", "TR", "İstanbul")
        val jsonStr = Json.encodeToString(listOf(default))
        it[REGISTERED_CITIES_KEY] = jsonStr
        it[DEFAULT_CITY_KEY] = Json.encodeToString(default)
    }

    suspend fun resetAll(context: Context) = context.dataStore.edit {
        it.clear()
    }

    fun getAutoTheme(month: Int, isDay: Boolean): AppTheme {
        return when (month) {
            3, 4, 5 -> if (isDay) AppTheme.SPRING_DAY else AppTheme.SPRING_NIGHT
            6, 7, 8 -> if (isDay) AppTheme.SUMMER_DAY else AppTheme.SUMMER_NIGHT
            9, 10, 11 -> if (isDay) AppTheme.AUTUMN_DAY else AppTheme.AUTUMN_NIGHT
            12, 1, 2 -> if (isDay) AppTheme.WINTER_DAY else AppTheme.WINTER_NIGHT
            else -> if (isDay) AppTheme.SUMMER_DAY else AppTheme.DARK
        }
    }

    fun getColorScheme(theme: AppTheme): ColorScheme {
        return when (theme) {
            AppTheme.DARK, AppTheme.SPRING_NIGHT, AppTheme.SUMMER_NIGHT, AppTheme.AUTUMN_NIGHT, AppTheme.WINTER_NIGHT -> darkColorScheme(
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
            AppTheme.SPRING_DAY -> lightColorScheme(
                primary = Color(0xFF4ADE80),
                secondary = Color(0xFFF472B6),
                background = Color(0xFFF0FFF4),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFE6F9ED),
                onBackground = Color(0xFF1F2937),
                onSurface = Color(0xFF1F2937),
                onSurfaceVariant = Color(0xFF6B7280)
            )
            AppTheme.SUMMER_DAY -> lightColorScheme(
                primary = Color(0xFF00B4D8),
                secondary = Color(0xFFFFD60A),
                background = Color(0xFFE0F7FF),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFD6F4FF),
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A),
                onSurfaceVariant = Color(0xFF475569)
            )
            AppTheme.AUTUMN_DAY -> lightColorScheme(
                primary = Color(0xFFF97316),
                secondary = Color(0xFFA16207),
                background = Color(0xFFFFF7ED),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFFFEAD5),
                onBackground = Color(0xFF3F2A1D),
                onSurface = Color(0xFF3F2A1D),
                onSurfaceVariant = Color(0xFF7C5A3C)
            )
            AppTheme.WINTER_DAY -> lightColorScheme(
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
}
