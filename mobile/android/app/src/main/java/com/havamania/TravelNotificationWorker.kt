package com.havamania

import kotlinx.coroutines.ensureActive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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

    private var executionEpoch = Long.MIN_VALUE

    override suspend fun doWork(): Result = try {
        performWork()
    } catch (error: kotlinx.coroutines.CancellationException) {
        throw error
    } catch (_: Exception) {
        Result.retry()
    }

    private suspend fun performWork(): Result {
        executionEpoch = NotificationSessionEpoch.capture()
        val application = applicationContext as android.app.Application
        if (AccountDeletionManager.getInstance(application).state.value.pending) return Result.success()
        val weatherDb = WeatherDatabase.getDatabase(application)
        val notificationDb = NotificationDatabase.getDatabase(application)
        val weatherDao = weatherDb.weatherDao()
        val notificationDao = notificationDb.notificationDao()

        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val currentUid = auth.currentUser?.uid ?: return Result.success()

        // Bildirimlerin açık olup olmadığını kontrol et
        val notificationsEnabled = ThemeManager.getNotificationsEnabled(application, currentUid).first()
        if (!notificationsEnabled) return Result.success()

        val plans = weatherDao.getAllTravelPlans(currentUid)
        val today = LocalDate.now()
        val dateStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        var retryRouteForecast = false
        plans.map { it.toDomain() }.forEach { plan ->
            if (!canPublishFor(currentUid)) return Result.success()
            if (plan.isArchived) return@forEach

            val planToday = tripAnalysisToday(plan, today)
            val daysUntil = ChronoUnit.DAYS.between(planToday, plan.startDate).toInt()
            val isOngoing = !planToday.isBefore(plan.startDate) && !planToday.isAfter(plan.endDate)
            val isUpcoming = daysUntil >= 0 && TravelStatusResolver.isTripAnalysisEligible(plan.startDate, plan.endDate, planToday)

            // Sadece yaklaşan veya devam eden seyahatler için günlük özet gönder
            if (isUpcoming || isOngoing) {
                // Bugün bu seyahat için bildirim gönderildi mi?
                if (plan.lastDailyNotificationDate == dateStr) {
                    if (routeForecastNeedsRetry(plan, application) { }) retryRouteForecast = true
                    return@forEach
                }

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
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    return@forEach
                }

                if (updatedPlan.weatherAnalysisStatus != TravelWeatherAnalysisStatus.WEATHER_READY_ANALYSIS_READY ||
                    updatedPlan.lastForecastSnapshot == null) return@forEach
                if (!canPublishFor(currentUid)) return Result.success()

                // Analiz güncellendiyse kaydet
                val analysisRevision = if (updatedPlan != plan) {
                    weatherDao.saveTripAnalysisIfVersion(updatedPlan.toEntity(), plan.updatedAt) ?: return@forEach
                } else plan.updatedAt
                if (weatherDao.getTravelPlanByIdRaw(plan.id)?.updatedAt != analysisRevision) return@forEach

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
                    deepLinkTarget = "havamania://app/calendar?focusId=${Uri.encode(plan.id)}",
                    actionType = NotificationActionType.TRAVEL_DETAIL,
                    targetId = plan.id,
                    relatedTripId = plan.id,
                    actionLabel = "Analizi Gör",
                    travelData = travelData,
                    deduplicationKey = "travel_daily_${currentUid}_${plan.id}_$dateStr"
                )

                if (!canPublishFor(currentUid)) return Result.success()
                val delivery = deliverLocalNotification(notificationItem, { notificationDao.reserveForDelivery(it) },
                    notificationDao::delete, complete = { notificationDao.completeDelivery(it.userId, it.id) },
                    recovery = NotificationDeliveryRecovery.shared, publish = ::showSystemNotification)
                if (delivery == LocalNotificationDelivery.SKIPPED) return@forEach

                // Güzergâh bildirimi kontrolü
                var finalRevision = analysisRevision
                if (routeForecastNeedsRetry(updatedPlan.copy(updatedAt = analysisRevision), application) { finalRevision = it }) {
                    retryRouteForecast = true
                }

                // Update only tracking metadata, never overwrite the whole trip with an old copy.
                weatherDao.markTripDailyNotification(currentUid, plan.id, finalRevision, dateStr)
            }
        }

        return if (shouldRetryRouteForecast(retryRouteForecast, runAttemptCount) && canPublishFor(currentUid)) {
            Result.retry()
        } else {
            Result.success()
        }
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

    private suspend fun routeForecastNeedsRetry(plan: TravelPlan, context: Context, onRevision: (Long) -> Unit): Boolean {
        if (!canPublishFor(plan.userId)) return false
        val departureDateTime = plan.departureDateTime ?: return false
        val nowInstant = Instant.now()
        val now = nowInstant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        val departureInstant = departureDateTime.atZone(ZoneId.systemDefault()).toInstant()
        if (!TravelStatusResolver.isWithinRouteWeatherWindow(departureInstant, nowInstant)) return false

        val notificationDao = NotificationDatabase.getDatabase(context).notificationDao()
        val weatherDao = WeatherDatabase.getDatabase(context).weatherDao()

        val todayStr = now.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dailyKey = "route_daily_${plan.userId}_${plan.id}_$todayStr"
        val notificationId = notificationEventRecordId(plan.userId, "route_daily", dailyKey)
        if (notificationDao.getVisibleNotification(plan.userId, notificationId) != null) return false

        val summary = when (val forecast = performRouteWeatherAnalysis(plan)) {
            RouteForecastOutcome.Unavailable -> return true
            RouteForecastOutcome.Ineligible -> return false
            is RouteForecastOutcome.Ready -> forecast.summary
        }

        val oldSummary = plan.routeWeatherSummary


        val title = when {
            oldSummary == null -> "Güzergâh hava tahminin hazır"
            oldSummary != summary -> "Güzergâhında hava değişikliği var"
            else -> "Güzergâhında hava güncellendi"
        }
        val notificationItem = NotificationItem(
            userId = plan.userId,
            id = notificationId,
            title = title,
            message = summary,
            category = NotificationCategory.TRAVEL,
            createdAt = System.currentTimeMillis(),
            deepLinkTarget = "havamania://app/route/${Uri.encode(plan.id)}",
            deduplicationKey = dailyKey,
            actionLabel = "Tahmini Aç"
        )

        val revision = weatherDao.saveTripAnalysisIfVersion(plan.copy(
            routeWeatherSummary = summary,
            lastRouteAnalysisAt = System.currentTimeMillis()
        ).toEntity(), plan.updatedAt) ?: return false
        onRevision(revision)
        if (!canPublishFor(plan.userId)) return false
        deliverLocalNotification(notificationItem, { notificationDao.reserveForDelivery(it) },
            notificationDao::delete, complete = { notificationDao.completeDelivery(it.userId, it.id) },
                    recovery = NotificationDeliveryRecovery.shared, publish = ::showSystemNotification)
        return false
    }

    private suspend fun performRouteWeatherAnalysis(plan: TravelPlan): RouteForecastOutcome {
        try {
            val origin = plan.originPoint?.let { GeoPoint(it.first, it.second) } ?: return RouteForecastOutcome.Ineligible
            val dest = GeoPoint(plan.latitude, plan.longitude)

            val routeProvider = OsrmRouteProvider()
            val routeResult = routeProvider.getRoute(origin, dest)
            when (routeResult) {
                RouteResult.NoRoute -> return RouteForecastOutcome.Ineligible
                is RouteResult.Error -> return RouteForecastOutcome.Unavailable
                is RouteResult.Success -> Unit
            }

            val route = routeResult.route
            val departureDateTime = plan.departureDateTime ?: return RouteForecastOutcome.Ineligible
            val departureMillis = departureDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val forecastPoints = route.forecastWaypoints(departureMillis)
            val results = RouteWeatherProvider().weatherAtBatch(
                forecastPoints.map { it.location },
                forecastPoints.map { it.etaEpochMillis }
            )
            return travelRouteForecastSummary(results)?.let { RouteForecastOutcome.Ready(it) }
                ?: RouteForecastOutcome.Unavailable
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                Log.e("RouteWorker", "Route analysis failed", e)
}
            return RouteForecastOutcome.Unavailable
        }
    }

    private fun canPublishFor(uid: String): Boolean =
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid == uid &&
            NotificationSessionEpoch.capture() == executionEpoch &&
            AccountDeletionManager.getInstance(applicationContext as android.app.Application).state.value.let { !it.pending && !it.busy }

    private suspend fun showSystemNotification(item: NotificationItem): Boolean {
        if (NotificationDatabase.getDatabase(applicationContext).notificationDao().isDeletedAccountHash(accountOwnerHash(item.userId))) return false
        if (!canPublishFor(item.userId)) return false
        if (!PermissionHelper.hasNotificationPermission(applicationContext)) {
            return false
        }
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

        val pendingIntent = notificationTapPendingIntent(applicationContext, item)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(item.title)
            .setContentText(item.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        return postNotificationIfCurrent(item.userId, executionEpoch,
            { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid },
            { !canPublishFor(item.userId) || !PermissionHelper.canPostToChannel(applicationContext, channelId) },
            enabled = { com.havamania.ui.theme.ThemeManager.getNotificationsEnabled(applicationContext, item.userId).first() },
            stillReserved = { NotificationDatabase.getDatabase(applicationContext).notificationDao().ownsPendingDelivery(item.userId, item.id) }) {
            notificationManager.notify(notificationSystemTag(item), 0, notification)
        }
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

internal fun travelRouteForecastSummary(results: List<WaypointWeather?>): String? {
    val forecasts = results.filterNotNull()
    if (forecasts.isEmpty()) return null

    val dangerCount = forecasts.count { it.risk == RouteRisk.DANGER }
    val summary = when {
        dangerCount > 0 -> "Güzergâhta $dangerCount noktada tehlikeli hava koşulları bekleniyor."
        forecasts.any { it.risk == RouteRisk.CAUTION } -> "Güzergâhın bazı noktalarında dikkat gerektiren hava koşulları bekleniyor."
        forecasts.size == results.size -> "İncelenen güzergâh noktalarında önemli bir hava riski görülmedi."
        else -> return "Güzergâhın bazı noktalarında tahmin alınamadı. Hava riski kısmen değerlendirilebildi."
    }
    return if (forecasts.size < results.size) {
        "$summary Bazı noktalar için tahmin alınamadı."
    } else {
        summary
    }
}

private sealed interface RouteForecastOutcome {
    data class Ready(val summary: String) : RouteForecastOutcome
    data object Ineligible : RouteForecastOutcome
    data object Unavailable : RouteForecastOutcome
}

internal fun shouldRetryRouteForecast(unavailable: Boolean, attempt: Int): Boolean =
    unavailable && attempt < 2
