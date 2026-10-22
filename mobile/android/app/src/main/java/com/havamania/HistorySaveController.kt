package com.havamania

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

internal class HistorySaveController(scope: CoroutineScope, owner: () -> String) {
    private val mutableState = MutableStateFlow<ProfileSaveState>(ProfileSaveState.Idle)
    val state = mutableState.asStateFlow()
    private val runner = AssistantTaskRunner(scope, owner, {}, 30_000)
    private var attemptId: String? = null

    fun reset() {
        cancel()
        attemptId = null
    }

    fun cancel() {
        runner.reset()
        mutableState.value = ProfileSaveState.Idle
    }

    fun save(id: String? = null, persist: suspend (String, String) -> Unit) {
        val recordId = attemptId ?: (id ?: UUID.randomUUID().toString()).also { attemptId = it }
        runner.start(
            onStart = { mutableState.value = ProfileSaveState.Saving },
            operation = { uid -> persist(uid, recordId) },
            onSuccess = { mutableState.value = ProfileSaveState.Saved },
            onError = { mutableState.value = ProfileSaveState.Failed(
                "Kayıt onayı alınamadı. Sohbetin korunuyor; tekrar deneyebilirsin.") }
        )
    }
}
