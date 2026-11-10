package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/** Isolated local preference keys only. No account, ViewModel or HTTP calls. */
class AssistantHealthPrivacyStoreTest {
    private fun owner() = "privacy-local-test-" + UUID.randomUUID()
    private fun cleanup(vararg owners: String) {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val edit = context.getSharedPreferences("assistant_local_privacy", android.content.Context.MODE_PRIVATE).edit()
        owners.forEach { edit.remove("health_blocked_$it").remove("health_revision_$it").remove("withdraw_pending_$it") }
        assertTrue(edit.commit())
    }
    @Test fun persistedOptOutAndRevisionRejectLateOptInAcrossStoreInstances() = runBlocking {
        val a = owner(); val b = owner()
        try {
            val first = AndroidAssistantWithdrawalStore()
            val oldOptIn = first.beginHealthUpdate(a, true)
            val second = AndroidAssistantWithdrawalStore()
            second.beginHealthUpdate(a, false)
            assertTrue(first.healthBlocked(a))
            assertFalse(first.confirmHealthUpdate(a, oldOptIn, true))
            assertFalse(second.healthBlocked(b))
            val latest = second.beginHealthUpdate(a, true)
            assertTrue(first.healthBlocked(a))
            assertTrue(second.confirmHealthUpdate(a, latest, true))
            assertFalse(AndroidAssistantWithdrawalStore().healthBlocked(a))
        } finally { cleanup(a,b) }
    }
    @Test fun concurrentInstancesAllocateDistinctDurableRevisions() = runBlocking {
        val a = owner()
        try {
            val revisions = ConcurrentLinkedQueue<Long>()
            coroutineScope {
                repeat(16) { launch(Dispatchers.IO) { revisions.add(AndroidAssistantWithdrawalStore().beginHealthUpdate(a, false)) } }
            }
            assertEquals(16, revisions.toSet().size)
            assertEquals(16L, revisions.maxOrNull())
            assertTrue(AndroidAssistantWithdrawalStore().healthBlocked(a))
        } finally { cleanup(a) }
    }
}
