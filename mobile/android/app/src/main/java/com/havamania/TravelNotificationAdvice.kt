package com.havamania

internal fun buildTravelNotificationData(plan: TravelPlan, daysLeft: Int): TravelNotificationData {
    val snapshot = plan.lastForecastSnapshot?.validAdviceData()
    val prevSnapshot = plan.previousForecastSnapshot

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
        previousAnalysisSummary = plan.weatherSummary,
        comparisonText = comparisonText,
        recommendedItems = recommendedItems
    )
}

