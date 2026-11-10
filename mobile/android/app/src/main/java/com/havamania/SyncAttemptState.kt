package com.havamania

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class SyncAttemptState {
    private var generation = 0L
    private var active = 0
    private val _running = MutableStateFlow(false)
    val running = _running.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    @Synchronized fun ticket(): Long = generation
    @Synchronized fun begin(ticket: Long): Boolean {
        if (ticket != generation) return false
        if (active == 0) _error.value = null
        active++
        _running.value = true
        return true
    }
    @Synchronized fun fail(ticket: Long, message: String) {
        if (ticket == generation) _error.value = message
    }
    @Synchronized fun finish(ticket: Long) {
        if (ticket == generation) {
            active = (active - 1).coerceAtLeast(0)
            _running.value = active > 0
        }
    }
    suspend fun run(ticket: Long, block: suspend () -> Unit) {
        if (!begin(ticket)) return
        try {
            block()
        } finally {
            finish(ticket)
        }
    }
    @Synchronized fun reset() {
        generation++
        active = 0
        _running.value = false
        _error.value = null
    }
}
