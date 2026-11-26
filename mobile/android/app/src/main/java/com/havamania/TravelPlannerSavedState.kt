package com.havamania

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.LocalDate

@Serializable
internal data class TravelPlannerSavedState(val owner: String, val date: String,
    val filter: TravelFilter = TravelFilter.UPCOMING, val handledFocusId: String? = null)

internal fun restoreTravelPlannerState(owner: String, encoded: String, today: LocalDate): TravelPlannerSavedState =
    runCatching { Json.decodeFromString<TravelPlannerSavedState>(encoded) }
        .getOrNull()?.takeIf { it.owner == owner && runCatching { LocalDate.parse(it.date) }.isSuccess }
        ?: TravelPlannerSavedState(owner, today.toString())

@Composable
internal fun rememberTravelPlannerState(owner: String, session: Long, today: LocalDate): MutableState<TravelPlannerSavedState> {
    val saver = remember(owner) {
        Saver<TravelPlannerSavedState, String>(save = { Json.encodeToString(it) },
            restore = { restoreTravelPlannerState(owner, it, today) })
    }
    val state = rememberSaveable(owner, stateSaver = saver) { mutableStateOf(TravelPlannerSavedState(owner, today.toString())) }
    var previousSession by remember(owner) { mutableLongStateOf(session) }
    LaunchedEffect(owner, session) {
        if (previousSession != 0L && previousSession != session) state.value = TravelPlannerSavedState(owner, today.toString())
        previousSession = session
    }
    return state
}
