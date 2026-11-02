package com.havamania

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal enum class FcmGatewayAction { APPLIED, RETRY, STOP, SUPERSEDED, AUTH_REQUIRED }
internal data class FcmGatewayResult(val action: FcmGatewayAction, val retryAfterSeconds: Long = 0)

internal suspend fun sendFcmTokenWithAuth(
    service: BackendFcmService, desired: FcmTokenRegistration, expectedOwner: String,
    authorization: suspend (forceRefresh: Boolean) -> String?, isCurrent: () -> Boolean
): FcmGatewayResult {
    if (desired.authRequired) return FcmGatewayResult(FcmGatewayAction.AUTH_REQUIRED)
    if (!desired.pending || desired.token == null || desired.ownerUid == null || expectedOwner.isBlank()) return FcmGatewayResult(FcmGatewayAction.STOP)
    var awaitingResponse = false
    try {
        for (attempt in 0..1) {
            currentCoroutineContext().ensureActive()
            if (!isCurrent()) return FcmGatewayResult(FcmGatewayAction.SUPERSEDED)
            val bearer = authorization(attempt == 1)
            currentCoroutineContext().ensureActive()
            if (!isCurrent()) return FcmGatewayResult(FcmGatewayAction.SUPERSEDED)
            if (bearer == null) return FcmGatewayResult(FcmGatewayAction.RETRY, 30)
            if (!bearer.startsWith("Bearer ") || bearer.removePrefix("Bearer ").isBlank() || bearer.any(Char::isISOControl))
                return FcmGatewayResult(FcmGatewayAction.STOP)
            awaitingResponse = true
            val response = service.registerPushToken(bearer, BackendFcmRegistration(
                desired.token, desired.installationId, desired.revision
            ))
            currentCoroutineContext().ensureActive()
            if (!isCurrent()) return FcmGatewayResult(FcmGatewayAction.SUPERSEDED)
            awaitingResponse = false
            when (response.code()) {
                401 -> if (attempt == 0) continue else return FcmGatewayResult(FcmGatewayAction.STOP)
                429 -> {
                    val delay = response.headers()["Retry-After"]?.toLongOrNull()?.takeIf { it in 1..86400 } ?: 60
                    return FcmGatewayResult(FcmGatewayAction.RETRY, delay)
                }
                200 -> return FcmGatewayResult(if (matchingFcmReceipt(response.body(), desired, expectedOwner))
                    FcmGatewayAction.APPLIED else FcmGatewayAction.STOP)
                408, in 500..599 -> return FcmGatewayResult(FcmGatewayAction.RETRY, 30)
                else -> return FcmGatewayResult(FcmGatewayAction.STOP)
            }
        }
    } catch (error: CancellationException) { throw error }
    catch (_: FcmAuthRequiredException) {
        return FcmGatewayResult(if (isCurrent()) FcmGatewayAction.AUTH_REQUIRED else FcmGatewayAction.SUPERSEDED)
    }
    catch (_: kotlinx.serialization.SerializationException) { return FcmGatewayResult(FcmGatewayAction.STOP) }
    catch (_: Exception) {
        return FcmGatewayResult(if (isCurrent()) FcmGatewayAction.RETRY else FcmGatewayAction.SUPERSEDED,
            if (awaitingResponse) 0 else 30)
    }
    return FcmGatewayResult(FcmGatewayAction.STOP)
}
