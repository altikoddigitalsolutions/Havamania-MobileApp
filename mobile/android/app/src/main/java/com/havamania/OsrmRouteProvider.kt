package com.havamania

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * İki nokta arasında gerçek sürüş rotası hesaplayan soyutlama (Aşama 2).
 *
 * Sağlayıcı bağımsızdır; Aşama 5'te self-host OSRM'e (veya başka bir motora) geçmek
 * için yalnızca implementasyon değiştirilir, çağıran ekranlar aynı kalır.
 */
interface RouteProvider {
    suspend fun getRoute(origin: GeoPoint, destination: GeoPoint): RouteResult
}

/**
 * OSRM public demo sunucusunu kullanan implementasyon.
 *
 * NOT (Aşama 5): `router.project-osrm.org` demo sunucusu üretim için garanti vermez
 * (rate limit / kesinti). Prodüksiyonda `turkey-latest.osm.pbf` ile self-host OSRM'e geçilecek;
 * o zaman yalnızca [OsrmModule.BASE_URL] değişecek.
 */
class OsrmRouteProvider(
    private val api: OsrmApiService = OsrmModule.service
) : RouteProvider {

    override suspend fun getRoute(origin: GeoPoint, destination: GeoPoint): RouteResult =
        withContext(Dispatchers.IO) {
            // OSRM koordinat sırası: lon,lat  (path: lon,lat;lon,lat)
            val coords = "${origin.longitude},${origin.latitude};" +
                "${destination.longitude},${destination.latitude}"
            try {
                val response = api.getRoute(coords)
                val route = response.routes.firstOrNull()
                if (response.code != "Ok" || route == null || route.geometry.coordinates.isEmpty()) {
                    return@withContext RouteResult.NoRoute
                }
                // GeoJSON [lon, lat] -> domain GeoPoint(lat, lon)
                val points = route.geometry.coordinates.mapNotNull { c ->
                    if (c.size >= 2) GeoPoint(latitude = c[1], longitude = c[0]) else null
                }
                if (points.size < 2) return@withContext RouteResult.NoRoute
                RouteResult.Success(
                    RoutePath(
                        points = points,
                        distanceMeters = route.distance,
                        durationSeconds = route.duration
                    )
                )
            } catch (e: Exception) {
                RouteResult.Error(e.message ?: "Rota alınamadı", e)
            }
        }
}

/** OSRM için ayrı Retrofit örneği (Open-Meteo'dan farklı base URL). */
object OsrmModule {
    // Aşama 5'te self-host adresiyle değiştirilecek.
    private const val BASE_URL = "https://router.project-osrm.org/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    val service: OsrmApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OsrmApiService::class.java)
    }
}
