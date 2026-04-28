package com.havamania

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.HavamaniaDesign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSuggestionCard(
    condition: String,
    windSpeed: Double = 0.0,
    humidity: Int = 0,
    pressure: Double = 1013.0,
    interests: List<String> = listOf("Balıkçılık", "Kamp", "Spor"),
    modifier: Modifier = Modifier,
    onAskAiClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val aiAccentColor = colorScheme.primary // Temaya göre dinamik vurgu rengi

    val suggestion = when {
        interests.contains("Spor") -> {
            when {
                condition.contains("Yağmurlu") || condition.contains("Sağanak") ->
                    "Bugün hava yağışlı. Dışarıda koşu yerine spor salonunda bir antrenman veya evde yoga harika bir fikir olabilir."
                windSpeed > 25.0 ->
                    "Rüzgar hızı ${windSpeed.toInt()} km/s. Dışarıda bisiklet sürmek veya tenis oynamak biraz zorlayıcı olabilir, dikkatli olmalısın."
                condition.contains("Güneşli") && humidity < 50 ->
                    "Harika bir spor havası! Açık havada yürüyüş veya hafif tempolu bir koşu için enerji topla."
                else -> "Genel hava $condition. Günlük egzersizini ihmal etme, vücuduna iyi bak!"
            }
        }

        interests.contains("Balıkçılık") -> {
            when {
                windSpeed > 22.0 -> "Dikkat! Sert rüzgar (${windSpeed.toInt()} km/s) denizi çalkalıyor. Balık için uygun değil, kıyıda kalmanı öneririm."
                pressure < 1005.0 -> "Alçak basınç var. Balıklar derine çekilmiş olabilir, bugün biraz sabırlı olman gerekebilir."
                pressure > 1020.0 -> "Yüksek basınç! Balıklar yüzeye yakın ve iştahlı olabilir. Tam zamanı!"
                condition.contains("Güneşli") && windSpeed < 12.0 -> "Hava süt liman, tam balık havası. Rastgele!"
                else -> "Genel hava durumu $condition. Balıkçılık ekipmanlarını hazırla ama rüzgarı takip et."
            }
        }

        else -> "Bugün hava $condition. İlgi alanlarına göre planını yapabilirsin!"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(HavamaniaDesign.CardCornerRadius))
            .clickable(onClick = onAskAiClick),
        shape = RoundedCornerShape(HavamaniaDesign.CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        border = BorderStroke(1.dp, aiAccentColor.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = aiAccentColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "HAVAMANİA ÖNERİSİ",
                            style = MaterialTheme.typography.labelLarge,
                            color = aiAccentColor
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = aiAccentColor.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 22.sp,
                        color = colorScheme.onSurface.copy(alpha = 0.95f)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    onClick = onAskAiClick,
                    color = aiAccentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, aiAccentColor.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.ChatBubbleOutline, null, tint = aiAccentColor, modifier = Modifier.size(14.dp))
                        Text(
                            text = "AI'ya detaylı soru sor",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = aiAccentColor
                            )
                        )
                    }
                }
            }
        }
    }
}
