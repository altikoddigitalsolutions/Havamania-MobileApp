package com.havamania

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

/** Bounded session-only storage; never serialize image bytes into chat history. */
internal fun retainAssistantPreviews(existing: Map<String, List<AssistantPhoto>>, messageId: String,
    photos: List<AssistantPhoto>, maxBytes: Long = 16L * 1024 * 1024): Map<String, List<AssistantPhoto>> {
    // An unretainable batch must not evict unrelated or previously shown images.
    if (photos.sumOf { it.bytes.size.toLong() } > maxBytes) return existing
    val result = LinkedHashMap(existing)
    result.remove(messageId)
    result[messageId] = photos.toList()
    fun size() = result.values.sumOf { batch -> batch.sumOf { it.bytes.size.toLong() } }
    while (result.isNotEmpty() && size() > maxBytes) result.remove(result.keys.first())
    return result
}

@Composable
internal fun AssistantMessagePhotos(photos: List<AssistantPhoto>) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    fun request(photo: AssistantPhoto) = ImageRequest.Builder(context).data(photo.bytes)
        .memoryCachePolicy(CachePolicy.DISABLED).diskCachePolicy(CachePolicy.DISABLED).build()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
            AsyncImage(model = request(photo), contentDescription = "Fotoğraf ${index + 1}, büyüt",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(112.dp).clip(RoundedCornerShape(12.dp))
                    .clickable(onClickLabel = "Fotoğrafı büyüt") { selectedId = photo.id })
        }
    }
    photos.firstOrNull { it.id == selectedId }?.let { photo ->
        Dialog(onDismissRequest = { selectedId = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding()) {
                TextButton(onClick = { selectedId = null }) { Text("Fotoğrafı kapat", color = Color.White) }
                AsyncImage(model = request(photo), contentDescription = "Büyütülmüş fotoğraf",
                    contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}
