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

    val aiHistoryItems by aiHistoryViewModel.historyItems.collectAsState()
    val travelPlans by travelViewModel.plans.collectAsState()

    var showComingSoonDialog by remember { mutableStateOf(false) }
    var comingSoonTitle by remember { mutableStateOf("") }

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
                StatCardPremium(label = "En Çok", value = defaultCity, icon = Icons.Rounded.TrendingUp, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Weather Preferences Summary
            SectionHeader("HAVA DURUMU TERCİHLERİ")
            HavamaniaGlassCard(alpha = 0.4f) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    PreferenceSummaryRow("Sıcaklık Birimi", tempUnit.symbol, Icons.Rounded.Thermostat)
                    PreferenceSummaryRow("Varsayılan Şehir", defaultCity, Icons.Rounded.LocationCity)
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

            // 5. Selected Theme Card Preview
            SectionHeader("AKTİF TEMA")
            ThemePreviewCard(theme = currentTheme, onClick = onNavigateToSettings)

            Spacer(modifier = Modifier.height(32.dp))

            // 6. Quick Actions
            SectionHeader("HIZLI İŞLEMLER")
            QuickActionsGrid(
                onManageCities = onNavigateToCities,
                onAiHistory = onNavigateToAiHistory,
                onMyTravels = onNavigateToTravels,
                onChooseTheme = onNavigateToSettings,
                onEditProfile = onNavigateToEditProfile
            )

            Spacer(modifier = Modifier.height(100.dp))
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
    onEditProfile: () -> Unit
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
