package com.havamania

internal fun isPermanentAuthSessionFailure(code: String?): Boolean = code in setOf(
    "ERROR_USER_DISABLED", "ERROR_USER_NOT_FOUND", "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN"
)
