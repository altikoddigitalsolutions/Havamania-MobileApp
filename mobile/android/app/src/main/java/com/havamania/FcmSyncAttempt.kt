package com.havamania

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout

internal enum class FcmWorkDecision { FINISH, RETRY }
// Largest Retry-After accepted by this protocol. A lost response or failed
// follow-up disk write must not make the old snapshot immediately sendable.
internal const val FCM_ATTEMPT_TIMEOUT_MILLIS = 180000L
internal const val FCM_UNCERTAIN_RETRY_SECONDS = 86400L + FCM_ATTEMPT_TIMEOUT_MILLIS / 1000

/** FINISH ends the work attempt; only a matching receipt can acknowledge the journal. */
internal suspend fun runFcmSyncAttempt(
    journal: FcmTokenJournal, currentUid: () -> String?, sessionEpoch: () -> Long,
    deletionPending: () -> Boolean, nowSeconds: () -> Long,
    send: suspend (FcmTokenRegistration, () -> Boolean) -> FcmGatewayResult
): FcmWorkDecision {
    currentCoroutineContext().ensureActive()
    val initial = journal.snapshot()
    val epoch = sessionEpoch()
    if (!initial.pending) return FcmWorkDecision.FINISH
    if (currentUid() != initial.ownerUid || sessionEpoch() != epoch || deletionPending()) return FcmWorkDecision.FINISH
    if (initial.authRequired) return FcmWorkDecision.FINISH
    val startedAt = nowSeconds()
    if (initial.retryNotBeforeEpochSeconds > startedAt) return FcmWorkDecision.RETRY
    // Persist BEFORE HTTP. If a later write is uncertain, the prior durable
    // fence still protects both this process and a newly opened journal.
    val desired = journal.reserveAttempt(initial, startedAt) ?: return FcmWorkDecision.FINISH
    currentCoroutineContext().ensureActive()
    fun current() = currentUid() == desired.ownerUid && sessionEpoch() == epoch &&
        !deletionPending() && journal.snapshot() == desired
    // Logout/deletion supersedes even a future retry deadline.
    if (!current()) return FcmWorkDecision.FINISH
    var outcome = FcmGatewayResult(FcmGatewayAction.RETRY)
    val sync = try {
        withTimeout(FCM_ATTEMPT_TIMEOUT_MILLIS) {
            syncPendingFcmToken(journal, currentUid, sessionEpoch, deletionPending) { sent ->
                if (sent != desired || !current()) {
                    outcome = FcmGatewayResult(FcmGatewayAction.SUPERSEDED)
                    false
                } else {
                    outcome = send(sent, ::current)
                    outcome.action == FcmGatewayAction.APPLIED
                }
            }
        }
    } catch (_: TimeoutCancellationException) {
        // Local timeout is a retry with an uncertain response. Parent/worker
        // cancellation must still propagate instead of becoming success/retry.
        currentCoroutineContext().ensureActive()
        return if (current()) FcmWorkDecision.RETRY else FcmWorkDecision.FINISH
    }
    currentCoroutineContext().ensureActive()
    if (sync != FcmSyncResult.RETRY || !current()) return FcmWorkDecision.FINISH
    if (outcome.action == FcmGatewayAction.AUTH_REQUIRED) {
        journal.requireAuthentication(desired)
        return FcmWorkDecision.FINISH
    }
    if (outcome.action in setOf(FcmGatewayAction.STOP, FcmGatewayAction.SUPERSEDED))
        return FcmWorkDecision.FINISH
    if (outcome.retryAfterSeconds > 0 && !journal.deferRetry(desired, nowSeconds(), outcome.retryAfterSeconds))
        return FcmWorkDecision.FINISH
    return FcmWorkDecision.RETRY
}
