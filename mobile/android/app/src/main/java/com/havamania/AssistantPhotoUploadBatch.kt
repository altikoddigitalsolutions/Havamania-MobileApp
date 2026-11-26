package com.havamania

/** Only unfinished photos are uploaded. Commit each receipt before starting the next upload. */
internal suspend fun uploadAssistantPhotoBatch(
    expectedCount: Int,
    uploadedIds: List<String>,
    remaining: List<AssistantPhoto>,
    upload: suspend (AssistantPhoto) -> String,
    onUploaded: (AssistantPhoto, List<String>) -> Unit
): List<String> {
    if (expectedCount !in 0..ASSISTANT_MAX_PHOTOS || uploadedIds.size + remaining.size != expectedCount)
        throw TerminalAssistantException("Fotoğraf seçimi artık mevcut değil. Fotoğrafları yeni bir mesaja tekrar ekleyin.")
    var ids = uploadedIds.toList()
    for (photo in remaining) {
        ids = ids + upload(photo)
        onUploaded(photo, ids)
    }
    return ids
}
