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
    private val dao = WeatherDatabase.getDatabase(application).weatherDao()

    private val _historyItems = MutableStateFlow<List<AiHistoryEntity>>(emptyList())
    val historyItems: StateFlow<List<AiHistoryEntity>> = _historyItems.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val newUid = firebaseAuth.currentUser?.uid ?: "legacy"
        _historyItems.value = emptyList()
        loadHistoryForUid(newUid)
    }

    private var historyListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var clearEpoch = 0

    init {
        auth.addAuthStateListener(authListener)
        observeFirestoreHistory(currentUid)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        historyListener?.remove()
    }

    private fun observeFirestoreHistory(uid: String) {
        historyListener?.remove()
        if (uid == "legacy") return

        val observationEpoch = clearEpoch
        historyListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(uid).collection("ai_history")
            .addSnapshotListener { snapshot, e ->
                if (e == null && snapshot != null) {
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        if (observationEpoch != clearEpoch) return@launch
                        val items = snapshot.documents.mapNotNull { doc ->
                            try { doc.toObject(AiHistoryEntity::class.java) } catch (ex: Exception) { null }
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
        viewModelScope.launch {
            _historyItems.value = dao.getAllAiHistory(uid)
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
        viewModelScope.launch {
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
        viewModelScope.launch {
            dao.deleteAiHistory(id, uid)
            if (uid != "legacy") {
                try {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(uid).collection("ai_history")
                        .document(id).delete()
                } catch (e: Exception) {
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
        viewModelScope.launch {
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
