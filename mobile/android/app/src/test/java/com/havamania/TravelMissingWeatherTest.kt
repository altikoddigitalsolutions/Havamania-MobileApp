package com.havamania

import com.havamania.ui.theme.AssistantTone
import org.junit.Assert.*
import org.junit.Test

class TravelMissingWeatherTest {
    private fun reply(snapshot: ForecastSnapshot?, tone: AssistantTone = AssistantTone.DENGELI) =
        TravelAiHelper.generateTravelAiSuggestion("Ankara", TripType.VACATION, snapshot, null, 2, tone = tone)
    private fun weather(snapshot: ForecastSnapshot?, tone: AssistantTone = AssistantTone.DENGELI) = reply(snapshot, tone).substringBefore("[SEP]")
    private fun packing(snapshot: ForecastSnapshot?) = reply(snapshot).substringAfter("VALİZ TAVSİYESİ|").substringBefore("[SEP]")

    @Test fun `no forecast never implies good weather in any tone`() {
        AssistantTone.entries.forEach { tone ->
            val text = weather(null, tone)
            assertTrue(text, text.contains("tahmin", true))
            listOf("elverişli", "güzel bir dönem", "eminim", "mevsim normalleri").forEach { assertFalse(text, text.contains(it, true)) }
        }
    }
    @Test fun `empty and nonfinite snapshots do not invent temperatures or calm weather`() {
        listOf(ForecastSnapshot(), ForecastSnapshot(maxTemp = Double.NaN, minTemp = Double.NEGATIVE_INFINITY,
            windSpeed = Double.POSITIVE_INFINITY, precipitationProbability = -1, uvIndex = Double.NaN)).forEach { snapshot ->
            AssistantTone.entries.forEach { tone ->
                val text = weather(snapshot, tone)
                listOf("20°", "NaN", "Infinity", "harika", "ideal", "elverişli", "Açık").forEach { assertFalse(text, text.contains(it, true)) }
                assertTrue(text, text.contains("eksik", true) || text.contains("bulunmuyor", true))
            }
        }
    }
    @Test fun `partial forecast reports actual value without inventing missing fields`() {
        val text = weather(ForecastSnapshot(maxTemp = 17.0, weatherCode = null))
        assertTrue(text, text.contains("17"))
        assertTrue(text, text.contains("eksik", true))
        assertFalse(text, text.contains("%0")); assertFalse(text, text.contains("açık", true))
    }
    @Test fun `known hazard survives missing temperature and wind in every tone`() {
        AssistantTone.entries.forEach { tone ->
            val text = weather(ForecastSnapshot(weatherCode = 95), tone)
            assertTrue(text, text.contains("fırtına", true)); assertFalse(text, text.contains("20°"))
        }
    }
    @Test fun `unknown temperatures do not imply mild evening packing`() {
        val text = packing(ForecastSnapshot())
        assertTrue(text, text.contains("tahmin", true))
        assertFalse(text, text.contains("ceket")); assertFalse(text, text.contains("serin"))
    }
    @Test fun `rain packing uses known rain even without temperature`() {
        assertTrue(packing(ForecastSnapshot(precipitationProbability = 80)).contains("yağmurluk"))
    }
    @Test fun `missing comparison probability is not zero rain`() {
        val a = ForecastSnapshot(precipitationProbability = null)
        val b = ForecastSnapshot(precipitationProbability = 80)
        listOf(a to b, b to a).forEach { (old, new) ->
            val text = TravelAiHelper.generateComparisonText(old, new)
            assertFalse(text, text.contains("arttı") || text.contains("azaldı"))
            assertTrue(text, text.contains("karşılaştır", true))
        }
    }
    @Test fun `valid comparable rain still reports change`() {
        assertTrue(TravelAiHelper.generateComparisonText(ForecastSnapshot(precipitationProbability = 10),
            ForecastSnapshot(precipitationProbability = 80)).contains("arttı"))
    }
}