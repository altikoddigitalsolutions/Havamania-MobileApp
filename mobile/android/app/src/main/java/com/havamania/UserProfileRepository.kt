package com.havamania

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * P2 OPTIMIZATION: Unified repository for User Profile to prevent duplicate Firestore listeners.
 */
internal data class ServerDefaultCity(val uid: String, val name: String?)

class UserProfileRepository private constructor() {
    private val db get() = FirebaseFirestore.getInstance()
    private var profileListener: ListenerRegistration? = null

    private val _serverDefaultCity = MutableStateFlow<ServerDefaultCity?>(null)
    internal val serverDefaultCity = _serverDefaultCity.asStateFlow()
    private var listenerVersion = 0L

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

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

    fun startObserving(uid: String) {
        if (uid == activeUid && profileListener != null) return

        if (uid == "legacy") {
            stopObserving()
            _profile.value = null
            return
        }

        val version = ++listenerVersion
        profileListener?.remove()
        _serverDefaultCity.value = null
        _profile.value = null
        activeUid = uid
        profileListener = db.collection("users").document(uid)
            .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, e ->
                if (activeUid != uid || version != listenerVersion) return@addSnapshotListener
                if (e == null && snapshot != null && snapshot.exists()) {
                    _profile.value = snapshot.toObject(UserProfile::class.java)
                    if (!snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()) {
                        _serverDefaultCity.value = ServerDefaultCity(uid, _profile.value?.defaultCity)
                    }
                } else {
                    _profile.value = null
                }
            }
    }

    fun stopObserving() {
        listenerVersion++
        _serverDefaultCity.value = null
        profileListener?.remove()
        profileListener = null
        activeUid = null
        _profile.value = null
    }
}
