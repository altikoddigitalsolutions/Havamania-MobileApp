package com.havamania

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FcmTokenJournalTest {
    private class Disk : FcmJournalStorage {
        var bytes: String? = null
        var fail = false
        override fun read() = bytes
        override fun write(value: String) { check(!fail); bytes = value }
    }
    @Test fun pendingRegistrationAndInstallationIdentitySurviveRecreation() {
        val disk = Disk()
        val first = FcmTokenJournal(disk).update("a", "local-token-123")
        val reopened = FcmTokenJournal(disk)
        assertEquals(first, reopened.snapshot())
        assertTrue(first.pending)
        assertEquals(first, reopened.update("a", "local-token-123"))
        assertFalse(first.toString().contains("local-token-123"))
        assertFalse(first.toString().contains(first.installationId))
    }
    @Test fun oldAcknowledgementCannotClearNewOwnerOrNewToken() {
        val journal = FcmTokenJournal(Disk())
        val a = journal.update("a", "local-token-123")
        val b = journal.update("b", "local-token-123")
        assertTrue(b.revision > a.revision)
        assertFalse(journal.acknowledge(a))
        val rotated = journal.update("b", "new-local-token")
        assertFalse(journal.acknowledge(b))
        assertTrue(journal.acknowledge(rotated))
        assertFalse(journal.snapshot().pending)
    }
    @Test fun failedDiskWriteDoesNotAdvanceMemoryAndCorruptJournalFailsClosed() {
        val disk = Disk()
        val journal = FcmTokenJournal(disk)
        val saved = journal.update("a", "local-token-123")
        disk.fail = true
        try { journal.update("b", "local-token-123"); fail("write must fail") } catch (_: IllegalStateException) { }
        assertEquals(saved, journal.snapshot())
        assertEquals(saved, FcmTokenJournal(disk).snapshot())
        disk.bytes = "corrupt"
        try { FcmTokenJournal(disk); fail("must not silently replace installation identity") } catch (_: IllegalArgumentException) { }
    }
    @Test fun clearingAccountRemovesAccountAndTokenButKeepsRevisionTombstone() {
        val disk = Disk()
        val journal = FcmTokenJournal(disk)
        val first = journal.update("a", "local-token-123")
        val cleared = journal.clearAccount()
        assertNull(cleared.ownerUid)
        assertNull(cleared.token)
        assertFalse(cleared.pending)
        assertTrue(cleared.revision > first.revision)
        assertEquals(first.installationId, cleared.installationId)
        assertFalse(disk.bytes!!.contains("local-token-123"))
    }
    @Test fun staleInFlightResultCannotClearLatestDesiredState() = runBlocking {
        val journal = FcmTokenJournal(Disk())
        journal.update("a", "local-token-123")
        var uid: String? = "a"
        var epoch = 1L
        val started = CompletableDeferred<Unit>()
        val reply = CompletableDeferred<Boolean>()
        val old = async { syncPendingFcmToken(journal, { uid }, { epoch }, { false }) {
            started.complete(Unit); reply.await()
        } }
        started.await()
        uid = "b"; epoch++
        journal.update(uid, "local-token-123")
        reply.complete(true)
        assertEquals(FcmSyncResult.SUPERSEDED, old.await())
        assertTrue(journal.snapshot().pending)
        assertEquals(FcmSyncResult.APPLIED, syncPendingFcmToken(journal, { uid }, { epoch }, { false }) { true })
    }
    @Test fun offlineFailureKeepsRetryAndDeletionNeverCallsGateway() = runBlocking {
        val journal = FcmTokenJournal(Disk())
        journal.update("a", "local-token-123")
        assertEquals(FcmSyncResult.RETRY, syncPendingFcmToken(journal, { "a" }, { 1 }, { false }) { throw java.io.IOException() })
        assertTrue(journal.snapshot().pending)
        assertEquals(FcmSyncResult.SUPERSEDED, syncPendingFcmToken(journal, { "a" }, { 1 }, { true }) { fail("no HTTP"); true })
        assertEquals(FcmSyncResult.SUPERSEDED, syncPendingFcmToken(journal, { null }, { 1 }, { false }) { fail("no HTTP"); true })
    }
}
