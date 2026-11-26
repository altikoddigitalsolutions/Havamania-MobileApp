package com.havamania

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** Screen-session choices; callers must reset this state when account/session changes. */
internal data class TravelGuideSelection(
    val closedIds: Set<String> = emptySet(),
    val preferredId: String? = null
) {
    fun visibleId(ids: List<String>, autoOpen: Boolean): String? =
        if (preferredId != null && preferredId in ids) preferredId.takeUnless { it in closedIds }
        else if (autoOpen) ids.firstOrNull()?.takeUnless { it in closedIds } else null

    fun open(id: String) = copy(closedIds = closedIds - id, preferredId = id)
    fun close(id: String) = copy(closedIds = closedIds + id,
        preferredId = id)
}

@Serializable
private data class SavedGuideSelection(val owner: String, val closedIds: Set<String>, val preferredId: String?)

internal fun encodeGuideSelection(owner: String, value: TravelGuideSelection): String =
    Json.encodeToString(SavedGuideSelection(owner, value.closedIds, value.preferredId))

internal fun restoreGuideSelection(owner: String, encoded: String): TravelGuideSelection =
    runCatching { Json.decodeFromString<SavedGuideSelection>(encoded) }.getOrNull()
        ?.takeIf { it.owner == owner }
        ?.let { TravelGuideSelection(it.closedIds, it.preferredId) } ?: TravelGuideSelection()

@Composable
internal fun rememberTravelGuideSelection(owner: String, session: Long): MutableState<TravelGuideSelection> {
    val saver = remember(owner) {
        Saver<TravelGuideSelection, String>(save = { encodeGuideSelection(owner, it) },
            restore = { restoreGuideSelection(owner, it) })
    }
    val selection = rememberSaveable(owner, stateSaver = saver) { mutableStateOf(TravelGuideSelection()) }
    var previousSession by remember(owner) { mutableLongStateOf(session) }
    LaunchedEffect(owner, session) {
        if (previousSession != 0L && previousSession != session) selection.value = TravelGuideSelection()
        previousSession = session
    }
    return selection
}
