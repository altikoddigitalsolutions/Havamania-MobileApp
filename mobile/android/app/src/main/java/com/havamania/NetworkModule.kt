package com.havamania

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object NetworkModule {
    private const val BASE_URL = "https://api.open-meteo.com/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        })
        .addInterceptor { chain ->
            val request = chain.request()
            var response: okhttp3.Response? = null
            var error: Exception? = null
            var tryCount = 0
            val maxLimit = 2 // Total 3 attempts

            while (tryCount <= maxLimit) {
                try {
                    response?.close()
                    response = chain.proceed(request)

                    // KURAL 4: Sadece 5xx hatalarında tekrar dene.
                    // 4xx hataları (401, 404 vb) retry edilmemeli.
                    if (response!!.isSuccessful || response!!.code < 500) {
                        return@addInterceptor response!!
                    }
                } catch (e: Exception) {
                    error = e
                    // Timeout veya bağlantı hatası durumunda tekrar dene
                    if (e is java.io.IOException || e is java.net.SocketTimeoutException) {
                        android.util.Log.w("NetworkModule", "Retry attempt ${tryCount + 1} due to: ${e.message}")
                    } else {
                        throw e
                    }
                }
                tryCount++
            }

            response ?: throw error ?: java.io.IOException("Unknown network error")
        }
        .build()

    val apiService: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WeatherApiService::class.java)
    }
}
