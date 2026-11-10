package com.havamania

import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Constructs SDK exception objects only; never signs in or contacts Firebase. */
class FcmAuthLocalDeviceTest {
    @Test fun sdkInvalidUserErrorsAreConvertedWithoutPrivateCause() = runBlocking {
        for (code in listOf("ERROR_USER_DISABLED", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_USER_TOKEN", "ERROR_USER_TOKEN_EXPIRED")) {
            try {
                fcmAuthorization { throw FirebaseAuthInvalidUserException(code, "PRIVATE_PROVIDER_DETAIL") }
                fail("Must require authentication")
            } catch (error: FcmAuthRequiredException) {
                assertNull(error.cause)
                assertFalse(error.toString().contains("PRIVATE_"))
            }
        }
    }

    @Test fun authenticationBlockSurvivesRealFileReopenUntilNewRegistration() = runBlocking {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "fcm-auth-test-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        val file = File(root, "journal.json")
        try {
            val first = FcmTokenJournal(AtomicFcmJournalStorage(file)).apply { update("local-a", "local-test-token") }
            runFcmSyncAttempt(first, { "local-a" }, { 1 }, { false }, { 1000 }) { _, _ ->
                FcmGatewayResult(FcmGatewayAction.AUTH_REQUIRED)
            }
            val reopened = FcmTokenJournal(AtomicFcmJournalStorage(file))
            assertTrue(reopened.snapshot().pending && reopened.snapshot().authRequired)
            assertEquals(FcmWorkDecision.FINISH, runFcmSyncAttempt(reopened, { "local-a" }, { 1 }, { false }, { 1000000 }) { _, _ ->
                fail("Blocked auth cannot send"); FcmGatewayResult(FcmGatewayAction.APPLIED)
            })
            val latest = reopened.update("local-b", "local-test-token")
            assertFalse(latest.authRequired)
            assertFalse(FcmTokenJournal(AtomicFcmJournalStorage(file)).snapshot().authRequired)
        } finally {
            for (suffix in listOf("", ".new", ".bak")) {
                val part = File(file.path + suffix)
                if (part.exists()) assertTrue(part.delete())
            }
            assertTrue(root.delete())
        }
    }
}
