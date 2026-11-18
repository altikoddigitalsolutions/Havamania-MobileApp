package com.havamania

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/** Transient confirmations belong to one account and are never restored as pending actions. */
internal class SettingsDialogState {
    val theme = mutableStateOf(false)
    val language = mutableStateOf(false)
    val tone = mutableStateOf(false)
    val delete = mutableStateOf(false)
    val logout = mutableStateOf(false)
    val comingSoon = mutableStateOf(false)
    val title = mutableStateOf("")
}

@Composable
internal fun rememberSettingsDialogs(owner: String?): SettingsDialogState = remember(owner) { SettingsDialogState() }