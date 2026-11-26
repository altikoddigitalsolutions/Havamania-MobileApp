package com.havamania

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class HeroSystemMotionDeviceTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .bufferedReader().use { it.readText().trim() }
    private fun awaitScale(scenario: ActivityScenario<HeroMotionTestActivity>, expected: Float) {
        val deadline = SystemClock.uptimeMillis() + 10000
        var value = -1f
        while (SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity { value = it.observedScale }
            if (value == expected) return
            SystemClock.sleep(50)
        }
        assertEquals("Real window must observe changed system animation scale", expected, value, 0f)
    }
    @Test fun realWindowObservesSystemScaleWhileOpenAndAfterRecreation() {
        val old = shell("settings get global animator_duration_scale")
        var scenario: ActivityScenario<HeroMotionTestActivity>? = null
        try {
            shell("settings put global animator_duration_scale 1")
            val opened = ActivityScenario.launch(HeroMotionTestActivity::class.java)
            scenario = opened
            awaitScale(opened, 1f)
            shell("settings put global animator_duration_scale 0")
            awaitScale(opened, 0f)
            opened.recreate()
            awaitScale(opened, 0f)
            shell("settings put global animator_duration_scale 1")
            awaitScale(opened, 1f)
        } finally {
            scenario?.close()
            if (old == "null") shell("settings delete global animator_duration_scale")
            else shell("settings put global animator_duration_scale $old")
        }
    }
}
