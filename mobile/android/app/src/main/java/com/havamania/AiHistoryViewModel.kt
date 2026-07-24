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

    init {
        viewModelScope.launch {
            auth.addAuthStateListener { firebaseAuth ->
                val newUid = firebaseAuth.currentUser?.uid ?: "legacy"
                _historyItems.value = emptyList()
                loadHistoryForUid(newUid)
            }
        }
    }

    fun loadHistoryForUid(uid: String) {
        viewModelScope.launch {
            _historyItems.value = dao.getAllAiHistory(uid)
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
            loadHistoryForUid(uid)
        }
    }

    fun deleteItem(id: String) {
        val uid = currentUid
        viewModelScope.launch {
            dao.deleteAiHistory(id)
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
