package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class TurkeyFirstLocationSearchTest {
    private val bali = GeocodingResultDto(1, "Bali", -8.4, 115.1, "Endonezya", "ID")
    private val balikesir = GeocodingResultDto(2, "Balıkesir", 39.6, 27.8, "Türkiye", "TR")
    @Test fun turkeyMatchesAreFetchedEvenWhenGlobalLimitExcludedThem() = runBlocking {
        val countries = mutableSetOf<String?>()
        val result = turkeyFirstLocationSearch { country ->
            countries += country
            if (country == "TR") listOf(balikesir) else listOf(bali)
        }
        assertEquals(setOf("TR", null), countries)
        assertEquals(listOf(balikesir, bali), result)
    }
    @Test fun countryPriorityIsStableAndDuplicateIdIsRemoved() = runBlocking {
        val district = balikesir.copy(id = 3, name = "Balya", countryCode = "tr")
        val result = turkeyFirstLocationSearch { country ->
            if (country == "TR") listOf(balikesir, district) else listOf(bali, balikesir)
        }
        assertEquals(listOf(balikesir, district, bali), result)
    }
    @Test fun globalResultsRemainWhenTurkeyHasNoMatch() = runBlocking {
        assertEquals(listOf(bali), turkeyFirstLocationSearch { if (it == "TR") emptyList() else listOf(bali) })
    }
    @Test fun oneFailedEndpointDoesNotHideOtherEndpointResults() = runBlocking {
        assertEquals(listOf(balikesir), turkeyFirstLocationSearch { if (it == "TR") listOf(balikesir) else throw java.io.IOException("global") })
        assertEquals(listOf(bali), turkeyFirstLocationSearch { if (it == "TR") throw java.io.IOException("TR") else listOf(bali) })
    }
    @Test fun failureWithoutAnyUsableResultIsNotReportedAsNoMatches() = runBlocking {
        try {
            turkeyFirstLocationSearch { if (it == "TR") emptyList() else throw java.io.IOException("global") }
            fail("Must preserve retryable error")
        } catch (_: java.io.IOException) { }
    }
    @Test fun typedPrefixOutranksUnrelatedDomesticFuzzyMatch() = runBlocking {
        val batman = balikesir.copy(id = 4, name = "Batman")
        val result = turkeyFirstLocationSearch("BAL") {
            if (it == "TR") listOf(batman, balikesir) else listOf(bali)
        }
        assertEquals(listOf(balikesir, batman, bali), result)
    }
    @Test fun parentCancellationCancelsBothLookups() = runBlocking {
        val started = CompletableDeferred<Unit>()
        var calls = 0; var stopped = 0
        val job = launch {
            turkeyFirstLocationSearch {
                calls++; if (calls == 2) started.complete(Unit)
                try { awaitCancellation() } finally { stopped++ }
            }
        }
        started.await(); job.cancelAndJoin()
        assertEquals(2, stopped)
    }
}