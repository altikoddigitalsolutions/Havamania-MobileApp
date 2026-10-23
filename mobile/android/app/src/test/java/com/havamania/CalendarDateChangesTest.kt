package com.havamania

import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarDateChangesTest {
    @Test fun midnightAndBackwardClockChangeEmitButSameDayDoesNot() = runBlocking {
        var date = LocalDate.of(2026, 10, 23)
        val ticks = Channel<Unit>(Channel.UNLIMITED)
        val values = mutableListOf<LocalDate>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            calendarDateChanges({ date }, { ticks.receive() }).collect { values.add(it) }
        }
        ticks.send(Unit)
        yield()
        assertEquals(listOf(date), values)
        date = date.plusDays(1)
        ticks.send(Unit)
        yield()
        date = date.minusDays(2)
        ticks.send(Unit)
        yield()
        assertEquals(listOf(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 24), date), values)
        job.cancelAndJoin()
    }

    @Test fun cancellationStopsChecksAndRestartReadsCurrentDateImmediately() = runBlocking {
        var date = LocalDate.of(2026, 10, 23)
        var reads = 0
        val ticks = Channel<Unit>(Channel.UNLIMITED)
        val values = mutableListOf<LocalDate>()
        val dates = calendarDateChanges({ reads++; date }, { ticks.receive() })
        val first = launch(start = CoroutineStart.UNDISPATCHED) { dates.collect { values.add(it) } }
        first.cancelAndJoin()
        date = date.plusDays(1)
        yield()
        assertEquals(1, reads)
        val second = launch(start = CoroutineStart.UNDISPATCHED) { dates.collect { values.add(it) } }
        assertEquals(date, values.last())
        assertEquals(2, reads)
        second.cancelAndJoin()
    }
}
