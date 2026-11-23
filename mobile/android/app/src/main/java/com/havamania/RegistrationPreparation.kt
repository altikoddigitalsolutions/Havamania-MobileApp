package com.havamania

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

internal data class RegistrationIdentity(val uid: String, val email: String?)
internal class RegistrationPreparationException(message: String) : IllegalStateException(message)

/** One ViewModel session; never retains a password. Main-thread confined. */
internal class RegistrationPreparation {
    private class Attempt(val email: String, var uid: String? = null)
    private var pending: Attempt? = null
    val blocked: Boolean get() = pending != null
    fun clear() { pending = null }

    suspend fun finish(
        name: String, email: String,
        current: () -> RegistrationIdentity?,
        create: suspend (String) -> RegistrationIdentity,
        updateName: suspend (String, String) -> Unit,
        saveProfile: suspend (String, String, String) -> Unit
    ) {
        val address = email.trim()
        val previous = pending
        if (previous != null && !previous.email.equals(address, ignoreCase = true))
            throw RegistrationPreparationException("Önce mevcut kaydı tamamlayın veya oturumu kapatın.")
        if (previous == null && current() != null)
            throw RegistrationPreparationException("Yeni hesap oluşturmadan önce oturumu kapatın.")
        val attempt = previous ?: Attempt(address).also { pending = it }
        coroutineContext.ensureActive()
        // A cancelled await may have created the SDK account before returning its result.
        val identity = if (previous != null && current()?.email.equals(address, ignoreCase = true)) {
            checkNotNull(current())
        } else {
            if (attempt.uid != null) throw RegistrationPreparationException("Oturum değişti. Yeniden giriş yapın.")
            try {
                create(address)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                // No account was established: allow correcting the address on the next attempt.
                if (pending === attempt && current() == null) pending = null
                throw failure
            }
        }
        fun verify() {
            if (pending !== attempt || current()?.uid != identity.uid ||
                (attempt.uid != null && attempt.uid != identity.uid))
                throw RegistrationPreparationException("Oturum değişti. Yeniden giriş yapın.")
        }
        coroutineContext.ensureActive()
        verify()
        attempt.uid = identity.uid
        updateName(identity.uid, name.trim())
        coroutineContext.ensureActive()
        verify()
        saveProfile(identity.uid, name.trim(), address)
        coroutineContext.ensureActive()
        verify()
        pending = null
    }
}