package com.havamania

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Fake app/system states. Never changes Android permissions or real preferences. */
class NotificationSettingsUiTest {
    @get:Rule val compose = createComposeRule()
    private val blocked = "Android bildirim izni kapalı. Bildirim alabilmek için sistem ayarlarından izin verin."
    private fun show(theme: AppTheme, enabled: MutableState<Boolean>, allowed: MutableState<Boolean>,
        lifecycleOwner: LifecycleOwner? = null, readPermission: (() -> Boolean)? = null, open: () -> Unit) {
        compose.setContent {
            val colors = ThemeFactory.createColors(theme)
            val density = LocalDensity.current.density
            val owner = lifecycleOwner ?: LocalLifecycleOwner.current
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalDensity provides Density(density, 2f), LocalLifecycleOwner provides owner) {
                val access = if (readPermission == null) allowed.value else rememberSystemNotificationAccess { readPermission() }.value
                ProvideResponsiveLayout { MaterialTheme {
                    NotificationSettingsSection(enabled.value, access, { enabled.value = it }, open)
                } }
            }
        }
    }

    @Test fun enabledPreferenceDoesNotHideSystemDenialAndSettingsButtonIsActionable() {
        val enabled = mutableStateOf(true)
        val allowed = mutableStateOf(false)
        var opens = 0
        show(AppTheme.LIGHT, enabled, allowed) { opens++ }
        compose.onNode(isToggleable()).assertIsOn()
        compose.onNodeWithText(blocked).assertIsDisplayed()
        compose.onNodeWithText("Android ayarlarını aç").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, opens); assertTrue(enabled.value) }
    }

    @Test fun offPreferenceDoesNotPromptUntilUserEnablesIt() {
        val enabled = mutableStateOf(false)
        val allowed = mutableStateOf(false)
        show(AppTheme.DARK, enabled, allowed) { fail("No automatic settings launch") }
        compose.onNode(isToggleable()).assertIsOff()
        compose.onNodeWithText(blocked).assertDoesNotExist()
        compose.onNodeWithText("Android ayarlarını aç").assertDoesNotExist()
        compose.onNode(isToggleable()).performClick().assertIsOn()
        compose.onNodeWithText(blocked).assertIsDisplayed()
    }

    @Test fun changedSystemStateRemovesStaleWarningWithoutChangingPreference() {
        val enabled = mutableStateOf(true)
        val allowed = mutableStateOf(false)
        show(AppTheme.DARK, enabled, allowed) {}
        compose.onNodeWithText(blocked).assertIsDisplayed()
        compose.runOnIdle { allowed.value = true }
        compose.onNodeWithText(blocked).assertDoesNotExist()
        compose.onNode(isToggleable()).assertIsOn()
        compose.onNodeWithText("Android ayarlarını aç").assertIsDisplayed()
    }

    @Test fun returningToResumedLifecycleActuallyRechecksPermissionReader() {
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this)
            override val lifecycle: Lifecycle get() = registry
        }
        val enabled = mutableStateOf(true)
        var actualAllowed = false
        var reads = 0
        show(AppTheme.LIGHT, enabled, mutableStateOf(false), lifecycleOwner = owner,
            readPermission = { reads++; actualAllowed }) { fail("No automatic launch") }
        compose.onNodeWithText(blocked).assertIsDisplayed()
        compose.runOnIdle {
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            actualAllowed = true
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        compose.onNodeWithText(blocked).assertDoesNotExist()
        compose.runOnIdle { assertTrue(reads >= 2); assertTrue(enabled.value) }
    }
}
