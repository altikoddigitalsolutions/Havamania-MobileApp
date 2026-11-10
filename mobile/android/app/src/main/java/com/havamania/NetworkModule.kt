package com.havamania

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ProtocolException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

internal fun weatherRetryInterceptor(pause: (Long) -> Unit = { Thread.sleep(it) }) = Interceptor { chain ->
    val request = chain.request()
    if (request.method != "GET") return@Interceptor chain.proceed(request)

    val lastAttempt = 2
    for (attempt in 0..lastAttempt) {
        if (chain.call().isCanceled()) throw IOException("Canceled")
        if (attempt > 0) {
            try {
                pause(NetworkModule.calculateBackoffMillis(attempt - 1))
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
                throw InterruptedIOException("Request interrupted").apply { initCause(interrupted) }
            }
            if (chain.call().isCanceled()) throw IOException("Canceled")
        }

        val response = try {
            chain.proceed(request)
        } catch (error: IOException) {
            if (error is SSLHandshakeException || error is SSLPeerUnverifiedException || error is ProtocolException) {
                throw error
            }
            if (chain.call().isCanceled() || Thread.currentThread().isInterrupted || attempt == lastAttempt) throw error
            if (BuildConfig.DEBUG) android.util.Log.w("NetworkModule", "Retrying weather request after an I/O failure")
            continue
        }
        if (chain.call().isCanceled()) {
            response.close()
            throw IOException("Canceled")
        }
        if (response.code !in 500..599 || attempt == lastAttempt) return@Interceptor response
        response.close()
    }
    error("Weather retry loop did not finish")
}

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
        .addInterceptor(weatherRetryInterceptor())
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
