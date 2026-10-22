package com.havamania

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout

internal sealed interface ProfileSaveState {
    data object Idle : ProfileSaveState
    data object Saving : ProfileSaveState
    data object Saved : ProfileSaveState
    data class Failed(val message: String) : ProfileSaveState
}

/** A save has its own state; incoming profile snapshots cannot acknowledge it. */
internal class ProfileSaveController(private val timeoutMillis: Long = 20_000) {
    private val mutableState = MutableStateFlow<ProfileSaveState>(ProfileSaveState.Idle)
    val state = mutableState.asStateFlow()
    fun reset() { mutableState.value = ProfileSaveState.Idle }

    suspend fun save(name: String, bio: String, persist: suspend (String, String) -> Unit) {
        if (mutableState.value == ProfileSaveState.Saving) return
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            mutableState.value = ProfileSaveState.Failed("İsim boş olamaz.")
            return
        }
        mutableState.value = ProfileSaveState.Saving
        try {
            withTimeout(timeoutMillis) { persist(trimmedName.take(50), bio.trim().take(500)) }
            mutableState.value = ProfileSaveState.Saved
        } catch (_: TimeoutCancellationException) {
            mutableState.value = ProfileSaveState.Failed("Kayıt onayı alınamadı. Bağlantınızı kontrol edip tekrar deneyin.")
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            mutableState.value = ProfileSaveState.Failed("Profil kaydedilemedi. Lütfen tekrar deneyin.")
        }
    }
}
