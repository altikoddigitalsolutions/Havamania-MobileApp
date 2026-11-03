package com.havamania

/** Minimal owner fingerprint for local account-deletion fences; never an anonymity guarantee. */
internal fun accountOwnerHash(uid: String): String = java.security.MessageDigest.getInstance("SHA-256")
    .digest(uid.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
