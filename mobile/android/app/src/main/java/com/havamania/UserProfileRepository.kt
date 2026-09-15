package com.havamania

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * P2 OPTIMIZATION: Unified repository for User Profile to prevent duplicate Firestore listeners.
 */
class UserProfileRepository private constructor() {
    private val db get() = FirebaseFirestore.getInstance()
    private var profileListener: ListenerRegistration? = null

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

        profileListener?.remove()
        _profile.value = null
        activeUid = uid
        profileListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (activeUid != uid) return@addSnapshotListener
                if (e == null && snapshot != null && snapshot.exists()) {
                    _profile.value = snapshot.toObject(UserProfile::class.java)
                } else {
                    _profile.value = null
                }
            }
    }

    fun stopObserving() {
        profileListener?.remove()
        profileListener = null
        activeUid = null
        _profile.value = null
    }
}
