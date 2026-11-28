package com.havamania

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AssistantPendingMessagesTest {
    private fun message(prompt: String = "Seyahatimde hava nasıl?", fromDraft: Boolean = false,
        owner: String = "A", conversation: String = "chat") = PendingAssistantMessage(
        prompt, null, false, null, conversation, owner, fromDraft, "trip-a")

    @Test fun editingDraftCancelsPendingQuestionsFromBothCardsAndInput() {
        for (fromDraft in listOf(false, true)) {
            val pending = AssistantPendingMessages()
            val version = pending.beginAcceptedSend()
            assertTrue(pending.queue(message(fromDraft = fromDraft), version))
            pending.onDraftChanged("", "Başka bir soru")
            assertNull(pending.take("A", "chat"))
            assertFalse(pending.queue(message(), version))
        }
    }
    @Test fun unchangedDraftKeepsQuestionAndApprovalConsumesItOnlyOnce() {
        val pending = AssistantPendingMessages()
        val version = pending.beginAcceptedSend()
        val value = message()
        pending.queue(value, version)
        pending.onDraftChanged("aynı", "aynı")
        assertEquals(value, pending.take("A", "chat"))
        assertNull(pending.take("A", "chat"))
        assertFalse(pending.queue(value, version))
    }
    @Test fun acceptedGreetingCannotLeaveAnEarlierRemoteQuestionForLaterApproval() = runBlocking {
        val pending = AssistantPendingMessages()
        val old = pending.beginAcceptedSend()
        pending.queue(message(), old)
        pending.beginAcceptedSend()
        var remoteCalls = 0
        assistantPromptReply("Merhaba") { remoteCalls++; "remote" }
        assertEquals(0, remoteCalls)
        assertNull(pending.take("A", "chat"))
        assertFalse(pending.queue(message(), old))
    }
    @Test fun lateErrorCannotOverwriteTheNewerAcceptedQuestion() {
        val pending = AssistantPendingMessages()
        val old = pending.beginAcceptedSend()
        val next = pending.beginAcceptedSend()
        val current = message("Yeni soru")
        assertTrue(pending.queue(current, next))
        assertFalse(pending.queue(message("Eski soru"), old))
        assertEquals(current, pending.take("A", "chat"))
    }
    @Test fun draftTypedDuringRequestPreventsLateConsentErrorFromRequeuingIt() {
        val pending = AssistantPendingMessages()
        val requestVersion = pending.beginAcceptedSend()
        pending.onDraftChanged("", "Sonraki sorum")
        assertFalse(pending.queue(message(), requestVersion))
        assertFalse(pending.hasPending)
    }
    @Test fun wrongOwnerOrConversationDiscardsQuestionAndCannotReviveOnReturn() {
        for ((owner, chat) in listOf("B" to "chat", "A" to "other-chat")) {
            val pending = AssistantPendingMessages()
            val version = pending.beginAcceptedSend()
            pending.queue(message(), version)
            assertNull(pending.take(owner, chat))
            assertNull(pending.take("A", "chat"))
            assertFalse(pending.queue(message(), version))
        }
    }
    @Test fun retryDraftAndTripMetadataSurviveAnUnchangedPendingQuestion() {
        val pending = AssistantPendingMessages()
        val value = message(fromDraft = true).copy(retry = true, requestId = "request", context = "context")
        val version = pending.beginAcceptedSend()
        pending.queue(value, version)
        assertEquals(value, pending.take("A", "chat"))
    }
    @Test fun checkingAnEmptyQueueDoesNotInvalidateAnActiveRequest() {
        val pending = AssistantPendingMessages()
        val version = pending.beginAcceptedSend()
        assertNull(pending.take("A", "chat"))
        assertTrue(pending.queue(message(), version))
        pending.clear()
        assertFalse(pending.queue(message(), version))
    }
}
