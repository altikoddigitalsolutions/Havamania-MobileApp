package com.havamania

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

/** Collect on the UI dispatcher; expensive mapping belongs upstream in flowOn(IO). */
internal suspend fun <T> observeAccountData(
    source: Flow<T>, owner: String, currentOwner: () -> String,
    onData: (T) -> Unit, onError: (Exception) -> Unit
) {
    try {
        source.collect { value -> publishAccountResult(owner, currentOwner) { onData(value) } }
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        publishAccountResult(owner, currentOwner) { onError(error) }
    }
}
