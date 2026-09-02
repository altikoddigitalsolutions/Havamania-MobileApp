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

        historyListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(uid).collection("ai_history")
            .addSnapshotListener { snapshot, e ->
                if (e == null && snapshot != null) {
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val items = snapshot.documents.mapNotNull { doc ->
                            try { doc.toObject(AiHistoryEntity::class.java) } catch (ex: Exception) { null }
                        }
                        items.forEach { dao.insertAiHistory(it) }
                        loadHistory()
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
            val existing = dao.getAiHistoryItem(finalId)

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
                    android.util.Log.e("AiHistoryVM", "Firestore save failed", e)
                }
            }
            loadHistoryForUid(uid)
        }
    }

    fun deleteItem(id: String) {
        val uid = currentUid
        viewModelScope.launch {
            dao.deleteAiHistory(id)
            if (uid != "legacy") {
                try {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(uid).collection("ai_history")
                        .document(id).delete()
                } catch (e: Exception) {
                    android.util.Log.e("AiHistoryVM", "Firestore delete failed", e)
                }
            }
            loadHistoryForUid(uid)
        }
    }

    fun clearAll() {
        val uid = currentUid
        viewModelScope.launch {
            dao.clearAllAiHistory(uid)
            _historyItems.value = emptyList()
        }
    }
}
