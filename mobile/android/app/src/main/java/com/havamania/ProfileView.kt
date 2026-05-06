package com.havamania

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.havamania.ui.theme.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToCities: () -> Unit = {},
    onNavigateToAiHistory: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToTravels: () -> Unit = {},
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

    val availableInterests = listOf(
        "Kamp", "Yürüyüş", "Deniz", "Kayak", "Fotoğrafçılık",
        "Spor", "Bisiklet", "Seyahat", "Koşu", "Yüzme"
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { themeViewModel.setUserImageUri(it.toString()) }
        }
    )

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "PROFİL PANELİ",
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

            // 1. Header with Photo & Info
            EnhancedProfileHeader(
                name = name,
                bio = bio,
                imageUri = imageUri,
                onAvatarClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onEditClick = onNavigateToEditProfile
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 2. Statistics Cards
            SectionHeader("İSTATİSTİKLER")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCardPremium(label = "Şehir", value = registeredCities.size.toString(), icon = Icons.Rounded.LocationOn, modifier = Modifier.weight(1f))
                StatCardPremium(label = "Seyahat", value = travelPlans.size.toString(), icon = Icons.Rounded.Route, modifier = Modifier.weight(1f))
                StatCardPremium(label = "AI Analiz", value = aiHistoryItems.size.toString(), icon = Icons.Rounded.AutoAwesome, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val favTrip = travelPlans.groupBy { it.tripType }.maxByOrNull { it.value.size }?.key?.label ?: "Belirsiz"
                StatCardPremium(label = "Favori Tip", value = favTrip, icon = Icons.Rounded.Star, modifier = Modifier.weight(1.5f))
                StatCardPremium(label = "En Çok", value = defaultCity.name, icon = Icons.Rounded.TrendingUp, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Weather Preferences Summary
            SectionHeader("HAVA DURUMU TERCİHLERİ")
            HavamaniaGlassCard(alpha = 0.4f) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    PreferenceSummaryRow("Sıcaklık Birimi", tempUnit.symbol, Icons.Rounded.Thermostat)
                    PreferenceSummaryRow("Varsayılan Şehir", defaultCity.name, Icons.Rounded.LocationCity)
                    PreferenceSummaryRow("Uyarılar", if (notificationsEnabled) "Açık" else "Kapalı", Icons.Rounded.NotificationsActive)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Interests
            SectionHeader("İLGİ ALANLARINIZ")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableInterests.forEach { interest ->
                    val isSelected = userInterests.contains(interest)
                    HavamaniaChip(
                        selected = isSelected,
                        onClick = { themeViewModel.toggleInterest(interest) },
                        label = interest
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 5. Kendinden Bahset
            SectionHeader("KENDİNDEN BAHSET")
            AboutMeCard(
                text = aboutMe,
                onClick = { showAboutMeSheet = true }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 6. Selected Theme Card Preview
            SectionHeader("AKTİF TEMA")
            ThemePreviewCard(theme = currentTheme, onClick = onNavigateToSettings)

            Spacer(modifier = Modifier.height(32.dp))

            // 7. Quick Actions
            SectionHeader("HIZLI İŞLEMLER")
            QuickActionsGrid(
                onManageCities = onNavigateToCities,
                onAiHistory = onNavigateToAiHistory,
                onMyTravels = onNavigateToTravels,
                onChooseTheme = onNavigateToSettings,
                onEditProfile = onNavigateToEditProfile,
                onNotifications = {
                    comingSoonTitle = "Bildirim tercihleri yakında eklenecek."
                    showComingSoonDialog = true
                },
                onPremium = {
                    comingSoonTitle = "Havamania Premium yakında hizmetinizde olacak."
                    showComingSoonDialog = true
                }
            )

            Spacer(modifier = Modifier.height(100.dp))
        }

        if (showAboutMeSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAboutMeSheet = false },
                containerColor = themeColors.surface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = themeColors.textPrimary.copy(0.2f)) }
            ) {
                AboutMeContent(
                    initialText = aboutMe,
                    onSave = {
                        themeViewModel.setUserAboutMe(it)
                        showAboutMeSheet = false
                    }
                )
            }
        }
    }
}

@Composable
fun AboutMeCard(text: String, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    val hasContent = text.isNotBlank()

    HavamaniaGlassCard(
        onClick = onClick,
        alpha = 0.5f,
        cornerRadius = 24.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(themeColors.accent.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = themeColors.accent, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Kendinden Bahset",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = themeColors.textPrimary
                    )
                }
                Icon(Icons.Rounded.Edit, null, tint = themeColors.accent, modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.height(12.dp))

            if (hasContent) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = themeColors.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                // AI Badge Mockup (Bonus Özellik)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val badges = remember(text) { generateBadges(text) }
                    badges.forEach { badge ->
                        Surface(
                            color = themeColors.accent.copy(alpha = 0.1f),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, themeColors.accent.copy(alpha = 0.2f))
                        ) {
                            Text(
                                badge,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = themeColors.accent
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Havamania deneyimini sana özel hale getirelim.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.textMuted
                )
            }
        }
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
    if (badges.isEmpty() && text.isNotBlank()) badges.add("Hava Meraklısı")
    return badges.take(3)
}

@Composable
fun AboutMeContent(initialText: String, onSave: (String) -> Unit) {
    val themeColors = HavamaniaTheme.colors
    var text by remember { mutableStateOf(initialText) }

    val sampleTexts = listOf(
        "Hafta sonları doğa yürüyüşü yapmayı seviyorum. Genelde çocuklarımla dışarı çıkıyorum, bu yüzden yağmur ve rüzgar durumunu önceden bilmek benim için önemli. Yazın deniz tatillerini, kışın ise kısa şehir gezilerini tercih ederim. Çok sıcak havaları sevmem. Kamp, fotoğrafçılık ve sahil yürüyüşleri ilgimi çeker.",
        "İş için sık seyahat ediyorum. Toplantı günlerinde yağmur, rüzgar ve trafik etkilerini önceden bilmek istiyorum. Genelde hafif ama şık giyinmeyi tercih ederim.",
        "Çocuklarım için hava durumunu takip ediyorum. Hafta sonları park, yürüyüş ve açık hava aktiviteleri planlıyoruz. Yağmur, UV ve rüzgar uyarıları benim için önemli.",
        "Kamp ve doğa aktiviteleriyle ilgileniyorum. Rüzgar, gece sıcaklığı, yağış ihtimali ve görüş mesafesi benim için önemli. Hava uygunsa hafta sonu rota planlamayı severim."
    )

    var currentSampleIndex by remember { mutableIntStateOf(0) }

    val suggestions = listOf(
        "Kamp seviyorum",
        "Çocuklar için kullanıyorum",
        "Seyahat etmeyi seviyorum",
        "Bisiklet sürüyorum",
        "Yağmuru seviyorum",
        "Kış sporlarıyla ilgileniyorum",
        "Hafta sonu planları yapıyorum",
        "Sıcak havaları sevmiyorum"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "KENDİNDEN BAHSET",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = themeColors.textPrimary
        )
        Text(
            "Ne kadar çok bilgi verirsen, Havamania sana o kadar iyi öneriler sunar.",
            style = MaterialTheme.typography.bodyMedium,
            color = themeColors.textSecondary
        )

        Spacer(Modifier.height(24.dp))

        // Örnek Metin Kartı
        HavamaniaGlassCard(
            alpha = 0.3f,
            cornerRadius = 20.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    "ÖRNEK METİN",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                    color = themeColors.accent
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = sampleTexts[currentSampleIndex],
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = themeColors.textPrimary.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { text = sampleTexts[currentSampleIndex] },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent.copy(alpha = 0.2f), contentColor = themeColors.accent)
                    ) {
                        Text("Örneği Kullan", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                    OutlinedButton(
                        onClick = { currentSampleIndex = (currentSampleIndex + 1) % sampleTexts.size },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Text("Başka Örnek", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = themeColors.accent)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            placeholder = {
                Text(
                    "Kendinden bahset... İlgi alanların, seyahat alışkanlıkların, hava hassasiyetlerin veya kimin için hava durumunu takip ettiğini yazabilirsin.",
                    color = themeColors.textMuted.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColors.accent,
                unfocusedBorderColor = themeColors.border.copy(alpha = 0.3f),
                cursorColor = themeColors.accent,
                focusedTextColor = themeColors.textPrimary,
                unfocusedTextColor = themeColors.textPrimary
            )
        )

        Spacer(Modifier.height(16.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 24.dp)
        ) {
            items(suggestions) { suggestion ->
                Surface(
                    onClick = {
                        val separator = if (text.isNotBlank() && !text.endsWith(" ")) " " else ""
                        text += separator + suggestion + "."
                    },
                    color = themeColors.accent.copy(alpha = 0.05f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.1f))
                ) {
                    Text(
                        suggestion,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.textPrimary
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { onSave(text) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = text.isNotBlank(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
        ) {
            Text("KAYDET", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = Color.White)
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    val themeColors = HavamaniaTheme.colors
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Black),
        color = themeColors.accent.copy(alpha = 0.8f),
        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
    )
}

@Composable
fun StatCardPremium(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val themeColors = HavamaniaTheme.colors
    HavamaniaGlassCard(
        modifier = modifier,
        cornerRadius = 24.dp,
        alpha = 0.5f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(themeColors.accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = themeColors.textPrimary,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = themeColors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PreferenceSummaryRow(label: String, value: String, icon: ImageVector) {
    val themeColors = HavamaniaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = themeColors.accent.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = themeColors.textPrimary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = themeColors.accent)
    }
}

@Composable
fun EnhancedProfileHeader(
    name: String,
    bio: String,
    imageUri: String?,
    onAvatarClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .blur(20.dp)
                    .background(themeColors.accent.copy(alpha = 0.2f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(themeColors.surfaceGlass)
                    .border(1.5.dp, themeColors.accent.copy(alpha = 0.5f), CircleShape)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Rounded.Person,
                        null,
                        tint = themeColors.textPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Icon(Icons.Rounded.PhotoCamera, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 6.dp).size(14.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = name, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
            IconButton(onClick = onEditClick) {
                Icon(Icons.Rounded.Edit, null, tint = themeColors.accent, modifier = Modifier.size(18.dp))
            }
        }
        Text(text = bio, style = MaterialTheme.typography.bodyMedium, color = themeColors.textSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
fun ThemePreviewCard(theme: AppTheme, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    HavamaniaGlassCard(onClick = onClick, alpha = 0.6f, cornerRadius = 24.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(themeColors.gradientPrimary)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Palette, null, tint = themeColors.onAccent, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(theme.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
                Text("Seçili Atmosfer", style = MaterialTheme.typography.bodySmall, color = themeColors.textSecondary)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = themeColors.textMuted)
        }
    }
}

@Composable
fun QuickActionsGrid(
    onManageCities: () -> Unit,
    onAiHistory: () -> Unit,
    onMyTravels: () -> Unit,
    onChooseTheme: () -> Unit,
    onEditProfile: () -> Unit,
    onNotifications: () -> Unit,
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
            QuickActionItem("Bildirimler", Icons.Rounded.Notifications, Modifier.weight(1f), onNotifications)
            QuickActionItem("Premium", Icons.Rounded.WorkspacePremium, Modifier.weight(1f), onPremium)
        }
        QuickActionItem("Profili Düzenle", Icons.Rounded.AccountCircle, Modifier.fillMaxWidth(), onEditProfile)
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
