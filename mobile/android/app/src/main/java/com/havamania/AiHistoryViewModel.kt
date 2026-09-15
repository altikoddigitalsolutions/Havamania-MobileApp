package com.havamania

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AiHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val currentUid: String get() = auth.currentUser?.uid ?: "legacy"
    private val accountTasks = com.havamania.AccountTaskScope(viewModelScope) { currentUid }
    private val dao = WeatherDatabase.getDatabase(application).weatherDao()

    private val _historyItems = MutableStateFlow<List<AiHistoryEntity>>(emptyList())
    val historyItems: StateFlow<List<AiHistoryEntity>> = _historyItems.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        accountTasks.reset()
        val newUid = firebaseAuth.currentUser?.uid ?: "legacy"
        clearEpoch++
        _historyItems.value = emptyList()
        observeFirestoreHistory(newUid)
        loadHistoryForUid(newUid)
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
    }

    private fun observeFirestoreHistory(uid: String) {
        if (uid != currentUid) return
        historyListener?.remove()
        historyListener = null
        if (uid == "legacy") return

        val observationEpoch = clearEpoch
        historyListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(uid).collection("ai_history")
            .addSnapshotListener { snapshot, e ->
                if (uid != currentUid || observationEpoch != clearEpoch) return@addSnapshotListener
                if (e == null && snapshot != null) {
                    accountTasks.launch(kotlinx.coroutines.Dispatchers.IO) launch@ { currentUid ->
                        if (observationEpoch != clearEpoch) return@launch
                        val items = snapshot.documents.mapNotNull { doc ->
                            try { doc.toObject(AiHistoryEntity::class.java)?.copy(id = doc.id, userId = uid) } catch (ex: Exception) {
                                if (ex is kotlinx.coroutines.CancellationException) throw ex
                                null
                            }
                        }
                        if (observationEpoch != clearEpoch) return@launch
                        items.forEach {
                            if (observationEpoch == clearEpoch) {
                                dao.insertAiHistory(it)
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
            if (uid != "legacy" && historyListener == null) {
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
        val uid = currentUid
        accountTasks.launch launch@ { currentUid ->
            val finalId = id ?: java.util.UUID.randomUUID().toString()

            // Fetch existing to preserve timestamp if updating
            val existing = dao.getAiHistoryItem(finalId, uid)

            val item = AiHistoryEntity(
                id = finalId,
                userId = uid,
                title = title,
                summary = summary,
                messages = messages,
                cityName = cityName,
                timestamp = existing?.timestamp ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            dao.insertAiHistory(item)

            if (uid != "legacy") {
                try {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(uid).collection("ai_history")
                        .document(finalId).set(item)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                        android.util.Log.e("AiHistoryVM", "Firestore save failed", e)
}
                }
            }
            loadHistoryForUid(uid)
        }
    }

    fun deleteItem(id: String) {
        val uid = currentUid
        accountTasks.launch launch@ { currentUid ->
            dao.deleteAiHistory(id, uid)
            if (uid != "legacy") {
                try {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(uid).collection("ai_history")
                        .document(id).delete()
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                        android.util.Log.e("AiHistoryVM", "Firestore delete failed", e)
}
                }
            }
            loadHistoryForUid(uid)
        }
    }

    fun clearAll() {
        val uid = currentUid
        accountTasks.launch launch@ { currentUid ->
            clearEpoch++
            historyListener?.remove()
            historyListener = null

            if (uid != "legacy") {
                try {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val colRef = db.collection("users").document(uid).collection("ai_history")
                    val snapshot = colRef.get().await()
                    if (!snapshot.isEmpty) {
                        for (chunk in snapshot.documents.chunked(400)) {
                            val batch = db.batch()
                            for (doc in chunk) {
                                batch.delete(doc.reference)
                            }
                            batch.commit().await()
                        }
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    if (BuildConfig.DEBUG) {
                        android.util.Log.e("AiHistoryVM", "Firestore clearAll failed", e)
                    }
                    observeFirestoreHistory(uid)
                    return@launch
                }
            }
            dao.clearAllAiHistory(uid)
            _historyItems.value = emptyList()
            observeFirestoreHistory(uid)
        }
    }
}
