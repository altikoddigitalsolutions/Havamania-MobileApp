package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class RegistrationPreparationTest {
    private class Harness {
        val gate = RegistrationPreparation()
        var user: RegistrationIdentity? = null
        var creates = 0; var updates = 0; var saves = 0
        var failSave = false
        var afterUpdate: () -> Unit = {}
        val events = mutableListOf<String>()
        suspend fun finish(email: String = " demo@example.invalid ", pauseSave: suspend () -> Unit = {}) {
            gate.finish(" Demo Gezgin ", email, { user }, { address ->
                creates++; events += "create"
                RegistrationIdentity("a", address).also { user = it }
            }, { uid, name ->
                assertEquals("a", uid); assertEquals("Demo Gezgin", name)
                updates++; events += "update"; afterUpdate()
            }, { uid, name, address ->
                assertEquals("a", uid); assertEquals("Demo Gezgin", name)
                assertEquals("demo@example.invalid", address.lowercase())
                saves++; events += "save"; pauseSave()
                if (failSave) throw java.io.IOException("local-offline")
            })
        }
    }

    @Test fun preparationStaysBlockedUntilProfileWriteCompletes() = runBlocking {
        val h = Harness(); val reply = CompletableDeferred<Unit>()
        val work = launch(start = CoroutineStart.UNDISPATCHED) { h.finish(pauseSave = { reply.await() }) }
        assertEquals(listOf("create", "update", "save"), h.events)
        assertTrue(h.gate.blocked); assertTrue(work.isActive)
        reply.complete(Unit); work.join()
        assertFalse(h.gate.blocked); assertEquals(1, h.creates)
    }

    @Test fun retryAfterProfileFailureUsesSameSdkAccount() = runBlocking {
        val h = Harness(); h.failSave = true
        try { h.finish(); fail("Expected failure") } catch (_: java.io.IOException) {}
        assertTrue(h.gate.blocked)
        h.failSave = false; h.finish("DEMO@example.invalid")
        assertEquals(1, h.creates); assertEquals(2, h.saves); assertFalse(h.gate.blocked)
    }

    @Test fun differentAddressCannotResumeAnotherPendingRegistration() = runBlocking {
        val h = Harness(); h.failSave = true
        try { h.finish() } catch (_: java.io.IOException) {}
        try { h.finish("other@example.invalid"); fail("Expected rejection") }
        catch (_: RegistrationPreparationException) {}
        assertEquals(1, h.creates); assertEquals(1, h.saves); assertTrue(h.gate.blocked)
    }

    @Test fun ownerSwitchBeforeSavePreventsOldOwnerWrite() = runBlocking {
        val h = Harness(); h.afterUpdate = { h.user = RegistrationIdentity("b", "other@example.invalid") }
        try { h.finish(); fail("Expected rejection") } catch (_: RegistrationPreparationException) {}
        assertEquals(0, h.saves); assertTrue(h.gate.blocked)
    }

    @Test fun clearWhileWriteIsInFlightCannotPublishRegistrationReady() = runBlocking {
        val h = Harness(); var ready = false
        try {
            h.finish(pauseSave = { h.gate.clear() })
            ready = true
        } catch (_: RegistrationPreparationException) {}
        assertFalse(ready); assertFalse(h.gate.blocked)
    }

    @Test fun cancelledPreparationRemainsRetryableWithoutRetainingPassword() = runBlocking {
        val h = Harness(); val wait = CompletableDeferred<Unit>()
        val work = launch(start = CoroutineStart.UNDISPATCHED) { h.finish(pauseSave = { wait.await() }) }
        work.cancelAndJoin()
        assertTrue(h.gate.blocked)
        h.finish(); assertEquals(1, h.creates); assertFalse(h.gate.blocked)
    }

    @Test fun createdAccountWithLostAwaitResultCanResume() = runBlocking {
        val gate = RegistrationPreparation(); var user: RegistrationIdentity? = null
        var creates = 0; var saves = 0
        try {
            gate.finish("Demo", "demo@example.invalid", { user }, {
                creates++; user = RegistrationIdentity("a", it)
                throw java.io.IOException("local-lost-reply")
            }, { _, _ -> fail("No update before create returns") }, { _, _, _ -> fail("No write") })
        } catch (_: java.io.IOException) {}
        gate.finish("Demo", "demo@example.invalid", { user }, { error("Must reuse SDK session") },
            { _, _ -> }, { _, _, _ -> saves++ })
        assertEquals(1, creates); assertEquals(1, saves); assertFalse(gate.blocked)
    }

    @Test fun unrelatedExistingSessionCannotCreateRegistration() = runBlocking {
        val h = Harness(); h.user = RegistrationIdentity("b", "other@example.invalid")
        try { h.finish(); fail("Expected rejection") } catch (_: RegistrationPreparationException) {}
        assertEquals(0, h.creates); assertEquals(0, h.saves); assertFalse(h.gate.blocked)
    }
    @Test fun creationFailureWithoutSdkSessionAllowsAddressCorrection() = runBlocking {
        val gate = RegistrationPreparation(); var user: RegistrationIdentity? = null
        try {
            gate.finish("Demo", "wrong@example.invalid", { user }, { throw java.io.IOException("local-rejected") },
                { _, _ -> fail("No account") }, { _, _, _ -> fail("No account") })
            fail("Expected failure")
        } catch (_: java.io.IOException) {}
        assertFalse(gate.blocked)
        var saved = false
        gate.finish("Demo", "corrected@example.invalid", { user }, {
            RegistrationIdentity("a", it).also { identity -> user = identity }
        }, { _, _ -> }, { _, _, address -> assertEquals("corrected@example.invalid", address); saved = true })
        assertTrue(saved); assertFalse(gate.blocked)
    }
}