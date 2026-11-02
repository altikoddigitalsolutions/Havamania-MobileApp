package com.havamania

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

internal data class CitySyncClock(val wall: Long, val elapsed: Long, val boot: String)

internal object CitySyncClocks {
    private val processId = java.util.UUID.randomUUID().toString()
    fun capture(context: Context): CitySyncClock {
        val boot = try {
            val count = Settings.Global.getInt(context.contentResolver, "boot_count")
            "boot:$count"
        } catch (_: Exception) { "process:$processId" }
        return CitySyncClock(System.currentTimeMillis() / 1000, SystemClock.elapsedRealtime() / 1000, boot)
    }
}

internal fun CitySyncChange.withRetryClock(clock: CitySyncClock, duration: Long): CitySyncChange = copy(
    retryDuration = duration.coerceIn(0, CITY_SYNC_UNCERTAIN_SECONDS),
    retryElapsedStart = clock.elapsed.coerceAtLeast(0), retryBoot = clock.boot
)

internal fun CitySyncChange.retryClockNeedsReset(clock: CitySyncClock): Boolean =
    retryNotBefore > 0 && (retryBoot.isEmpty() || retryBoot != clock.boot ||
        retryElapsedStart > clock.elapsed || retryDuration !in 1..CITY_SYNC_UNCERTAIN_SECONDS)

internal fun CitySyncChange.retrySecondsLeft(clock: CitySyncClock): Long = when {
    retryNotBefore <= 0 -> 0
    retryClockNeedsReset(clock) -> CITY_SYNC_UNCERTAIN_SECONDS
    else -> (retryDuration - (clock.elapsed - retryElapsedStart)).coerceAtLeast(0)
}
