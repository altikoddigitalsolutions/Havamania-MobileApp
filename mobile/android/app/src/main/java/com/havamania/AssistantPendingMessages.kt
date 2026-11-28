package com.havamania

internal data class PendingAssistantMessage(val prompt: String, val context: String?,
    val retry: Boolean, val requestId: String?, val conversation: String, val owner: String,
    val fromDraft: Boolean = false, val tripId: String? = null)

/** Main-thread intent tracking: a late consent response cannot revive a superseded send. */
internal class AssistantPendingMessages {
    private var version = 0L
    private var message: PendingAssistantMessage? = null
    val hasPending: Boolean get() = message != null

    fun clear() { version++; message = null }
    fun onDraftChanged(previous: String, next: String) { if (previous != next) clear() }
    fun beginAcceptedSend(): Long { clear(); return version }
    fun queue(value: PendingAssistantMessage, expectedVersion: Long): Boolean {
        if (expectedVersion != version) return false
        message = value
        return true
    }
    fun take(owner: String, conversation: String): PendingAssistantMessage? {
        val value = message ?: return null
        clear()
        return value.takeIf { it.owner == owner && it.conversation == conversation }
    }
}
