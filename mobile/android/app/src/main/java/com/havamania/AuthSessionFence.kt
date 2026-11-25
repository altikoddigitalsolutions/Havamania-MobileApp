package com.havamania

import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class AuthSessionRecord(val version: Int = 1, val kind: String, val uid: String? = null)

/** SDK-scoped, main-thread confined. Logout intent survives ViewModel and process recreation. */
internal class AuthSessionFence(private val store: RegistrationPendingStore) {
    private var failed = false
    private var record: AuthSessionRecord? = try {
        store.read()?.let { Json.decodeFromString<AuthSessionRecord>(it) }?.also {
            require(it.version == 1 && it.kind in setOf("pending", "signed_out", "accepted"))
            require(if (it.kind == "accepted") !it.uid.isNullOrBlank() && it.uid.length <= 256 else it.uid == null)
        }
    } catch (_: Exception) { failed = true; AuthSessionRecord(kind = "signed_out") }
    private var active: Any? = null
    private var acceptedAnchor: Any? = if (!failed && (record == null || record?.kind == "accepted")) Any() else null
    private val rawTasks = mutableMapOf<Any, Any>()
    val signedOut: Boolean get() = record?.kind == "signed_out"
    val unresolved: Boolean get() = failed || record?.kind == "pending" || rawTasks.values.any { it !== acceptedAnchor }
    private fun persist(value: AuthSessionRecord) {
        try { store.write(Json.encodeToString(value)); record = value; failed = false }
        catch (failure: Exception) { failed = true; throw failure }
    }
    fun canRestore(uid: String?): Boolean = !uid.isNullOrBlank() && !failed && active == null && acceptedAnchor != null && rawTasks.values.all { it === acceptedAnchor } &&
        (record == null || (record?.kind == "accepted" && record?.uid == uid))
    fun canStartProfileWrite(uid: String): Boolean = canRestore(uid) && rawTasks.isEmpty()
    fun ownerLease(uid: String): Any {
        check(canRestore(uid)) { "Account session changed" }
        return checkNotNull(acceptedAnchor)
    }
    fun canInvalidateOwnerLease(lease: Any): Boolean =
        active == null && (acceptedAnchor === lease || signedOut)
    fun verifyOwnerLease(uid: String, lease: Any) {
        check(canRestore(uid) && acceptedAnchor === lease) { "Account session changed" }
    }
    fun mayRecoverRegistration(uid: String): Boolean = !failed && active == null && rawTasks.isEmpty() &&
        (record == null || record?.kind == "pending" || (record?.kind == "accepted" && record?.uid == uid))
    fun begin(): Any {
        if (active != null || rawTasks.isNotEmpty())
            throw RegistrationPreparationException("Önceki giriş isteği tamamlanıyor. Lütfen biraz sonra yeniden deneyin.")
        persist(AuthSessionRecord(kind = "pending"))
        acceptedAnchor = null
        return Any().also { active = it }
    }
    fun reserveSdk(ticket: Any): Any {
        check(active === ticket)
        return Any().also { rawTasks[it] = ticket }
    }
    fun reserveOwnerMutation(uid: String): Pair<Any, Any> {
        check(canRestore(uid)) { "Profile owner session changed" }
        val anchor = checkNotNull(acceptedAnchor)
        return Any().also { rawTasks[it] = anchor } to anchor
    }
    fun finishSdk(raw: Any, ticket: Any, rejectStale: () -> Unit) {
        if (raw !in rawTasks) return
        // Reconcile SDK state before allowing a new authentication request.
        try { if (active !== ticket && acceptedAnchor !== ticket) rejectStale() }
        finally { rawTasks.remove(raw) }
    }
    fun accept(ticket: Any, uid: String) {
        check(active === ticket && rawTasks.isEmpty())
        require(uid.isNotBlank())
        persist(AuthSessionRecord(kind = "accepted", uid = uid))
        active = null
        acceptedAnchor = ticket
    }
    fun abandon(ticket: Any?) { if (ticket != null && active === ticket) active = null }
    fun lockSignedOut() {
        active = null
        acceptedAnchor = null
        persist(AuthSessionRecord(kind = "signed_out"))
    }
    fun rejectSdkUser(uid: String?): Boolean = uid != null && active == null && !canRestore(uid)
    fun observeExternalSignOut() { if (active == null && (record?.kind == "accepted" || rawTasks.isNotEmpty())) lockSignedOut() }
}

/** The fence outlives a ViewModel while its uncancellable SDK Task is still running. */
internal object AuthSessionFences {
    private val fences = java.util.WeakHashMap<com.google.firebase.auth.FirebaseAuth, AuthSessionFence>()
    @Synchronized fun forAuth(auth: com.google.firebase.auth.FirebaseAuth, store: RegistrationPendingStore): AuthSessionFence =
        fences.getOrPut(auth) { AuthSessionFence(store) }
}
internal suspend fun <T> awaitFencedSdkTask(
    sdk: com.google.firebase.auth.FirebaseAuth, fence: AuthSessionFence, ticket: Any,
    request: () -> com.google.android.gms.tasks.Task<T>
): T {
    return awaitTrackedSdkTask(sdk, fence, fence.reserveSdk(ticket), ticket, request)
}

internal suspend fun <T> awaitOwnedSdkMutation(
    sdk: com.google.firebase.auth.FirebaseAuth, fence: AuthSessionFence, uid: String,
    request: () -> com.google.android.gms.tasks.Task<T>
): T {
    check(sdk.currentUser?.uid == uid) { "Profile owner changed" }
    val (raw, anchor) = fence.reserveOwnerMutation(uid)
    return awaitTrackedSdkTask(sdk, fence, raw, anchor, request)
}

private suspend fun <T> awaitTrackedSdkTask(
    sdk: com.google.firebase.auth.FirebaseAuth, fence: AuthSessionFence, raw: Any, ticket: Any,
    request: () -> com.google.android.gms.tasks.Task<T>
): T {
    val task = try { request() } catch (failure: Exception) {
        fence.finishSdk(raw, ticket) { sdk.signOut() }; throw failure
    }
    task.addOnCompleteListener(com.google.android.gms.tasks.TaskExecutors.MAIN_THREAD) {
        fence.finishSdk(raw, ticket) { sdk.signOut() }
    }
    try { return task.await() }
    finally { if (task.isComplete) fence.finishSdk(raw, ticket) { sdk.signOut() } }
}

/** Explicit credentials for the deletion owner, never authorization for a different account. */
internal suspend fun authenticateDeletionOwner(
    sdk: com.google.firebase.auth.FirebaseAuth, fence: AuthSessionFence,
    email: String, password: String, expectedUid: String
): com.google.firebase.auth.FirebaseUser {
    val ticket = fence.begin()
    try {
        val user = checkNotNull(awaitFencedSdkTask(sdk, fence, ticket) {
            sdk.signInWithEmailAndPassword(email.trim(), password)
        }.user)
        if (user.uid != expectedUid) {
            fence.lockSignedOut(); sdk.signOut()
            throw RegistrationPreparationException("Silinecek hesapla giriş yapın.")
        }
        fence.accept(ticket, user.uid)
        return user
    } catch (failure: Exception) {
        fence.abandon(ticket)
        if (fence.rejectSdkUser(sdk.currentUser?.uid)) sdk.signOut()
        throw failure
    }
}
internal suspend fun reauthenticateDeletionOwner(
    sdk: com.google.firebase.auth.FirebaseAuth, fence: AuthSessionFence,
    user: com.google.firebase.auth.FirebaseUser, email: String, password: String, expectedUid: String,
    request: (() -> com.google.android.gms.tasks.Task<Void>)? = null
) {
    require(user.uid == expectedUid && sdk.currentUser?.uid == expectedUid)
    val ticket = fence.begin()
    try {
        awaitFencedSdkTask(sdk, fence, ticket) {
            request?.invoke() ?: user.reauthenticate(com.google.firebase.auth.EmailAuthProvider.getCredential(email, password))
        }
        check(sdk.currentUser?.uid == expectedUid)
        fence.accept(ticket, expectedUid)
    } catch (failure: Exception) {
        fence.abandon(ticket)
        if (fence.rejectSdkUser(sdk.currentUser?.uid)) sdk.signOut()
        throw failure
    }
}
