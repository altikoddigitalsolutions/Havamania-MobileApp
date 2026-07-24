package com.havamania

import org.junit.Assert.*
import org.junit.Test

class AiAssistantLogicTest {

    @Test
    fun `botId should be the verified integer ID`() {
        // Use reflection or check the code to verify that botId used is "6"
        val repository = AiAssistantRepository()
        val field = repository.javaClass.getDeclaredField("botId")
        field.isAccessible = true
        val value = field.get(repository) as String

        assertEquals("6", value)
    }

    @Test
    fun `AssistantResult Success should contain content`() {
        val result = AssistantResult.Success("Test content")
        assertEquals("Test content", result.content)
    }

    @Test
    fun `AssistantResult HttpError should contain code`() {
        val result = AssistantResult.HttpError(404)
        assertEquals(404, result.code)
    }
}
