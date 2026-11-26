package com.havamania

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.graphics.Bitmap
import android.provider.MediaStore
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class AssistantPhotoPickerSystemTest {
    @get:Rule val compose = createAndroidComposeRule<AssistantPhotoPickerTestActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val automation get() = instrumentation.uiAutomation

    @Test fun actualSystemPickerCancelReturnsWithoutPhotos() {
        openPicker()
        assertTrue(automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK))
        waitFor("Picker cancellation did not return") { compose.activity.returns == 1 }
        compose.runOnIdle { assertTrue(compose.activity.photos.isEmpty()); assertNull(compose.activity.error) }
        compose.onNodeWithContentDescription("Fotoğraf ekle").assertIsEnabled()
    }

    @Test fun selectionTokenStaysBoundToLaunchingOwner() {
        openPicker()
        instrumentation.runOnMainSync { compose.activity.owner = "fixture-B" }
        automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        waitFor("Picker cancellation did not return") { compose.activity.returns == 1 }
        compose.runOnIdle { assertEquals("fixture-A", compose.activity.lastToken); assertTrue(compose.activity.photos.isEmpty()) }
    }

    @Test fun rapidDoubleLaunchCreatesOnePicker() {
        compose.runOnIdle {
            val open = checkNotNull(compose.activity.openPickerForTest)
            open()
            open()
        }
        waitFor("System picker did not open") {
            automation.rootInActiveWindow?.packageName?.toString()?.let { it != compose.activity.packageName } == true
        }
        automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        waitFor("Duplicate picker remained open") { compose.activity.returns == 1 &&
            automation.rootInActiveWindow?.packageName?.toString() == compose.activity.packageName }
    }

    @Test fun unavailableAndDeniedPickerAreSafe() {
        var errors = 0
        assistantPhotoPickerLaunch({ throw ActivityNotFoundException() }, { errors++ })
        assistantPhotoPickerLaunch({ throw SecurityException() }, { errors++ })
        assistantPhotoPickerLaunch({}, { errors++ })
        assertEquals(2, errors)
    }

    @Test fun ownPhotoFromActualPickerIsPreparedAndRemovable() {
        val resolver = compose.activity.contentResolver
        val name = "QA-Picker-${UUID.randomUUID()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Havamania-Picker-QA")
            put(MediaStore.Images.Media.IS_PENDING, 1)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        try {
            val bitmap = Bitmap.createBitmap(240, 160, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.BLUE)
            try { resolver.openOutputStream(uri)!!.use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) } }
            finally { bitmap.recycle() }
            assertEquals(1, resolver.update(uri, ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }, null, null))
            resolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.IS_PENDING,
                MediaStore.Images.Media.SIZE), null, null, null)!!.use {
                assertTrue(it.moveToFirst()); assertEquals(name, it.getString(0))
                assertEquals(0, it.getInt(1)); assertTrue(it.getLong(2) > 0)
            }
            compose.runOnIdle { compose.activity.expectedName = name; compose.activity.expectedMediaId = uri.lastPathSegment }
            openPicker()
            waitFor("Could not select the owned test fixture", 15000) {
                if (clickText(name) || clickText(name.removeSuffix(".jpg"))) return@waitFor true
                // Native picker exposes thumbnails, not filenames. Only try the newest
                // item; the fixture rejects any URI whose DISPLAY_NAME is not ours
                // before reading image bytes. No other photos are prepared or uploaded.
                val root = automation.rootInActiveWindow ?: return@waitFor false
                val thumbnail = find(root) {
                    it.viewIdResourceName?.endsWith("/icon_thumbnail") == true
                } ?: return@waitFor false
                var target: AccessibilityNodeInfo? = thumbnail
                repeat(5) {
                    val current = target ?: return@repeat
                    if (current.isClickable && current.isEnabled) return@waitFor current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    target = current.parent
                }
                false
            }
            waitFor("Photo selection did not return", 12000) {
                if (compose.activity.returns > 0) true
                else { listOf("Ekle", "Add", "Aç", "Open", "Bitti", "Done", "Tamam", "OK", "Seç", "Select").any { clickText(it) }; false }
            }
            compose.waitUntil(10000) { compose.activity.photos.size == 1 || compose.activity.error != null }
            compose.runOnIdle { assertNull(compose.activity.error); assertEquals(1, compose.activity.photos.size) }
            compose.onNodeWithContentDescription("Eklenen fotoğraf 1").assertIsDisplayed()
            compose.onNodeWithContentDescription("Fotoğraf 1 ekini kaldır").performClick()
            compose.runOnIdle { assertTrue(compose.activity.photos.isEmpty()) }
        } finally {
            if (compose.activity.returns == 0) automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            resolver.delete(uri, null, null) // Only the URI created by this test.
        }
    }

    private fun openPicker() {
        val info = automation.serviceInfo
        info.flags = info.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        automation.serviceInfo = info
        compose.onNodeWithContentDescription("Fotoğraf ekle").performClick()
        waitFor("System picker did not open") {
            automation.rootInActiveWindow?.packageName?.toString()?.let { it != compose.activity.packageName } == true
        }
    }
    private fun clickText(text: String): Boolean {
        val root = automation.rootInActiveWindow ?: return false
        val node = root.findAccessibilityNodeInfosByText(text).firstOrNull {
            val label = it.text?.toString() ?: it.contentDescription?.toString()
            label == text || (text in setOf("Ekle", "Add", "Aç", "Open", "Bitti", "Done", "Tamam", "OK", "Seç", "Select") &&
                label?.startsWith("$text (") == true && label.endsWith(")") &&
                label.removePrefix("$text (").removeSuffix(")").toIntOrNull() in 1..4)
        } ?: return false
        var target: AccessibilityNodeInfo? = node
        repeat(4) {
            val current = target ?: return false
            if (current.isClickable && current.isEnabled) return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            target = current.parent
        }
        return false
    }
    private fun find(node: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (predicate(node)) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            find(child, predicate)?.let { return it }
        }
        return null
    }
    private fun pickerDiagnostics(): String {
        val root = automation.rootInActiveWindow ?: return "no active accessibility window"
        val known = setOf("Recent", "Son", "Resimler", "Images", "Fotoğraflar", "Photos", "Albümler", "Albums", "Downloads", "İndirilenler", "Show roots", "Kökleri göster")
        val nodes = mutableListOf<String>()
        fun visit(node: AccessibilityNodeInfo) {
            val label = node.text?.toString() ?: node.contentDescription?.toString().orEmpty()
            val safe = label.takeIf { it in known || it.startsWith("Havamania-Picker-QA") || it.startsWith("QA-Picker-") }.orEmpty()
            node.viewIdResourceName?.let { nodes += "$it:${node.className}:${node.isClickable}:$safe" }
            for (i in 0 until node.childCount) node.getChild(i)?.let { visit(it) }
        }
        visit(root)
        return "package=${root.packageName};" + nodes.distinct().joinToString(";").take(4000)
    }
    private fun waitFor(message: String, timeout: Long = 10000, condition: () -> Boolean) {
        val until = android.os.SystemClock.uptimeMillis() + timeout
        while (android.os.SystemClock.uptimeMillis() < until) {
            if (condition()) return
            Thread.sleep(200)
        }
        fail(message + "; " + pickerDiagnostics())
    }
}
