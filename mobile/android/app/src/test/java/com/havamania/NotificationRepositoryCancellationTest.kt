package com.havamania

import java.lang.reflect.Proxy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class NotificationRepositoryCancellationTest {
    @Test fun cancelledInsertMustStopTheWorkerInsteadOfReportingSuccess() = runBlocking {
        val cancellation = CancellationException("test cancellation")
        val dao = Proxy.newProxyInstance(NotificationDao::class.java.classLoader,
            arrayOf(NotificationDao::class.java)) { _, method, _ ->
            check(method.name == "insert")
            throw cancellation
        } as NotificationDao
        try {
            NotificationRepository(dao).insert(NotificationItem(userId = "owner", title = "Test", message = "Test"))
            fail("Cancelled work must not continue to system notification delivery")
        } catch (error: CancellationException) {
            assertSame(cancellation, error)
        }
    }
}
