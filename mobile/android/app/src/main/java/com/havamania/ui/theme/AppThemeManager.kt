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
    AUTO("🤖 Otomatik"),
    SPRING("🌸 İlkbahar"),
    SUMMER("☀️ Yaz"),
    AUTUMN("🍂 Sonbahar"),
    WINTER("❄️ Kış"),
    LIGHT("⚪ Açık"),
    DARK("⚫ Koyu")
}

enum class LocationMode(val title: String) {
    AUTO("Otomatik (GPS)"),
    MANUAL("Manuel")
}

enum class TemperatureUnit(val symbol: String, val title: String) {
    CELSIUS("°C", "Celsius"), FAHRENHEIT("°F", "Fahrenheit")
}

enum class WindSpeedUnit(val symbol: String, val title: String) {
    KMH("km/sa", "Kilometre/Saat"),
    MPH("mph", "Mil/Saat"),
    MS("m/s", "Metre/Saniye")
}

enum class PressureUnit(val symbol: String, val title: String) {
    HPA("hPa", "Hektopaskal"),
    MBAR("mbar", "Milibar"),
    INHG("inHg", "İnç Civa")
}

enum class AssistantTone(val title: String, val description: String) {
    SAMIMI("Samimi", "Daha sıcak, doğal ve arkadaşça bir konuşma tarzı."),
    RESMI("Resmi", "Profesyonel, ciddi ve kurumsal bir hitap dili."),
    DENGELI("Dengeli", "Ne çok resmi ne çok samimi; ideal ve bilgilendirici."),
    KISA_NET("Kısa ve Net", "Gereksiz detaylardan arındırılmış, doğrudan sonuç odaklı."),
    DETAYLI_UZMAN("Detaylı Uzman", "Meteorolojik verilerin derinlemesine analiz edildiği kapsamlı tarz.")
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
    // User-Specific Key Helpers
    private fun themeKey(uid: String) = stringPreferencesKey("havamania:$uid:app_theme")
    private fun liveEffectsKey(uid: String) = booleanPreferencesKey("havamania:$uid:live_effects")
    private fun animationsKey(uid: String) = booleanPreferencesKey("havamania:$uid:animations_enabled")
    private fun tiltEffectKey(uid: String) = booleanPreferencesKey("havamania:$uid:tilt_effect_enabled")
    private fun effectIntensityKey(uid: String) = stringPreferencesKey("havamania:$uid:effect_intensity")
    private fun tempUnitKey(uid: String) = stringPreferencesKey("havamania:$uid:temp_unit")
    private fun windUnitKey(uid: String) = stringPreferencesKey("havamania:$uid:wind_unit")
    private fun pressureUnitKey(uid: String) = stringPreferencesKey("havamania:$uid:pressure_unit")
    private fun languageKey(uid: String) = stringPreferencesKey("havamania:$uid:language")

    private fun nameKey(uid: String) = stringPreferencesKey("havamania:$uid:user_name")
    private fun bioKey(uid: String) = stringPreferencesKey("havamania:$uid:user_bio")
    private fun imageUriKey(uid: String) = stringPreferencesKey("havamania:$uid:profileImagePath")
    private fun interestsKey(uid: String) = stringPreferencesKey("havamania:$uid:user_interests")
    private fun aboutMeKey(uid: String) = stringPreferencesKey("havamania:$uid:user_about_me")
    private fun citiesKey(uid: String) = stringPreferencesKey("havamania:$uid:registered_cities")
    private fun defaultCityKey(uid: String) = stringPreferencesKey("havamania:$uid:default_city")
    private fun toneKey(uid: String) = stringPreferencesKey("havamania:$uid:assistant_tone")
    private fun notificationsKey(uid: String) = booleanPreferencesKey("havamania:$uid:notifications_enabled")
    private fun personalizationEnabledKey(uid: String) = booleanPreferencesKey("havamania:$uid:personalization_enabled")
    private fun seededTripsKey(uid: String) = booleanPreferencesKey("havamania:$uid:has_seeded_trips")
    private fun seededNotificationsKey(uid: String) = booleanPreferencesKey("havamania:$uid:has_seeded_notifications")
    private fun onboardingCompletedKey(uid: String) = booleanPreferencesKey("havamania:$uid:onboarding_completed")
    private fun migrationChoiceMadeKey(uid: String) = booleanPreferencesKey("havamania:$uid:migration_choice_made")
    private fun locationModeKey(uid: String) = stringPreferencesKey("havamania:$uid:location_mode")

    // Smart Alert Keys
    private fun rainAlertKey(uid: String) = booleanPreferencesKey("havamania:$uid:alert_rain")
    private fun windAlertKey(uid: String) = booleanPreferencesKey("havamania:$uid:alert_wind")
    private fun heatAlertKey(uid: String) = booleanPreferencesKey("havamania:$uid:alert_heat")
    private fun frostAlertKey(uid: String) = booleanPreferencesKey("havamania:$uid:alert_frost")
    private fun fogAlertKey(uid: String) = booleanPreferencesKey("havamania:$uid:alert_fog")
    private fun stormAlertKey(uid: String) = booleanPreferencesKey("havamania:$uid:alert_storm")
    private fun uvAlertKey(uid: String) = booleanPreferencesKey("havamania:$uid:alert_uv")
    private fun pollenAlertKey(uid: String) = booleanPreferencesKey("havamania:$uid:alert_pollen")
    private fun airQualityAlertKey(uid: String) = booleanPreferencesKey("havamania:$uid:alert_aqi")

    // Legacy / Global (Only used for guest users or non-critical device state if any)
    private val GLOBAL_USER_NAME_KEY = stringPreferencesKey("user_name")
    private val GLOBAL_USER_BIO_KEY = stringPreferencesKey("user_bio")

    // --- User Specific Methods ---

    suspend fun saveTheme(context: Context, theme: AppTheme, uid: String) = context.dataStore.edit { it[themeKey(uid)] = theme.name }
    fun getTheme(context: Context, uid: String): Flow<AppTheme> = context.dataStore.data.map {
        val themeName = it[themeKey(uid)] ?: AppTheme.DARK.name
        try { AppTheme.valueOf(themeName) } catch (e: Exception) { AppTheme.DARK }
    }

    suspend fun saveTempUnit(context: Context, unit: TemperatureUnit, uid: String) = context.dataStore.edit { it[tempUnitKey(uid)] = unit.name }
    fun getTempUnit(context: Context, uid: String): Flow<TemperatureUnit> = context.dataStore.data.map {
        try { TemperatureUnit.valueOf(it[tempUnitKey(uid)] ?: TemperatureUnit.CELSIUS.name) } catch (e: Exception) { TemperatureUnit.CELSIUS }
    }

    suspend fun saveWindUnit(context: Context, unit: WindSpeedUnit, uid: String) = context.dataStore.edit { it[windUnitKey(uid)] = unit.name }
    fun getWindUnit(context: Context, uid: String): Flow<WindSpeedUnit> = context.dataStore.data.map {
        try { WindSpeedUnit.valueOf(it[windUnitKey(uid)] ?: WindSpeedUnit.KMH.name) } catch (e: Exception) { WindSpeedUnit.KMH }
    }

    suspend fun savePressureUnit(context: Context, unit: PressureUnit, uid: String) = context.dataStore.edit { it[pressureUnitKey(uid)] = unit.name }
    fun getPressureUnit(context: Context, uid: String): Flow<PressureUnit> = context.dataStore.data.map {
        try { PressureUnit.valueOf(it[pressureUnitKey(uid)] ?: PressureUnit.HPA.name) } catch (e: Exception) { PressureUnit.HPA }
    }

    suspend fun saveLanguage(context: Context, lang: String, uid: String) = context.dataStore.edit { it[languageKey(uid)] = lang }
    fun getLanguage(context: Context, uid: String): Flow<String> = context.dataStore.data.map { it[languageKey(uid)] ?: "TR" }

    suspend fun saveAssistantTone(context: Context, uid: String, tone: AssistantTone) = context.dataStore.edit { it[toneKey(uid)] = tone.name }
    fun getAssistantTone(context: Context, uid: String): Flow<AssistantTone> = context.dataStore.data.map {
        try { AssistantTone.valueOf(it[toneKey(uid)] ?: AssistantTone.DENGELI.name) } catch (e: Exception) { AssistantTone.DENGELI }
    }

    suspend fun saveNotificationsEnabled(context: Context, uid: String, enabled: Boolean) = context.dataStore.edit { it[notificationsKey(uid)] = enabled }
    fun getNotificationsEnabled(context: Context, uid: String): Flow<Boolean> = context.dataStore.data.map { it[notificationsKey(uid)] ?: true }

    suspend fun savePersonalizationEnabled(context: Context, uid: String, enabled: Boolean) = context.dataStore.edit { it[personalizationEnabledKey(uid)] = enabled }
    fun getPersonalizationEnabled(context: Context, uid: String): Flow<Boolean> = context.dataStore.data.map { it[personalizationEnabledKey(uid)] ?: true }

    suspend fun saveHasSeededTrips(context: Context, seeded: Boolean, uid: String) = context.dataStore.edit { it[seededTripsKey(uid)] = seeded }
    fun getHasSeededTrips(context: Context, uid: String): Flow<Boolean> = context.dataStore.data.map { it[seededTripsKey(uid)] ?: false }

    suspend fun saveHasSeededNotifications(context: Context, seeded: Boolean, uid: String) = context.dataStore.edit { it[seededNotificationsKey(uid)] = seeded }
    fun getHasSeededNotifications(context: Context, uid: String): Flow<Boolean> = context.dataStore.data.map { it[seededNotificationsKey(uid)] ?: false }

    suspend fun saveOnboardingCompleted(context: Context, uid: String, completed: Boolean) = context.dataStore.edit { it[onboardingCompletedKey(uid)] = completed }
    fun getOnboardingCompleted(context: Context, uid: String): Flow<Boolean> = context.dataStore.data.map { it[onboardingCompletedKey(uid)] ?: false }

    suspend fun saveMigrationChoiceMade(context: Context, uid: String, made: Boolean) = context.dataStore.edit { it[migrationChoiceMadeKey(uid)] = made }
    fun getMigrationChoiceMade(context: Context, uid: String): Flow<Boolean> = context.dataStore.data.map { it[migrationChoiceMadeKey(uid)] ?: false }

    suspend fun saveLocationMode(context: Context, uid: String, mode: LocationMode) = context.dataStore.edit { it[locationModeKey(uid)] = mode.name }
    fun getLocationMode(context: Context, uid: String): Flow<LocationMode> = context.dataStore.data.map {
        val modeName = it[locationModeKey(uid)] ?: LocationMode.MANUAL.name
        try { LocationMode.valueOf(modeName) } catch (e: Exception) { LocationMode.MANUAL }
    }

    suspend fun saveUserName(context: Context, uid: String, name: String) = context.dataStore.edit { it[nameKey(uid)] = name }
    fun getUserName(context: Context, uid: String): Flow<String> = context.dataStore.data.map { it[nameKey(uid)] ?: "" }

    suspend fun saveUserBio(context: Context, uid: String, bio: String) = context.dataStore.edit { it[bioKey(uid)] = bio }
    fun getUserBio(context: Context, uid: String): Flow<String> = context.dataStore.data.map { it[bioKey(uid)] ?: "" }

    suspend fun saveUserImageUriByUid(context: Context, uid: String, path: String?) = context.dataStore.edit {
        val key = imageUriKey(uid)
        if (path == null) it.remove(key) else it[key] = path
    }
    fun getUserImageUriByUid(context: Context, uid: String): Flow<String?> = context.dataStore.data.map { it[imageUriKey(uid)] }

    suspend fun saveUserInterests(context: Context, uid: String, interests: Set<String>) = context.dataStore.edit {
        it[interestsKey(uid)] = interests.joinToString(",")
    }
    fun getUserInterests(context: Context, uid: String): Flow<Set<String>> = context.dataStore.data.map {
        val str = it[interestsKey(uid)] ?: ""
        if (str.isEmpty()) emptySet() else str.split(",").toSet()
    }

    suspend fun saveUserAboutMe(context: Context, uid: String, aboutMe: String) = context.dataStore.edit { it[aboutMeKey(uid)] = aboutMe }
    fun getUserAboutMe(context: Context, uid: String): Flow<String> = context.dataStore.data.map { it[aboutMeKey(uid)] ?: "" }

    suspend fun saveRegisteredCities(context: Context, uid: String, cities: List<GeocodingResultDto>) = context.dataStore.edit {
        it[citiesKey(uid)] = Json.encodeToString(cities)
    }
    fun getRegisteredCities(context: Context, uid: String): Flow<List<GeocodingResultDto>> = context.dataStore.data.map {
        val str = it[citiesKey(uid)] ?: ""
        if (str.isEmpty()) {
            if (uid == "legacy") {
                listOf(GeocodingResultDto(0, "İstanbul", 41.0082, 28.9784, "Turkey", "TR", "İstanbul"))
            } else {
                emptyList()
            }
        } else {
            try { Json.decodeFromString(str) } catch(e: Exception) { emptyList() }
        }
    }

    suspend fun saveDefaultCity(context: Context, uid: String, city: GeocodingResultDto) = context.dataStore.edit {
        it[defaultCityKey(uid)] = Json.encodeToString(city)
    }
    fun getDefaultCity(context: Context, uid: String): Flow<GeocodingResultDto?> = context.dataStore.data.map {
        val str = it[defaultCityKey(uid)] ?: ""
        if (str.isEmpty()) {
            if (uid == "legacy") {
                GeocodingResultDto(0, "İstanbul", 41.0082, 28.9784, "Turkey", "TR", "İstanbul")
            } else {
                null
            }
        } else {
            try { Json.decodeFromString<GeocodingResultDto>(str) } catch(e: Exception) {
                if (uid == "legacy") GeocodingResultDto(0, "İstanbul", 41.0082, 28.9784, "Turkey", "TR", "İstanbul") else null
            }
        }
    }

    // --- System / UI Config ---
    suspend fun saveLiveEffects(context: Context, enabled: Boolean, uid: String) = context.dataStore.edit { it[liveEffectsKey(uid)] = enabled }
    fun getLiveEffects(context: Context, uid: String): Flow<Boolean> = context.dataStore.data.map { it[liveEffectsKey(uid)] ?: true }

    suspend fun saveAnimationsEnabled(context: Context, enabled: Boolean, uid: String) = context.dataStore.edit { it[animationsKey(uid)] = enabled }
    fun getAnimationsEnabled(context: Context, uid: String): Flow<Boolean> = context.dataStore.data.map { it[animationsKey(uid)] ?: true }

    suspend fun saveTiltEffectEnabled(context: Context, enabled: Boolean, uid: String) = context.dataStore.edit { it[tiltEffectKey(uid)] = enabled }
    fun getTiltEffectEnabled(context: Context, uid: String): Flow<Boolean> = context.dataStore.data.map { it[tiltEffectKey(uid)] ?: true }

    suspend fun saveEffectIntensity(context: Context, intensity: String, uid: String) = context.dataStore.edit { it[effectIntensityKey(uid)] = intensity }
    fun getEffectIntensity(context: Context, uid: String): Flow<String> = context.dataStore.data.map { it[effectIntensityKey(uid)] ?: "MEDIUM" }

    suspend fun clearRegisteredCities(context: Context, uid: String) = context.dataStore.edit {
        val default = GeocodingResultDto(0, "İstanbul", 41.0082, 28.9784, "Turkey", "TR", "İstanbul")
        it[citiesKey(uid)] = Json.encodeToString(listOf(default))
        it[defaultCityKey(uid)] = Json.encodeToString(default)
    }

    suspend fun resetAll(context: Context, uid: String) = context.dataStore.edit {
        it.remove(nameKey(uid))
        it.remove(bioKey(uid))
        it.remove(imageUriKey(uid))
        it.remove(interestsKey(uid))
        it.remove(aboutMeKey(uid))
        it.remove(citiesKey(uid))
        it.remove(defaultCityKey(uid))
        it.remove(toneKey(uid))
        it.remove(notificationsKey(uid))
        it.remove(personalizationEnabledKey(uid))
    }

    // --- Smart Alert Methods ---

    suspend fun saveSmartAlertConfig(context: Context, uid: String, config: com.havamania.SmartAlertConfig) = context.dataStore.edit {
        it[rainAlertKey(uid)] = config.rainEnabled
        it[windAlertKey(uid)] = config.windEnabled
        it[heatAlertKey(uid)] = config.heatEnabled
        it[frostAlertKey(uid)] = config.frostEnabled
        it[fogAlertKey(uid)] = config.fogEnabled
        it[stormAlertKey(uid)] = config.stormEnabled
        it[uvAlertKey(uid)] = config.uvEnabled
        it[pollenAlertKey(uid)] = config.pollenEnabled
        it[airQualityAlertKey(uid)] = config.airQualityEnabled
    }

    fun getSmartAlertConfig(context: Context, uid: String): Flow<com.havamania.SmartAlertConfig> = context.dataStore.data.map {
        com.havamania.SmartAlertConfig(
            rainEnabled = it[rainAlertKey(uid)] ?: true,
            windEnabled = it[windAlertKey(uid)] ?: true,
            heatEnabled = it[heatAlertKey(uid)] ?: true,
            frostEnabled = it[frostAlertKey(uid)] ?: true,
            fogEnabled = it[fogAlertKey(uid)] ?: true,
            stormEnabled = it[stormAlertKey(uid)] ?: true,
            uvEnabled = it[uvAlertKey(uid)] ?: true,
            pollenEnabled = it[pollenAlertKey(uid)] ?: false,
            airQualityEnabled = it[airQualityAlertKey(uid)] ?: false
        )
    }

    suspend fun saveSingleAlertSetting(context: Context, uid: String, alertId: String, enabled: Boolean) = context.dataStore.edit {
        val key = when(alertId) {
            "rain" -> rainAlertKey(uid)
            "wind" -> windAlertKey(uid)
            "heat" -> heatAlertKey(uid)
            "frost" -> frostAlertKey(uid)
            "fog" -> fogAlertKey(uid)
            "storm" -> stormAlertKey(uid)
            "uv" -> uvAlertKey(uid)
            "pollen" -> pollenAlertKey(uid)
            "aqi" -> airQualityAlertKey(uid)
            else -> null
        }
        key?.let { k -> it[k] = enabled }
    }

    // ... (helper methods like getAutoTheme, getColorScheme)

    fun getSeasonalTheme(month: Int): AppTheme {
        return when (month) {
            3, 4, 5 -> AppTheme.SPRING
            6, 7, 8 -> AppTheme.SUMMER
            9, 10, 11 -> AppTheme.AUTUMN
            12, 1, 2 -> AppTheme.WINTER
            else -> AppTheme.SUMMER
        }
    }

    /**
     * Otomatik tema kontrolü - Aylar bazında (Business Rule 3)
     */
    fun getCurrentThemeSelection(context: Context, uid: String): Flow<AppTheme> = context.dataStore.data.map {
        val themeName = it[themeKey(uid)] ?: AppTheme.AUTO.name
        val theme = try { AppTheme.valueOf(themeName) } catch (e: Exception) { AppTheme.DARK }

        if (theme == AppTheme.AUTO) {
            getSeasonalTheme(java.time.LocalDate.now().monthValue)
        } else {
            theme
        }
    }

    fun getColorScheme(theme: AppTheme): ColorScheme {
        return when (theme) {
            AppTheme.DARK, AppTheme.AUTUMN, AppTheme.WINTER -> darkColorScheme(
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
            AppTheme.LIGHT, AppTheme.SPRING, AppTheme.SUMMER -> lightColorScheme(
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
            AppTheme.AUTO -> getColorScheme(getSeasonalTheme(java.time.LocalDate.now().monthValue))
        }
    }
}
