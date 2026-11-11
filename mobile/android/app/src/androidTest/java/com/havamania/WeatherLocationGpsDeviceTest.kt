package com.havamania

import android.Manifest
import android.os.ParcelFileDescriptor
import android.view.KeyEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Temporarily toggles the tablet location service and restores its original state in finally. */
@SdkSuppress(minSdkVersion = 28)
class WeatherLocationGpsDeviceTest {
    @get:Rule val compose = createComposeRule()
    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
    ).bufferedReader().use { it.readText().trim() }

    @Test fun disabledDeviceLocationShowsSettingsAndReturningEnabledStartsOnce() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assertEquals("com.havamania", context.packageName)
        val original = shell("cmd location is-location-enabled")
        assertTrue("Unable to read original device location state", original in setOf("true", "false"))
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        assertTrue(PermissionHelper.hasLocationPermission(context))
        var located = 0
        try {
            shell("cmd location set-location-enabled false")
            compose.waitUntil(5_000) { !deviceLocationEnabled(context) }
            compose.setContent {
                val colors = ThemeFactory.createColors(AppTheme.DARK)
                CompositionLocalProvider(LocalHavamaniaColors provides colors,
                    LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                    ProvideResponsiveLayout { MaterialTheme {
                        var resolved by remember { mutableStateOf(false) }
                        WeatherLocationAccess(1, !resolved, true, { located++; resolved = true }, {}) { locate ->
                            if (resolved) Text("Konum isteği başlatıldı")
                            else NoCitiesEmptyState(onUseLocation = locate, onAddClick = {})
                        }
                    } }
                }
            }
            try {
                compose.waitUntil(10_000) { runCatching {
                    compose.onAllNodesWithText("Cihaz konumu kapalı").fetchSemanticsNodes().isNotEmpty()
                }.getOrDefault(false) }
                compose.onNodeWithText("Cihaz konumu kapalı").assertIsDisplayed()
            } catch (error: Throwable) {
                shell("screencap -p /data/local/tmp/havamania-gps-qa.png")
                throw AssertionError("GPS UI unavailable; active package=${instrumentation.uiAutomation.rootInActiveWindow?.packageName}", error)
            }
            compose.runOnIdle { assertEquals(0, located) }
            compose.onNodeWithText("Konum ayarlarını aç").performClick()
            compose.waitUntil(10_000) { instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString()?.contains("settings") == true }
            shell("cmd location set-location-enabled true")
            compose.waitUntil(5_000) { deviceLocationEnabled(context) }
            assertTrue(instrumentation.uiAutomation.performGlobalAction(
                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK))
            compose.waitUntil(10_000) { instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() == context.packageName }
            compose.onNodeWithText("Konum isteği başlatıldı").assertIsDisplayed()
            compose.runOnIdle { assertEquals(1, located) }
        } finally {
            shell("cmd location set-location-enabled $original")
            assertEquals(original, shell("cmd location is-location-enabled"))
        }
    }
}
