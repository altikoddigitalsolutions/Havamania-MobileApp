package com.havamania

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
        val dao = NotificationDatabase.getDatabase(application).notificationDao()
        repository = NotificationRepository(dao)

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

        // Seed mock data if empty
        viewModelScope.launch {
            repository.allNotifications.first().let {
                if (it.isEmpty()) seedMockData()
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
                // Automatically exit selection mode if no notifications remain
                isSelectionMode = current.isSelectionMode && filtered.isNotEmpty()
            )
        }
    }

    private suspend fun seedMockData() {
        try {
            val mocks = listOf(
                NotificationItem(
                    title = "Fırtına Uyarısı ⚡",
                    message = "Yarın öğleden sonra İstanbul için şiddetli rüzgar ve fırtına bekleniyor.",
                    category = NotificationCategory.WARNING
                ),
                NotificationItem(
                    title = "Seyahat Hatırlatıcı",
                    message = "Ankara seyahatinize 2 gün kaldı!",
                    category = NotificationCategory.TRAVEL
                ),
                NotificationItem(
                    title = "UV Uyarısı",
                    message = "Bugün UV endeksi çok yüksek. Güneş kremi kullanın.",
                    category = NotificationCategory.UV
                )
            )
            mocks.forEach { repository.insert(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding", e)
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
                selectedIds = emptySet(),
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
            try {
                repository.delete(listOf(id))
                _uiState.update { current ->
                    current.copy(selectedIds = current.selectedIds - id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Delete failed", e)
            }
        }
    }

    fun deleteSelected() {
        val idsToDelete = _uiState.value.selectedIds.toList()
        if (idsToDelete.isEmpty()) return

        viewModelScope.launch {
            try {
                repository.delete(idsToDelete)
                _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
            } catch (e: Exception) {
                Log.e(TAG, "Bulk delete failed", e)
            }
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            repository.markAsRead(listOf(id))
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
}
