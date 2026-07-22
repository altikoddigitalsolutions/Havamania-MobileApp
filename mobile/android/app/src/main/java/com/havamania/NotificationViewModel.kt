package com.havamania

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

data class NotificationUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val filteredNotifications: List<NotificationItem> = emptyList(),
    val unreadCount: Int = 0,
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val activeFilter: NotificationFilter = NotificationFilter.ALL,
    val isLoading: Boolean = false
)

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "NotificationVM"
    private val repository: NotificationRepository
    private var didSeedInThisSession = false
    private val auth = FirebaseAuth.getInstance()
    private val currentUid: String get() = auth.currentUser?.uid ?: "legacy"

    private val _uiState = MutableStateFlow(NotificationUiState(isLoading = true))
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private var collectJob: kotlinx.coroutines.Job? = null

    init {
        val database = NotificationDatabase.getDatabase(application)
        repository = NotificationRepository(database.notificationDao())

        viewModelScope.launch {
            auth.addAuthStateListener { firebaseAuth ->
                val newUid = firebaseAuth.currentUser?.uid ?: "legacy"
                Log.d(TAG, "Auth state changed. Re-initializing notifications for $newUid")
                _uiState.value = NotificationUiState(isLoading = true)
                didSeedInThisSession = false
                startCollectingNotifications(newUid)
            }
        }
    }

    private fun startCollectingNotifications(uid: String) {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            try {
                repository.getAllNotificationsFlow(uid)
                    .catch { e ->
                        Log.e(TAG, "Catch block: Failed to fetch notifications. Emitting empty for $uid.", e)
                        emit(emptyList())
                    }
                    .collect { list ->
                        Log.d("Notifications", "collected size=${list.size} for $uid")

                        // If user is legacy, we might want to seed.
                        // If user is authenticated, we start clean (size 0).
                        if (list.isEmpty() && uid == "legacy") {
                            Log.d("Notifications", "Legacy user list is empty. Forcing DefaultNotifications.")
                            updateStateWithList(DefaultNotifications.create(uid))

                            if (!didSeedInThisSession) {
                                didSeedInThisSession = true
                                withContext(Dispatchers.IO) {
                                    repository.seedInitialDataIfNeeded(getApplication(), uid)
                                }
                            }
                        } else {
                            updateStateWithList(list)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Fatal error in collect chain for $uid", e)
                updateStateWithList(emptyList())
            }
        }
    }

    fun ensureSeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedInitialDataIfNeeded(getApplication(), currentUid)
        }
    }

    private fun updateStateWithList(list: List<NotificationItem>) {
        _uiState.update { current ->
            val filtered = filterNotifications(list, current.activeFilter)
            current.copy(
                notifications = list,
                filteredNotifications = filtered,
                unreadCount = list.count { !it.isRead },
                isLoading = false,
                isSelectionMode = current.isSelectionMode && filtered.isNotEmpty()
            )
        }
    }

    private fun filterNotifications(list: List<NotificationItem>, filter: NotificationFilter): List<NotificationItem> {
        return when (filter) {
            NotificationFilter.ALL -> list
            NotificationFilter.UNREAD -> list.filter { !it.isRead }
            NotificationFilter.TRAVEL -> list.filter { it.category == NotificationCategory.TRAVEL }
            NotificationFilter.RAIN -> list.filter { it.category == NotificationCategory.RAIN }
            NotificationFilter.UV -> list.filter { it.category == NotificationCategory.UV }
            NotificationFilter.WARNING -> list.filter { it.category == NotificationCategory.WARNING }
            NotificationFilter.SUMMARY -> list.filter { it.category == NotificationCategory.SUMMARY }
            NotificationFilter.UPDATE -> list.filter { it.category == NotificationCategory.UPDATE }
            NotificationFilter.GENERAL -> list.filter { it.category == NotificationCategory.GENERAL }
            NotificationFilter.SYSTEM -> list.filter { it.category == NotificationCategory.SYSTEM }
        }
    }

    fun setFilter(filter: NotificationFilter) {
        _uiState.update { current ->
            val filtered = filterNotifications(current.notifications, filter)
            current.copy(
                activeFilter = filter,
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
            repository.markAllAsRead(currentUid)
        }
    }

    fun deleteAllNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uid = currentUid
                repository.deleteAll(uid)
                com.havamania.ui.theme.ThemeManager.saveHasSeededNotifications(getApplication(), true, uid)
            } catch (e: Exception) {
                Log.e("NotificationVM", "Error in deleteAllNotifications", e)
            }
        }
    }

    fun deleteTravelNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteByCategory(currentUid, NotificationCategory.TRAVEL)
            } catch (e: Exception) {
                Log.e("NotificationVM", "Error in deleteTravelNotifications", e)
            }
        }
    }

    fun refreshDemoNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.refreshDemoNotifications(currentUid)
            } catch (e: Exception) {
                Log.e("NotificationVM", "Error in refreshDemoNotifications", e)
            }
        }
    }
}
