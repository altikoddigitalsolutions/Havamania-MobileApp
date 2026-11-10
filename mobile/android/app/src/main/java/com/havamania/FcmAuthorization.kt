package com.havamania

import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.CancellationException

internal class FcmAuthRequiredException : Exception("FCM registration requires a new sign-in")

internal suspend fun fcmAuthorization(loadToken: suspend () -> String?): String? = try {
    loadToken()?.let { "Bearer $it" }
} catch (error: CancellationException) {
    throw error
} catch (_: FirebaseAuthInvalidUserException) {
    // Do not chain provider messages or credentials into diagnostics.
    throw FcmAuthRequiredException()
}
