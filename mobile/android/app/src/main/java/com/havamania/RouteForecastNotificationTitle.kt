package com.havamania

/** Wording or completeness changes do not prove meteorological change. */
internal fun routeForecastNotificationTitle(previous: String?, current: String): String = when {
    current.isBlank() -> "Güzergâh hava verisi bekleniyor"
    previous.isNullOrBlank() -> "Güzergâh hava tahminin hazır"
    else -> "Güzergâh hava tahminin güncellendi"
}
