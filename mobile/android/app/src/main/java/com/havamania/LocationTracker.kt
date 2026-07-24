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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

interface LocationTracker {
    suspend fun getCurrentLocation(): Location?
    suspend fun getCurrentCity(): GeocodingResultDto?
}

class DefaultLocationTracker(
    private val locationClient: FusedLocationProviderClient,
    private val application: Application
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

        val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!hasAccessCoarseLocationPermission && !hasAccessFineLocationPermission) {
            return null
        }

        if (!isGpsEnabled) return null

        return suspendCancellableCoroutine { cont ->
            locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    cont.resume(location)
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
                .addOnCanceledListener {
                    cont.resume(null)
                }
        }
    }

    override suspend fun getCurrentCity(): GeocodingResultDto? {
        val location = getCurrentLocation() ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(application, Locale("tr"))
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]

                    val cityName = address.adminArea ?: address.locality ?: "Bilinmeyen Şehir"
                    val districtName = address.locality ?: address.subLocality ?: address.subAdminArea

                    GeocodingResultDto(
                        id = (location.latitude * 1000 + location.longitude * 1000).toInt(),
                        name = districtName ?: cityName,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        country = address.countryName ?: "Türkiye",
                        countryCode = address.countryCode,
                        admin1 = cityName
                    )
                } else null
            } catch (e: Exception) {
                android.util.Log.e("LocationTracker", "Reverse Geocoding failed", e)
                null
            }
        }
    }
}
