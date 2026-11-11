package com.havamania

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.location.CurrentLocationRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import java.util.Locale
import kotlin.coroutines.resume

internal fun currentLocationNames(
    adminArea: String?,
    locality: String?,
    subLocality: String?,
    subAdminArea: String?
): Pair<String, String?> {
    val city = sequenceOf(adminArea, locality, subAdminArea, subLocality)
        .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
        .firstOrNull() ?: return "Mevcut konum" to null
    val locale = Locale.forLanguageTag("tr")
    val cityKey = city.lowercase(locale)
    val district = sequenceOf(locality, subLocality, subAdminArea)
        .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
        .firstOrNull { it.lowercase(locale) != cityKey }
    return city to district
}

interface LocationTracker {
    suspend fun getCurrentLocation(): Location?
    suspend fun getCurrentCity(): GeocodingResultDto?
}

enum class LocationLookupStage { POSITION_STARTED, POSITION_RECEIVED, POSITION_EMPTY, POSITION_RETRY, POSITION_TIMEOUT,
    PROVIDER_ERROR, PERMISSION_MISSING, LOCATION_DISABLED, GEOCODER_STARTED, CITY_RECEIVED, CITY_EMPTY, GEOCODER_ERROR }

class DefaultLocationTracker(
    private val locationClient: FusedLocationProviderClient,
    private val application: Application,
    private val diagnostic: (LocationLookupStage) -> Unit = {}
) : LocationTracker {

    override suspend fun getCurrentLocation(): Location? {
        val hasAccessFineLocationPermission = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasAccessCoarseLocationPermission = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAccessCoarseLocationPermission && !hasAccessFineLocationPermission) {
            diagnostic(LocationLookupStage.PERMISSION_MISSING)
            return null
        }

        if (!deviceLocationEnabled(application)) {
            diagnostic(LocationLookupStage.LOCATION_DISABLED)
            return null
        }

        return try {
            diagnostic(LocationLookupStage.POSITION_STARTED)
            kotlinx.coroutines.withTimeout(31_000) {
                var providerFailed = false
                suspend fun requestPosition(): Location? {
                val position = suspendCancellableCoroutine<Location?> { cont ->
                    val cancellation = com.google.android.gms.tasks.CancellationTokenSource()
                    cont.invokeOnCancellation { cancellation.cancel() }
                    val priority = if (hasAccessFineLocationPermission) {
                        Priority.PRIORITY_HIGH_ACCURACY
                    } else {
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY
                    }
                    val request = CurrentLocationRequest.Builder().setPriority(priority)
                        .setDurationMillis(30_000).setMaxUpdateAgeMillis(10_000).build()
                    locationClient.getCurrentLocation(request, cancellation.token)
                        .addOnSuccessListener { location ->
                            if (cont.isActive) cont.resume(location)
                        }
                        .addOnFailureListener {
                            if (cont.isActive) {
                                providerFailed = true
                                diagnostic(LocationLookupStage.PROVIDER_ERROR)
                                cont.resume(null)
                            }
                        }
                        .addOnCanceledListener {
                            if (cont.isActive) cont.resume(null)
                        }
                }
                diagnostic(if (position == null) LocationLookupStage.POSITION_EMPTY else LocationLookupStage.POSITION_RECEIVED)
                return position
                }
                recoverEarlyEmptyPosition(::requestPosition,
                    canRetry = { !providerFailed && PermissionHelper.hasLocationPermission(application) && deviceLocationEnabled(application) },
                    elapsedMillis = android.os.SystemClock::elapsedRealtime,
                    onRetry = { diagnostic(LocationLookupStage.POSITION_RETRY) })
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            diagnostic(LocationLookupStage.POSITION_TIMEOUT)
            null
        } catch (_: SecurityException) {
            diagnostic(LocationLookupStage.PERMISSION_MISSING)
            null
        }
    }

    override suspend fun getCurrentCity(): GeocodingResultDto? {
        val location = getCurrentLocation() ?: return null
        diagnostic(LocationLookupStage.GEOCODER_STARTED)
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(application, Locale.forLanguageTag("tr"))
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]

                    val (cityName, districtName) = currentLocationNames(
                        address.adminArea, address.locality, address.subLocality, address.subAdminArea
                    )
                    if (cityName == "Mevcut konum") {
                        diagnostic(LocationLookupStage.CITY_EMPTY)
                        return@withContext null
                    }

                    diagnostic(LocationLookupStage.CITY_RECEIVED)
                    GeocodingResultDto(
                        id = (location.latitude * 1000 + location.longitude * 1000).toLong(),
                        name = districtName ?: cityName,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        country = address.countryName?.trim().orEmpty(),
                        countryCode = address.countryCode,
                        admin1 = cityName
                    )
                } else {
                    diagnostic(LocationLookupStage.CITY_EMPTY)
                    null
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                diagnostic(LocationLookupStage.GEOCODER_ERROR)
if (BuildConfig.DEBUG) {
                    android.util.Log.e("LocationTracker", "Reverse Geocoding failed", e)
}
                null
            }
        }
    }
}
