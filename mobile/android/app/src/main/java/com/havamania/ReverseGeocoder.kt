package com.havamania

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Koordinatı okunabilir yer adına çeviren sarmalayıcı (Aşama 3).
 *
 * Android [Geocoder] kullanır (ek izin gerektirmez). API 33+ asenkron callback API'sini,
 * daha eski sürümlerde deprecated senkron API'yi kullanır. Herhangi bir hata / sonuç
 * yoksa null döner (çağıran taraf koordinatla devam edebilsin).
 */
class ReverseGeocoder(private val context: Context) {

    suspend fun placeName(point: GeoPoint): String? {
        if (!Geocoder.isPresent()) return null
        val addresses = fetchAddresses(point) ?: return null
        return addresses.firstOrNull()?.let { it.toReadableName() }
    }

    private suspend fun fetchAddresses(point: GeoPoint): List<Address>? =
        withContext(Dispatchers.IO) {
            val geocoder = Geocoder(context, Locale("tr"))
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(point.latitude, point.longitude, 1) { result ->
                            if (cont.isActive) cont.resume(result)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(point.latitude, point.longitude, 1)
                }
            } catch (e: Exception) {
                null
            }
        }

    /** Şehir/ilçe önceliğiyle en anlamlı adı seç. */
    private fun Address.toReadableName(): String? =
        subAdminArea    // ilçe
            ?: locality      // şehir/yerleşim
            ?: adminArea     // il
            ?: featureName
}
