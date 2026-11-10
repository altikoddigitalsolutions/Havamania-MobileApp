package com.havamania

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest

// showSnackbar removes its visible/queued message when the caller is cancelled.
internal suspend fun <T> collectSessionMessages(
    session: StateFlow<Long>,
    messages: Flow<T>,
    show: suspend (T) -> Unit
) {
    session.collectLatest {
        messages.collect { show(it) }
    }
}
