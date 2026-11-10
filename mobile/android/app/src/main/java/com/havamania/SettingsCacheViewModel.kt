package com.havamania

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

internal enum class WeatherCacheClearState { IDLE, CLEARING, SUCCESS, ERROR }

internal class WeatherCacheClearer(private val clearCache: suspend () -> Unit) {
    private val lock = Mutex()
    private val mutableState = MutableStateFlow(WeatherCacheClearState.IDLE)
    val state = mutableState.asStateFlow()

    suspend fun clear() {
        currentCoroutineContext().ensureActive()
        if (!lock.tryLock()) return
        try {
            mutableState.value = WeatherCacheClearState.CLEARING
            clearCache()
            currentCoroutineContext().ensureActive()
            mutableState.value = WeatherCacheClearState.SUCCESS
        } catch (error: CancellationException) {
            mutableState.value = WeatherCacheClearState.IDLE
            throw error
        } catch (_: Exception) {
            mutableState.value = WeatherCacheClearState.ERROR
        } finally { lock.unlock() }
    }

    fun dismiss() {
        if (mutableState.value != WeatherCacheClearState.CLEARING) mutableState.value = WeatherCacheClearState.IDLE
    }
}

class SettingsCacheViewModel(application: Application) : AndroidViewModel(application) {
    internal val clearer = WeatherCacheClearer {
        withContext(Dispatchers.IO) { WeatherRepository.getInstance(application).clearAllCachedWeather() }
    }
    fun clearWeatherCache() { viewModelScope.launch { clearer.clear() } }
}
