package com.havamania

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AccountDeletionAuthenticationTest {
    private fun signedIn(): AuthSessionFence = AuthSessionFence(MemoryRegistrationPendingStore()).apply {
        accept(begin(), "a")
    }
    @Test fun validOwnerReauthenticatesThenGetsToken() = runBlocking {
        val fence = signedIn(); val calls = mutableListOf<String>()
        val token = prepareDeletionOwner("a", { "a" }, fence,
            { calls.add("availability"); true },
            { calls.add("reauth"); fence.accept(fence.begin(), "a") },
            { calls.add("token"); "proof" })
        assertEquals("proof", token); assertEquals(listOf("availability", "reauth", "token"), calls)
    }
    @Test fun unavailableServiceDoesNotAuthenticateOrRequestToken() = runBlocking {
        assertNull(prepareDeletionOwner("a", { "a" }, signedIn(), { false },
            { fail("No authentication") }, { fail("No token"); "proof" }))
    }
    @Test fun accountChangeDuringAvailabilityCannotSelectNewDeletionOwner() = runBlocking {
        var owner = "a"; val fence = signedIn()
        try {
            prepareDeletionOwner("a", { owner }, fence, {
                owner = "b"; fence.lockSignedOut(); fence.accept(fence.begin(), "b"); true
            }, { fail("Cannot authenticate B") }, { fail("Cannot request token"); "proof" })
            fail("Must reject changed account")
        } catch (_: IllegalStateException) { }
    }
    @Test fun sameUidReloginDuringAvailabilityStillInvalidatesOldIntent() = runBlocking {
        val fence = signedIn()
        try {
            prepareDeletionOwner("a", { "a" }, fence, {
                fence.lockSignedOut(); fence.accept(fence.begin(), "a"); true
            }, { fail("Old intent must not reauthenticate") }, { "proof" })
            fail("Same UID is not the same session")
        } catch (_: IllegalStateException) { }
    }
    @Test fun logoutDuringTokenWaitCannotReturnDeletionCredentials() = runBlocking {
        val fence = signedIn()
        try {
            awaitDeletionOwnerToken("a", { null }, fence) { fail("No token after logout"); "proof" }
            fail("Must reject")
        } catch (_: IllegalStateException) { }
        try {
            awaitDeletionOwnerToken("a", { "a" }, fence) {
                fence.lockSignedOut(); fence.accept(fence.begin(), "a"); "stale-proof"
            }
            fail("Stale token must not proceed to journal/backend")
        } catch (_: IllegalStateException) { }
    }
}
