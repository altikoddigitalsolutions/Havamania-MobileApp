package com.havamania

import org.junit.Assert.*
import org.junit.Test

class FcmEnvelopeTest {
    private val event = "d3667c20-5d15-42f3-a448-2ae58774b96b"
    private fun payload() = mapOf("protocol" to "1", "event_id" to event,
        "owner_backend_user_id" to "f956a692-5a48-5f1c-9652-1984131da3d9",
        "title" to "Fırtına uyarısı", "body" to "Yerel test mesajı", "target" to "weather", "expires_at" to "2000")
    private fun parse(data: Map<String,String> = payload(), uid: String? = "firebase-user", pending: Boolean = false) =
        validatedFcmEnvelope(data, "test-project", uid, pending, 1000)

    @Test fun backendIdentityMatchesPythonUuidV5AndChangesWithProjectOrUser() {
        assertEquals("f956a692-5a48-5f1c-9652-1984131da3d9", firebaseBackendUserId("test-project", "firebase-user"))
        assertNotEquals(firebaseBackendUserId("other-project", "firebase-user"), firebaseBackendUserId("test-project", "firebase-user"))
        assertNotEquals(firebaseBackendUserId("test-project", "other-user"), firebaseBackendUserId("test-project", "firebase-user"))
    }
    @Test fun delayedOldAccountMessageLogoutAndDeletionAreRejected() {
        assertEquals(event, parse()!!.eventId)
        assertNull(parse(uid = "other-user"))
        assertNull(parse(uid = null))
        assertNull(parse(uid = "legacy"))
        assertNull(parse(pending = true))
        assertNull(validatedFcmEnvelope(payload(), null, "firebase-user", false, 1000))
    }
    @Test fun invalidExpiryProtocolTargetAndIdentifierAreRejected() {
        mapOf("expires_at" to listOf("1000", "999", "87401", "oops", "9223372036854775808"),
            "protocol" to listOf("", "2"), "target" to listOf("https://example.invalid", "route/private"),
            "event_id" to listOf("", "not-uuid", "../../private")).forEach { (key, values) ->
            values.forEach { assertNull(parse(payload() + (key to it))) }
        }
    }
    @Test fun malformedOrOversizedContentCannotReachPresentation() {
        listOf("title" to " ", "title" to "x".repeat(201), "title" to "Title\nInjected",
            "body" to "x".repeat(1001), "body" to "bad\u0000text", "ignored" to "x".repeat(4096))
            .forEach { assertNull(parse(payload() + it)) }
    }
    @Test fun accountEpochDetectsSwitchBackToTheSameUid() {
        val first = NotificationSessionEpoch.capture()
        NotificationSessionEpoch.changed()
        NotificationSessionEpoch.changed()
        assertNotEquals(first, NotificationSessionEpoch.capture())
    }
}
