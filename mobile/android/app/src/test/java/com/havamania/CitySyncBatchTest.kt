package com.havamania

import java.io.IOException
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CitySyncBatchTest {
    @Test fun failedAndWaitingItemsDoNotPreventLaterItems() = runBlocking {
        val visited = mutableListOf<Int>()
        var failures = 0
        runCitySyncBatch(listOf(1, 2, 3), "a", { "a" }, {
            visited.add(it)
            if (it == 1) throw CitySyncWaitingException(null, 200)
            if (it == 2) throw IOException("private")
        }, { failures++ })
        assertEquals(listOf(1, 2, 3), visited)
        assertEquals(2, failures)
    }
    @Test fun changedOwnerStopsSendingAndSuppressesOldError() = runBlocking {
        var uid = "a"
        val visited = mutableListOf<Int>()
        runCitySyncBatch(listOf(1, 2), "a", { uid }, {
            visited.add(it); uid = "b"; throw IOException("test")
        }, { fail("No old-account error") })
        assertEquals(listOf(1), visited)
    }
    @Test fun cancellationIsNotAnItemFailure() = runBlocking {
        try {
            runCitySyncBatch(listOf(1, 2), "a", { "a" }, {
                if (it == 1) throw CancellationException("test")
                fail("No next item")
            }, { fail("No cancellation error") })
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
    }
    @Test fun waitingMessageExplainsManualRetryAndRoundsMinutesUp() {
        val message = citySyncErrorText(CitySyncWaitingException(null, 161), 100)
        assertTrue(message.contains("2 dakika"))
        assertTrue(message.contains("otomatik gönderim başlatmaz"))
    }
    @Test fun blockedMessagesGiveSafeRecoveryAdvice() {
        assertTrue(citySyncErrorText(CitySyncWaitingException("AUTHENTICATION", 0)).contains("yeniden giriş"))
        assertTrue(citySyncErrorText(CitySyncWaitingException("REJECTED", 0)).contains("Sunucu"))
        assertFalse(citySyncErrorText(IOException("private-token")).contains("private-token"))
    }
}
