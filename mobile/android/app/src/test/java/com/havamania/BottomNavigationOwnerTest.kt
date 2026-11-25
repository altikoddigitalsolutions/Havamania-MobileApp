package com.havamania

import org.junit.Assert.*
import org.junit.Test

class BottomNavigationOwnerTest {
    @Test fun `subscreens and query routes select their parent tab`() {
        mapOf("root_weather?city=a" to Routes.WEATHER_ROOT,
            "root_calendar?focusId=abc" to Routes.CALENDAR_ROOT,
            "sub_route_weather/trip" to Routes.CALENDAR_ROOT,
            "sub_ai_history_detail/item" to Routes.AI_ROOT,
            Routes.SMART_ALERTS to Routes.PROFILE_ROOT).forEach { (route, parent) ->
                assertEquals(route, parent, bottomNavigationOwner(route))
            }
    }
    @Test fun `unknown routes never inherit a prefix selection`() {
        listOf(null, "", "root_calendar_other", "root_ai_unknown", "sub_ai_history_other").forEach {
            assertNull(it, bottomNavigationOwner(it))
        }
    }
}
