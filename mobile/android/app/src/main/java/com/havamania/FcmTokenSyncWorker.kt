package com.havamania

import android.app.Application
import android.content.Context
import androidx.work.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class FcmTokenSyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        if (!BuildConfig.FCM_TOKEN_SYNC_ENABLED) return Result.success()
        try {
            val app = applicationContext as Application
            val journal = FcmTokenQueue.journalForSync(app)
            val desired = journal.snapshot()
            if (!desired.pending) return Result.success()
            if (desired.retryNotBeforeEpochSeconds > System.currentTimeMillis() / 1000) return Result.retry()
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser ?: return Result.success()
            val epoch = NotificationSessionEpoch.capture()
            val deletion = AccountDeletionManager.getInstance(app)
            fun current() = auth.currentUser?.uid == desired.ownerUid && user.uid == desired.ownerUid &&
                NotificationSessionEpoch.capture() == epoch && !deletion.state.value.pending && journal.snapshot() == desired
            if (!current()) return Result.success()
            val project = FirebaseApp.getInstance().options.projectId?.takeIf { it.isNotBlank() } ?: return Result.success()
            val owner = firebaseBackendUserId(project, user.uid)
            val service = BackendChatbotFactory.create()
            var outcome = FcmGatewayResult(FcmGatewayAction.RETRY)
            val sync = syncPendingFcmToken(journal, { auth.currentUser?.uid },
                { NotificationSessionEpoch.capture() }, { deletion.state.value.pending }) { sent ->
                outcome = sendFcmTokenWithAuth(service, sent, owner, { refresh ->
                    if (!current()) null else user.getIdToken(refresh).await()?.token?.let { "Bearer $it" }
                }, ::current)
                outcome.action == FcmGatewayAction.APPLIED
            }
            if (sync != FcmSyncResult.RETRY) return Result.success()
            // Finish this attempt without acknowledgement. Failing a prerequisite
            // would also cancel a newer account's already-appended work.
            if (outcome.action == FcmGatewayAction.STOP) return Result.success()
            if (outcome.retryAfterSeconds > 0 && !journal.deferRetry(desired,
                    System.currentTimeMillis() / 1000, outcome.retryAfterSeconds)) return Result.success()
            return Result.retry()
        } catch (error: CancellationException) { throw error }
        catch (_: Exception) { return Result.retry() }
    }

    companion object {
        private const val NAME = "fcm_token_sync"
        fun schedule(context: Context) {
            if (!BuildConfig.FCM_TOKEN_SYNC_ENABLED) return
            val work = OneTimeWorkRequestBuilder<FcmTokenSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
            // Preserve in-flight work; append the newest desired state so KEEP
            // cannot lose an update arriving just before the old worker exits.
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, work)
        }
        fun cancel(context: Context) {
            if (BuildConfig.FCM_TOKEN_SYNC_ENABLED) WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
