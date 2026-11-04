package com.havamania

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.havamania.ui.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/** Short, local processing only; never register tokens or call backend from this callback. */
class HavamaniaMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        try { FcmTokenQueue.recordToken(application, token) }
        catch (_: Exception) { android.util.Log.w("FCM", "Token queue could not be updated") }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.notification != null) return // This receiver accepts data-only protocol 1.
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        val epoch = NotificationSessionEpoch.capture()
        val deletion = AccountDeletionManager.getInstance(application)
        fun ownsSession() = auth.currentUser?.uid == uid &&
            NotificationSessionEpoch.capture() == epoch && deletion.state.value.let { !it.pending && !it.busy }
        val envelope = validatedFcmEnvelope(message.data, FirebaseApp.getInstance().options.projectId,
            uid, deletion.state.value.pending, System.currentTimeMillis() / 1000) ?: return
        try {
            // Firebase callback has a short lifetime. No detached coroutine can publish later.
            runBlocking(Dispatchers.IO) {
                withTimeout(3000) {
                    if (!ownsSession() || !ThemeManager.getNotificationsEnabled(applicationContext, uid).first()) return@withTimeout
                    val dao = NotificationDatabase.getDatabase(applicationContext).notificationDao()
                    withNotificationDeliveryLock {
                        deliverFcmRecord(uid, envelope, ::ownsSession,
                            { item -> dao.reserveForDelivery(item).let { if (it > 0) it else -1L } }, dao::delete,
                            complete = { dao.completeDelivery(it.userId, it.id) },
                            recovery = NotificationDeliveryRecovery.shared) { item ->
                            if (System.currentTimeMillis() / 1000 >= envelope.expiresAt ||
                                !PermissionHelper.hasNotificationPermission(applicationContext)) return@deliverFcmRecord false
                            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            val channel = "critical_weather"
                            if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
                                NotificationChannel(channel, "Kritik Hava Uyarıları", NotificationManager.IMPORTANCE_HIGH))
                            val pending = notificationTapPendingIntent(applicationContext, item)
                            val notification = NotificationCompat.Builder(applicationContext, channel)
                                .setSmallIcon(R.mipmap.ic_launcher).setContentTitle(item.title).setContentText(item.message)
                                .setStyle(NotificationCompat.BigTextStyle().bigText(item.message))
                                .setContentIntent(pending).setOnlyAlertOnce(true).setAutoCancel(true).build()
                            currentCoroutineContext().ensureActive()
                            postNotificationIfCurrent(uid, epoch, { auth.currentUser?.uid },
                                { !ownsSession() || System.currentTimeMillis() / 1000 >= envelope.expiresAt ||
                                    !PermissionHelper.canPostToChannel(applicationContext, channel) },
                                enabled = { ThemeManager.getNotificationsEnabled(applicationContext, uid).first() },
                                stillReserved = { dao.ownsPendingDelivery(uid, item.id) }) {
                                manager.notify(notificationSystemTag(item), 0, notification)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fail closed. Never log device tokens, payload content or account identifiers.
        }
    }
}
