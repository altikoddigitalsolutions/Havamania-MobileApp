package com.havamania

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.*
import java.time.LocalDate
import androidx.lifecycle.compose.collectAsStateWithLifecycle

enum class TravelFilter { UPCOMING, PAST, ARCHIVED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelPlannerScreen(
    viewModel: TravelViewModel = viewModel(),
    initialCity: String? = null,
    initialTripType: String? = null,
    initialStartDate: String? = null,
    focusId: String? = null,
    highlight: String? = null,
    onViewRoute: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    val themeStyles = HavamaniaTheme.styles
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf(TravelFilter.UPCOMING) }
    var showAddDialog by remember { mutableStateOf(false) }
    var planToEdit by remember { mutableStateOf<TravelPlan?>(null) }
    var prefillPlan by remember { mutableStateOf<TravelPlan?>(null) }

    val responsive = LocalResponsiveValues.current
    val windowSize = LocalWindowSize.current
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val today by viewModel.today.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadPlans()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // UI Events (Snackbar)
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            if (event.startsWith("DUPLICATE_TRIP|")) {
                snackbarHostState.showSnackbar(event.substringAfter("|"))
            } else {
                snackbarHostState.showSnackbar(event)
            }
        }
    }

    var planForSummary by remember { mutableStateOf<TravelPlan?>(null) }
    var deleteConfirmPlan by remember { mutableStateOf<TravelPlan?>(null) }
    val showMigrationDialog = remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val currentUid = auth.currentUser?.uid ?: "legacy"

    val migrationChoiceMade by remember { ThemeManager.getMigrationChoiceMade(context, currentUid) }.collectAsState(initial = true)

    LaunchedEffect(currentUid, plans) {
        if (currentUid != "legacy" && !migrationChoiceMade) {
            val legacyPlans = WeatherDatabase.getDatabase(context).weatherDao().getAllTravelPlans("legacy")
            if (legacyPlans.isNotEmpty()) {
                showMigrationDialog.value = true
            }
        }
    }

    var selectedDate by remember { mutableStateOf(today) }

    LaunchedEffect(initialCity, initialTripType, initialStartDate) {
        if (initialCity != null) {
            val start = try {
                if (initialStartDate != null) LocalDate.parse(initialStartDate) else LocalDate.now()
            } catch(e: Exception) { LocalDate.now() }

            selectedDate = start

            val type = try {
                if (initialTripType != null) TripType.valueOf(initialTripType) else TripType.VACATION
            } catch(e: Exception) { TripType.VACATION }

            prefillPlan = TravelPlan(
                city = initialCity,
                tripType = type,
                startDate = start,
                endDate = start,
                latitude = 0.0,
                longitude = 0.0
            )
            showAddDialog = true
        }
    }

    LaunchedEffect(focusId, plans) {
        if (focusId != null && plans.isNotEmpty()) {
            val targetTrip = plans.find { it.id == focusId }
            if (targetTrip != null) {
                selectedDate = targetTrip.startDate
                val status = TravelStatusResolver.getStatus(targetTrip.startDate, targetTrip.endDate, today)
                selectedFilter = if (targetTrip.isArchived) TravelFilter.ARCHIVED
                                 else if (status == TravelStatus.PAST) TravelFilter.PAST
                                 else TravelFilter.UPCOMING
            }
        }
    }

    val upcomingPlans = remember(plans, today) {
        plans.filter { !it.isArchived && TravelStatusResolver.getStatus(it.startDate, it.endDate, today) == TravelStatus.UPCOMING }
            .sortedBy { it.startDate }
    }
    val ongoingPlans = remember(plans, today) {
        plans.filter { !it.isArchived && TravelStatusResolver.getStatus(it.startDate, it.endDate, today) == TravelStatus.ONGOING }
            .sortedBy { it.startDate }
    }
    val pastPlans = remember(plans, today) {
        plans.filter { !it.isArchived && TravelStatusResolver.getStatus(it.startDate, it.endDate, today) == TravelStatus.PAST }
            .sortedByDescending { it.startDate }
    }
    val archivedPlans = remember(plans) {
        plans.filter { it.isArchived }
            .sortedByDescending { it.updatedAt }
    }

    val tripDates = remember(plans) {
        plans.filter { !it.isArchived }.flatMap { plan ->
            val dates = mutableListOf<LocalDate>()
            var curr = plan.startDate
            while (!curr.isAfter(plan.endDate)) {
                dates.add(curr)
                curr = curr.plusDays(1)
            }
            dates
        }.toSet()
    }

    val filteredPlans = remember(selectedFilter, upcomingPlans, ongoingPlans, pastPlans, archivedPlans) {
        when (selectedFilter) {
            TravelFilter.UPCOMING -> (ongoingPlans + upcomingPlans).sortedBy { it.startDate }
            TravelFilter.PAST -> pastPlans
            TravelFilter.ARCHIVED -> archivedPlans
        }
    }

    val displayPlans = remember(plans, selectedDate, selectedFilter, pastPlans, ongoingPlans, upcomingPlans, archivedPlans) {
        if (selectedFilter == TravelFilter.UPCOMING) {
            // Include ALL non-archived plans that overlap with selectedDate
            plans.filter {
                !it.isArchived &&
                !it.startDate.isAfter(selectedDate) &&
                !it.endDate.isBefore(selectedDate)
            }.sortedBy { it.startDate }
        } else {
            when (selectedFilter) {
                TravelFilter.PAST -> pastPlans
                TravelFilter.ARCHIVED -> archivedPlans
                else -> emptyList()
            }
        }
    }

    val nextTrip = remember(upcomingPlans, ongoingPlans) {
        if (ongoingPlans.isNotEmpty()) ongoingPlans.first()
        else upcomingPlans.firstOrNull()
    }

    var expandedGuidePlan by remember { mutableStateOf<TravelPlan?>(null) }

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "Seyahat Planlayıcı",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(themeColors.accent.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = themeColors.accent)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .then(
                        if (windowSize.isTablet || windowSize.isLargeTablet)
                            Modifier.widthIn(max = responsive.maxContentWidth)
                        else Modifier.fillMaxWidth()
                    )
            ) {
                if (windowSize.isTablet || windowSize.isLargeTablet) {
                    // Tablet layout: Two columns for summary/calendar and trip list, with full-width rich guide below when expanded
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                if (BuildConfig.DEBUG) {
                                    android.util.Log.d("HAVAMANIA_TABLET_LAYOUT_DEBUG", "TABLET_ROOT_HEIGHT_DP=${coords.size.height / 3}")
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coords ->
                                    if (BuildConfig.DEBUG) {
                                        android.util.Log.d("HAVAMANIA_TABLET_LAYOUT_DEBUG", "TOP_ROW_HEIGHT_DP=${coords.size.height / 3}")
                                    }
                                },
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .onGloballyPositioned { coords ->
                                        if (BuildConfig.DEBUG) {
                                            android.util.Log.d("HAVAMANIA_TABLET_LAYOUT_DEBUG", "LEFT_COLUMN_HEIGHT_DP=${coords.size.height / 3}")
                                        }
                                    }
                            ) {
                                if (nextTrip != null) {
                                    NextTripHero(nextTrip, today, onViewRoute)
                                    Spacer(Modifier.height(themeStyles.spacingMD))
                                }

                                TravelCalendarStripe(
                                    selectedDate = selectedDate,
                                    onDateSelect = { selectedDate = it },
                                    tripDates = tripDates
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { coords ->
                                        if (BuildConfig.DEBUG) {
                                            android.util.Log.d("HAVAMANIA_TABLET_LAYOUT_DEBUG", "RIGHT_COLUMN_HEIGHT_DP=${coords.size.height / 3}")
                                        }
                                    }
                            ) {
                                TripListSection(
                                    filter = selectedFilter,
                                    plans = displayPlans,
                                    isLoading = isLoading && plans.isEmpty(),
                                    onFilterChange = { selectedFilter = it },
                                    selectedDate = selectedDate,
                                    onAddClick = { showAddDialog = true },
                                    onDelete = { deleteConfirmPlan = it },
                                    onEdit = { planToEdit = it; showAddDialog = true },
                                    onArchive = { viewModel.archiveTrip(it) },
                                    onUnarchive = { viewModel.unarchiveTrip(it) },
                                    onShowDetail = { planForSummary = it },
                                    onReanalyze = { viewModel.analyzeTravelWeather(it) },
                                    onViewRoute = onViewRoute,
                                    isOnline = isOnline,
                                    focusId = focusId,
                                    highlight = highlight,
                                    today = today,
                                    onToggleGuide = { plan ->
                                        expandedGuidePlan = if (expandedGuidePlan?.id == plan.id) null else plan
                                    }
                                )
                            }
                        }

                        if (expandedGuidePlan != null) {
                            Spacer(Modifier.height(themeStyles.spacingMD))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = responsive.pagePadding)
                                    .onGloballyPositioned { coords ->
                                        if (BuildConfig.DEBUG) {
                                            android.util.Log.d("HAVAMANIA_TABLET_LAYOUT_DEBUG", "RICH_GUIDE_Y_DP=${coords.positionInParent().y / 3}")
                                        }
                                    }
                            ) {
                                RichTravelGuideView(plan = expandedGuidePlan!!, today = today)
                            }
                        }
                    }
                } else {
                    // Phone layout: Single column
                    if (selectedFilter == TravelFilter.UPCOMING) {
                        if (nextTrip != null) {
                            NextTripHero(nextTrip, today, onViewRoute)
                            Spacer(Modifier.height(themeStyles.spacingLG))
                        }

                        TravelCalendarStripe(
                            selectedDate = selectedDate,
                            onDateSelect = { selectedDate = it },
                            tripDates = tripDates
                        )
                        Spacer(Modifier.height(themeStyles.spacingMD))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = responsive.pagePadding),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TravelFilter.entries.forEach { filter ->
                            HavamaniaChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = when (filter) {
                                    TravelFilter.UPCOMING -> "Yaklaşanlar"
                                    TravelFilter.PAST -> "Geçmiş"
                                    TravelFilter.ARCHIVED -> "Arşiv"
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(themeStyles.spacingMD))

                    TripListSection(
                        filter = selectedFilter,
                        plans = displayPlans,
                        isLoading = isLoading && plans.isEmpty(),
                        onFilterChange = { selectedFilter = it },
                        selectedDate = selectedDate,
                        onAddClick = { showAddDialog = true },
                        onDelete = { deleteConfirmPlan = it },
                        onEdit = { planToEdit = it; showAddDialog = true },
                        onArchive = { viewModel.archiveTrip(it) },
                        onUnarchive = { viewModel.unarchiveTrip(it) },
                        onShowDetail = { planForSummary = it },
                        onReanalyze = { viewModel.analyzeTravelWeather(it) },
                        onViewRoute = onViewRoute,
                        isOnline = isOnline,
                        focusId = focusId,
                        highlight = highlight,
                        today = today,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddTravelPlanDialog(
            viewModel = viewModel,
            editPlan = planToEdit ?: prefillPlan,
            onDismiss = {
                showAddDialog = false
                planToEdit = null
                prefillPlan = null
            },
            onSave = { plan ->
                viewModel.savePlan(plan)
                showAddDialog = false
                planToEdit = null
                prefillPlan = null
            }
        )
    }

    if (planForSummary != null) {
        planForSummary?.let { plan ->
            PastTravelDetailDialog(
                plan = plan,
                onDismiss = { planForSummary = null },
                onSaveNote = { note, rating ->
                    viewModel.updateTripNoteAndRating(plan.id, note, rating)
                }
            )
        }
    }

    if (deleteConfirmPlan != null) {
        deleteConfirmPlan?.let { plan ->
            HavamaniaDialog(
                onDismissRequest = { deleteConfirmPlan = null },
                title = "Seyahati Sil?",
                text = "${plan.city} seyahati kalıcı olarak silinecektir.",
                confirmText = "Sil",
                confirmColor = themeColors.error,
                icon = Icons.Rounded.DeleteForever,
                onConfirm = {
                    viewModel.deletePlan(plan.id)
                    deleteConfirmPlan = null
                }
            )
        }
    }

    if (showMigrationDialog.value) {
        HavamaniaDialog(
            onDismissRequest = { showMigrationDialog.value = false; viewModel.declineMigration() },
            title = "Verileri Aktar?",
            text = "Bu cihazdaki eski seyahat ve tercih verilerini hesabına aktarmak ister misin?",
            confirmText = "Hesabı Aktar",
            dismissText = "Aktarma",
            icon = Icons.Rounded.CloudUpload,
            onConfirm = {
                viewModel.migrateLegacyDataToUser()
                showMigrationDialog.value = false
            }
        )
    }
}
