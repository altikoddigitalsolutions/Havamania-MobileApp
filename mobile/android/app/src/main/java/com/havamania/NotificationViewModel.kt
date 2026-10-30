package com.havamania

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
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

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError = _actionError.asStateFlow()
    private val actionTasks = AccountTaskScope(viewModelScope) { currentUid }
    private val actionMutex = kotlinx.coroutines.sync.Mutex()
    fun dismissActionError() { _actionError.value = null }
    fun canOpenNotification(item: NotificationItem): Boolean {
        val allowed = canOpenOwnedNotification(item.userId, auth.currentUser?.uid,
            AccountDeletionManager.getInstance(getApplication()).state.value.pending) &&
            _uiState.value.notifications.any { it.id == item.id && it.userId == item.userId }
        if (!allowed) _actionError.value = "Bu bildirim mevcut hesap için kullanılamıyor."
        return allowed
    }

    private fun notificationAction(onSuccess: () -> Unit = {}, action: suspend (String) -> Unit) {
        actionTasks.launch { uid ->
            try {
                actionMutex.withLock {
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    action(uid)
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    if (uid == currentUid) { _actionError.value = null; onSuccess() }
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                if (uid == currentUid) _actionError.value = "İşlem tamamlanamadı. Lütfen tekrar deneyin."
            }
        }
    }

    private val deleteAllRunner = AssistantTaskRunner(viewModelScope, { currentUid }, {}, 20_000)
    private var collectJob: kotlinx.coroutines.Job? = null

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val newUid = firebaseAuth.currentUser?.uid ?: "legacy"
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Auth state changed. Re-initializing notifications for $newUid")
        }
        actionTasks.reset()
        _actionError.value = null
        deleteAllRunner.reset()
        _uiState.value = NotificationUiState(isLoading = true)
        didSeedInThisSession = false
        startCollectingNotifications(newUid)
    }

    init {
        val database = NotificationDatabase.getDatabase(application)
        repository = NotificationRepository(database.notificationDao())
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        collectJob?.cancel()
        actionTasks.reset()
        _actionError.value = null
        deleteAllRunner.reset()
    }

    private fun startCollectingNotifications(uid: String) {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            try {
                repository.getAllNotificationsFlow(uid)
                    .catch { e ->
if (BuildConfig.DEBUG) {
                            Log.e(TAG, "Catch block: Failed to fetch notifications. Emitting empty for $uid.", e)
}
                        emit(emptyList())
                    }
                    .collect { list ->
                        if (uid != currentUid) return@collect
                        if (BuildConfig.DEBUG) {
                            Log.d("Notifications", "collected size=${list.size} for $uid")
                        }

                        updateStateWithList(list)
                    }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (uid != currentUid) return@launch
if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Fatal error in collect chain for $uid", e)
}
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

    fun deleteNotification(id: String) = notificationAction(
        onSuccess = { _uiState.update { it.copy(selectedIds = it.selectedIds - id) } }
    ) { uid -> repository.delete(uid, listOf(id)) }

    fun deleteSelected() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isNotEmpty()) notificationAction(onSuccess = ::clearSelection) { uid -> repository.delete(uid, ids) }
    }

    fun markAsRead(id: String) = notificationAction { uid -> repository.markAsRead(uid, listOf(id)) }

    fun toggleReadStatus(id: String) {
        val item = _uiState.value.notifications.find { it.id == id } ?: return
        notificationAction { uid -> repository.toggleReadStatus(uid, id, item.isRead) }
    }

    fun markSelectedAsRead() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isNotEmpty()) notificationAction(onSuccess = ::clearSelection) { uid -> repository.markAsRead(uid, ids) }
    }

    fun markAllAsRead() = notificationAction { uid -> repository.markAllAsRead(uid) }

    fun deleteAllNotifications(onResult: (Boolean) -> Unit) {
        deleteAllRunner.start(
            operation = { uid ->
                repository.deleteAll(uid)
                com.havamania.ui.theme.ThemeManager.saveHasSeededNotifications(getApplication(), true, uid)
            },
            onSuccess = { clearSelection(); onResult(true) },
            onError = { onResult(false) }
        )
    }

    fun deleteTravelNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteByCategory(currentUid, NotificationCategory.TRAVEL)
            } catch (e: Exception) {
if (BuildConfig.DEBUG) {
                    Log.e("NotificationVM", "Error in deleteTravelNotifications", e)
}
            }
        }
    }

    fun refreshDemoNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.refreshDemoNotifications(currentUid)
            } catch (e: Exception) {
if (BuildConfig.DEBUG) {
                    Log.e("NotificationVM", "Error in refreshDemoNotifications", e)
}
            }
        }
    }
}
