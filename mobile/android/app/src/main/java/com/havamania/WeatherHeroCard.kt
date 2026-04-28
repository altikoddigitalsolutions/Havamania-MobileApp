package com.havamania

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hava durumuna göre premium, soft atmosferik katmanlar oluşturur.
 */
@Composable
fun RenderWeatherAtmosphere(condition: String, isNight: Boolean = false) {
    val lower = condition.lowercase()
    val glowColor = when {
        isNight -> Color(0xFF6366F1)
        lower.contains("güneş") || lower.contains("açık") -> Color(0xFFFDE047)
        lower.contains("bulut") -> Color(0xFFF8FAFC)
        lower.contains("yağmur") -> Color(0xFF38BDF8)
        lower.contains("kar") -> Color(0xFFE0F2FE)
        else -> Color(0xFFF8FAFC)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isNight && (lower.contains("güneş") || lower.contains("açık"))) {
            // Sun flare effect
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 50.dp, y = (-50).dp)
                    .size(200.dp)
                    .background(glowColor.copy(alpha = 0.3f), CircleShape)
            )
        } else if (isNight) {
            // Moon glow
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-40).dp, y = 40.dp)
                    .size(60.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .shadow(elevation = 40.dp, shape = CircleShape, spotColor = Color.White)
            )
        }

        if (lower.contains("yağmur")) {
            // Simulated rain lines
            repeat(10) { i ->
                Box(
                    modifier = Modifier
                        .offset(x = (20 * i).dp, y = (15 * i).dp)
                        .size(width = 1.dp, height = 20.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }
        }

        if (lower.contains("bulut")) {
            // Cloud silhouettes
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = 20.dp)
                    .size(width = 150.dp, height = 80.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(50.dp))
            )
        }
    }
}

@Composable
fun WeatherHeroCard(
    cityName: String,
    temperature: String,
    condition: String, // Bu her zaman anlık (current) durum olmalı
    high: String,
    low: String,
    feelsLike: String,
    onCityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNight = false // ViewModel'dan gelen is_day bilgisine göre setlenebilir
    val theme = getWeatherCardTheme(condition, isNight)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(theme.gradient))
        ) {
            // 1. DİNAMİK ATMOSFER (currentWeather'a bağlı)
            RenderWeatherAtmosphere(condition = condition, isNight = isNight)

            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ŞEHİR ETİKETİ
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onCityClick() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.LocationOn, null, tint = theme.accent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = cityName.uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontSize = 12.sp
                        ),
                        color = Color.White
                    )
                }

                // SICAKLIK VE DURUM
                Column {
                    Text(
                        text = temperature,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 88.sp,
                            fontWeight = FontWeight.ExtraLight,
                            letterSpacing = (-4).sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = condition,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium, fontSize = 20.sp),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // ESKİ SADE MAX/MIN SATIRI VE HİSSEDİLEN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("↑", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(text = high, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                        Spacer(Modifier.width(16.dp))

                        Text("↓", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(text = low, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "Hissedilen $feelsLike",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Hava durumuna göre premium atmosfer renklerini belirler.
 */
data class CardTheme(val gradient: List<Color>, val accent: Color)

@Composable
fun getWeatherCardTheme(condition: String, isNight: Boolean): CardTheme {
    val lower = condition.lowercase()
    return when {
        isNight -> CardTheme(listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)), Color(0xFFE94560))
        lower.contains("güneş") || lower.contains("açık") -> CardTheme(listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)), Color(0xFFF9D423))
        lower.contains("bulut") -> CardTheme(listOf(Color(0xFF757F9A), Color(0xFFD7DDE8)), Color(0xFFFFFFFF))
        lower.contains("yağmur") -> CardTheme(listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF)), Color(0xFF3498DB))
        lower.contains("kar") -> CardTheme(listOf(Color(0xFF83A4D4), Color(0xFFB6FBFF)), Color(0xFFFFFFFF))
        lower.contains("sis") -> CardTheme(listOf(Color(0xFFBDC3C7), Color(0xFF2C3E50)), Color(0xFFECF0F1))
        else -> CardTheme(listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E)), Color(0xFFF1C40F))
    }
}
