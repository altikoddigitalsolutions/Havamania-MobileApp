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

internal enum class ConsentOperation { LOAD, SAVE, REVOKE }

internal data class AssistantConsentState(
    val document: AssistantConsentDocument? = null, val busy: Boolean = false,
    val visible: Boolean = false, val error: String? = null,
    val retry: ConsentRetry? = null, val notice: String? = null, val withdrawalPending: Boolean = false,
    val operation: ConsentOperation? = null, val attempt: Int = 0
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
    private var stateOwner: String? = null
    private val mutable = MutableStateFlow(AssistantConsentState())
    val state = mutable.asStateFlow()
    private var dismissed = false
    private var reviewRequested = false
    private var lastOperation: ConsentOperation? = null
    private var attempt = 0

    private fun beginOperation(kind: ConsentOperation) {
        attempt = if (lastOperation == kind) attempt + 1 else 1
        lastOperation = kind
    }

    private fun publish(next: AssistantConsentState) {
        mutable.value = next.copy(visible = next.visible && !dismissed, attempt = attempt,
            operation = if (next.busy) next.operation else null)
    }

    private suspend fun <T> timed(block: suspend () -> T): T = try {
        withTimeout(timeoutMillis) { block() }
    } catch (e: TimeoutCancellationException) {
        currentCoroutineContext().ensureActive()
        throw AssistantConsentServiceException(ConsentFailure.TIMEOUT)
    }

    private suspend fun ensureCurrentOwner(owner: String) {
        currentCoroutineContext().ensureActive()
        if (uid() != owner) throw CancellationException("Consent account changed")
    }

    fun reset() { tasks.reset(); operation?.cancel(); stateOwner = null; dismissed = false; lastOperation = null; attempt = 0; mutable.value = AssistantConsentState() }
    fun close() { tasks.close() }
    fun dismiss() { dismissed = true; mutable.value = mutable.value.copy(visible = false) }
    fun open() = load(review = true)
    fun retry() {
        if (!hasCurrentOwner()) return
        if (mutable.value.retry == ConsentRetry.REVOKE || mutable.value.withdrawalPending) revoke() else open()
    }

    private fun hasCurrentOwner(): Boolean {
        if (stateOwner == uid()) return true
        reset()
        return false
    }

    fun load(review: Boolean = false, showLoading: Boolean = review) {
        val owner = uid()
        if (stateOwner != owner) {
            reset()
            stateOwner = owner
        }
        if (mutable.value.busy) {
            if (review || showLoading) {
                dismissed = false
                reviewRequested = reviewRequested || review
                publish(mutable.value.copy(visible = true))
            }
            return
        }
        dismissed = false
        if (mutable.value.withdrawalPending) {
            publish(mutable.value.copy(visible = true))
            return
        }
        operation?.cancel()
        reviewRequested = review
        beginOperation(ConsentOperation.LOAD)
        publish(AssistantConsentState(busy = true, visible = showLoading, operation = ConsentOperation.LOAD))
        operation = tasks.launch {
            try {
                ensureCurrentOwner(owner)
                val doc = timed { gateway.load(owner) }
                ensureCurrentOwner(owner)
                if (!doc.isValid()) throw AssistantConsentServiceException(ConsentFailure.INVALID)
                publish(AssistantConsentState(document = doc, visible = reviewRequested || !doc.accepted || doc.health_review_required))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                ensureCurrentOwner(owner)
                val pending = e is AssistantWithdrawalPendingException
                publish(AssistantConsentState(visible = true, withdrawalPending = pending,
                    retry = if (pending) ConsentRetry.REVOKE else ConsentRetry.LOAD,
                    error = "Asistan izin metni alınamadı. " + consentFailureMessage(e)))
            }
        }
    }

    fun save(health: Boolean) {
        val owner = stateOwner ?: return
        if (!hasCurrentOwner()) return
        val current = mutable.value
        val doc = current.document ?: return
        if (current.busy || current.withdrawalPending || !doc.isValid()) return
        dismissed = false
        beginOperation(ConsentOperation.SAVE)
        publish(current.copy(busy = true, error = null, operation = ConsentOperation.SAVE))
        operation = tasks.launch {
            try {
                ensureCurrentOwner(owner)
                val updated = timed { gateway.save(owner, doc, health && doc.health != null) }
                ensureCurrentOwner(owner)
                require(updated.isValid())
                publish(AssistantConsentState(document = updated, visible = !updated.accepted))
            } catch (e: CancellationException) { throw e }
            catch (e: AssistantConsentStaleException) {
                ensureCurrentOwner(owner)
                try {
                    val updated = timed { gateway.load(owner) }
                    ensureCurrentOwner(owner)
                    require(updated.isValid())
                    publish(AssistantConsentState(document = updated.copy(accepted = false, health_accepted = false),
                        visible = true, error = "Metin güncellendi. Lütfen yeni metni okuyup yeniden karar verin."))
                } catch (cancel: CancellationException) { throw cancel }
                catch (failure: Exception) {
                    ensureCurrentOwner(owner)
                    publish(AssistantConsentState(visible = true, retry = ConsentRetry.LOAD,
                        error = "Güncel metin alınamadı. " + consentFailureMessage(failure)))
                }
            } catch (e: Exception) {
                ensureCurrentOwner(owner)
                val pending = e is AssistantWithdrawalPendingException
                publish(current.copy(visible = true, withdrawalPending = pending,
                    retry = if (pending) ConsentRetry.REVOKE else null,
                    error = "İzin kaydedilemedi. " + consentFailureMessage(e)))
            }
        }
    }

    fun revoke() {
        val owner = stateOwner ?: return
        if (!hasCurrentOwner()) return
        if (mutable.value.busy) return
        val current = mutable.value
        val blocked = current.copy(document = current.document?.copy(accepted = false, health_accepted = false),
            withdrawalPending = true, retry = ConsentRetry.REVOKE, notice = null)
        dismissed = false
        beginOperation(ConsentOperation.REVOKE)
        publish(blocked.copy(busy = true, error = null, visible = true, operation = ConsentOperation.REVOKE))
        operation = tasks.launch {
            try {
                ensureCurrentOwner(owner)
                timed { gateway.revoke(owner) }
                ensureCurrentOwner(owner)
                publish(AssistantConsentState(document = blocked.document, visible = true,
                    notice = "Uzak asistan izinleriniz geri çekildi."))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                ensureCurrentOwner(owner)
                publish(blocked.copy(busy = false, visible = true,
                    error = "İzinlerin geri çekildiği doğrulanamadı. " + consentFailureMessage(e)))
            }
        }
    }
}
