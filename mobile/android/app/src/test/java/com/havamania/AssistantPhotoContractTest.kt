package com.havamania

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class AssistantPhotoContractTest {
    @Test fun greetingWithPhotoGoesToPlatform() = runBlocking {
        var calls = 0
        assertEquals("remote", assistantPromptReply("selam", hasAttachments = true) { calls++; "remote" })
        assertEquals(1, calls)
    }
    @Test fun retryAndHistoryKeepTheExactPhotoIdsWithoutBytes() {
        val id = UUID.randomUUID().toString()
        val original = AltikodChatMessage(text = "Fotoğrafı yorumla", isUser = true,
            attachmentIds = listOf(id), photoCount = 1)
        val restored = Json.decodeFromString<AltikodChatMessage>(Json.encodeToString(original))
        val retry = assistantMessageForSend(listOf(restored), original.text, true, original.id)!!
        assertEquals(original.id, retry.id)
        assertEquals(listOf(id), retry.attachmentIds)
        assertEquals(1, retry.photoCount)
        val request = BackendChatbotAskRequest(retry.text, retry.id, attachment_ids = retry.attachmentIds)
        val json = Json.encodeToString(request)
        assertEquals(listOf(id), Json.decodeFromString<BackendChatbotAskRequest>(json).attachment_ids)
        assertFalse(json.contains("bytes"))
        assertFalse(json.contains("content://"))
    }
    @Test fun partialUploadRetryNeverUploadsCompletedPhotosAgain() = runBlocking {
        val first = AssistantPhoto(bytes = byteArrayOf(1))
        val second = AssistantPhoto(bytes = byteArrayOf(2))
        var ids = emptyList<String>()
        var remaining = listOf(first, second)
        val calls = mutableListOf<String>()
        try {
            uploadAssistantPhotoBatch(2, ids, remaining, upload = {
                calls += it.id
                if (it === second) throw java.io.IOException()
                "first-receipt"
            }, onUploaded = { photo, completed -> ids = completed; remaining = remaining.filterNot { it.id == photo.id } })
            fail("Expected second upload failure")
        } catch (_: java.io.IOException) { }
        assertEquals(listOf("first-receipt"), ids)
        assertEquals(listOf(second), remaining)
        ids = uploadAssistantPhotoBatch(2, ids, remaining, upload = { calls += it.id; "second-receipt" }, onUploaded = { _, _ -> })
        assertEquals(listOf("first-receipt", "second-receipt"), ids)
        assertEquals(1, calls.count { it == first.id })
        assertEquals(ids, uploadAssistantPhotoBatch(2, ids, emptyList(), { error("Must not reupload") }, { _, _ -> }))
    }
    @Test fun processLossCannotSendPartialPhotoSelection() = runBlocking {
        try {
            uploadAssistantPhotoBatch(2, listOf("one"), emptyList(), { error("Must not upload") }, { _, _ -> })
            fail("Missing photo accepted")
        } catch (_: TerminalAssistantException) { }
    }
    @Test fun olderMessagesRemainCompatible() {
        val message = Json.decodeFromString<AltikodChatMessage>("""{"text":"eski"}""")
        assertEquals(0, message.photoCount)
        assertTrue(message.attachmentIds.isEmpty())
        assertNull(BackendChatbotAskRequest("selam").attachment_ids)
    }
}
