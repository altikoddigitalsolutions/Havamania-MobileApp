package com.havamania

import java.util.UUID

object RecommendationEngine {
    fun generateRecommendation(
        weather: WeatherData,
        timeOfDay: TimeOfDay,
        userInterests: Set<String>,
        travelPlans: List<TravelPlan>
    ): HavamaniaRecommendation {
        val tempValue = weather.temperature.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 20
        val condition = weather.condition.lowercase()
        val uvIndex = weather.details.find { it.title.contains("UV") }?.value?.toIntOrNull() ?: 0
        val windSpeed = weather.details.find { it.title.contains("Rüzgar") }?.value?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        val humidity = weather.details.find { it.title.contains("Nem") }?.value?.filter { it.isDigit() }?.toIntOrNull() ?: 50

        // Priority 1: Critical Warnings
        if (condition.contains("fırtına") || windSpeed > 40) {
            return HavamaniaRecommendation(
                message = "Şiddetli rüzgar ve fırtına uyarısı! Dışarı çıkarken çok dikkatli olmalı, mümkünse korunaklı yerlerde kalmalısın.",
                type = RecommendationType.WARNING,
                highlightedWords = listOf("şiddetli", "fırtına", "korunaklı"),
                priority = RecommendationPriority.CRITICAL
            )
        }

        // Priority 2: Rain
        if (condition.contains("yağmur") || condition.contains("sağanak")) {
             return HavamaniaRecommendation(
                message = "Yağmur ihtimali yüksek. Dışarı çıkacaksan şemsiyeni yanınıza almayı unutma.",
                type = RecommendationType.WARNING,
                highlightedWords = listOf("Yağmur", "yüksek", "şemsiyeni"),
                priority = RecommendationPriority.HIGH
            )
        }

        // Priority 3: UV Index
        if (uvIndex > 6 && timeOfDay == TimeOfDay.DAY) {
            return HavamaniaRecommendation(
                message = "Güneş keyifli ama UV seviyesi yüksek. Cildini korumak için güneş kremi kullanmayı unutma.",
                type = RecommendationType.HEALTH,
                highlightedWords = listOf("UV seviyesi", "yüksek", "güneş kremi"),
                priority = RecommendationPriority.HIGH
            )
        }

        // Priority 4: Travel Plans
        if (travelPlans.isNotEmpty()) {
            val nearestPlan = travelPlans.first()
            return HavamaniaRecommendation(
                message = "${nearestPlan.city} seyahati yaklaşıyor. Seyahat gününde hava değişken görünüyor. Valizine hafif bir yağmurluk ekleyebilirsin.",
                type = RecommendationType.TRAVEL,
                highlightedWords = listOf(nearestPlan.city, "değişken", "yağmurluk"),
                priority = RecommendationPriority.HIGH
            )
        }

        // Priority 5: Sports & Interests
        if (userInterests.contains("acik_hava") || userInterests.contains("trekking")) {
            if (tempValue in 15..25 && !condition.contains("yağmur")) {
                return HavamaniaRecommendation(
                    message = "Harika bir spor havası. Açık havada yürüyüş veya hafif koşu için iyi bir zaman.",
                    type = RecommendationType.SPORT,
                    highlightedWords = listOf("spor havası", "yürüyüş", "koşu"),
                    priority = RecommendationPriority.MEDIUM
                )
            }
        }

        // Priority 6: Wind & Outdoor
        if (windSpeed > 20) {
            return HavamaniaRecommendation(
                message = "Rüzgar bugün güçlü. Sahil yürüyüşü yerine korunaklı bir rota daha iyi olabilir.",
                type = RecommendationType.OUTDOOR,
                highlightedWords = listOf("Rüzgar", "güçlü", "korunaklı"),
                priority = RecommendationPriority.MEDIUM
            )
        }

        // Priority 7: Comfort
        if (tempValue < 10) {
            return HavamaniaRecommendation(
                message = "Hava biraz serin. Kat kat giyinerek hem şıklığını koruyabilir hem de soğuktan etkilenmezsin.",
                type = RecommendationType.COMFORT,
                highlightedWords = listOf("serin", "kat kat", "soğuktan"),
                priority = RecommendationPriority.LOW
            )
        }

        // Default: General
        return HavamaniaRecommendation(
            message = "Bugün hava $condition ve sıcaklık $tempValue derece. Planlarını yaparken bu güzel havanın keyfini çıkarmayı unutma.",
            type = RecommendationType.GENERAL,
            highlightedWords = listOf(condition, "$tempValue derece", "keyfini"),
            priority = RecommendationPriority.LOW
        )
    }
}
