package com.havamania

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.material3.Text
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed

/** Run only in the isolated QA application, after granting its coarse location permission. */
@androidx.test.filters.SdkSuppress(minSdkVersion = 28)
class CurrentLocationDeviceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun approximateLocationPermissionCanResolveCurrentCity() = verifyCity(false)
    @Test fun firstPositionRequestAfterLocationServiceRestartResolvesCity() = verifyCity(true)

    private fun shell(command: String): String = android.os.ParcelFileDescriptor.AutoCloseInputStream(
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
    ).bufferedReader().use { it.readText().trim() }

    private fun verifyCity(restart: Boolean) = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assertEquals("Location QA must never target the Play package", "com.havamania", context.packageName)
        compose.setContent { Text("Konum izni testi") }
        compose.onNodeWithText("Konum izni testi").assertIsDisplayed()
        compose.waitUntil(5_000) {
            var focused = false
            instrumentation.runOnMainSync {
                focused = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                    .any { it.packageName == context.packageName && it.hasWindowFocus() }
            }
            focused
        }
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        assertEquals(PackageManager.PERMISSION_GRANTED,
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION))
        assertTrue(PermissionHelper.hasLocationPermission(context))
        val original = if (restart) shell("cmd location is-location-enabled") else null
        if (restart) assertTrue(original in setOf("true", "false"))
        val stages = java.util.Collections.synchronizedList(mutableListOf<LocationLookupStage>())
        val tracker = DefaultLocationTracker(LocationServices.getFusedLocationProviderClient(context),
            context.applicationContext as Application) { stages.add(it) }
        try {
            if (restart) {
                shell("cmd location set-location-enabled false")
                assertNull(tracker.getCurrentLocation())
                assertTrue(stages.contains(LocationLookupStage.LOCATION_DISABLED))
                shell("cmd location set-location-enabled true")
                compose.waitUntil(5_000) { deviceLocationEnabled(context) }
                stages.clear()
            }
            val started = android.os.SystemClock.elapsedRealtime()
            val city = tracker.getCurrentCity()
            val elapsed = android.os.SystemClock.elapsedRealtime() - started
            android.util.Log.i("HavamaniaLocationQA", "restart=$restart elapsedMs=$elapsed stages=${stages.joinToString()}")
            assertNotNull("Location unavailable; restart=$restart elapsedMs=$elapsed stages=${stages.joinToString()}", city)
            assertTrue(city!!.name.isNotBlank() && city.name != "Mevcut konum")
            assertTrue(city.latitude in -90.0..90.0 && city.longitude in -180.0..180.0)
        } finally {
            if (restart) {
                shell("cmd location set-location-enabled $original")
                assertEquals(original, shell("cmd location is-location-enabled"))
            }
        }
    }
}
