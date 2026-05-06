package com.havamania

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AiHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = WeatherDatabase.getDatabase(application).weatherDao()

    private val _historyItems = MutableStateFlow<List<AiHistoryEntity>>(emptyList())
    val historyItems: StateFlow<List<AiHistoryEntity>> = _historyItems.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _historyItems.value = dao.getAllAiHistory()
        }
    }

    fun addHistoryItem(title: String, summary: String, messages: List<AltikodChatMessage>, cityName: String?) {
        viewModelScope.launch {
            val item = AiHistoryEntity(
                id = java.util.UUID.randomUUID().toString(),
                title = title,
                summary = summary,
                messages = messages,
                cityName = cityName
            )
            dao.insertAiHistory(item)
            loadHistory()
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            dao.deleteAiHistory(id)
            loadHistory()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            dao.clearAllAiHistory()
            _historyItems.value = emptyList()
        }
    }
}
