package com.havamania

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.LocalDate

// Store identifiers and small creation parameters, never analysis/history payloads.
@Serializable
internal data class TravelEditorContext(
    val owner: String,
    val open: Boolean = false,
    val editId: String? = null,
    val initialHandled: Boolean = false,
    val prefillCity: String? = null,
    val prefillType: String? = null,
    val prefillDate: String? = null,
    val prefillId: String? = null
) {
    fun prefill(): TravelPlan? {
        val city = prefillCity ?: return null
        val date = LocalDate.parse(requireNotNull(prefillDate))
        return TravelPlan(id = requireNotNull(prefillId), city = city,
            tripType = TripType.valueOf(requireNotNull(prefillType)), startDate = date, endDate = date)
    }

    fun close() = TravelEditorContext(owner, initialHandled = initialHandled)
    fun create() = copy(open = true, editId = null, prefillCity = null, prefillType = null, prefillDate = null, prefillId = null)
    fun edit(id: String) = create().copy(editId = id)
}

@Composable
internal fun rememberTravelEditor(owner: String): MutableState<TravelEditorContext> {
    val saver = remember(owner) {
        Saver<TravelEditorContext, String>(
            save = { Json.encodeToString(it) },
            restore = { encoded ->
                runCatching { Json.decodeFromString<TravelEditorContext>(encoded) }.getOrNull()
                    ?.takeIf { it.owner == owner } ?: TravelEditorContext(owner)
            }
        )
    }
    return rememberSaveable(owner, stateSaver = saver) { mutableStateOf(TravelEditorContext(owner)) }
}
