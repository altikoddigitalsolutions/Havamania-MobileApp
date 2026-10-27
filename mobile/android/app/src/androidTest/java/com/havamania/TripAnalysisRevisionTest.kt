package com.havamania

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TripAnalysisRevisionTest {
    @Test fun lateAnalysisCannotOverwriteEditedTripOrCrossOwnerOrDeletion() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), WeatherDatabase::class.java).build()
        try {
            val dao = database.weatherDao()
            val original = TravelPlanEntity(id = "local-trip", userId = "A", city = "Ankara", updatedAt = 100)
            dao.saveLocalTrip(original)
            val initialVersion = dao.getTravelPlanByIdRaw(original.id)!!.updatedAt
            dao.saveLocalTrip(original.copy(city = "Tokyo", updatedAt = 200))
            assertNull(dao.saveTripAnalysisIfVersion(original.copy(aiSuggestion = "Old result"), initialVersion))
            val edited = dao.getTravelPlanByIdRaw(original.id)!!
            assertEquals("Tokyo", edited.city)
            assertNull(edited.aiSuggestion)
            assertNull(dao.saveTripAnalysisIfVersion(edited.copy(userId = "B"), edited.updatedAt))
            val acceptedVersion = dao.saveTripAnalysisIfVersion(edited.copy(aiSuggestion = "Current result"), edited.updatedAt)!!
            assertEquals("Current result", dao.getTravelPlanByIdRaw(original.id)!!.aiSuggestion)
            assertEquals(0, dao.markTripDailyNotification("A", original.id, edited.updatedAt, "2026-11-01"))
            assertEquals(1, dao.markTripDailyNotification("A", original.id, acceptedVersion, "2026-11-01"))
            dao.enqueueTripDeletion(TripDeletionRequest("A", original.id))
            assertNull(dao.saveTripAnalysisIfVersion(edited, acceptedVersion))
        } finally { database.close() }
    }
}
