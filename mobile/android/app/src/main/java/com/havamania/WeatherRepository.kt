package com.havamania

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Hava durumu verilerini sağlayan Repository - Cache ve Network koordinasyonu
 */
class WeatherRepository(
    private val apiService: WeatherApiService = NetworkModule.apiService,
    private val weatherDao: WeatherDao,
    private val accessAllowed: () -> Boolean = { true },
    private val currentOwner: () -> String? = { null }
) {
    private val cacheWrites = Mutex()
    private val deletionEpoch = java.util.concurrent.atomic.AtomicLong()
    private val stateGate = Any()
    private var observedOwner = currentOwner()
    private data class RequestSession(val epoch: Long, val owner: String?)
    private fun current(session: RequestSession) =
        session.epoch == deletionEpoch.get() && session.owner == currentOwner() && accessAllowed()

    /** Idempotent across listeners; A -> B -> A invalidates the first A request. */
    fun onAccountChanged(owner: String?) = synchronized(stateGate) {
        if (owner != observedOwner) {
            observedOwner = owner
            deletionEpoch.incrementAndGet()
            _currentWeatherState.value = null
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val _currentWeatherState = MutableStateFlow<WeatherData?>(null)
    val currentWeatherState: StateFlow<WeatherData?> = _currentWeatherState.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: WeatherRepository? = null

        fun getInstance(application: android.app.Application): WeatherRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let { return@synchronized it }
                val database = WeatherDatabase.getDatabase(application)
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val instance = WeatherRepository(weatherDao = database.weatherDao(), accessAllowed = {
                    val state = AccountDeletionManager.getInstance(application).state.value
                    !state.pending && !state.busy
                }, currentOwner = { auth.currentUser?.uid })
                // Application-lifetime listener also fences requests with no Activity present.
                auth.addAuthStateListener { instance.onAccountChanged(it.currentUser?.uid) }
                INSTANCE = instance
                instance
            }
        }
    }

    private fun canonicalKey(cityName: String, districtName: String?): String {
        val normCity = cityName.trim().lowercase(java.util.Locale("tr"))
        val normDistrict = districtName?.trim()?.takeIf { it.isNotBlank() }?.lowercase(java.util.Locale("tr"))
        return if (normDistrict != null) "$normCity-$normDistrict" else normCity
    }

    suspend fun clearCache(cityName: String, districtName: String? = null) {
        try {
            val cacheKey = canonicalKey(cityName, districtName)
            weatherDao.deleteWeatherByPrefix(weatherCachePrefix(cityName, districtName), cacheKey)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            // Log or ignore
        }
    }

    fun clearCurrentWeather() {
        synchronized(stateGate) { _currentWeatherState.value = null }
    }

    /** Serialize deletion with writes already in progress; old requests stay fenced after recovery. */
    suspend fun clearForAccountDeletion() {
        cacheWrites.withLock {
            synchronized(stateGate) {
                deletionEpoch.incrementAndGet()
                _currentWeatherState.value = null
            }
            weatherDao.clearAllWeatherCache()
        }
    }

    private suspend fun FlowCollector<WeatherData>.emitCurrent(data: WeatherData, epoch: RequestSession) {
        currentCoroutineContext().ensureActive()
        val publish = synchronized(stateGate) {
            if (current(epoch)) { _currentWeatherState.value = data; true } else false
        }
        if (publish && current(epoch)) emit(data)
    }

    /** Clear downloaded weather only. Account records and visible data are not a cache. */
    suspend fun clearAllCachedWeather() {
        weatherDao.clearAllWeatherCache()
    }

    /**
     * Şehir arama fonksiyonu
     */
    suspend fun searchCityOrThrow(query: String): List<GeocodingResultDto> =
        apiService.searchCity(cityName = query).results ?: emptyList()

    suspend fun searchCity(query: String): List<GeocodingResultDto> {
        return try {
            val response = apiService.searchCity(cityName = query)
            response.results ?: emptyList()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emptyList()
        }
    }

    /**
     * Tek seferlik hava durumu anlık görüntüsü — asistanın sorulan şehir için veri çekmesi içindir.
     *
     * [getWeatherData] bilinçli olarak kullanılmıyor: o akış cache'i ve [currentWeatherState]'i
     * günceller, yani sohbette başka bir şehir sorulduğunda ana ekranın konumunu ezerdi.
     */
    suspend fun fetchWeatherSnapshot(
        lat: Double,
        lon: Double,
        cityName: String,
        districtName: String? = null
    ): WeatherData? {
        return try {
            val response = apiService.getFullWeather(lat = lat, lon = lon)
            WeatherMapper.mapToDomain(response, cityName, districtName)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                android.util.Log.e("WeatherRepo", "Could not fetch weather snapshot.")
}
            null
        }
    }

    /**
     * Önce cache verisini döner, sonra API'den güncel veriyi çeker.
     * Stale-while-revalidate stratejisi (Business Rule 6)
     */
    fun getWeatherData(
        lat: Double,
        lon: Double,
        cityName: String,
        districtName: String? = null,
        forceRefresh: Boolean = false
    ): Flow<WeatherData> {
        val epoch = synchronized(stateGate) { RequestSession(deletionEpoch.get(), currentOwner()) }
        return flow {
        if (!current(epoch)) return@flow
        val cacheKey = weatherCacheKey(cityName, districtName, lat, lon)
        val cachedEntity = try {
            weatherDao.getCachedWeather(cacheKey)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null // A broken disposable cache must not prevent a network refresh.
        }
        if (!current(epoch)) return@flow
        val cachedData = cachedEntity?.let {
            try { json.decodeFromString<WeatherData>(it.jsonData) } catch (_: Exception) { null }
        }
        val isCacheFresh = cachedEntity != null && weatherCacheIsFresh(cachedEntity.timestamp, System.currentTimeMillis())
        if (cachedData != null && cachedEntity != null) {
            emitCurrent(cachedData.copy(timestamp = cachedEntity.timestamp, isStale = !isCacheFresh), epoch)
        }
        if (cachedData != null && isCacheFresh && !forceRefresh) return@flow

        if (!current(epoch)) return@flow
        val domainData = try {
            val response = apiService.getFullWeather(lat = lat, lon = lon)
            if (!current(epoch)) return@flow
            WeatherMapper.mapToDomain(response, cityName, districtName)
                .copy(timestamp = System.currentTimeMillis(), isStale = false)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!current(epoch)) return@flow
            if (cachedData == null) throw e
            if (forceRefresh && isCacheFresh) {
                emitCurrent(cachedData.copy(timestamp = cachedEntity.timestamp, isStale = true), epoch)
            }
            return@flow
        }
        try {
            cacheWrites.withLock {
                if (current(epoch)) weatherDao.insertWeather(WeatherCacheEntity(cacheKey, json.encodeToString(domainData), domainData.timestamp))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            // Show valid network data even if the disk is full or cache write fails.
        }
        // Keep emit outside exception handlers: downstream errors belong to the collector.
        emitCurrent(domainData, epoch)
        }
    }
}

internal fun weatherCachePrefix(city: String, district: String?): String {
    val locale = java.util.Locale.forLanguageTag("tr")
    val normalizedCity = city.trim().lowercase(locale)
    val normalizedDistrict = district?.trim()?.lowercase(locale).orEmpty()
    return "v2|${normalizedCity.length}:$normalizedCity|${normalizedDistrict.length}:$normalizedDistrict|"
}

internal fun weatherCacheKey(city: String, district: String?, lat: Double, lon: Double): String {
    require(lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0)
    // ~100 m cells prevent GPS jitter from creating a new row for every refresh.
    return weatherCachePrefix(city, district) + "${kotlin.math.round(lat * 1000).toInt()}:${kotlin.math.round(lon * 1000).toInt()}"
}

internal fun weatherCacheIsFresh(timestamp: Long, now: Long): Boolean =
    timestamp <= now && now - timestamp in 0 until 15 * 60 * 1000L
