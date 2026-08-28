package com.havamania

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
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

        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val currentUid = auth.currentUser?.uid ?: "legacy"

        // Bildirimlerin açık olup olmadığını kontrol et
        val notificationsEnabled = ThemeManager.getNotificationsEnabled(application, currentUid).first()
        if (!notificationsEnabled) return Result.success()

        val plans = weatherDao.getAllTravelPlans(currentUid)
        val today = LocalDate.now()
        val dateStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        plans.map { it.toDomain() }.forEach { plan ->
            if (plan.isArchived) return@forEach

            val daysUntil = ChronoUnit.DAYS.between(today, plan.startDate).toInt()
            val isOngoing = !today.isBefore(plan.startDate) && !today.isAfter(plan.endDate)
            val isUpcoming = daysUntil in 0..10

            // Sadece yaklaşan veya devam eden seyahatler için günlük özet gönder
            if (isUpcoming || isOngoing) {
                // Bugün bu seyahat için bildirim gönderildi mi?
                if (plan.lastDailyNotificationDate == dateStr) return@forEach

                // Önce analizi güncelle
                val updatedPlan = try {
                    TravelAnalysisEngine.performAnalysis(
                        context = application,
                        plan = plan,
                        currentUid = currentUid,
                        apiService = NetworkModule.apiService,
                        repository = WeatherRepository.getInstance(application)
                    )
                } catch (e: Exception) {
                    plan
                }

                // Analiz güncellendiyse kaydet
                if (updatedPlan != plan) {
                    weatherDao.insertTravelPlan(updatedPlan.toEntity())
                }

                val travelData = buildNotificationData(updatedPlan, daysUntil)
                val (title, message) = generateNotificationText(updatedPlan, daysUntil, isOngoing, travelData)

                val notificationId = "travel_${currentUid}_${plan.id}_$dateStr"

                val notificationItem = NotificationItem(
                    id = notificationId,
                    userId = currentUid,
                    title = title,
                    message = message,
                    category = NotificationCategory.TRAVEL,
                    createdAt = System.currentTimeMillis(),
                    eventAt = plan.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    deepLinkTarget = "havamania://app/calendar?focusId=${plan.id}",
                    relatedTripId = plan.id,
                    actionLabel = "Analizi Gör",
                    travelData = travelData,
                    deduplicationKey = "travel_daily_${currentUid}_${plan.id}_$dateStr"
                )

                notificationDao.insert(notificationItem)
                showSystemNotification(notificationItem)

                // Güzergâh bildirimi kontrolü
                val routeNotificationSent = checkAndSendRouteWeatherNotifications(updatedPlan, application)

                // Bildirim gönderildi işaretle
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
            windRisk = WeatherUtils.formatWindWithLevel(snapshot?.windSpeed),
            previousAnalysisSummary = plan.weatherSummary,
            comparisonText = comparisonText,
            recommendedItems = recommendedItems
        )
    }

    private fun generateNotificationText(
        plan: TravelPlan,
        daysLeft: Int,
        isOngoing: Boolean,
        data: TravelNotificationData
    ): Pair<String, String> {
        val latestAnalysis = plan.analyses.lastOrNull()
        val dateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale("tr"))
        val dateRange = "${plan.startDate.format(dateFormatter)} - ${plan.endDate.format(dateFormatter)}"

        val title = "Seyahat Güncellemesi"
        val cityLabel = "${plan.city} • $dateRange"

        val message = when {
            isOngoing -> {
                val today = LocalDate.now()
                val dayCount = ChronoUnit.DAYS.between(plan.startDate, today).toInt() + 1
                val weather = if (data.weatherSummary != null)
                    "Bugün hava ${data.weatherSummary.lowercase()}, ${data.maxTemp?.toInt()}°."
                    else "Hava tahminlerini kontrol etmeyi unutma."
                "$cityLabel: Seyahatin devam ediyor (Gün: $dayCount). $weather"
            }
            daysLeft == 0 -> {
                val weather = if (data.weatherSummary != null)
                    "Hava ${data.weatherSummary.lowercase()}, sıcaklık ${data.minTemp?.toInt()}-${data.maxTemp?.toInt()}°."
                    else "Seyahatin bugün başlıyor."
                "$cityLabel: Seyahatin bugün başlıyor! $weather Çıkmadan önce son kontrollerini yapmayı unutma! ✈️"
            }
            daysLeft == 1 -> "$cityLabel: Seyahatine yarın çıkıyorsun! Bugünkü güncel hava analizi ve valiz önerilerin hazır. 🎒"
            else -> {
                val base = if (latestAnalysis?.comparisonText != null && !latestAnalysis.comparisonText.contains("ilk analiz")) {
                    "Hava tahminlerinde bazı değişiklikler var. Güncel analizi inceleyebilirsin."
                } else {
                    "Seyahatin yaklaşıyor ($daysLeft gün kaldı), hava analizin hazır. Hazırlıklarını gözden geçir."
                }
                "$cityLabel: $base 🎒"
            }
        }

        return title to message
    }

    private suspend fun checkAndSendRouteWeatherNotifications(plan: TravelPlan, context: Context): Boolean {
        val departureDateTime = plan.departureDateTime ?: return false
        val now = LocalDateTime.now()
        val hoursUntil = java.time.Duration.between(now, departureDateTime).toHours()

        if (hoursUntil > 48 || hoursUntil < 0) return false

        val notificationDao = NotificationDatabase.getDatabase(context).notificationDao()
        val weatherDao = WeatherDatabase.getDatabase(context).weatherDao()

        val readyKey = "route_ready_${plan.userId}_${plan.id}"
        val readySent = notificationDao.existsWithKey(plan.userId, readyKey)
        if (!readySent) {
            val item = NotificationItem(
                userId = plan.userId,
                id = java.util.UUID.randomUUID().toString(),
                title = "Güzergâh hava tahminin hazır 🌤️",
                message = "${plan.city} seyahatine 2 günden az kaldı. Yol boyunca beklenen hava koşullarını şimdi inceleyebilirsin.",
                category = NotificationCategory.TRAVEL,
                createdAt = System.currentTimeMillis(),
                deepLinkTarget = "havamania://app/route/${plan.id}",
                deduplicationKey = readyKey,
                actionLabel = "Güzergâhı Gör"
            )
            notificationDao.insert(item)
            showSystemNotification(item)
            return true
        }

        val todayStr = now.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dailyKey = "route_daily_${plan.userId}_${plan.id}_$todayStr"
        val dailySent = notificationDao.existsWithKey(plan.userId, dailyKey)
        if (dailySent) return false

        val summary = performRouteWeatherAnalysis(plan, context) ?: return false

        val oldSummary = plan.routeWeatherSummary
        val hasChange = oldSummary != null && oldSummary != summary

        val title = if (hasChange) "Güzergâhında hava değişikliği var" else "Güzergâhında hava güncellendi"
        val message = if (hasChange) {
            "Rotalarındaki hava tahminlerinde bazı değişimler saptadık. Güncel durumu incelemeni öneririz."
        } else {
            summary
        }

        val notificationItem = NotificationItem(
            userId = plan.userId,
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            message = message,
            category = NotificationCategory.TRAVEL,
            createdAt = System.currentTimeMillis(),
            deepLinkTarget = "havamania://app/route/${plan.id}",
            deduplicationKey = dailyKey,
            actionLabel = "Tahmini Aç"
        )

        notificationDao.insert(notificationItem)
        showSystemNotification(notificationItem)

        weatherDao.insertTravelPlan(plan.copy(
            routeWeatherSummary = summary,
            lastRouteAnalysisAt = System.currentTimeMillis()
        ).toEntity())

        return true
    }

    private suspend fun performRouteWeatherAnalysis(plan: TravelPlan, context: Context): String? {
        try {
            val origin = plan.originPoint?.let { GeoPoint(it.first, it.second) } ?: return null
            val dest = GeoPoint(plan.latitude, plan.longitude)

            val routeProvider = OsrmRouteProvider()
            val routeResult = routeProvider.getRoute(origin, dest)
            if (routeResult !is RouteResult.Success) return null

            val route = routeResult.route
            val departureDateTime = plan.departureDateTime ?: return null
            val departureMillis = departureDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val waypoints = EtaCalculator.assignEtas(
                route,
                RouteSampler.sample(route),
                departureMillis
            )

            val weatherProvider = RouteWeatherProvider()
            val arrivalMillis = departureMillis + (route.durationSeconds * 1000).toLong()

            val points = mutableListOf<GeoPoint>()
            val etas = mutableListOf<Long?>()

            points.add(origin)
            etas.add(departureMillis)

            waypoints.forEach {
                points.add(it.location)
                etas.add(it.etaEpochMillis)
            }

            points.add(dest)
            etas.add(arrivalMillis)

            val results = weatherProvider.weatherAtBatch(points, etas)
            val all = results.filterNotNull()
            if (all.isEmpty()) return null

            val dangerCount = all.count { it.risk == RouteRisk.DANGER }
            val cautionCount = all.count { it.risk == RouteRisk.CAUTION }

            return when {
                dangerCount > 0 -> "Dikkat! Güzergâhında $dangerCount noktada riskli hava koşulları (kar/fırtına) bekleniyor."
                cautionCount > 0 -> "${plan.city} yolculuğunda bazı bölgelerde yağış veya sis görülebilir. Tedbirli olun."
                else -> "Güzergâh hava tahmininde önemli bir değişiklik yok. Yol boyunca koşullar uygun görünüyor."
            }
        } catch (e: Exception) {
            Log.e("RouteWorker", "Route analysis failed", e)
            return null
        }
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
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(item.title)
            .setContentText(item.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(item.id.hashCode(), notification)
    }

    private fun TravelPlanEntity.toDomain() = TravelPlan(
        id = id,
        userId = userId,
        city = city,
        district = district,
        latitude = latitude,
        longitude = longitude,
        originCity = originCity,
        originDistrict = originDistrict,
        originLatitude = originLatitude,
        originLongitude = originLongitude,
        tripType = try { TripType.valueOf(tripType) } catch (e: Exception) { TripType.OTHER },
        startDate = Instant.ofEpochMilli(startDate).atZone(ZoneId.systemDefault()).toLocalDate(),
        endDate = Instant.ofEpochMilli(endDate).atZone(ZoneId.systemDefault()).toLocalDate(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        archivedAt = archivedAt,
        lastAnalysisAt = lastAnalysisAt,
        weatherSummary = weatherSummary,
        packingAdvice = packingAdvice,
        mustSee = mustSee,
        foodAdvice = foodAdvice,
        localAdvice = localAdvice,
        aiSuggestion = aiSuggestion,
        comfortScore = comfortScore,
        userNote = userNote,
        userRating = userRating,
        isAnalyzing = false,
        weatherAnalysisStatus = try { TravelWeatherAnalysisStatus.valueOf(weatherAnalysisStatus) } catch (e: Exception) { TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW },
        isArchived = isArchived,
        analyses = analyses,
        lastDailyNotificationDate = lastDailyNotificationDate,
        isDemo = isDemo,
        lastForecastSnapshot = lastForecastSnapshot,
        previousForecastSnapshot = previousForecastSnapshot,
        departureTime = departureTime
    )

    private fun TravelPlan.toEntity() = TravelPlanEntity(
        id = id,
        userId = userId,
        city = city,
        district = district,
        latitude = latitude,
        longitude = longitude,
        originCity = originCity,
        originDistrict = originDistrict,
        originLatitude = originLatitude,
        originLongitude = originLongitude,
        tripType = tripType.name,
        startDate = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        endDate = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        archivedAt = archivedAt,
        lastAnalysisAt = lastAnalysisAt,
        weatherSummary = weatherSummary,
        packingAdvice = packingAdvice,
        mustSee = mustSee,
        foodAdvice = foodAdvice,
        localAdvice = localAdvice,
        aiSuggestion = aiSuggestion,
        comfortScore = comfortScore,
        userNote = userNote,
        userRating = userRating,
        lastWeatherAnalysisText = if (weatherAnalysisStatus == TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW) "Bekleniyor" else "Hazır",
        lastWeatherAnalysisDate = lastAnalysisAt,
        lastForecastSnapshot = lastForecastSnapshot,
        previousForecastSnapshot = previousForecastSnapshot,
        nextAnalysisEligibleDate = null,
        weatherAnalysisStatus = weatherAnalysisStatus.name,
        isArchived = isArchived,
        analyses = analyses,
        lastDailyNotificationDate = lastDailyNotificationDate,
        isDemo = isDemo,
        departureTime = departureTime
    )

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

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
