package com.havamania

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NotificationUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val filteredNotifications: List<NotificationItem> = emptyList(),
    val unreadCount: Int = 0,
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val activeFilter: NotificationCategory? = null,
    val isLoading: Boolean = false
)

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "NotificationVM"
    private val repository: NotificationRepository

    private val _uiState = MutableStateFlow(NotificationUiState(isLoading = true))
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        val database = NotificationDatabase.getDatabase(application)
        repository = NotificationRepository(database.notificationDao())

        // Observe all notifications from Room
        viewModelScope.launch {
            repository.allNotifications
                .catch { e ->
                    Log.e(TAG, "Failed to fetch notifications", e)
                    emit(emptyList())
                }
                .collect { list ->
                    updateStateWithList(list)
                }
        }
    }

    private fun updateStateWithList(list: List<NotificationItem>) {
        _uiState.update { current ->
            val filtered = list.filter { item ->
                current.activeFilter == null || item.category == current.activeFilter
            }
            current.copy(
                notifications = list,
                filteredNotifications = filtered,
                unreadCount = list.count { !it.isRead },
                isLoading = false,
                isSelectionMode = current.isSelectionMode && filtered.isNotEmpty()
            )
        }
    }

    fun setFilter(category: NotificationCategory?) {
        _uiState.update { current ->
            val filtered = current.notifications.filter { item ->
                category == null || item.category == category
            }
            current.copy(
                activeFilter = category,
                filteredNotifications = filtered,
                selectedIds = if (current.isSelectionMode) emptySet() else current.selectedIds,
                isSelectionMode = false
            )
        }
    }

    fun toggleSelection(id: String) {
        _uiState.update { current ->
            val newSelected = if (current.selectedIds.contains(id)) {
                current.selectedIds - id
            } else {
                current.selectedIds + id
            }
            current.copy(
                selectedIds = newSelected,
                isSelectionMode = newSelected.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        _uiState.update { current ->
            val allIds = current.filteredNotifications.map { it.id }.toSet()
            val newSelected = if (current.selectedIds.size == allIds.size) emptySet() else allIds
            current.copy(
                selectedIds = newSelected,
                isSelectionMode = newSelected.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.delete(id)
            _uiState.update { it.copy(selectedIds = it.selectedIds - id) }
        }
    }

    fun deleteSelected() {
        val idsToDelete = _uiState.value.selectedIds.toList()
        if (idsToDelete.isEmpty()) return

        viewModelScope.launch {
            repository.delete(idsToDelete)
            clearSelection()
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            repository.markAsRead(id)
        }
    }

    fun toggleReadStatus(id: String) {
        val item = _uiState.value.notifications.find { it.id == id } ?: return
        viewModelScope.launch {
            repository.toggleReadStatus(id, item.isRead)
        }
    }

    fun markSelectedAsRead() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return

        viewModelScope.launch {
            repository.markAsRead(ids)
            clearSelection()
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun deleteAllNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteAll()
            } catch (e: Exception) {
                Log.e("NotificationVM", "Error in deleteAllNotifications", e)
            }
        }
    }

    fun refreshDemoNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.refreshDemoNotifications()
            } catch (e: Exception) {
                Log.e("NotificationVM", "Error in refreshDemoNotifications", e)
            }
        }
    }
}
