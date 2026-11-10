package com.havamania

import android.database.sqlite.SQLiteException
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
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.havamania.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Fake app/system states. Never changes Android permissions or real preferences. */
class NotificationSettingsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun retryButtonShowsSavedNotificationAfterRoomReadFailure() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java).build()
        try {
            val dao = database.notificationDao()
            val repo = NotificationRepository(dao)
            runBlocking {
                dao.insert(NotificationItem(id = "saved", userId = "test-owner", title = "Kayıtlı bildirim"))
            }
            var attempt by mutableIntStateOf(0)
            compose.setContent {
                var result by remember { mutableStateOf<Result<List<NotificationItem>>?>(null) }
                LaunchedEffect(attempt) {
                    val source = if (attempt == 0) flow { throw SQLiteException("Injected read failure") }
                    else repo.getAllNotificationsFlow("test-owner")
                    result = source.inboxReadResults().first()
                }
                HavamaniaTheme {
                    if (result?.isFailure == true) {
                        NotificationLoadError {
                            result = null
                            attempt++
                        }
                    } else {
                        result?.getOrNull().orEmpty().forEach { item ->
                            SwipeableNotificationItem(
                                notification = item,
                                isSelected = false,
                                isSelectionMode = false,
                                onClick = {},
                                onLongClick = {},
                                onDelete = {},
                                onToggleRead = {},
                                onToggleSelection = {},
                                onNavigateToDetail = { _, _ -> }
                            )
                        }
                    }
                }
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("Tekrar dene").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Tekrar dene").performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("Kayıtlı bildirim").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Kayıtlı bildirim").assertIsDisplayed()
            compose.onNodeWithText("Bildirimler yüklenemedi").assertDoesNotExist()
            compose.runOnIdle { assertEquals(1, attempt) }
        } finally {
            database.close()
        }
    }

    @Test fun notificationLoadErrorAllowsRetry() {
        var failed by mutableStateOf(true)
        var retries = 0
        compose.setContent {
            HavamaniaTheme {
                if (failed) NotificationLoadError {
                    retries++
                    failed = false
                } else {
                    EmptyNotificationState(hasNotifications = false, onResetFilter = {})
                }
            }
        }
        compose.onNodeWithText("Bildirimler yüklenemedi").assertIsDisplayed()
        compose.onNodeWithText("Henüz bildirim yok").assertDoesNotExist()
        compose.onNodeWithText("Tekrar dene").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, retries) }
        compose.onNodeWithText("Bildirimler yüklenemedi").assertDoesNotExist()
        compose.onNodeWithText("Henüz bildirim yok").assertIsDisplayed()
    }

    @Test fun emptyInboxAndEmptyFilterShowDifferentActions() {
        var hasNotifications by mutableStateOf(false)
        var resets = 0
        compose.setContent {
            HavamaniaTheme { EmptyNotificationState(hasNotifications) { resets++ } }
        }
        val reset = compose.onNodeWithText("Filtreyi temizle")
        compose.onNodeWithText("Henüz bildirim yok").assertIsDisplayed()
        reset.assertDoesNotExist()
        compose.runOnIdle { hasNotifications = true }
        compose.onNodeWithText("Bu filtrede bildirim yok").assertIsDisplayed()
        reset.assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, resets) }
    }

    @Test fun notificationFilterExposesTheSelectedChip() {
        var filter by mutableStateOf(NotificationFilter.ALL)
        compose.setContent {
            HavamaniaTheme { NotificationFilterRow(filter) { filter = it } }
        }
        val all = compose.onNodeWithText("Tümü")
        val unread = compose.onNodeWithText("Okunmamış")
        all.assertIsSelected()
        unread.assertIsNotSelected().performClick()
        unread.assertIsSelected()
        all.assertIsNotSelected()
    }

    @Test fun notificationRowKeepsReadStatusInSelectionMode() {
        var selected by mutableStateOf(false)
        compose.setContent {
            HavamaniaTheme {
                SwipeableNotificationItem(
                    notification = NotificationItem(id = "notice", title = "Test bildirimi"),
                    isSelected = selected,
                    isSelectionMode = true,
                    onClick = {},
                    onLongClick = {},
                    onDelete = {},
                    onToggleRead = {},
                    onToggleSelection = { selected = !selected },
                    onNavigateToDetail = { _, _ -> }
                )
            }
        }
        val stateDescription = androidx.compose.ui.semantics.SemanticsProperties.StateDescription
        compose.onNode(SemanticsMatcher.expectValue(stateDescription, "Seçili değil, Okunmadı"))
            .assertIsDisplayed().performClick()
        compose.onNode(SemanticsMatcher.expectValue(stateDescription, "Seçili, Okunmadı")).assertIsDisplayed()
    }

    @Test fun selectAllButtonAnnouncesItsCurrentAction() {
        var selection by mutableStateOf(NotificationUiState(
            filteredNotifications = listOf(NotificationItem(id = "a"), NotificationItem(id = "b")),
            selectedIds = setOf("a"),
            isSelectionMode = true
        ))
        compose.setContent {
            HavamaniaTheme {
                NotificationSelectAllButton(selection) { selection = selection.toggleVisibleSelection() }
            }
        }
        compose.onNodeWithContentDescription("Tümünü seç").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(setOf("a", "b"), selection.selectedIds) }
        compose.onNodeWithContentDescription("Seçimi kaldır").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(selection.selectedIds.isEmpty()) }
        compose.onNodeWithContentDescription("Tümünü seç").assertIsDisplayed()
    }
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
