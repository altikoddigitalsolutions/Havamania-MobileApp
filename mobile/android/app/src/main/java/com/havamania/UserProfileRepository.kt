package com.havamania

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * P2 OPTIMIZATION: Unified repository for User Profile to prevent duplicate Firestore listeners.
 */
internal data class ServerDefaultCity(val uid: String, val name: String?, val location: Any? = null)

class UserProfileRepository internal constructor(
    private val callbackExecutor: java.util.concurrent.Executor = com.google.android.gms.tasks.TaskExecutors.MAIN_THREAD,
    private val firestore: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) {
    private val db get() = firestore()
    private var profileListener: ListenerRegistration? = null

    private val _serverDefaultCity = MutableStateFlow<ServerDefaultCity?>(null)
    internal val serverDefaultCity = _serverDefaultCity.asStateFlow()
    private var listenerVersion = 0L
    private var confirmedPremium = false

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()
    private val _readState = MutableStateFlow<ProfileReadState>(ProfileReadState.Idle)
    internal val readState = _readState.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: UserProfileRepository? = null

        fun getInstance(): UserProfileRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserProfileRepository().also { INSTANCE = it }
            }
        }
    }

    private var activeUid: String? = null

    fun startObserving(uid: String, forceRefresh: Boolean = false) {
        if (!forceRefresh && uid == activeUid && profileListener != null) return

        if (uid == "legacy") {
            stopObserving()
            _profile.value = null
            return
        }

        val version = ++listenerVersion
        profileListener?.remove()
        _serverDefaultCity.value = null
        confirmedPremium = false
        _profile.value = null
        activeUid = uid
        _readState.value = ProfileReadState.Loading(uid)
        profileListener = db.collection("users").document(uid)
            .addSnapshotListener(callbackExecutor, com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, e ->
                if (activeUid != uid || version != listenerVersion) return@addSnapshotListener
                if (e != null || snapshot == null) {
                    _profile.value = null
                    _serverDefaultCity.value = null
                    confirmedPremium = false
                    _readState.value = ProfileReadState.Failed(uid,
                        if (e?.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                            e?.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAUTHENTICATED)
                            PROFILE_ACCESS_ERROR else PROFILE_CONNECTION_ERROR)
                } else if (!snapshot.exists()) {
                    _profile.value = null
                    _serverDefaultCity.value = null
                    confirmedPremium = false
                    _readState.value = if (!snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites())
                        ProfileReadState.Missing(uid) else ProfileReadState.Loading(uid)
                } else {
                    val decoded = decodeObservedProfile(uid) {
                        snapshot.toObject(UserProfile::class.java)?.copy(isPremium = profilePremiumForSnapshot(
                            snapshot.data.orEmpty(), confirmedPremium,
                            snapshot.metadata.isFromCache, snapshot.metadata.hasPendingWrites()))
                    }
                    if (decoded is ProfileReadState.Ready) {
                        _profile.value = decoded.profile
                        if (!snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()) {
                            confirmedPremium = decoded.profile.isPremium
                            _serverDefaultCity.value = ServerDefaultCity(uid, decoded.profile.defaultCity, snapshot.get("defaultCityLocation"))
                        }
                    } else {
                        _profile.value = null
                        _serverDefaultCity.value = null
                        confirmedPremium = false
                    }
                    _readState.value = decoded
                }
            }
    }

    fun stopObserving() {
        listenerVersion++
        _serverDefaultCity.value = null
        confirmedPremium = false
        profileListener?.remove()
        profileListener = null
        activeUid = null
        _profile.value = null
        _readState.value = ProfileReadState.Idle
    }
}
