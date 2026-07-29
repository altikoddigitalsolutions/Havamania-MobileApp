package com.havamania

sealed interface SaveResult {
    data object Success : SaveResult
    data object Unauthenticated : SaveResult
    data object PermissionDenied : SaveResult
    data object NetworkError : SaveResult
    data object ValidationError : SaveResult
    data class UnknownError(val message: String) : SaveResult
}
