package com.havamania

import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

class ProfileReadStateTest {
    @Test fun validProfileRemainsOwnedByDocumentPath() {
        val profile = UserProfile(uid = "a", name = "Owner")
        assertEquals(ProfileReadState.Ready(profile), decodeObservedProfile("a") { profile })
    }
    @Test fun partialLegacyProfileCanInheritMissingUidWithoutMutation() {
        val partial = UserProfile(name = "Partial")
        val result = decodeObservedProfile("a") { partial } as ProfileReadState.Ready
        assertEquals("a", result.profile.uid)
        assertEquals("", partial.uid)
    }
    @Test fun anotherUidIsRejectedRatherThanPublishedOrRewritten() {
        val wrong = UserProfile(uid = "b", name = "Other")
        assertEquals(ProfileReadState.Failed("a", PROFILE_DATA_ERROR), decodeObservedProfile("a") { wrong })
        assertEquals("b", wrong.uid)
    }
    @Test fun malformedFieldsDoNotEscapeCallbackOrExposeRawData() {
        val result = decodeObservedProfile("a") { throw IllegalArgumentException("private-field-value") }
        assertEquals(ProfileReadState.Failed("a", PROFILE_DATA_ERROR), result)
        assertFalse((result as ProfileReadState.Failed).message.contains("private-field-value"))
    }
    @Test fun nullDecodeIsFailureNotMissingDocument() {
        val result = decodeObservedProfile("a") { null }
        assertTrue(result is ProfileReadState.Failed)
        assertFalse(result is ProfileReadState.Missing)
    }
    @Test fun actualCancellationStillPropagates() {
        try { decodeObservedProfile("a") { throw CancellationException("cancelled") }; fail("Cancellation swallowed") }
        catch (_: CancellationException) { }
    }
}
