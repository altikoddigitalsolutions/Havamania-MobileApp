package com.havamania

import kotlinx.coroutines.CancellationException

internal suspend fun <T> runCitySyncBatch(
    items: List<T>, owner: String, currentOwner: () -> String,
    send: suspend (T) -> Unit, onFailure: (Exception) -> Unit
) {
    for (item in items) {
        try { publishAccountResult(owner, currentOwner) { send(item) } }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) {
            publishAccountResult(owner, currentOwner) { onFailure(error) }
        }
    }
}
