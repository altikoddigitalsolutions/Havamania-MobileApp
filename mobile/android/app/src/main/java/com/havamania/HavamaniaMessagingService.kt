package com.havamania

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
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
            NotificationSessionEpoch.capture() == epoch && !deletion.state.value.pending
        val envelope = validatedFcmEnvelope(message.data, FirebaseApp.getInstance().options.projectId,
            uid, deletion.state.value.pending, System.currentTimeMillis() / 1000) ?: return
        try {
            // Firebase callback has a short lifetime. No detached coroutine can publish later.
            runBlocking(Dispatchers.IO) {
                withTimeout(3000) {
                    if (!ownsSession() || !ThemeManager.getNotificationsEnabled(applicationContext, uid).first()) return@withTimeout
                    val dao = NotificationDatabase.getDatabase(applicationContext).notificationDao()
                    val item = persistFcmRecord(uid, envelope, ::ownsSession, dao::insertIncoming, dao::delete)
                        ?: return@withTimeout
                    if (System.currentTimeMillis() / 1000 >= envelope.expiresAt ||
                        !PermissionHelper.hasNotificationPermission(applicationContext)) return@withTimeout
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val channel = "critical_weather"
                    if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
                        NotificationChannel(channel, "Kritik Hava Uyarıları", NotificationManager.IMPORTANCE_HIGH))
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("havamania://app/weather"),
                        applicationContext, WeatherPremiumActivity::class.java).apply {
                        action = NOTIFICATION_ACTION_PREFIX + item.id
                        putExtra(NOTIFICATION_OWNER, uid)
                        putExtra(NOTIFICATION_ID, item.id)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val pending = PendingIntent.getActivity(applicationContext, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    val notification = NotificationCompat.Builder(applicationContext, channel)
                        .setSmallIcon(R.mipmap.ic_launcher).setContentTitle(item.title).setContentText(item.message)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(item.message))
                        .setContentIntent(pending).setAutoCancel(true).build()
                    currentCoroutineContext().ensureActive()
                    if (ownsSession()) manager.notify(item.id, 0, notification)
                }
            }
        } catch (_: Exception) {
            // Fail closed. Never log device tokens, payload content or account identifiers.
        }
    }
}
