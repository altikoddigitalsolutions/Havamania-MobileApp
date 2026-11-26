package com.havamania

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

/** Shared by the real chat and the offline device fixture. */
@Composable
internal fun rememberAssistantPhotoPicker(
    selectionToken: () -> String,
    onPhotos: (List<Uri>, String) -> Unit,
    onUnavailable: () -> Unit
): () -> Unit {
    var pendingToken by rememberSaveable { mutableStateOf<String?>(null) }
    val currentPhotos by rememberUpdatedState(onPhotos)
    val currentUnavailable by rememberUpdatedState(onUnavailable)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(ASSISTANT_MAX_PHOTOS)
    ) { uris ->
        val token = pendingToken
        pendingToken = null
        if (token != null) currentPhotos(uris, token)
    }
    return open@ {
        if (pendingToken != null) return@open
        pendingToken = selectionToken()
        assistantPhotoPickerLaunch(
            launch = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onUnavailable = { pendingToken = null; currentUnavailable() }
        )
    }
}

internal fun assistantPhotoPickerLaunch(launch: () -> Unit, onUnavailable: () -> Unit) {
    try {
        launch()
    } catch (_: ActivityNotFoundException) {
        onUnavailable()
    } catch (_: SecurityException) {
        onUnavailable()
    }
}
