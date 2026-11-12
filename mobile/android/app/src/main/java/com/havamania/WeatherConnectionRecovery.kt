package com.havamania

internal fun weatherNeedsConnectionRecovery(state: WeatherUiState, now: java.time.Instant): Boolean = when (state) {
    is WeatherUiState.Success -> ageVisibleWeather(state.data, now).isStale
    is WeatherUiState.Error -> state.retryNeedsInternet
    else -> false
}
