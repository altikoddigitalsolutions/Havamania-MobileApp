package com.havamania

import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class BackendFcmServiceTest {
    private val owner = UUID.randomUUID().toString()
    private val desired = FcmTokenRegistration(UUID.randomUUID().toString(), 7, "uid-a", "private-device-token", true)
    private fun receipt() = BackendFcmReceipt("record", owner, "android", desired.token!!, desired.installationId, desired.revision, 1)
    private fun service(reply: suspend (BackendFcmRegistration) -> Response<BackendFcmReceipt>) = object : BackendFcmService {
        override suspend fun registerPushToken(authorization: String, request: BackendFcmRegistration): Response<BackendFcmReceipt> {
            assertEquals("Bearer local-test-id-token", authorization)
            return reply(request)
        }
    }

    @Test fun matchingReceiptConfirmsExactSnapshotAndRedactsTokens() = runBlocking {
        val api = service { request ->
            assertEquals(desired.token, request.token)
            assertEquals(desired.installationId, request.installation_id)
            assertEquals(7L, request.revision)
            assertEquals(1, request.owner_protocol)
            assertFalse(request.toString().contains("private-device-token"))
            Response.success(receipt())
        }
        assertTrue(registerFcmTokenSnapshot(api, desired, owner, "Bearer local-test-id-token") { true })
        assertFalse(receipt().toString().contains("private-device-token"))
    }

    @Test fun mismatchedOrLegacyReceiptsCannotAcknowledge() = runBlocking {
        val valid = receipt()
        for (invalid in listOf(valid.copy(user_id = "other"), valid.copy(platform = "ios"),
            valid.copy(token = "rotated-token"), valid.copy(installation_id = null),
            valid.copy(revision = 6), valid.copy(owner_protocol = 0), valid.copy(id = ""))) {
            assertFalse(registerFcmTokenSnapshot(service { Response.success(invalid) }, desired,
                owner, "Bearer local-test-id-token") { true })
        }
    }

    @Test fun sessionChangeBeforeOrDuringHttpRejectsResult() = runBlocking {
        var calls = 0
        var current = true
        val api = service { calls++; current = false; Response.success(receipt()) }
        assertFalse(registerFcmTokenSnapshot(api, desired, owner, "Bearer local-test-id-token") { false })
        assertEquals(0, calls)
        assertFalse(registerFcmTokenSnapshot(api, desired, owner, "Bearer local-test-id-token") { current })
        assertEquals(1, calls)
    }

    @Test fun cancellationIsNotConvertedToAcknowledgement() = runBlocking {
        try {
            registerFcmTokenSnapshot(service { throw CancellationException("cancel") }, desired,
                owner, "Bearer local-test-id-token") { true }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }
}
