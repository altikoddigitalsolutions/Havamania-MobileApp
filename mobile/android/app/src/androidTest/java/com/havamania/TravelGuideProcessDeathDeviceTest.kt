package com.havamania

import android.os.ParcelFileDescriptor
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Instrumentation survives in main QA process; only the named child is killed. */
class TravelGuideProcessDeathDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val automation get() = instrumentation.uiAutomation
    private val component = "com.havamania/com.havamania.TravelGuideProcessDeathActivity"
    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        automation.executeShellCommand(command)).bufferedReader().use { it.readText() }
    private fun status(): JSONObject? = runCatching {
        JSONObject(File(instrumentation.targetContext.filesDir, "guide-process-qa.json").readText())
    }.getOrNull()
    private fun await(message: String, condition: () -> Boolean) {
        val deadline = android.os.SystemClock.uptimeMillis() + 15_000
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(150)
        }
        val labels = mutableListOf<String>()
        val root = automation.rootInActiveWindow
        if (root?.packageName?.toString() == "com.havamania") {
            val nodes = java.util.ArrayDeque<AccessibilityNodeInfo>()
            nodes.add(root)
            while (nodes.isNotEmpty() && labels.size < 60) {
                val node = nodes.removeFirst()
                if (node.isVisibleToUser) {
                    node.text?.toString()?.let(labels::add)
                    node.contentDescription?.toString()?.let(labels::add)
                }
                for (i in 0 until node.childCount) node.getChild(i)?.let(nodes::add)
            }
            shell("mkdir -p /sdcard/Pictures/Havamania-QA/guide-process-20261127")
            shell("screencap -p /sdcard/Pictures/Havamania-QA/guide-process-20261127/failure.png")
        }
        fail("$message; qaStatus=${status()}; QA labels=$labels")
    }
    private fun find(text: String): AccessibilityNodeInfo? {
        val root = automation.rootInActiveWindow ?: return null
        val queue = java.util.ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (!node.refresh()) continue
            if (node.isVisibleToUser && (node.text?.toString()?.contains(text) == true ||
                node.contentDescription?.toString()?.contains(text) == true)) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return null
    }
    private fun click(text: String) {
        await("Missing control: $text") { find(text) != null }
        var node = find(text)
        while (node != null && !node.isClickable) node = node.parent
        checkNotNull(node) { "No clickable ancestor: $text" }
        assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }
    private fun relaunchAfterKill(): Int {
        val before = checkNotNull(status()).getInt("pid")
        assertNotEquals(android.os.Process.myPid(), before)
        shell("input keyevent KEYCODE_HOME")
        await("Child state was not saved") { status()?.optBoolean("saved") == true }
        val processes = shell("ps -A -o PID,NAME")
        assertTrue(processes.lineSequence().any { line ->
            val fields = line.trim().split(Regex("\\s+"))
            fields.size == 2 && fields[0] == before.toString() && fields[1] == "com.havamania:guide_process_qa"
        })
        val manager = instrumentation.targetContext.getSystemService(android.app.ActivityManager::class.java)
        val task = manager.appTasks.firstOrNull {
            it.taskInfo.baseActivity?.className == "com.havamania.TravelGuideProcessDeathActivity"
        }
        assertNotNull("QA task must remain in Recents", task)
        android.os.Process.killProcess(before)
        await("Old QA process still exists") {
            shell("ps -A -o PID,NAME").lineSequence().none { line ->
                val fields = line.trim().split(Regex("\\s+"))
                fields.size == 2 && fields[0] == before.toString() && fields[1] == "com.havamania:guide_process_qa"
            }
        }
        val disk = androidx.room.Room.databaseBuilder(instrumentation.targetContext,
            WeatherDatabase::class.java, "guide-process-qa.db").build()
        try {
            kotlinx.coroutines.runBlocking {
                assertEquals(setOf("process-guide-upcoming", "process-guide-past"),
                    disk.weatherDao().getAllTravelPlans("legacy").map { it.id }.toSet())
            }
        } finally { disk.close() }
        Thread.sleep(1_000) // Allow ActivityManager to detach the dead process before task resume.
        instrumentation.runOnMainSync { task!!.moveToFront() }
        await("No new restored child process") {
            val current = status()
            current != null && current.optInt("pid") != before && current.optBoolean("restored")
        }
        return checkNotNull(status()).getInt("pid")
    }
    @Test fun diskTripsAndGuideChoicesSurviveRealChildProcessDeath() {
        assertEquals("com.havamania", instrumentation.targetContext.packageName)
        assertNull(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser)
        try {
            shell("am start -W -n $component")
            await("Initial guide missing") { find("Rehberi kapat") != null }
            click("Rehberi kapat")
            val first = relaunchAfterKill()
            await("Closed guide choice lost") { find("Rehberi aç") != null }
            click("Geçmiş")
            await("Disk past trip missing") { find("Bursa") != null }
            click("Rehberi aç")
            await("Past guide did not open") { find("Rehberi kapat") != null }
            val second = relaunchAfterKill()
            assertNotEquals(first, second)
            await("Past trip/filter lost after process death") { find("Bursa") != null }
            await("Open guide choice lost after process death") { find("Rehberi kapat") != null }
        } finally {
            shell("am start -W -n $component --ez qa_finish true")
        }
    }
}
