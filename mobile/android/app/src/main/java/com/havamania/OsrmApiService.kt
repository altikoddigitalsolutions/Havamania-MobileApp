package com.havamania

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * OSRM rota servisi (Aşama 2).
 *
 * Koordinatlar path içinde `{lon},{lat};{lon},{lat}` biçiminde, virgül ve noktalı
 * virgülle gönderilir; bu yüzden [encoded = true] ile Retrofit'in bunları yeniden
 * kodlaması engellenir.
 */
interface OsrmApiService {
    @GET("route/v1/driving/{coordinates}")
    suspend fun getRoute(
        @Path(value = "coordinates", encoded = true) coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson",
        @Query("steps") steps: Boolean = false
    ): OsrmRouteResponse
}
