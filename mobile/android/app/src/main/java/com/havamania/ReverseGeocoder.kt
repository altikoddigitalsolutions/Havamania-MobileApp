package com.havamania

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Koordinatı okunabilir yer adına çeviren sarmalayıcı (Aşama 3).
 *
 * Önce Android [Geocoder] denenir (ek izin gerektirmez). Emülatörde / geocoder backend'i
 * olmayan cihazlarda bu null döndüğü için, key gerektirmeyen ağ tabanlı reverse-geocode
 * (BigDataCloud) yedeği devreye girer. İkisi de başarısızsa null döner.
 */
class ReverseGeocoder(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class BdcReverse(
        val city: String? = null,
        val locality: String? = null,
        val principalSubdivision: String? = null
    )

    suspend fun placeName(point: GeoPoint): String? {
        val local = if (Geocoder.isPresent()) fetchAddresses(point)?.firstOrNull()?.toReadableName() else null
        if (!local.isNullOrBlank()) return local
        return networkPlaceName(point)
    }

    /** Ağ tabanlı yedek reverse-geocode (BigDataCloud, ücretsiz, key gerektirmez). */
    private suspend fun networkPlaceName(point: GeoPoint): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.bigdatacloud.net/data/reverse-geocode-client" +
                "?latitude=${point.latitude}&longitude=${point.longitude}&localityLanguage=tr"
            val text = URL(url).readText()
            val r = json.decodeFromString(BdcReverse.serializer(), text)
            r.city?.takeIf { it.isNotBlank() }
                ?: r.locality?.takeIf { it.isNotBlank() }
                ?: r.principalSubdivision?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
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
