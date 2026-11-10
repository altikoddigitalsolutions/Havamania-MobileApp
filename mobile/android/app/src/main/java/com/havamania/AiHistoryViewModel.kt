package com.havamania

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext
import java.util.UUID
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

internal enum class AiHistoryListStatus { LOADING, READY, FAILED }

class AiHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val currentUid: String get() = auth.currentUser?.uid ?: "legacy"
    private val accountTasks = com.havamania.AccountTaskScope(viewModelScope) { currentUid }
    private val dao = WeatherDatabase.getDatabase(application).weatherDao()
    private val detailLoader = AiHistoryDetailLoader(viewModelScope) { currentUid }
    internal val detailState = detailLoader.state
    private val _detailOwner = MutableStateFlow(currentUid)
    internal val detailOwner = _detailOwner.asStateFlow()

    fun loadDetail(id: String) {
        detailLoader.load { owner ->
            readVisibleHistoryDetail(isDeleted = { dao.hasHistoryDeletion(owner, id) }) {
                dao.getAiHistoryItem(id, owner) ?: if (owner == "legacy") null else {
                    val document = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(owner).collection("ai_history").document(id)
                        .get(com.google.firebase.firestore.Source.SERVER).await()
                    if (!document.exists()) null else document.toObject(AiHistoryEntity::class.java)
                        ?.copy(id = document.id, userId = owner)
                        ?: error("History document could not be decoded")
                }
            }
        }
    }

    private val _confirmedEmptyOwner = MutableStateFlow<String?>(null)
    val confirmedEmptyOwner = _confirmedEmptyOwner.asStateFlow()

    private val _historyItems = MutableStateFlow<List<AiHistorySummary>>(emptyList())
    val historyItems: StateFlow<List<AiHistorySummary>> = _historyItems.asStateFlow()
    private val _listStatus = MutableStateFlow(AiHistoryListStatus.LOADING)
    internal val listStatus = _listStatus.asStateFlow()
    private val _syncFailed = MutableStateFlow(false)
    internal val syncFailed = _syncFailed.asStateFlow()

    fun retryHistorySync() {
        val owner = currentUid
        if (owner != "legacy" && _syncFailed.value && !_deleting.value) observeFirestoreHistory(owner)
    }

    private val saveController = HistorySaveController(viewModelScope) { currentUid }
    internal val saveState = saveController.state
    fun resetSaveState() = saveController.reset()

    private val historyMutex = Mutex()
    private val mutationMutex = HistoryOperationLock.mutex
    private val _deleting = MutableStateFlow(false)
    val deleting = _deleting.asStateFlow()
    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError = _deleteError.asStateFlow()
    private var retryDelete: (() -> Unit)? = null
    private val deleteRunner = AssistantTaskRunner(viewModelScope, { currentUid }, { _deleting.value = it }, 30_000)
    fun retryDeletion() { retryDelete?.invoke() }

    private val _uploading = MutableStateFlow(false)
    val uploading = _uploading.asStateFlow()
    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError = _uploadError.asStateFlow()
    private val uploadRunner = AssistantTaskRunner(viewModelScope, { currentUid }, { _uploading.value = it }, 60_000)

    fun retryPendingUploads() {
        if (_deleting.value) return
        uploadRunner.start(
            onStart = { _uploadError.value = null },
            operation = { uid ->
                mutationMutex.withLock {
                    for (request in dao.getHistoryDeletions(uid)) {
                        coroutineContext.ensureActive()
                        finishHistoryDeletion(uid, request.recordId)
                        // A confirmed full-history deletion also acknowledges the individual requests.
                        if (request.recordId.isEmpty()) break
                    }
                    for (id in dao.getPendingAiHistoryIds(uid)) {
                        coroutineContext.ensureActive()
                        if (_deleting.value || uid != currentUid) break
                        // Re-read under the operation lock; a cancelled deletion must not be resurrected.
                        val pending = dao.getAiHistoryItem(id, uid) ?: continue
                        if (!pending.pendingUpload || uid == "legacy") continue
                        uploadHistory(pending)
                    }
                }
                loadHistoryForUid(uid)
            },
            onSuccess = {},
            onError = {
                _uploadError.value = "Bekleyen kayıt veya silme işlemleri tamamlanamadı. Tekrar deneyebilirsin."
                loadHistory()
            }
        )
    }

    private suspend fun ensureHistoryOwner(uid: String) {
        coroutineContext.ensureActive()
        check(uid == currentUid)
    }

    private suspend fun uploadHistory(item: AiHistoryEntity) {
        ensureHistoryOwner(item.userId)
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(item.userId).collection("ai_history")
            .document(item.id).set(item).await()
        ensureHistoryOwner(item.userId)
        dao.acknowledgeAiHistory(item.id, item.userId, item.updatedAt)
    }

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val newUid = firebaseAuth.currentUser?.uid ?: "legacy"
        accountTasks.reset()
        // Firebase also invokes this listener for the initial, unchanged session.
        // Cancelling that first read would leave Loading without a new effect key.
        if (_detailOwner.value != newUid) detailLoader.reset()
        uploadRunner.reset()
        _uploadError.value = null
        deleteRunner.reset()
        saveController.reset()
        retryDelete = null
        _deleteError.value = null
        _detailOwner.value = newUid
        clearEpoch++
        _confirmedEmptyOwner.value = null
        _syncFailed.value = false
        _listStatus.value = AiHistoryListStatus.LOADING
        _historyItems.value = emptyList()
        observeFirestoreHistory(newUid)
        loadHistoryForUid(newUid)
        retryPendingUploads()
    }

    private var historyListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var historyObservation: Any? = null
    private var historyReadJob: Job? = null
    @Volatile private var clearEpoch = 0

    init {
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        historyListener?.remove()
        historyObservation = null
        detailLoader.reset()
        uploadRunner.reset()
        _uploadError.value = null
        deleteRunner.reset()
        saveController.reset()
        retryDelete = null
        _deleteError.value = null
    }

    private fun observeFirestoreHistory(uid: String) {
        if (uid != currentUid) return
        historyListener?.remove()
        historyListener = null
        historyObservation = null
        if (uid == "legacy") return

        _syncFailed.value = false
        val observationEpoch = clearEpoch
        val observation = Any()
        historyObservation = observation
        fun isCurrentObservation() =
            historyObservation === observation && uid == currentUid && observationEpoch == clearEpoch
        fun reportSyncFailure() {
            if (!isCurrentObservation()) return
            historyObservation = null
            historyListener?.remove()
            historyListener = null
            _confirmedEmptyOwner.value = null
            _syncFailed.value = true
        }
        historyListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(uid).collection("ai_history")
            .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, e ->
                if (!isCurrentObservation()) return@addSnapshotListener
                if (e != null) {
                    reportSyncFailure()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    accountTasks.launch launch@ {
                        if (!isCurrentObservation()) return@launch
                        historyMutex.withLock {
                            coroutineContext.ensureActive()
                            if (!isCurrentObservation()) return@launch
                            // Cache/latency-compensated events cannot establish server absence.
                            if (!snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()) {
                                try {
                                    val documents = snapshot.documents
                                    val items = documents.map { doc ->
                                        checkNotNull(doc.toObject(AiHistoryEntity::class.java)) {
                                            "History document could not be decoded"
                                        }.copy(id = doc.id, userId = uid)
                                    }
                                    dao.reconcileAiHistory(uid, items, documents.map { it.id }.toSet())
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    coroutineContext.ensureActive()
                                    reportSyncFailure()
                                    return@launch
                                }
                                coroutineContext.ensureActive()
                                if (!isCurrentObservation()) return@launch
                                _confirmedEmptyOwner.value = if (snapshot.isEmpty) uid else null
                            }

                        }
                        if (isCurrentObservation()) loadHistoryForUid(uid)
                    }
                }
            }
    }

    fun loadHistoryForUid(uid: String) {
        if (uid != currentUid) return
        val epoch = clearEpoch
        historyReadJob?.cancel()
        historyReadJob = accountTasks.launch launch@ {
            if (epoch != clearEpoch) return@launch
            if (_listStatus.value == AiHistoryListStatus.FAILED) _listStatus.value = AiHistoryListStatus.LOADING
            val items = try {
                dao.getVisibleAiHistory(uid)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                coroutineContext.ensureActive()
                if (uid == currentUid && epoch == clearEpoch) {
                    _listStatus.value = AiHistoryListStatus.FAILED
                }
                return@launch
            }
            coroutineContext.ensureActive()
            if (uid != currentUid || epoch != clearEpoch) return@launch
            _historyItems.value = items
            _listStatus.value = AiHistoryListStatus.READY
            if (uid != "legacy" && historyListener == null && !_deleting.value && !_syncFailed.value) {
                observeFirestoreHistory(uid)
            }
        }
    }

    fun loadHistory() {
        loadHistoryForUid(currentUid)
    }

    fun addHistoryItem(
        id: String? = null,
        title: String,
        summary: String,
        messages: List<AltikodChatMessage>,
        cityName: String?
    ) {
        saveController.save(id) { uid, finalId ->
            mutationMutex.withLock {
                ensureHistoryOwner(uid)
                val now = System.currentTimeMillis()
                val item = dao.saveAiHistoryRevision(
                    AiHistoryEntity(
                        id = finalId, userId = uid, title = title, summary = summary,
                        messages = messages, cityName = cityName, timestamp = now, updatedAt = now,
                        pendingUpload = uid != "legacy"
                    )
                )
                loadHistoryForUid(uid)
                if (uid != "legacy") {
                    uploadHistory(item)
                }
                coroutineContext.ensureActive()
                loadHistoryForUid(uid)
            }
        }
    }

    private suspend fun finishHistoryDeletion(uid: String, recordId: String) {
        deleteHistoryWithAcknowledgement(
            remoteDelete = {
                ensureHistoryOwner(uid)
                if (uid != "legacy") {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val collection = db.collection("users").document(uid).collection("ai_history")
                    if (recordId.isNotEmpty()) collection.document(recordId).delete().await()
                    else {
                        val snapshot = collection.get(com.google.firebase.firestore.Source.SERVER).await()
                        for (chunk in snapshot.documents.chunked(400)) {
                            ensureHistoryOwner(uid)
                            dao.markRemoteHistoryForDeletion(uid, chunk.map { it.id })
                            ensureHistoryOwner(uid)
                            val batch = db.batch()
                            chunk.forEach { batch.delete(it.reference) }
                            batch.commit().await()
                        }
                    }
                }
            },
            localDelete = {
                historyMutex.withLock {
                    ensureHistoryOwner(uid)
                    dao.completeHistoryDeletion(uid, recordId)
                }
            }
        )
    }

    fun deleteItem(id: String) = deleteHistory(id)

    fun clearAll() = deleteHistory(null)

    private fun deleteHistory(id: String?, enqueue: Boolean = true) {
        val recordId = id.orEmpty()
        var needsEnqueue = enqueue
        deleteRunner.start(
            onStart = {
                _deleteError.value = null
                retryDelete = { deleteHistory(id, enqueue = needsEnqueue) }
                clearEpoch++
                historyListener?.remove()
                historyListener = null
            },
            operation = { uid ->
                val operationEpoch = clearEpoch
                try {
                    mutationMutex.withLock {
                        if (needsEnqueue) {
                            dao.enqueueHistoryDeletion(uid, id)
                            needsEnqueue = false
                        }
                        // Another screen can finish a queued deletion while this error is still visible.
                        dao.finishQueuedHistoryDeletion(uid, recordId) {
                            loadHistoryForUid(uid)
                            finishHistoryDeletion(uid, recordId)
                        }
                        coroutineContext.ensureActive()
                        loadHistoryForUid(uid)
                    }
                } finally {
                    if (uid == currentUid && operationEpoch == clearEpoch) observeFirestoreHistory(uid)
                }
            },
            onSuccess = { retryDelete = null },
            onError = {
                _deleteError.value = "Silme işleminin tamamlandığı doğrulanamadı. Bağlantını kontrol edip tekrar deneyebilirsin."
            }
        )
    }
}
