package com.havamania

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.junit.Assert.*
import org.junit.Test

/** Real isolated DataStore; only the first caller's return is delayed after disk commit. */
class CityPreferencePublicationDeviceTest {
    @Test fun lateDefaultWriteReturnCannotRevertNewerPersistedSelection() = verify(false)
    @Test fun lateCityWriteReturnCannotRevertNewerPersistedList() = verify(true)

    private fun verify(cities: Boolean) = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "city-publication-${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val base = PreferenceDataStoreFactory.create(scope = scope) { file }
        val committed = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val first = AtomicBoolean(true)
        val delayed = object : DataStore<Preferences> {
            override val data = base.data
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                val result = base.updateData(transform)
                if (first.compareAndSet(true, false)) { committed.complete(Unit); release.await() }
                return result
            }
        }
        val old = GeocodingResultDto(id = 1, name = "Eski")
        val newer = old.copy(id = 2, name = "Yeni")
        val store = CitySyncStore(base)
        val oldSeen = CompletableDeferred<Unit>()
        val newSeen = CompletableDeferred<Unit>()
        var visible = ""
        val source: Flow<String> = if (cities) store.observeCities("a").map { it.lastOrNull()?.name.orEmpty() }
            else store.observeDefaultCity("a").map { it?.name.orEmpty() }
        val observer = launch {
            observeAccountData(source, "a", { "a" }, {
                visible = it
                if (it == "Eski") oldSeen.complete(Unit)
                if (it == "Yeni") newSeen.complete(Unit)
            }, { throw AssertionError(it) })
        }
        val slowWrite = launch {
            if (cities) CitySyncStore(delayed).recordCityChange("a", old, false)
            else CitySyncStore(delayed).recordDefaultCity("a", old)
        }
        try {
            withTimeout(5000) { committed.await(); oldSeen.await() }
            if (cities) store.recordCityChange("a", newer, false) else store.recordDefaultCity("a", newer)
            withTimeout(5000) { newSeen.await() }
            release.complete(Unit); slowWrite.join()
            assertEquals("Yeni", visible)
            assertEquals(newer, if (cities) store.pendingCityChanges("a").last().city
                else store.pendingDefaultCity("a")!!.city)
        } finally {
            release.complete(Unit); slowWrite.cancelAndJoin(); observer.cancelAndJoin()
            scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete()
        }
    }
}
