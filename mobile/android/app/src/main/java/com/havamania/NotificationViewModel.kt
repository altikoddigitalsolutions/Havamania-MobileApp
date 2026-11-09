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
    val isLoading: Boolean = false,
    val owner: String? = null,
    val loadFailed: Boolean = false
)

internal fun NotificationUiState.withSelection(ids: Set<String>): NotificationUiState =
    copy(selectedIds = ids, isSelectionMode = ids.isNotEmpty())

internal fun NotificationUiState.toggleVisibleSelection(): NotificationUiState {
    val visibleIds = filteredNotifications.map { it.id }.toSet()
    return withSelection(if (selectedIds == visibleIds) emptySet() else visibleIds)
}

internal fun Flow<List<NotificationItem>>.inboxReadResults(): Flow<Result<List<NotificationItem>>> =
    map { Result.success(it) }.catch { error ->
        if (error !is Exception || error is kotlinx.coroutines.CancellationException) throw error
        emit(Result.failure(error))
    }

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

    private fun notificationAction(
        owner: String = currentUid,
        onSuccess: () -> Unit = {},
        action: suspend (String) -> Unit
    ) {
        if (owner != currentUid) return
        actionTasks.launch { uid ->
            try {
                actionMutex.withLock {
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    if (uid != owner || uid != currentUid) return@withLock
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
            Log.d(TAG, "Auth state changed. Re-initializing notifications.")
        }
        actionTasks.reset()
        _actionError.value = null
        deleteAllRunner.reset()
        _uiState.value = NotificationUiState(isLoading = true, owner = newUid)
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
            repository.getAllNotificationsFlow(uid).inboxReadResults().collect { result ->
                if (uid != currentUid || _uiState.value.owner != uid) return@collect
                updateStateWithList(result.getOrNull().orEmpty(), loadFailed = result.isFailure)
            }
        }
    }

    fun retryLoad(owner: String?) {
        if (owner == null || owner != currentUid || owner != _uiState.value.owner || !_uiState.value.loadFailed) return
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        startCollectingNotifications(owner)
    }

    fun ensureSeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedInitialDataIfNeeded(getApplication(), currentUid)
        }
    }

    private fun updateStateWithList(list: List<NotificationItem>, loadFailed: Boolean = false) {
        _uiState.update { current ->
            val filtered = filterNotifications(list, current.activeFilter)
            val selected = if (current.selectedIds.isEmpty()) emptySet() else
                filtered.mapNotNullTo(mutableSetOf()) { it.id.takeIf(current.selectedIds::contains) }
            current.copy(
                notifications = list,
                filteredNotifications = filtered,
                unreadCount = list.count { !it.isRead },
                isLoading = false,
                loadFailed = loadFailed
            ).withSelection(selected)
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
            if (current.activeFilter == filter) return@update current
            val filtered = filterNotifications(current.notifications, filter)
            current.copy(
                activeFilter = filter,
                filteredNotifications = filtered
            ).withSelection(emptySet())
        }
    }

    fun toggleSelection(item: NotificationItem) {
        _uiState.update { current ->
            val id = item.id
            if (item.userId != currentUid || current.owner != item.userId ||
                current.filteredNotifications.none { it.id == id }) return@update current
            val newSelected = if (current.selectedIds.contains(id)) {
                current.selectedIds - id
            } else {
                current.selectedIds + id
            }
            current.withSelection(newSelected)
        }
    }

    fun selectAll(owner: String?) {
        _uiState.update { state ->
            if (owner == null || owner != currentUid || owner != state.owner) state
            else state.toggleVisibleSelection()
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
    }

    fun deleteNotification(item: NotificationItem) = notificationAction(
        owner = item.userId,
        onSuccess = { _uiState.update { it.withSelection(it.selectedIds - item.id) } }
    ) { uid -> repository.delete(uid, listOf(item.id)) }

    fun deleteSelected(selection: NotificationUiState) {
        val owner = selection.owner ?: return
        val ids = selection.selectedIds.toList()
        if (ids.isNotEmpty()) notificationAction(owner, ::clearSelection) { uid -> repository.delete(uid, ids) }
    }

    fun markAsRead(item: NotificationItem) =
        notificationAction(owner = item.userId) { uid -> repository.markAsRead(uid, listOf(item.id)) }

    fun toggleReadStatus(item: NotificationItem) =
        notificationAction(owner = item.userId) { uid -> repository.toggleReadStatus(uid, item.id, item.isRead) }

    fun markSelectedAsRead(selection: NotificationUiState) {
        val owner = selection.owner ?: return
        val ids = selection.selectedIds.toList()
        if (ids.isNotEmpty()) notificationAction(owner, ::clearSelection) { uid -> repository.markAsRead(uid, ids) }
    }

    fun markAllAsRead(owner: String?) {
        if (owner != null) notificationAction(owner) { uid -> repository.markAllAsRead(uid) }
    }

    fun deleteAllNotifications(owner: String, onResult: (Boolean) -> Unit) {
        if (owner != currentUid) return
        deleteAllRunner.start(
            operation = { uid ->
                if (uid != owner || uid != currentUid) throw kotlinx.coroutines.CancellationException()
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
