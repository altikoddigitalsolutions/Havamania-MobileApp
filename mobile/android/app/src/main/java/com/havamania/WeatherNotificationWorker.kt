package com.havamania

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

/**
 * Arka planda hava durumunu kontrol eden ve bildirim gönderen Worker
 */
class WeatherNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val application = applicationContext as? android.app.Application ?: return Result.failure()
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: "legacy"

        val repository = WeatherRepository.getInstance(application)
        val database = NotificationDatabase.getDatabase(application)
        val notifRepo = NotificationRepository(database.notificationDao())

        try {
            // 1. Fetch current weather for default city
            val defaultCity = com.havamania.ui.theme.ThemeManager.getDefaultCity(application, uid).firstOrNull() ?: return Result.success()

            val weatherData = repository.getWeatherData(
                defaultCity.latitude, defaultCity.longitude, defaultCity.name, defaultCity.admin1
            ).firstOrNull() ?: return Result.retry()

            // 2. Check Smart Alerts
            val config = com.havamania.ui.theme.ThemeManager.getSmartAlertConfig(application, uid).first()
            val alerts = SmartAlertEngine.generateAlerts(weatherData, config, uid)

            // 3. Filter and Show Notifications (Business Rule 5: Deduplication)
            alerts.forEach { alert ->
                val key = alert.deduplicationKey
                val alreadyNotified = if (key.isNotEmpty()) {
                    database.notificationDao().existsWithKey(uid, key)
                } else false

                if (!alreadyNotified) {
                    val notificationItem = NotificationItem(
                        userId = uid,
                        title = alert.title,
                        message = alert.description,
                        category = when(alert.id) {
                            "rain" -> NotificationCategory.RAIN
                            "uv" -> NotificationCategory.UV
                            "storm" -> NotificationCategory.WARNING
                            else -> NotificationCategory.GENERAL
                        },
                        severity = if (alert.severity == AlertSeverity.CRITICAL) "HIGH" else "NORMAL",
                        deduplicationKey = if (key.isNotEmpty()) key else null
                    )
                    notifRepo.insert(notificationItem)
                    showNotification(alert.title, alert.description)
                }
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "weather_updates"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Hava Durumu Güncellemeleri",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Günlük hava durumu özetlerini ve önemli uyarıları içerir."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_day) // Özelleştirilebilir
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    companion object {
        private const val WORK_NAME = "WeatherDailyUpdateWork"

        /**
         * Günlük bildirimi planla
         */
        fun scheduleDailyUpdate(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<WeatherNotificationWorker>(
                24, TimeUnit.HOURS // Günde bir kez
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        /**
         * Bildirimi iptal et
         */
        fun cancelUpdate(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
