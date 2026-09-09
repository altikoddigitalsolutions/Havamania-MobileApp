package com.havamania

import org.junit.Assert.*
import org.junit.Test

class AiAssistantLogicTest {

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
