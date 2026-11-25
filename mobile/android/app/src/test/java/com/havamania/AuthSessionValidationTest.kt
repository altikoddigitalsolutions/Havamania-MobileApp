package com.havamania

import org.junit.Assert.*
import org.junit.Test

class AuthSessionValidationTest {
    @Test fun permanentOwnerFailuresRequireNewCredentials() {
        listOf("ERROR_USER_DISABLED", "ERROR_USER_NOT_FOUND", "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN")
            .forEach { assertTrue(isPermanentAuthSessionFailure(it)) }
    }
    @Test fun transientOrCredentialErrorsDoNotInvalidateExistingOwner() {
        listOf(null, "ERROR_NETWORK_REQUEST_FAILED", "ERROR_TOO_MANY_REQUESTS", "ERROR_WRONG_PASSWORD", "UNKNOWN")
            .forEach { assertFalse(isPermanentAuthSessionFailure(it)) }
    }
    @Test fun oldLeaseCannotInvalidateNewAuthenticationOrAcceptedOwner() {
        val fence = AuthSessionFence(MemoryRegistrationPendingStore())
        fence.accept(fence.begin(), "a"); val lease = fence.ownerLease("a")
        assertTrue(fence.canInvalidateOwnerLease(lease))
        val loginB = fence.begin()
        assertFalse(fence.canInvalidateOwnerLease(lease))
        fence.accept(loginB, "b")
        assertFalse(fence.canInvalidateOwnerLease(lease))
    }
}
