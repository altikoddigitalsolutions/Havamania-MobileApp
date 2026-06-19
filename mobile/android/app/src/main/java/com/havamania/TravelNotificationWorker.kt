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
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.havamania.NotificationItem
import com.havamania.NotificationCategory

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

        // Bildirimlerin açık olup olmadığını kontrol et
        val notificationsEnabled = ThemeManager.getNotificationsEnabled(application).first()
        if (!notificationsEnabled) return Result.success()

        val plans = weatherDao.getAllTravelPlans()
        val today = LocalDate.now()
        val dateStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val travelViewModel = TravelViewModel(application)

        for (entity in plans) {
            // Arşivlenmiş seyahatleri atla
            if (entity.isArchived) continue

            val plan = entity.toDomain()
            val daysUntil = ChronoUnit.DAYS.between(today, plan.startDate).toInt()
            val isOver = today.isAfter(plan.endDate)

            // Geçmiş seyahatleri atla
            if (isOver) continue

            // KURAL: Seyahate 15 gün veya daha az kaldıysa (seyahat günü dahil)
            if (daysUntil in 0..15) {
                // KURAL: Günde maksimum 1 bildirim gönder
                if (plan.lastDailyNotificationDate == dateStr) continue

                // Önce analizi güncelle (İnternet bağımlılığı TravelViewModel tarafından yönetilir)
                val updatedPlan = try {
                    travelViewModel.performAnalysis(plan)
                } catch (e: Exception) {
                    plan // Analiz başarısızsa eski veriyi kullan
                }

                // Analiz güncellendiyse kaydet
                if (updatedPlan != plan) {
                    weatherDao.insertTravelPlan(updatedPlan.toEntity())
                }

                val travelData = buildNotificationData(updatedPlan, daysUntil)
                val (title, message) = generateNotificationText(updatedPlan, daysUntil, travelData)

                val notificationId = "travel_${plan.id}_$dateStr"

                val notificationItem = NotificationItem(
                    id = notificationId,
                    title = title,
                    message = message,
                    category = NotificationCategory.TRAVEL,
                    createdAt = System.currentTimeMillis(),
                    eventAt = plan.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    deepLinkTarget = "havamania://app/calendar?focusId=${plan.id}",
                    relatedTripId = plan.id,
                    actionLabel = "Analizi Gör",
                    travelData = travelData
                )

                // Yerel bildirim listesine ekle
                notificationDao.insert(notificationItem)

                // Sistem bildirimini göster
                showSystemNotification(notificationItem)

                // KURAL: Bugün bildirim gönderildi olarak işaretle
                weatherDao.insertTravelPlan(updatedPlan.copy(lastDailyNotificationDate = dateStr).toEntity())
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
        data: TravelNotificationData
    ): Pair<String, String> {
        val latestAnalysis = plan.analyses.lastOrNull()
        val dateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale("tr"))
        val dateRange = "${plan.startDate.format(dateFormatter)} - ${plan.endDate.format(dateFormatter)}"

        // KURAL 13: Başlık formatı "Şehir • Tarih Aralığı"
        val title = "${plan.city} • $dateRange"

        val message = when {
            daysLeft == 0 -> {
                val weather = if (data.weatherSummary != null)
                    "Hava ${data.weatherSummary.lowercase()}, sıcaklık ${data.minTemp?.toInt()}-${data.maxTemp?.toInt()}°."
                    else "Seyahatin bugün başlıyor."
                "Seyahatin bugün başlıyor! $weather Çıkmadan önce son kontrollerini yapmayı unutma! ✈️"
            }
            daysLeft == 1 -> "Seyahatine yarın çıkıyorsun! Bugünkü güncel hava analizi ve valiz önerilerin hazır. 🎒"
            else -> {
                val base = if (latestAnalysis?.comparisonText != null && !latestAnalysis.comparisonText.contains("ilk analiz")) {
                    "Hava tahminlerinde bazı değişiklikler var. Güncel analizi inceleyebilirsin."
                } else {
                    "Seyahatine $daysLeft gün kaldı. Hava durumuna göre hazırlıklarını gözden geçir."
                }
                "$base 🎒"
            }
        }

        return title to message
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

        // Tarih formatını ekle
        val eventTimeStr = NotificationDateFormatter.formatEventTime(item.eventAt ?: item.createdAt)
        val displayMessage = if (eventTimeStr.isNotEmpty()) "$eventTimeStr • ${item.message}" else item.message

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
            .setContentText(displayMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayMessage))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(item.id.hashCode(), notification)
    }

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
        weatherAnalysisStatus = try { TravelWeatherAnalysisStatus.valueOf(weatherAnalysisStatus) } catch (e: Exception) { TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW },
        isArchived = isArchived,
        analyses = analyses,
        lastDailyNotificationDate = lastDailyNotificationDate
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
        analyses = analyses,
        lastDailyNotificationDate = lastDailyNotificationDate
    )

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Saat 09:00 için gecikmeyi hesapla
            val currentDate = LocalDateTime.now()
            var dueDate = LocalDateTime.now()
                .withHour(9)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

            if (currentDate.isAfter(dueDate)) {
                dueDate = dueDate.plusDays(1)
            }

            val initialDelay = ChronoUnit.MILLIS.between(currentDate, dueDate)

            val request = PeriodicWorkRequestBuilder<TravelNotificationWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "travel_notifications_daily",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
