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

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

class TravelViewModel(application: Application) : AndroidViewModel(application) {
    private val database = WeatherDatabase.getDatabase(application)
    private val dao = database.weatherDao()
    private val repository = WeatherRepository.getInstance(application)
    private val apiService = NetworkModule.apiService
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val currentUid: String get() = auth.currentUser?.uid ?: "legacy"
    private val timeProvider: TimeProvider = DefaultTimeProvider

    private var firestoreListener: ListenerRegistration? = null

    private val networkMonitor: NetworkMonitor = ConnectivityManagerNetworkMonitor(application)
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    private val _plans = MutableStateFlow<List<TravelPlan>>(emptyList())
    val plans: StateFlow<List<TravelPlan>> = _plans.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _citySuggestions = MutableStateFlow<List<GeocodingResultDto>>(emptyList())
    val citySuggestions: StateFlow<List<GeocodingResultDto>> = _citySuggestions.asStateFlow()

    private val _originSuggestions = MutableStateFlow<List<GeocodingResultDto>>(emptyList())
    val originSuggestions: StateFlow<List<GeocodingResultDto>> = _originSuggestions.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    private val TAG = "TravelAnalysisDebug"
    private val AUTO_TAG = "TravelAutoAnalysis"
    private val FLOW_TAG = "TripCreateFlow"

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val newUid = firebaseAuth.currentUser?.uid ?: "legacy"
        if (BuildConfig.DEBUG) Log.d(TAG, "Auth state changed. New UID: $newUid")
        _plans.value = emptyList()
        _isLoading.value = true
        loadPlansForUid(newUid)
        observeFirestoreTrips(newUid)
    }

    init {
        auth.addAuthStateListener(authListener)
        seedInitialDataIfNeeded()
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        firestoreListener?.remove()
        analysisJobs.values.forEach { it.cancel() }
    }

    private fun observeFirestoreTrips(uid: String) {
        firestoreListener?.remove()
        if (uid == "legacy") return

        firestoreListener = db.collection("users").document(uid).collection("trips")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Firestore listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val isFromCache = snapshot.metadata.isFromCache
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val entities = snapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(TravelPlanEntity::class.java)
                                } catch (me: Exception) {
                                    Log.e(TAG, "Data mapping error for doc ${doc.id}", me)
                                    null
                                }
                            }

                            entities.forEach { dao.insertTravelPlan(it) }

                            if (!isFromCache) {
                                val localEntities = dao.getAllTravelPlans(uid)
                                val remoteIds = entities.map { it.id }.toSet()
                                localEntities.forEach { local ->
                                    if (!remoteIds.contains(local.id) && !local.isDemo) {
                                        val ageMs = System.currentTimeMillis() - local.createdAt
                                        if (ageMs > 60000) {
                                            dao.deleteTravelPlan(local.id)
                                        }
                                    }
                                }
                            }
                        } catch (ex: Exception) {
                            Log.e(TAG, "Sync process failed", ex)
                        }
                    }
                }
            }
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


    fun seedInitialDataIfNeeded(force: Boolean = false) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (currentUid != "legacy" && !force) {
                loadPlans()
                return@launch
            }

            _isLoading.value = true
            val hasSeeded = ThemeManager.getHasSeededTrips(getApplication(), currentUid).first()
            val entities = if (force) emptyList() else dao.getAllTravelPlans(currentUid)

            if (entities.isEmpty() && (!hasSeeded || force)) {
                if (force) dao.clearAllTravelPlans(currentUid)

                if (currentUid == "legacy") {
                    val seedPlans = emptyList<TravelPlan>()
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
        _today.value = timeProvider.today()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isLoading.value = true
            val entities = dao.getAllTravelPlans(currentUid)
            val domainPlans = entities.map { it.toDomain() }.sortedBy { it.startDate }
            _plans.value = domainPlans
            _isLoading.value = false
            checkAndTriggerAutoAnalysis(domainPlans)
        }
    }

    private val analysisJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    private fun checkAndTriggerAutoAnalysis(plans: List<TravelPlan>) {
        val todayVal = _today.value
        plans.forEach { plan ->
            if (plan.isArchived) return@forEach

            val daysUntil = ChronoUnit.DAYS.between(todayVal, plan.startDate).toInt()
            val isWithinWindow = daysUntil <= TRIP_ANALYSIS_WINDOW_DAYS
            val isOver = todayVal.isAfter(plan.endDate)

            if (isOver) return@forEach

            val status = plan.weatherAnalysisStatus
            val shouldAutoAnalyze = isWithinWindow && (
                status == TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW ||
                status == TravelWeatherAnalysisStatus.WEATHER_FAILED ||
                plan.aiSuggestion == null
            )

            if (shouldAutoAnalyze && !analysisJobs.containsKey(plan.id)) {
                analyzeTravelWeather(plan)
            }
        }
    }

    fun analyzeTravelWeather(plan: TravelPlan) {
        if (analysisJobs.containsKey(plan.id)) return

        val job = viewModelScope.launch {
            _plans.value = _plans.value.map {
                if (it.id == plan.id) it.copy(isAnalyzing = true) else it
            }

            try {
                val updatedPlan = performAnalysis(plan)
                val isNewAnalysis = updatedPlan.analyses.size > plan.analyses.size ||
                                   (plan.analyses.isEmpty() && updatedPlan.analyses.isNotEmpty())

                if (updatedPlan.weatherAnalysisStatus == TravelWeatherAnalysisStatus.WEATHER_READY_ANALYSIS_READY && isNewAnalysis) {
                    val entity = updatedPlan.toEntity()
                    dao.insertTravelPlan(entity)
                    if (currentUid != "legacy") {
                        try {
                            db.collection("users").document(currentUid).collection("trips").document(updatedPlan.id).set(entity).await()
                        } catch (e: Exception) {
                            Log.e(TAG, "Analysis Firestore sync failed", e)
                        }
                    }
                    _plans.value = _plans.value.map {
                        if (it.id == plan.id) updatedPlan else it
                    }
                    _uiEvent.emit("Seyahat önerileri güncellendi.")
                } else if (updatedPlan.weatherAnalysisStatus == TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW) {
                     _plans.value = _plans.value.map {
                        if (it.id == plan.id) updatedPlan else it
                    }
                } else {
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
        return TravelAnalysisEngine.performAnalysis(
            context = getApplication(),
            plan = plan,
            currentUid = currentUid,
            apiService = apiService,
            repository = repository
        )
    }

    fun savePlan(plan: TravelPlan) {
        val cityNameTrimmed = plan.city.trim()
        if (cityNameTrimmed.isEmpty()) return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val todayVal = _today.value
            val now = LocalDateTime.now()

            val planDt = if (plan.departureTime != null) {
                try {
                    val parts = plan.departureTime!!.split(":")
                    plan.startDate.atTime(parts[0].toInt(), parts[1].toInt())
                } catch (e: Exception) {
                    plan.startDate.atTime(8, 0)
                }
            } else {
                plan.startDate.atTime(0, 0)
            }

            val isValidTime = if (plan.departureTime != null) {
                planDt.isAfter(now)
            } else {
                !plan.startDate.isBefore(LocalDate.now())
            }

            if (!isValidTime && !plan.isDemo) {
                _uiEvent.emit("PAST_TRIP_ERROR|Geçmiş bir tarih veya saat için seyahat oluşturamazsın.")
                return@launch
            }

            val existing = dao.getAllTravelPlans(currentUid)
            val isDuplicate = existing.any {
                it.id != plan.id && // Don't check against self when editing
                it.city.equals(cityNameTrimmed, ignoreCase = true) &&
                (it.district ?: "").equals(plan.district ?: "", ignoreCase = true) &&
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

            val entity = finalPlan.toEntity()
            dao.insertTravelPlan(entity)

            if (currentUid != "legacy") {
                try {
                    db.collection("users").document(currentUid).collection("trips")
                        .document(entity.id).set(entity).await()
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore save failed", e)
                    _uiEvent.emit("Seyahat yerel olarak kaydedildi ancak bulut senkronizasyonu şu anda yapılamıyor.")
                }
            }

            val daysUntil = ChronoUnit.DAYS.between(todayVal, plan.startDate).toInt()
            val isUpcoming = !plan.startDate.isBefore(todayVal)
            if (isUpcoming && daysUntil <= TRIP_ANALYSIS_WINDOW_DAYS) {
                analyzeTravelWeather(finalPlan)
            }
        }
    }

    fun deletePlan(id: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            dao.deleteTravelPlan(id)
            if (currentUid != "legacy") {
                try {
                    db.collection("users").document(currentUid).collection("trips")
                        .document(id).delete().await()
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore delete failed", e)
                    _uiEvent.emit("Şu anda seyahat buluttan silinemedi.")
                }
            }
        }
    }

    fun clearAllPlans() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val uid = currentUid
            dao.clearAllTravelPlans(uid)
            dao.clearAllWeatherCache()
            ThemeManager.saveHasSeededTrips(getApplication(), true, uid)
            _plans.value = emptyList()
        }
    }

    private var citySearchJob: kotlinx.coroutines.Job? = null
    private var originSearchJob: kotlinx.coroutines.Job? = null

    fun searchCity(query: String) {
        citySearchJob?.cancel()
        if (query.trim().length < 2) {
            _citySuggestions.value = emptyList()
            return
        }
        citySearchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            _citySuggestions.value = repository.searchCity(query.trim())
        }
    }

    fun searchOrigin(query: String) {
        originSearchJob?.cancel()
        if (query.trim().length < 2) {
            _originSuggestions.value = emptyList()
            return
        }
        originSearchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            _originSuggestions.value = repository.searchCity(query.trim())
        }
    }

    fun archiveTrip(id: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val plan = _plans.value.find { it.id == id } ?: return@launch
            val updated = plan.copy(isArchived = true, archivedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
            val entity = updated.toEntity()

            dao.insertTravelPlan(entity)
            if (currentUid != "legacy") {
                try {
                    db.collection("users").document(currentUid).collection("trips").document(id).set(entity).await()
                } catch (e: Exception) {
                    Log.e(TAG, "Archive Firestore sync failed", e)
                    _uiEvent.emit("Şu anda seyahat arşivlenemedi.")
                }
            }
        }
    }

    fun unarchiveTrip(id: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val plan = _plans.value.find { it.id == id } ?: return@launch
            val updated = plan.copy(isArchived = false, archivedAt = null, updatedAt = System.currentTimeMillis())
            val entity = updated.toEntity()

            dao.insertTravelPlan(entity)
            if (currentUid != "legacy") {
                try {
                    db.collection("users").document(currentUid).collection("trips").document(id).set(entity).await()
                } catch (e: Exception) {
                    Log.e(TAG, "Unarchive Firestore sync failed", e)
                    _uiEvent.emit("Şu anda seyahat aktifleştirilemedi.")
                }
            }
        }
    }

    fun updateTripNoteAndRating(id: String, note: String, rating: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val plan = _plans.value.find { it.id == id } ?: return@launch
            val updated = plan.copy(userNote = note, userRating = rating, updatedAt = System.currentTimeMillis())
            val entity = updated.toEntity()

            dao.insertTravelPlan(entity)
            if (currentUid != "legacy") {
                try {
                    db.collection("users").document(currentUid).collection("trips").document(id).set(entity).await()
                } catch (e: Exception) {
                    Log.e(TAG, "Note/Rating Firestore sync failed", e)
                    _uiEvent.emit("Not kaydedilemedi ancak yerel olarak saklandı.")
                }
            }
        }
    }

    fun migrateLegacyDataToUser() {
        if (currentUid == "legacy") return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val legacyPlans = dao.getAllTravelPlans("legacy")
            if (legacyPlans.isNotEmpty()) {
                legacyPlans.forEach { entity ->
                    val newEntity = entity.copy(id = UUID.randomUUID().toString(), userId = currentUid)
                    dao.insertTravelPlan(newEntity)
                    try {
                        db.collection("users").document(currentUid).collection("trips").document(newEntity.id).set(newEntity).await()
                    } catch (e: Exception) {
                        Log.e(TAG, "Migration sync failed for trip ${newEntity.id}", e)
                    }
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

    fun getTripStatus(plan: TravelPlan): com.havamania.TripStatus {
        val todayValue = _today.value
        val status = TravelStatusResolver.getStatus(plan.startDate, plan.endDate, todayValue)
        return when (status) {
            TravelStatus.PAST -> com.havamania.TripStatus.COMPLETED
            TravelStatus.ONGOING -> com.havamania.TripStatus.ACTIVE
            TravelStatus.UPCOMING -> com.havamania.TripStatus.UPCOMING
        }
    }
}
