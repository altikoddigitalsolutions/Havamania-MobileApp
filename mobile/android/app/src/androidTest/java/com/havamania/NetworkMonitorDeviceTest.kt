package com.havamania

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.havamania.ui.theme.HavamaniaTheme
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class NetworkMonitorDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun weatherAndTripSyncReportWifiLossAndRecovery() {
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

        val monitor = ConnectivityManagerNetworkMonitor(context)
        val weather = WeatherData(cityName = "İzmir", timezone = "Europe/Istanbul")
        val retrying = mutableStateOf(true)
        compose.setContent {
            val online by monitor.isOnline.collectAsStateWithLifecycle(initialValue = null)
            HavamaniaTheme {
                val isOnline = online
                if (isOnline != null) {
                    Column {
                        TripSyncStatusPanel(
                            pendingUploads = 1,
                            pendingDeletions = 1,
                            retrying = retrying.value,
                            isOnline = isOnline,
                            uploadError = null,
                            deletionError = null,
                            onRetry = {}
                        )
                        Box(Modifier.weight(1f)) {
                            WeatherSuccessContent(
                                data = weather,
                                selectedHourlyWeather = null,
                                selectedForecastDate = LocalDate.now(ForecastTime.zone(weather.timezone)),
                                selectedDailyForecast = null,
                                recommendation = null,
                                onSelectHour = {},
                                onSelectDaily = {},
                                scrollState = rememberScrollState(),
                                onAskAiClick = {},
                                isOnline = isOnline
                            )
                        }
                    }
                }
            }
        }
        val city = compose.onNodeWithText("İZMİR")
        val offlineLabel = "Çevrimdışı hava verisi"
        val warning = compose.onNodeWithText(offlineLabel, substring = true)
        try {
            city.assertIsDisplayed()
            warning.assertDoesNotExist()
            compose.onNodeWithText("Eşitleniyor…").assertIsNotEnabled()
            shell("svc wifi disable")
            compose.waitUntil(25_000) {
                compose.onAllNodesWithText(offlineLabel, substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            warning.assertIsDisplayed()
            city.assertIsDisplayed()
            compose.onNodeWithText("Bağlantı bekleniyor").assertIsNotEnabled()
            compose.onNodeWithText("1 seyahat değişikliği eşitlenmeyi bekliyor.").assertIsDisplayed()
            compose.onNodeWithText("1 seyahat silme işlemi tamamlanmayı bekliyor.").assertIsDisplayed()
            compose.runOnIdle { retrying.value = false }
            shell("svc wifi enable")
            compose.waitUntil(60_000) {
                compose.onAllNodesWithText(offlineLabel, substring = true).fetchSemanticsNodes().isEmpty()
            }
            city.assertIsDisplayed()
            compose.onNodeWithText("Yeniden dene").assertIsEnabled()
        } finally {
            shell("svc wifi enable")
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
