package com.havamania

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.util.Log
import com.havamania.ui.theme.AssistantTone
import com.havamania.ui.theme.ThemeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class Success(val profile: UserProfile) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel internal constructor(
    application: Application,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val userProfileRepository: UserProfileRepository,
    private val writeGate: (() -> Boolean)? = null,
    private val snapshotWriter: (suspend (UserProfile) -> Unit)? = null,
    private val storageProvider: () -> FirebaseStorage = { FirebaseStorage.getInstance() },
    private val personalizationBasicsWriter: (suspend (String, Set<String>) -> Unit)? = null,
    private val sessionStore: RegistrationPendingStore = MemoryRegistrationPendingStore(),
    private val profileBasicsWriter: (suspend (String, String, String) -> Unit)? = null,
    private val beforeProfileSdkWrite: (suspend (String) -> Unit)? = null
) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, FirebaseAuth.getInstance(),
        FirebaseFirestore.getInstance(), UserProfileRepository.getInstance(),
        sessionStore = FileRegistrationPendingStore(java.io.File(application.noBackupFilesDir, "auth-session-state.json")))


    private val _activeUid = MutableStateFlow(auth.currentUser?.uid)
    internal val activeUid: StateFlow<String?> = _activeUid
    private val accountTasks = AccountTaskScope(viewModelScope) { auth.currentUser?.uid ?: "legacy" }
    private val saveController = ProfileSaveController()
    internal val saveState = saveController.state
    private var reconciliationTicket: Any? = null
    private val _profileReconciliationBusy = MutableStateFlow(false)
    internal val profileReconciliationBusy: StateFlow<Boolean> = _profileReconciliationBusy
    private val sessionFence get() = AuthSessionFences.forAuth(auth, sessionStore)
    internal fun resetSaveState() = saveController.reset()
    private val personalizationSave = PersonalizationSaveController(viewModelScope, { auth.currentUser?.uid ?: "legacy" })
    internal val personalizationSaveState = personalizationSave.state
    internal fun resetPersonalizationSave() = personalizationSave.reset()
    private val _aboutMeSaveState = MutableStateFlow<ProfileSaveState>(ProfileSaveState.Idle)
    internal val aboutMeSaveState: StateFlow<ProfileSaveState> = _aboutMeSaveState
    private val aboutMeRunner = AssistantTaskRunner(viewModelScope, { auth.currentUser?.uid ?: "legacy" }, {}, 20_000)
    internal fun resetAboutMeSave() { aboutMeRunner.reset(); _aboutMeSaveState.value = ProfileSaveState.Idle }

    private val storage get() = storageProvider()

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState

    private val _uploadProgress = MutableStateFlow(false)
    val uploadProgress: StateFlow<Boolean> = _uploadProgress

    private val _avatarVersion = MutableStateFlow(System.currentTimeMillis())
    val avatarVersion: StateFlow<Long> = _avatarVersion

    private val avatarMutations = AvatarMutationController(viewModelScope,
        { auth.currentUser?.uid }, ::profileWritesAllowed,
        { _uploadProgress.value = it },
        { _avatarVersion.value = System.currentTimeMillis() },
        { _profileState.value = ProfileState.Error(it) })


    private var profileJob: kotlinx.coroutines.Job? = null

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        accountTasks.reset()
        saveController.reset()
        personalizationSave.reset()
        resetAboutMeSave()
        avatarMutations.reset()
        val user = firebaseAuth.currentUser
        _activeUid.value = user?.uid
        if (user == null) {
            profileJob?.cancel()
            userProfileRepository.stopObserving()
            _profileState.value = ProfileState.Idle
        } else {
            observeProfile(user.uid)
        }
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    fun refresh() {
        val uid = auth.currentUser?.uid ?: return
        observeProfile(uid, forceRefresh = true)
    }

    fun dismissProfileError() {
        if (_profileState.value !is ProfileState.Error) return
        val uid = auth.currentUser?.uid ?: return
        userProfileRepository.profile.value?.takeIf { it.uid == uid }?.let {
            _profileState.value = ProfileState.Success(it)
        }
    }

    private fun profileWritesAllowed(): Boolean {
        writeGate?.let { return it() }
        val state = AccountDeletionManager.getInstance(getApplication()).state.value
        return !state.pending && !state.busy
    }

    private fun observeProfile(uid: String, forceRefresh: Boolean = false) {
        profileJob?.cancel()
        _profileState.value = userProfileRepository.profile.value
            ?.takeIf { it.uid == uid }?.let { ProfileState.Success(it) } ?: ProfileState.Loading
        userProfileRepository.startObserving(uid, forceRefresh)

        profileJob = accountTasks.launch launch@ { ownerUid ->
            try {
            userProfileRepository.readState.collect { state ->
                if (state.uid != uid || auth.currentUser?.uid != uid || !profileWritesAllowed()) return@collect
                when (state) {
                    is ProfileReadState.Ready -> {
                        _profileState.value = ProfileState.Success(state.profile)
                        if (!state.hasPendingWrites) {
                            if (snapshotWriter != null) snapshotWriter.invoke(state.profile)
                            else ThemeManager.saveProfileSnapshot(getApplication(), state.profile)
                        }
                        reconcileProfileName(state)
                    }
                    is ProfileReadState.Missing -> {
                        _profileState.value = ProfileState.Loading
                        ensureRemoteProfile(uid)
                    }
                    is ProfileReadState.Failed -> _profileState.value = ProfileState.Error(state.message)
                    else -> _profileState.value = ProfileState.Loading
                }
            }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                publishAccountResult(uid, { auth.currentUser?.uid ?: "legacy" }) {
                    if (profileWritesAllowed()) _profileState.value = ProfileState.Error("Profil yüklenemedi. Lütfen bağlantınızı kontrol edip tekrar deneyin.")
                }
            }
        }
    }

    internal fun reconcileAfterSessionReady() {
        accountTasks.launch { owner ->
            val ready = userProfileRepository.readState.value as? ProfileReadState.Ready ?: return@launch
            if (ready.profile.uid == owner) reconcileProfileName(ready)
        }
    }

    private suspend fun reconcileProfileName(state: ProfileReadState.Ready) {
        if (_profileReconciliationBusy.value) return
        val user = auth.currentUser ?: return
        if (!needsProfileNameReconciliation(state.profile, user.uid, user.displayName,
                state.serverConfirmed, sessionFence.canStartProfileWrite(user.uid),
                saveState.value == ProfileSaveState.Saving)) return
        val ticket = Any()
        reconciliationTicket = ticket
        _profileReconciliationBusy.value = true
        try {
            kotlinx.coroutines.withTimeout(20_000) {
                runAccountProfileWrite(user.uid, { auth.currentUser?.uid }, ::profileWritesAllowed, {
                    awaitOwnedSdkMutation<Void>(auth, sessionFence, user.uid) {
                        user.updateProfile(com.google.firebase.auth.userProfileChangeRequest { displayName = state.profile.name })
                    }
                })
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            if (cancelled !is kotlinx.coroutines.TimeoutCancellationException) throw cancelled
        } catch (_: Exception) {
            // Keep the server profile visible. Refresh/new server events may retry the mirror.
        } finally {
            if (reconciliationTicket === ticket) {
                reconciliationTicket = null
                _profileReconciliationBusy.value = false
            }
        }
    }

    private suspend fun ensureRemoteProfile(uid: String) {
        val user = auth.currentUser?.takeIf { it.uid == uid } ?: return
        val newProfile = UserProfile(
            uid = uid, email = user.email ?: "", name = user.displayName ?: "",
            createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
        )
        val document = db.collection("users").document(uid)
        createMissingAccountProfile(uid, { auth.currentUser?.uid }, ::profileWritesAllowed) { isCurrent ->
            db.runTransaction { transaction ->
                stageMissingAccountProfile(isCurrent,
                    exists = { transaction.get(document).exists() },
                    create = { transaction.set(document, newProfile) })
            }.await()
        }
    }


    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        personalizationSave.reset()
        resetAboutMeSave()
        avatarMutations.reset()
        // We don't stopObserving in repository here because other ViewModels might still need it.
        // It's a singleton.
    }

    fun fetchProfile() {
        val uid = auth.currentUser?.uid ?: return
        observeProfile(uid)
    }

    fun updateProfile(name: String, bio: String) {
        val user = auth.currentUser ?: return
        if (_profileReconciliationBusy.value || !sessionFence.canStartProfileWrite(user.uid)) {
            saveController.reject("Önceki profil işlemi tamamlanıyor. Biraz sonra tekrar deneyin.")
            return
        }
        accountTasks.launch { ownerUid ->
            if (user.uid != ownerUid) return@launch
            saveController.save(name, bio) { finalName, finalBio ->
                val updates = mapOf(
                    "name" to finalName,
                    "bio" to finalBio,
                    "updatedAt" to System.currentTimeMillis()
                )
                runAccountProfileWrite(ownerUid, { auth.currentUser?.uid }, ::profileWritesAllowed,
                    { db.collection("users").document(ownerUid).update(updates).await() },
                    { beforeProfileSdkWrite?.invoke(ownerUid)
                      awaitOwnedSdkMutation<Void>(auth, AuthSessionFences.forAuth(auth, sessionStore), ownerUid) {
                        user.updateProfile(com.google.firebase.auth.userProfileChangeRequest { displayName = finalName })
                    } },
                    { profileBasicsWriter?.invoke(ownerUid, finalName, finalBio)
                        ?: ThemeManager.saveProfileBasics(getApplication(), ownerUid, finalName, finalBio) })
            }
        }
    }

    fun updatePersonalization(interests: List<String>, travelStyles: List<String>, weatherPrefs: WeatherPreferences) {
        if (auth.currentUser == null) return
        personalizationSave.save { owner ->
            val personalization = PersonalizationProfile(
                uid = owner, selectedInterests = interests.toList(),
                travelStyles = travelStyles.toList(), weatherPreferences = weatherPrefs)
            runAccountProfileWrite(owner, { auth.currentUser?.uid }, ::profileWritesAllowed,
                { db.collection("users").document(owner).update(mapOf(
                    "personalizationProfile" to personalization,
                    "onboardingCompleted" to true, "updatedAt" to System.currentTimeMillis()
                )).await() },
                { personalizationBasicsWriter?.invoke(owner, interests.toSet())
                        ?: ThemeManager.savePersonalizationBasics(getApplication(), owner, interests.toSet()) })
        }
    }

    fun setUserAboutMe(aboutMe: String) {
        if (auth.currentUser == null) return
        aboutMeRunner.start(
            onStart = { _aboutMeSaveState.value = ProfileSaveState.Saving },
            operation = { owner ->
                runAccountProfileWrite(owner, { auth.currentUser?.uid }, ::profileWritesAllowed,
                    { db.collection("users").document(owner).update("aboutMe", aboutMe.trim(), "updatedAt", System.currentTimeMillis()).await() },
                    { ThemeManager.saveUserAboutMe(getApplication(), owner, aboutMe.trim()) })
            },
            onSuccess = { _aboutMeSaveState.value = ProfileSaveState.Saved },
            onError = { _aboutMeSaveState.value = ProfileSaveState.Failed("Kayıt onayı alınamadı. Metnin korundu; tekrar deneyebilirsin.") }
        )
    }

    fun setInterestSelected(interestId: String, selected: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        accountTasks.launch { owner ->
            if (uid != owner) return@launch
            val job = kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]
            try {
                runAccountProfileWrite(owner, { auth.currentUser?.uid }, ::profileWritesAllowed, {
                    val document = db.collection("users").document(owner)
                    db.runTransaction { transaction ->
                        if (job?.isActive != true || auth.currentUser?.uid != owner || !profileWritesAllowed()) return@runTransaction
                        val snapshot = transaction.get(document)
                        val raw = snapshot.get("personalizationProfile.selectedInterests")
                        require(raw == null || (raw is List<*> && raw.all { it is String }))
                        val existing = (raw as? List<*>)?.filterIsInstance<String>().orEmpty()
                        val updated = updateInterestSelection(existing, interestId, selected)
                        if (job.isActive && auth.currentUser?.uid == owner && profileWritesAllowed() && updated != existing) {
                            transaction.update(document, mapOf(
                                "personalizationProfile.selectedInterests" to updated,
                                "updatedAt" to System.currentTimeMillis()))
                        }
                    }.await()
                })
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                publishAccountResult(owner, { auth.currentUser?.uid ?: "legacy" }) {
                    if (profileWritesAllowed()) _profileState.value = ProfileState.Error("İlgi alanı güncellenemedi. Lütfen tekrar deneyin.")
                }
            }
        }
    }

    fun uploadProfileImage(uri: Uri) {
        avatarMutations.start { owner ->
            val context = getApplication<Application>()
            val reference = storage.reference.child("profile-images/$owner/avatar.jpg")
            lateinit var data: ByteArray
            lateinit var downloadUrl: String
            runAccountProfileWrite(owner, { auth.currentUser?.uid }, ::profileWritesAllowed,
                {
                    data = prepareAvatarJpeg { context.contentResolver.openInputStream(uri) }
                },
                {
                    val metadata = com.google.firebase.storage.StorageMetadata.Builder().setContentType("image/jpeg").build()
                    val upload = reference.putBytes(data, metadata)
                    try { upload.await() }
                    catch (cancelled: kotlinx.coroutines.CancellationException) { upload.cancel(); throw cancelled }
                },
                { downloadUrl = reference.downloadUrl.await().toString() },
                { db.collection("users").document(owner).update("photoURL", downloadUrl, "updatedAt", System.currentTimeMillis()).await() },
                { ThemeManager.saveUserImageUriByUid(context, owner, downloadUrl) })
        }
    }

    fun removeProfileImage() {
        avatarMutations.start { owner ->
            runAccountProfileWrite(owner, { auth.currentUser?.uid }, ::profileWritesAllowed,
                {
                    try { storage.reference.child("profile-images/$owner/avatar.jpg").delete().await() }
                    catch (error: com.google.firebase.storage.StorageException) {
                        if (error.errorCode != com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND) throw error
                    }
                },
                { db.collection("users").document(owner).update("photoURL", null, "updatedAt", System.currentTimeMillis()).await() },
                { ThemeManager.saveUserImageUriByUid(getApplication(), owner, null) })
        }
    }

}
