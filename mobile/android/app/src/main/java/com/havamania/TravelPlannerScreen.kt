package com.havamania

import android.util.Log
import android.widget.Toast
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

enum class TravelFilter { UPCOMING, PAST, ARCHIVED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelPlannerScreen(
    viewModel: TravelViewModel = viewModel(),
    initialCity: String? = null,
    initialTripType: String? = null,
    focusId: String? = null,
    highlight: String? = null,
    onBack: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    val plans by viewModel.plans.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedFilter by remember { mutableStateOf(TravelFilter.UPCOMING) }
    var showAddDialog by remember { mutableStateOf(false) }
    var planToEdit by remember { mutableStateOf<TravelPlan?>(null) }
    var planForSummary by remember { mutableStateOf<TravelPlan?>(null) }
    var deleteConfirmPlan by remember { mutableStateOf<TravelPlan?>(null) }
    val listState = rememberLazyListState()

    val filteredPlans = remember(plans, selectedFilter) {
        when (selectedFilter) {
            TravelFilter.UPCOMING -> plans.filter { !it.isArchived && !it.endDate.isBefore(LocalDate.now()) }
            TravelFilter.PAST -> plans.filter { !it.isArchived && it.endDate.isBefore(LocalDate.now()) }
            TravelFilter.ARCHIVED -> plans.filter { it.isArchived }
        }
    }

    LaunchedEffect(initialCity, initialTripType) {
        if (initialCity != null) {
            showAddDialog = true
        }
    }

    LaunchedEffect(focusId, plans) {
        if (focusId != null && plans.isNotEmpty()) {
            val index = filteredPlans.indexOfFirst { it.id == focusId }
            if (index != -1) {
                listState.animateScrollToItem(index)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp) // Reduced padding for density
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(themeColors.surface.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = themeColors.textPrimary)
                    }
                    Text(
                        "Seyahat Planlayıcı",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = themeColors.textPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (plans.isEmpty()) {
                            Text(
                                "Yeni şehir analizi oluşturmak için sağ üstteki + simgesine dokun.",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = themeColors.accent,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .background(themeColors.accent.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(themeColors.accent)
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = themeColors.onAccent)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp)) // Reduced spacer

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TravelFilter.values().forEach { filter ->
                        val count = when (filter) {
                            TravelFilter.UPCOMING -> plans.count { !it.isArchived && !it.endDate.isBefore(LocalDate.now()) }
                            TravelFilter.PAST -> plans.count { !it.isArchived && it.endDate.isBefore(LocalDate.now()) }
                            TravelFilter.ARCHIVED -> plans.count { it.isArchived }
                        }
                        HavamaniaFilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = when (filter) {
                                TravelFilter.UPCOMING -> "Yaklaşanlar ($count)"
                                TravelFilter.PAST -> "Geçmiş ($count)"
                                TravelFilter.ARCHIVED -> "Arşiv ($count)"
                            }
                        )
                    }
                }

                if (plans.isEmpty() && selectedFilter == TravelFilter.UPCOMING) {
                    Spacer(Modifier.height(12.dp))
                    TravelHowItWorksCard()
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading && plans.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = themeColors.accent)
            } else if (filteredPlans.isEmpty()) {
                TravelEmptyState(selectedFilter) { showAddDialog = true }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredPlans, key = { it.id }) { plan ->
                        TravelPlanCard(
                            plan = plan,
                            isFocused = plan.id == focusId || plan.city.contains(highlight ?: "", ignoreCase = true),
                            onDelete = { deleteConfirmPlan = plan },
                            onEdit = {
                                planToEdit = plan
                                showAddDialog = true
                            },
                            onArchive = { viewModel.archiveTrip(plan.id) },
                            onUnarchive = { viewModel.unarchiveTrip(plan.id) },
                            onShowDetail = { planForSummary = plan },
                            onReanalyze = { viewModel.analyzeTravelWeather(plan) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTravelPlanDialog(
            viewModel = viewModel,
            editPlan = planToEdit,
            onDismiss = {
                showAddDialog = false
                planToEdit = null
            },
            onSave = { plan ->
                viewModel.savePlan(plan)
                showAddDialog = false
                planToEdit = null
            }
        )
    }

    if (planForSummary != null) {
        PastTravelDetailDialog(
            plan = planForSummary!!,
            onDismiss = { planForSummary = null }
        )
    }

    if (deleteConfirmPlan != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmPlan = null },
            title = { Text("Seyahati Sil") },
            text = { Text("${deleteConfirmPlan?.city} seyahatini kalıcı olarak silmek istiyor musun?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlan(deleteConfirmPlan!!.id)
                    deleteConfirmPlan = null
                }) {
                    Text("Sil", color = themeColors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmPlan = null }) {
                    Text("İptal")
                }
            },
            containerColor = themeColors.surface,
            titleContentColor = themeColors.textPrimary,
            textContentColor = themeColors.textSecondary
        )
    }
}

@Composable
fun PastTravelDetailDialog(
    plan: TravelPlan,
    viewModel: TravelViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    val summary = remember(plan) { TravelAiHelper.generateHistorySummary(plan) }

    var userNote by remember { mutableStateOf(plan.userNote ?: "") }
    var userRating by remember { mutableIntStateOf(plan.userRating ?: 0) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HavamaniaGlassCard(
            modifier = Modifier
                .fillMaxWidth(0.94f) // Wider for premium feel
                .wrapContentHeight()
                .padding(16.dp),
            alpha = 0.98f
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            "Geçmiş Seyahat Özeti",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = themeColors.accent
                        )
                        Text(
                            plan.city,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = themeColors.textPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp).background(themeColors.surface.copy(alpha = 0.5f), CircleShape)) {
                        Icon(Icons.Rounded.Close, null, modifier = Modifier.size(18.dp))
                    }
                }

                val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr"))
                Text(
                    "${plan.startDate.format(formatter)} - ${plan.endDate.format(formatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.textMuted
                )
                Text(
                    "${summary.durationDays} günlük seyahat",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = themeColors.textSecondary
                )

                Spacer(Modifier.height(24.dp))

                // Stats Grid
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryStatCard(
                            Modifier.weight(1f),
                            "Ort. Sıcaklık",
                            if (summary.averageTemp != null) "${summary.averageTemp}°" else "Veri yok",
                            Icons.Rounded.Thermostat
                        )
                        SummaryStatCard(
                            Modifier.weight(1f),
                            "Güneşli Gün",
                            if (summary.sunnyDays >= 0) "${summary.sunnyDays}" else "Veri yok",
                            Icons.Rounded.WbSunny
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryStatCard(
                            Modifier.weight(1f),
                            "Yağışlı Gün",
                            if (summary.rainyDays >= 0) "${summary.rainyDays}" else "Veri yok",
                            Icons.Rounded.WaterDrop
                        )
                        SummaryStatCard(
                            Modifier.weight(1f),
                            "Konfor Skoru",
                            if (summary.comfortScore > 0) "%${summary.comfortScore}" else "Veri yok",
                            Icons.Rounded.AutoAwesome
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // AI Evaluation Section
                Text("ASİSTAN DEĞERLENDİRMESİ", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = themeColors.textMuted)
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = themeColors.accent.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.1f))
                ) {
                    Text(
                        summary.summaryText,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = themeColors.textPrimary,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Detailed Analysis Blocks
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnalysisBlock("📌 SEYAHAT NOTU", summary.riskDayText, Icons.Rounded.PushPin)
                    AnalysisBlock("🎒 BİR SONRAKİ SEFER İÇİN", summary.packingAdvice, Icons.Rounded.Backpack)
                    AnalysisBlock("📍 HATIRLANACAK ÖNERİ", summary.nextTripAdvice, Icons.Rounded.Lightbulb)
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = themeColors.divider.copy(alpha = 0.1f))
                Spacer(Modifier.height(24.dp))

                // Personal Input Section
                Text("BU SEYAHATİ PUANLA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = themeColors.textMuted)
                Spacer(Modifier.height(8.dp))
                RatingStars(rating = userRating, onRatingChange = { userRating = it })

                Spacer(Modifier.height(20.dp))

                Text("BU SEYAHATE NOT EKLE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = themeColors.textMuted)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = userNote,
                    onValueChange = { userNote = it },
                    placeholder = { Text("Bu seyahatten aklında kalanları yaz...", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = themeColors.divider.copy(alpha = 0.2f),
                        unfocusedContainerColor = themeColors.surface.copy(alpha = 0.3f)
                    ),
                    maxLines = 4
                )

                Spacer(Modifier.height(32.dp))

                // Footer Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, themeColors.textMuted.copy(alpha = 0.3f))
                    ) {
                        Text("Anladım", color = themeColors.textPrimary)
                    }
                    HavamaniaPrimaryButton(
                        text = "Notu Kaydet",
                        onClick = {
                            viewModel.updateTripNoteAndRating(plan.id, userNote, userRating)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.5f).height(50.dp),
                        icon = Icons.Rounded.Save
                    )
                }
            }
        }
    }
}

@Composable
fun RatingStars(rating: Int, onRatingChange: (Int) -> Unit) {
    val themeColors = HavamaniaTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(5) { index ->
            val starIndex = index + 1
            IconButton(
                onClick = { onRatingChange(starIndex) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (starIndex <= rating) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    contentDescription = null,
                    tint = if (starIndex <= rating) Color(0xFFFBBF24) else themeColors.textMuted.copy(alpha = 0.3f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun SummaryStatCard(modifier: Modifier, label: String, value: String, icon: ImageVector) {
    val themeColors = HavamaniaTheme.colors
    Surface(
        modifier = modifier,
        color = themeColors.surface.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, themeColors.divider.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = themeColors.textMuted)
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
        }
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
    var isExpanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val formatter = DateTimeFormatter.ofPattern("d MMM", Locale("tr"))
    val dateRange = "${plan.startDate.format(formatter)} - ${plan.endDate.format(formatter)}"
    val cityDesc = TravelAiHelper.getCityDescription(plan.city)
    val latestAnalysis = plan.analyses.lastOrNull()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressedState = interactionSource.collectIsPressedAsState()
    val isPressed = isPressedState.value
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "cardScale")

    Surface(
        color = themeColors.surface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(20.dp), // Slightly smaller radius for professional look
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(if (isPast && !isArchived) 0.85f else 1f)
            .then(if (isFocused) Modifier.border(1.5.dp, themeColors.accent, RoundedCornerShape(20.dp)) else Modifier)
            .clickable {
                if (!isPast && !isArchived) isExpanded = !isExpanded
                else onShowDetail()
            }
    ) {
        Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Brush.linearGradient(
                                colors = if (isArchived) listOf(themeColors.textMuted, themeColors.textMuted.copy(0.6f))
                                else themeColors.buttonGradient ?: listOf(themeColors.accent, themeColors.accent.copy(alpha = 0.6f))
                            ),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (isArchived) Icons.Rounded.Archive else plan.tripType.icon, null, tint = themeColors.onAccent, modifier = Modifier.size(20.dp))
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.city,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 16.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = themeColors.textPrimary
                    )
                    Text(
                        text = dateRange,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = themeColors.textSecondary
                    )
                }

                if (latestAnalysis != null && !isArchived) {
                    val scoreColor = when {
                        latestAnalysis.travelScore >= 80 -> Color(0xFF10B981)
                        latestAnalysis.travelScore >= 60 -> Color(0xFFFBBF24)
                        else -> Color(0xFFEF4444)
                    }
                    Surface(
                        color = scoreColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, scoreColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "%${latestAnalysis.travelScore}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = scoreColor
                        )
                    }
                }

                Box(modifier = Modifier.padding(start = 4.dp)) {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.MoreVert, null, tint = themeColors.textMuted, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = themeColors.surface
                    ) {
                        if (!isPast && !isArchived) {
                            DropdownMenuItem(
                                text = { Text("Düzenle", color = themeColors.textPrimary) },
                                onClick = { onEdit(); showMenu = false },
                                leadingIcon = { Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                        if (isArchived) {
                            DropdownMenuItem(
                                text = { Text("Aktifleştir", color = themeColors.textPrimary) },
                                onClick = { onUnarchive(); showMenu = false },
                                leadingIcon = { Icon(Icons.Rounded.Unarchive, null, modifier = Modifier.size(18.dp)) }
                            )
                        } else if (isPast) {
                            DropdownMenuItem(
                                text = { Text("Arşivle", color = themeColors.textPrimary) },
                                onClick = { onArchive(); showMenu = false },
                                leadingIcon = { Icon(Icons.Rounded.Archive, null, modifier = Modifier.size(18.dp)) }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Arşivle", color = themeColors.textPrimary) },
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
            }

            Spacer(Modifier.height(8.dp))

            // Chips Row
            if (latestAnalysis != null && !isArchived) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactInfoChip(
                        Icons.Rounded.WaterDrop,
                        "Yağış: %${latestAnalysis.rainRiskPercent ?: 0}",
                        if ((latestAnalysis.rainRiskPercent ?: 0) > 40) themeColors.error else themeColors.accent
                    )
                    CompactInfoChip(
                        Icons.Rounded.Thermostat,
                        "${latestAnalysis.averageTemperature?.toInt() ?: "--"}°C",
                        themeColors.textSecondary
                    )
                    val statusLabel = if (isPast) "Tamamlandı" else if (plan.isAnalyzing) "Analiz ediliyor" else "Planlandı"
                    CompactInfoChip(
                        if (isPast) Icons.Rounded.CheckCircle else Icons.Rounded.Event,
                        statusLabel,
                        if (isPast) Color(0xFF10B981) else themeColors.accent
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            if (!isArchived) {
                Text(
                    text = latestAnalysis?.summary ?: "Detaylı şehir analizi seyahate 15 gün kala otomatik olarak burada belirecek.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = themeColors.textPrimary.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "Bu seyahat arşivde saklanıyor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.textMuted
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isArchived && !isPast && ChronoUnit.DAYS.between(today, plan.startDate) <= 15) {
                    TextButton(
                        onClick = onReanalyze,
                        enabled = !plan.isAnalyzing,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(if (plan.isAnalyzing) Icons.Rounded.Sync else Icons.Rounded.Refresh, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Güncelle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                }

                HavamaniaPrimaryButton(
                    text = if (isPast || isArchived) "Raporu Gör" else "Detaylar",
                    onClick = { if (isPast || isArchived) onShowDetail() else isExpanded = !isExpanded },
                    modifier = Modifier.height(34.dp).width(110.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    if (analysis?.comparisonText != null && !analysis.comparisonText.contains("ilk analiz")) {
                        Spacer(Modifier.height(12.dp))
                        ComparisonSection(analysis.comparisonText, analysis.previousAnalysisId != null)
                    }

                    Spacer(Modifier.height(12.dp))
                    PremiumAnalysisBlocks(plan.aiSuggestion ?: "")
                }
            }
        }
    }
}


@Composable
fun PastTripContent(plan: TravelPlan, analysis: TravelWeatherAnalysis?, duration: Int) {
    val themeColors = HavamaniaTheme.colors
    Column {
        Text(
            text = "$duration günlük keyifli bir seyahat olarak kaydedildi.",
            style = MaterialTheme.typography.bodySmall,
            color = themeColors.textSecondary
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val weatherLabel = analysis?.summary?.split(" ")?.lastOrNull() ?: "Veri yok"
            CompactInfoChip(Icons.Rounded.WbSunny, "Hava: $weatherLabel", themeColors.accent)
            CompactInfoChip(Icons.Rounded.AutoAwesome, "Skor: %${analysis?.travelScore ?: "--"}", Color(0xFFFBBF24))
        }

        if (!plan.userNote.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                Icon(Icons.Rounded.PushPin, null, tint = themeColors.accent, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = plan.userNote,
                    style = MaterialTheme.typography.labelSmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = themeColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ArchiveContent(plan: TravelPlan, analysis: TravelWeatherAnalysis?, duration: Int, dateFormatter: DateTimeFormatter) {
    val themeColors = HavamaniaTheme.colors
    val archiveDate = Instant.ofEpochMilli(plan.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()

    Column {
        Text(
            text = "Bu seyahatin analizleri ve notları kütüphanende saklanıyor.",
            style = MaterialTheme.typography.bodySmall,
            color = themeColors.textSecondary
        )

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ArchiveStatItem("Toplam Gün", "$duration")
            ArchiveStatItem("Skor", "%${analysis?.travelScore ?: "--"}")
            ArchiveStatItem("Notlar", if (plan.userNote.isNullOrBlank()) "0" else "1")
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "Kütüphaneye Eklenme: ${archiveDate.format(dateFormatter)}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = themeColors.textMuted
        )
    }
}

@Composable
fun UpcomingTripContent(plan: TravelPlan, analysis: TravelWeatherAnalysis?, isExpanded: Boolean, onToggleExpand: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    val today = LocalDate.now()
    val daysUntilTrip = ChronoUnit.DAYS.between(today, plan.startDate)
    val hasAnalysisData = analysis != null
    val isWeatherReal = analysis?.averageTemperature != 0.0 && analysis?.averageTemperature != null

    Column {
        val statusText = when {
            plan.isAnalyzing -> "Analiz hazırlanıyor..."
            hasAnalysisData && isWeatherReal -> "✅ Seyahat Önerilerin Hazır"
            hasAnalysisData -> "✅ Temel Öneriler Hazır"
            daysUntilTrip > 15 -> "📅 15 gün kala hazırlanacak"
            else -> "❌ Veri Alınamadı"
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
            color = if (hasAnalysisData) themeColors.accent else themeColors.textMuted
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = analysis?.summary ?: "Detaylı şehir analizi seyahate 15 gün kala otomatik olarak burada belirecek.",
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
            color = themeColors.textPrimary.copy(alpha = 0.9f),
            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis
        )

        if (hasAnalysisData) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AnalysisMetric("SKOR", WeatherUtils.formatRainProbability(analysis.travelScore), themeColors.accent)
                    AnalysisMetric("YAĞIŞ", WeatherUtils.getPrecipitationRiskText(analysis.rainRiskPercent, 0.0, 0), if((analysis.rainRiskPercent ?: 0) > 40) themeColors.error else themeColors.textPrimary)
                }

                TextButton(onClick = onToggleExpand, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text(if (isExpanded) "Küçült ▲" else "Detaylar ▼", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = themeColors.accent)
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    if (analysis?.comparisonText != null && !analysis.comparisonText.contains("ilk analiz")) {
                        Spacer(Modifier.height(12.dp))
                        ComparisonSection(analysis.comparisonText, analysis.previousAnalysisId != null)
                    }

                    Spacer(Modifier.height(12.dp))
                    PremiumAnalysisBlocks(plan.aiSuggestion ?: "")
                }
            }
        }
    }
}

@Composable
fun ActionRow(
    isPast: Boolean,
    isArchived: Boolean,
    isAnalyzing: Boolean,
    daysUntilTrip: Long,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onShowDetail: () -> Unit,
    onReanalyze: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CompactActionButton(Icons.Rounded.DeleteOutline, "Sil", themeColors.error.copy(alpha = 0.8f), onDelete)
            if (!isPast && !isArchived) {
                CompactActionButton(Icons.Rounded.Edit, "Düzenle", themeColors.textSecondary, onEdit)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isArchived) {
                HavamaniaPrimaryButton(
                    text = "Aktif Listeye Taşı",
                    onClick = onUnarchive,
                    modifier = Modifier.height(36.dp).width(160.dp),
                    icon = Icons.Rounded.Unarchive
                )
            } else if (isPast) {
                CompactActionButton(Icons.Rounded.Archive, "Arşivle", Color(0xFF3B82F6), onArchive)
                HavamaniaPrimaryButton(
                    text = "Detaylı Rapor",
                    onClick = onShowDetail,
                    modifier = Modifier.height(36.dp).width(130.dp),
                    icon = Icons.Rounded.Analytics
                )
            } else {
                if (daysUntilTrip <= 15) {
                    OutlinedButton(
                        onClick = onReanalyze,
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        enabled = !isAnalyzing
                    ) {
                        Icon(if (isAnalyzing) Icons.Rounded.Sync else Icons.Rounded.Refresh, null, tint = themeColors.accent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isAnalyzing) "Bekle..." else "Güncelle", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    }
                }
            }
        }
    }
}

@Composable
fun CompactActionButton(icon: ImageVector, text: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CompactInfoChip(icon: ImageVector, text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = color)
        }
    }
}

@Composable
fun ArchiveStatItem(label: String, value: String) {
    val themeColors = HavamaniaTheme.colors
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = themeColors.textMuted)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
    }
}

@Composable
fun ComparisonSection(comparisonText: String, hasHistory: Boolean) {
    val themeColors = HavamaniaTheme.colors

    Surface(
        color = themeColors.accent.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CompareArrows,
                    contentDescription = null,
                    tint = themeColors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "ÖNCEKİ ANALİZE GÖRE DEĞİŞİM",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
                    color = themeColors.accent
                )
            }
            Spacer(Modifier.height(8.dp))

            val changes = if (comparisonText.contains(": ")) {
                 comparisonText.split(": ").last().split(". ").filter { it.isNotBlank() }
            } else {
                 listOf(comparisonText)
            }

            if (changes.size > 1 || (changes.size == 1 && changes[0].contains("→") || changes[0].contains("yükseldi") || changes[0].contains("düştü"))) {
                changes.forEach { change ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("•", color = themeColors.accent, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = change.trim().let { if (it.endsWith(".")) it else "$it." },
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                            color = themeColors.textPrimary
                        )
                    }
                }
            } else {
                Text(
                    text = comparisonText,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                    color = themeColors.textPrimary
                )
            }
        }
    }
}

@Composable
fun AnalysisMetric(label: String, value: String, valueColor: Color) {
    val themeColors = HavamaniaTheme.colors
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = themeColors.textMuted)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = valueColor)
    }
}

@Composable
fun PremiumAnalysisBlocks(suggestion: String) {
    if (suggestion.isBlank()) return

    val sections = suggestion.split("[SEP]")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        sections.forEach { section ->
            val parts = section.split("|")
            if (parts.size == 2) {
                val title = parts[0].trim()
                val content = parts[1].trim()
                val icon = when {
                    title.contains("HAVA") -> Icons.Rounded.Cloud
                    title.contains("VALİZ") -> Icons.Rounded.Backpack
                    title.contains("MUTLAKA") -> Icons.Rounded.LocationOn
                    title.contains("DENEMEDEN") -> Icons.Rounded.Restaurant
                    title.contains("YEREL") -> Icons.Rounded.Coffee
                    else -> Icons.Rounded.TipsAndUpdates
                }

                val emoji = when {
                    title.contains("HAVA") -> "☁️ "
                    title.contains("VALİZ") -> "🧳 "
                    title.contains("MUTLAKA") -> "📍 "
                    title.contains("DENEMEDEN") -> "🍽 "
                    title.contains("YEREL") -> "☕ "
                    else -> "✨ "
                }

                AnalysisBlock(emoji + title, content, icon)
            }
        }
    }
}

@Composable
fun AnalysisBlock(title: String, content: String, icon: ImageVector) {
    val themeColors = HavamaniaTheme.colors
    Surface(
        color = themeColors.surface.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
                    color = themeColors.accent
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                    color = themeColors.textPrimary.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun TravelHowItWorksCard() {
    val themeColors = HavamaniaTheme.colors
    Surface(
        color = themeColors.accent.copy(alpha = 0.05f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Explore, null, tint = themeColors.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("🧭 Nasıl Başlanır?", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black), color = themeColors.accent)
            }
            Spacer(Modifier.height(12.dp))
            val steps = listOf(
                "Sağ üstteki + simgesine dokun",
                "Şehrini seç",
                "Tarihini belirle",
                "Seyahat tipini seç",
                "Kişisel önerilerini al"
            )
            steps.forEach { step ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = themeColors.accent.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(step, fontSize = 11.sp, color = themeColors.textPrimary.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
fun TravelEmptyState(filter: TravelFilter, onAdd: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            when(filter) {
                TravelFilter.UPCOMING -> Icons.Rounded.Explore
                TravelFilter.PAST -> Icons.Rounded.History
                TravelFilter.ARCHIVED -> Icons.Rounded.Archive
            },
            null,
            tint = themeColors.accent.copy(alpha = 0.2f),
            modifier = Modifier.size(100.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            when(filter) {
                TravelFilter.UPCOMING -> "🧭 İlk Seyahat Analizini Oluştur"
                TravelFilter.PAST -> "✈ Henüz tamamlanan seyahat yok."
                TravelFilter.ARCHIVED -> "📦 Henüz arşivlenmiş seyahat bulunmuyor."
            },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = themeColors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            when(filter) {
                TravelFilter.UPCOMING -> "Şehir seç, tarih belirle, seyahat tipini seç ve kişisel önerilerini hemen al."
                TravelFilter.PAST -> "Seyahatin bittiğinde analizlerin ve notların burada birikir."
                TravelFilter.ARCHIVED -> "Kütüphanene eklediğin seyahat kayıtlarına buradan ulaşabilirsin."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = themeColors.textMuted,
            textAlign = TextAlign.Center
        )
        if (filter == TravelFilter.UPCOMING) {
            Spacer(Modifier.height(32.dp))
            HavamaniaPrimaryButton(
                text = "Yeni Analiz Oluştur",
                onClick = onAdd,
                modifier = Modifier.width(240.dp).height(56.dp)
            )
        }
    }
}

@Composable
fun ActionIconButton(icon: ImageVector, text: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) { Icon(icon, null, tint = color) }
        Text(text, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HavamaniaFilterChip(selected: Boolean, onClick: () -> Unit, label: String) {
    val themeColors = HavamaniaTheme.colors
    val bgColor by animateColorAsState(if (selected) themeColors.accent else themeColors.surface.copy(alpha = 0.5f), label = "chipBg")
    val textColor by animateColorAsState(if (selected) themeColors.onAccent else themeColors.textSecondary, label = "chipText")

    Surface(
        onClick = onClick,
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = textColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTravelPlanDialog(
    viewModel: TravelViewModel,
    editPlan: TravelPlan? = null,
    onDismiss: () -> Unit,
    onSave: (TravelPlan) -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    val context = LocalContext.current

    val turkishCities = listOf(
        "Adana", "Adıyaman", "Afyonkarahisar", "Ağrı", "Amasya", "Ankara", "Antalya", "Artvin",
        "Aydın", "Balıkesir", "Bilecik", "Bingöl", "Bitlis", "Bolu", "Burdur", "Bursa", "Çanakkale",
        "Çankırı", "Çorum", "Denizli", "Diyarbakır", "Edirne", "Elazığ", "Erzincan", "Erzurum",
        "Eskişehir", "Gaziantep", "Giresun", "Gümüşhane", "Hakkari", "Hatay", "Isparta", "Mersin",
        "İstanbul", "İzmir", "Kars", "Kastamonu", "Kayseri", "Kırklareli", "Kırşehir", "Kocaeli",
        "Konya", "Kütahya", "Malatya", "Manisa", "Kahramanmaraş", "Mardin", "Muğla", "Muş",
        "Nevşehir", "Niğde", "Ordu", "Rize", "Sakarya", "Samsun", "Siirt", "Sinop", "Sivas",
        "Tekirdağ", "Tokat", "Trabzon", "Tunceli", "Şanlıurfa", "Uşak", "Van", "Yozgat", "Zonguldak",
        "Aksaray", "Bayburt", "Karaman", "Kırıkkale", "Batman", "Şırnak", "Bartın", "Ardahan",
        "Iğdır", "Yalova", "Karabük", "Kilis", "Osmaniye", "Düzce"
    ).sorted()

    var citySearch by remember { mutableStateOf(editPlan?.city ?: "") }
    var selectedCity by remember { mutableStateOf(editPlan?.city) }
    var isCityMenuExpanded by remember { mutableStateOf(false) }

    var tripType by remember { mutableStateOf(editPlan?.tripType ?: TripType.VACATION) }
    var startDate by remember { mutableStateOf(editPlan?.startDate ?: LocalDate.now()) }
    var endDate by remember { mutableStateOf(editPlan?.endDate ?: LocalDate.now().plusDays(3)) }

    val filteredCities = remember(citySearch) {
        if (citySearch.isBlank()) emptyList()
        else {
            val normalizedSearch = citySearch.lowercase(Locale("tr"))
                .replace('ı', 'i').replace('i', 'i').replace('ş', 's')
                .replace('ğ', 'g').replace('ü', 'u').replace('ö', 'o').replace('ç', 'c')

            turkishCities.filter {
                val normalizedCity = it.lowercase(Locale("tr"))
                    .replace('ı', 'i').replace('i', 'i').replace('ş', 's')
                    .replace('ğ', 'g').replace('ü', 'u').replace('ö', 'o').replace('ç', 'c')
                normalizedCity.contains(normalizedSearch)
            }
        }
    }

    val displayFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr"))

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HavamaniaGlassCard(
            modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp),
            alpha = 0.95f
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (editPlan != null) "Şehir Analizini Düzenle" else "Yeni Şehir Analizi Oluştur",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Şehir ve tarih seç; hava durumuna göre kişisel seyahat önerilerini hazırlayalım.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.textMuted
                )
                Spacer(Modifier.height(24.dp))

                Box {
                    OutlinedTextField(
                        value = citySearch,
                        onValueChange = {
                            citySearch = it
                            selectedCity = null
                            isCityMenuExpanded = true
                        },
                        label = { Text("Şehir ara veya seç") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (citySearch.isNotEmpty()) {
                                IconButton(onClick = { citySearch = ""; selectedCity = null }) {
                                    Icon(Icons.Rounded.Close, null)
                                }
                            }
                        }
                    )

                    if (isCityMenuExpanded && filteredCities.isNotEmpty() && selectedCity == null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp)
                                .heightIn(max = 200.dp)
                                .zIndex(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = themeColors.surface,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, themeColors.divider.copy(alpha = 0.2f))
                        ) {
                            LazyColumn {
                                items(filteredCities) { cityName ->
                                    Text(
                                        text = cityName,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedCity = cityName
                                                citySearch = cityName
                                                isCityMenuExpanded = false
                                            }
                                            .padding(16.dp),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = themeColors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Seyahat Tipi", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(TripType.values()) { type ->
                        HavamaniaFilterChip(
                            selected = tripType == type,
                            onClick = { tripType = type },
                            label = type.label
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Başlangıç", style = MaterialTheme.typography.labelSmall, color = themeColors.textMuted)
                        Surface(
                            onClick = {
                                val picker = android.app.DatePickerDialog(context, { _, y, m, d ->
                                    startDate = LocalDate.of(y, m + 1, d)
                                }, startDate.year, startDate.monthValue - 1, startDate.dayOfMonth)
                                picker.show()
                            },
                            color = Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(startDate.format(displayFormatter), modifier = Modifier.padding(vertical = 8.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bitiş", style = MaterialTheme.typography.labelSmall, color = themeColors.textMuted)
                        Surface(
                            onClick = {
                                val picker = android.app.DatePickerDialog(context, { _, y, m, d ->
                                    endDate = LocalDate.of(y, m + 1, d)
                                }, endDate.year, endDate.monthValue - 1, endDate.dayOfMonth)
                                picker.show()
                            },
                            color = Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(endDate.format(displayFormatter), modifier = Modifier.padding(vertical = 8.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("İptal") }
                    HavamaniaPrimaryButton(
                        text = "ANALİZ OLUŞTUR",
                        onClick = {
                            // Validations
                            if (selectedCity == null && !turkishCities.contains(citySearch.trim())) {
                                Toast.makeText(context, "Lütfen listeden bir şehir seçin.", Toast.LENGTH_SHORT).show()
                                return@HavamaniaPrimaryButton
                            }

                            val finalCity = selectedCity ?: citySearch.trim()

                            if (startDate.isAfter(endDate)) {
                                Toast.makeText(context, "Başlangıç tarihi bitiş tarihinden sonra olamaz.", Toast.LENGTH_SHORT).show()
                                return@HavamaniaPrimaryButton
                            }

                            val plan = (editPlan ?: TravelPlan(
                                city = finalCity,
                                tripType = tripType,
                                startDate = startDate,
                                endDate = endDate,
                                latitude = 0.0,
                                longitude = 0.0
                            )).copy(city = finalCity, tripType = tripType, startDate = startDate, endDate = endDate)
                            onSave(plan)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

