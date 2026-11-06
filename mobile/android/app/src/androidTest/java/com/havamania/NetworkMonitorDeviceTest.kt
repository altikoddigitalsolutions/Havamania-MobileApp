package com.havamania

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class NetworkMonitorDeviceTest {
    @Test fun activeObserverReportsWifiLossAndRecovery() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("testWifiToggle") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        fun shell(command: String): String =
            ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
                .bufferedReader().use { it.readText().trim() }

        val context = instrumentation.targetContext
        val manager = requireNotNull(context.getSystemService(ConnectivityManager::class.java))
        val initialNetwork = manager.activeNetwork?.let(manager::getNetworkCapabilities)
        check(shell("settings get global wifi_on") == "1")
        check(initialNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true)

        val changes = Channel<Boolean>(Channel.UNLIMITED)
        val observer = launch {
            ConnectivityManagerNetworkMonitor(context).isOnline.collect { changes.send(it) }
        }
        try {
            assertEquals(true, withTimeout(15_000) { changes.receive() })
            shell("svc wifi disable")
            assertEquals(false, withTimeout(25_000) { changes.receive() })
            shell("svc wifi enable")
            assertEquals(true, withTimeout(60_000) { changes.receive() })
        } finally {
            shell("svc wifi enable")
            observer.cancel()
        }
    }

    @Test fun reportsInternetValidationOfTheActualDefaultNetwork() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = requireNotNull(context.getSystemService(ConnectivityManager::class.java))
        val monitor = ConnectivityManagerNetworkMonitor(context)

        repeat(2) {
            val online = withTimeout(5_000) { monitor.isOnline.first() }
            val capabilities = manager.activeNetwork?.let(manager::getNetworkCapabilities)
            val expected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            assertEquals(expected, online)
        }
    }
}
