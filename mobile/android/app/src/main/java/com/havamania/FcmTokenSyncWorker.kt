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
            if (!journal.snapshot().pending) return Result.success()
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser ?: return Result.success()
            val epoch = NotificationSessionEpoch.capture()
            val deletion = AccountDeletionManager.getInstance(app)
            val project = FirebaseApp.getInstance().options.projectId?.takeIf { it.isNotBlank() } ?: return Result.success()
            val owner = firebaseBackendUserId(project, user.uid)
            val service by lazy { BackendChatbotFactory.create() }
            val decision = runFcmSyncAttempt(journal,
                { auth.currentUser?.uid?.takeIf { it == user.uid && NotificationSessionEpoch.capture() == epoch } },
                { NotificationSessionEpoch.capture() }, { deletion.state.value.pending },
                { System.currentTimeMillis() / 1000 }) { sent, current ->
                sendFcmTokenWithAuth(service, sent, owner, { refresh ->
                    if (!current()) null else fcmAuthorization { user.getIdToken(refresh).await()?.token }
                }, current)
            }
            // Success ends this job without cancelling newer appended jobs.
            // Journal acknowledgement is handled separately by the sync core.
            return if (decision == FcmWorkDecision.RETRY) Result.retry() else Result.success()
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
