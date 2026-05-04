package com.havamania

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import java.util.*

/**
 * Modern Travel Planner Models
 */
enum class TripType(val label: String, val icon: ImageVector) {
    BUSINESS("İş", Icons.Rounded.BusinessCenter),
    VACATION("Tatil", Icons.Rounded.BeachAccess),
    FAMILY("Aile", Icons.Rounded.People),
    SPORTS("Spor", Icons.Rounded.Sports),
    CAMPING("Kamp", Icons.Rounded.Terrain),
    OTHER("Diğer", Icons.Rounded.MoreHoriz)
}

data class TravelPlan(
    val id: String = UUID.randomUUID().toString(),
    val city: String,
    val tripType: TripType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val createdAt: Long = System.currentTimeMillis(),
    val weatherSummary: String? = null,
    val aiSuggestion: String? = null,
    val isAnalyzing: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelPlannerScreen(
    viewModel: TravelViewModel = viewModel(),
    onBack: () -> Unit
) {
    val plans by viewModel.plans.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var planToEdit by remember { mutableStateOf<TravelPlan?>(null) }
    var planToDelete by remember { mutableStateOf<TravelPlan?>(null) }

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "Seyahat Takvimi",
                onBack = onBack
            )
        },
        floatingActionButton = {
            PremiumRouteButton(onClick = { showAddDialog = true })
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isLoading && plans.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = HavamaniaTheme.colors.accent)
            } else if (plans.isEmpty()) {
                TravelEmptyState(onAddClick = { showAddDialog = true })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(plans, key = { it.id }) { plan ->
                        TravelPlanCard(
                            plan = plan,
                            onDelete = { planToDelete = plan },
                            onEdit = { planToEdit = plan; showAddDialog = true },
                            onReanalyze = {
                                scope.launch {
                                    // Simulated re-analysis
                                    val suggestion = generateAiSuggestion(plan.city, plan.tripType)
                                    val updatedPlan = plan.copy(aiSuggestion = suggestion)
                                    viewModel.savePlan(updatedPlan)
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
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
}

@Composable
fun TravelPlanCard(
    plan: TravelPlan,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onReanalyze: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors

    val formatter = DateTimeFormatter.ofPattern("d MMM", Locale("tr"))
    val dateRange = "${plan.startDate.format(formatter)} - ${plan.endDate.format(formatter)}"

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "cardScale")

    HavamaniaGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        alpha = 0.85f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            Brush.linearGradient(
                                colors = themeColors.buttonGradient ?: listOf(themeColors.accent, themeColors.accent.copy(alpha = 0.6f))
                            ),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(plan.tripType.icon, null, tint = themeColors.onAccent, modifier = Modifier.size(30.dp))
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.city,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = themeColors.textPrimary
                    )
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

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = themeColors.divider.copy(alpha = 0.1f))
            Spacer(Modifier.height(20.dp))

            // Weather Summary
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Rounded.WbSunny,
                    null,
                    tint = themeColors.accent,
                    modifier = Modifier.padding(top = 2.dp).size(16.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = plan.weatherSummary ?: "Hava verisi daha sonra güncellenecek",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = themeColors.textPrimary.copy(alpha = 0.9f),
                    overflow = TextOverflow.Visible
                )
            }

            Spacer(Modifier.height(16.dp))

            // AI Suggestion
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    null,
                    tint = themeColors.accent,
                    modifier = Modifier.padding(top = 2.dp).size(16.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = plan.aiSuggestion ?: "Analiz yapılması bekleniyor...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        lineHeight = 22.sp
                    ),
                    color = themeColors.textSecondary,
                    overflow = TextOverflow.Visible
                )
            }

            Spacer(Modifier.height(24.dp))

            // Actions - Re-arranged to avoid squeezing text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ActionIconButton(
                        icon = Icons.Rounded.DeleteOutline,
                        text = "Sil",
                        color = themeColors.error,
                        onClick = onDelete
                    )
                    ActionIconButton(
                        icon = Icons.Rounded.Edit,
                        text = "Düzenle",
                        color = themeColors.textSecondary,
                        onClick = onEdit
                    )
                }

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

                                onConfirm(
                                    TravelPlan(
                                        id = initialPlan?.id ?: UUID.randomUUID().toString(),
                                        city = cityInput,
                                        tripType = selectedType,
                                        startDate = finalStart,
                                        endDate = finalEnd,
                                        weatherSummary = generateWeatherSummary(cityInput, finalStart, finalEnd),
                                        aiSuggestion = generateAiSuggestion(cityInput, selectedType)
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                Box {
                    HavamaniaTextField(
                        value = cityInput,
                        onValueChange = {
                            cityInput = it
                        },
                        placeholder = "Nereye gidiyorsun?",
                        leadingIcon = Icons.Rounded.LocationOn
                    )

                    if (showSuggestions && citySuggestions.isNotEmpty()) {
                        HavamaniaGlassCard(
                            modifier = Modifier.padding(top = 64.dp).fillMaxWidth().zIndex(10f),
                            alpha = 0.95f,
                            cornerRadius = 16.dp
                        ) {
                            citySuggestions.take(5).forEach { suggestion ->
                                Text(
                                    text = "${suggestion.name}, ${suggestion.admin1 ?: suggestion.country}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            cityInput = suggestion.name
                                            selectedCity = suggestion
                                            showSuggestions = false
                                            focusManager.clearFocus()
                                        }
                                        .padding(vertical = 12.dp),
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
                    TripType.values().forEach { type ->
                        HavamaniaChip(selected = selectedType == type, onClick = { selectedType = type }, label = type.label)
                    }
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

// Logic Helpers
fun generateWeatherSummary(city: String, start: LocalDate, end: LocalDate): String {
    val tempRange = (18..26).random().let { "${it - 2}-${it + 3}°C" }
    return "$city için seyahat tarihlerinde hava genellikle parçalı bulutlu, sıcaklık $tempRange."
}

fun generateAiSuggestion(city: String, type: TripType): String {
    return when(type) {
        TripType.VACATION -> "Hafif kıyafetler ve güneş koruması eklemeyi unutma. $city sokaklarını keşfetmek için rahat ayakkabılar şart!"
        TripType.BUSINESS -> "Toplantı günleri için hava değişimine karşı yanına ince bir ceket almanı öneririm."
        TripType.FAMILY -> "Çocuklar için yedek kıyafet ve akşam serinliğine karşı hırka bulundurmak iyi olur."
        TripType.SPORTS -> "Açık hava aktiviteleri için rüzgar ve UV durumunu kontrol etmeyi ihmal etme."
        TripType.CAMPING -> "Gece sıcaklık düşüşlerine karşı termal ekipman kontrolü yapman yerinde olur."
        TripType.OTHER -> "Seyahat tarihine yaklaştıkça güncel hava durumuna göre valizini kontrol etmelisin."
    }
}

@Composable
fun TravelEmptyState(onAddClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(180.dp).blur(60.dp).background(themeColors.accent.copy(alpha = 0.15f), CircleShape))
            Icon(Icons.Rounded.Map, null, modifier = Modifier.size(100.dp), tint = themeColors.accent.copy(alpha = 0.4f))
        }
        Spacer(Modifier.height(32.dp))
        Text("Henüz bir planın yok", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
        Text("Yeni bir rota ekleyerek Havamania AI asistanından akıllı öneriler alabilirsin.", color = themeColors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))
        HavamaniaPrimaryButton(text = "Yeni Rota Oluştur", onClick = onAddClick, modifier = Modifier.width(220.dp), icon = Icons.Rounded.Add)
    }
}

@Composable
fun PremiumRouteButton(onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "fabScale")

    val backgroundBrush = if (themeColors.buttonGradient != null) {
        Brush.linearGradient(themeColors.buttonGradient)
    } else {
        Brush.linearGradient(listOf(themeColors.accent, themeColors.accent.copy(alpha = 0.7f)))
    }

    Box(
        modifier = Modifier
            .scale(scale)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = themeColors.shadow,
                spotColor = themeColors.accent
            )
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundBrush)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Add, null, tint = themeColors.onAccent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "YENİ ROTA",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = themeColors.onAccent
                )
            )
        }
    }
}

@Composable
fun ActionIconButton(icon: ImageVector, text: String, color: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "btnScale")
    val themeColors = HavamaniaTheme.colors

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.05f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = color.copy(alpha = 0.9f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black
        )
    }
}
