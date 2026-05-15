package com.havamania

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    val notifications: StateFlow<List<AppNotification>> = NotificationRepository.notifications

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val actionMutex = Mutex()
    private var lastActionTime = 0L
    private val DEBOUNCE_TIME = 500L

    private fun checkDebounce(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < DEBOUNCE_TIME) return false
        lastActionTime = now
        return true
    }

    fun toggleSelection(id: String) {
        viewModelScope.launch {
            actionMutex.withLock {
                val current = _selectedIds.value
                if (current.contains(id)) {
                    _selectedIds.value = current - id
                    if (_selectedIds.value.isEmpty()) {
                        _isSelectionMode.value = false
                    }
                } else {
                    _selectedIds.value = current + id
                    _isSelectionMode.value = true
                }
            }
        }
    }

    fun enterSelectionMode(firstId: String) {
        viewModelScope.launch {
            actionMutex.withLock {
                _isSelectionMode.value = true
                _selectedIds.value = setOf(firstId)
            }
        }
    }

    fun exitSelectionMode() {
        viewModelScope.launch {
            actionMutex.withLock {
                _isSelectionMode.value = false
                _selectedIds.value = emptySet()
            }
        }
    }

    fun selectAll() {
        viewModelScope.launch {
            actionMutex.withLock {
                _selectedIds.value = notifications.value.map { it.id }.toSet()
            }
        }
    }

    fun markAsRead(id: String) {
        if (!checkDebounce()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                NotificationRepository.markAsRead(id)
            } catch (e: Exception) {
                Log.e("NotificationVM", "Error in markAsRead", e)
            }
        }
    }

    fun toggleReadStatus(id: String) {
        if (!checkDebounce()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                NotificationRepository.toggleReadStatus(id)
            } catch (e: Exception) {
                Log.e("NotificationVM", "Error in toggleReadStatus", e)
            }
        }
    }

    fun markSelectedAsRead() {
        val ids = _selectedIds.value
        viewModelScope.launch(Dispatchers.IO) {
            try {
                NotificationRepository.markMultipleAsRead(ids)
                launch(Dispatchers.Main) { exitSelectionMode() }
            } catch (e: Exception) {
                Log.e("NotificationVM", "Error in markSelectedAsRead", e)
            }
        }
    }

    fun deleteSelected() {
        val ids = _selectedIds.value
        viewModelScope.launch(Dispatchers.IO) {
            try {
                NotificationRepository.deleteMultiple(ids)
                launch(Dispatchers.Main) { exitSelectionMode() }
            } catch (e: Exception) {
                Log.e("NotificationVM", "Error in deleteSelected", e)
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                NotificationRepository.markAllAsRead()
            } catch (e: Exception) {
                Log.e("NotificationVM", "Error in markAllAsRead", e)
            }
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                NotificationRepository.deleteNotification(id)
                if (_selectedIds.value.contains(id)) {
                    launch(Dispatchers.Main) { toggleSelection(id) }
                }
            } catch (e: Exception) {
                Log.e("NotificationVM", "Error in deleteNotification", e)
            }
        }
    }

    fun undoDelete() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                NotificationRepository.undoDelete()
            } catch (e: Exception) {
                Log.e("NotificationVM", "Error in undoDelete", e)
            }
        }
    }
}
