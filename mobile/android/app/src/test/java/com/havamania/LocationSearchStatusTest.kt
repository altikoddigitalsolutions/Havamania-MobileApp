package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class LocationSearchStatusTest {
    @Test fun emptyResponseAndNetworkFailureAreDifferent() = runBlocking {
        assertEquals(LocationSearchStatus.EMPTY, fetchLocationSuggestions { emptyList() }.status)
        assertEquals(LocationSearchStatus.ERROR, fetchLocationSuggestions { throw java.io.IOException() }.status)
    }
    @Test fun resultsArePreserved() = runBlocking {
        val item = GeocodingResultDto(id = 1, name = "Ankara", country = "Türkiye")
        val result = fetchLocationSuggestions { listOf(item) }
        assertEquals(LocationSearchStatus.RESULTS, result.status)
        assertEquals(listOf(item), result.items)
    }
    @Test fun cancellationDoesNotBecomeAnErrorResult() = runBlocking {
        try {
            fetchLocationSuggestions { throw CancellationException("replaced query") }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }
}
