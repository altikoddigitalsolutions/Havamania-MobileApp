package com.havamania

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import android.widget.Toast

enum class TravelFilter { UPCOMING, PAST, ARCHIVED }

/**
 * TravelPlannerScreen - Reusable UI for managing travels
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelPlannerScreen(
    viewModel: TravelViewModel = viewModel(),
    focusId: String? = null,
    highlight: String? = null,
    onBack: () -> Unit
) {
    val allPlans by viewModel.plans.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedFilter by remember { mutableStateOf(TravelFilter.UPCOMING) }

    val today = LocalDate.now()
    val plans = remember(allPlans, selectedFilter) {
        when (selectedFilter) {
            TravelFilter.UPCOMING -> allPlans.filter { !it.isArchived && !it.endDate.isBefore(today) }
            TravelFilter.PAST -> allPlans.filter { !it.isArchived && it.endDate.isBefore(today) }
            TravelFilter.ARCHIVED -> allPlans.filter { it.isArchived }
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var planToEdit by remember { mutableStateOf<TravelPlan?>(null) }
    var planToDelete by remember { mutableStateOf<TravelPlan?>(null) }
    var selectedPlanForDetail by remember { mutableStateOf<TravelPlan?>(null) }

    val listState = rememberLazyListState()
    LaunchedEffect(focusId, plans) {
        if (!focusId.isNullOrBlank() && plans.isNotEmpty()) {
            val index = plans.indexOfFirst { it.id == focusId }
            if (index >= 0) {
                listState.animateScrollToItem(index)
            }
        }
    }

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "Seyahat Takvimi",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { selectedFilter = TravelFilter.ARCHIVED }) {
                        Icon(
                            imageVector = Icons.Rounded.Archive,
                            contentDescription = "Arşiv",
                            tint = if (selectedFilter == TravelFilter.ARCHIVED) HavamaniaTheme.colors.accent else HavamaniaTheme.colors.textSecondary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            PremiumRouteButton(onClick = { showAddDialog = true })
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Filter Row
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    HavamaniaFilterChip(
                        selected = selectedFilter == TravelFilter.UPCOMING,
                        onClick = { selectedFilter = TravelFilter.UPCOMING },
                        label = "Yaklaşanlar"
                    )
                }
                item {
                    HavamaniaFilterChip(
                        selected = selectedFilter == TravelFilter.PAST,
                        onClick = { selectedFilter = TravelFilter.PAST },
                        label = "Geçmiş"
                    )
                }
                item {
                    HavamaniaFilterChip(
                        selected = selectedFilter == TravelFilter.ARCHIVED,
                        onClick = { selectedFilter = TravelFilter.ARCHIVED },
                        label = "Arşiv"
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading && allPlans.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = HavamaniaTheme.colors.accent)
                } else if (plans.isEmpty()) {
                    TravelEmptyState(
                        filter = selectedFilter,
                        onAddClick = { showAddDialog = true }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(plans, key = { it.id }) { plan ->
                            val context = LocalContext.current
                            val isFocused = plan.id == focusId
                            TravelPlanCard(
                                plan = plan,
                                isFocused = isFocused,
                                onDelete = { planToDelete = plan },
                                onEdit = { planToEdit = plan; showAddDialog = true },
                                onArchive = { viewModel.archiveTrip(plan.id) },
                                onUnarchive = { viewModel.unarchiveTrip(plan.id) },
                                onShowDetail = { selectedPlanForDetail = plan },
                                onReanalyze = {
                                    val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), plan.startDate)
                                    if (daysUntil > 15) {
                                        Toast.makeText(context, "Bu seyahat için güvenilir hava analizi seyahate 15 gün kala yapılabilir.", Toast.LENGTH_LONG).show()
                                    } else {
                                        viewModel.analyzeTravelWeather(plan)
                                    }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(100.dp)) }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTravelPlanDialog(
            viewModel = viewModel,
            initialPlan = planToEdit,
            onDismiss = { showAddDialog = false; planToEdit = null },
            onConfirm = { newPlan ->
                viewModel.savePlan(newPlan)
                showAddDialog = false
                planToEdit = null
            }
        )
    }

    if (planToDelete != null) {
        AlertDialog(
            onDismissRequest = { planToDelete = null },
            containerColor = HavamaniaTheme.colors.surface,
            title = { Text("Seyahati Sil", fontWeight = FontWeight.Black, color = HavamaniaTheme.colors.textPrimary) },
            text = { Text("${planToDelete?.city} seyahatini silmek istediğinize emin misiniz?", color = HavamaniaTheme.colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlan(planToDelete!!.id)
                    planToDelete = null
                }) {
                    Text("Sil", color = HavamaniaTheme.colors.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { planToDelete = null }) {
                    Text("İptal", color = HavamaniaTheme.colors.textPrimary)
                }
            }
        )
    }

    if (selectedPlanForDetail != null) {
        PastTripDetailDialog(
            plan = selectedPlanForDetail!!,
            onDismiss = { selectedPlanForDetail = null }
        )
    }
}

@Composable
fun TravelPlanCard(
    plan: TravelPlan,
    isFocused: Boolean = false,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onShowDetail: () -> Unit,
    onReanalyze: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    val today = LocalDate.now()
    val isPast = plan.endDate.isBefore(today)
    val isArchived = plan.isArchived

    val formatter = DateTimeFormatter.ofPattern("d MMM", Locale("tr"))
    val dateRange = "${plan.startDate.format(formatter)} - ${plan.endDate.format(formatter)}"

    val interactionSource = remember { MutableInteractionSource() }
    val isPressedState = interactionSource.collectIsPressedAsState()
    val isPressed = isPressedState.value
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "cardScale")

    val borderGlow by animateColorAsState(
        targetValue = if (isFocused) themeColors.accent else Color.Transparent,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "borderGlow"
    )

    HavamaniaGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(if (isPast && !isArchived) 0.8f else 1f)
            .then(if (isFocused) Modifier.border(2.dp, borderGlow, RoundedCornerShape(32.dp)) else Modifier),
        alpha = if (isArchived) 0.7f else 0.85f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            Brush.linearGradient(
                                colors = if (isArchived) listOf(themeColors.textMuted, themeColors.textMuted.copy(0.6f))
                                else themeColors.buttonGradient ?: listOf(themeColors.accent, themeColors.accent.copy(alpha = 0.6f))
                            ),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (isArchived) Icons.Rounded.Archive else plan.tripType.icon, null, tint = themeColors.onAccent, modifier = Modifier.size(30.dp))
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = plan.city,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = themeColors.textPrimary,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isPast && !isArchived) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF3B82F6).copy(alpha = 0.1f))
                                    .border(0.5.dp, Color(0xFF3B82F6).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("TAMAMLANDI", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF60A5FA))
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Event, null, tint = themeColors.textMuted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = dateRange,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = themeColors.textSecondary
                        )
                    }
                }

                if (!isArchived) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(themeColors.accent.copy(alpha = 0.1f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = plan.tripType.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
                            color = themeColors.accent
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = themeColors.divider.copy(alpha = 0.1f))
            Spacer(Modifier.height(20.dp))

            // Analysis Section
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    if (isPast || isArchived) Icons.Rounded.Info else Icons.Rounded.WbSunny,
                    null,
                    tint = if (isArchived) themeColors.textMuted else themeColors.accent,
                    modifier = Modifier.padding(top = 2.dp).size(16.dp)
                )
                Spacer(Modifier.width(12.dp))
                val weatherText = when {
                    isArchived -> "Bu seyahat arşivlendi. Tüm analiz notların ve hava durumu geçmişi burada saklanıyor."
                    isPast -> "Bu seyahat tamamlandı. Trabzon seyahatinizdeki hava koşulları ve önceki analiz notları arşivlenmeye hazır."
                    plan.isAnalyzing -> "Hava durumu verileri analiz ediliyor..."
                    plan.weatherAnalysisStatus == TravelWeatherAnalysisStatus.ERROR -> plan.lastWeatherAnalysisText ?: "Analiz sırasında bir hata oluştu."
                    plan.weatherAnalysisStatus == TravelWeatherAnalysisStatus.TOO_EARLY -> "Bu seyahat için güvenilir hava tahmini henüz erken. Seyahate 15 gün kala hava analizini başlatacağım."
                    else -> plan.lastWeatherAnalysisText ?: "Hava verisi henüz analiz edilmedi."
                }
                Text(
                    text = weatherText,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = themeColors.textPrimary.copy(alpha = 0.9f),
                    overflow = TextOverflow.Visible
                )
            }

            if (!isArchived && !isPast && plan.aiSuggestion != null) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = themeColors.accent, modifier = Modifier.padding(top = 2.dp).size(16.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = plan.aiSuggestion,
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, lineHeight = 22.sp),
                        color = themeColors.textSecondary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ActionIconButton(icon = Icons.Rounded.DeleteOutline, text = "Sil", color = themeColors.error, onClick = onDelete)
                    ActionIconButton(icon = Icons.Rounded.Edit, text = "Düzenle", color = themeColors.textSecondary, onClick = onEdit)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isArchived) {
                        ActionIconButton(icon = Icons.Rounded.Unarchive, text = "Geri Al", color = themeColors.textMuted, onClick = onUnarchive)
                    } else if (isPast) {
                        ActionIconButton(icon = Icons.Rounded.Archive, text = "Arşivle", color = Color(0xFF60A5FA), onClick = onArchive)
                    }

                    if (isPast || isArchived) {
                        HavamaniaPrimaryButton(
                            text = "Geçmiş Özeti",
                            onClick = onShowDetail,
                            modifier = Modifier.width(140.dp).height(40.dp),
                            icon = Icons.Rounded.Analytics
                        )
                    } else {
                        HavamaniaPrimaryButton(
                            text = if (plan.isAnalyzing) "Analiz..." else "Yeniden Analiz",
                            onClick = onReanalyze,
                            modifier = Modifier.width(150.dp).height(44.dp),
                            icon = Icons.Rounded.AutoAwesome
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PastTripDetailDialog(plan: TravelPlan, onDismiss: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HavamaniaScreen(
            topBar = { HavamaniaTopBar(title = "Geçmiş Seyahat Özeti", onBack = onDismiss) }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp)
            ) {
                Text(text = plan.city, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
                Text(text = "${plan.startDate} - ${plan.endDate}", style = MaterialTheme.typography.titleMedium, color = themeColors.textSecondary)

                Spacer(Modifier.height(32.dp))

                DetailSection(title = "HAVA DURUMU KAYITLARI", icon = Icons.Rounded.CloudDone) {
                    Text(
                        text = "Bu seyahat için kayıtlı detaylı hava geçmişi bulunmuyor. Ancak seyahat notların ve önceki analiz önerilerin saklandı.",
                        color = themeColors.textSecondary,
                        lineHeight = 22.sp
                    )
                }

                if (!plan.aiSuggestion.isNullOrBlank()) {
                    Spacer(Modifier.height(16.dp))
                    DetailSection(title = "AI ANALİZ ÖNERİLERİ", icon = Icons.Rounded.AutoAwesome) {
                        Text(text = plan.aiSuggestion, color = themeColors.textSecondary, lineHeight = 22.sp)
                    }
                }

                Spacer(Modifier.height(40.dp))
                HavamaniaPrimaryButton(
                    text = "Bu Rotayı Tekrar Planla",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun DetailSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    HavamaniaGlassCard(modifier = Modifier.fillMaxWidth(), alpha = 0.5f) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp), color = themeColors.textPrimary)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTravelPlanDialog(
    viewModel: TravelViewModel,
    initialPlan: TravelPlan?,
    onDismiss: () -> Unit,
    onConfirm: (TravelPlan) -> Unit
) {
    var cityInput by remember { mutableStateOf(initialPlan?.city ?: "") }
    var selectedCity by remember { mutableStateOf<GeocodingResultDto?>(null) }
    var selectedType by remember { mutableStateOf(initialPlan?.tripType ?: TripType.VACATION) }

    val citySuggestions by viewModel.citySuggestions.collectAsState()
    var showSuggestions by remember { mutableStateOf(false) }

    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialPlan?.startDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        initialSelectedEndDateMillis = initialPlan?.endDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    )

    val themeColors = HavamaniaTheme.colors
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    LaunchedEffect(cityInput) {
        if (cityInput.length > 1 && cityInput != selectedCity?.name) {
            viewModel.searchCity(cityInput)
            showSuggestions = true
        } else {
            showSuggestions = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        HavamaniaScreen(
            topBar = {
                HavamaniaTopBar(
                    title = if (initialPlan == null) "Yeni Seyahat" else "Planı Düzenle",
                    onBack = onDismiss,
                    actions = {
                        val isFormValid = cityInput.isNotBlank() && dateRangePickerState.selectedStartDateMillis != null
                        TextButton(
                            enabled = isFormValid,
                            onClick = {
                                val start = Instant.ofEpochMilli(dateRangePickerState.selectedStartDateMillis!!).atZone(ZoneId.systemDefault()).toLocalDate()
                                val end = dateRangePickerState.selectedEndDateMillis?.let {
                                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                                } ?: start

                                val finalStart = if (end.isBefore(start)) end else start
                                val finalEnd = if (end.isBefore(start)) start else end

                                if (finalStart.isBefore(LocalDate.now()) && initialPlan == null) {
                                    Toast.makeText(context, "Geçmiş tarihli seyahat oluşturamazsınız.", Toast.LENGTH_SHORT).show()
                                    return@TextButton
                                }

                                onConfirm(
                                    TravelPlan(
                                        id = initialPlan?.id ?: UUID.randomUUID().toString(),
                                        city = cityInput,
                                        latitude = selectedCity?.latitude ?: initialPlan?.latitude ?: 0.0,
                                        longitude = selectedCity?.longitude ?: initialPlan?.longitude ?: 0.0,
                                        tripType = selectedType,
                                        startDate = finalStart,
                                        endDate = finalEnd,
                                        weatherSummary = initialPlan?.weatherSummary,
                                        aiSuggestion = initialPlan?.aiSuggestion,
                                        isArchived = initialPlan?.isArchived ?: false
                                    )
                                )
                            }
                        ) {
                            Text("KAYDET", fontWeight = FontWeight.Black, color = if (isFormValid) themeColors.accent else themeColors.textMuted)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                Box {
                    HavamaniaTextField(value = cityInput, onValueChange = { cityInput = it }, placeholder = "Nereye gidiyorsun?", leadingIcon = Icons.Rounded.LocationOn)
                    if (showSuggestions && citySuggestions.isNotEmpty()) {
                        HavamaniaGlassCard(modifier = Modifier.padding(top = 64.dp).fillMaxWidth().zIndex(10f), alpha = 0.95f, cornerRadius = 16.dp) {
                            citySuggestions.take(5).forEach { suggestion ->
                                Text(
                                    text = "${suggestion.name}, ${suggestion.admin1 ?: suggestion.country}",
                                    modifier = Modifier.fillMaxWidth().clickable { cityInput = suggestion.name; selectedCity = suggestion; showSuggestions = false; focusManager.clearFocus() }.padding(vertical = 12.dp),
                                    color = themeColors.textPrimary
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
                Text("SEYAHAT TİPİ", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.5.sp), color = themeColors.accent)
                Spacer(Modifier.height(16.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TripType.values().forEach { type -> HavamaniaChip(selected = selectedType == type, onClick = { selectedType = type }, label = type.label) }
                }
                Spacer(Modifier.height(32.dp))
                Text("TARİH ARALIĞI SEÇ", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.5.sp), color = themeColors.accent)
                Spacer(Modifier.height(16.dp))
                HavamaniaGlassCard(alpha = 0.5f, cornerRadius = 20.dp) {
                    DateRangePicker(
                        state = dateRangePickerState,
                        modifier = Modifier.height(400.dp),
                        title = null,
                        headline = null,
                        showModeToggle = false,
                        colors = DatePickerDefaults.colors(
                            containerColor = Color.Transparent,
                            selectedDayContainerColor = themeColors.accent,
                            selectedDayContentColor = themeColors.onAccent,
                            todayContentColor = themeColors.accent,
                            todayDateBorderColor = themeColors.accent,
                            dayContentColor = themeColors.textPrimary,
                            disabledDayContentColor = themeColors.textMuted,
                            titleContentColor = themeColors.textPrimary,
                            headlineContentColor = themeColors.textPrimary,
                            weekdayContentColor = themeColors.textSecondary,
                            subheadContentColor = themeColors.textSecondary,
                            navigationContentColor = themeColors.accent
                        )
                    )
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun TravelEmptyState(filter: TravelFilter, onAddClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    val text = when(filter) {
        TravelFilter.ARCHIVED -> "Arşivlenmiş seyahatiniz yok"
        TravelFilter.PAST -> "Geçmiş seyahatiniz yok"
        TravelFilter.UPCOMING -> "Henüz bir planın yok"
    }
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(180.dp).blur(60.dp).background(themeColors.accent.copy(alpha = 0.15f), CircleShape))
            Icon(if (filter == TravelFilter.ARCHIVED) Icons.Rounded.Archive else Icons.Rounded.Map, null, modifier = Modifier.size(100.dp), tint = themeColors.accent.copy(alpha = 0.4f))
        }
        Spacer(Modifier.height(32.dp))
        Text(text, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
        if (filter == TravelFilter.UPCOMING) {
            Text("Yeni bir rota ekleyerek Havamania AI asistanından akıllı öneriler alabilirsin.", color = themeColors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))
            HavamaniaPrimaryButton(text = "Yeni Rota Oluştur", onClick = onAddClick, modifier = Modifier.width(220.dp), icon = Icons.Rounded.Add)
        }
    }
}

@Composable
fun PremiumRouteButton(onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressedState = interactionSource.collectIsPressedAsState()
    val isPressed = isPressedState.value
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "fabScale")
    val backgroundBrush = if (themeColors.buttonGradient != null) Brush.linearGradient(themeColors.buttonGradient) else Brush.linearGradient(listOf(themeColors.accent, themeColors.accent.copy(alpha = 0.7f)))

    Box(
        modifier = Modifier.scale(scale).shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp), ambientColor = themeColors.shadow, spotColor = themeColors.accent).clip(RoundedCornerShape(24.dp)).background(backgroundBrush).clickable(interactionSource = interactionSource, indication = null, onClick = onClick).padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Add, null, tint = themeColors.onAccent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text("YENİ ROTA", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = themeColors.onAccent))
        }
    }
}

@Composable
fun ActionIconButton(icon: ImageVector, text: String, color: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressedState = interactionSource.collectIsPressedAsState()
    val isPressed = isPressedState.value
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "btnScale")
    Row(
        modifier = Modifier.scale(scale).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.05f)).clickable(interactionSource = interactionSource, indication = null, onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = text, color = color.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
    }
}

@Composable
fun HavamaniaFilterChip(selected: Boolean, onClick: () -> Unit, label: String) {
    val themeColors = HavamaniaTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) themeColors.accent else themeColors.surfaceGlass.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, if (selected) themeColors.accent else themeColors.border.copy(alpha = 0.1f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            color = if (selected) Color.White else themeColors.textPrimary
        )
    }
}
