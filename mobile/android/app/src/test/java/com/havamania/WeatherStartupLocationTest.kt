package com.havamania

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WeatherStartupLocationTest {
    private val savedCity = GeocodingResultDto(id = 1, name = "İzmir")

    @Test fun unavailableLocationReportsFailureWhilePreservingSavedCity() = runBlocking {
        val displayed = mutableListOf<GeocodingResultDto?>()
        var failures = 0
        resolveAutomaticWeatherCity(savedCity, { throw java.io.IOException("offline geocoder") },
            displayed::add, onUnavailable = { failures++ })
        assertEquals(listOf(savedCity), displayed)
        assertEquals(1, failures)
        displayed.clear()
        resolveAutomaticWeatherCity(null, { null }, displayed::add, onUnavailable = { failures++ })
        assertEquals(listOf<GeocodingResultDto?>(null), displayed)
        assertEquals(2, failures)
    }

    @Test fun cancelledLookupDoesNotPublishAnUnavailableNotice() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        var failures = 0
        val lookup = launch {
            resolveAutomaticWeatherCity(null, { entered.complete(Unit); kotlinx.coroutines.awaitCancellation() },
                {}, onUnavailable = { failures++ })
        }
        entered.await(); lookup.cancel(); lookup.join()
        assertEquals(0, failures)
    }

    @Test fun automaticLocationDoesNotRepeatProvinceAsDistrict() {
        assertEquals("İzmir" to "Konak", currentLocationNames(" İzmir ", "İZMİR", "Konak", null))
        assertEquals("İzmir" to null, currentLocationNames("İzmir", "İZMİR", " ", null))
        assertEquals("Bursa" to null, currentLocationNames(" ", " Bursa ", null, null))
        assertEquals("Mevcut konum" to null, currentLocationNames(null, " ", null, null))
    }

    @Test fun completedLookupCancelsPendingCityPicker() = runBlocking {
        val timerStarted = CompletableDeferred<Unit>()
        val deadline = CompletableDeferred<Unit>()
        val displayed = mutableListOf<GeocodingResultDto?>()
        var pickerOffers = 0
        resolveAutomaticWeatherCity(
            savedCity = null,
            locate = {
                kotlinx.coroutines.withTimeout(5_000) { timerStarted.await() }
                savedCity
            },
            display = displayed::add,
            offerCityPicker = { pickerOffers++ },
            waitForPicker = {
                timerStarted.complete(Unit)
                deadline.await()
            }
        )
        deadline.complete(Unit)
        assertEquals(listOf(savedCity), displayed)
        assertEquals(0, pickerOffers)
    }

    @Test fun cancellingLookupAlsoCancelsPendingCityPicker() = runBlocking {
        val timerStarted = CompletableDeferred<Unit>()
        val deadline = CompletableDeferred<Unit>()
        var pickerOffers = 0
        val lookup = launch {
            resolveAutomaticWeatherCity(
                savedCity = null,
                locate = { kotlinx.coroutines.awaitCancellation() },
                display = { error("Cancelled lookup displayed a city") },
                offerCityPicker = { pickerOffers++ },
                waitForPicker = {
                    timerStarted.complete(Unit)
                    deadline.await()
                }
            )
        }
        kotlinx.coroutines.withTimeout(5_000) { timerStarted.await() }
        lookup.cancel()
        deadline.complete(Unit)
        lookup.join()
        assertEquals(0, pickerOffers)
    }

    @Test fun slowLookupOffersCityPickerBeforeLocationArrives() = runBlocking {
        val deadline = CompletableDeferred<Unit>()
        val location = CompletableDeferred<GeocodingResultDto?>()
        val pickerShown = CompletableDeferred<Unit>()
        val displayed = mutableListOf<GeocodingResultDto?>()
        val lookup = launch {
            resolveAutomaticWeatherCity(null, { location.await() }, displayed::add,
                offerCityPicker = { pickerShown.complete(Unit) }, waitForPicker = { deadline.await() })
        }
        deadline.complete(Unit)
        kotlinx.coroutines.withTimeout(5_000) { pickerShown.await() }
        assertEquals(emptyList<GeocodingResultDto?>(), displayed)
        location.complete(savedCity)
        lookup.join()
        assertEquals(listOf(savedCity), displayed)
    }

    @Test fun locationProviderFailurePreservesSavedCityOrOffersCitySelection() = runBlocking {
        val displayed = mutableListOf<GeocodingResultDto?>()
        val unavailable: suspend () -> GeocodingResultDto? = { throw IllegalStateException("Provider failed") }
        resolveAutomaticWeatherCity(savedCity, unavailable, displayed::add)
        assertEquals(listOf(savedCity), displayed)

        displayed.clear()
        resolveAutomaticWeatherCity(null, unavailable, displayed::add)
        assertEquals(listOf<GeocodingResultDto?>(null), displayed)
    }

    @Test fun cancelledLocationCannotReplaceManuallySelectedCity() = runBlocking {
        lateinit var location: Continuation<GeocodingResultDto?>
        val displayed = mutableListOf<GeocodingResultDto?>()
        val lookup = launch(start = CoroutineStart.UNDISPATCHED) {
            resolveAutomaticWeatherCity(savedCity, { suspendCoroutine { location = it } }, displayed::add)
        }
        lookup.cancel()
        val manual = GeocodingResultDto(id = 2, name = "Bursa")
        displayed.add(manual)
        location.resume(GeocodingResultDto(id = 3, name = "Ankara"))
        lookup.join()
        assertEquals(listOf(savedCity, manual), displayed)
    }

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
