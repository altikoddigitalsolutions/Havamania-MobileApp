package com.havamania

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth

class AssistantConsentSettingsViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    internal val consent = AssistantConsentController(viewModelScope, { auth.currentUser?.uid ?: "legacy" })
    private val listener = FirebaseAuth.AuthStateListener { consent.reset() }
    init { auth.addAuthStateListener(listener) }
    override fun onCleared() { auth.removeAuthStateListener(listener); consent.close(); super.onCleared() }
}
