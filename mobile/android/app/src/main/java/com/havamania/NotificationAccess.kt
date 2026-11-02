package com.havamania

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal fun notificationPostingAllowed(sdk: Int, runtimeGranted: Boolean, systemAllowed: Boolean): Boolean =
    systemAllowed && (sdk < 33 || runtimeGranted)

@Composable
internal fun rememberSystemNotificationAccess(
    readPermission: (Context) -> Boolean = PermissionHelper::hasNotificationPermission
): State<Boolean> {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val reader by rememberUpdatedState(readPermission)
    val allowed = remember(context) { mutableStateOf(readPermission(context)) }
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) allowed.value = reader(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return allowed
}

internal fun openSystemNotificationSettings(context: Context): Boolean {
    val intents = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) add(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        )
        add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
    }
    for (intent in intents) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return true
        } catch (_: android.content.ActivityNotFoundException) { }
        catch (_: SecurityException) { }
    }
    return false
}
