package com.havamania

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.*
import org.junit.Test

class WeatherAccountDeletionDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun historyRevisionAtMaximumCannotWrapAndOverwriteTheExistingRecord() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WeatherDatabase::class.java).build()
        try {
            val dao = db.weatherDao()
            val original = AiHistoryEntity(id = "limit", userId = "a", updatedAt = Long.MAX_VALUE)
            dao.insertAiHistory(original)
            val result = runCatching { dao.saveAiHistoryRevision(original.copy(title = "New", updatedAt = 42L)) }
            assertTrue(result.exceptionOrNull() is IllegalStateException)
            assertEquals(original, dao.getAiHistoryItem("limit", "a"))
        } finally { db.close() }
    }

    @Test fun concurrentHistorySavesAdvanceDistinctVersionsAndPreserveCreationTime() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WeatherDatabase::class.java).build()
        try {
            val dao = db.weatherDao()
            val original = AiHistoryEntity(id = "shared", userId = "a", timestamp = 100L, updatedAt = 200L)
            dao.insertAiHistory(original)
            val saves = listOf("first", "second").map { title ->
                async(Dispatchers.Default) {
                    dao.saveAiHistoryRevision(original.copy(title = title, timestamp = 300L, updatedAt = 200L))
                }
            }.awaitAll()
            assertEquals(listOf(201L, 202L), saves.map { it.updatedAt }.sorted())
            assertTrue(saves.all { it.timestamp == 100L })
            assertEquals(saves.maxBy { it.updatedAt }, dao.getAiHistoryItem("shared", "a"))
        } finally { db.close() }
    }

    @Test fun historyListReadsSummaryAndDetailStillReturnsTheFullConversation() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WeatherDatabase::class.java).build()
        try {
            val dao = db.weatherDao()
            val messages = List(200) { AltikodChatMessage(text = "Message $it", isUser = true) }
            val record = AiHistoryEntity(id = "long", userId = "a", title = "Title", summary = "Summary",
                cityName = "İzmir", timestamp = 100L, updatedAt = 200L, pendingUpload = true, messages = messages)
            dao.insertAiHistory(record)
            assertEquals(AiHistorySummary("long", "Title", "Summary", "İzmir", 100L, true),
                dao.getVisibleAiHistory("a").single())
            assertEquals(AiHistoryVersion("a", 100L, 200L, true), dao.getAiHistoryVersion("long"))
            assertEquals(messages, dao.getAiHistoryItem("long", "a")?.messages)
        } finally { db.close() }
    }

    @Test fun retryingFinishedBulkHistoryDeletionDoesNotTouchNewConversations() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WeatherDatabase::class.java).build()
        try {
            val dao = db.weatherDao()
            HistoryOperationLock.mutex.withLock {
                dao.insertAiHistory(AiHistoryEntity(id = "old", userId = "a"))
                dao.enqueueHistoryDeletion("a", null)
                var requests = 0
                dao.finishQueuedHistoryDeletion("a", "") {
                    requests++
                    dao.completeHistoryDeletion("a", "")
                }
                dao.insertAiHistory(AiHistoryEntity(id = "new", userId = "a"))
                dao.finishQueuedHistoryDeletion("a", "") {
                    requests++
                    dao.completeHistoryDeletion("a", "")
                }
                assertEquals(1, requests)
                assertEquals(listOf("new"), dao.getVisibleAiHistory("a").map { it.id })
            }
        } finally { db.close() }
    }

    @Test fun savingTheSameHistoryIdForAnotherOwnerFailsWithoutOverwritingTheOriginal() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WeatherDatabase::class.java).build()
        try {
            val dao = db.weatherDao()
            val original = AiHistoryEntity(id = "shared-id", userId = "a", title = "First")
            dao.insertAiHistory(original)
            val rejected = runCatching {
                dao.insertAiHistory(original.copy(userId = "b", title = "Second", pendingUpload = true))
            }
            assertTrue(rejected.exceptionOrNull() is IllegalStateException)
            assertEquals(original, dao.getAiHistoryItem(original.id, "a"))
            assertTrue(dao.getPendingAiHistoryIds("b").isEmpty())
        } finally { db.close() }
    }

    @Test fun pendingHistoryDeletionHidesRecordsFromListAndUploadOnlyForItsOwner() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WeatherDatabase::class.java).build()
        try {
            val dao = db.weatherDao()
            for ((index, entry) in listOf("two" to "a", "one" to "a", "three" to "b").withIndex()) {
                val (id, owner) = entry
                dao.insertAiHistory(AiHistoryEntity(id = id, userId = owner,
                    updatedAt = (3 - index).toLong(), pendingUpload = true))
            }
            assertEquals(listOf("one", "two"), dao.getPendingAiHistoryIds("a"))
            dao.enqueueHistoryDeletion("a", "one")
            assertEquals(listOf("two"), dao.getVisibleAiHistory("a").map { it.id })
            assertEquals(listOf("two"), dao.getPendingAiHistoryIds("a"))
            dao.enqueueHistoryDeletion("a", null)
            assertTrue(dao.getVisibleAiHistory("a").isEmpty())
            assertTrue(dao.getPendingAiHistoryIds("a").isEmpty())
            assertEquals(2, dao.getAllAiHistory("a").size)
            assertEquals(listOf("three"), dao.getVisibleAiHistory("b").map { it.id })
            assertEquals(listOf("three"), dao.getPendingAiHistoryIds("b"))
            dao.enqueueHistoryDeletion("b", "three")
            dao.markRemoteHistoryForDeletion("a", listOf("one", "remote-only"))
            assertTrue(dao.getHistoryDeletions("a").any { it.recordId == "one" })
            dao.completeHistoryDeletion("a", "")
            assertTrue(dao.getHistoryDeletions("a").isEmpty())
            assertTrue(dao.hasHistoryDeletion("a", "two"))
            assertTrue(dao.hasHistoryDeletion("a", "remote-only"))
            assertFalse(dao.hasHistoryDeletion("b", "remote-only"))
            assertFalse(dao.hasHistoryDeletion("b", "two"))
            assertEquals(listOf("three"), dao.getHistoryDeletions("b").map { it.recordId })
            dao.mergeAiHistory(AiHistoryEntity(id = "one", userId = "a"))
            assertTrue(dao.getAllAiHistory("a").isEmpty())
        } finally { db.close() }
    }

    @Test fun reopenedDatabaseRejectsLatePlansHistoryAndQueues() = runBlocking {
        val name = "weather-account-delete-${UUID.randomUUID()}"
        var db = Room.databaseBuilder(context, WeatherDatabase::class.java, name).build()
        try {
            var dao = db.weatherDao()
            for (uid in listOf("a", "b")) {
                dao.saveLocalTrip(TravelPlanEntity(id = "trip-$uid", userId = uid, city = "City"))
                dao.insertAiHistory(AiHistoryEntity(id = "history-$uid", userId = uid, title = "Saved"))
                dao.enqueueTripDeletion(TripDeletionRequest(uid, "removed-$uid"))
                dao.insertHistoryDeletion(HistoryDeletionRequest(uid, "removed-$uid"))
            }
            dao.deleteAccountData("a")
            db.close()
            db = Room.databaseBuilder(context, WeatherDatabase::class.java, name).build()
            dao = db.weatherDao()
            val trip = TravelPlanEntity(id = "late", userId = "a", city = "Late")
            dao.insertTravelPlan(trip); dao.saveLocalTrip(trip)
            assertNull(dao.saveTripAnalysisIfVersion(trip, 0))
            val history = AiHistoryEntity(id = "late", userId = "a", title = "Late")
            try { dao.insertAiHistory(history); fail("Expected blocked local history") }
            catch (_: IllegalStateException) { }
            dao.mergeAiHistory(history)
            dao.reconcileAiHistory("a", listOf(history), setOf("late"))
            dao.enqueueTripUpload(TripUploadRequest("a", "late", 1))
            dao.enqueueTripDeletion(TripDeletionRequest("a", "late"))
            dao.insertHistoryDeletion(HistoryDeletionRequest("a", "late"))
            dao.enqueueHistoryDeletion("a", null)
            dao.completeHistoryDeletion("a", "")
            dao.completeTripDeletion("a", "late")
            assertTrue(dao.getAllTravelPlans("a").isEmpty())
            assertTrue(dao.getAllAiHistory("a").isEmpty())
            assertTrue(dao.pendingTripUploads("a").isEmpty())
            assertTrue(dao.pendingTripDeletions("a").isEmpty())
            assertTrue(dao.getHistoryDeletions("a").isEmpty())
            assertEquals(1, dao.getAllTravelPlans("b").size)
            assertEquals(1, dao.getAllAiHistory("b").size)
            assertEquals(1, dao.pendingTripUploads("b").size)
            assertEquals(1, dao.pendingTripDeletions("b").size)
            assertEquals(1, dao.getHistoryDeletions("b").size)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun queuedLocalWritesCannotOutrunAccountCleanup() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WeatherDatabase::class.java).build()
        try {
            val dao = db.weatherDao()
            val started = CompletableDeferred<Unit>()
            val producer = launch(Dispatchers.Default) {
                repeat(100) { index ->
                    dao.saveLocalTrip(TravelPlanEntity(id = "trip-$index", userId = "a", city = "City"))
                    if (index == 0) started.complete(Unit)
                    yield()
                }
            }
            started.await(); dao.deleteAccountData("a"); producer.join()
            assertTrue(dao.getAllTravelPlans("a").isEmpty())
            assertTrue(dao.pendingTripUploads("a").isEmpty())
        } finally { db.close() }
    }

    @Test fun version22MigrationPreservesDataAndAddsAccountFence() = runBlocking {
        val name = "weather-v22-${UUID.randomUUID()}"
        var db = Room.databaseBuilder(context, WeatherDatabase::class.java, name).build()
        try {
            db.weatherDao().saveLocalTrip(TravelPlanEntity(id = "kept", userId = "a", city = "Ankara"))
            db.weatherDao().insertAiHistory(AiHistoryEntity(id = "history", userId = "a", title = "Keep"))
            db.close()
            val sqlite = android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath(name).path,
                null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE)
            try { sqlite.execSQL("DROP TABLE weather_account_deletions"); sqlite.version = 22 }
            finally { sqlite.close() }
            db = Room.databaseBuilder(context, WeatherDatabase::class.java, name)
                .addMigrations(WeatherDatabase.MIGRATION_22_23).build()
            val dao = db.weatherDao()
            assertEquals("Ankara", dao.getAllTravelPlans("a").single().city)
            assertEquals("Keep", dao.getAllAiHistory("a").single().title)
            assertEquals(1, dao.pendingTripUploads("a").size)
            dao.deleteAccountData("a")
            dao.saveLocalTrip(TravelPlanEntity(id = "late", userId = "a", city = "Late"))
            assertTrue(dao.getAllTravelPlans("a").isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
