package com.havamania

object MapStyleProvider {
    /**
     * OpenFreeMap Liberty style: Free, open-source vector/raster road map style with full road network,
     * highways, primary/secondary streets, water bodies, and place labels. Requires NO API key and NO account.
     * Attribution: OpenStreetMap / OpenFreeMap / MapLibre
     */
    const val ROAD_MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

    /**
     * Fallback demo map style
     */
    const val DEMO_MAP_STYLE_URL = "https://demotiles.maplibre.org/style.json"

    fun currentStyle(): String {
        return ROAD_MAP_STYLE_URL
    }
}
