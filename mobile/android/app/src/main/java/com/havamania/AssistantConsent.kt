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
    val health_review_required: Boolean = false,
    @kotlinx.serialization.Transient val health_sync_required: Boolean = false
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
    val visible: Boolean = false, val error: String? = null,
    val retry: ConsentRetry? = null, val notice: String? = null, val withdrawalPending: Boolean = false
) {
    val allowed: Boolean get() = !busy && !withdrawalPending && document?.let { it.isValid() && it.accepted } == true
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
    private val gateway: AssistantConsentGateway = defaultAssistantConsentGateway(),
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
        throw AssistantConsentServiceException(ConsentFailure.TIMEOUT)
    }

    fun reset() { tasks.reset(); operation?.cancel(); mutable.value = AssistantConsentState() }
    fun close() { tasks.close() }
    fun dismiss() { if (!mutable.value.busy) mutable.value = mutable.value.copy(visible = false) }
    fun open() = load(review = true)
    fun retry() { if (mutable.value.retry == ConsentRetry.REVOKE || mutable.value.withdrawalPending) revoke() else open() }

    fun load(review: Boolean = false, showLoading: Boolean = review) {
        if (mutable.value.withdrawalPending) {
            mutable.value = mutable.value.copy(visible = true)
            return
        }
        operation?.cancel()
        mutable.value = AssistantConsentState(busy = true, visible = showLoading)
        operation = tasks.launch { owner ->
            try {
                val doc = timed { gateway.load(owner) }
                ensureActive()
                if (!doc.isValid()) throw AssistantConsentServiceException(ConsentFailure.INVALID)
                mutable.value = AssistantConsentState(document = doc, visible = review || !doc.accepted || doc.health_review_required)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                ensureActive()
                val pending = e is AssistantWithdrawalPendingException
                mutable.value = AssistantConsentState(visible = true, withdrawalPending = pending,
                    retry = if (pending) ConsentRetry.REVOKE else ConsentRetry.LOAD,
                    error = "Asistan izin metni alınamadı. " + consentFailureMessage(e))
            }
        }
    }

    fun save(health: Boolean) {
        val current = mutable.value
        val doc = current.document ?: return
        if (current.busy || current.withdrawalPending || !doc.isValid()) return
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
                    mutable.value = AssistantConsentState(visible = true, retry = ConsentRetry.LOAD,
                        error = "Güncel metin alınamadı. " + consentFailureMessage(failure))
                }
            } catch (e: Exception) {
                ensureActive()
                val pending = e is AssistantWithdrawalPendingException
                mutable.value = current.copy(visible = true, withdrawalPending = pending,
                    retry = if (pending) ConsentRetry.REVOKE else null,
                    error = "İzin kaydedilemedi. " + consentFailureMessage(e))
            }
        }
    }

    fun revoke() {
        if (mutable.value.busy) return
        val current = mutable.value
        val blocked = current.copy(document = current.document?.copy(accepted = false, health_accepted = false),
            withdrawalPending = true, retry = ConsentRetry.REVOKE, notice = null)
        mutable.value = blocked.copy(busy = true, error = null, visible = true)
        operation = tasks.launch { owner ->
            try {
                timed { gateway.revoke(owner) }
                ensureActive()
                mutable.value = AssistantConsentState(document = blocked.document, visible = true,
                    notice = "Uzak asistan izinleriniz geri çekildi.")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                ensureActive()
                mutable.value = blocked.copy(busy = false, visible = true,
                    error = "İzinlerin geri çekildiği doğrulanamadı. " + consentFailureMessage(e))
            }
        }
    }
}
