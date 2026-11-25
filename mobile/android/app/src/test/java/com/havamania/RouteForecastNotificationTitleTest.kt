package com.havamania

import org.junit.Assert.*
import org.junit.Test

class RouteForecastNotificationTitleTest {
    @Test fun `changed wording is an update rather than proof of weather change`() {
        val title = routeForecastNotificationTitle("Eksik veri", "Bazı noktalarda veri eksik")
        assertEquals("Güzergâh hava tahminin güncellendi", title)
    }
    @Test fun `blank previous summary is first availability`() {
        assertEquals("Güzergâh hava tahminin hazır", routeForecastNotificationTitle("  ", "Yeni analiz"))
    }
    @Test fun `missing new summary never claims readiness`() {
        assertEquals("Güzergâh hava verisi bekleniyor", routeForecastNotificationTitle(null, ""))
    }
}