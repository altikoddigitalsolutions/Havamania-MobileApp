package com.havamania

import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

class AssistantTravelsTest {
    private val today = LocalDate.of(2026, 11, 27)
    private fun trip(id: String, owner: String = "A", day: LocalDate = today) =
        TravelPlan(id = id, userId = owner, city = id, startDate = day, endDate = day)

    @Test fun filtersForeignDemoArchivedAndExpiredPlansAndSortsDeterministically() = runBlocking {
        val rows = listOf(trip("z"), trip("b", day = today.plusDays(1)), trip("a"), trip("foreign", "B"),
            trip("demo").copy(isDemo = true), trip("archive").copy(isArchived = true),
            trip("past", day = today.minusDays(1)))
        val result = observeAssistantTravels(flowOf(rows), flowOf(today), "A").first()
        assertEquals(listOf("a", "z", "b"), result.map { it.id })
    }

    @Test fun observesCreateEditArchiveAndDeleteWithoutReopeningAssistant() = runBlocking {
        val rows = MutableStateFlow<List<TravelPlan>>(emptyList())
        val dates = MutableStateFlow(today)
        val results = Channel<List<TravelPlan>>(Channel.UNLIMITED)
        val job = launch { observeAssistantTravels(rows, dates, "A").collect { results.send(it) } }
        try {
            withTimeout(5000) {
                assertTrue(results.receive().isEmpty())
                rows.value = listOf(trip("a"))
                assertEquals("a", results.receive().single().city)
                rows.value = listOf(trip("a").copy(city = "İzmir", endDate = today.plusDays(2)))
                assertEquals("İzmir", results.receive().single().city)
                rows.value = listOf(trip("a").copy(isArchived = true))
                assertTrue(results.receive().isEmpty())
                rows.value = listOf(trip("b"))
                assertEquals("b", results.receive().single().id)
                rows.value = emptyList()
                assertTrue(results.receive().isEmpty())
            }
        } finally { job.cancelAndJoin(); results.close() }
    }

    @Test fun dayChangeRemovesFinishedTripButKeepsOngoingTripWithoutDatabaseWrite() = runBlocking {
        val dates = MutableStateFlow(today)
        val results = Channel<List<TravelPlan>>(Channel.UNLIMITED)
        val rows = flowOf(listOf(trip("finished"), trip("ongoing").copy(endDate = today.plusDays(1))))
        val job = launch { observeAssistantTravels(rows, dates, "A").collect { results.send(it) } }
        try {
            withTimeout(5000) {
                assertEquals(2, results.receive().size)
                dates.value = today.plusDays(1)
                assertEquals("ongoing", results.receive().single().id)
                dates.value = today.plusDays(2)
                assertTrue(results.receive().isEmpty())
            }
        } finally { job.cancelAndJoin(); results.close() }
    }

    @Test fun accountResetCancelsOldTravelObserver() = runBlocking {
        var owner = "A"
        val tasks = AccountTaskScope(this) { owner }
        val rows = MutableStateFlow(listOf(trip("a"), trip("b", "B")))
        val results = Channel<List<TravelPlan>>(Channel.UNLIMITED)
        try {
            withTimeout(5000) {
                val old = tasks.launch { uid -> observeAssistantTravels(rows, flowOf(today), uid).collect { results.send(it) } }
                assertEquals("a", results.receive().single().id)
                owner = "B"; tasks.reset(); old.join()
                assertTrue(old.isCancelled)
                tasks.launch { uid -> observeAssistantTravels(rows, flowOf(today), uid).collect { results.send(it) } }
                assertEquals("b", results.receive().single().id)
                rows.value = listOf(trip("a2"), trip("b2", "B"))
                assertEquals("b2", results.receive().single().id)
                assertTrue(results.tryReceive().isFailure)
            }
        } finally { tasks.close(); results.close() }
    }
}
