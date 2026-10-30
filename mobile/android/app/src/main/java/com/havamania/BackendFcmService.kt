package com.havamania

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@Serializable
data class BackendFcmRegistration(
    val token: String, val installation_id: String, val revision: Long,
    val platform: String = "android", val owner_protocol: Int = 1
) {
    override fun toString() = "BackendFcmRegistration(revision=$revision)"
}

@Serializable
data class BackendFcmReceipt(
    val id: String, val user_id: String, val platform: String, val token: String,
    val installation_id: String? = null, val revision: Long? = null, val owner_protocol: Int = 0
) {
    override fun toString() = "BackendFcmReceipt(revision=$revision, owner_protocol=$owner_protocol)"
}

interface BackendFcmService {
    @POST("v1/devices/push-token")
    suspend fun registerPushToken(
        @Header("Authorization") authorization: String,
        @Body request: BackendFcmRegistration
    ): Response<BackendFcmReceipt>
}

/** No queue acknowledgement here: the journal rechecks its snapshot after this returns. */
internal suspend fun registerFcmTokenSnapshot(
    service: BackendFcmService, desired: FcmTokenRegistration,
    expectedBackendOwner: String, authorization: String,
    isCurrent: () -> Boolean
): Boolean {
    if (!desired.pending || desired.ownerUid == null || desired.token == null ||
        expectedBackendOwner.isBlank() || !authorization.startsWith("Bearer ") ||
        authorization.removePrefix("Bearer ").isBlank() || authorization.any(Char::isISOControl)
    ) return false
    currentCoroutineContext().ensureActive()
    if (!isCurrent()) return false
    val response = service.registerPushToken(authorization, BackendFcmRegistration(
        token = desired.token, installation_id = desired.installationId, revision = desired.revision
    ))
    currentCoroutineContext().ensureActive()
    if (!isCurrent() || response.code() != 200) return false
    return matchingFcmReceipt(response.body(), desired, expectedBackendOwner)
}

internal fun matchingFcmReceipt(receipt: BackendFcmReceipt?, desired: FcmTokenRegistration, expectedBackendOwner: String): Boolean {
    return receipt != null && expectedBackendOwner.isNotBlank() && receipt.id.isNotBlank() && receipt.user_id == expectedBackendOwner &&
        receipt.platform == "android" && receipt.token == desired.token &&
        receipt.installation_id == desired.installationId && receipt.revision == desired.revision &&
        receipt.owner_protocol == 1
}
