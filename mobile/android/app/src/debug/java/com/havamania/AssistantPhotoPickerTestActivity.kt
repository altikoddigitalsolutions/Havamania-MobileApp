package com.havamania

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.havamania.ui.theme.*

/** Offline fixture: no account, repository or network calls. */
class AssistantPhotoPickerTestActivity : ComponentActivity() {
    var photos by mutableStateOf<List<AssistantPhoto>>(emptyList())
        private set
    var returns by mutableStateOf(0)
        private set
    var lastToken: String? = null
        private set
    var expectedName: String? = null
    var expectedMediaId: String? = null
    var openPickerForTest: (() -> Unit)? = null
    var owner by mutableStateOf("fixture-A")
    var error by mutableStateOf<String?>(null)
        private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scope = rememberCoroutineScope()
            val open = rememberAssistantPhotoPicker({ owner }, { uris, token ->
                returns++
                lastToken = token
                if (token == owner && uris.isNotEmpty()) scope.launch {
                    val allowed = expectedName
                    val matches = allowed != null && uris.all { uri ->
                        val localPickerId = uri.authority == "media" &&
                            uri.pathSegments.any { it == "com.android.providers.media.photopicker" } &&
                            expectedMediaId != null && uri.lastPathSegment == expectedMediaId
                        localPickerId || contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                            it.moveToFirst() && it.getString(0) == allowed
                        } == true
                    }
                    if (!matches) { error = "Unexpected fixture selection"; return@launch }

                    try {
                        photos = withContext(Dispatchers.IO) {
                            uris.take(ASSISTANT_MAX_PHOTOS).map { prepareAssistantPhoto(contentResolver, it) }
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                    catch (_: Exception) { error = "Fixture photo preparation failed" }
                }
            }, { error = "Picker unavailable" })
            SideEffect { openPickerForTest = open }
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                ProvideResponsiveLayout { MaterialTheme { Column {
                    ChatInput("", {}, {}, false, colors, HavamaniaTheme.styles, photos = photos,
                        onAttachPhoto = open, onRemovePhoto = { id -> photos = photos.filterNot { it.id == id } }, photoError = error)
                    Text("Picker returns: $returns")
                } } }
            }
        }
    }
}
