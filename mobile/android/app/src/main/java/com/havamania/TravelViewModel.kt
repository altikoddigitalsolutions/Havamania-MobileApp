package com.havamania

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.havamania.ui.theme.ThemeManager
import com.havamania.ui.theme.AssistantTone
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
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
    private val accountTasks = com.havamania.AccountTaskScope(viewModelScope) { currentUid }
    private val timeProvider: TimeProvider = DefaultTimeProvider

    private var firestoreListener: ListenerRegistration? = null

    private val networkMonitor: NetworkMonitor = ConnectivityManagerNetworkMonitor(application)
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    private val _confirmedEmptyOwner = MutableStateFlow<String?>(null)
    val confirmedEmptyOwner = _confirmedEmptyOwner.asStateFlow()

    private val uploadAttempt = SyncAttemptState()
    private val deletionAttempt = SyncAttemptState()
    val uploadRetrying = uploadAttempt.running
    val deletionRetrying = deletionAttempt.running
    val uploadSyncError = uploadAttempt.error
    val deletionSyncError = deletionAttempt.error

    private val _pendingDeletions = MutableStateFlow(0)
    val pendingDeletions = _pendingDeletions.asStateFlow()

    private val _pendingUploads = MutableStateFlow(0)
    val pendingUploads = _pendingUploads.asStateFlow()
    private var uploadRetryJob: kotlinx.coroutines.Job? = null

    private val _plans = MutableStateFlow<List<TravelPlan>>(emptyList())
    val plans: StateFlow<List<TravelPlan>> = _plans.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _loadError = MutableStateFlow<String?>(null)
    val loadError = _loadError.asStateFlow()

    private val _citySearchStatus = MutableStateFlow(LocationSearchStatus.IDLE)
    val citySearchStatus = _citySearchStatus.asStateFlow()
    private val _originSearchStatus = MutableStateFlow(LocationSearchStatus.IDLE)
    val originSearchStatus = _originSearchStatus.asStateFlow()

    private val _citySuggestions = MutableStateFlow<List<GeocodingResultDto>>(emptyList())
    val citySuggestions: StateFlow<List<GeocodingResultDto>> = _citySuggestions.asStateFlow()

    private val _originSuggestions = MutableStateFlow<List<GeocodingResultDto>>(emptyList())
    val originSuggestions: StateFlow<List<GeocodingResultDto>> = _originSuggestions.asStateFlow()

    private val _sessionVersion = MutableStateFlow(0L)
    val sessionVersion = _sessionVersion.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _today = MutableStateFlow(timeProvider.today())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    private suspend fun emitAccountMessage(owner: String, message: String) {
        publishAccountResult(owner, { currentUid }) { _uiEvent.emit(message) }
    }

    private val TAG = "TravelAnalysisDebug"
    private val AUTO_TAG = "TravelAutoAnalysis"
    private val FLOW_TAG = "TripCreateFlow"

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        uploadAttempt.reset()
        deletionAttempt.reset()
        accountTasks.reset()
        _sessionVersion.value++
        _citySearchStatus.value = LocationSearchStatus.IDLE
        _originSearchStatus.value = LocationSearchStatus.IDLE
        _citySuggestions.value = emptyList()
        _originSuggestions.value = emptyList()
        val newUid = firebaseAuth.currentUser?.uid ?: "legacy"
        if (BuildConfig.DEBUG) Log.d(TAG, "Auth state changed. New UID: $newUid")
        analysisJobs.values.forEach { it.cancel() }
        analysisJobs.clear()
        _confirmedEmptyOwner.value = null
        _pendingDeletions.value = 0
        accountTasks.launch { uid ->
            dao.pendingTripDeletionCount(uid).collect { count ->
                if (uid == currentUid) _pendingDeletions.value = count
            }
        }
        _pendingUploads.value = 0
        accountTasks.launch { uid ->
            dao.pendingTripUploadCount(uid).collect { count ->
                if (uid == currentUid) _pendingUploads.value = count
            }
        }
        _plans.value = emptyList()
        _isLoading.value = true
        loadPlansForUid(newUid)
        observeFirestoreTrips(newUid)
        retryPendingTripDeletions()
        retryPendingTripUploads()
    }

    init {
        auth.addAuthStateListener(authListener)
        viewModelScope.launch {
            networkMonitor.isOnline.distinctUntilChanged().collect { online ->
                if (online) {
                    retryPendingTripDeletions()
                    retryPendingTripUploads()
                }
            }
        }
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
            .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, e ->
                if (currentUid != uid) return@addSnapshotListener
                if (e != null) {
if (BuildConfig.DEBUG) {
                        Log.w(TAG, "Firestore listen failed.", e)
}
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    accountTasks.launch(kotlinx.coroutines.Dispatchers.IO) launch@ { currentUid ->
                        try {
                            val entities = snapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(TravelPlanEntity::class.java)?.copy(id = doc.id, userId = uid)
                                } catch (me: Exception) {
                                    if (me is kotlinx.coroutines.CancellationException) throw me
if (BuildConfig.DEBUG) {
                                        Log.e(TAG, "Data mapping error for doc ${doc.id}", me)
}
                                    null
                                }
                            }

                            entities.forEach { dao.insertTravelPlan(it) }
                            if (uid == this@TravelViewModel.currentUid &&
                                !snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()) {
                                _confirmedEmptyOwner.value = if (snapshot.isEmpty) uid else null
                            }


                            // Remote absence is not proof of deletion: local trips may still
                            // be waiting for upload. Reconciliation requires persisted sync
                            // acknowledgements/tombstones, not the age of a local record.

                        } catch (ex: Exception) {
                            if (ex is kotlinx.coroutines.CancellationException) throw ex
if (BuildConfig.DEBUG) {
                                Log.e(TAG, "Sync process failed", ex)
}
                        }
                    }
                }
            }
    }

    private var plansJob: kotlinx.coroutines.Job? = null

    @Synchronized private fun loadPlansForUid(uid: String) {
        plansJob?.cancel()
        _isLoading.value = true
        _loadError.value = null
        plansJob = accountTasks.launch launch@ { owner ->
            if (owner != uid) return@launch
            val source = flow { emitAll(dao.getAllTravelPlansFlow(uid)) }
                .map { entities -> entities.map { it.toDomain() }.sortedBy { it.startDate } }
                .flowOn(kotlinx.coroutines.Dispatchers.IO)
            observeAccountData(source, uid, { this@TravelViewModel.currentUid }, onData = { domainPlans ->
                _plans.value = domainPlans
                _isLoading.value = false
                _loadError.value = null
                checkAndTriggerAutoAnalysis(domainPlans)
            }, onError = { error ->
                if (BuildConfig.DEBUG) Log.e(TAG, "Local trip loading failed", error)
                _isLoading.value = false
                _loadError.value = "Seyahatler cihazdan yüklenemedi. Yeniden deneyin."
            })
        }
    }

    fun refreshToday() {
        _today.value = timeProvider.today()
    }

    fun loadPlans() {
        refreshToday()
        loadPlansForUid(currentUid)
    }

    private val analysisJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    private fun checkAndTriggerAutoAnalysis(plans: List<TravelPlan>) {
        val todayVal = _today.value
        plans.forEach { plan ->
            if (plan.isArchived) return@forEach

            val planToday = tripAnalysisToday(plan, todayVal)
            val isWithinWindow = TravelStatusResolver.isTripAnalysisEligible(plan.startDate, plan.endDate, planToday)
            val isOver = planToday.isAfter(plan.endDate)

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
        if (plan.userId != currentUid || analysisJobs.containsKey(plan.id)) return

        val job = accountTasks.launch launch@ { currentUid ->
            _plans.value = _plans.value.map {
                if (it.id == plan.id) it.copy(isAnalyzing = true) else it
            }

            try {
                val updatedPlan = performAnalysis(plan)
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                val isNewAnalysis = updatedPlan.analyses.size > plan.analyses.size ||
                                   (plan.analyses.isEmpty() && updatedPlan.analyses.isNotEmpty())

                if (updatedPlan.weatherAnalysisStatus == TravelWeatherAnalysisStatus.WEATHER_READY_ANALYSIS_READY && isNewAnalysis) {
                    val entity = updatedPlan.toEntity()
                    dao.saveTripAnalysisIfVersion(entity, plan.updatedAt) ?: return@launch
                    if (currentUid != "legacy") {
                        try {
                            uploadTrip(entity, currentUid)
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                                Log.e(TAG, "Analysis Firestore sync failed", e)
}
                        }
                    }
                    emitAccountMessage(currentUid, "Seyahat önerileri güncellendi.")
                } else if (updatedPlan.weatherAnalysisStatus == TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW) {
                     _plans.value = _plans.value.map {
                        if (it.id == plan.id) updatedPlan else it
                    }
                } else {
                    _plans.value = _plans.value.map {
                        if (it.id == plan.id) it.copy(isAnalyzing = false) else it
                    }
                    emitAccountMessage(currentUid, "Öneriler şu anda hazırlanamadı. Biraz sonra tekrar deneyebilirsiniz.")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Analysis failed", e)
}
                _plans.value = _plans.value.map {
                    if (it.id == plan.id) it.copy(isAnalyzing = false) else it
                }
                emitAccountMessage(currentUid, "Öneriler şu anda hazırlanamadı. Biraz sonra tekrar deneyebilirsiniz.")
            } finally {
                analysisJobs.remove(plan.id, kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job])
            }
        }
        analysisJobs[plan.id] = job
    }

    suspend fun performAnalysis(plan: TravelPlan): TravelPlan {
        return TravelAnalysisEngine.performAnalysis(
            context = getApplication(),
            plan = plan,
            currentUid = plan.userId,
            apiService = apiService,
            repository = repository
        )
    }

    fun savePlan(plan: TravelPlan) {
        val cityNameTrimmed = plan.city.trim()
        if (cityNameTrimmed.isEmpty()) return

        accountTasks.launch(kotlinx.coroutines.Dispatchers.IO) launch@ { currentUid ->
            if (plan.userId != "legacy" && plan.userId != currentUid) return@launch
            tripLocationError(plan)?.let {
                emitAccountMessage(currentUid, it)
                return@launch
            }
            val now = LocalDateTime.now()

            val depTime = parseDepartureTime(plan.departureTime)
            val planDt = if (depTime != null) {
                plan.startDate.atTime(depTime)
            } else {
                plan.startDate.atTime(0, 0)
            }

            val isValidTime = if (depTime != null) {
                planDt.isAfter(now)
            } else {
                !plan.startDate.isBefore(LocalDate.now())
            }

            if (!isValidTime && !plan.isDemo) {
                emitAccountMessage(currentUid, "PAST_TRIP_ERROR|Geçmiş bir tarih veya saat için seyahat oluşturamazsın.")
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
                emitAccountMessage(currentUid, "DUPLICATE_TRIP|Bu şehir ve tarihler için zaten bir seyahatin bulunuyor.")
                return@launch
            }

            val prepared = if (plan.userId == "legacy" && currentUid != "legacy") {
                plan.copy(userId = currentUid, city = cityNameTrimmed, updatedAt = System.currentTimeMillis())
            } else {
                plan.copy(city = cityNameTrimmed, updatedAt = System.currentTimeMillis())
            }

            val previous = existing.firstOrNull { it.id == prepared.id }?.toDomain()
            val finalPlan = previous?.let { invalidateChangedTripForecast(it, prepared) } ?: prepared
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                analysisJobs.remove(finalPlan.id)?.cancel()
            }
            val entity = finalPlan.toEntity()
            dao.saveLocalTrip(entity)

            if (currentUid != "legacy") {
                try {
                    uploadTrip(entity, currentUid)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Firestore save failed", e)
}
                }
            }


        }
    }

    private suspend fun uploadTrip(entity: TravelPlanEntity, uid: String) {
        val ticket = uploadAttempt.ticket()
        if (!uploadAttempt.begin(ticket)) return
        try {
        TripMutationGate.run {
            check(uid == currentUid && entity.userId == uid) { "Trip owner changed" }
            check(!dao.hasTripDeletion(uid, entity.id)) { "Trip deletion already requested" }
            val latest = dao.getTravelPlanByIdRaw(entity.id) ?: return@run
            check(latest.userId == uid)
            val revision = dao.pendingTripUploadVersion(uid, entity.id) ?: return@run
            if (latest.updatedAt != revision) return@run
            kotlinx.coroutines.withTimeout(30_000) {
                db.collection("users").document(uid).collection("trips")
                    .document(entity.id).set(latest).await()
                dao.acknowledgeTripUpload(uid, entity.id, revision)
            }
        }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException && e !is kotlinx.coroutines.TimeoutCancellationException) throw e
            uploadAttempt.fail(ticket, "Seyahat değişikliği eşitlenemedi. Yerel kayıt korunuyor.")
            throw e
        } finally {
            uploadAttempt.finish(ticket)
        }
    }

    fun deletePlan(id: String) = deletePlanForOwner(id, currentUid)

    private fun deletePlanForOwner(id: String, owner: String): kotlinx.coroutines.Job {
        val ticket = deletionAttempt.ticket()
        return accountTasks.launch(kotlinx.coroutines.Dispatchers.IO) launch@ { currentUid ->
            if (currentUid != owner) return@launch
            deletionAttempt.run(ticket) {
                try {
                    TripMutationGate.run {
                    dao.enqueueTripDeletion(TripDeletionRequest(currentUid, id))
                    deleteTripWithAcknowledgement(
                        remoteDelete = {
                            if (currentUid != "legacy") {
                                db.collection("users").document(currentUid).collection("trips")
                                    .document(id).delete().await()
                            }
                        },
                        localDelete = { dao.completeTripDeletion(currentUid, id) }
                    )
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException && e !is kotlinx.coroutines.TimeoutCancellationException) throw e
                    if (BuildConfig.DEBUG) Log.e(TAG, "Trip deletion failed", e)
                    deletionAttempt.fail(ticket, "Silme tamamlanamadı. Yeniden deneyebilirsin.")
                }
            }
        }
    }

    private var deletionRetryJob: kotlinx.coroutines.Job? = null

    @Synchronized fun retryPendingTripDeletions() {
        if (deletionRetryJob?.isActive == true) return
        val ticket = deletionAttempt.ticket()
        deletionRetryJob = accountTasks.launch(kotlinx.coroutines.Dispatchers.IO) { uid ->
            if (!deletionAttempt.begin(ticket)) return@launch
            try {
                dao.pendingTripDeletions(uid).forEach { request -> deletePlanForOwner(request.recordId, uid).join() }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                deletionAttempt.fail(ticket, "Silme kuyruğu okunamadı. Yeniden deneyebilirsin.")
            } finally {
                deletionAttempt.finish(ticket)
            }
        }
    }

    @Synchronized fun retryPendingTripUploads() {
        if (uploadRetryJob?.isActive == true) return
        val ticket = uploadAttempt.ticket()
        uploadRetryJob = accountTasks.launch(kotlinx.coroutines.Dispatchers.IO) { uid ->
            if (!uploadAttempt.begin(ticket)) return@launch
            try {
                dao.pendingTripUploads(uid).forEach { request ->
                    if (!dao.hasTripDeletion(uid, request.recordId)) {
                        val entity = dao.getTravelPlanByIdRaw(request.recordId)
                        if (entity != null && entity.userId == uid) uploadTrip(entity, uid)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException && e !is kotlinx.coroutines.TimeoutCancellationException) throw e
                uploadAttempt.fail(ticket, "Seyahatler eşitlenemedi. Yeniden deneyebilirsin.")
            } finally {
                uploadAttempt.finish(ticket)
            }
        }
    }

    fun clearAllPlans() {
        accountTasks.launch(kotlinx.coroutines.Dispatchers.IO) launch@ { currentUid ->
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
        _citySearchStatus.value = if (query.trim().length < 2) LocationSearchStatus.IDLE else LocationSearchStatus.LOADING
        _citySuggestions.value = emptyList()
        if (query.trim().length < 2) return
        citySearchJob = accountTasks.launch launch@ { currentUid ->
            kotlinx.coroutines.delay(300)
            val result = fetchLocationSuggestions { repository.searchCityOrThrow(query.trim()) }
            publishAccountResult(currentUid, { this@TravelViewModel.currentUid }) {
                _citySuggestions.value = result.items
                _citySearchStatus.value = result.status
            }
        }
    }

    fun searchOrigin(query: String) {
        originSearchJob?.cancel()
        _originSearchStatus.value = if (query.trim().length < 2) LocationSearchStatus.IDLE else LocationSearchStatus.LOADING
        _originSuggestions.value = emptyList()
        if (query.trim().length < 2) return
        originSearchJob = accountTasks.launch launch@ { currentUid ->
            kotlinx.coroutines.delay(300)
            val result = fetchLocationSuggestions { repository.searchCityOrThrow(query.trim()) }
            publishAccountResult(currentUid, { this@TravelViewModel.currentUid }) {
                _originSuggestions.value = result.items
                _originSearchStatus.value = result.status
            }
        }
    }

    fun archiveTrip(id: String) {
        accountTasks.launch(kotlinx.coroutines.Dispatchers.IO) launch@ { currentUid ->
            val plan = _plans.value.find { it.id == id && it.userId == currentUid } ?: return@launch
            val updated = plan.copy(isArchived = true, archivedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
            val entity = updated.toEntity()

            dao.saveLocalTrip(entity)
            if (currentUid != "legacy") {
                try {
                    uploadTrip(entity, currentUid)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Archive Firestore sync failed", e)
}
                }
            }
        }
    }

    fun unarchiveTrip(id: String) {
        accountTasks.launch(kotlinx.coroutines.Dispatchers.IO) launch@ { currentUid ->
            val plan = _plans.value.find { it.id == id && it.userId == currentUid } ?: return@launch
            val updated = plan.copy(isArchived = false, archivedAt = null, updatedAt = System.currentTimeMillis())
            val entity = updated.toEntity()

            dao.saveLocalTrip(entity)
            if (currentUid != "legacy") {
                try {
                    uploadTrip(entity, currentUid)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Unarchive Firestore sync failed", e)
}
                }
            }
        }
    }

    fun updateTripNoteAndRating(id: String, note: String, rating: Int) {
        accountTasks.launch(kotlinx.coroutines.Dispatchers.IO) launch@ { currentUid ->
            val plan = _plans.value.find { it.id == id && it.userId == currentUid } ?: return@launch
            val updated = plan.copy(userNote = note, userRating = rating, updatedAt = System.currentTimeMillis())
            val entity = updated.toEntity()

            dao.saveLocalTrip(entity)
            if (currentUid != "legacy") {
                try {
                    uploadTrip(entity, currentUid)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Note/Rating Firestore sync failed", e)
}
                }
            }
        }
    }

    fun migrateLegacyDataToUser() {
        if (currentUid == "legacy") return
        accountTasks.launch(kotlinx.coroutines.Dispatchers.IO) launch@ { currentUid ->
            val legacyPlans = dao.getAllTravelPlans("legacy")
            if (legacyPlans.isNotEmpty()) {
                legacyPlans.forEach { entity ->
                    val newEntity = entity.copy(id = UUID.randomUUID().toString(), userId = currentUid)
                    dao.saveLocalTrip(newEntity)
                    try {
                        uploadTrip(newEntity, currentUid)
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                            Log.e(TAG, "Migration sync failed for trip ${newEntity.id}", e)
}
                    }
                }
            }
            ThemeManager.saveMigrationChoiceMade(getApplication(), currentUid, true)
            loadPlans()
        }
    }

    fun declineMigration() {
        if (currentUid == "legacy") return
        accountTasks.launch launch@ { currentUid ->
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
