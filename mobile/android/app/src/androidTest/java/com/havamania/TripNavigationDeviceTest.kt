package com.havamania

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TripNavigationDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun systemAndToolbarBackCloseGuideBeforeLeavingCalendar() {
        val guideOpen = mutableStateOf(true)
        val calendarOpen = mutableStateOf(true)
        compose.setContent {
            if (calendarOpen.value) {
                val leaveCalendar = { calendarOpen.value = false }
                BackHandler(onBack = leaveCalendar)
                val back = travelGuideBackAction(guideOpen.value, { guideOpen.value = false }, leaveCalendar)
                Column {
                    Text("Takvim")
                    if (guideOpen.value) Text("Seyahat rehberi")
                    TextButton(onClick = back) { Text("Geri") }
                }
            }
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        compose.onNodeWithText("Seyahat rehberi").assertIsDisplayed()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("Seyahat rehberi").assertDoesNotExist()
        compose.onNodeWithText("Takvim").assertIsDisplayed()

        compose.runOnIdle { guideOpen.value = true }
        compose.onNodeWithText("Geri").performClick()
        compose.onNodeWithText("Seyahat rehberi").assertDoesNotExist()
        compose.onNodeWithText("Takvim").assertIsDisplayed()

        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("Takvim").assertDoesNotExist()
    }

    @Test fun switchingMainTabsRemovesOutgoingScreenWithoutWaitingForFade() {
        lateinit var nav: NavHostController
        compose.setContent {
            nav = rememberNavController()
            NavHost(
                navController = nav,
                startDestination = Routes.WEATHER_ROOT,
                enterTransition = { appScreenEnterTransition() },
                exitTransition = { appScreenExitTransition() }
            ) {
                composable(Routes.WEATHER_ROOT) { Text("Hava içeriği") }
                composable(
                    "${Routes.CALENDAR_ROOT}?focusId={focusId}",
                    listOf(navArgument("focusId") { type = NavType.StringType; nullable = true; defaultValue = null })
                ) { Text("Takvim içeriği") }
            }
        }
        compose.onNodeWithText("Hava içeriği").assertIsDisplayed()
        compose.mainClock.autoAdvance = false
        try {
            compose.runOnIdle { nav.navigate(Routes.CALENDAR_ROOT) }
            compose.mainClock.advanceTimeBy(64)
            compose.onNodeWithText("Hava içeriği").assertDoesNotExist()
            compose.onNodeWithText("Takvim içeriği").assertIsDisplayed()

            compose.runOnIdle { nav.popBackStack() }
            compose.mainClock.advanceTimeBy(64)
            compose.onNodeWithText("Takvim içeriği").assertDoesNotExist()
            compose.onNodeWithText("Hava içeriği").assertIsDisplayed()
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }

    @Test fun finishingSavedChatReplacesOnlyTheCurrentEntryAndBackReturnsToHistory() {
        lateinit var nav: NavHostController
        compose.setContent {
            nav = rememberNavController()
            NavHost(nav, startDestination = Routes.AI_HISTORY) {
                composable(Routes.AI_HISTORY) {}
                composable("${Routes.AI_ROOT}?conversationId={conversationId}",
                    listOf(navArgument("conversationId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })) {}
            }
        }
        compose.runOnIdle {
            nav.navigate("${Routes.AI_ROOT}?conversationId=saved-chat")
            val saved = requireNotNull(nav.currentBackStackEntry)
            assertEquals("saved-chat", saved.arguments?.getString("conversationId"))
            assertTrue(nav.replaceSavedAssistantChat(saved))
            assertNull(nav.currentBackStackEntry?.arguments?.getString("conversationId"))
            assertFalse(nav.replaceSavedAssistantChat(saved))
            assertTrue(nav.popBackStack())
            assertEquals(Routes.AI_HISTORY, nav.currentDestination?.route)
        }
    }

    @Test fun routeAndCalendarReceiveTheOriginalTripId() {
        lateinit var nav: NavHostController
        compose.setContent {
            nav = rememberNavController()
            NavHost(nav, startDestination = Routes.WEATHER_ROOT) {
                composable(Routes.WEATHER_ROOT) {}
                composable(Routes.ROUTE_WEATHER, listOf(navArgument("tripId") { type = NavType.StringType })) {}
                composable("${Routes.CALENDAR_ROOT}?focusId={focusId}",
                    listOf(navArgument("focusId") { type = NavType.StringType; nullable = true })) {}
            }
        }
        val tripId = "İzmir/+?#%2F &"
        compose.runOnIdle {
            val source = requireNotNull(nav.currentBackStackEntry)
            assertTrue(nav.navigateFromActiveScreen(source, Routes.routeWeather(tripId)))
            assertEquals(tripId, nav.currentBackStackEntry?.arguments?.getString("tripId"))
            assertFalse(nav.navigateFromActiveScreen(source, Routes.routeWeather("another trip")) {
                launchSingleTop = true
            })
            assertEquals(tripId, nav.currentBackStackEntry?.arguments?.getString("tripId"))
            val detail = requireNotNull(nav.currentBackStackEntry)
            assertTrue(nav.popActiveScreen(detail))
            assertFalse(nav.popActiveScreen(detail))
            assertEquals(source, nav.currentBackStackEntry)
            nav.navigate(Routes.calendarTrip(tripId))
            assertEquals(tripId, nav.currentBackStackEntry?.arguments?.getString("focusId"))
        }
    }
}
