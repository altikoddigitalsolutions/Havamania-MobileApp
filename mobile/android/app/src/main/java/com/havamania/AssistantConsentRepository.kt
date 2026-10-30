package com.havamania

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await
import retrofit2.Response

internal class AssistantConsentRepository(
    private val api: BackendChatbotService = BackendChatbotFactory.create()
) : AssistantConsentGateway {
    private suspend fun <T> call(owner: String, action: suspend (String) -> Response<T>): T {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser?.takeIf { it.uid == owner } ?: error("No session")
        var token = user.getIdToken(false).await()?.token ?: error("No token")
        currentCoroutineContext().ensureActive()
        check(auth.currentUser?.uid == owner)
        var response = action("Bearer $token")
        if (response.code() == 401) {
            token = user.getIdToken(true).await()?.token ?: error("No token")
            currentCoroutineContext().ensureActive()
            check(auth.currentUser?.uid == owner)
            response = action("Bearer $token")
        }
        currentCoroutineContext().ensureActive()
        check(auth.currentUser?.uid == owner)
        if (response.code() == 409) throw AssistantConsentStaleException()
        check(response.isSuccessful)
        return response.body() ?: error("Empty response")
    }

    override suspend fun load(owner: String): AssistantConsentDocument = call(owner) { api.consent(it) }
    override suspend fun save(owner: String, document: AssistantConsentDocument, health: Boolean): AssistantConsentDocument {
        call(owner) { api.updateConsent(it, AssistantConsentUpdate(true, document.version, document.text_hash,
            health, document.health?.version.takeIf { health }, document.health?.text_hash.takeIf { health })) }
        return load(owner)
    }
    override suspend fun revoke(owner: String) {
        call(owner) { api.updateConsent(it, AssistantConsentUpdate(assistant_accepted = false)) }
    }
}
