package com.havamania

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class WeatherLocationAccessTest {
    private class Gateway : WeatherLocationGateway {
        var granted = false
        var enabled = true
        var requests = 0
        var appSettings = 0
        var gpsSettings = 0
        var failSettings = false
        override fun hasPermission() = granted
        override fun locationEnabled() = enabled
        override fun requestPermission() { requests++ }
        override fun openAppSettings() { if (failSettings) error("no activity"); appSettings++ }
        override fun openLocationSettings() { if (failSettings) error("no activity"); gpsSettings++ }
    }

    @Test fun deniedPermissionDoesNotLocateOrReopenUntilExplicitRetry() {
        val gateway = Gateway(); var calls = 0
        val controller = WeatherLocationAccessController(gateway, { calls++ })
        controller.offer(true)
        assertEquals(LocationAccessDialog.RATIONALE, controller.state.value.dialog)
        controller.requestPermission(); controller.requestPermission()
        assertEquals(1, gateway.requests)
        controller.permissionResult(false)
        assertEquals(LocationAccessDialog.DENIED, controller.state.value.dialog)
        controller.dismiss(); controller.offer(true); controller.resumed()
        assertEquals(LocationAccessDialog.NONE, controller.state.value.dialog)
        assertEquals(0, calls)
        controller.start()
        assertEquals(LocationAccessDialog.RATIONALE, controller.state.value.dialog)
    }

    @Test fun grantingPermissionLocatesOnceAndNormalResumeDoesNotRepeat() {
        val gateway = Gateway(); var calls = 0
        val controller = WeatherLocationAccessController(gateway, { calls++ })
        controller.offer(true); controller.requestPermission(); gateway.granted = true
        controller.permissionResult(true); controller.permissionResult(true); controller.resumed()
        assertEquals(1, calls)
        assertEquals(LocationAccessDialog.NONE, controller.state.value.dialog)
    }

    @Test fun gpsDisabledBlocksLookupAndSettingsReturnRetriesOnlyWhenEnabled() {
        val gateway = Gateway().apply { granted = true; enabled = false }; var calls = 0
        val controller = WeatherLocationAccessController(gateway, { calls++ })
        controller.start()
        assertEquals(LocationAccessDialog.GPS_DISABLED, controller.state.value.dialog)
        assertEquals(0, calls)
        controller.settings(true); controller.resumed()
        assertEquals(LocationAccessDialog.GPS_DISABLED, controller.state.value.dialog)
        controller.settings(true); gateway.enabled = true; controller.resumed(); controller.resumed()
        assertEquals(2, gateway.gpsSettings)
        assertEquals(1, calls)
    }

    @Test fun deniedSettingsReturnDoesNotLoopAndGrantAfterRecreationIsConsumedOnce() {
        val gateway = Gateway(); var calls = 0
        var controller = WeatherLocationAccessController(gateway, { calls++ })
        controller.start(); controller.requestPermission(); controller.permissionResult(false)
        controller.settings(false); controller.resumed()
        assertEquals(LocationAccessDialog.DENIED, controller.state.value.dialog)
        assertEquals(1, gateway.appSettings)
        controller.settings(false)
        val snapshot = Json.encodeToString(WeatherLocationAccessState.serializer(), controller.state.value)
        controller.close()
        controller = WeatherLocationAccessController(gateway, { calls++ },
            Json.decodeFromString(WeatherLocationAccessState.serializer(), snapshot))
        gateway.granted = true; controller.resumed(); controller.resumed()
        assertEquals(1, calls)
    }

    @Test fun alreadyAutomaticLookupIsNotStartedTwiceButManualRetryWorks() {
        val gateway = Gateway().apply { granted = true }; var calls = 0
        val controller = WeatherLocationAccessController(gateway, { calls++ })
        controller.offer(false); controller.offer(true)
        assertEquals(0, calls)
        controller.start(); assertEquals(1, calls)
    }

    @Test fun closedSessionOrManualSelectionIgnoresLatePermissionResult() {
        val gateway = Gateway(); var calls = 0
        val controller = WeatherLocationAccessController(gateway, { calls++ })
        controller.start(); controller.requestPermission(); controller.dismiss()
        gateway.granted = true; controller.permissionResult(true)
        assertEquals(0, calls)
        gateway.granted = false; controller.start(); controller.requestPermission(); controller.close()
        gateway.granted = true; controller.permissionResult(true); controller.resumed(); controller.start()
        assertEquals(0, calls)
    }

    @Test fun unavailableSettingsRemainDismissibleWithoutLocationRequest() {
        val gateway = Gateway().apply { failSettings = true }; var calls = 0
        val controller = WeatherLocationAccessController(gateway, { calls++ })
        controller.settings(false)
        assertEquals(LocationAccessDialog.ERROR, controller.state.value.dialog)
        assertFalse(controller.state.value.awaitingSettings)
        controller.dismiss()
        assertEquals(LocationAccessDialog.NONE, controller.state.value.dialog)
        assertEquals(0, calls)
    }
}
