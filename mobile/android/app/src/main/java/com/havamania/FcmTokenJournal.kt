package com.havamania

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlinx.coroutines.ensureActive

internal interface FcmJournalStorage {
    fun read(): String?
    fun write(value: String)
}

@Serializable
internal data class FcmTokenRegistration(val installationId: String, val revision: Long = 0,
    val ownerUid: String? = null, val token: String? = null, val pending: Boolean = false) {
    override fun toString() = "FcmTokenRegistration(revision=$revision, pending=$pending)"
}

/** One serialized, durable desired state. Failed writes never advance the in-memory revision. */
internal class FcmTokenJournal(private val storage: FcmJournalStorage) {
    private val json = Json { ignoreUnknownKeys = true }
    private var record = storage.read()?.let { json.decodeFromString<FcmTokenRegistration>(it) }
        ?: FcmTokenRegistration(UUID.randomUUID().toString())

    init {
        require(UUID.fromString(record.installationId).toString() == record.installationId)
        require(record.revision in 0..9_007_199_254_740_991L)
        require(record.token == null || record.token!!.length in 10..512)
        require(record.ownerUid == null || !record.ownerUid!!.isBlank() && record.ownerUid != "legacy" && record.ownerUid!!.length <= 128)
        require(record.token?.none(Char::isISOControl) != false)
        require(!record.pending || (!record.ownerUid.isNullOrBlank() && record.token != null))
    }

    @Synchronized fun snapshot(): FcmTokenRegistration = record

    @Synchronized fun update(ownerUid: String?, token: String?): FcmTokenRegistration {
        require(ownerUid == null || ownerUid.isNotBlank() && ownerUid != "legacy" && ownerUid.length <= 128)
        require(token == null || token.length in 10..512 && token.none(Char::isISOControl))
        if (record.ownerUid == ownerUid && record.token == token) return record
        check(record.revision < 9_007_199_254_740_991L)
        return commit(record.copy(revision = record.revision + 1, ownerUid = ownerUid, token = token,
            pending = ownerUid != null && token != null))
    }

    @Synchronized fun acknowledge(sent: FcmTokenRegistration): Boolean {
        if (record != sent || !record.pending) return false
        commit(record.copy(pending = false))
        return true
    }

    @Synchronized fun clearAccount(): FcmTokenRegistration = update(null, null)

    private fun commit(next: FcmTokenRegistration): FcmTokenRegistration {
        storage.write(json.encodeToString(next))
        record = next
        return next
    }
}

internal enum class FcmSyncResult { IDLE, APPLIED, SUPERSEDED, RETRY }

/** Gateway must verify the server's installation/revision/owner receipt before returning true. */
internal suspend fun syncPendingFcmToken(journal: FcmTokenJournal, currentUid: () -> String?,
    sessionEpoch: () -> Long, deletionPending: () -> Boolean,
    register: suspend (FcmTokenRegistration) -> Boolean): FcmSyncResult {
    val desired = journal.snapshot()
    val epoch = sessionEpoch()
    if (!desired.pending) return FcmSyncResult.IDLE
    fun current() = currentUid() == desired.ownerUid && !deletionPending() && sessionEpoch() == epoch &&
        journal.snapshot() == desired
    if (!current()) return FcmSyncResult.SUPERSEDED
    return try {
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        val accepted = register(desired)
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        if (!current()) FcmSyncResult.SUPERSEDED
        else if (!accepted) FcmSyncResult.RETRY
        else if (journal.acknowledge(desired)) FcmSyncResult.APPLIED else FcmSyncResult.SUPERSEDED
    } catch (error: kotlinx.coroutines.CancellationException) { throw error }
    catch (_: Exception) { if (current()) FcmSyncResult.RETRY else FcmSyncResult.SUPERSEDED }
}
