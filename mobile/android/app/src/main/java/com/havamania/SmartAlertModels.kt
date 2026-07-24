package com.havamania

import androidx.compose.ui.graphics.vector.ImageVector

data class SmartAlertConfig(
    val rainEnabled: Boolean = true,
    val windEnabled: Boolean = true,
    val heatEnabled: Boolean = true,
    val frostEnabled: Boolean = true,
    val fogEnabled: Boolean = true,
    val stormEnabled: Boolean = true,
    val uvEnabled: Boolean = true,
    val pollenEnabled: Boolean = false,
    val airQualityEnabled: Boolean = false
)

data class SmartAlert(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val severity: AlertSeverity = AlertSeverity.INFO,
    val deduplicationKey: String = "" // userId + city + alertType + eventDate
)

enum class AlertSeverity {
    INFO, WARNING, CRITICAL
}
