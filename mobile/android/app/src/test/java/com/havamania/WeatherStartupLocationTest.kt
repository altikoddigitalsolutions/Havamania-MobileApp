package com.havamania

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherStartupLocationTest {
    private val savedCity = GeocodingResultDto(id = 1, name = "İzmir")

    @Test fun savedCityLoadsBeforeLocationReturnsAndSurvivesUnavailableLocation() = runBlocking {
        val location = CompletableDeferred<GeocodingResultDto?>()
        val displayed = mutableListOf<GeocodingResultDto?>()
        val lookup = launch(start = CoroutineStart.UNDISPATCHED) {
            resolveAutomaticWeatherCity(savedCity, { location.await() }, displayed::add)
        }
        assertEquals(listOf(savedCity), displayed)
        location.complete(null)
        lookup.join()
        assertEquals(listOf(savedCity), displayed)
    }

    @Test fun availableLocationReplacesSavedCityAndMissingCitiesResolveToEmptyState() = runBlocking {
        val currentCity = GeocodingResultDto(id = 2, name = "Ankara")
        val displayed = mutableListOf<GeocodingResultDto?>()
        resolveAutomaticWeatherCity(savedCity, { currentCity }, displayed::add)
        assertEquals(listOf(savedCity, currentCity), displayed)

        displayed.clear()
        resolveAutomaticWeatherCity(null, { null }, displayed::add)
        assertEquals(listOf<GeocodingResultDto?>(null), displayed)
    }
}
