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

    private val _confirmedEmptyOwner = MutableStateFlow<String?>(null)
    val confirmedEmptyOwner = _confirmedEmptyOwner.asStateFlow()

    private val _historyItems = MutableStateFlow<List<AiHistoryEntity>>(emptyList())
    val historyItems: StateFlow<List<AiHistoryEntity>> = _historyItems.asStateFlow()

    private val saveController = HistorySaveController(viewModelScope) { currentUid }
    internal val saveState = saveController.state
    fun resetSaveState() = saveController.reset()
    fun cancelSave() = saveController.cancel()

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
                    }
                    for (item in dao.getPendingAiHistory(uid)) {
                        coroutineContext.ensureActive()
                        if (_deleting.value || uid != currentUid) break
                        // Re-read under the operation lock; a cancelled deletion must not be resurrected.
                        val pending = dao.getAiHistoryItem(item.id, uid) ?: continue
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

    private suspend fun uploadHistory(item: AiHistoryEntity) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(item.userId).collection("ai_history")
            .document(item.id).set(item).await()
        coroutineContext.ensureActive()
        check(item.userId == currentUid)
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
        _historyItems.value = emptyList()
        observeFirestoreHistory(newUid)
        loadHistoryForUid(newUid)
        retryPendingUploads()
    }

    private var historyListener: com.google.firebase.firestore.ListenerRegistration? = null
    @Volatile private var clearEpoch = 0

    init {
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        historyListener?.remove()
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
        if (uid == "legacy") return

        val observationEpoch = clearEpoch
        historyListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(uid).collection("ai_history")
            .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, e ->
                if (uid != currentUid || observationEpoch != clearEpoch) return@addSnapshotListener
                if (e == null && snapshot != null) {
                    accountTasks.launch launch@ { currentUid ->
                        if (observationEpoch != clearEpoch) return@launch
                        val items = snapshot.documents.mapNotNull { doc ->
                            try { doc.toObject(AiHistoryEntity::class.java)?.copy(id = doc.id, userId = uid) } catch (ex: Exception) {
                                if (ex is kotlinx.coroutines.CancellationException) throw ex
                                null
                            }
                        }
                        if (observationEpoch != clearEpoch) return@launch
                        historyMutex.withLock {
                            coroutineContext.ensureActive()
                            if (observationEpoch != clearEpoch) return@launch
                            // Cache/latency-compensated events cannot establish server absence.
                            if (!snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()) {
                                dao.reconcileAiHistory(uid, items, snapshot.documents.map { it.id }.toSet())
                                _confirmedEmptyOwner.value = if (snapshot.isEmpty) uid else null
                            }

                        }
                        if (observationEpoch == clearEpoch) {
                            loadHistory()
                        }
                    }
                }
            }
    }

    fun loadHistoryForUid(uid: String) {
        if (uid != currentUid) return
        accountTasks.launch launch@ { currentUid ->
            val epoch = clearEpoch
            val items = dao.getAllAiHistory(uid)
            if (uid != this@AiHistoryViewModel.currentUid || epoch != clearEpoch) return@launch
            _historyItems.value = items
            if (uid != "legacy" && historyListener == null && !_deleting.value) {
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
                coroutineContext.ensureActive()
                val existing = dao.getAiHistoryItem(finalId, uid)
                val item = AiHistoryEntity(
                    id = finalId, userId = uid, title = title, summary = summary,
                    messages = messages, cityName = cityName,
                    timestamp = existing?.timestamp ?: System.currentTimeMillis(),
                    updatedAt = maxOf(System.currentTimeMillis(), (existing?.updatedAt ?: 0) + 1),
                    pendingUpload = uid != "legacy"
                )
                dao.insertAiHistory(item)
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
                if (uid != "legacy") {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val collection = db.collection("users").document(uid).collection("ai_history")
                    if (recordId.isNotEmpty()) collection.document(recordId).delete().await()
                    else {
                        val snapshot = collection.get(com.google.firebase.firestore.Source.SERVER).await()
                        for (document in snapshot.documents) {
                            dao.insertHistoryDeletion(HistoryDeletionRequest(uid, document.id, true))
                        }
                        for (chunk in snapshot.documents.chunked(400)) {
                            val batch = db.batch()
                            chunk.forEach { batch.delete(it.reference) }
                            batch.commit().await()
                        }
                    }
                }
            },
            localDelete = {
                check(uid == currentUid)
                historyMutex.withLock { dao.completeHistoryDeletion(uid, recordId) }
            }
        )
    }

    fun deleteItem(id: String) = deleteHistory(id)

    fun clearAll() = deleteHistory(null)

    private fun deleteHistory(id: String?) {
        deleteRunner.start(
            onStart = {
                _deleteError.value = null
                retryDelete = { deleteHistory(id) }
                clearEpoch++
                historyListener?.remove()
                historyListener = null
            },
            operation = { uid ->
                val operationEpoch = clearEpoch
                try {
                    mutationMutex.withLock {
                    dao.enqueueHistoryDeletion(uid, id)
                    finishHistoryDeletion(uid, id ?: "")
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
