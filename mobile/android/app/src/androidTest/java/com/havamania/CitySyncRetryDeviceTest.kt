package com.havamania

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CitySyncRetryDeviceTest {
    @Test fun retryMetadataSurvivesRestartAndOldFailureCannotBlockNewVersion() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "retry-cities-${UUID.randomUUID()}.preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val city = GeocodingResultDto(id = 1, name = "A")
        try {
            var store = CitySyncStore(PreferenceDataStoreFactory.create(scope = scope) { file })
            store.recordDefaultCity("a", city)
            val sent = store.pendingDefaultCity("a")!!
            val retry = citySyncAfterFailure(sent, CitySyncFailure.TEMPORARY, 100) { 0 }
            store.recordFailure("a", sent, retry, true)
            scope.coroutineContext[Job]!!.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            store = CitySyncStore(PreferenceDataStoreFactory.create(scope = scope) { file })
            assertEquals(retry, store.pendingDefaultCity("a"))
            store.recordDefaultCity("a", city.copy(name = "New"))
            store.recordFailure("a", sent, retry, true)
            val newer = store.pendingDefaultCity("a")!!
            assertEquals(0L, newer.retryNotBefore)
            store.recordFailure("a", newer, citySyncAfterFailure(newer, CitySyncFailure.REJECTED, 100), true)
            store.releaseBlocks("a", true)
            assertEquals("REJECTED", store.pendingDefaultCity("a")!!.blockedReason)
            store.releaseBlocks("a", false)
            assertNull(store.pendingDefaultCity("a")!!.blockedReason)
            assertNull(store.pendingDefaultCity("b"))
            for ((code, category) in listOf(
                FirebaseFirestoreException.Code.UNAVAILABLE to CitySyncFailure.TEMPORARY,
                FirebaseFirestoreException.Code.PERMISSION_DENIED to CitySyncFailure.REJECTED,
                FirebaseFirestoreException.Code.UNAUTHENTICATED to CitySyncFailure.AUTHENTICATION)) {
                assertEquals(category, citySyncFailure(FirebaseFirestoreException("local test", code)))
            }
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete() }
    }
}
