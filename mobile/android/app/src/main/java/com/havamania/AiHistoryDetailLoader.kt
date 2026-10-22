package com.havamania

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal sealed interface AiHistoryDetailState {
    data object Loading : AiHistoryDetailState
    data class Ready(val item: AiHistoryEntity) : AiHistoryDetailState
    data object Missing : AiHistoryDetailState
    data object Failed : AiHistoryDetailState
}

internal class AiHistoryDetailLoader(scope: CoroutineScope, owner: () -> String) {
    private val mutableState = MutableStateFlow<AiHistoryDetailState>(AiHistoryDetailState.Loading)
    val state = mutableState.asStateFlow()
    private val runner = AssistantTaskRunner(scope, owner, {}, 20_000)
    fun reset() { runner.reset(); mutableState.value = AiHistoryDetailState.Loading }
    fun load(read: suspend (String) -> AiHistoryEntity?) {
        runner.reset()
        runner.start(
            onStart = { mutableState.value = AiHistoryDetailState.Loading },
            operation = read,
            onSuccess = { mutableState.value = if (it == null) AiHistoryDetailState.Missing else AiHistoryDetailState.Ready(it) },
            onError = { mutableState.value = AiHistoryDetailState.Failed }
        )
    }
}
