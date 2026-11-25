@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.havamania

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.havamania.ui.theme.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToCities: () -> Unit = {},
    onNavigateToAiHistory: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToTravels: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToPersonalization: () -> Unit = {},
    profileViewModel: ProfileViewModel = viewModel(),
    aiHistoryViewModel: AiHistoryViewModel = viewModel(),
    travelViewModel: TravelViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val themeColors = HavamaniaTheme.colors

    val profileState by profileViewModel.profileState.collectAsStateWithLifecycle()
    val uploadProgress by profileViewModel.uploadProgress.collectAsStateWithLifecycle()

    val aiHistoryItems by aiHistoryViewModel.historyItems.collectAsStateWithLifecycle()
    val travelPlans by travelViewModel.plans.collectAsStateWithLifecycle()
    val emptyTripsOwner by travelViewModel.confirmedEmptyOwner.collectAsStateWithLifecycle()
    val emptyHistoryOwner by aiHistoryViewModel.confirmedEmptyOwner.collectAsStateWithLifecycle()

    val owner by profileViewModel.activeUid.collectAsStateWithLifecycle()
    var profileRetryAttempt by remember(owner) { mutableStateOf(0) }
    val retryProfile = { profileRetryAttempt++; profileViewModel.refresh() }
    var showComingSoonDialog by remember(owner) { mutableStateOf(false) }
    var comingSoonTitle by remember(owner) { mutableStateOf("") }

    var showAboutMeSheet by remember(owner) { mutableStateOf(false) }
    val aboutMeSaveState by profileViewModel.aboutMeSaveState.collectAsStateWithLifecycle()
    val savingAboutMe = aboutMeSaveState == ProfileSaveState.Saving
    LaunchedEffect(aboutMeSaveState) {
        if (aboutMeSaveState == ProfileSaveState.Saved) {
            showAboutMeSheet = false
            profileViewModel.resetAboutMeSave()
        }
    }
    var showStatsDetail by remember(owner) { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                profileViewModel.uploadProfileImage(uri)
            }
        }
    )

    val responsive = LocalResponsiveValues.current
    val windowSize = LocalWindowSize.current

    // Keep identity, biography and personality on the same account snapshot.
    val profile = (profileState as? ProfileState.Success)?.profile?.takeIf { it.uid == owner }
    val userInterests = profile?.personalizationProfile?.selectedInterests?.toSet().orEmpty()
    val aboutMe = profile?.aboutMe.orEmpty()

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "PROFİL",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Rounded.Settings, "Ayarlar", tint = themeColors.textPrimary)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (profile == null) {
            ProfileUnavailableContent(owner, profileState, profileRetryAttempt, retryProfile, onBack,
                modifier = Modifier.padding(paddingValues))
        } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .then(
                        if (windowSize.isTablet || windowSize.isLargeTablet)
                            Modifier.widthIn(max = responsive.maxContentWidth)
                        else Modifier.fillMaxWidth()
                    )
            ) {
            Spacer(modifier = Modifier.height(HavamaniaTheme.styles.spacingMD))

            PremiumProfileHeader(
                name = profile.name,
                bio = profile.bio,
                imageUri = profile.photoURL,
                avatarVersion = profile.updatedAt,
                isUploading = uploadProgress,
                interests = userInterests,
                aboutMe = aboutMe,
                stats = mapOf(
                    "İlgi" to userInterests.size.toString(),
                    "Rota" to profileCountLabel(travelPlans.count { !it.isDemo }, owner != null && emptyTripsOwner == owner),
                    "Analiz" to profileCountLabel(aiHistoryItems.size, owner != null && emptyHistoryOwner == owner)
                ),
                onAvatarClick = {
                    if (!uploadProgress) photoPickerLauncher.launch("image/*")
                },
                onStatClick = { showStatsDetail = it }
            )

            Spacer(modifier = Modifier.height(HavamaniaTheme.styles.spacingLG))

            Column(modifier = Modifier.padding(horizontal = HavamaniaTheme.styles.pagePadding)) {
                SectionHeader("HIZLI İŞLEMLER")
                QuickActionsGrid(
                    onManageCities = onNavigateToCities,
                    onAiHistory = onNavigateToAiHistory,
                    onMyTravels = onNavigateToTravels,
                    onPersonalization = onNavigateToPersonalization,
                    onEditProfile = onNavigateToEditProfile,
                    onPremium = {
                        comingSoonTitle = "Havamania Premium yakında hizmetinizde olacak."
                        showComingSoonDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(HavamaniaTheme.styles.spacingLG))

                SectionHeader("KİŞİSEL ANALİZ")
                PremiumAboutMeCard(aboutMe, interests = userInterests) { showAboutMeSheet = true }

                Spacer(modifier = Modifier.height(HavamaniaTheme.styles.spacingLG))

                SectionHeader("HAVA TERCİHLERİ")
                PremiumInterestsSection(userInterests) { profileViewModel.setInterestSelected(it, !userInterests.contains(it)) }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

        }
        if (showAboutMeSheet) {
            DisposableEffect(profileViewModel) { onDispose { profileViewModel.resetAboutMeSave() } }
            val sheetState = rememberModalBottomSheetState(confirmValueChange = { !savingAboutMe || it != SheetValue.Hidden })
            ModalBottomSheet(
                sheetState = sheetState,
                onDismissRequest = { if (!savingAboutMe) showAboutMeSheet = false },
                containerColor = themeColors.surface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = themeColors.textPrimary.copy(0.2f)) }
            ) {
                PremiumAboutMeContent(aboutMe, isSaving = savingAboutMe,
                    errorMessage = (aboutMeSaveState as? ProfileSaveState.Failed)?.message) {
                    profileViewModel.setUserAboutMe(it)
                }
            }
        }

        if (showStatsDetail != null) {
            ModalBottomSheet(
                onDismissRequest = { showStatsDetail = null },
                containerColor = themeColors.surface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = themeColors.textPrimary.copy(0.2f)) }
            ) {
                StatsDetailContent(showStatsDetail!!, userInterests, travelPlans, aiHistoryItems)
            }
        }

        if (showComingSoonDialog) {
            HavamaniaDialog(
                onDismissRequest = { showComingSoonDialog = false },
                title = "YAKINDA",
                text = comingSoonTitle,
                confirmText = "TAMAM",
                onConfirm = { showComingSoonDialog = false }
            )
        }

        ProfileLoadErrorDialog((profileState as? ProfileState.Error)?.message, owner, retryProfile, profileViewModel::dismissProfileError)
    }
}

@Composable
fun PremiumAboutMeCard(text: String, interests: Set<String> = emptySet(), onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    val hasContent = text.isNotBlank()

    Surface(
        onClick = onClick,
        color = themeColors.surface.copy(alpha = if (themeColors.isDark) 0.5f else 0.7f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, themeColors.border.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = themeColors.accent, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Kendinden bahset", style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
                    Text("Öneriler için kendini tanıt.", style = HavamaniaTheme.typography.bodySmall, color = themeColors.textSecondary)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = themeColors.textMuted)
            }

            if (hasContent) {
                Spacer(Modifier.height(16.dp))
                Text(text = text, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp), color = themeColors.textPrimary.copy(alpha = 0.9f), maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(16.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val badges = remember(text, interests) { generateBadges(text, interests) }
                    badges.forEach { PremiumBadge(it) }
                }
            } else {
                Spacer(Modifier.height(16.dp))
                Text("Henüz bir bilgi eklemedin. Öneriler için profilini tamamla.", style = MaterialTheme.typography.bodySmall, color = themeColors.textSecondary)
            }
        }
    }
}

@Composable
fun PremiumBadge(text: String) {
    val themeColors = HavamaniaTheme.colors
    Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(themeColors.accent))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = themeColors.textSecondary)
    }
}

@Composable
fun StatsDetailContent(type: String, userInterests: Set<String>, travelPlans: List<TravelPlan>, aiHistory: List<AiHistorySummary>) {
    val themeColors = HavamaniaTheme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).heightIn(max = 500.dp)) {
        val title = when(type) {
            "İlgi" -> "Seçili Hava Tercihlerin"
            "Rota" -> "Kayıtlı Rotaların"
            else -> "Kaydedilen Analizlerin"
        }
        Text(text = title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
        Spacer(Modifier.height(20.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f, false)) {
            when(type) {
                "İlgi" -> {
                    val selectedItems = InterestsData.categories.flatMap { cat -> cat.interests.filter { userInterests.contains(it.id) }.map { cat.title to it } }
                    if (selectedItems.isEmpty()) { item { Text("Henüz bir tercih seçmedin.", color = themeColors.textMuted) } }
                    else { items(selectedItems) { (category, item) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(item.icon, null, tint = themeColors.accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(item.label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = themeColors.textPrimary)
                                Text(category, style = MaterialTheme.typography.bodySmall, color = themeColors.textSecondary)
                            }
                        }
                    }}
                }
                "Rota" -> {
                    val userPlans = travelPlans.filter { !it.isDemo }
                    if (userPlans.isEmpty()) { item { Text("Henüz kendi rotanı oluşturmadın.", color = themeColors.textMuted) } }
                    else { items(userPlans) { plan ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Route, null, tint = themeColors.accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(plan.city, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = themeColors.textPrimary)
                                Text("${plan.startDate} - ${plan.endDate}", style = MaterialTheme.typography.bodySmall, color = themeColors.textSecondary)
                            }
                        }
                    }}
                }
                "Analiz" -> {
                    if (aiHistory.isEmpty()) { item { Text("Henüz AI analizi kaydetmedin.", color = themeColors.textMuted) } }
                    else { items(aiHistory) { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AutoAwesome, null, tint = themeColors.accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(item.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = themeColors.textPrimary)
                                Text(item.summary, style = MaterialTheme.typography.bodySmall, color = themeColors.textSecondary)
                            }
                        }
                    }}
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun PremiumInterestsSection(selectedInterests: Set<String>, onInterestToggle: (String) -> Unit) {
    val prioritized = InterestsData.categories.filter { listOf("Hava & Atmosfer", "Ulaşım", "Outdoor").contains(it.title) }
    val others = InterestsData.categories.filter { !listOf("Hava & Atmosfer", "Ulaşım", "Outdoor").contains(it.title) }
    var showOthers by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        prioritized.forEach { PremiumInterestCategoryCard(it, selectedInterests, onInterestToggle) }
        if (!showOthers) {
            TextButton(onClick = { showOthers = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Daha fazla göster", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp), color = HavamaniaTheme.colors.accent)
            }
        } else {
            others.forEach { PremiumInterestCategoryCard(it, selectedInterests, onInterestToggle) }
        }
    }
}

@Composable
fun PremiumInterestCategoryCard(category: InterestCategory, selectedInterests: Set<String>, onInterestToggle: (String) -> Unit) {
    val themeColors = HavamaniaTheme.colors
    var expanded by rememberSaveable(category.title) { mutableStateOf(false) }
    val selectedCount = remember(selectedInterests) { category.interests.count { selectedInterests.contains(it.id) } }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = themeColors.surface.copy(alpha = if (themeColors.isDark) 0.5f else 0.7f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, themeColors.border.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                stateDescription = if (expanded) "Açık" else "Kapalı"
            }.clickable(role = Role.Button) { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    Icon(category.icon, null, tint = themeColors.accent, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.title, style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
                    Text(category.description, style = HavamaniaTheme.typography.bodySmall.copy(fontSize = 11.sp), color = themeColors.textSecondary)
                    if (selectedCount > 0) Text("$selectedCount seçili", style = MaterialTheme.typography.labelSmall, color = themeColors.textSecondary)
                }
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = themeColors.textMuted)
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        category.interests.forEach { PremiumInterestChip(it, selectedInterests.contains(it.id)) { onInterestToggle(it.id) } }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumInterestChip(item: InterestItem, isSelected: Boolean, onClick: () -> Unit) {
    val colors = HavamaniaTheme.colors
    val palette = themeButtonPalette(listOf(colors.accent), colors.onAccent)
    Surface(
        color = if (isSelected) palette.stops.first() else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.widthIn(max = 280.dp).heightIn(min = 48.dp).toggleable(value = isSelected,
            role = Role.Checkbox, onValueChange = { onClick() })
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(item.icon, null, tint = if (isSelected) palette.content else colors.accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(item.label, modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) palette.content else colors.textPrimary)
        }
    }
}

@Composable
fun PremiumAboutMeContent(initialText: String, isSaving: Boolean = false, errorMessage: String? = null, onSave: (String) -> Unit) {
    val themeColors = HavamaniaTheme.colors
    var text by rememberSaveable { mutableStateOf(initialText) }
    val sampleCards = listOf(
        AboutMeSample("Outdoor", "Hafta sonları doğa yürüyüşü ve kamp yapmayı seviyorum.", Icons.Rounded.Terrain),
        AboutMeSample("Aile", "Çocuklarım için hava durumunu takip ediyorum.", Icons.Rounded.ChildCare),
        AboutMeSample("Seyahat", "İş için sık seyahat ediyorum.", Icons.Rounded.Flight),
        AboutMeSample("Macera", "Kar tutkunuyum. Kışın snowboard yapmayı seviyorum.", Icons.Rounded.Snowboarding)
    )

    Column(modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("HAVA KİMLİĞİ", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp), color = themeColors.accent)
        Text("Kendinden Bahset", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
        Spacer(Modifier.height(32.dp))
        Text("ÖRNEK SENARYOLAR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = themeColors.textMuted)
        Spacer(Modifier.height(16.dp))
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(end = 40.dp)) {
            items(sampleCards) { sample ->
                PremiumSampleCard(sample = sample, enabled = !isSaving) { text = sample.text }
            }
        }
        Spacer(Modifier.height(32.dp))
        Surface(color = themeColors.surfaceGlass.copy(alpha = 0.3f), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, themeColors.border.copy(alpha = 0.1f))) {
            TextField(value = text, onValueChange = { text = it }, enabled = !isSaving, label = { Text("Hakkımda") }, minLines = 4, maxLines = 8, modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledIndicatorColor = Color.Transparent, disabledTextColor = themeColors.textSecondary, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = themeColors.accent, focusedTextColor = themeColors.textPrimary, unfocusedTextColor = themeColors.textPrimary))
        }
        Spacer(Modifier.height(32.dp))
        HavamaniaPrimaryButton(text = "PROFİLİ GÜNCELLE", onClick = { onSave(text) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), isLoading = isSaving, enabled = text.isNotBlank() && !isSaving)
        errorMessage?.let { Text(it, color = themeColors.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    }
}

@Composable
fun PremiumSampleCard(sample: AboutMeSample, enabled: Boolean = true, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    HavamaniaCard(onClick = if (enabled) onClick else null, elevation = 0.dp,
        backgroundColor = themeColors.surface, padding = 16.dp,
        modifier = Modifier.width(220.dp).semantics { if (!enabled) disabled() }) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(sample.icon, null, tint = themeColors.accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(sample.label, modifier = Modifier.weight(1f), style = HavamaniaTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
            }
            Spacer(Modifier.height(12.dp))
            Text(sample.text, style = HavamaniaTheme.typography.bodySmall.copy(lineHeight = 16.sp), color = themeColors.textSecondary)
        }
    }
}

data class AboutMeSample(val label: String, val text: String, val icon: ImageVector)
