package com.havamania

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test

/** Real Android permission dialog and settings, only for the isolated QA package. */
class WeatherLocationNativePermissionDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun systemDenialCanReturnFromAppSettingsAndKeepManualSelection() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assertEquals("com.havamania", context.packageName)
        assumeFalse("Native denial requires a QA installation without location permission", PermissionHelper.hasLocationPermission(context))
        var located = 0
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                ProvideResponsiveLayout { MaterialTheme {
                    var manual by remember { mutableStateOf(false) }
                    WeatherLocationAccess(1, !manual, true, { located++ }, { manual = true }) { locate ->
                        if (manual) Text("Manuel konum seçimi")
                        else NoCitiesEmptyState(onUseLocation = locate, onAddClick = { manual = true })
                    }
                } }
            }
        }
        val automation = instrumentation.uiAutomation
        val originalFlags = automation.serviceInfo.flags
        automation.serviceInfo = automation.serviceInfo.apply { flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS }
        try {
            compose.onNodeWithText("İzin ver").performClick()
            var deny: AccessibilityNodeInfo? = null
            var immediatelyDenied = false
            compose.waitUntil(10_000) {
                deny = sequenceOf("com.android.permissioncontroller", "com.google.android.permissioncontroller")
                    .mapNotNull { pkg -> automation.rootInActiveWindow?.findAccessibilityNodeInfosByViewId("$pkg:id/permission_deny_button")?.firstOrNull() }
                    .firstOrNull()
                immediatelyDenied = deny == null && runCatching {
                    compose.onAllNodesWithText("Konum izni verilmedi").fetchSemanticsNodes().isNotEmpty()
                }.getOrDefault(false)
                deny != null || immediatelyDenied
            }
            if (deny != null) assertTrue(deny!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            else assertTrue(immediatelyDenied)
            android.util.Log.i("HavamaniaLocationQA", if (deny != null) "Native permission dialog denied" else "Native API denied without showing a dialog")
            compose.waitUntil(10_000) { automation.rootInActiveWindow?.packageName?.toString() == context.packageName }
            compose.onNodeWithText("Konum izni verilmedi").assertIsDisplayed()
            compose.onNodeWithText("Uygulama ayarlarını aç").performClick()
            compose.waitUntil(10_000) { automation.rootInActiveWindow?.packageName?.toString()?.contains("settings") == true }
            assertTrue(automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK))
            compose.waitUntil(10_000) { automation.rootInActiveWindow?.packageName?.toString() == context.packageName }
            compose.onNodeWithText("Konum izni verilmedi").assertIsDisplayed()
            compose.onNodeWithText("Konumu elle seç").performClick()
            compose.onNodeWithText("Manuel konum seçimi").assertIsDisplayed()
            compose.runOnIdle { assertEquals(0, located) }
            assertFalse(PermissionHelper.hasLocationPermission(context))
        } finally {
            automation.serviceInfo = automation.serviceInfo.apply { flags = originalFlags }
        }
    }
}
