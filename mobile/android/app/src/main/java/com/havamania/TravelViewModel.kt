package com.havamania

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.havamania.ui.theme.ThemeManager
import com.havamania.ui.theme.AssistantTone
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.havamania.NetworkMonitor
import com.havamania.ConnectivityManagerNetworkMonitor
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

class TravelViewModel(application: Application) : AndroidViewModel(application) {
    private val database = WeatherDatabase.getDatabase(application)
    private val dao = database.weatherDao()
    private val repository = WeatherRepository.getInstance(application)
    private val apiService = NetworkModule.apiService
    private val auth = FirebaseAuth.getInstance()
    private val currentUid: String get() = auth.currentUser?.uid ?: "legacy"

    private val networkMonitor: NetworkMonitor = ConnectivityManagerNetworkMonitor(application)
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    private val _plans = MutableStateFlow<List<TravelPlan>>(emptyList())
    val plans: StateFlow<List<TravelPlan>> = _plans.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _citySuggestions = MutableStateFlow<List<GeocodingResultDto>>(emptyList())
    val citySuggestions: StateFlow<List<GeocodingResultDto>> = _citySuggestions.asStateFlow()

    private val _uiEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val TAG = "TravelAnalysisDebug"
    private val AUTO_TAG = "TravelAutoAnalysis"
    private val FLOW_TAG = "TripCreateFlow"

    private val CITY_FALLBACKS = mapOf(
        "istanbul" to Pair(41.0082, 28.9784),
        "ankara" to Pair(39.9334, 32.8597),
        "izmir" to Pair(38.4237, 27.1428),
        "antalya" to Pair(36.8969, 30.7133),
        "balikesir" to Pair(39.6484, 27.8826),
        "trabzon" to Pair(41.0027, 39.7168),
        "mardin" to Pair(37.3129, 40.7350),
        "gaziantep" to Pair(37.0662, 37.3833),
        "batman" to Pair(37.8812, 41.1322),
        "bali" to Pair(-8.4095, 115.1889)
    )

    init {
        viewModelScope.launch {
            auth.addAuthStateListener { firebaseAuth ->
                val newUid = firebaseAuth.currentUser?.uid ?: "legacy"
                Log.d(TAG, "Auth state changed. New UID: $newUid")
                // Reset state immediately
                _plans.value = emptyList()
                _isLoading.value = true

                // Restart data flow if needed, but since getAllTravelPlansFlow
                // is likely collected in a child coroutine, I should manage it.
                loadPlansForUid(newUid)
            }
        }
        seedInitialDataIfNeeded()
    }

    private var plansJob: kotlinx.coroutines.Job? = null

    private fun loadPlansForUid(uid: String) {
        plansJob?.cancel()
        plansJob = viewModelScope.launch {
            dao.getAllTravelPlansFlow(uid).collect { entities ->
                val domainPlans = entities.map { it.toDomain() }.sortedBy { it.startDate }
                _plans.value = domainPlans
                checkAndTriggerAutoAnalysis(domainPlans)
                _isLoading.value = false
            }
        }
    }

    private fun normalizeCityName(name: String): String {
        return name.trim().lowercase(Locale("tr"))
            .replace('ç', 'c')
            .replace('ğ', 'g')
            .replace('ı', 'i')
            .replace("i\u0307", "i")
            .replace('ö', 'o')
            .replace('ş', 's')
            .replace('ü', 'u')
    }

    fun seedInitialDataIfNeeded(force: Boolean = false) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Yeni kullanıcılar tamamen boş başlamalı.
            if (currentUid != "legacy" && !force) {
                Log.d(TAG, "Logged in user: $currentUid starting with empty list.")
                loadPlans()
                return@launch
            }

            _isLoading.value = true
            val hasSeeded = ThemeManager.getHasSeededTrips(getApplication(), currentUid).first()
            val entities = if (force) emptyList() else dao.getAllTravelPlans(currentUid)

            // Sadece giriş yapmamış (legacy) kullanıcılar için ilk seferde örnek veri ekle
            if (entities.isEmpty() && (!hasSeeded || force)) {
                if (force) dao.clearAllTravelPlans(currentUid)

                if (currentUid == "legacy") {
                    val today = LocalDate.now()
                    val currentYear = today.year

                    val seedPlans = listOf(
                        TravelPlan(
                            userId = "legacy",
                            city = "Batman",
                            latitude = 37.8812,
                            longitude = 41.1322,
                            tripType = TripType.EVENT,
                            startDate = LocalDate.of(currentYear, 5, 15),
                            endDate = LocalDate.of(currentYear, 5, 16),
                            isDemo = true
                        ),
                        TravelPlan(
                            userId = "legacy",
                            city = "Bali",
                            latitude = -8.4095,
                            longitude = 115.1889,
                            tripType = TripType.WEEKEND,
                            startDate = LocalDate.of(currentYear, 5, 28),
                            endDate = LocalDate.of(currentYear, 5, 30),
                            isDemo = true
                        ),
                        TravelPlan(
                            userId = "legacy",
                            city = "Trabzon",
                            latitude = 41.0027,
                            longitude = 39.7168,
                            tripType = TripType.SHOPPING,
                            startDate = LocalDate.of(currentYear, 5, 18),
                            endDate = LocalDate.of(currentYear, 5, 21),
                            isDemo = true
                        )
                    )

                    seedPlans.forEach { plan ->
                        dao.insertTravelPlan(plan.toEntity())
                    }
                }
                ThemeManager.saveHasSeededTrips(getApplication(), true, currentUid)
            }
            loadPlans()
        }
    }

    fun loadPlans() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isLoading.value = true
            val entities = dao.getAllTravelPlans(currentUid)
            Log.d(TAG, "Source: Room DB, Loaded ${entities.size} entities for $currentUid")

            val domainPlans = entities.map { it.toDomain() }.sortedBy { it.startDate }
            _plans.value = domainPlans
            _isLoading.value = false

            checkAndTriggerAutoAnalysis(domainPlans)
        }
    }

    private val analysisJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    private fun checkAndTriggerAutoAnalysis(plans: List<TravelPlan>) {
        val today = LocalDate.now()
        plans.forEach { plan ->
            if (plan.isArchived) return@forEach

            val daysUntil = ChronoUnit.DAYS.between(today, plan.startDate).toInt()
            val isWithinWindow = daysUntil <= TRIP_ANALYSIS_WINDOW_DAYS
            val isOver = today.isAfter(plan.endDate)

            if (isOver) return@forEach

            val status = plan.weatherAnalysisStatus

            // KURAL: 10 gün ve altındaysa otomatik analiz başlar
            val shouldAutoAnalyze = isWithinWindow && (
                status == TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW ||
                status == TravelWeatherAnalysisStatus.WEATHER_FAILED ||
                plan.aiSuggestion == null
            )

            if (shouldAutoAnalyze && !analysisJobs.containsKey(plan.id)) {
                Log.i(AUTO_TAG, "AutoAnalysisTriggered City=${plan.city} DaysUntil=$daysUntil")
                analyzeTravelWeather(plan)
            }
        }
    }

    fun analyzeTravelWeather(plan: TravelPlan) {
        if (analysisJobs.containsKey(plan.id)) return // Race condition engeli

        val job = viewModelScope.launch {
            Log.d(TAG, "RE-ANALYZE TRIGGERED for ${plan.city}")

            _plans.value = _plans.value.map {
                if (it.id == plan.id) it.copy(isAnalyzing = true) else it
            }

            try {
                val updatedPlan = performAnalysis(plan)

                // Root Cause Fix: Check if a NEW analysis was actually generated (Issue #3)
                val isNewAnalysis = updatedPlan.analyses.size > plan.analyses.size ||
                                   (plan.analyses.isEmpty() && updatedPlan.analyses.isNotEmpty())

                if (updatedPlan.weatherAnalysisStatus == TravelWeatherAnalysisStatus.WEATHER_READY_ANALYSIS_READY && isNewAnalysis) {
                    dao.insertTravelPlan(updatedPlan.toEntity())
                    // Atomik update
                    _plans.value = _plans.value.map {
                        if (it.id == plan.id) updatedPlan else it
                    }
                    _uiEvent.emit("Seyahat önerileri güncellendi.")
                } else if (updatedPlan.weatherAnalysisStatus == TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW) {
                     _plans.value = _plans.value.map {
                        if (it.id == plan.id) updatedPlan else it
                    }
                    // No snackbar for natural waiting state
                } else {
                    // Failure case: preserve old data in UI but show error
                    _plans.value = _plans.value.map {
                        if (it.id == plan.id) it.copy(isAnalyzing = false) else it
                    }
                    _uiEvent.emit("Öneriler şu anda hazırlanamadı. Biraz sonra tekrar deneyebilirsiniz.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Analysis failed", e)
                _plans.value = _plans.value.map {
                    if (it.id == plan.id) it.copy(isAnalyzing = false) else it
                }
                _uiEvent.emit("Öneriler şu anda hazırlanamadı. Biraz sonra tekrar deneyebilirsiniz.")
            } finally {
                analysisJobs.remove(plan.id)
            }
        }
        analysisJobs[plan.id] = job
    }

    suspend fun performAnalysis(plan: TravelPlan): TravelPlan {
        val today = LocalDate.now()
        val daysUntil = ChronoUnit.DAYS.between(today, plan.startDate).toInt()
        val isWithinWindow = daysUntil <= TRIP_ANALYSIS_WINDOW_DAYS

        val tone = ThemeManager.getAssistantTone(getApplication(), currentUid).first()

        // 1. TAMAMLANMIŞ SEYAHAT KONTROLÜ
        if (today.isAfter(plan.endDate)) {
             val suggestion = TravelAiHelper.generateTravelAiSuggestion(
                city = plan.city, tripType = plan.tripType, forecastSnapshot = null,
                previousSnapshot = null, daysUntilTrip = daysUntil, isPastTrip = true,
                endDate = plan.endDate, tone = tone
            )
            return plan.copy(
                isAnalyzing = false,
                weatherAnalysisStatus = TravelWeatherAnalysisStatus.WEATHER_READY_ANALYSIS_READY,
                aiSuggestion = suggestion,
                lastAnalysisAt = System.currentTimeMillis()
            )
        }

        // 2. 10 GÜN KURALI (Business Rule 1)
        if (!isWithinWindow) {
            return plan.copy(
                isAnalyzing = false,
                weatherAnalysisStatus = TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW,
                aiSuggestion = null,
                lastAnalysisAt = System.currentTimeMillis()
            )
        }

        // 3. KOORDİNAT VE API ÇAĞRISI (Yalnızca gerekiyorsa)
        var lat = plan.latitude
        var lon = plan.longitude

        if (lat == 0.0 && lon == 0.0) {
            val normalized = normalizeCityName(plan.city)
            val fallback = CITY_FALLBACKS[normalized]
            if (fallback != null) {
                lat = fallback.first
                lon = fallback.second
            } else {
                val geoResults = try {
                    kotlinx.coroutines.withTimeout(5000) { repository.searchCity(plan.city) }
                } catch(e: Exception) { emptyList() }

                if (geoResults.isNotEmpty()) {
                    lat = geoResults[0].latitude
                    lon = geoResults[0].longitude
                }
            }
        }

        if (lat == 0.0 && lon == 0.0) throw Exception("Koordinat bulunamadı")

        // API Çağrısı (Timeout ile)
        val response = try {
            kotlinx.coroutines.withTimeout(15000) {
                apiService.getFullWeather(lat = lat, lon = lon, days = 16)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Weather API fail", e)
            null
        }

        if (response == null) {
            // Offline/Hata durumunda mevcut veriyi koru
            if (plan.lastForecastSnapshot != null) {
                return plan.copy(isAnalyzing = false, weatherAnalysisStatus = TravelWeatherAnalysisStatus.WEATHER_PARTIAL_READY)
            }
            throw Exception("API hatası")
        }

        // 4. ANALİZ ÜRETİMİ
        val daily = response.daily
        var snapshot: ForecastSnapshot? = null

        if (daily != null) {
            val tripDates = daily.time.map { LocalDate.parse(it) }
            val overlapIndices = tripDates.indices.filter { i ->
                val date = tripDates[i]
                !date.isBefore(plan.startDate) && !date.isAfter(plan.endDate)
            }

            if (overlapIndices.isNotEmpty()) {
                val maxCode = overlapIndices.mapNotNull { i -> daily.weatherCode.getOrNull(i) }.groupBy { it }.maxByOrNull { it.value.size }?.key ?: 0
                val avgMin = overlapIndices.mapNotNull { i -> daily.tempMin.getOrNull(i) }.average()
                val avgMax = overlapIndices.mapNotNull { i -> daily.tempMax.getOrNull(i) }.average()

                snapshot = ForecastSnapshot(
                    precipitationProbability = overlapIndices.mapNotNull { i -> daily.precipProbMax?.getOrNull(i) }.maxOrNull(),
                    minTemp = avgMin,
                    maxTemp = avgMax,
                    windSpeed = overlapIndices.mapNotNull { i -> daily.windSpeedMax.getOrNull(i) }.maxOrNull(),
                    uvIndex = overlapIndices.mapNotNull { i -> daily.uvIndexMax?.getOrNull(i) }.maxOrNull(),
                    conditionSummary = WeatherMapper.getWeatherCondition(maxCode),
                    weatherCode = maxCode,
                    travelScore = calculateTravelScore(ForecastSnapshot(minTemp = avgMin, maxTemp = avgMax), plan.tripType)
                )
            }
        }

        // Kişiselleştirme
        val interests = ThemeManager.getUserInterests(getApplication(), currentUid).first()
        val personalization = PersonalizationProfile(uid = currentUid, selectedInterests = interests.toList())

        val aiResult = TravelAiHelper.generateTravelAiSuggestion(
            plan.city, plan.tripType, snapshot, plan.lastForecastSnapshot,
            daysUntil, tone = tone, personalization = personalization
        )

        // Parse AI Result to separate fields for the model (Stability)
        val sections = aiResult.split("[SEP]")
        var weatherSum: String? = null
        var pack: String? = null
        var must: String? = null
        var food: String? = null
        var local: String? = null

        sections.forEach { s ->
            when {
                s.contains("HAVA ÖZETİ|") -> weatherSum = s.split("|").last().trim()
                s.contains("VALİZ TAVSİYESİ|") -> pack = s.split("|").last().trim()
                s.contains("MUTLAKA GÖR|") -> must = s.split("|").last().trim()
                s.contains("DENEMEDEN DÖNME|") -> food = s.split("|").last().trim()
                s.contains("YEREL TAVSİYE|") -> local = s.split("|").last().trim()
            }
        }

        return plan.copy(
            isAnalyzing = false,
            aiSuggestion = aiResult,
            weatherSummary = weatherSum,
            packingAdvice = pack,
            mustSee = must,
            foodAdvice = food,
            localAdvice = local,
            comfortScore = snapshot?.travelScore,
            lastAnalysisAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastForecastSnapshot = snapshot,
            previousForecastSnapshot = plan.lastForecastSnapshot,
            weatherAnalysisStatus = TravelWeatherAnalysisStatus.WEATHER_READY_ANALYSIS_READY,
            latitude = lat,
            longitude = lon,
            analyses = plan.analyses + TravelWeatherAnalysis(
                tripId = plan.id,
                travelScore = snapshot?.travelScore ?: 0,
                rainRiskPercent = snapshot?.precipitationProbability,
                averageTemperature = ((snapshot?.minTemp ?: 0.0) + (snapshot?.maxTemp ?: 0.0)) / 2.0,
                summary = weatherSum ?: "Hava durumu verisi alındı.",
                recommendation = aiResult,
                comparisonText = if (plan.lastForecastSnapshot != null && snapshot != null)
                    TravelAiHelper.generateComparisonText(plan.lastForecastSnapshot, snapshot)
                    else null
            )
        )
    }

    private fun estimateRainRisk(condition: String?): Int {
        if (condition == null) return 10 // "Düşük"

        val c = condition.lowercase(Locale("tr"))
        return when {
            c.contains("güneşli") || c.contains("açık") -> 5
            c.contains("parçalı bulutlu") -> 15
            c.contains("bulutlu") -> 30
            c.contains("sisli") || c.contains("puslu") -> 20
            c.contains("yağmurlu") -> 70
            c.contains("sağanak") -> 85
            c.contains("fırtınalı") || c.contains("gök gürültülü") -> 90
            c.contains("karlı") -> 75
            else -> 10 // "Düşük"
        }
    }

    private fun calculateTravelScore(snapshot: ForecastSnapshot, type: TripType): Int {
        var score = 85 + (Math.random() * 10).toInt() // Base score between 85-95
        val precip = snapshot.precipitationProbability ?: 0
        val wind = snapshot.windSpeed ?: 0.0
        val temp = snapshot.maxTemp ?: 20.0

        // Impact of precipitation
        if (precip > 70) score -= 35
        else if (precip > 40) score -= 20
        else if (precip > 15) score -= 8

        // Impact of wind
        if (wind > 50) score -= 25
        else if (wind > 30) score -= 12
        else if (wind > 20) score -= 5

        // Impact of temperature based on trip type
        when(type) {
            TripType.BEACH -> {
                if (temp < 22) score -= 30
                else if (temp < 26) score -= 15
                else if (temp > 38) score -= 10
            }
            TripType.WINTER -> {
                if (temp > 8) score -= 25
                else if (temp < -10) score -= 15
            }
            TripType.CAMPING -> {
                if (temp < 12) score -= 25
                else if (temp > 35) score -= 15
                if (precip > 30) score -= 20
            }
            TripType.SPORTS -> {
                if (temp > 32) score -= 20
                if (wind > 25) score -= 15
            }
            else -> {
                if (temp < 5) score -= 20
                else if (temp > 35) score -= 15
            }
        }

        return score.coerceIn(40, 100)
    }

    private fun generateComparisonSummary(old: TravelWeatherAnalysis, newScore: Int, newSnapshot: ForecastSnapshot): String {
        val sb = StringBuilder()
        val scoreDiff = newScore - old.travelScore
        if (scoreDiff != 0) {
            val direction = if (scoreDiff > 0) "iyileşti" else "düştü"
            val formatted = WeatherUtils.formatRainProbability(kotlin.math.abs(scoreDiff))
            sb.append("Seyahat skoru önceki analize göre $formatted $direction. ")
        }

        val oldTemp = old.averageTemperature
        val newTemp = ((newSnapshot.minTemp ?: 0.0) + (newSnapshot.maxTemp ?: 0.0)) / 2.0
        val tempDiff = (newTemp - oldTemp).toInt()

        if (tempDiff != 0) {
            val sign = if (tempDiff > 0) "+" else ""
            sb.append("Ortalama sıcaklık $sign$tempDiff° değişti. ")
        }

        val oldRain = old.rainRiskPercent ?: 0
        val newRain = newSnapshot.precipitationProbability ?: 0
        val rainDiff = newRain - oldRain

        if (kotlin.math.abs(rainDiff) > 10) {
            val direction = if (rainDiff > 0) "arttı" else "azaldı"
            val formatted = WeatherUtils.formatRainProbability(kotlin.math.abs(rainDiff))
            sb.append("Yağış riski $formatted $direction.")
        }

        return sb.toString().trim()
    }

    fun savePlan(plan: TravelPlan) {
        val cityNameTrimmed = plan.city.trim()
        if (cityNameTrimmed.isEmpty()) return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val today = LocalDate.now()
            val daysUntil = ChronoUnit.DAYS.between(today, plan.startDate).toInt()
            val isUpcoming = !plan.startDate.isBefore(today)

            // DUPLICATE CHECK (Business Rule 6)
            val existing = dao.getAllTravelPlans(currentUid)
            val isDuplicate = existing.any {
                it.city.equals(cityNameTrimmed, ignoreCase = true) &&
                it.startDate == plan.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() &&
                it.endDate == plan.endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }

            if (isDuplicate && !plan.isDemo) {
                _uiEvent.emit("DUPLICATE_TRIP|Bu şehir ve tarihler için zaten bir seyahatin bulunuyor.")
                return@launch
            }

            val finalPlan = if (plan.userId == "legacy" && currentUid != "legacy") {
                plan.copy(userId = currentUid, city = cityNameTrimmed, updatedAt = System.currentTimeMillis())
            } else {
                plan.copy(city = cityNameTrimmed, updatedAt = System.currentTimeMillis())
            }

            dao.insertTravelPlan(finalPlan.toEntity())

            val domainPlans = dao.getAllTravelPlans(currentUid).map { it.toDomain() }.sortedBy { it.startDate }

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                _plans.value = domainPlans
                if (isUpcoming && daysUntil <= TRIP_ANALYSIS_WINDOW_DAYS) {
                    analyzeTravelWeather(finalPlan)
                }
            }
        }
    }

    fun deletePlan(id: String) {
        viewModelScope.launch {
            Log.i(TAG, "Deleting TripId=$id")
            dao.deleteTravelPlan(id)
            loadPlans()
        }
    }

    fun clearAllPlans() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val uid = currentUid
            dao.clearAllTravelPlans(uid)
            dao.clearAllWeatherCache() // Seyahatlerle ilgili hava durumu önbelleğini de temizle
            ThemeManager.saveHasSeededTrips(getApplication(), true, uid)
            _plans.value = emptyList()
        }
    }

    fun searchCity(query: String) {
        if (query.length < 2) {
            _citySuggestions.value = emptyList()
            return
        }
        viewModelScope.launch {
            _citySuggestions.value = repository.searchCity(query)
        }
    }

    fun archiveTrip(id: String) {
        viewModelScope.launch {
            val plan = _plans.value.find { it.id == id } ?: return@launch
            val updated = plan.copy(isArchived = true, archivedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
            dao.insertTravelPlan(updated.toEntity())
            loadPlans()
        }
    }

    fun unarchiveTrip(id: String) {
        viewModelScope.launch {
            val plan = _plans.value.find { it.id == id } ?: return@launch
            val updated = plan.copy(isArchived = false, archivedAt = null, updatedAt = System.currentTimeMillis())
            dao.insertTravelPlan(updated.toEntity())
            loadPlans()
        }
    }

    fun updateTripNoteAndRating(id: String, note: String, rating: Int) {
        viewModelScope.launch {
            val plan = _plans.value.find { it.id == id } ?: return@launch
            val updated = plan.copy(userNote = note, userRating = rating)
            dao.insertTravelPlan(updated.toEntity())
            loadPlans()
        }
    }

    fun migrateLegacyDataToUser() {
        if (currentUid == "legacy") return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val legacyPlans = dao.getAllTravelPlans("legacy")
            if (legacyPlans.isNotEmpty()) {
                legacyPlans.forEach { entity ->
                    dao.insertTravelPlan(entity.copy(id = UUID.randomUUID().toString(), userId = currentUid))
                }
            }
            ThemeManager.saveMigrationChoiceMade(getApplication(), currentUid, true)
            loadPlans()
        }
    }

    fun declineMigration() {
        if (currentUid == "legacy") return
        viewModelScope.launch {
            ThemeManager.saveMigrationChoiceMade(getApplication(), currentUid, true)
        }
    }

    /**
     * Merkezi Seyahat Durum Hesaplayıcısı (Business Rule 2)
     */
    fun getTripStatus(plan: TravelPlan): com.havamania.TripStatus {
        val today = LocalDate.now()
        return when {
            today.isAfter(plan.endDate) -> com.havamania.TripStatus.COMPLETED
            !today.isBefore(plan.startDate) && !today.isAfter(plan.endDate) -> com.havamania.TripStatus.ACTIVE
            else -> com.havamania.TripStatus.UPCOMING
        }
    }

    private fun TravelPlanEntity.toDomain() = TravelPlan(
        id = id,
        userId = userId,
        city = city,
        latitude = latitude,
        longitude = longitude,
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
        isAnalyzing = false, // Reset flag on load
        weatherAnalysisStatus = try { TravelWeatherAnalysisStatus.valueOf(weatherAnalysisStatus) } catch (e: Exception) { TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW },
        isArchived = isArchived,
        analyses = analyses,
        lastDailyNotificationDate = lastDailyNotificationDate,
        isDemo = isDemo,
        lastForecastSnapshot = lastForecastSnapshot,
        previousForecastSnapshot = previousForecastSnapshot
    )

    private fun TravelPlan.toEntity() = TravelPlanEntity(
        id = id,
        userId = userId,
        city = city,
        latitude = latitude,
        longitude = longitude,
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
        weatherAnalysisStatus = weatherAnalysisStatus.name,
        isArchived = isArchived,
        analyses = analyses,
        lastDailyNotificationDate = lastDailyNotificationDate,
        isDemo = isDemo
    )
}
