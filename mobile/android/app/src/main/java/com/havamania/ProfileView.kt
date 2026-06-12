@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.havamania

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.havamania.ui.theme.*

@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToCities: () -> Unit = {},
    onNavigateToAiHistory: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToTravels: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    themeViewModel: ThemeViewModel = viewModel(),
    aiHistoryViewModel: AiHistoryViewModel = viewModel(),
    travelViewModel: TravelViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val themeColors = HavamaniaTheme.colors

    val name by themeViewModel.userName.collectAsState()
    val bio by themeViewModel.userBio.collectAsState()
    val imageUri by themeViewModel.userImageUri.collectAsState()
    val userInterests by themeViewModel.userInterests.collectAsState()
    val registeredCities by themeViewModel.registeredCities.collectAsState()
    val defaultCity by themeViewModel.defaultCity.collectAsState()
    val tempUnit by themeViewModel.tempUnit.collectAsState()
    val notificationsEnabled by themeViewModel.notificationsEnabled.collectAsState()
    val currentTheme by themeViewModel.currentTheme.collectAsState()
    val aboutMe by themeViewModel.userAboutMe.collectAsState()

    val aiHistoryItems by aiHistoryViewModel.historyItems.collectAsState()
    val travelPlans by travelViewModel.plans.collectAsState()

    var showComingSoonDialog by remember { mutableStateOf(false) }
    var comingSoonTitle by remember { mutableStateOf("") }
    var showAboutMeSheet by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { themeViewModel.setUserImageUri(it.toString()) }
        }
    )

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "HAVA KİMLİĞİ",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Rounded.Settings, null, tint = themeColors.textPrimary)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Premium Profile Header
            PremiumProfileHeader(
                name = name,
                bio = bio,
                imageUri = imageUri,
                interests = userInterests,
                aboutMe = aboutMe,
                stats = mapOf(
                    "İlgi" to userInterests.size.toString(),
                    "Rota" to travelPlans.size.toString(),
                    "Analiz" to aiHistoryItems.size.toString(),
                    "Favori" to (travelPlans.groupBy { it.tripType }.maxByOrNull { it.value.size }?.key?.label ?: "Outdoor")
                ),
                onAvatarClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onEditClick = onNavigateToEditProfile
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Quick Actions - MOVED HIGHER
            SectionHeader("HIZLI İŞLEMLER")
            QuickActionsGrid(
                onManageCities = onNavigateToCities,
                onAiHistory = onNavigateToAiHistory,
                onMyTravels = onNavigateToTravels,
                onChooseTheme = onNavigateToSettings,
                onEditProfile = onNavigateToEditProfile,
                onPremium = {
                    comingSoonTitle = "Havamania yakında hizmetinizde olacak."
                    showComingSoonDialog = true
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 3. Kendinden Bahset (Premium Card)
            SectionHeader("KİŞİSEL ANALİZ")
            PremiumAboutMeCard(
                text = aboutMe,
                onClick = { showAboutMeSheet = true }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 4. Premium Kategori Bazlı İlgi Alanları
            SectionHeader("HAVA TERCİHLERİ & İLGİ ALANLARI")
            PremiumInterestsSection(
                selectedInterests = userInterests,
                onInterestToggle = { themeViewModel.toggleInterest(it) }
            )

            Spacer(modifier = Modifier.height(100.dp))
        }

        if (showAboutMeSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAboutMeSheet = false },
                containerColor = themeColors.surface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = themeColors.textPrimary.copy(0.2f)) }
            ) {
                PremiumAboutMeContent(
                    initialText = aboutMe,
                    onSave = {
                        themeViewModel.setUserAboutMe(it)
                        showAboutMeSheet = false
                    }
                )
            }
        }

        if (showComingSoonDialog) {
            AlertDialog(
                onDismissRequest = { showComingSoonDialog = false },
                containerColor = themeColors.surface,
                title = { Text("YAKINDA", fontWeight = FontWeight.Black, color = themeColors.textPrimary) },
                text = { Text(comingSoonTitle, color = themeColors.textSecondary) },
                confirmButton = {
                    TextButton(onClick = { showComingSoonDialog = false }) {
                        Text("TAMAM", fontWeight = FontWeight.Black, color = themeColors.accent)
                    }
                }
            )
        }
    }
}

@Composable
fun PremiumProfileHeader(
    name: String,
    bio: String,
    imageUri: String?,
    interests: Set<String>,
    aboutMe: String,
    stats: Map<String, String>,
    onAvatarClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Photo Section with Ambient Glow
        Box(contentAlignment = Alignment.Center) {
            val infiniteTransition = rememberInfiniteTransition(label = "profile_glow")
            val glowScale by infiniteTransition.animateFloat(
                initialValue = 0.8f, targetValue = 1.2f,
                animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
                label = "glow_scale"
            )

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(glowScale)
                    .blur(30.dp)
                    .background(themeColors.accent.copy(alpha = 0.15f), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(themeColors.surfaceGlass)
                    .border(2.dp, Brush.linearGradient(themeColors.gradientPrimary), CircleShape)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Rounded.Person, null, tint = themeColors.textPrimary, modifier = Modifier.size(50.dp))
                }
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Icon(Icons.Rounded.PhotoCamera, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 8.dp).size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = name, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp), color = themeColors.textPrimary)
            IconButton(onClick = onEditClick) {
                Icon(Icons.Rounded.Verified, null, tint = themeColors.accent, modifier = Modifier.size(20.dp))
            }
        }

        Text(text = bio, style = MaterialTheme.typography.bodyMedium, color = themeColors.textSecondary, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(16.dp))

        // AI Personality Badge
        val personality = remember(interests, aboutMe) { generateAiPersonality(interests, aboutMe) }
        Surface(
            color = themeColors.accent.copy(alpha = 0.1f),
            shape = CircleShape,
            border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))
        ) {
            Text(
                personality.uppercase(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                color = themeColors.accent
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mini Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            stats.forEach { (label, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
                    Text(label.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = themeColors.textMuted)
                }
            }
        }
    }
}

@Composable
fun PremiumAboutMeCard(text: String, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    val hasContent = text.isNotBlank()

    HavamaniaGlassCard(
        onClick = onClick,
        alpha = 0.6f,
        cornerRadius = 28.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(themeColors.accent.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = themeColors.accent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Kendinden Bahset", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
                        Text("AI seni tanısın, önerileri özelleştirsin.", style = MaterialTheme.typography.bodySmall, color = themeColors.textSecondary)
                    }
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = themeColors.textMuted)
            }

            if (hasContent) {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(themeColors.surfaceGlass.copy(alpha = 0.3f)).padding(16.dp)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = themeColors.textPrimary.copy(alpha = 0.9f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(16.dp))
                // Analysis Badges
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val badges = remember(text) { generateBadges(text) }
                    badges.forEach { badge ->
                        PremiumBadge(badge)
                    }
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
    Surface(
        color = themeColors.surfaceGlass,
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.border.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(themeColors.accent))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = themeColors.textPrimary)
        }
    }
}

@Composable
fun PremiumInterestsSection(
    selectedInterests: Set<String>,
    onInterestToggle: (String) -> Unit
) {
    val allCategories = InterestsData.categories
    val mainCategories = listOf("Hava & Atmosfer", "Ulaşım & Yol", "Outdoor & Macera")

    val prioritized = allCategories.filter { mainCategories.contains(it.title) }
    val others = allCategories.filter { !mainCategories.contains(it.title) }

    var showOthers by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        prioritized.forEach { category ->
            PremiumInterestCategoryCard(
                category = category,
                selectedInterests = selectedInterests,
                onInterestToggle = onInterestToggle
            )
        }

        if (!showOthers) {
            TextButton(
                onClick = { showOthers = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Text("DAHA FAZLA GÖSTER", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp), color = HavamaniaTheme.colors.accent)
            }
        } else {
            others.forEach { category ->
                PremiumInterestCategoryCard(
                    category = category,
                    selectedInterests = selectedInterests,
                    onInterestToggle = onInterestToggle
                )
            }
        }
    }
}

@Composable
fun PremiumInterestCategoryCard(
    category: InterestCategory,
    selectedInterests: Set<String>,
    onInterestToggle: (String) -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val selectedCount = remember(selectedInterests) { category.interests.count { selectedInterests.contains(it.id) } }

    HavamaniaGlassCard(
        alpha = 0.45f,
        cornerRadius = 24.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(themeColors.accent.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(category.icon, null, tint = themeColors.accent, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
                    Text(category.description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = themeColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                if (selectedCount > 0) {
                    Surface(color = themeColors.accent, shape = CircleShape) {
                        Text(selectedCount.toString(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                }

                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    null, tint = themeColors.textMuted
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(20.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        category.interests.forEach { item ->
                            PremiumInterestChip(
                                item = item,
                                isSelected = selectedInterests.contains(item.id),
                                onClick = { onInterestToggle(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumInterestChip(
    item: InterestItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressedState = interactionSource.collectIsPressedAsState()
    val isPressed = isPressedState.value
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "chip_scale")

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        color = if (isSelected) themeColors.accent else themeColors.surfaceGlass.copy(alpha = 0.5f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) themeColors.accent else themeColors.border.copy(alpha = 0.1f)
        ),
        modifier = Modifier.scale(scale)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                item.icon, null,
                tint = if (isSelected) Color.White else themeColors.accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                item.label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) Color.White else themeColors.textPrimary
            )
        }
    }
}

@Composable
fun PremiumAboutMeContent(initialText: String, onSave: (String) -> Unit) {
    val themeColors = HavamaniaTheme.colors
    var text by remember { mutableStateOf(initialText) }

    val sampleCards = listOf(
        AboutMeSample("Outdoor", "Hafta sonları doğa yürüyüşü ve kamp yapmayı seviyorum. Rüzgar ve sıcaklık takibi benim için kritik.", Icons.Rounded.Terrain),
        AboutMeSample("Aile", "Çocuklarım için hava durumunu takip ediyorum. Park ve bahçe günlerini planlıyorum.", Icons.Rounded.ChildCare),
        AboutMeSample("Seyahat", "İş için sık seyahat ediyorum. Hafif ama şık giyinmeyi, yağmurdan kaçınmayı severim.", Icons.Rounded.Flight),
        AboutMeSample("Macera", "Kar tutkunuyum. Kışın her fırsatta snowboard yapmaya giderim. Kar durumunu bilmek önemli.", Icons.Rounded.Snowboarding),
        AboutMeSample("Şehir", "Şehir hayatını seviyorum. Fotoğraf çekmek için en güzel ışığı ve bulutları arıyorum.", Icons.Rounded.CameraAlt)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("HAVA KİMLİĞİ", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp), color = themeColors.accent)
        Text("Kendinden Bahset", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
        Text("AI, bu bilgilerle sana özel seyahat ve kıyafet önerileri üretecek.", style = MaterialTheme.typography.bodyMedium, color = themeColors.textSecondary)

        Spacer(Modifier.height(32.dp))

        // Örnek Kartlar (Yatay Kaydırılabilir)
        Text("ÖRNEK SENARYOLAR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = themeColors.textMuted)
        Spacer(Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 40.dp)
        ) {
            items(sampleCards, key = { it.label }) { sample ->
                PremiumSampleCard(sample = sample, onClick = { text = sample.text })
            }
        }

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().height(160.dp),
            placeholder = { Text("Kimsin? Hava durumu hayatını nasıl etkiliyor? Havamania sana nasıl yardımcı olabilir?", color = themeColors.textMuted.copy(alpha = 0.6f)) },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColors.accent,
                unfocusedBorderColor = themeColors.border.copy(alpha = 0.2f),
                cursorColor = themeColors.accent,
                focusedTextColor = themeColors.textPrimary,
                unfocusedTextColor = themeColors.textPrimary
            )
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { onSave(text) },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            enabled = text.isNotBlank(),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("PROFİLİ GÜNCELLE", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = Color.White)
            }
        }
    }
}

@Composable
fun PremiumSampleCard(sample: AboutMeSample, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    HavamaniaGlassCard(
        onClick = onClick,
        alpha = 0.4f,
        cornerRadius = 24.dp,
        modifier = Modifier.width(220.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(themeColors.accent.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(sample.icon, null, tint = themeColors.accent, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(sample.label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
            }
            Spacer(Modifier.height(12.dp))
            Text(sample.text, style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp), color = themeColors.textSecondary, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(12.dp))
            Text("Örneği Kullan", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = themeColors.accent))
        }
    }
}

data class AboutMeSample(val label: String, val text: String, val icon: ImageVector)

fun generateAiPersonality(interests: Set<String>, aboutMe: String): String {
    val t = aboutMe.lowercase()
    return when {
        t.contains("firtina") || interests.contains("firtina_takibi") -> "Atmosfer Avcısı"
        t.contains("kamp") || interests.contains("kamp") -> "Outdoor Gezgini"
        t.contains("cocuk") || interests.contains("cocuklar_icin") -> "Aile Meteoroloğu"
        t.contains("drone") || interests.contains("drone") -> "Gökyüzü Kaşifi"
        t.contains("motorsiklet") || interests.contains("motorsiklet") -> "Yol Savaşçısı"
        interests.contains("snowboard") || interests.contains("kayak") -> "Kış Tutkunu"
        interests.size > 10 -> "Hava Gurusu"
        else -> "Hava Meraklısı"
    }
}

fun generateBadges(text: String): List<String> {
    val badges = mutableListOf<String>()
    val t = text.lowercase()
    if (t.contains("kamp") || t.contains("doğa") || t.contains("yürüyüş")) badges.add("Doğa Sever")
    if (t.contains("çocuk") || t.contains("aile")) badges.add("Aile Odaklı")
    if (t.contains("seyahat") || t.contains("rota")) badges.add("Gezgin")
    if (t.contains("bisiklet") || t.contains("spor") || t.contains("koşu")) badges.add("Aktif Yaşam")
    if (t.contains("yağmur") || t.contains("kar") || t.contains("kış")) badges.add("Kış Tutkunu")
    if (t.contains("drone") || t.contains("pilot")) badges.add("Hava Meraklısı")
    if (t.contains("iş") || t.contains("seyahat")) badges.add("Şehir Kaşifi")
    if (badges.isEmpty() && text.isNotBlank()) badges.add("Hava Analisti")
    return badges.take(3)
}

@Composable
fun SectionHeader(text: String) {
    val themeColors = HavamaniaTheme.colors
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Black),
        color = themeColors.accent.copy(alpha = 0.8f),
        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
    )
}

@Composable
fun QuickActionsGrid(
    onManageCities: () -> Unit,
    onAiHistory: () -> Unit,
    onMyTravels: () -> Unit,
    onChooseTheme: () -> Unit,
    onEditProfile: () -> Unit,
    onPremium: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionItem("Şehirler", Icons.Rounded.Map, Modifier.weight(1f), onManageCities)
            QuickActionItem("AI Geçmişi", Icons.Rounded.History, Modifier.weight(1f), onAiHistory)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionItem("Seyahatlerim", Icons.Rounded.Route, Modifier.weight(1f), onMyTravels)
            QuickActionItem("Tema Seç", Icons.Rounded.Palette, Modifier.weight(1f), onChooseTheme)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionItem("Premium", Icons.Rounded.WorkspacePremium, Modifier.weight(1f), onPremium)
            QuickActionItem("Profili Düzenle", Icons.Rounded.AccountCircle, Modifier.weight(1f), onEditProfile)
        }
    }
}

@Composable
fun QuickActionItem(title: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    HavamaniaGlassCard(modifier = modifier, cornerRadius = 18.dp, alpha = 0.4f, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = themeColors.textPrimary)
        }
    }
}
