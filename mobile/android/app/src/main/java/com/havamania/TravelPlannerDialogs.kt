package com.havamania

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.havamania.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTravelPlanDialog(
    viewModel: TravelViewModel,
    editPlan: TravelPlan? = null,
    onDismiss: () -> Unit,
    onSave: (TravelPlan) -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    val themeStyles = HavamaniaTheme.styles

    var cityName by remember { mutableStateOf(editPlan?.city ?: "") }
    var districtName by remember { mutableStateOf(editPlan?.district ?: "") }
    var destinationPick by remember { mutableStateOf<GeocodingResultDto?>(null) }
    var originPick by remember { mutableStateOf<GeocodingResultDto?>(null) }

    var destinationQuery by remember { mutableStateOf(editPlan?.city ?: "") }
    var originQuery by remember { mutableStateOf(editPlan?.originCity ?: "") }

    var destinationLocked by remember { mutableStateOf(editPlan != null) }
    var originLocked by remember { mutableStateOf(editPlan?.originCity != null) }

    var tripType by remember { mutableStateOf(editPlan?.tripType ?: TripType.VACATION) }
    var startDate by remember { mutableStateOf(editPlan?.startDate ?: LocalDate.now()) }
    var endDate by remember { mutableStateOf(editPlan?.endDate ?: startDate) }
    var departureTime by remember { mutableStateOf(editPlan?.departureTime) }

    val citySuggestions by viewModel.citySuggestions.collectAsState()
    val originSuggestions by viewModel.originSuggestions.collectAsState()

    val displayFormatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr")) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(themeColors.surface),
            color = themeColors.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                HavamaniaTopBar(
                    title = if (editPlan != null) "Seyahati Düzenle" else "Yeni Seyahat",
                    onBack = onDismiss
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    // 1. Destination
                    SectionLabel("VARALACAK ŞEHİR")
                    if (destinationLocked) {
                        LockedLocationCard(
                            title = destinationQuery,
                            subtitle = if (editPlan?.district != null) "${editPlan.district}, ${editPlan.city}" else "Hedef Belirlendi",
                            onUnlock = { destinationLocked = false }
                        )
                    } else {
                        LocationSearchField(
                            query = destinationQuery,
                            onQueryChange = {
                                destinationQuery = it
                                viewModel.searchCity(it)
                            },
                            suggestions = citySuggestions,
                            onSelect = {
                                destinationPick = it
                                destinationQuery = it.name
                                destinationLocked = true
                            },
                            placeholder = "Şehir veya ilçe ara..."
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // 2. Origin (Optional)
                    SectionLabel("KALKIŞ NOKTASI (OPSİYONEL)")
                    if (originLocked) {
                        LockedLocationCard(
                            title = originQuery,
                            subtitle = "Kalkış Belirlendi",
                            onUnlock = { originLocked = false }
                        )
                    } else {
                        LocationSearchField(
                            query = originQuery,
                            onQueryChange = {
                                originQuery = it
                                viewModel.searchOrigin(it)
                            },
                            suggestions = originSuggestions,
                            onSelect = {
                                originPick = it
                                originQuery = it.name
                                originLocked = true
                            },
                            placeholder = "Nereden yola çıkacaksın?"
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // 3. Trip Type
                    SectionLabel("SEYAHAT TİPİ")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TripType.entries.forEach { type ->
                            HavamaniaChip(
                                selected = tripType == type,
                                onClick = { tripType = type },
                                label = type.label
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // 4. Dates
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionLabel("BAŞLANGIÇ")
                            DateSelectionField(
                                date = startDate,
                                onDateChange = {
                                    val diff = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate)
                                    startDate = it
                                    endDate = it.plusDays(diff.coerceAtLeast(0L))
                                },
                                formatter = displayFormatter
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            SectionLabel("BİTİŞ")
                            DateSelectionField(
                                date = endDate,
                                onDateChange = { endDate = it },
                                formatter = displayFormatter
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // 5. Time
                    SectionLabel("YOLA ÇIKIŞ SAATİ")
                    TimeSelectionField(
                        time = departureTime,
                        onTimeChange = { departureTime = it },
                        onClear = { departureTime = null }
                    )

                    Spacer(Modifier.height(32.dp))

                    val hasDestination = destinationPick != null || (editPlan != null && destinationLocked && destinationQuery.isNotBlank())
                    val isFormValid = hasDestination && !startDate.isAfter(endDate)
                    val isProcessingState by viewModel.isLoading.collectAsState()

                    HavamaniaPrimaryButton(
                        text = if (editPlan != null) "DEĞİŞİKLİKLERİ KAYDET" else "SEYAHATİ OLUŞTUR",
                        enabled = isFormValid && !isProcessingState,
                        isLoading = isProcessingState,
                        onClick = {
                            val finalPlan = if (editPlan != null) {
                                editPlan.copy(
                                    city = if (destinationPick != null) destinationPick!!.name else destinationQuery,
                                    district = if (destinationPick != null) destinationPick!!.district else editPlan.district,
                                    originCity = if (originQuery.isBlank()) null else (originPick?.name ?: originQuery),
                                    tripType = tripType,
                                    startDate = startDate,
                                    endDate = endDate,
                                    departureTime = departureTime,
                                    latitude = destinationPick?.latitude ?: editPlan.latitude,
                                    longitude = destinationPick?.longitude ?: editPlan.longitude,
                                    originLatitude = originPick?.latitude ?: editPlan.originLatitude,
                                    originLongitude = originPick?.longitude ?: editPlan.originLongitude
                                )
                            } else {
                                TravelPlan(
                                    city = destinationPick!!.name,
                                    district = destinationPick!!.district,
                                    originCity = if (originQuery.isBlank()) null else originPick?.name,
                                    tripType = tripType,
                                    startDate = startDate,
                                    endDate = endDate,
                                    departureTime = departureTime,
                                    latitude = destinationPick!!.latitude,
                                    longitude = destinationPick!!.longitude,
                                    originLatitude = originPick?.latitude,
                                    originLongitude = originPick?.longitude
                                )
                            }
                            onSave(finalPlan)
                        }
                    )

                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun LockedLocationCard(title: String, subtitle: String, onUnlock: () -> Unit) {
    val colors = HavamaniaTheme.colors
    Surface(
        color = colors.accent.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocationOn, null, tint = colors.accent)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = HavamaniaTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                Text(subtitle, style = HavamaniaTheme.typography.bodySmall, color = colors.textMuted)
            }
            IconButton(onClick = onUnlock) {
                Icon(Icons.Rounded.Edit, null, tint = colors.accent, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun LocationSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<GeocodingResultDto>,
    onSelect: (GeocodingResultDto) -> Unit,
    placeholder: String
) {
    Column {
        HavamaniaTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = placeholder,
            leadingIcon = Icons.Rounded.Search
        )

        if (suggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .heightIn(max = 200.dp),
                color = HavamaniaTheme.colors.surface,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, HavamaniaTheme.colors.border.copy(alpha = 0.1f))
            ) {
                LazyColumn {
                    items(suggestions) { item ->
                        ListItem(
                            headlineContent = { Text(item.name, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("${item.admin1 ?: ""}, ${item.country}") },
                            modifier = Modifier.clickable { onSelect(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateSelectionField(date: LocalDate, onDateChange: (LocalDate) -> Unit, formatter: DateTimeFormatter) {
    val context = LocalContext.current
    Surface(
        onClick = {
            val picker = android.app.DatePickerDialog(
                context,
                { _, y, m, d -> onDateChange(LocalDate.of(y, m + 1, d)) },
                date.year, date.monthValue - 1, date.dayOfMonth
            )
            picker.show()
        },
        color = HavamaniaTheme.colors.surfaceGlass.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, HavamaniaTheme.colors.border.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Event, null, tint = HavamaniaTheme.colors.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(date.format(formatter), style = HavamaniaTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun TimeSelectionField(time: String?, onTimeChange: (String) -> Unit, onClear: () -> Unit) {
    val context = LocalContext.current
    Surface(
        onClick = {
            val current = try { java.time.LocalTime.parse(time ?: "09:00") } catch(e: Exception) { java.time.LocalTime.of(9, 0) }
            val picker = android.app.TimePickerDialog(
                context,
                { _, h, m -> onTimeChange(String.format("%02d:%02d", h, m)) },
                current.hour, current.minute, true
            )
            picker.show()
        },
        color = HavamaniaTheme.colors.surfaceGlass.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, HavamaniaTheme.colors.border.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Schedule, null, tint = HavamaniaTheme.colors.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(time ?: "Saat seçin...", style = HavamaniaTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            if (time != null) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClear, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun PastTravelDetailDialog(
    plan: TravelPlan,
    onDismiss: () -> Unit,
    onSaveNote: (String, Int) -> Unit
) {
    var note by remember { mutableStateOf(plan.userNote ?: "") }
    var rating by remember { mutableIntStateOf(plan.userRating ?: 0) }
    val colors = HavamaniaTheme.colors
    val summary = remember(plan) { TravelAiHelper.generateHistorySummary(plan) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = plan.city.uppercase(),
                    style = HavamaniaTheme.typography.sectionTitle,
                    color = colors.accent
                )
                Text(
                    text = "Seyahat Özeti",
                    style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Black)
                )

                Spacer(Modifier.height(16.dp))

                Text(summary.summaryText, style = HavamaniaTheme.typography.bodyMedium)

                Spacer(Modifier.height(20.dp))

                SectionLabel("DENEYİMİN")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(5) { i ->
                        val star = i + 1
                        Icon(
                            imageVector = if (rating >= star) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = null,
                            tint = if (rating >= star) Color(0xFFFFB300) else colors.textMuted,
                            modifier = Modifier.size(32.dp).clickable { rating = star }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                HavamaniaTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = "Notların (Neler yedin, nereyi gezdin?)..."
                )

                Spacer(Modifier.height(24.dp))

                HavamaniaPrimaryButton(
                    text = "KAYDET",
                    onClick = { onSaveNote(note, rating); onDismiss() }
                )
            }
        }
    }
}
