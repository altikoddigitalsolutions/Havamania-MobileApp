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

    fun calculateBackoffMillis(attempt: Int): Long {
        val base = 250L * (1 shl attempt.coerceAtMost(4))
        val jitter = kotlin.random.Random.nextLong(0, 150)
        return base + jitter
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        })
        .addInterceptor { chain ->
            val request = chain.request()
            // GET-only safety
            if (request.method != "GET") {
                return@addInterceptor chain.proceed(request)
            }

            var response: okhttp3.Response? = null
            var error: Exception? = null
            var tryCount = 0
            val maxLimit = 2 // Total 3 attempts

            while (tryCount <= maxLimit) {
                try {
                    if (tryCount > 0) {
                        try {
                            val backoff = calculateBackoffMillis(tryCount - 1)
                            Thread.sleep(backoff)
                        } catch (ie: InterruptedException) {
                            Thread.currentThread().interrupt()
                            response?.close()
                            throw java.io.InterruptedIOException("Request interrupted").initCause(ie)
                        }
                    }

                    response?.close()
                    response = chain.proceed(request)

                    val resp = response
                    if (resp == null) {
                        tryCount++
                        continue
                    }

                    // Retry ONLY 5xx server errors. Do not retry 4xx, 429, or successful responses.
                    if (resp.isSuccessful || resp.code < 500) {
                        return@addInterceptor resp
                    }
                } catch (e: Exception) {
                    error = e
                    if (e is java.io.IOException || e is java.net.SocketTimeoutException) {
                        if (BuildConfig.DEBUG) {
                            android.util.Log.w("NetworkModule", "Retry attempt ${tryCount + 1} due to: ${e.message}")
                        }
                    } else {
                        response?.close()
                        throw e
                    }
                }
                tryCount++
            }

            response?.let {
                if (it.code >= 500) {
                    return@addInterceptor it
                }
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
