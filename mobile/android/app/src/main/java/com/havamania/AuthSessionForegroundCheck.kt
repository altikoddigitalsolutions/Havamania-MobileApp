package com.havamania

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Cached offline sessions remain usable; permanent server rejection is checked on foreground. */
@Composable
internal fun AuthSessionForegroundCheck(
    model: AuthViewModel, networkAvailable: (() -> Boolean)? = null, profileModel: ProfileViewModel? = null
) {
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val readyEpoch by model.sessionReadyEpoch.collectAsStateWithLifecycle()
    LaunchedEffect(model, profileModel, readyEpoch) {
        if (readyEpoch > 0) profileModel?.reconcileAfterSessionReady()
    }
    DisposableEffect(model, owner, networkAvailable) {
        fun validate() {
            val online = networkAvailable?.invoke() ?: runCatching {
                val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                manager.getNetworkCapabilities(manager.activeNetwork)
                    ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            }.getOrDefault(false)
            if (online) model.refreshSession()
        }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) validate() }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) validate()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
