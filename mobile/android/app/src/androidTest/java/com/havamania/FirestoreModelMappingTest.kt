package com.havamania

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.util.CustomClassMapper
import org.junit.Assert.*
import org.junit.Test

/** Uses only the SDK object mapper. No get/set/listener or network operation. */
class FirestoreModelMappingTest {
    @Test fun nestedHistoryRoundTripsWithExistingBooleanFieldNames() {
        val original = AiHistoryEntity(
            id = "conversation", userId = "owner", title = "Seyahat", summary = "Özet",
            messages = listOf(AltikodChatMessage(text = "Merhaba", isUser = true,
                isFallback = true, action = AssistantAction(AssistantActionType.CREATE_TRAVEL_PLAN, "Planla"))),
            cityName = "İstanbul"
        )
        val serialized = CustomClassMapper.convertToPlainJavaTypes(original)
        val reference = FirebaseFirestore.getInstance().document("users/local-test/ai_history/conversation")
        val restored = CustomClassMapper.convertToCustomClass(serialized, AiHistoryEntity::class.java, reference)
        assertEquals(original, restored)
        val messages = (serialized as Map<*, *>)["messages"] as List<*>
        val message = messages.single() as Map<*, *>
        assertEquals(true, message["user"])
        assertEquals(true, message["fallback"])
    }

    @Test fun historicalSparseDocumentAndNestedActionCanBeRead() {
        val data = mapOf("id" to "old", "title" to "Eski", "messages" to listOf(
            mapOf("text" to "Merhaba", "user" to true, "action" to mapOf("type" to "NONE", "label" to ""))
        ))
        val reference = FirebaseFirestore.getInstance().document("users/local-test/ai_history/old")
        val restored = CustomClassMapper.convertToCustomClass(data, AiHistoryEntity::class.java, reference)
        assertEquals("old", restored.id)
        assertTrue(restored.messages.single().isUser)
        assertFalse(restored.messages.single().isFallback)
        assertEquals(AssistantActionType.NONE, restored.messages.single().action?.type)
    }
}
