package com.havamania

import android.util.AtomicFile
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.nio.charset.CharacterCodingException
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

/** Random test files only. Never opens the production journal, Firebase or HTTP. */
class FcmJournalStorageDeviceTest {
    private fun isolated(block: (File) -> Unit) {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "fcm-journal-test-" + UUID.randomUUID())
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

    @Test fun registrationAndRetryDeadlineSurviveRealFileReopening() = isolated { file ->
        val journal = FcmTokenJournal(AtomicFcmJournalStorage(file))
        val sent = journal.update("test-owner-a", "local-test-token")
        assertTrue(journal.deferRetry(sent, 1000, 120))
        val reopened = FcmTokenJournal(AtomicFcmJournalStorage(file))
        assertEquals(sent.installationId, reopened.snapshot().installationId)
        assertEquals(1120L, reopened.snapshot().retryNotBeforeEpochSeconds)
        assertFalse(reopened.acknowledge(sent))
        val newer = reopened.update("test-owner-b", "local-test-token")
        assertFalse(reopened.acknowledge(sent))
        assertTrue(reopened.acknowledge(newer))
        assertFalse(FcmTokenJournal(AtomicFcmJournalStorage(file)).snapshot().pending)
    }

    @Test fun interruptedNewWritePreservesCommittedIdentity() = isolated { file ->
        val sent = FcmTokenJournal(AtomicFcmJournalStorage(file)).update("test-owner-a", "local-test-token")
        // Closing without finishWrite/failWrite simulates an interrupted writer;
        // no target process or user application is killed.
        AtomicFile(file).startWrite().use { it.write("incomplete".toByteArray()) }
        assertEquals(sent, FcmTokenJournal(AtomicFcmJournalStorage(file)).snapshot())
    }

    @Test fun legacyBackupRestoresLastCommittedRecord() = isolated { file ->
        val sent = FcmTokenJournal(AtomicFcmJournalStorage(file)).update("test-owner-a", "local-test-token")
        file.copyTo(File(file.path + ".bak"))
        file.writeText("corrupt base")
        assertEquals(sent, FcmTokenJournal(AtomicFcmJournalStorage(file)).snapshot())
    }

    @Test fun oversizedReadAndWriteFailWithoutReplacingCommittedRecord() = isolated { file ->
        val storage = AtomicFcmJournalStorage(file)
        storage.write("valid")
        try { storage.write("x".repeat(4097)); fail("Oversized write") }
        catch (_: IllegalStateException) { }
        assertEquals("valid", storage.read())
        file.writeBytes(ByteArray(4097) { 120 })
        try { storage.read(); fail("Oversized read") }
        catch (_: IllegalStateException) { }
        assertEquals(4097L, file.length())
    }

    @Test fun invalidUtf8AndCorruptJsonNeverResetInstallation() = isolated { file ->
        val storage = AtomicFcmJournalStorage(file)
        file.writeBytes(byteArrayOf(0xc3.toByte(), 0x28))
        try { storage.read(); fail("Invalid UTF-8") }
        catch (_: CharacterCodingException) { }
        assertArrayEquals(byteArrayOf(0xc3.toByte(), 0x28), file.readBytes())
        file.writeText("corrupt-json")
        try { FcmTokenJournal(storage); fail("Corrupt identity must not reset") }
        catch (_: IllegalArgumentException) { }
        assertEquals("corrupt-json", file.readText())
    }
}
