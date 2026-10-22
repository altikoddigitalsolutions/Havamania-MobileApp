package com.havamania

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class PersonalizationSaveController(scope: CoroutineScope, currentUid: () -> String,
                                             timeoutMillis: Long = 20_000) {
    private val mutableState = MutableStateFlow<ProfileSaveState>(ProfileSaveState.Idle)
    val state = mutableState.asStateFlow()
    private val runner = AssistantTaskRunner(scope, currentUid, {}, timeoutMillis)

    fun reset() {
        runner.reset()
        mutableState.value = ProfileSaveState.Idle
    }

    fun save(persist: suspend (String) -> Unit) {
        runner.start(
            onStart = { mutableState.value = ProfileSaveState.Saving },
            operation = persist,
            onSuccess = { mutableState.value = ProfileSaveState.Saved },
            onError = { mutableState.value = ProfileSaveState.Failed(
                "Tercihler için kayıt onayı alınamadı. Seçimlerin korundu; tekrar deneyebilirsin.") }
        )
    }
}
