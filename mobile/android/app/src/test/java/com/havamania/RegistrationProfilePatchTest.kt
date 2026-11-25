package com.havamania
import org.junit.Assert.*
import org.junit.Test

class RegistrationProfilePatchTest {
    @Test fun recoveryDoesNotReplaceNewerProfileOrBumpTimestamp() {
        val existing = mapOf<String, Any?>("uid" to "a", "name" to "Newer Name", "email" to "new@example.invalid",
            "createdAt" to 123L, "updatedAt" to 456L, "onboardingCompleted" to true, "isPremium" to true)
        assertTrue(registrationProfilePatch("a", "Old Draft", "old@example.invalid", existing, true, 999L).isEmpty())
    }
    @Test fun recoveryFillsMissingFieldsAndPreservesUnrelatedData() {
        val patch = registrationProfilePatch("a", "Demo", "demo@example.invalid", mapOf("bio" to "keep"), true, 999L)
        assertEquals("a", patch["uid"]); assertEquals("Demo", patch["name"])
        assertEquals(999L, patch["createdAt"]); assertEquals(999L, patch["updatedAt"])
        assertFalse(patch.containsKey("bio")); assertFalse(patch.containsKey("isPremium"))
    }
    @Test fun explicitRegistrationMayUpdateNameButCannotResetCreationDate() {
        val patch = registrationProfilePatch("a", "Entered Name", "demo@example.invalid",
            mapOf("uid" to "a", "name" to "Earlier", "createdAt" to 123L), false, 999L)
        assertEquals("Entered Name", patch["name"]); assertFalse(patch.containsKey("createdAt"))
        assertEquals(999L, patch["updatedAt"])
    }
}