package com.havamania

import java.util.UUID
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class FcmAuthRecoveryTest {
    private class Disk : FcmJournalStorage {
        var bytes: String? = null
        override fun read() = bytes
        override fun write(value: String) { bytes = value }
    }
    private val owner = UUID.randomUUID().toString()
    private val desired = FcmTokenRegistration(UUID.randomUUID().toString(), 1, "a", "local-test-token", true)

    @Test fun invalidUserStopsBeforeHttpAndAfterSingle401Refresh() = runBlocking {
        for (failOnRefresh in listOf(false, true)) {
            var calls = 0
            val api = object : BackendFcmService {
                override suspend fun registerPushToken(authorization: String, request: BackendFcmRegistration): Response<BackendFcmReceipt> {
                    calls++; return Response.error(401, "{}".toResponseBody())
                }
            }
            val result = sendFcmTokenWithAuth(api, desired, owner, { refresh ->
                if (!failOnRefresh || refresh) throw FcmAuthRequiredException()
                "Bearer local-test"
            }, { true })
            assertEquals(FcmGatewayAction.AUTH_REQUIRED, result.action)
            assertEquals(if (failOnRefresh) 1 else 0, calls)
        }
    }

    @Test fun blockedAuthenticationSurvivesReopenWithoutAckOrNewHttp() = runBlocking {
        val disk = Disk()
        val queue = FcmTokenJournal(disk).apply { update("a", "local-test-token") }
        assertEquals(FcmWorkDecision.FINISH, runFcmSyncAttempt(queue, { "a" }, { 1 }, { false }, { 1000 }) { _, _ ->
            FcmGatewayResult(FcmGatewayAction.AUTH_REQUIRED)
        })
        val reopened = FcmTokenJournal(disk)
        assertTrue(reopened.snapshot().pending)
        assertTrue(reopened.snapshot().authRequired)
        assertFalse(reopened.acknowledge(reopened.snapshot()))
        assertEquals(FcmWorkDecision.FINISH, runFcmSyncAttempt(reopened, { "a" }, { 1 }, { false }, { 1000000 }) { _, _ ->
            fail("Blocked auth must not send"); FcmGatewayResult(FcmGatewayAction.APPLIED)
        })
        reopened.update("a", "local-test-token")
        assertTrue(reopened.snapshot().authRequired)
        reopened.clearAccount()
        reopened.update("a", "new-local-test-token")
        assertFalse(reopened.snapshot().authRequired)
        assertEquals(FcmWorkDecision.FINISH, runFcmSyncAttempt(reopened, { "a" }, { 2 }, { false }, { 1000000 }) { _, _ ->
            FcmGatewayResult(FcmGatewayAction.APPLIED)
        })
        assertFalse(reopened.snapshot().pending)
    }

    @Test fun transientTokenFailureRetriesAndAccountSwitchCannotBlockNewOwner() = runBlocking {
        val api = object : BackendFcmService {
            override suspend fun registerPushToken(authorization: String, request: BackendFcmRegistration): Response<BackendFcmReceipt> =
                error("No HTTP expected")
        }
        val transient = sendFcmTokenWithAuth(api, desired, owner,
            { throw java.io.IOException("local-offline") }, { true })
        assertEquals(FcmGatewayAction.RETRY, transient.action)
        assertEquals(30L, transient.retryAfterSeconds)
        var current = true
        val stale = sendFcmTokenWithAuth(api, desired, owner,
            { current = false; throw FcmAuthRequiredException() }, { current })
        assertEquals(FcmGatewayAction.SUPERSEDED, stale.action)
    }

    @Test fun lateAuthFailureCannotMarkNewRevision() {
        val queue = FcmTokenJournal(Disk()).apply { update("a", "local-test-token") }
        val old = queue.snapshot()
        val newer = queue.update("b", "local-test-token")
        assertFalse(queue.requireAuthentication(old))
        assertEquals(newer, queue.snapshot())
        assertFalse(queue.snapshot().authRequired)
    }
}
