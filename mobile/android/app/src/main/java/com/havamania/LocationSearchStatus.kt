package com.havamania

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

enum class LocationSearchStatus { IDLE, LOADING, RESULTS, EMPTY, ERROR }
internal data class LocationSearchResult(val items: List<GeocodingResultDto>, val status: LocationSearchStatus)

internal suspend fun fetchLocationSuggestions(load: suspend () -> List<GeocodingResultDto>): LocationSearchResult =
    try {
        val items = withTimeout(15_000) { load() }
        LocationSearchResult(items, if (items.isEmpty()) LocationSearchStatus.EMPTY else LocationSearchStatus.RESULTS)
    } catch (e: Exception) {
        if (e is CancellationException && e !is TimeoutCancellationException) throw e
        LocationSearchResult(emptyList(), LocationSearchStatus.ERROR)
    }
