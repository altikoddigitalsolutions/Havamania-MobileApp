package com.havamania

internal fun buildTravelNotificationData(plan: TravelPlan, daysLeft: Int, now: java.time.Instant = java.time.Instant.now()): TravelNotificationData {
    val snapshot = plan.lastForecastSnapshot?.takeIf { tripForecastMatchesPlan(plan, it, now) }?.validAdviceData()
    val prevSnapshot = plan.previousForecastSnapshot?.takeIf { snapshot != null && tripForecastMatchesPlan(plan, it, now) }?.validAdviceData()

    val comparisonText = if (snapshot != null && prevSnapshot != null) {
        TravelAiHelper.generateComparisonText(prevSnapshot, snapshot)
    } else null

    val recommendedItems = travelWeatherPreparation(snapshot).flatMap { reason -> when (reason) {
        WeatherPreparation.RAIN -> listOf("Yağmurluk", "Su geçirmeyen ayakkabı")
        WeatherPreparation.COLD -> listOf("Sıcak tutan katmanlar", "Kaymayan ayakkabı")
        WeatherPreparation.HEAT -> listOf("Hafif kıyafetler", "İçme suyu")
        WeatherPreparation.WIND -> listOf("Rüzgarlık")
        WeatherPreparation.UV -> listOf("Güneş kremi", "Şapka")
        WeatherPreparation.TEMPERATURE_SWING -> listOf("Hırka")
    } }.distinct()

    return TravelNotificationData(
        travelId = plan.id,
        destination = plan.city,
        travelStartDate = plan.startDate.toString(),
        travelEndDate = plan.endDate.toString(),
        daysLeft = daysLeft,
        weatherSummary = snapshot?.conditionSummary,
        rainProbability = snapshot?.precipitationProbability,
        minTemp = snapshot?.minTemp,
        maxTemp = snapshot?.maxTemp,
        windRisk = snapshot?.windSpeed?.let(WeatherUtils::formatWindWithLevel),
        previousAnalysisSummary = prevSnapshot?.conditionSummary,
        comparisonText = comparisonText,
        recommendedItems = recommendedItems
    )
}

