package com.havamania

import java.util.concurrent.atomic.AtomicLong

/** Invalidate queued edits across deletion, including after its UI state is acknowledged. */
internal class PreferenceWriteEpoch {
    private val generation = AtomicLong()
    fun capture(): Long = generation.get()
    fun invalidate() { generation.incrementAndGet() }
    fun permits(token: Long, owner: String, currentOwner: String?, blocked: Boolean): Boolean =
        !blocked && token == generation.get() && owner == (currentOwner ?: "legacy")
}
internal val accountPreferenceWriteEpoch = PreferenceWriteEpoch()
