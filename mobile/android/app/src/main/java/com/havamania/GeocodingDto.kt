package com.havamania

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResultDto>? = null
)

@Keep
@Serializable
@IgnoreExtraProperties
data class GeocodingResultDto(
    val id: Long = 0,
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val country: String = "",
    @SerialName("country_code")
    @get:PropertyName("country_code")
    @set:PropertyName("country_code")
    var countryCode: String? = null,
    val admin1: String? = null,
    val admin2: String? = null,
    val admin3: String? = null
) {
    /**
     * TR: İlçe ve il bilgisini birleştirerek anlamlı bir isim döner.
     */
    val displayName: String
        get() = if (admin1 != null && admin1 != name) "$name, $admin1" else name

    val city: String
        get() = admin1 ?: name

    val district: String?
        get() = if (admin1 != null && admin1 != name) name else null

    fun getSafeCity(): String = city
    fun getSafeDistrict(): String? = district
}
