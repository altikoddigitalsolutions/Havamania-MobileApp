package com.havamania

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class WeatherLocationAccessDeviceTest {
    @get:Rule val compose = createComposeRule()
    private class Gateway : WeatherLocationGateway {
        var granted = false; var enabled = true; var requested = 0; var appSettings = 0; var gpsSettings = 0
        override fun hasPermission() = granted
        override fun locationEnabled() = enabled
        override fun requestPermission() { requested++ }
        override fun openAppSettings() { appSettings++ }
        override fun openLocationSettings() { gpsSettings++ }
    }
    private var located = 0
    private fun show(controller: WeatherLocationAccessController) {
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                ProvideResponsiveLayout { MaterialTheme {
                    var manual by remember { mutableStateOf(false) }
                    if (manual) Text("Manuel konum seçimi")
                    else NoCitiesEmptyState(onUseLocation = { controller.start() }, onAddClick = { manual = true })
                    WeatherLocationAccessDialog(controller) { manual = true }
                } }
            }
        }
    }

    @Test fun permissionRationaleCanBeDeclinedAndManualSelectionStaysAccessible() {
        val gateway = Gateway()
        val controller = WeatherLocationAccessController(gateway, { located++ }).apply { offer(true) }
        show(controller)
        compose.onNodeWithText("Konumu elle seç").performClick()
        compose.onNodeWithText("Manuel konum seçimi").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, gateway.requested); assertEquals(0, located) }
    }

    @Test fun deniedPermissionCanOpenSettingsAndReturnWithGrantOnlyOnce() {
        val gateway = Gateway()
        val controller = WeatherLocationAccessController(gateway, { located++ }).apply { offer(true) }
        show(controller)
        compose.onNodeWithText("İzin ver").performClick()
        compose.runOnIdle { controller.permissionResult(false) }
        compose.onNodeWithText("Konum izni verilmedi").assertIsDisplayed()
        compose.onNodeWithText("Uygulama ayarlarını aç").performClick()
        compose.runOnIdle { assertEquals(1, gateway.appSettings); gateway.granted = true; controller.resumed(); controller.resumed() }
        compose.onNodeWithText("Konum izni verilmedi").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, located) }
    }

    @Test fun gpsDisabledOffersSettingsAndManualFallbackWithoutLookup() {
        val gateway = Gateway().apply { granted = true; enabled = false }
        val controller = WeatherLocationAccessController(gateway, { located++ }).apply { start() }
        show(controller)
        compose.onNodeWithText("Cihaz konumu kapalı").assertIsDisplayed()
        compose.onNodeWithText("Konum ayarlarını aç").performClick()
        compose.runOnIdle { controller.resumed(); assertEquals(1, gateway.gpsSettings) }
        compose.onNodeWithText("Cihaz konumu kapalı").assertIsDisplayed()
        compose.onNodeWithText("Konumu elle seç").performClick()
        compose.onNodeWithText("Manuel konum seçimi").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, located) }
    }

    @Test fun unavailableLookupExplainsRetryAndBusyStateKeepsManualSelectionEnabled() {
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                ProvideResponsiveLayout { MaterialTheme {
                    var manual by remember { mutableStateOf(false) }
                    if (manual) Text("Manuel konum seçimi")
                    else NoCitiesEmptyState(onUseLocation = {}, locating = true,
                        locationNotice = "Mevcut konumdan şehir bilgisi alınamadı.", onAddClick = { manual = true })
                } }
            }
        }
        compose.onNodeWithText("Mevcut konumdan şehir bilgisi alınamadı.").assertIsDisplayed()
        compose.onNodeWithText("Konum alınıyor…").assertIsNotEnabled()
        compose.onNodeWithText("Konum Seç").assertIsEnabled().performClick()
        compose.onNodeWithText("Manuel konum seçimi").assertIsDisplayed()
    }
}
