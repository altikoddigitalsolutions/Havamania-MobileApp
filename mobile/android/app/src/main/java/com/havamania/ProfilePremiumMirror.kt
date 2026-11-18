package com.havamania

/** Display metadata only. Paid API authorization remains the backend subscription decision. */
internal fun readProfilePremiumMirror(fields: Map<String, Any?>): Boolean {
    fun value(key: String): Boolean? {
        if (!fields.containsKey(key)) return null
        return fields[key] as? Boolean ?: throw IllegalArgumentException("Invalid premium mirror type")
    }
    val canonical = value("isPremium")
    val legacy = value("premium")
    require(canonical == null || legacy == null || canonical == legacy) { "Conflicting premium mirrors" }
    return canonical ?: legacy ?: false
}

internal fun profilePremiumForSnapshot(fields: Map<String, Any?>, confirmed: Boolean,
    fromCache: Boolean, pendingWrites: Boolean): Boolean =
    if (fromCache || pendingWrites) confirmed else readProfilePremiumMirror(fields)
