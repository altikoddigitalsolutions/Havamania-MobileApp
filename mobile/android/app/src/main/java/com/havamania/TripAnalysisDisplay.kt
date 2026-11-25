package com.havamania

import java.time.Instant

/** A retained snapshot is not evidence of a successful refresh. Keep its time visible. */
internal fun tripAnalysisDisplayLabel(plan: TravelPlan, now: Instant = Instant.now()): String {
    val retained = plan.lastForecastSnapshot != null || plan.analyses.isNotEmpty()
    val state = when {
        plan.isAnalyzing || plan.weatherAnalysisStatus == TravelWeatherAnalysisStatus.LOADING ->
            if (retained) "Hava analizi yenileniyor; son kayıtlı veriler gösteriliyor." else "Hava analizi hazırlanıyor."
        plan.weatherAnalysisStatus == TravelWeatherAnalysisStatus.WEATHER_PARTIAL_READY ->
            if (retained) "Hava verisi eksik veya yenilenemedi; son kayıtlı veriler gösteriliyor." else "Hava verisi eksik; analiz bekleniyor."
        plan.weatherAnalysisStatus == TravelWeatherAnalysisStatus.WEATHER_FAILED ->
            if (retained) "Hava analizi yenilenemedi; son kayıtlı veriler gösteriliyor." else "Hava analizi alınamadı. Yeniden deneyin."
        plan.weatherAnalysisStatus == TravelWeatherAnalysisStatus.WEATHER_READY_AI_FAILED ->
            "Hava verisi alındı; asistan analizi tamamlanamadı."
        plan.weatherAnalysisStatus == TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW ->
            "Seyahat hava analizi için tahmin aralığı bekleniyor."
        else -> ""
    }
    return listOf(state, tripForecastTimeLabel(plan.lastForecastSnapshot, now)).filter { it.isNotBlank() }.joinToString(" ")
}