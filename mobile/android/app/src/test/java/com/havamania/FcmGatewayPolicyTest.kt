package com.havamania

import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class FcmGatewayPolicyTest {
    private val desired = FcmTokenRegistration(UUID.randomUUID().toString(), 3, "uid-a", "local-test-token", true)
    private val owner = UUID.randomUUID().toString()
    private fun receipt() = BackendFcmReceipt("record", owner, "android", desired.token!!, desired.installationId, 3, 1)
    private fun api(block: (BackendFcmRegistration) -> Response<BackendFcmReceipt>) = object : BackendFcmService {
        override suspend fun registerPushToken(authorization: String, request: BackendFcmRegistration) = block(request)
    }

    @Test fun unauthorizedRefreshesOnceWithIdenticalSnapshot() = runBlocking {
        val requests = mutableListOf<BackendFcmRegistration>()
        val refreshes = mutableListOf<Boolean>()
        val service = api { request ->
            requests.add(request)
            if (requests.size == 1) Response.error(401, "{}".toResponseBody()) else Response.success(receipt())
        }
        val result = sendFcmTokenWithAuth(service, desired, owner,
            { refreshes.add(it); "Bearer local-test" }, { true })
        assertEquals(FcmGatewayAction.APPLIED, result.action)
        assertEquals(listOf(false, true), refreshes)
        assertEquals(2, requests.size)
        assertEquals(requests[0], requests[1])
    }

    @Test fun accountChangeDuringRefreshNeverSendsAgain() = runBlocking {
        var current = true
        var calls = 0
        val service = api { calls++; Response.error(401, "{}".toResponseBody()) }
        val result = sendFcmTokenWithAuth(service, desired, owner,
            { refresh -> if (refresh) current = false; "Bearer local-test" }, { current })
        assertEquals(FcmGatewayAction.SUPERSEDED, result.action)
        assertEquals(1, calls)
    }

    @Test fun permanentAndTransientErrorsHaveBoundedAttempts() = runBlocking {
        for (status in listOf(401, 403, 409, 422, 429, 500, 503)) {
            var calls = 0
            val service = api { calls++; Response.error(status, "{}".toResponseBody()) }
            val result = sendFcmTokenWithAuth(service, desired, owner, { "Bearer local-test" }, { true })
            assertEquals(if (status == 429 || status >= 500) FcmGatewayAction.RETRY else FcmGatewayAction.STOP, result.action)
            assertEquals(if (status == 401) 2 else 1, calls)
            if (status == 429) assertEquals(60L, result.retryAfterSeconds)
        }
    }

    @Test fun retryAfterIsBoundedAndParsedWithoutExposingErrorBody() = runBlocking {
        for ((header, expected) in listOf("120" to 120L, "86400" to 86400L, "-1" to 60L, "999999" to 60L, "bad" to 60L)) {
            val service = api {
                val raw = okhttp3.Response.Builder().request(okhttp3.Request.Builder().url("https://local.invalid/").build())
                    .protocol(okhttp3.Protocol.HTTP_1_1).code(429).message("rate limited").header("Retry-After", header).build()
                Response.error<BackendFcmReceipt>("PRIVATE_BODY".toResponseBody(), raw)
            }
            val result = sendFcmTokenWithAuth(service, desired, owner, { "Bearer local-test" }, { true })
            assertEquals(expected, result.retryAfterSeconds)
            assertFalse(result.toString().contains("PRIVATE_BODY"))
        }
    }

    @Test fun malformedReceiptAndCancellationCannotComplete() = runBlocking {
        val result = sendFcmTokenWithAuth(api { Response.success(receipt().copy(user_id = "other")) },
            desired, owner, { "Bearer local-test" }, { true })
        assertEquals(FcmGatewayAction.STOP, result.action)
        try {
            sendFcmTokenWithAuth(api { error("must not send") }, desired, owner,
                { throw CancellationException("cancel") }, { true })
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }

    @Test fun retryDeadlineSurvivesRestartAndCannotModifyNewRevision() {
        val disk = object : FcmJournalStorage {
            var bytes: String? = null
            override fun read() = bytes
            override fun write(value: String) { bytes = value }
        }
        val journal = FcmTokenJournal(disk)
        val sent = journal.update("uid-a", "local-test-token")
        assertTrue(journal.deferRetry(sent, 1000, 120))
        val deferred = FcmTokenJournal(disk).snapshot()
        assertEquals(1120L, deferred.retryNotBeforeEpochSeconds)
        assertTrue(deferred.pending)
        assertEquals(sent.revision, deferred.revision)
        assertFalse(journal.acknowledge(sent))
        val newer = journal.update("uid-b", "local-test-token")
        assertEquals(0L, newer.retryNotBeforeEpochSeconds)
        assertFalse(journal.deferRetry(deferred, 1000, 86400))
        assertEquals(newer, journal.snapshot())
    }
}
