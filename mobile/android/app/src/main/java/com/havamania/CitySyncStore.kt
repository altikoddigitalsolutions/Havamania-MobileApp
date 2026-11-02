package com.havamania

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

internal class CitySyncStore(
    private val store: DataStore<Preferences>, private val clock: () -> CitySyncClock? = { null },
    private val writesAllowed: () -> Boolean = { true }
) {
    private fun citiesKey(uid: String) = stringPreferencesKey("havamania:$uid:registered_cities")
    private fun cityChangesKey(uid: String) = stringPreferencesKey("havamania:$uid:city_sync_changes")

    private fun defaultKey(uid: String) = stringPreferencesKey("havamania:$uid:default_city")
    private fun defaultChangeKey(uid: String) = stringPreferencesKey("havamania:$uid:default_city_sync")

    fun observeCities(uid: String): Flow<List<GeocodingResultDto>> = store.data.map {
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

    fun observeDefaultCity(uid: String): Flow<GeocodingResultDto?> = store.data.map {
        val str = it[defaultKey(uid)] ?: ""
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

    suspend fun recordDefaultCity(uid: String, city: GeocodingResultDto) {
        store.edit { prefs ->
            if (!writesAllowed()) return@edit
            prefs[defaultKey(uid)] = Json.encodeToString(city)
            if (uid != "legacy") prefs[defaultChangeKey(uid)] = Json.encodeToString(CitySyncChange(city, false))
        }
    }
    suspend fun pendingDefaultCity(uid: String): CitySyncChange? =
        store.data.first()[defaultChangeKey(uid)]?.let { Json.decodeFromString<CitySyncChange>(it) }

    suspend fun acknowledgeDefaultCity(uid: String, sent: CitySyncChange) {
        store.edit { prefs ->
            if (!writesAllowed()) return@edit
            val current = prefs[defaultChangeKey(uid)]?.let { Json.decodeFromString<CitySyncChange>(it) }
            if (current?.revision == sent.revision) {
                prefs[defaultChangeKey(uid)] = Json.encodeToString(current.copy(acknowledged = true))
            }
        }
    }
    suspend fun mergeServerDefaultCity(uid: String, name: String?, resolved: GeocodingResultDto?): GeocodingResultDto? {
        var result: GeocodingResultDto? = null
        store.edit { prefs ->
            if (!writesAllowed()) return@edit
            val pending = prefs[defaultChangeKey(uid)]?.let { Json.decodeFromString<CitySyncChange>(it) }
            result = pending?.city ?: resolved
            result?.let { prefs[defaultKey(uid)] = Json.encodeToString(it) }
            if (pending?.acknowledged == true && pending.city.name == name) prefs.remove(defaultChangeKey(uid))
        }
        return result
    }

    suspend fun recordCityChange(uid: String, city: GeocodingResultDto, deleted: Boolean): Boolean {
        var applied = false
        store.edit { prefs ->
            if (!writesAllowed()) return@edit
            val changes = prefs[cityChangesKey(uid)]?.let {
                Json.decodeFromString<List<com.havamania.CitySyncChange>>(it)
            }.orEmpty()
            val cities = prefs[citiesKey(uid)]?.let { Json.decodeFromString<List<GeocodingResultDto>>(it) }
                ?: if (uid == "legacy") listOf(GeocodingResultDto(0, "İstanbul", 41.0082, 28.9784, "Turkey", "TR", "İstanbul")) else emptyList()
            val default = prefs[defaultKey(uid)]?.let { Json.decodeFromString<GeocodingResultDto>(it) }
                ?: if (uid == "legacy") GeocodingResultDto(0, "İstanbul", 41.0082, 28.9784, "Turkey", "TR", "İstanbul") else null
            if (deleted) {
                if (cities.size <= 1 || default?.id == city.id || cities.none { it.id == city.id }) return@edit
            } else if (cities.any { it.id == city.id }) return@edit
            val updated = changes.filterNot { it.city.id == city.id } + com.havamania.CitySyncChange(city, deleted)
            prefs[citiesKey(uid)] = Json.encodeToString(com.havamania.mergeCityChanges(cities, updated))
            if (uid != "legacy") prefs[cityChangesKey(uid)] = Json.encodeToString(updated)
            applied = true
        }
        return applied
    }

    suspend fun refreshRetryClock(uid: String, expected: CitySyncChange, defaultCity: Boolean): CitySyncChange? {
        val now = clock() ?: return expected
        if (!expected.retryClockNeedsReset(now)) return expected
        var refreshed: CitySyncChange? = null
        store.edit { prefs ->
            if (!writesAllowed()) return@edit
            fun refresh(current: CitySyncChange?): CitySyncChange? {
                if (current != expected) return null
                val duration = expected.retryDuration.takeIf { it in 1..CITY_SYNC_UNCERTAIN_SECONDS }
                    ?: CITY_SYNC_UNCERTAIN_SECONDS
                return expected.copy(retryNotBefore = now.wall.coerceIn(0, Long.MAX_VALUE - duration) + duration)
                    .withRetryClock(now, duration)
            }
            if (defaultCity) {
                refreshed = refresh(prefs[defaultChangeKey(uid)]?.let { Json.decodeFromString<CitySyncChange>(it) })
                refreshed?.let { prefs[defaultChangeKey(uid)] = Json.encodeToString(it) }
            } else {
                val changes = prefs[cityChangesKey(uid)]?.let { Json.decodeFromString<List<CitySyncChange>>(it) }.orEmpty()
                prefs[cityChangesKey(uid)] = Json.encodeToString(changes.map { refresh(it)?.also { updated -> refreshed = updated } ?: it })
            }
        }
        return refreshed
    }

    suspend fun reserveAttempt(uid: String, expected: CitySyncChange, defaultCity: Boolean, now: Long): CitySyncChange? {
        var reserved: CitySyncChange? = null
        store.edit { prefs ->
            if (!writesAllowed()) return@edit
            fun reserve(current: CitySyncChange?): CitySyncChange? {
                if (current == null || current != expected || current.acknowledged || current.blockedReason != null ||
                    (clock()?.let { current.retrySecondsLeft(it) > 0 } ?: (current.retryNotBefore > now))) return null
                val reserved = current.copy(retryNotBefore = now.coerceIn(0, Long.MAX_VALUE - CITY_SYNC_UNCERTAIN_SECONDS) + CITY_SYNC_UNCERTAIN_SECONDS)
                return clock()?.let { reserved.withRetryClock(it, CITY_SYNC_UNCERTAIN_SECONDS) } ?: reserved
            }
            if (defaultCity) {
                val current = prefs[defaultChangeKey(uid)]?.let { Json.decodeFromString<CitySyncChange>(it) }
                reserved = reserve(current)
                reserved?.let { prefs[defaultChangeKey(uid)] = Json.encodeToString(it) }
            } else {
                val changes = prefs[cityChangesKey(uid)]?.let { Json.decodeFromString<List<CitySyncChange>>(it) }.orEmpty()
                prefs[cityChangesKey(uid)] = Json.encodeToString(changes.map { change ->
                    reserve(change)?.also { reserved = it } ?: change
                })
            }
        }
        return reserved
    }

    suspend fun recordFailure(uid: String, sent: CitySyncChange, replacement: CitySyncChange, defaultCity: Boolean) {
        store.edit { prefs ->
            if (!writesAllowed()) return@edit
            if (defaultCity) {
                val current = prefs[defaultChangeKey(uid)]?.let { Json.decodeFromString<CitySyncChange>(it) }
                if (current?.revision == sent.revision && !current.acknowledged) {
                    prefs[defaultChangeKey(uid)] = Json.encodeToString(replacement)
                }
            } else {
                val changes = prefs[cityChangesKey(uid)]?.let { Json.decodeFromString<List<CitySyncChange>>(it) }.orEmpty()
                prefs[cityChangesKey(uid)] = Json.encodeToString(changes.map {
                    if (it.revision == sent.revision && !it.acknowledged) replacement else it
                })
            }
        }
    }

    suspend fun releaseBlocks(uid: String, authOnly: Boolean) {
        fun release(change: CitySyncChange): CitySyncChange =
            if (change.blockedReason != null && (!authOnly || change.blockedReason == "AUTHENTICATION"))
                change.copy(blockedReason = null) else change
        store.edit { prefs ->
            if (!writesAllowed()) return@edit
            prefs[cityChangesKey(uid)]?.let { raw ->
                prefs[cityChangesKey(uid)] = Json.encodeToString(Json.decodeFromString<List<CitySyncChange>>(raw).map(::release))
            }
            prefs[defaultChangeKey(uid)]?.let { raw ->
                prefs[defaultChangeKey(uid)] = Json.encodeToString(release(Json.decodeFromString(raw)))
            }
        }
    }

    suspend fun pendingCityChanges(uid: String): List<com.havamania.CitySyncChange> {
        val prefs = store.data.first()
        return prefs[cityChangesKey(uid)]?.let { Json.decodeFromString<List<CitySyncChange>>(it) }.orEmpty()
    }

    suspend fun acknowledge(uid: String, sent: CitySyncChange) {
        store.edit { prefs ->
            if (!writesAllowed()) return@edit
            val changes = prefs[cityChangesKey(uid)]?.let {
                Json.decodeFromString<List<CitySyncChange>>(it)
            }.orEmpty()
            prefs[cityChangesKey(uid)] = Json.encodeToString(changes.map {
                if (it.revision == sent.revision) it.copy(acknowledged = true) else it
            })
        }
    }

    suspend fun mergeRemoteCities(uid: String, remote: List<GeocodingResultDto>): List<GeocodingResultDto> {
        var result = emptyList<GeocodingResultDto>()
        store.edit { prefs ->
            if (!writesAllowed()) return@edit
            val changes = prefs[cityChangesKey(uid)]?.let {
                Json.decodeFromString<List<com.havamania.CitySyncChange>>(it)
            }.orEmpty()
            result = com.havamania.mergeCityChanges(remote, changes)
            prefs[citiesKey(uid)] = Json.encodeToString(result)
            prefs[cityChangesKey(uid)] = Json.encodeToString(com.havamania.unconfirmedCityChanges(remote, changes))
        }
        return result
    }

}
