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

/** Run only in the isolated QA application, after granting its coarse location permission. */
@androidx.test.filters.SdkSuppress(minSdkVersion = 28)
class CurrentLocationDeviceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun approximateLocationPermissionCanResolveCurrentCity() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assertEquals("Location QA must never target the Play package", "com.havamania", context.packageName)
        compose.setContent { Text("Konum izni testi") }
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        assertEquals(PackageManager.PERMISSION_GRANTED,
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION))
        assertTrue(PermissionHelper.hasLocationPermission(context))
        val tracker = DefaultLocationTracker(LocationServices.getFusedLocationProviderClient(context),
            context.applicationContext as Application)
        val city = tracker.getCurrentCity()
        assertNotNull("No current location/address was supplied by this device; real GPS/geocoder validation remains open", city)
        assertTrue(city!!.name.isNotBlank())
        assertTrue(city.latitude in -90.0..90.0 && city.longitude in -180.0..180.0)
    }
}
