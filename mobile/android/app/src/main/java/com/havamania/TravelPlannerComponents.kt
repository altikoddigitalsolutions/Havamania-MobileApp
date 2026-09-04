package com.havamania

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun NextTripHero(
    plan: TravelPlan,
    today: LocalDate,
    onViewRoute: (String) -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    val themeStyles = HavamaniaTheme.styles
    val daysUntil = ChronoUnit.DAYS.between(today, plan.startDate)
    val isOngoing = TravelStatusResolver.getStatus(plan.startDate, plan.endDate, today) == TravelStatus.ONGOING
    val formatter = remember { DateTimeFormatter.ofPattern("d MMMM", Locale("tr")) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = themeStyles.pagePadding, vertical = 8.dp)
    ) {
        Text(
            text = if (isOngoing) "ŞU ANKİ SEYAHATİN" else "SIRADAKİ SEYAHATİN",
            style = HavamaniaTheme.typography.sectionTitle,
            color = if (isOngoing) themeColors.success else themeColors.accent
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.displayName,
                    style = HavamaniaTheme.typography.screenTitle.copy(fontWeight = FontWeight.Black),
                    color = themeColors.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CalendarToday, null, tint = themeColors.textMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (plan.startDate == plan.endDate) plan.startDate.format(formatter)
                               else "${plan.startDate.format(formatter)} - ${plan.endDate.format(formatter)}",
                        style = HavamaniaTheme.typography.bodyMedium,
                        color = themeColors.textSecondary
                    )
                }
            }

            Surface(
                color = if (daysUntil <= 0) themeColors.success else themeColors.accent,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (daysUntil <= 0) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White)
                            Text(
                                text = "ŞİMDİ",
                                style = HavamaniaTheme.typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color.White.copy(alpha = 0.8f)),
                            )
                        } else {
                            Text(
                                text = daysUntil.toString(),
                                style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Black, color = Color.White),
                                fontSize = 20.sp
                            )
                            Text(
                                text = "GÜN",
                                style = HavamaniaTheme.typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 7.sp, color = Color.White.copy(alpha = 0.8f)),
                            )
                        }
                    }
                }
            }
        }

        // City Personality for Hero
        val personality = remember(plan.city, plan.tripType) { CityPersonalityProvider.getPersonality(plan.city, plan.tripType) }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = if (isOngoing) themeColors.success else themeColors.accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = personality.slogan,
                style = HavamaniaTheme.typography.caption.copy(fontWeight = FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                color = if (isOngoing) themeColors.success else themeColors.accent
            )
        }

        Spacer(Modifier.height(12.dp))

        val analysis = plan.analyses.lastOrNull()
        if (analysis != null) {
            Text(
                text = RecommendationEngine.getShortWeatherSummaryFromAnalysis(analysis, isOngoing),
                style = HavamaniaTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = themeColors.textPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        HavamaniaPrimaryButton(
            text = "ROTAYI İNCELE",
            onClick = { onViewRoute(plan.id) },
            height = 40.dp,
            icon = Icons.Rounded.Route,
            fillMaxWidth = true
        )
    }
}

@Composable
fun TravelPlanCard(
    plan: TravelPlan,
    today: LocalDate,
    isFocused: Boolean = false,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onShowDetail: () -> Unit,
    onReanalyze: () -> Unit,
    onViewRoute: (String) -> Unit = {},
    isOnline: Boolean = true,
    isAnalyzing: Boolean = false,
    onToggleGuide: ((TravelPlan) -> Unit)? = null
) {
    val themeColors = HavamaniaTheme.colors
    val tripStatus = TravelStatusResolver.getStatus(plan.startDate, plan.endDate, today)
    val isPast = tripStatus == TravelStatus.PAST
    val isOngoing = tripStatus == TravelStatus.ONGOING

    val daysUntil = ChronoUnit.DAYS.between(today, plan.startDate)
    val isLocked = !isPast && !isOngoing && daysUntil > TRIP_ANALYSIS_WINDOW_DAYS

    val isArchived = plan.isArchived
    var showMenu by remember { mutableStateOf(false) }

    val formatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr")) }
    val latestAnalysis = plan.analyses.lastOrNull()

    HavamaniaCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isPast && !isArchived) 0.8f else 1f)
            .then(
                if (isFocused) Modifier.border(2.dp, themeColors.accent, RoundedCornerShape(HavamaniaTheme.styles.cardCornerRadius))
                else Modifier
            ),
        onClick = {
            if (isPast || isArchived) onShowDetail()
            else onViewRoute(plan.id)
        },
        padding = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TripStatusBadge(tripStatus, isArchived, today == plan.startDate)

                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.MoreVert, null, tint = themeColors.textMuted)
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = themeColors.surface,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (!isPast && !isArchived) {
                        DropdownMenuItem(
                            text = { Text("Düzenle") },
                            onClick = { onEdit(); showMenu = false },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                    if (isArchived) {
                        DropdownMenuItem(
                            text = { Text("Aktifleştir") },
                            onClick = { onUnarchive(); showMenu = false },
                            leadingIcon = { Icon(Icons.Rounded.Unarchive, null, modifier = Modifier.size(18.dp)) }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Arşivle") },
                            onClick = { onArchive(); showMenu = false },
                            leadingIcon = { Icon(Icons.Rounded.Archive, null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Sil", color = themeColors.error) },
                        onClick = { onDelete(); showMenu = false },
                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = themeColors.error, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                plan.originCity?.takeIf { it.isNotBlank() }?.let { origin ->
                    Text(
                        text = origin,
                        style = HavamaniaTheme.typography.cardTitle,
                        color = themeColors.textSecondary
                    )
                    Icon(
                        imageVector = Icons.Rounded.East,
                        contentDescription = null,
                        tint = themeColors.accent.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 8.dp).size(20.dp)
                    )
                }
                Text(
                    text = plan.city,
                    style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Black),
                    color = themeColors.textPrimary
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Event, null, tint = themeColors.textMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (plan.startDate == plan.endDate) plan.startDate.format(formatter)
                           else "${plan.startDate.format(formatter)} - ${plan.endDate.format(formatter)}",
                    style = HavamaniaTheme.typography.bodySmall,
                    color = themeColors.textSecondary
                )
                plan.departureTime?.let { depTime ->
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Rounded.Schedule, null, tint = themeColors.textMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = depTime,
                        style = HavamaniaTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = themeColors.textPrimary
                    )
                }
            }

            if (!isArchived && !isPast) {
                Spacer(Modifier.height(16.dp))

                if (isLocked) {
                    HavamaniaInfoBanner(
                        text = "Güzergâh hava tahmini seyahatine 2 gün kala hazırlanacak.",
                        backgroundColor = themeColors.surface.copy(alpha = 0.3f),
                        contentColor = themeColors.textMuted
                    )
                } else if (latestAnalysis != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WeatherScoreIndicator(latestAnalysis.travelScore)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = RecommendationEngine.getShortWeatherSummaryFromAnalysis(latestAnalysis),
                                style = HavamaniaTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = themeColors.textPrimary
                            )
                            Text(
                                text = "Yolculuk için beklenen hava kalitesi.",
                                style = HavamaniaTheme.typography.caption,
                                color = themeColors.textMuted
                            )
                        }
                    }
                } else if (isAnalyzing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = themeColors.accent,
                        trackColor = themeColors.accent.copy(alpha = 0.1f)
                    )
                }

                Spacer(Modifier.height(16.dp))
                HavamaniaSecondaryButton(
                    text = "ROTAYI İNCELE",
                    onClick = { onViewRoute(plan.id) },
                    height = 44.dp,
                    icon = Icons.Rounded.Route,
                    fillMaxWidth = true
                )
            } else if (isPast) {
                 Spacer(Modifier.height(16.dp))
                 HavamaniaSecondaryButton(
                    text = "ÖZETİ GÖR",
                    onClick = onShowDetail,
                    height = 44.dp,
                    icon = Icons.Rounded.History,
                    fillMaxWidth = true
                )
            }

            // City Personality Section (Trip-Type Aware & Clean Surface)
            var showPersonality by remember { mutableStateOf(false) }
            val isTablet = LocalWindowSize.current.isTablet || LocalWindowSize.current.isLargeTablet
            val personality = remember(plan.city, plan.tripType) { CityPersonalityProvider.getPersonality(plan.city, plan.tripType) }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = themeColors.border.copy(alpha = 0.1f))
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isTablet && onToggleGuide != null) {
                            onToggleGuide(plan)
                        } else {
                            showPersonality = !showPersonality
                        }
                    }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = themeColors.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isTablet || showPersonality) "Rehberi Kapat / Aç" else personality.slogan,
                        style = HavamaniaTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                        color = themeColors.accent,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (showPersonality) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        null,
                        tint = themeColors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (!isTablet) {
                    AnimatedVisibility(visible = showPersonality) {
                        RichTravelGuideView(plan = plan, today = today)
                    }
                }
            }
        }
    }
}

@Composable
fun RichTravelGuideView(
    plan: TravelPlan,
    today: LocalDate
) {
    val themeColors = HavamaniaTheme.colors
    val personality = remember(plan.city, plan.tripType) { CityPersonalityProvider.getPersonality(plan.city, plan.tripType) }
    val analysis = plan.analyses.lastOrNull()

    val avgTemp = analysis?.averageTemperature?.toInt()
    val rainRisk = analysis?.rainRiskPercent
    val score = analysis?.travelScore

    val avgTempText = avgTemp?.let { "$it°C" } ?: "Veri bekleniyor"
    val rainRiskText = rainRisk?.let { "%$it" } ?: "Veri bekleniyor"
    val scoreText = score?.let { "$it/100" } ?: "Analiz Bekleniyor"

    LaunchedEffect(plan) {
        if (BuildConfig.DEBUG) {
            val hasAnalysis = analysis != null
            android.util.Log.d(
                "HAVAMANIA_TRAVEL_SCORE_DEBUG",
                "TRIP_ID=${plan.id} | TRIP_TYPE=${plan.tripType} | " +
                "TEMP_METRIC_NAME=averageTemperature | TEMP_INPUT_EXACT=${analysis?.averageTemperature ?: "N/A"} | " +
                "PRECIP_METRIC_NAME=rainRiskPercent | PRECIP_INPUT_EXACT=${analysis?.rainRiskPercent ?: "N/A"} | " +
                "WIND_METRIC_NAME=N/A | WIND_INPUT_EXACT=N/A | " +
                "INPUTS_AVAILABLE=$hasAnalysis | PERSIST_SCORE=${analysis?.travelScore ?: -1} | UI_SCORE=${score ?: -1}"
            )
        }
        android.util.Log.d("HAVAMANIA_TRAVEL_GUIDE_DEBUG", "TRIP_ID=${plan.id} | DESTINATION=${plan.city} | TRIP_TYPE=${plan.tripType} | HAS_ANALYSIS=${analysis != null} | SCORE=$score")
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(
            text = personality.description,
            style = HavamaniaTheme.typography.bodySmall,
            color = themeColors.textPrimary
        )

        Spacer(Modifier.height(16.dp))

        // HAVA ÖZETİ Section
        SectionHeader("HAVA ÖZETİ", Icons.Rounded.WbSunny)
        Spacer(Modifier.height(6.dp))
        val weatherSummaryText = if (analysis != null && avgTemp != null && rainRisk != null) {
            when {
                rainRisk > 40 -> "Seyahat gününde hava yağışlı ve bulutlu geçebilir. Ortalama sıcaklık ${avgTemp}°C civarında bekleniyor."
                avgTemp > 28 -> "Hava sıcak ve güneşli olacak. Sıcaklık ${avgTemp}°C seviyelerinde seyredecek."
                avgTemp < 10 -> "Hava serin ve soğuk geçecek. Ortalama sıcaklık ${avgTemp}°C."
                else -> "Hava genel olarak ılıman ve seyahat için elverişli. Ortalama sıcaklık ${avgTemp}°C civarında."
            }
        } else {
            "Bu seyahat için henüz detaylı hava analizi yapılmadı. Tahminler hazır olduğunda burada görünecek."
        }
        Text(weatherSummaryText, style = HavamaniaTheme.typography.bodySmall, color = themeColors.textPrimary)

        Spacer(Modifier.height(16.dp))

        // Metrics Grid (Seyahat Skoru, Ort. Sıcaklık, Yağış Riski)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricTile("SEYAHAT SKORU", scoreText, Icons.Rounded.Star, themeColors.warning, Modifier.weight(1f))
            MetricTile("ORT. SICAKLIK", avgTempText, Icons.Rounded.Thermostat, themeColors.accent, Modifier.weight(1f))
            MetricTile("YAĞIŞ RİSKİ", rainRiskText, Icons.Rounded.WaterDrop, if ((rainRisk ?: 0) > 50) themeColors.error else themeColors.success, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        // Valiz / Hazırlık Tavsiyesi
        SectionHeader("VALİZ / HAZIRLIK TAVSİYESİ", Icons.Rounded.Backpack)
        Spacer(Modifier.height(6.dp))
        val packingTip = if (avgTemp != null && rainRisk != null) {
            val weatherPart = when {
                avgTemp < 12 -> "Hava serin/soğuk; kalın mont ve sıcak tutan katmanlar al."
                rainRisk > 40 -> "Yağış ihtimali yüksek; yağmurluk veya şemsiye ekle."
                avgTemp > 28 -> "Hava sıcak; hafif kıyafetler ve güneş koruması al."
                else -> "Mevsim koşullarına uygun giysiler tercih et."
            }
            val tripTypePart = when (plan.tripType) {
                TripType.GASTRONOMY -> "Şehir içindeki lezzet durakları arasında rahat hareket etmek için konforlu günlük ayakkabılar ve pratik çanta seç."
                TripType.NATURE, TripType.CAMPING, TripType.ADVENTURE -> "Doğa yürüyüşlerine uygun sağlam tabanlı ayakkabı ve esnek outdoor kıyafetler tercih et."
                TripType.CULTURE -> "Uzun müze ve sokak gezileri için rahat yürüyüş ayakkabıları idealdir."
                TripType.BEACH -> "Plaj giysileri, havlu ve güneş gözlüğü unutma."
                else -> "Seyahat konforunu artıracak rahat günlük eşyalar al."
            }
            "$weatherPart $tripTypePart"
        } else {
            when (plan.tripType) {
                TripType.GASTRONOMY -> "Şehir içi keşifler ve lezzet durakları için konforlu günlük ayakkabılar tercih et."
                TripType.NATURE, TripType.CAMPING, TripType.ADVENTURE -> "Doğa yürüyüşlerine uygun sağlam tabanlı ayakkabılar ve rahat outdoor kıyafetler seç."
                else -> "Mevsim şartlarına uygun temel seyahat hazırlıkları yapılabilir."
            }
        }
        Text(packingTip, style = HavamaniaTheme.typography.bodySmall, color = themeColors.textPrimary)

        Spacer(Modifier.height(16.dp))

        // Mutlaka Gör
        SectionHeader("MUTLAKA GÖR", Icons.Rounded.LocationOn)
        Spacer(Modifier.height(6.dp))
        personality.mustSee.forEach { item ->
            Text("• $item", style = HavamaniaTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = themeColors.textPrimary, modifier = Modifier.padding(vertical = 2.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Denemeden Dönme
        SectionHeader("DENEMEDEN DÖNME", Icons.Rounded.Restaurant)
        Spacer(Modifier.height(6.dp))
        personality.food.forEach { item ->
            Text("• $item", style = HavamaniaTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = themeColors.textPrimary, modifier = Modifier.padding(vertical = 2.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Mini Tavsiyeler
        SectionHeader("MİNİ TAVSİYELER", Icons.Rounded.Lightbulb)
        Spacer(Modifier.height(6.dp))
        Text("• ${personality.tip}", style = HavamaniaTheme.typography.bodySmall, color = themeColors.textSecondary)
        Text("• Yerel halkla sohbet ederek bölgenin gizli güzelliklerini öğrenebilirsin.", style = HavamaniaTheme.typography.bodySmall, color = themeColors.textSecondary, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun MetricTile(title: String, value: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    val themeColors = HavamaniaTheme.colors
    Surface(
        modifier = modifier,
        color = themeColors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, themeColors.border.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Black, fontSize = 16.sp), color = themeColors.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(title, style = HavamaniaTheme.typography.caption.copy(fontSize = 9.sp), color = themeColors.textMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    val themeColors = HavamaniaTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(title, style = HavamaniaTheme.typography.caption.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp), color = themeColors.accent)
    }
}

@Composable
private fun PersonalityInfoBlock(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val themeColors = HavamaniaTheme.colors
    Column(modifier = modifier) {
        Text(label, style = HavamaniaTheme.typography.caption.copy(fontSize = 8.sp, fontWeight = FontWeight.Black), color = themeColors.textMuted)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
            Icon(icon, null, tint = themeColors.accent.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(value, style = HavamaniaTheme.typography.caption.copy(fontWeight = FontWeight.Bold), color = themeColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TripStatusBadge(status: TravelStatus, isArchived: Boolean, isToday: Boolean) {
    val colors = HavamaniaTheme.colors
    val (label, color) = when {
        isArchived -> "ARŞİVLENDİ" to colors.textMuted
        status == TravelStatus.ONGOING -> {
            if (isToday) "BUGÜN" to colors.accent
            else "ŞU ANDA" to colors.success
        }
        status == TravelStatus.PAST -> "TAMAMLANDI" to colors.textSecondary
        else -> "PLANLANDI" to colors.accent.copy(alpha = 0.7f)
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = HavamaniaTheme.typography.caption.copy(fontWeight = FontWeight.Black),
            color = color
        )
    }
}

@Composable
private fun WeatherScoreIndicator(score: Int) {
    val color = when {
        score >= 80 -> Color(0xFF10B981)
        score >= 60 -> Color(0xFFFBBF24)
        else -> Color(0xFFEF4444)
    }

    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier.size(40.dp),
            color = color,
            strokeWidth = 4.dp,
            trackColor = color.copy(alpha = 0.1f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Text(
            text = "%$score",
            style = HavamaniaTheme.typography.caption.copy(fontWeight = FontWeight.Black, fontSize = 10.sp),
            color = color
        )
    }
}

@Composable
fun TripListSection(
    filter: TravelFilter,
    plans: List<TravelPlan>,
    isLoading: Boolean,
    onFilterChange: (TravelFilter) -> Unit,
    selectedDate: LocalDate,
    onAddClick: () -> Unit,
    onDelete: (TravelPlan) -> Unit,
    onEdit: (TravelPlan) -> Unit,
    onArchive: (String) -> Unit,
    onUnarchive: (String) -> Unit,
    onShowDetail: (TravelPlan) -> Unit,
    onReanalyze: (TravelPlan) -> Unit,
    onViewRoute: (String) -> Unit,
    isOnline: Boolean,
    focusId: String?,
    highlight: String?,
    today: LocalDate,
    modifier: Modifier = Modifier,
    onToggleGuide: ((TravelPlan) -> Unit)? = null
) {
    val responsive = LocalResponsiveValues.current

    Box(modifier = modifier.fillMaxWidth()) {
        if (isLoading) {
            TravelListSkeleton()
        } else if (plans.isEmpty()) {
            if (filter == TravelFilter.UPCOMING) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.EventBusy, null, tint = HavamaniaTheme.colors.textMuted.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Bu tarih için planlanmış seyahat yok.",
                        style = HavamaniaTheme.typography.bodyMedium,
                        color = HavamaniaTheme.colors.textMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    HavamaniaSecondaryButton(
                        text = "SEYAHAT EKLE",
                        onClick = onAddClick,
                        icon = Icons.Rounded.Add,
                        fillMaxWidth = false
                    )
                }
            } else {
                TravelEmptyState(filter, onAddClick)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                plans.forEach { plan ->
                    TravelPlanCard(
                        plan = plan,
                        today = today,
                        isFocused = plan.id == focusId || plan.city.contains(highlight ?: "", ignoreCase = true),
                        onDelete = { onDelete(plan) },
                        onEdit = { onEdit(plan) },
                        onArchive = { onArchive(plan.id) },
                        onUnarchive = { onUnarchive(plan.id) },
                        onShowDetail = { onShowDetail(plan) },
                        onReanalyze = { onReanalyze(plan) },
                        onViewRoute = onViewRoute,
                        isOnline = isOnline,
                        isAnalyzing = plan.isAnalyzing,
                        onToggleGuide = onToggleGuide
                    )
                }
            }
        }
    }
}

@Composable
fun TravelEmptyState(filter: TravelFilter, onAdd: () -> Unit) {
    HavamaniaEmptyState(
        icon = when(filter) {
            TravelFilter.UPCOMING -> Icons.Rounded.Explore
            TravelFilter.PAST -> Icons.Rounded.History
            TravelFilter.ARCHIVED -> Icons.Rounded.Archive
        },
        title = when(filter) {
            TravelFilter.UPCOMING -> "Yeni bir rota seni bekliyor"
            TravelFilter.PAST -> "Geçmiş seyahat bulunmuyor"
            TravelFilter.ARCHIVED -> "Arşivlenmiş seyahat bulunmuyor"
        },
        description = when(filter) {
            TravelFilter.UPCOMING -> "Seyahatini ekle, yaklaşan yolculuğun için hava koşullarını Havamania takip etsin."
            TravelFilter.PAST -> "Tamamlanan seyahatlerin analizleri burada birikir."
            TravelFilter.ARCHIVED -> "Arşivlediğin planlara buradan ulaşabilirsin."
        },
        action = if (filter == TravelFilter.UPCOMING) {
            {
                HavamaniaPrimaryButton(
                    text = "İlk Seyahatimi Oluştur",
                    onClick = onAdd,
                    modifier = Modifier.width(240.dp)
                )
            }
        } else null
    )
}
