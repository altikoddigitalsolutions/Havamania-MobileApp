package com.havamania

import java.time.LocalDate
import java.time.ZoneId
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class AssistantTripContextTest {
    private val day = LocalDate.of(2026, 11, 28)
    private val now = day.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private fun trip(id: String) = TravelPlan(id = id, userId = "A", city = "Balıkesir", district = "Ayvalık",
        startDate = day, endDate = day.plusDays(1), userNote = "PRIVATE_NOTE", latitude = 39.987654,
        longitude = 27.123456)
    private fun snapshot(plans: List<TravelPlan>) = Json.parseToJsonElement(assistantTripContext(plans, day, "A")).jsonObject

    @Test fun zeroAndMultipleTripsHaveExplicitCountsScopeAndDisambiguationGuidance() {
        val empty = snapshot(emptyList())
        assertEquals(0, empty.getValue("active_total").jsonPrimitive.int)
        assertEquals("ongoing_and_upcoming_only", empty.getValue("scope").jsonPrimitive.content)
        val many = snapshot(listOf(trip("a"), trip("b")))
        assertEquals(2, many.getValue("active_total").jsonPrimitive.int)
        assertEquals(2, many.getValue("listed_count").jsonPrimitive.int)
        assertEquals(0, many.getValue("omitted_count").jsonPrimitive.int)
        assertTrue(many.getValue("guidance").jsonPrimitive.content.contains("ilk kaydı varsayma"))
    }
    @Test fun moreThanThreeTripsAreNotSilentlyPresentedAsTheCompleteInventory() {
        val plans = (1..40).map { trip("trip-$it") }
        val encoded = assistantTripContext(plans, day, "A")
        val data = Json.parseToJsonElement(encoded).jsonObject
        val records = data.getValue("records").jsonArray
        assertEquals(40, data.getValue("active_total").jsonPrimitive.int)
        assertTrue(records.size > 3)
        assertEquals(40 - records.size, data.getValue("omitted_count").jsonPrimitive.int)
        assertEquals(records.size, data.getValue("listed_count").jsonPrimitive.int)
        assertTrue(encoded.length <= 2000)
    }
    @Test fun sameCityDifferentDistrictAndDatesRemainDistinctAndOrdered() {
        val a = trip("a")
        val b = trip("b").copy(district = "Edremit", startDate = day.plusDays(3), endDate = day.plusDays(4))
        val records = snapshot(listOf(b, a)).getValue("records").jsonArray
        assertEquals("a", records[0].jsonObject.getValue("trip_id").jsonPrimitive.content)
        assertEquals("Ayvalık, Balıkesir", records[0].jsonObject.getValue("place").jsonPrimitive.content)
        assertEquals("Edremit, Balıkesir", records[1].jsonObject.getValue("place").jsonPrimitive.content)
        assertEquals("2026-12-02", records[1].jsonObject.getValue("end_date").jsonPrimitive.content)
    }
    @Test fun foreignDemoArchiveExpiredInvalidAndDuplicateRecordsCannotInflateCount() {
        val a = trip("a")
        val result = snapshot(listOf(a, a, trip("foreign").copy(userId = "B"), trip("demo").copy(isDemo = true),
            trip("archive").copy(isArchived = true), trip("expired").copy(endDate = day.minusDays(1)),
            trip("invalid").copy(startDate = day.plusDays(2))))
        assertEquals(1, result.getValue("active_total").jsonPrimitive.int)
    }
    @Test fun optOutOmitsAllInventoryAndEnabledPayloadContainsNoNotesCoordinatesOrOwner() {
        val enabled = assistantClientContext(null, null, listOf(trip("a")), true, now, "A")
        val encoded = enabled.trips!!
        for (privateValue in listOf("PRIVATE_NOTE", "39.987654", "27.123456", "userId")) assertFalse(encoded.contains(privateValue))
        assertNull(assistantClientContext(null, null, listOf(trip("a")), false, now, "A").trips)
    }
    @Test fun longEscapedUnicodeNamesNeverProduceTruncatedJsonOrLeakExtraFields() {
        val plans = (1..20).map { trip("trip-$it").copy(city = "\"\\\n😀".repeat(80), district = null) }
        val encoded = assistantTripContext(plans, day, "A")
        assertTrue(encoded.length <= 2000)
        val data = Json.parseToJsonElement(encoded).jsonObject
        assertEquals(20, data.getValue("active_total").jsonPrimitive.int)
        assertEquals(20, data.getValue("listed_count").jsonPrimitive.int + data.getValue("omitted_count").jsonPrimitive.int)
        assertEquals(encoded, assistantTripContext(plans.reversed(), day, "A"))
    }
}
