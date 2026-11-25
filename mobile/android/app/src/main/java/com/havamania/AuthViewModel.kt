package com.havamania

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object PasswordResetSent : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel internal constructor(
    application: Application,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    registrationStore: RegistrationPendingStore = MemoryRegistrationPendingStore(),
    sessionStore: RegistrationPendingStore = MemoryRegistrationPendingStore(),
    private val signInRequest: ((String, String) -> com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>)? = null,
    private val signUpRequest: ((String, String) -> com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>)? = null,
    private val sessionReloadRequest: ((FirebaseUser) -> com.google.android.gms.tasks.Task<Void>)? = null,
    private val beforeRegistrationProfileWrite: (suspend (String) -> Unit)? = null
) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, FirebaseAuth.getInstance(), FirebaseFirestore.getInstance(),
        registrationStore = FileRegistrationPendingStore(java.io.File(application.noBackupFilesDir, "registration-pending.json")),
        sessionStore = FileRegistrationPendingStore(java.io.File(application.noBackupFilesDir, "auth-session-state.json")))

    private val session = AuthSessionFences.forAuth(auth, sessionStore)
    private var activeAuthTicket: Any? = null
    private var cleared = false
    private var validationTicket: Any? = null
    private var validationJob: kotlinx.coroutines.Job? = null
    private val _sessionReadyEpoch = MutableStateFlow(0L)
    internal val sessionReadyEpoch: StateFlow<Long> = _sessionReadyEpoch
    internal val validatingSession: Boolean get() = validationJob?.isActive == true
    private val registration = RegistrationPreparation(registrationStore)
    private val _authState = MutableStateFlow<AuthState>(if (registration.blocked || session.unresolved)
        AuthState.Error("Oturum hazırlığı tamamlanamadı. Yeniden giriş yaparak devam edebilirsiniz.")
        else auth.currentUser?.takeIf { session.canRestore(it.uid) }?.let { AuthState.Authenticated(it) } ?: AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    val currentUser: FirebaseUser? get() =
        (_authState.value as? AuthState.Authenticated)?.user?.takeIf {
            !registration.blocked && session.canRestore(it.uid) && auth.currentUser?.uid == it.uid
        }
    private val operations = AuthOperationRunner(viewModelScope,
        onStart = { _authState.value = AuthState.Loading },
        onFailure = { abandonAuthentication(); _authState.value = AuthState.Error(mapFirebaseError(it)) },
        onCancelled = { abandonAuthentication(); _authState.value = AuthState.Idle })

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        if (cleared) return@AuthStateListener
        val sdkUser = firebaseAuth.currentUser
        if (session.rejectSdkUser(sdkUser?.uid) && !canKeepRegistrationSession()) {
            firebaseAuth.signOut()
            if (_authState.value !is AuthState.Error) _authState.value = AuthState.Idle
            return@AuthStateListener
        }
        if (operations.busy || registration.blocked) return@AuthStateListener
        if (sdkUser != null && !session.canRestore(sdkUser.uid)) return@AuthStateListener
        if (sdkUser == null) {
            runCatching { session.observeExternalSignOut() }.onFailure {
                _authState.value = AuthState.Error("Oturum kapatıldı; yerel oturum durumu kaydedilemedi.")
            }
        }
        val user = firebaseAuth.currentUser
        if (user != null) {
            _authState.value = AuthState.Authenticated(user)
        } else {
            if (_authState.value !is AuthState.Loading && _authState.value !is AuthState.Error && _authState.value !is AuthState.PasswordResetSent) {
                _authState.value = AuthState.Idle
            }
        }
    }

    init {
        auth.addAuthStateListener(authListener)
        val saved = registration.recovery
        val user = auth.currentUser
        if (saved?.uid != null && saved.uid == user?.uid && saved.email.equals(user?.email, ignoreCase = true) && session.mayRecoverRegistration(saved.uid)) {
            startAuthentication { ticket ->
                prepareRegistration(saved.name, saved.email, null, ticket)
            }
        }
    }

    override fun onCleared() {
        cleared = true
        cancelSessionValidation()
        abandonAuthentication()
        operations.close()
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }

    private fun mapFirebaseError(e: Exception): String {
        if (e is RegistrationPreparationException) return e.message ?: "Kayıt hazırlığı tamamlanamadı."
        val message = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode.orEmpty() + " " + e.message.orEmpty()
if (BuildConfig.DEBUG) {
            android.util.Log.e("AuthError", "Firebase Auth Error: $message", e)
}

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
        startAuthentication { ticket ->
            val saved = registration.recovery
            val user = checkNotNull(awaitSdkAuthentication(ticket) { signInRequest?.invoke(email.trim(), password) ?: auth.signInWithEmailAndPassword(email.trim(), password) }.user)
            if (registration.blocked) {
                if (saved?.uid != null && saved.uid == user.uid && saved.email.equals(user?.email, ignoreCase = true)) {
                    prepareRegistration(saved.name, saved.email, null, ticket)
                } else {
                    // Explicit credentials prove the new session; never apply another owner's pending fields.
                    registration.clear()
                }
            }
        }
    }

    fun signUp(name: String, email: String, password: String) {
        startAuthentication { ticket -> prepareRegistration(name, email, password, ticket) }
    }

    private suspend fun prepareRegistration(name: String, email: String, password: String?, ticket: Any) {
            registration.finish(name, email,
                current = { auth.currentUser?.let { RegistrationIdentity(it.uid, it.email) } },
                create = { address ->
                    val secret = password ?: throw RegistrationPreparationException("Kayıt için yeniden giriş yapın.")
                    val user = checkNotNull(awaitSdkAuthentication(ticket) { signUpRequest?.invoke(address, secret) ?: auth.createUserWithEmailAndPassword(address, secret) }.user)
                    RegistrationIdentity(user.uid, user.email)
                },
                updateName = { uid, displayName ->
                    val user = checkNotNull(auth.currentUser?.takeIf { it.uid == uid })
                    if (password != null) awaitFencedSdkTask<Void>(auth, session, ticket) {
                        user.updateProfile(userProfileChangeRequest { this.displayName = displayName })
                    }
                },
                saveProfile = { uid, displayName, address ->
                    beforeRegistrationProfileWrite?.invoke(uid)
                    val document = db.collection("users").document(uid)
                    db.runTransaction { transaction ->
                        check(auth.currentUser?.uid == uid) { "Account changed during registration" }
                        val existing = transaction.get(document)
                        val fields = registrationProfilePatch(uid, displayName, address, existing.data.orEmpty(),
                            recovering = password == null, now = System.currentTimeMillis())
                        if (fields.isNotEmpty()) transaction.set(document, fields, SetOptions.merge())
                        Unit
                    }.await()
                })
    }
    fun resetPassword(email: String) {
        operations.start(onSuccess = { _authState.value = AuthState.PasswordResetSent }) {
            auth.sendPasswordResetEmail(email.trim()).await()
        }
    }

    private fun canKeepRegistrationSession(): Boolean {
        val saved = registration.recovery ?: return false
        val user = auth.currentUser ?: return false
        return saved.uid == user.uid && saved.email.equals(user.email, true) && session.mayRecoverRegistration(user.uid)
    }
    private fun abandonAuthentication() {
        session.abandon(activeAuthTicket)
        activeAuthTicket = null
        if (session.rejectSdkUser(auth.currentUser?.uid) && !canKeepRegistrationSession()) auth.signOut()
    }
    private fun startAuthentication(work: suspend (Any) -> Unit) {
        if (cleared || operations.busy) return
        val ticket = try { session.begin() } catch (failure: Exception) {
            _authState.value = AuthState.Error(mapFirebaseError(failure)); return
        }
        cancelSessionValidation()
        activeAuthTicket = ticket
        if (!operations.start(onSuccess = {
            val user = checkNotNull(auth.currentUser)
            session.accept(ticket, user.uid)
            activeAuthTicket = null
            _authState.value = AuthState.Authenticated(user)
            _sessionReadyEpoch.value++
        }) { work(ticket) }) { session.abandon(ticket); activeAuthTicket = null }
    }
    private suspend fun awaitSdkAuthentication(ticket: Any,
        request: () -> com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>): com.google.firebase.auth.AuthResult {
        return awaitFencedSdkTask(auth, session, ticket, request)
    }
    internal fun cancelRegistration() {
        if (registration.blocked || operations.busy) signOut()
    }

    fun signOut() {
        cancelSessionValidation()
        val locked = runCatching { session.lockSignedOut() }.isSuccess
        activeAuthTicket = null
        val cleared = runCatching { registration.clear() }.isSuccess
        operations.cancel()
        auth.signOut()
        _authState.value = if (cleared && locked) AuthState.Idle else AuthState.Error("Oturum kapatıldı; yerel kayıt hazırlığı temizlenemedi. Yeniden giriş yapın.")
    }

    private fun cancelSessionValidation() {
        validationTicket = null
        validationJob?.cancel()
        validationJob = null
    }

    internal fun refreshSession() {
        if (cleared || operations.busy || validatingSession) return
        val user = currentUser ?: return
        val lease = session.ownerLease(user.uid)
        val ticket = Any()
        validationTicket = ticket
        val pending = viewModelScope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            var validated = false
            try {
                kotlinx.coroutines.withTimeout(20_000) {
                    while (!session.canStartProfileWrite(user.uid)) {
                        session.verifyOwnerLease(user.uid, lease)
                        kotlinx.coroutines.delay(25)
                    }
                    awaitOwnedSdkMutation<Void>(auth, session, user.uid) {
                        sessionReloadRequest?.invoke(user) ?: user.reload()
                    }
                    session.verifyOwnerLease(user.uid, lease)
                    awaitOwnedSdkMutation(auth, session, user.uid) { user.getIdToken(true) }
                    session.verifyOwnerLease(user.uid, lease)
                }
                validated = true
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                val code = (failure as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
                if (validationTicket === ticket && !cleared && !operations.busy &&
                    session.canInvalidateOwnerLease(lease) &&
                    (auth.currentUser == null || auth.currentUser?.uid == user.uid) &&
                    isPermanentAuthSessionFailure(code)) {
                    signOut()
                    _authState.value = AuthState.Error("Oturumunuz artık geçerli değil. Lütfen yeniden giriş yapın.")
                }
                // A transient service/network failure does not invalidate the cached owner.
            } finally {
                if (validationTicket === ticket) {
                    validationTicket = null
                    validationJob = null
                    if (validated) _sessionReadyEpoch.value++
                }
            }
        }
        validationJob = pending
        pending.start()
    }

    fun deleteAccount(password: String, onComplete: (Boolean, String?) -> Unit) {
        AccountDeletionManager.getInstance(getApplication()).start(password, onComplete)
    }

    fun clearError() {
        if (operations.busy) return
        _authState.value = AuthState.Idle
    }
}
