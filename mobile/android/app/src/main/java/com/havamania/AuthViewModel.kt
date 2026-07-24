package com.havamania

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    val currentUser: FirebaseUser? get() = auth.currentUser

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                _authState.value = AuthState.Authenticated(user)
            } else {
                if (_authState.value !is AuthState.Loading && _authState.value !is AuthState.Error) {
                    _authState.value = AuthState.Idle
                }
            }
        }
    }

    private fun mapFirebaseError(e: Exception): String {
        val message = e.message ?: ""
        android.util.Log.e("AuthError", "Firebase Auth Error: $message", e)

        return when {
            message.contains("INVALID_LOGIN_CREDENTIALS") ||
            message.contains("INVALID_PASSWORD") ||
            message.contains("USER_NOT_FOUND") ||
            message.contains("ERROR_INVALID_CREDENTIAL") ||
            message.contains("ERROR_WRONG_PASSWORD") ||
            message.contains("ERROR_USER_NOT_FOUND") ->
                "E-posta adresi veya şifre hatalı."

            message.contains("INVALID_EMAIL") || message.contains("ERROR_INVALID_EMAIL") ->
                "Geçerli bir e-posta adresi girin."

            message.contains("USER_DISABLED") || message.contains("ERROR_USER_DISABLED") ->
                "Bu kullanıcı hesabı devre dışı bırakılmış."

            message.contains("TOO_MANY_ATTEMPTS_TRY_LATER") ||
            message.contains("FirebaseTooManyRequestsException") ||
            message.contains("ERROR_TOO_MANY_REQUESTS") ->
                "Çok fazla başarısız giriş denemesi yapıldı. Lütfen bir süre sonra tekrar deneyin."

            message.contains("NETWORK_ERROR") ||
            message.contains("FirebaseNetworkException") ||
            message.contains("ERROR_NETWORK_REQUEST_FAILED") ->
                "İnternet bağlantısı kurulamadı. Bağlantınızı kontrol edip tekrar deneyin."

            message.contains("EMAIL_ALREADY_IN_USE") || message.contains("ERROR_EMAIL_ALREADY_IN_USE") ->
                "Bu e-posta adresi zaten kullanımda."

            message.contains("WEAK_PASSWORD") || message.contains("ERROR_WEAK_PASSWORD") ->
                "Şifre çok zayıf. Lütfen en az 6 karakter kullanın."

            message.contains("OPERATION_NOT_ALLOWED") || message.contains("ERROR_OPERATION_NOT_ALLOWED") ->
                "Giriş yöntemi devre dışı bırakılmış."

            else -> "Giriş işlemi tamamlanamadı. Lütfen tekrar deneyin."
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.signInWithEmailAndPassword(email, password).await()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapFirebaseError(e))
            }
        }
    }

    fun signUp(name: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user

                user?.updateProfile(userProfileChangeRequest {
                    displayName = name
                })?.await()

                // Create user document in Firestore
                user?.let {
                    val userData = hashMapOf(
                        "uid" to it.uid,
                        "name" to name,
                        "email" to email,
                        "createdAt" to System.currentTimeMillis()
                    )
                    db.collection("users").document(it.uid).set(userData).await()
                }

                // Sign in is automatic after createUserWithEmailAndPassword success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapFirebaseError(e))
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.sendPasswordResetEmail(email).await()
                _authState.value = AuthState.Idle
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapFirebaseError(e))
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }

    /**
     * Account Deletion with Safety (Business Rule 9)
     */
    fun deleteAccount(password: String, onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // 1. Re-authenticate
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
                user.reauthenticate(credential).await()

                // 2. Delete Firestore Data
                db.collection("users").document(user.uid).delete().await()

                // 3. Delete Auth Account
                user.delete().await()

                _authState.value = AuthState.Idle
                onComplete(true, null)
            } catch (e: Exception) {
                val error = mapFirebaseError(e)
                _authState.value = AuthState.Error(error)
                onComplete(false, error)
            }
        }
    }

    fun clearError() {
        _authState.value = AuthState.Idle
    }
}
