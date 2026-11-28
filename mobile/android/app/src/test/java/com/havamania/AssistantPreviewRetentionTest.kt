package com.havamania

import org.junit.Assert.*
import org.junit.Test

class AssistantPreviewRetentionTest {
    private fun photo(id: String, size: Int) = AssistantPhoto(id, ByteArray(size))
    @Test fun preservesMessageAssociationsAndEvictsOldestWithinBudget() {
        val first = retainAssistantPreviews(emptyMap(), "first", listOf(photo("a", 6)), 10)
        val next = retainAssistantPreviews(first, "second", listOf(photo("b", 6)), 10)
        assertEquals(setOf("second"), next.keys)
        assertEquals("b", next.getValue("second").single().id)
        assertEquals(setOf("first"), first.keys)
    }
    @Test fun replacementDoesNotDuplicateBytesAndOversizedBatchIsNotRetained() {
        val first = retainAssistantPreviews(emptyMap(), "message", listOf(photo("a", 6)), 10)
        val replaced = retainAssistantPreviews(first, "message", listOf(photo("b", 8)), 10)
        assertEquals("b", replaced.getValue("message").single().id)
        assertTrue(retainAssistantPreviews(emptyMap(), "large", listOf(photo("large", 11)), 10).isEmpty())
    }
    @Test fun oversizedNewBatchMustNotEraseEarlierMessagePreviews() {
        val previous = retainAssistantPreviews(emptyMap(), "previous", listOf(photo("kept", 6)), 10)
        val result = retainAssistantPreviews(previous, "oversized", listOf(photo("large", 11)), 10)
        assertEquals(setOf("previous"), result.keys)
        assertEquals("kept", result.getValue("previous").single().id)
    }
    @Test fun oversizedReplacementMustKeepTheExistingMessagePreview() {
        val previous = retainAssistantPreviews(emptyMap(), "same", listOf(photo("kept", 6)), 10)
        val result = retainAssistantPreviews(previous, "same", listOf(photo("large", 11)), 10)
        assertEquals("kept", result.getValue("same").single().id)
    }
}
