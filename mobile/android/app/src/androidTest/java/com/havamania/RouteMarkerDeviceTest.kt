package com.havamania

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

class RouteMarkerDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun tappingMarkerAfterRedrawSelectsCurrentPassage() {
        val point = GeoPoint(40.0, 29.0)
        val first = RouteWaypoint(point, 100.0)
        val second = first.copy(cumulativeDistanceMeters = 200.0)
        val waypointsById = mutableMapOf<Long, RouteWaypoint>()
        var map by mutableStateOf<MapLibreMap?>(null)
        var selected by mutableStateOf(emptyList<RouteWaypoint>())
        var chosen: RouteWaypoint? = null

        fun showMarker(target: MapLibreMap, waypoint: RouteWaypoint) {
            target.clear()
            waypointsById.clear()
            val position = LatLng(point.latitude, point.longitude)
            val marker = target.addMarker(MarkerOptions().position(position))
            waypointsById[marker.id] = waypoint
            target.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 13.0))
        }

        compose.setContent {
            MapLibre.getInstance(LocalContext.current, null, WellKnownTileServer.MapLibre)
            val view = rememberMapViewWithLifecycle()
            AndroidView(factory = { view }, modifier = Modifier.size(300.dp).testTag("route-map"))
            RouteMarkerSelection(map, waypointsById) { selected = it }
            if (selected.size > 1) {
                RoutePassageDialog(selected, onDismiss = { selected = emptyList() }) {
                    chosen = it
                    selected = emptyList()
                }
            }
            LaunchedEffect(view) {
                view.getMapAsync { ready ->
                    ready.setStyle(Style.Builder().fromJson("""{"version":8,"sources":{},"layers":[]}""")) {
                        showMarker(ready, first)
                        map = ready
                    }
                }
            }
        }
        compose.waitUntil(15_000) { map != null }
        fun tapMarker() {
            compose.onNodeWithTag("route-map").performTouchInput {
                click(center - Offset(0f, 12.dp.toPx()))
            }
        }
        tapMarker()
        compose.waitUntil(5_000) { selected == listOf(first) }
        compose.runOnIdle { showMarker(requireNotNull(map), second) }
        tapMarker()
        compose.waitUntil(5_000) { selected == listOf(second) }
        compose.runOnIdle { assertEquals(listOf(second), selected)
            val extra = requireNotNull(map).addMarker(MarkerOptions().position(LatLng(point.latitude, point.longitude)))
            waypointsById[extra.id] = first
        }
        tapMarker()
        compose.waitUntil(5_000) { selected == listOf(first, second) }
        compose.onNodeWithText("Bu noktadan geçişler").assertIsDisplayed()
        compose.onNodeWithText("1. geçiş").assertIsDisplayed()
        compose.onNodeWithText("2. geçiş").performClick()
        compose.onNodeWithText("Bu noktadan geçişler").assertDoesNotExist()
        compose.runOnIdle { assertEquals(second, chosen) }
    }
}
