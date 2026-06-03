package com.havamania

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.havamania.R
import com.havamania.ui.theme.ThemeManager
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class TravelNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val application = applicationContext as android.app.Application
        val weatherDb = WeatherDatabase.getDatabase(application)
        val notificationDb = NotificationDatabase.getDatabase(application)
        val weatherDao = weatherDb.weatherDao()
        val notificationDao = notificationDb.notificationDao()

        // Check if notifications are enabled
        val notificationsEnabled = ThemeManager.getNotificationsEnabled(application).first()
        if (!notificationsEnabled) return Result.success()

        val plans = weatherDao.getAllTravelPlans()
        val now = LocalDateTime.now()
        val today = LocalDate.now()

        val travelViewModel = TravelViewModel(application)

        for (entity in plans) {
            if (entity.isArchived) continue

            val plan = entity.toDomain()
            val daysUntil = ChronoUnit.DAYS.between(today, plan.startDate).toInt()
            val isOver = today.isAfter(plan.endDate)

            if (isOver) continue

            // 1. Rule: Within 15 days
            if (daysUntil in 0..15) {
                // Try to update analysis if possible (Internet dependency handled by TravelViewModel)
                val updatedPlan = try {
                    travelViewModel.performAnalysis(plan)
                } catch (e: Exception) {
                    plan // Use last saved data if analysis fails
                }

                // Save updated plan if changed
                if (updatedPlan != plan) {
                    weatherDao.insertTravelPlan(updatedPlan.toEntity())
                }

                // Determine notification type - Strictly once a day for days 0-15
                val hour = now.hour
                val isMorning = hour in 8..11

                val type = when {
                    daysUntil in 0..15 && isMorning -> "DAILY_REPORT"
                    else -> null
                }

                if (type != null) {
                    val dateStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val notificationId = "travel_notif_${plan.id}_$dateStr"

                    // The notify(id, ...) with same ID will update/suppress if already shown,
                    // but we check if we already inserted it in our notification log to be sure.
                    val alreadySent = notificationDao.getNotificationById(notificationId) != null
                    if (alreadySent) continue

                    val travelData = buildNotificationData(updatedPlan, daysUntil)
                    val (title, message) = generateNotificationText(updatedPlan, daysUntil, type, travelData)

                    val notificationItem = NotificationItem(
                        id = notificationId,
                        title = title,
                        message = message,
                        category = NotificationCategory.TRAVEL,
                        createdAt = System.currentTimeMillis(),
                        deepLinkTarget = "havamania://app/calendar?focusId=${plan.id}",
                        relatedTripId = plan.id,
                        actionLabel = "Detayları Gör",
                        travelData = travelData
                    )

                    // Save to local notification list
                    notificationDao.insert(notificationItem)

                    // Show system notification
                    showSystemNotification(notificationItem)
                }
            }
        }

        return Result.success()
    }

    private fun buildNotificationData(plan: TravelPlan, daysLeft: Int): TravelNotificationData {
        val snapshot = plan.lastForecastSnapshot
        val prevSnapshot = plan.previousForecastSnapshot

        val comparisonText = if (snapshot != null && prevSnapshot != null) {
            TravelAiHelper.generateComparisonText(prevSnapshot, snapshot)
        } else null

        val recommendedItems = mutableListOf<String>()
        if ((snapshot?.precipitationProbability ?: 0) > 40) {
            recommendedItems.add("Şemsiye")
            recommendedItems.add("Yağmurluk")
            recommendedItems.add("Yedek Ayakkabı")
        }
        if ((snapshot?.maxTemp ?: 20.0) < 15.0) {
            recommendedItems.add("Kalın Mont")
        } else if ((snapshot?.maxTemp ?: 20.0) > 28.0) {
            recommendedItems.add("Güneş Kremi")
            recommendedItems.add("Şapka")
        }

        return TravelNotificationData(
            travelId = plan.id,
            destination = plan.city,
            travelStartDate = plan.startDate.toString(),
            travelEndDate = plan.endDate.toString(),
            daysLeft = daysLeft,
            weatherSummary = snapshot?.conditionSummary,
            rainProbability = snapshot?.precipitationProbability,
            minTemp = snapshot?.minTemp,
            maxTemp = snapshot?.maxTemp,
            windRisk = if ((snapshot?.windSpeed ?: 0.0) > 30.0) "Yüksek" else "Düşük",
            previousAnalysisSummary = plan.lastWeatherAnalysisText,
            comparisonText = comparisonText,
            recommendedItems = recommendedItems
        )
    }

    private fun generateNotificationText(
        plan: TravelPlan,
        daysLeft: Int,
        type: String,
        data: TravelNotificationData
    ): Pair<String, String> {
        val title = when {
            daysLeft == 0 -> "${plan.city} seyahatin bugün başlıyor! ✈️"
            daysLeft == 1 -> "${plan.city} seyahatine 1 gün kaldı! 🎒"
            else -> "${plan.city} seyahatine $daysLeft gün kaldı 🎒"
        }

        val weatherPart = if (data.weatherSummary != null) {
            "Bugünkü tahmine göre hava ${data.weatherSummary.lowercase()}. "
        } else ""

        val comparisonPart = if (!data.comparisonText.isNullOrBlank()) {
            "${data.comparisonText} "
        } else ""

        val advicePart = if (data.recommendedItems.isNotEmpty()) {
            "${data.recommendedItems.first()} önerilir."
        } else "Keyifli yolculuklar!"

        val message = "$title $weatherPart$comparisonPart$advicePart"

        return "Havamania Seyahat Analizi" to message
    }

    private fun showSystemNotification(item: NotificationItem) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "travel_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Seyahat Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Deep link intent
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(item.deepLinkTarget),
            applicationContext,
            WeatherPremiumActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Use valid icon
            .setContentTitle(item.title)
            .setContentText(item.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(item.id.hashCode(), notification)
    }

    // Helper extensions duplicated here for self-containment in worker if needed,
    // but better to use them from where they are defined if possible.
    private fun TravelPlanEntity.toDomain() = TravelPlan(
        id = id,
        city = city,
        latitude = latitude,
        longitude = longitude,
        tripType = try { TripType.valueOf(tripType) } catch (e: Exception) { TripType.OTHER },
        startDate = Instant.ofEpochMilli(startDate).atZone(ZoneId.systemDefault()).toLocalDate(),
        endDate = Instant.ofEpochMilli(endDate).atZone(ZoneId.systemDefault()).toLocalDate(),
        createdAt = createdAt,
        weatherSummary = weatherSummary,
        aiSuggestion = aiSuggestion,
        lastWeatherAnalysisText = lastWeatherAnalysisText,
        lastWeatherAnalysisDate = lastWeatherAnalysisDate,
        lastForecastSnapshot = lastForecastSnapshot,
        previousForecastSnapshot = previousForecastSnapshot,
        nextAnalysisEligibleDate = nextAnalysisEligibleDate,
        weatherAnalysisStatus = try { TravelWeatherAnalysisStatus.valueOf(weatherAnalysisStatus) } catch (e: Exception) { TravelWeatherAnalysisStatus.TOO_EARLY },
        isArchived = isArchived,
        analysis = analysis
    )

    private fun TravelPlan.toEntity() = TravelPlanEntity(
        id = id,
        city = city,
        latitude = latitude,
        longitude = longitude,
        tripType = tripType.name,
        startDate = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        endDate = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        createdAt = createdAt,
        weatherSummary = weatherSummary,
        aiSuggestion = aiSuggestion,
        lastWeatherAnalysisText = lastWeatherAnalysisText,
        lastWeatherAnalysisDate = lastWeatherAnalysisDate,
        lastForecastSnapshot = lastForecastSnapshot,
        previousForecastSnapshot = previousForecastSnapshot,
        nextAnalysisEligibleDate = nextAnalysisEligibleDate,
        weatherAnalysisStatus = weatherAnalysisStatus.name,
        isArchived = isArchived,
        analysis = analysis
    )

    companion object {
        fun schedule(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Run even if offline using last data
                    .build()

                // Run every 4 hours to catch morning/evening windows
                val request = PeriodicWorkRequestBuilder<TravelNotificationWorker>(4, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "travel_notifications_work",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            } catch (e: Exception) {
                android.util.Log.e("TravelWorker", "Bildirim planlama hatası: WorkManager başlatılamadı.", e)
            }
        }
    }
}
