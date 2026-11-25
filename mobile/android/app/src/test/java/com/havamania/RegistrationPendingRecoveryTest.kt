package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class RegistrationPendingRecoveryTest {
    private class Disk : RegistrationPendingStore {
        var raw: String? = null
        var failWrite = false
        override fun read() = raw
        override fun write(value: String?) {
            if (failWrite) throw java.io.IOException("local-disk-failure")
            raw = value
        }
    }
    private val user = RegistrationIdentity("a", "demo@example.invalid")

    @Test fun boundFailureReopensWithoutSecondAccountCreationAndClearsAfterSuccess() = runBlocking {
        val disk = Disk(); val gate = RegistrationPreparation(disk)
        var current: RegistrationIdentity? = null
        try {
            gate.finish("Demo", user.email!!, { current }, { current = user; user }, { _, _ -> },
                { _, _, _ -> throw java.io.IOException("local-offline") })
        } catch (_: java.io.IOException) {}
        val reopened = RegistrationPreparation(disk)
        assertTrue(reopened.blocked); assertEquals("a", reopened.recovery?.uid); assertEquals("Demo", reopened.recovery?.name)
        var saved = false
        reopened.finish("Demo", user.email!!, { user }, { error("Must not create another account") }, { _, _ -> },
            { uid, _, _ -> assertEquals("a", uid); saved = true })
        assertTrue(saved); assertFalse(reopened.blocked); assertFalse(RegistrationPreparation(disk).blocked)
    }

    @Test fun reservationIsOnDiskBeforeCreateAndUidBeforeProfileMutation() = runBlocking {
        val disk = Disk(); var current: RegistrationIdentity? = null
        RegistrationPreparation(disk).finish("Demo", user.email!!, { current }, {
            assertTrue(RegistrationPreparation(disk).blocked)
            assertNull(RegistrationPreparation(disk).recovery?.uid)
            current = user; user
        }, { _, _ -> assertEquals("a", RegistrationPreparation(disk).recovery?.uid) }, { _, _, _ -> })
        assertNull(disk.raw)
    }

    @Test fun failedReservationWriteCannotCreateSdkAccount() = runBlocking {
        val disk = Disk().apply { failWrite = true }; var creates = 0
        try {
            RegistrationPreparation(disk).finish("Demo", user.email!!, { null }, { creates++; user }, { _, _ -> }, { _, _, _ -> })
            fail("Expected disk failure")
        } catch (_: java.io.IOException) {}
        assertEquals(0, creates)
    }

    @Test fun clearFailureRetainsBoundIntentAndCannotReportReady() = runBlocking {
        val disk = Disk(); var current: RegistrationIdentity? = null
        val gate = RegistrationPreparation(disk)
        try {
            gate.finish("Demo", user.email!!, { current }, { current = user; user }, { _, _ -> }, { _, _, _ -> disk.failWrite = true })
            fail("Expected clear failure")
        } catch (_: java.io.IOException) {}
        assertTrue(gate.blocked); assertEquals("a", RegistrationPreparation(disk).recovery?.uid)
    }

    @Test fun corruptOrFutureJournalBlocksUntilExplicitRepair() {
        for (bytes in listOf("not-json", "{\"version\":99,\"name\":\"Demo\",\"email\":\"demo@example.invalid\"}")) {
            val disk = Disk().apply { raw = bytes }; val gate = RegistrationPreparation(disk)
            assertTrue(gate.blocked); assertNull(gate.recovery)
            gate.clear(); assertFalse(RegistrationPreparation(disk).blocked)
        }
    }

    @Test fun boundUidCannotBeAdoptedByAnotherSessionAfterReopen() = runBlocking {
        val disk = Disk().apply { raw = PendingRegistration(name = "Owner A", email = user.email!!, uid = "a").encode() }
        var writes = 0
        try {
            RegistrationPreparation(disk).finish("Owner A", user.email!!, { RegistrationIdentity("b", user.email) },
                { error("No create") }, { _, _ -> writes++ }, { _, _, _ -> writes++ })
            fail("Expected owner rejection")
        } catch (_: RegistrationPreparationException) {}
        assertEquals(0, writes); assertEquals("a", RegistrationPreparation(disk).recovery?.uid)
    }
    @Test fun failedUidBindingCannotMutateProfileAndReopensUnbound() = runBlocking {
        val disk = Disk(); var current: RegistrationIdentity? = null; var writes = 0
        try {
            RegistrationPreparation(disk).finish("Demo", user.email!!, { current }, {
                current = user; disk.failWrite = true; user
            }, { _, _ -> writes++ }, { _, _, _ -> writes++ })
            fail("Expected disk failure")
        } catch (_: java.io.IOException) {}
        assertEquals(0, writes)
        val reopened = RegistrationPreparation(disk)
        assertTrue(reopened.blocked); assertNull(reopened.recovery?.uid)
    }
}