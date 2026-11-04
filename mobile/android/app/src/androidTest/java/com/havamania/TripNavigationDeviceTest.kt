package com.havamania

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TripNavigationDeviceTest {
    @get:Rule val compose = createComposeRule()

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
            nav.navigate(Routes.routeWeather(tripId))
            assertEquals(tripId, nav.currentBackStackEntry?.arguments?.getString("tripId"))
            nav.navigate(Routes.calendarTrip(tripId))
            assertEquals(tripId, nav.currentBackStackEntry?.arguments?.getString("focusId"))
        }
    }
}
