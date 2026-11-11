package com.havamania

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

internal val LocalTravelGuideRefresher = staticCompositionLocalOf<suspend (Context, String, String?) -> Unit> {
    { context, city, district -> TravelGuideUpdates.refresh(context, city, district) }
}

@Serializable
internal data class PublishedGuideExperience(
    val title: String, val category: String, val description: String, val sourceUrl: String,
    val tripTypes: List<String> = emptyList()
)

@Serializable
internal data class PublishedTravelGuide(
    val city: String, val district: String? = null, val version: Long,
    val validUntilEpochMs: Long, val description: String, val visit: String,
    val food: String, val experience: String, val experiences: List<PublishedGuideExperience>
) {
    fun validFor(city: String, district: String?, now: Long): Boolean =
        guideLocationKey(this.city, this.district) == guideLocationKey(city, district) &&
        version > 0 && validUntilEpochMs > now &&
        listOf(description, visit, food, experience).all { it.isNotBlank() && it.length <= 4000 } &&
        experiences.size in 1..12 && experiences.all {
            it.title.isNotBlank() && it.title.length <= 160 && it.category.isNotBlank() && it.category.length <= 80 &&
                it.description.isNotBlank() && it.description.length <= 2000 &&
                it.tripTypes.all { name -> TripType.entries.any { type -> type.name == name } } &&
                officialGuideUrl(it.sourceUrl)
        }

    fun guide() = LocalTravelGuide(description, visit, food, experience)
    fun selected(type: TripType?) = experiences.map {
        TravelGuideExperience(it.title, it.category, it.description, it.sourceUrl,
            it.tripTypes.map(TripType::valueOf).toSet())
    }.sortedByDescending { type in it.preferredTripTypes }.take(2)
}

internal fun officialGuideUrl(value: String): Boolean = runCatching {
    val uri = URI(value)
    val host = uri.host?.lowercase().orEmpty()
    value.length <= 2048 && uri.scheme == "https" && uri.userInfo == null &&
        (uri.port == -1 || uri.port == 443) &&
        (host == "goturkiye.com" || host.endsWith(".gov.tr") || host.endsWith(".bel.tr"))
}.getOrDefault(false)

internal fun guideLocationKey(city: String, district: String?): String {
    val cityKey = LocalTravelGuides.key(city)
    val districtKey = district?.let(LocalTravelGuides::key)?.takeIf { it.isNotBlank() && it != cityKey }
    return "$cityKey|${districtKey.orEmpty()}"
}

/** Public editorial data only. No UID, location coordinates or profile is sent. */
internal object TravelGuideUpdates {
    private val json = Json { ignoreUnknownKeys = true }
    private val entries = ConcurrentHashMap<String, PublishedTravelGuide>()
    private val attempts = ConcurrentHashMap<String, Long>()
    private val revision = MutableStateFlow(0L)
    val changes = revision.asStateFlow()

    fun current(city: String, district: String?): PublishedTravelGuide? =
        entries[guideLocationKey(city, district)]?.takeIf { it.validUntilEpochMs > System.currentTimeMillis() }

    suspend fun refresh(context: Context, city: String, district: String?) = withContext(Dispatchers.IO) {
        val key = guideLocationKey(city, district)
        val id = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val prefs = context.applicationContext.getSharedPreferences("published_travel_guides", Context.MODE_PRIVATE)
        fun accept(payload: String): Boolean {
            if (payload.length > 64_000) return false
            val guide = runCatching { json.decodeFromString<PublishedTravelGuide>(payload) }.getOrNull() ?: return false
            if (!guide.validFor(city, district, System.currentTimeMillis())) return false
            val previous = entries[key]
            if (previous != null && previous.version >= guide.version) return false
            entries[key] = guide
            if (entries.size > 64) entries.keys.firstOrNull { it != key }?.let { entries.remove(it) }
            revision.update { it + 1 }
            return true
        }
        prefs.getString(id, null)?.let(::accept)
        revision.update { it + 1 }
        val now = android.os.SystemClock.elapsedRealtime()
        synchronized(attempts) {
            if (attempts[key]?.let { now - it < 15 * 60_000 } == true) return@withContext
            attempts[key] = now
            if (attempts.size > 64) attempts.keys.firstOrNull { it != key }?.let { attempts.remove(it) }
        }
        try {
            val payload = withTimeout(10_000) {
                FirebaseFirestore.getInstance().collection("travel_guides").document(id)
                    .get(Source.SERVER).await().getString("payload")
            }
            if (payload != null && accept(payload)) {
                val edit = prefs.edit().putString(id, payload)
                if (prefs.all.size >= 64 && !prefs.contains(id)) prefs.all.keys.firstOrNull()?.let(edit::remove)
                edit.apply()
            }
        } catch (e: Exception) {
            if (e is CancellationException && e !is kotlinx.coroutines.TimeoutCancellationException) throw e
            // Keep the bundled/last valid guide when offline or not yet published.
        }
        revision.update { it + 1 } // Also removes expired content from the currently visible guide.
    }
}
