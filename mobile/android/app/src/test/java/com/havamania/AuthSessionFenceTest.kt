package com.havamania

import org.junit.Assert.*
import org.junit.Test

class AuthSessionFenceTest {
    private class Disk : RegistrationPendingStore {
        var text: String? = null
        var fail = false
        override fun read() = text
        override fun write(value: String?) { if (fail) throw java.io.IOException("local-disk-failure"); text = value }
    }
    @Test fun logoutAndUnfinishedLoginCannotRestoreCachedUserAfterReopen() {
        val disk = Disk(); val fence = AuthSessionFence(disk)
        assertTrue(fence.canRestore("a"))
        val ticket = fence.begin()
        assertFalse(AuthSessionFence(disk).canRestore("a"))
        fence.accept(ticket, "a")
        assertTrue(AuthSessionFence(disk).canRestore("a"))
        assertFalse(AuthSessionFence(disk).canRestore("b"))
        fence.lockSignedOut()
        assertFalse(AuthSessionFence(disk).canRestore("a"))
        assertTrue(AuthSessionFence(disk).signedOut)
    }
    @Test fun staleSdkResultIsReconciledBeforeNewAuthenticationMayBegin() {
        val fence = AuthSessionFence(Disk()); val old = fence.begin(); val raw = fence.reserveSdk(old)
        fence.lockSignedOut()
        try { fence.begin(); fail("Must wait for SDK") } catch (_: RegistrationPreparationException) {}
        var cleanups = 0
        fence.finishSdk(raw, old) {
            cleanups++
            try { fence.begin(); fail("Cleanup must precede unlocking") } catch (_: RegistrationPreparationException) {}
        }
        val next = fence.begin(); fence.accept(next, "b")
        fence.finishSdk(raw, old) { fail("Duplicate callback cannot sign out B") }
        assertEquals(1, cleanups); assertTrue(fence.canRestore("b")); assertFalse(fence.canRestore("a"))
    }
    @Test fun validTaskCompletesOnceAndOnlyItsTicketCanAccept() {
        val fence = AuthSessionFence(Disk()); val ticket = fence.begin(); val raw = fence.reserveSdk(ticket)
        fence.finishSdk(raw, ticket) { fail("Current task is not stale") }
        fence.accept(ticket, "a")
        try { fence.accept(Any(), "b"); fail("Wrong ticket") } catch (_: IllegalStateException) {}
        assertTrue(fence.canRestore("a"))
    }
    @Test fun viewModelAbandonmentDoesNotAuthorizeItsLateTask() {
        val fence = AuthSessionFence(Disk()); val ticket = fence.begin(); val raw = fence.reserveSdk(ticket)
        fence.abandon(ticket)
        var cleaned = false
        fence.finishSdk(raw, ticket) { cleaned = true }
        assertTrue(cleaned); assertFalse(fence.canRestore("a"))
        assertTrue(fence.mayRecoverRegistration("a"))
        fence.lockSignedOut(); assertFalse(fence.mayRecoverRegistration("a"))
    }
    @Test fun staleAbandonCannotAffectNewTicket() {
        val fence = AuthSessionFence(Disk()); val first = fence.begin(); fence.abandon(first)
        val second = fence.begin(); fence.abandon(first); fence.accept(second, "b")
        assertTrue(fence.canRestore("b"))
    }
    @Test fun persistenceFailureBlocksAuthorizationAndDoesNotStartWork() {
        val disk = Disk(); val fence = AuthSessionFence(disk); disk.fail = true
        try { fence.begin(); fail("Expected I/O failure") } catch (_: java.io.IOException) {}
        assertFalse(fence.canRestore("a")); assertFalse(fence.mayRecoverRegistration("a"))
    }
    @Test fun corruptOrFutureSessionStateFailsClosed() {
        for (bytes in listOf("broken", "{\"version\":99,\"kind\":\"accepted\",\"uid\":\"a\"}")) {
            val disk = Disk().apply { text = bytes }; val fence = AuthSessionFence(disk)
            assertFalse(fence.canRestore("a")); assertFalse(fence.mayRecoverRegistration("a"))
            val ticket = fence.begin(); fence.accept(ticket, "b")
            assertTrue(fence.canRestore("b"))
        }
    }
    @Test fun profileMutationKeepsCurrentSessionButLogoutBlocksNextLoginUntilCleanup() {
        val fence = AuthSessionFence(Disk())
        val login = fence.begin(); fence.accept(login, "a")
        val (raw, anchor) = fence.reserveOwnerMutation("a")
        assertTrue(fence.canRestore("a")); assertFalse(fence.unresolved)
        assertFalse(fence.canStartProfileWrite("a"))
        try { fence.reserveOwnerMutation("b"); fail("Wrong owner") } catch (_: IllegalStateException) {}
        fence.lockSignedOut()
        assertFalse(fence.canRestore("a"))
        try { fence.begin(); fail("SDK mutation is still running") } catch (_: RegistrationPreparationException) {}
        var cleaned = false
        fence.finishSdk(raw, anchor) { cleaned = true }
        assertTrue(cleaned)
        val next = fence.begin(); fence.accept(next, "b")
        fence.finishSdk(raw, anchor) { fail("Duplicate completion must not sign out B") }
        assertTrue(fence.canRestore("b"))
    }
    @Test fun currentOwnerMayFinishConcurrentProfileTasksWithoutLosingSession() {
        val fence = AuthSessionFence(Disk())
        val first = fence.reserveOwnerMutation("a")
        val second = fence.reserveOwnerMutation("a")
        fence.finishSdk(first.first, first.second) { fail("Current owner") }
        assertTrue(fence.canRestore("a"))
        fence.finishSdk(second.first, second.second) { fail("Current owner") }
        assertTrue(fence.canRestore("a")); assertFalse(fence.unresolved)
    }
    @Test fun externalSignOutInvalidatesOutstandingProfileMutation() {
        val fence = AuthSessionFence(Disk()); val ticket = fence.begin(); fence.accept(ticket, "a")
        val task = fence.reserveOwnerMutation("a")
        fence.observeExternalSignOut()
        assertTrue(fence.signedOut)
        var cleaned = false
        fence.finishSdk(task.first, task.second) { cleaned = true }
        assertTrue(cleaned); assertFalse(fence.canRestore("a"))
    }

}