package com.havamania

import java.io.IOException
import java.net.ProtocolException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class NetworkRetryTest {
    private val request = Request.Builder().url("https://example.invalid/forecast").build()

    @Test fun securityAndProtocolFailuresAreReturnedWithoutRetry() {
        for (failure in listOf(SSLHandshakeException("TLS"), SSLPeerUnverifiedException("peer"), ProtocolException("HTTP"))) {
            var attempts = 0
            val client = OkHttpClient.Builder()
                .addInterceptor(weatherRetryInterceptor { fail("Must not wait before retry") })
                .addInterceptor { attempts++; throw failure }
                .build()
            assertSame(failure, runCatching { client.newCall(request).execute().close() }.exceptionOrNull())
            assertEquals(1, attempts)
        }
    }

    @Test fun cancellationDuringBackoffDoesNotStartAnotherRequest() {
        var attempts = 0
        lateinit var call: Call
        call = OkHttpClient.Builder()
            .addInterceptor(weatherRetryInterceptor { call.cancel() })
            .addInterceptor { chain -> attempts++; response(chain, 503) }
            .build().newCall(request)

        assertTrue(runCatching { call.execute().close() }.exceptionOrNull() is IOException)
        assertEquals(1, attempts)
    }

    @Test fun serverFailureRetriesButRateLimitAndFinalResponseAreReturned() {
        for ((codes, expectedAttempts) in listOf(listOf(503, 503, 502) to 3, listOf(429) to 1)) {
            var attempts = 0
            val client = OkHttpClient.Builder()
                .addInterceptor(weatherRetryInterceptor {})
                .addInterceptor { chain -> response(chain, codes[attempts++]) }
                .build()
            client.newCall(request).execute().use { assertEquals(codes.last(), it.code) }
            assertEquals(expectedAttempts, attempts)
        }
    }

    private fun response(chain: Interceptor.Chain, code: Int) = Response.Builder()
        .request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("Synthetic")
        .body("test".toResponseBody()).build()

    @Test
    fun `calculateBackoffMillis increases with attempt and includes jitter`() {
        val delay0 = NetworkModule.calculateBackoffMillis(0)
        val delay1 = NetworkModule.calculateBackoffMillis(1)

        // Attempt 0 base is 250 + jitter (0..150) -> range [250, 400]
        // Attempt 1 base is 500 + jitter (0..150) -> range [500, 650]
        assertTrue(delay0 in 250..450)
        assertTrue(delay1 in 500..700)
    }
}
