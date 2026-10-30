package com.havamania

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

@Serializable
data class AssistantHealthConsent(val version: Int, val text: String, val text_hash: String)

@Serializable
data class AssistantConsentDocument(
    val version: Int, val text: String, val text_hash: String,
    val health: AssistantHealthConsent? = null,
    val accepted: Boolean = false, val health_accepted: Boolean = false,
    val health_review_required: Boolean = false
) {
    fun isValid(): Boolean = version > 0 && text.isNotBlank() && text_hash.matches(Regex("[0-9a-f]{64}")) &&
        (health == null || (health.version > 0 && health.text.isNotBlank() && health.text_hash.matches(Regex("[0-9a-f]{64}"))))
}

@Serializable
data class AssistantConsentUpdate(
    val assistant_accepted: Boolean,
    val consent_version: Int? = null, val consent_hash: String? = null,
    val health_accepted: Boolean = false,
    val health_consent_version: Int? = null, val health_consent_hash: String? = null
)

internal data class AssistantConsentState(
    val document: AssistantConsentDocument? = null, val busy: Boolean = false,
    val visible: Boolean = false, val error: String? = null
) {
    val allowed: Boolean get() = !busy && document?.let { it.isValid() && it.accepted } == true
}

internal interface AssistantConsentGateway {
    suspend fun load(owner: String): AssistantConsentDocument
    suspend fun save(owner: String, document: AssistantConsentDocument, health: Boolean): AssistantConsentDocument
    suspend fun revoke(owner: String)
}

internal class AssistantConsentStaleException : Exception()
internal class AssistantConsentRequiredException : Exception()

internal class AssistantConsentController(
    scope: CoroutineScope, private val uid: () -> String,
    private val gateway: AssistantConsentGateway = AssistantConsentRepository(),
    private val timeoutMillis: Long = 60_000
) {
    private val tasks = AccountTaskScope(scope, uid)
    private var operation: Job? = null
    private val mutable = MutableStateFlow(AssistantConsentState())
    val state = mutable.asStateFlow()

    private suspend fun <T> timed(block: suspend () -> T): T = try {
        withTimeout(timeoutMillis) { block() }
    } catch (e: TimeoutCancellationException) {
        currentCoroutineContext().ensureActive()
        throw IllegalStateException("Consent operation timed out")
    }

    fun reset() { tasks.reset(); operation?.cancel(); mutable.value = AssistantConsentState() }
    fun close() { tasks.close() }
    fun dismiss() { if (!mutable.value.busy) mutable.value = mutable.value.copy(visible = false) }
    fun open() = load(review = true)

    fun load(review: Boolean = false, showLoading: Boolean = review) {
        operation?.cancel()
        mutable.value = AssistantConsentState(busy = true, visible = showLoading)
        operation = tasks.launch { owner ->
            try {
                val doc = timed { gateway.load(owner) }
                ensureActive()
                require(doc.isValid())
                mutable.value = AssistantConsentState(document = doc, visible = review || !doc.accepted || doc.health_review_required)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                ensureActive()
                mutable.value = AssistantConsentState(visible = true,
                    error = "Asistan izin metni alınamadı. Bağlantınızı kontrol edip tekrar deneyin.")
            }
        }
    }

    fun save(health: Boolean) {
        val current = mutable.value
        val doc = current.document ?: return
        if (current.busy || !doc.isValid()) return
        mutable.value = current.copy(busy = true, error = null)
        operation = tasks.launch { owner ->
            try {
                val updated = timed { gateway.save(owner, doc, health && doc.health != null) }
                ensureActive()
                require(updated.isValid())
                mutable.value = AssistantConsentState(document = updated, visible = !updated.accepted)
            } catch (e: CancellationException) { throw e }
            catch (e: AssistantConsentStaleException) {
                try {
                    val updated = timed { gateway.load(owner) }
                    ensureActive()
                    require(updated.isValid())
                    mutable.value = AssistantConsentState(document = updated.copy(accepted = false, health_accepted = false),
                        visible = true, error = "Metin güncellendi. Lütfen yeni metni okuyup yeniden karar verin.")
                } catch (cancel: CancellationException) { throw cancel }
                catch (failure: Exception) {
                    ensureActive()
                    mutable.value = AssistantConsentState(visible = true, error = "Güncel metin alınamadı. Tekrar deneyin.")
                }
            } catch (e: Exception) {
                ensureActive()
                mutable.value = current.copy(visible = true, error = "İzin kaydedilemedi. Lütfen tekrar deneyin.")
            }
        }
    }

    fun revoke() {
        if (mutable.value.busy) return
        val current = mutable.value
        mutable.value = current.copy(busy = true, error = null)
        operation = tasks.launch { owner ->
            try {
                timed { gateway.revoke(owner) }
                ensureActive()
                mutable.value = AssistantConsentState(document = current.document?.copy(accepted = false, health_accepted = false))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                ensureActive()
                mutable.value = current.copy(visible = true, error = "İzin geri çekilemedi. Lütfen tekrar deneyin.")
            }
        }
    }
}
