package com.havamania

import kotlinx.coroutines.channels.Channel

/** One consumer applies snapshots in arrival order; only the newest waiting value is retained. */
internal class CitySnapshotQueue<T> {
    private val channel = Channel<T>(Channel.CONFLATED)
    fun offer(snapshot: T) { channel.trySend(snapshot) }
    fun close() { channel.cancel() }
    suspend fun consume(apply: suspend (T) -> Unit) {
        for (snapshot in channel) apply(snapshot)
    }
}
