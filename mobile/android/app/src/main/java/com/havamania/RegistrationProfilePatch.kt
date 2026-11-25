package com.havamania

/** Recovery initializes missing fields, never replays a stale draft over an existing profile. */
internal fun registrationProfilePatch(uid: String, name: String, email: String,
    existing: Map<String, Any?>, recovering: Boolean, now: Long): Map<String, Any> {
    val desired = mutableMapOf<String, Any>("uid" to uid)
    if (!recovering || (existing["name"] as? String).isNullOrBlank()) desired["name"] = name
    if (!recovering || (existing["email"] as? String).isNullOrBlank()) desired["email"] = email
    if (!existing.containsKey("createdAt")) desired["createdAt"] = now
    val changed = desired.filter { (key, value) -> existing[key] != value }
    return if (changed.isEmpty()) emptyMap() else changed + ("updatedAt" to now)
}