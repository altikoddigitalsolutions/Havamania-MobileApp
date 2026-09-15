package com.havamania

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountDeletionProtocolTest {
    @Test fun onlyExplicitCompletionPermitsLocalCleanup() {
        assertEquals(DeletionResult.COMPLETE, deletionResult(200, "complete"))
        listOf(200 to null, 200 to "pending", 202 to "complete", 500 to "complete", 204 to null)
            .forEach { (code, status) -> assertEquals(DeletionResult.RETRY, deletionResult(code, status)) }
    }

    @Test fun acceptedAndUnauthenticatedRequestsStayRecoverable() {
        assertEquals(DeletionResult.PENDING, deletionResult(202, "pending"))
        assertEquals(DeletionResult.AUTHENTICATE, deletionResult(401, null))
        assertEquals(DeletionResult.RETRY, deletionResult(503, null))
    }
}
