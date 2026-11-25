package com.havamania

import kotlinx.coroutines.CancellationException

internal sealed class ProfileReadState(open val uid: String?) {
    object Idle : ProfileReadState(null)
    data class Loading(override val uid: String) : ProfileReadState(uid)
    data class Ready(val profile: UserProfile, val serverConfirmed: Boolean = false, val hasPendingWrites: Boolean = false) : ProfileReadState(profile.uid)
    data class Missing(override val uid: String) : ProfileReadState(uid)
    data class Failed(override val uid: String, val message: String) : ProfileReadState(uid)
}

internal const val PROFILE_DATA_ERROR = "Profil verileri okunamadı. Kayıtlarınız değiştirilmedi; lütfen yeniden deneyin."
internal const val PROFILE_ACCESS_ERROR = "Profil erişimi doğrulanamadı. Oturumunuzu kontrol edip yeniden giriş yapın."
internal const val PROFILE_CONNECTION_ERROR = "Profil alınamadı. Bağlantınızı kontrol edip yeniden deneyin."

/** A missing UID can be inherited from its authoritative document path; another UID never can. */
internal fun decodeObservedProfile(uid: String, decode: () -> UserProfile?): ProfileReadState = try {
    val profile = decode()
    if (profile == null || (profile.uid.isNotBlank() && profile.uid != uid))
        ProfileReadState.Failed(uid, PROFILE_DATA_ERROR)
    else ProfileReadState.Ready(profile.copy(uid = uid))
} catch (error: CancellationException) {
    throw error
} catch (_: Exception) {
    ProfileReadState.Failed(uid, PROFILE_DATA_ERROR)
}
