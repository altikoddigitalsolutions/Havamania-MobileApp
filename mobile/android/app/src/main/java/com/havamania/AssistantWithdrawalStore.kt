package com.havamania

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface AssistantWithdrawalStore {
    suspend fun pending(owner: String): Boolean
    suspend fun setPending(owner: String, value: Boolean)
    suspend fun healthBlocked(owner: String): Boolean
    suspend fun beginHealthUpdate(owner: String, allow: Boolean): Long
    suspend fun confirmHealthUpdate(owner: String, revision: Long, allow: Boolean): Boolean
}

internal class AndroidAssistantWithdrawalStore : AssistantWithdrawalStore {
    companion object { private val healthLock = Any() }
    private fun preferences() = com.google.firebase.FirebaseApp.getInstance().applicationContext
        .getSharedPreferences("assistant_local_privacy", Context.MODE_PRIVATE)
    override suspend fun pending(owner: String) = withContext(Dispatchers.IO) {
        preferences().getBoolean("withdraw_pending_$owner", false)
    }
    override suspend fun setPending(owner: String, value: Boolean) = withContext(Dispatchers.IO) {
        check(preferences().edit().apply {
            if (value) putBoolean("withdraw_pending_$owner", true) else remove("withdraw_pending_$owner")
        }.commit()) { "Privacy intent could not be persisted" }
    }
    override suspend fun healthBlocked(owner: String) = withContext(Dispatchers.IO) {
        preferences().getBoolean("health_blocked_$owner", false)
    }
    override suspend fun beginHealthUpdate(owner: String, allow: Boolean): Long = withContext(Dispatchers.IO) {
        synchronized(healthLock) {
            val prefs = preferences()
            val previous = prefs.getLong("health_revision_$owner", 0)
            check(previous < Long.MAX_VALUE)
            val next = previous + 1
            check(prefs.edit().putLong("health_revision_$owner", next).apply {
                if (!allow) putBoolean("health_blocked_$owner", true)
            }.commit()) { "Health privacy intent could not be persisted" }
            next
        }
    }
    override suspend fun confirmHealthUpdate(owner: String, revision: Long, allow: Boolean): Boolean = withContext(Dispatchers.IO) {
        synchronized(healthLock) {
            val prefs = preferences()
            if (prefs.getLong("health_revision_$owner", 0) != revision) false
            else {
                check(prefs.edit().putBoolean("health_blocked_$owner", !allow).commit())
                true
            }
        }
    }
}

internal class WithdrawalTrackingConsentGateway(private val delegate: AssistantConsentGateway,
    private val store: AssistantWithdrawalStore) : AssistantConsentGateway {
    override suspend fun load(owner: String): AssistantConsentDocument {
        if (store.pending(owner)) throw AssistantWithdrawalPendingException()
        return effective(owner, delegate.load(owner))
    }
    override suspend fun save(owner: String, document: AssistantConsentDocument, health: Boolean): AssistantConsentDocument {
        if (store.pending(owner)) throw AssistantWithdrawalPendingException()
        val revision = store.beginHealthUpdate(owner, health)
        val updated = delegate.save(owner, document, health)
        if (!updated.isValid() || !updated.accepted || updated.health_accepted != health) {
            throw AssistantConsentServiceException(ConsentFailure.INVALID)
        }
        store.confirmHealthUpdate(owner, revision, health)
        return effective(owner, updated)
    }
    override suspend fun revoke(owner: String) {
        store.setPending(owner, true)
        store.beginHealthUpdate(owner, false)
        delegate.revoke(owner)
        store.setPending(owner, false)
    }
    private suspend fun effective(owner: String, document: AssistantConsentDocument) =
        if (store.healthBlocked(owner)) document.copy(health_accepted = false,
            health_sync_required = document.health_accepted,
            health_review_required = document.health_review_required || document.health_accepted) else document
}

internal fun applyAssistantHealthPrivacy(request: BackendChatbotAskRequest, blocked: Boolean): BackendChatbotAskRequest =
    if (blocked) request.copy(health_consent = false, health_consent_version = null) else request

internal fun defaultAssistantConsentGateway(): AssistantConsentGateway =
    WithdrawalTrackingConsentGateway(AssistantConsentRepository(), AndroidAssistantWithdrawalStore())
