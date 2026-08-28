@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

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
    themeViewModel: ThemeViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel(),
    aiHistoryViewModel: AiHistoryViewModel = viewModel(),
    travelViewModel: TravelViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val themeColors = HavamaniaTheme.colors

    val profileState by profileViewModel.profileState.collectAsState()
    val uploadProgress by profileViewModel.uploadProgress.collectAsState()
    val avatarVersion by profileViewModel.avatarVersion.collectAsState()

    val name by themeViewModel.userName.collectAsState()
    val bio by themeViewModel.userBio.collectAsState()
    val userInterests by themeViewModel.userInterests.collectAsState()
    val aboutMe by themeViewModel.userAboutMe.collectAsState()

    val aiHistoryItems by aiHistoryViewModel.historyItems.collectAsState()
    val travelPlans by travelViewModel.plans.collectAsState()

    var showComingSoonDialog by remember { mutableStateOf(false) }
    var comingSoonTitle by remember { mutableStateOf("") }
    var showAboutMeSheet by remember { mutableStateOf(false) }
    var showStatsDetail by remember { mutableStateOf<String?>(null) }

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

    val profile = (profileState as? ProfileState.Success)?.profile
    val localImageUri by themeViewModel.userImageUri.collectAsState()

    val displayNameToDisplay = profile?.name ?: name
    val bioToDisplay = profile?.bio ?: bio
    val photoToDisplay = profile?.photoURL ?: localImageUri

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "PROFIL",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Rounded.Settings, null, tint = themeColors.textPrimary)
                    }
                }
            )
        }
    ) { paddingValues ->
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
                name = displayNameToDisplay,
                bio = bioToDisplay,
                imageUri = photoToDisplay,
                avatarVersion = avatarVersion,
                isUploading = uploadProgress,
                interests = userInterests,
                aboutMe = aboutMe,
                stats = mapOf(
                    "İlgi" to userInterests.size.toString(),
                    "Rota" to travelPlans.count { !it.isDemo }.toString(),
                    "Analiz" to aiHistoryItems.size.toString()
                ),
                onAvatarClick = {
                    if (!uploadProgress) photoPickerLauncher.launch("image/*")
                },
                onEditClick = onNavigateToEditProfile,
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
                PremiumAboutMeCard(aboutMe) { showAboutMeSheet = true }

                Spacer(modifier = Modifier.height(HavamaniaTheme.styles.spacingLG))

                SectionHeader("HAVA TERCİHLERİ")
                PremiumInterestsSection(userInterests) { profileViewModel.toggleInterest(it, userInterests) }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

        if (showAboutMeSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAboutMeSheet = false },
                containerColor = themeColors.surface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = themeColors.textPrimary.copy(0.2f)) }
            ) {
                PremiumAboutMeContent(aboutMe) {
                    profileViewModel.setUserAboutMe(it)
                    showAboutMeSheet = false
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

        if (profileState is ProfileState.Error) {
            val errorMsg = (profileState as ProfileState.Error).message
            HavamaniaDialog(
                onDismissRequest = { profileViewModel.refresh() },
                title = "YÜKLEME HATASI",
                text = "$errorMsg\n\nLütfen internet bağlantınızı kontrol edin.",
                confirmText = "TAMAM",
                onConfirm = { profileViewModel.refresh() }
            )
        }
    }
}

@Composable
fun PremiumAboutMeCard(text: String, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    val hasContent = text.isNotBlank()

    HavamaniaGlassCard(onClick = onClick, alpha = 0.6f, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(HavamaniaTheme.styles.radiusSmall)).background(themeColors.accent.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = themeColors.accent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Kendinden Bahset", style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
                        Text("AI seni tanısın, önerileri özelleştirilsin.", style = HavamaniaTheme.typography.bodySmall, color = themeColors.textSecondary)
                    }
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = themeColors.textMuted)
            }

            if (hasContent) {
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(themeColors.surfaceGlass.copy(alpha = 0.3f)).padding(16.dp)) {
                    Text(text = text, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp), color = themeColors.textPrimary.copy(alpha = 0.9f), maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(16.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val badges = remember(text) { generateBadges(text) }
                    badges.forEach { PremiumBadge(it) }
                }
            } else {
                Spacer(Modifier.height(16.dp))
                Text("Henüz bir bilgi eklemedin. AI önerileri için profilini tamamla.", style = MaterialTheme.typography.bodySmall, color = themeColors.textMuted)
            }
        }
    }
}

@Composable
fun PremiumBadge(text: String) {
    val themeColors = HavamaniaTheme.colors
    Surface(color = themeColors.surfaceGlass, shape = CircleShape, border = BorderStroke(1.dp, themeColors.border.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(themeColors.accent))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = themeColors.textPrimary)
        }
    }
}

fun generateBadges(text: String): List<String> {
    val badges = mutableListOf<String>()
    val t = text.lowercase()
    if (t.contains("kamp") || t.contains("doğa") || t.contains("yürüyüş")) badges.add("Doğa Dostu")
    if (t.contains("çocuk") || t.contains("aile")) badges.add("Aile Odaklı")
    if (t.contains("seyahat") || t.contains("rota")) badges.add("Aktif Gezgin")
    if (t.contains("bisiklet") || t.contains("spor") || t.contains("koşu")) badges.add("Sporcu")
    if (t.contains("yağmur") || t.contains("kar") || t.contains("kış")) badges.add("Kar Tutkunu")
    if (t.contains("drone") || t.contains("pilot")) badges.add("Hava Meraklısı")
    if (t.contains("şehir") || t.contains("fotoğraf")) badges.add("Şehir Kaşifi")
    if (badges.isEmpty() && text.isNotBlank()) badges.add("Hava Analisti")
    return badges.take(3)
}

@Composable
fun StatsDetailContent(type: String, userInterests: Set<String>, travelPlans: List<TravelPlan>, aiHistory: List<AiHistoryEntity>) {
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
                                Text(item.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = themeColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.summary, style = MaterialTheme.typography.bodySmall, color = themeColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    val prioritized = InterestsData.categories.filter { listOf("Hava & Atmosfer", "Ulaşım & Yol", "Outdoor & Macera").contains(it.title) }
    val others = InterestsData.categories.filter { !listOf("Hava & Atmosfer", "Ulaşım & Yol", "Outdoor & Macera").contains(it.title) }
    var showOthers by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        prioritized.forEach { PremiumInterestCategoryCard(it, selectedInterests, onInterestToggle) }
        if (!showOthers) {
            TextButton(onClick = { showOthers = true }, modifier = Modifier.fillMaxWidth()) {
                Text("DAHA FAZLA GÖSTER", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp), color = HavamaniaTheme.colors.accent)
            }
        } else {
            others.forEach { PremiumInterestCategoryCard(it, selectedInterests, onInterestToggle) }
        }
    }
}

@Composable
fun PremiumInterestCategoryCard(category: InterestCategory, selectedInterests: Set<String>, onInterestToggle: (String) -> Unit) {
    val themeColors = HavamaniaTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val selectedCount = remember(selectedInterests) { category.interests.count { selectedInterests.contains(it.id) } }

    HavamaniaGlassCard(alpha = 0.45f, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(HavamaniaTheme.styles.radiusSmall)).background(themeColors.accent.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                    Icon(category.icon, null, tint = themeColors.accent, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.title, style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
                    Text(category.description, style = HavamaniaTheme.typography.bodySmall.copy(fontSize = 11.sp), color = themeColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (selectedCount > 0) {
                    Surface(color = themeColors.accent, shape = CircleShape) {
                        Text(selectedCount.toString(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = themeColors.textMuted)
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(20.dp))
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
    val themeColors = HavamaniaTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "chip_scale")

    Surface(
        onClick = onClick, interactionSource = interactionSource,
        color = if (isSelected) themeColors.accent else themeColors.surfaceGlass.copy(alpha = 0.5f),
        shape = CircleShape,
        border = BorderStroke(1.dp, if (isSelected) themeColors.accent else themeColors.border.copy(alpha = 0.1f)),
        modifier = Modifier.scale(scale)
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(item.icon, null, tint = if (isSelected) Color.White else themeColors.accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(item.label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = if (isSelected) Color.White else themeColors.textPrimary)
        }
    }
}

@Composable
fun PremiumAboutMeContent(initialText: String, onSave: (String) -> Unit) {
    val themeColors = HavamaniaTheme.colors
    var text by remember { mutableStateOf(initialText) }
    val sampleCards = listOf(
        AboutMeSample("Outdoor", "Hafta sonları doğa yürüyüşü ve kamp yapmayı seviyorum.", Icons.Rounded.Terrain),
        AboutMeSample("Aile", "Çocuklarım için hava durumunu takip ediyorum.", Icons.Rounded.ChildCare),
        AboutMeSample("Seyahat", "İş için sık seyahat ediyorum.", Icons.Rounded.Flight),
        AboutMeSample("Macera", "Kar tutkunuyum. Kışın snowboard yapmayı seviyorum.", Icons.Rounded.Snowboarding)
    )

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(8.dp))
        Text("HAVA KİMLİĞİ", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp), color = themeColors.accent)
        Text("Kendinden Bahset", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
        Spacer(Modifier.height(32.dp))
        Text("ÖRNEK SENARYOLAR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = themeColors.textMuted)
        Spacer(Modifier.height(16.dp))
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(end = 40.dp)) {
            items(sampleCards) { sample ->
                PremiumSampleCard(sample = sample) { text = sample.text }
            }
        }
        Spacer(Modifier.height(32.dp))
        Surface(color = themeColors.surfaceGlass.copy(alpha = 0.3f), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, themeColors.border.copy(alpha = 0.1f))) {
            TextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth().height(180.dp), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = themeColors.accent, focusedTextColor = themeColors.textPrimary, unfocusedTextColor = themeColors.textPrimary))
        }
        Spacer(Modifier.height(32.dp))
        HavamaniaPrimaryButton(text = "PROFİLİ GÜNCELLE", onClick = { onSave(text) }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = text.isNotBlank())
    }
}

@Composable
fun PremiumSampleCard(sample: AboutMeSample, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    HavamaniaGlassCard(onClick = onClick, alpha = 0.4f, modifier = Modifier.width(220.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(themeColors.accent.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(sample.icon, null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) }
                Spacer(Modifier.width(10.dp))
                Text(sample.label, style = HavamaniaTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
            }
            Spacer(Modifier.height(12.dp))
            Text(sample.text, style = HavamaniaTheme.typography.bodySmall.copy(lineHeight = 16.sp), color = themeColors.textSecondary, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
    }
}

data class AboutMeSample(val label: String, val text: String, val icon: ImageVector)
