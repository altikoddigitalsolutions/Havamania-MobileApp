package com.havamania

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal suspend fun createMissingAccountProfile(
    owner: String, currentOwner: () -> String?, writesAllowed: () -> Boolean,
    exists: suspend () -> Boolean, create: suspend () -> Unit
) {
    currentCoroutineContext().ensureActive()
    if (owner != currentOwner() || !writesAllowed()) return
    val present = exists()
    currentCoroutineContext().ensureActive()
    if (!present && owner == currentOwner() && writesAllowed()) create()
}

internal suspend fun saveProfilePreferences(
    store: DataStore<Preferences>, writesAllowed: () -> Boolean,
    transform: (MutablePreferences) -> Unit
) {
    store.edit { prefs -> if (writesAllowed()) transform(prefs) }
}
