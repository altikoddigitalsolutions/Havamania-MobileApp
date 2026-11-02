package com.havamania

import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

/** No real credentials or HTTP. Storage fixtures use random cache paths only. */
class FcmWorkerLocalDeviceTest {
    private fun isolated(block: suspend (File) -> Unit) = runBlocking {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "fcm-attempt-test-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        val file = File(root, "journal.json")
        try { block(file) }
        finally {
            for (suffix in listOf("", ".new", ".bak")) {
                val part = File(file.path + suffix)
                if (part.exists()) assertTrue(part.delete())
            }
            assertTrue(root.delete())
        }
    }

    @Test fun realWorkManagerExecutesDisabledWorkerWithoutEnablingFcm() = runBlocking {
        assertFalse(BuildConfig.FCM_TOKEN_SYNC_ENABLED)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = WorkManager.getInstance(context)
        val work = OneTimeWorkRequestBuilder<FcmTokenSyncWorker>().addTag("local-fcm-test-" + UUID.randomUUID()).build()
        try {
            withTimeout(30000) {
                manager.enqueue(work).await()
                val info = manager.getWorkInfoByIdFlow(work.id).first { it?.state?.isFinished == true }
                assertEquals(WorkInfo.State.SUCCEEDED, info?.state)
            }
        } finally { withTimeout(10000) { manager.cancelWorkById(work.id).await() } }
    }

    @Test fun reopenedFileWaitsUntilDeadlineThenAcknowledgesOnlyMatchingState() = isolated { file ->
        var now = 1000L
        val first = FcmTokenJournal(AtomicFcmJournalStorage(file)).apply { update("local-a", "local-test-token") }
        assertEquals(FcmWorkDecision.RETRY, runFcmSyncAttempt(first, { "local-a" }, { 1 }, { false }, { now }) { _, _ ->
            FcmGatewayResult(FcmGatewayAction.RETRY, 120)
        })
        val reopened = FcmTokenJournal(AtomicFcmJournalStorage(file))
        now = 1119
        assertEquals(FcmWorkDecision.RETRY, runFcmSyncAttempt(reopened, { "local-a" }, { 1 }, { false }, { now }) { _, _ ->
            fail("No send before persisted deadline"); FcmGatewayResult(FcmGatewayAction.APPLIED)
        })
        now = 1120
        assertEquals(FcmWorkDecision.FINISH, runFcmSyncAttempt(reopened, { "local-a" }, { 1 }, { false }, { now }) { _, _ ->
            FcmGatewayResult(FcmGatewayAction.APPLIED)
        })
        assertFalse(FcmTokenJournal(AtomicFcmJournalStorage(file)).snapshot().pending)
    }

    @Test fun accountSwitchDuringFakeResponsePersistsNewOwnerWithoutOldAck() = isolated { file ->
        val queue = FcmTokenJournal(AtomicFcmJournalStorage(file)).apply { update("local-a", "local-test-token") }
        var uid = "local-a"
        var epoch = 1L
        assertEquals(FcmWorkDecision.FINISH, runFcmSyncAttempt(queue, { uid }, { epoch }, { false }, { 1000 }) { _, _ ->
            uid = "local-b"; epoch++
            queue.update(uid, "local-test-token")
            FcmGatewayResult(FcmGatewayAction.APPLIED)
        })
        val persisted = FcmTokenJournal(AtomicFcmJournalStorage(file)).snapshot()
        assertEquals("local-b", persisted.ownerUid)
        assertTrue(persisted.pending)
    }

    @Test fun retryWriteFailureBeforeOrAfterCommitCannotSendEarlyAcrossReopen() {
        for (afterCommit in listOf(false, true)) isolated { file ->
            val real = AtomicFcmJournalStorage(file)
            var inject = false
            val storage = object : FcmJournalStorage {
                override fun read() = real.read()
                override fun write(value: String) {
                    if (inject && !afterCommit) throw java.io.IOException("local-write-failure")
                    real.write(value)
                    if (inject) throw java.io.IOException("local-commit-uncertain")
                }
            }
            val queue = FcmTokenJournal(storage).apply { update("local-a", "local-test-token") }
            var calls = 0
            try {
                runFcmSyncAttempt(queue, { "local-a" }, { 1 }, { false }, { 1000 }) { _, _ ->
                    calls++; inject = true; FcmGatewayResult(FcmGatewayAction.RETRY, 120)
                }
                fail("Injected disk fault")
            } catch (_: java.io.IOException) { }
            inject = false
            for (recovered in listOf(queue, FcmTokenJournal(AtomicFcmJournalStorage(file)))) {
                assertEquals(FcmWorkDecision.RETRY, runFcmSyncAttempt(recovered,
                    { "local-a" }, { 1 }, { false }, { 1001 }) { _, _ ->
                    calls++; fail("Must not send early"); FcmGatewayResult(FcmGatewayAction.APPLIED)
                })
            }
            assertEquals(1, calls)
        }
    }
}
