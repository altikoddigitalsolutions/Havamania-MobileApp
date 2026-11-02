package com.havamania

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** The adapter must read and stage creation in one server transaction. */
internal suspend fun createMissingAccountProfile(
    owner: String, currentOwner: () -> String?, writesAllowed: () -> Boolean,
    createAtomically: suspend (() -> Boolean) -> Unit
) {
    val job = currentCoroutineContext()[kotlinx.coroutines.Job]
    currentCoroutineContext().ensureActive()
    fun isCurrent() = job?.isActive != false && owner == currentOwner() && writesAllowed()
    if (isCurrent()) createAtomically(::isCurrent)
}

/** Called again by the SDK on transaction retries; never overwrite an existing document. */
internal fun stageMissingAccountProfile(
    isCurrent: () -> Boolean, exists: () -> Boolean, create: () -> Unit
): Boolean {
    if (!isCurrent()) return false
    val present = exists()
    if (present || !isCurrent()) return false
    create()
    return true
}

internal suspend fun saveProfilePreferences(
    store: DataStore<Preferences>, writesAllowed: () -> Boolean,
    transform: (MutablePreferences) -> Unit
) {
    store.edit { prefs -> if (writesAllowed()) transform(prefs) }
}
