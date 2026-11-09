package com.havamania

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class NotificationInboxLoadTest {
    @Test fun failedReadIsDistinctFromEmptyInboxAndCanBeRetried() = runBlocking {
        val failure = IOException("Read failed")
        var reads = 0
        val results = flow<List<NotificationItem>> {
            when (++reads) {
                1 -> throw failure
                2 -> emit(listOf(NotificationItem(id = "notice")))
                else -> emit(emptyList())
            }
        }.inboxReadResults()

        assertSame(failure, results.first().exceptionOrNull())
        assertEquals(listOf("notice"), results.first().getOrThrow().map { it.id })
        assertEquals(emptyList<NotificationItem>(), results.first().getOrThrow())
    }

    @Test fun cancellationIsNotReportedAsReadFailure() = runBlocking {
        val cancellation = CancellationException("Account changed")
        val results = flow<List<NotificationItem>> { throw cancellation }.inboxReadResults()
        assertSame(cancellation, runCatching { results.first() }.exceptionOrNull())
    }
}
