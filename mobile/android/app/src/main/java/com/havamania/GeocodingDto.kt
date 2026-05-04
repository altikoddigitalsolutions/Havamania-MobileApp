package com.havamania

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResultDto>? = null
)

@Serializable
data class GeocodingResultDto(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String,
    @SerialName("country_code")
    val countryCode: String? = null,
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
}
