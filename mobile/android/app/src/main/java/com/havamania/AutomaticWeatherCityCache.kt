package com.havamania

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.havamania.ui.theme.LocationMode
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class AutomaticWeatherCityRecord(val city: GeocodingResultDto, val savedAt: Long)

/** Local fallback only; never writes the manual default or a Firebase synchronization request. */
internal class AutomaticWeatherCityCache(
    private val store: DataStore<Preferences>, private val writesAllowed: () -> Boolean = { true },
    private val clock: () -> Long = System::currentTimeMillis
) {
    private fun key(uid: String) = stringPreferencesKey("havamania:$uid:last_automatic_weather_city")
    suspend fun read(uid: String): GeocodingResultDto? {
        val prefs = store.data.first()
        if (prefs.weatherLocationMode(uid) != LocationMode.AUTO) return null
        val payload = prefs[key(uid)] ?: return null
        val record = runCatching { Json.decodeFromString<AutomaticWeatherCityRecord>(payload) }.getOrNull()
        val now = clock()
        if (record != null && record.savedAt >= 0 && record.savedAt <= now && now - record.savedAt < 24 * 60 * 60_000L &&
            record.city.name.isNotBlank() && hasResolvedTripCoordinates(record.city.latitude, record.city.longitude)) return record.city
        store.edit { if (it[key(uid)] == payload) it.remove(key(uid)) }
        return null
    }
    suspend fun write(uid: String, city: GeocodingResultDto, isCurrentOwner: () -> Boolean): Boolean {
        if (uid.isBlank() || city.name.isBlank() || !hasResolvedTripCoordinates(city.latitude, city.longitude)) return false
        var saved = false
        store.edit { prefs ->
            if (writesAllowed() && isCurrentOwner() && prefs.weatherLocationMode(uid) == LocationMode.AUTO) {
                prefs[key(uid)] = Json.encodeToString(AutomaticWeatherCityRecord(city, clock()))
                saved = true
            }
        }
        return saved
    }
}
