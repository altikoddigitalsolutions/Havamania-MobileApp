package com.havamania

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
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

data class TravelSuggestions(
    val departureAdvice: String,
    val returnAdvice: String,
    val packingAdvice: String
)

data class WeatherSummary(
    val avgTemp: String,
    val condition: String,
    val iconName: String
)

data class TravelPlan(
    val id: String = UUID.randomUUID().toString(),
    val destination: String,
    val startDate: Long,
    val endDate: Long,
    val type: TripType,
    val weather: WeatherSummary? = null,
    val suggestions: TravelSuggestions? = null,
    val isAnalyzing: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelPlannerScreen(onBack: () -> Unit) {
    var plans by remember { mutableStateOf(listOf<TravelPlan>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var planToEdit by remember { mutableStateOf<TravelPlan?>(null) }

    val colorScheme = MaterialTheme.colorScheme

    // Initial Data
    LaunchedEffect(Unit) {
        if (plans.isEmpty()) {
            plans = listOf(
                TravelPlan(
                    destination = "Paris",
                    startDate = System.currentTimeMillis() + 86400000 * 5,
                    endDate = System.currentTimeMillis() + 86400000 * 10,
                    type = TripType.VACATION,
                    weather = WeatherSummary("18°C", "Parçalı Bulutlu", "Cloudy"),
                    suggestions = TravelSuggestions(
                        "Gidiş günü hafif yağmur geçişleri olabilir, tren istasyonuna erken git.",
                        "Dönüşte hava açıyor, uçuştan önce şehirde son bir tur atabilirsin.",
                        "Katmanlı giyin, yanına mutlaka şık bir ceket ve rahat yürüyüş ayakkabısı al."
                    )
                )
            )
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Seyahatlerim", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black))
                        Text("Akıllı hava durumu asistanın ile planla", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null) }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Yeni Rota", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(plans, key = { it.id }) { plan ->
                    TravelPlanCard(
                        plan = plan,
                        onDelete = { plans = plans.filter { it.id != plan.id } },
                        onEdit = { planToEdit = plan; showAddDialog = true },
                        onReanalyze = {
                            // Mock re-analysis
                            plans = plans.map {
                                if (it.id == plan.id) it.copy(isAnalyzing = true) else it
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            if (plans.isEmpty()) {
                EmptyTravelState()
            }
        }
    }

    if (showAddDialog) {
        AddTravelPlanDialog(
            initialPlan = planToEdit,
            onDismiss = { showAddDialog = false; planToEdit = null },
            onConfirm = { newPlan ->
                plans = if (planToEdit != null) {
                    plans.map { if (it.id == planToEdit?.id) newPlan else it }
                } else {
                    plans + newPlan
                }
                showAddDialog = false
                planToEdit = null
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
    val colorScheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("d MMM", Locale("tr"))
    val dateRange = "${dateFormat.format(Date(plan.startDate))} - ${dateFormat.format(Date(plan.endDate))}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = colorScheme.primary)
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Brush.linearGradient(listOf(colorScheme.primary, colorScheme.secondary)),
                            RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(plan.type.icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        plan.destination,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(dateRange, style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
                }

                // Weather Summary
                plan.weather?.let {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(it.avgTemp, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                        Text(it.condition, style = MaterialTheme.typography.labelSmall, color = colorScheme.primary)
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(Modifier.height(16.dp))

                    // AI Recommendations Section
                    RecommendationSection(plan.suggestions)

                    Spacer(Modifier.height(20.dp))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = colorScheme.error)) {
                            Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Sil")
                        }
                        TextButton(onClick = onEdit) {
                            Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Düzenle")
                        }
                        FilledTonalButton(
                            onClick = onReanalyze,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Yeniden Analiz")
                        }
                    }
                }
            }

            if (!expanded) {
                Icon(
                    Icons.Rounded.ExpandMore,
                    null,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun RecommendationSection(suggestions: TravelSuggestions?) {
    val colorScheme = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("HAVAMANIA AI ÖNERİLERİ", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp))
        }

        suggestions?.let {
            AdviceItem(Icons.Rounded.FlightTakeoff, "Gidiş Planı", it.departureAdvice)
            AdviceItem(Icons.Rounded.FlightLand, "Dönüş Planı", it.returnAdvice)
            AdviceItem(Icons.Rounded.Checklist, "Valiz Önerisi", it.packingAdvice)
        } ?: Text("Hava durumu verileri analiz ediliyor...", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun AdviceItem(icon: ImageVector, title: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, modifier = Modifier.size(18.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTravelPlanDialog(
    initialPlan: TravelPlan?,
    onDismiss: () -> Unit,
    onConfirm: (TravelPlan) -> Unit
) {
    var city by remember { mutableStateOf(initialPlan?.destination ?: "") }
    var selectedType by remember { mutableStateOf(initialPlan?.type ?: TripType.VACATION) }

    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialPlan?.startDate,
        initialSelectedEndDateMillis = initialPlan?.endDate
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Toolbar
                CenterAlignedTopAppBar(
                    title = { Text(if (initialPlan == null) "Yeni Seyahat" else "Planı Düzenle") },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, null) } },
                    actions = {
                        TextButton(
                            onClick = {
                                if (city.isNotBlank() && dateRangePickerState.selectedEndDateMillis != null) {
                                    onConfirm(
                                        TravelPlan(
                                            id = initialPlan?.id ?: UUID.randomUUID().toString(),
                                            destination = city,
                                            startDate = dateRangePickerState.selectedStartDateMillis!!,
                                            endDate = dateRangePickerState.selectedEndDateMillis!!,
                                            type = selectedType,
                                            // Simulated AI logic
                                            weather = WeatherSummary("22°C", "Güneşli", "Sunny"),
                                            suggestions = TravelSuggestions(
                                                "Hava mükemmel, yanına güneş gözlüğünü almayı unutma.",
                                                "Dönüşte hafif bir rüzgar olabilir.",
                                                "T-shirt ve şort ağırlıklı bir valiz hazırla."
                                            )
                                        )
                                    )
                                }
                            }
                        ) {
                            Text("Kaydet", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                )

                Column(
                    modifier = Modifier.padding(horizontal = 24.dp).verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Nereye gidiyorsun?") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Rounded.LocationOn, null) }
                    )

                    Spacer(Modifier.height(20.dp))

                    Text("Seyahat Tipi", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TripType.values().forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.label) },
                                leadingIcon = if (selectedType == type) {
                                    { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text("Tarih Aralığı Seç", style = MaterialTheme.typography.titleSmall)
                    DateRangePicker(
                        state = dateRangePickerState,
                        modifier = Modifier.weight(1f).heightIn(max = 500.dp),
                        title = null,
                        headline = null,
                        showModeToggle = false
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTravelState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Map,
            null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
        Spacer(Modifier.height(24.dp))
        Text("Henüz bir planın yok", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Text("Yeni bir rota ekleyerek başla", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
