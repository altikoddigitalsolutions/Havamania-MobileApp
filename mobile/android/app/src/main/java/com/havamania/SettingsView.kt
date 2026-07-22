package com.havamania

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.havamania.ui.theme.HavamaniaDialog
import com.havamania.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToCities: () -> Unit = {},
    onNavigateToLegal: (String, String) -> Unit = { _, _ -> },
    themeViewModel: ThemeViewModel = viewModel(),
    travelViewModel: TravelViewModel = viewModel(),
    aiHistoryViewModel: AiHistoryViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val currentTheme by themeViewModel.currentTheme.collectAsState()
    val tempUnit by themeViewModel.tempUnit.collectAsState()
    val language by themeViewModel.language.collectAsState()
    val assistantTone by themeViewModel.assistantTone.collectAsState()
    val notificationsEnabled by themeViewModel.notificationsEnabled.collectAsState()
    val defaultCity by themeViewModel.defaultCity.collectAsState()

    val themeColors = HavamaniaTheme.colors

    var showThemeSheet by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showToneDialog by remember { mutableStateOf(false) }
    var showUnitDialog by remember { mutableStateOf(false) }
    var showManageDataSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showComingSoonDialog by remember { mutableStateOf(false) }
    var comingSoonTitle by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState()

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "AYARLAR",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            SettingsGroupLabel("GENEL")
            HavamaniaGlassCard {
                SettingsNavRow("Dil", if (language == "TR") "Türkçe" else "English", Icons.Rounded.Language) { showLanguageDialog = true }
                SettingsDivider()
                SettingsNavRow("Sıcaklık Birimi", tempUnit.title + " (" + tempUnit.symbol + ")", Icons.Rounded.Thermostat) { showUnitDialog = true }
                SettingsDivider()
                SettingsNavRow("Varsayılan Şehir", defaultCity?.name ?: "Bilinmiyor", Icons.Rounded.LocationCity) { onNavigateToCities() }
            }

            Spacer(modifier = Modifier.height(28.dp))

            SettingsGroupLabel("GÖRÜNÜM")
            HavamaniaGlassCard {
                PremiumThemeRow(selectedTheme = currentTheme, onClick = { showThemeSheet = true })
            }

            Spacer(modifier = Modifier.height(28.dp))

            SettingsGroupLabel("ASİSTAN")
            HavamaniaGlassCard {
                SettingsNavRow("Asistan Konuşma Dili", assistantTone.title, Icons.Rounded.AutoAwesome) { showToneDialog = true }
            }

            Spacer(modifier = Modifier.height(28.dp))

            SettingsGroupLabel("BİLDİRİMLER")
            HavamaniaGlassCard {
                SettingsToggleRow("Hava Durumu Uyarıları", "Anlık meteorolojik bildirimler", Icons.Rounded.Notifications, notificationsEnabled) {
                    themeViewModel.setNotificationsEnabled(it)
                }
                SettingsDivider()
                SettingsNavRow("Akıllı Uyarılar", "Kişiselleştirilmiş tercihler", Icons.Rounded.SettingsSuggest) {
                    comingSoonTitle = "Akıllı hava uyarıları yakında eklenecek."
                    showComingSoonDialog = true
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            SettingsGroupLabel("PREMIUM")
            PremiumSettingsCard(onClick = {
                comingSoonTitle = "Havamania Premium yakında tüm özellikleri ile açılacak."
                showComingSoonDialog = true
            })

            Spacer(modifier = Modifier.height(28.dp))

            SettingsGroupLabel("HESAP VE VERİ")
            HavamaniaGlassCard {
                SettingsNavRow("Profil Bilgileri", "İsim, bio ve kimlik", Icons.Rounded.AccountCircle, onNavigateToEditProfile)
                SettingsDivider()
                SettingsNavRow("Verilerimi Yönet", "Güvenlik ve gizlilik", Icons.Rounded.Security) { showManageDataSheet = true }
                SettingsDivider()
                SettingsNavRow("Gizlilik Politikası", null, Icons.Rounded.PrivacyTip) {
                    onNavigateToLegal("GİZLİLİK POLİTİKASI", LegalUrls.PRIVACY_POLICY)
                }
                SettingsDivider()
                SettingsNavRow("Kullanım Şartları", null, Icons.Rounded.Description) {
                    onNavigateToLegal("KULLANIM KOŞULLARI", LegalUrls.TERMS_OF_USE)
                }
                SettingsDivider()
                SettingsNavRow("Oturumu Kapat", "Hesabınızdan çıkış yapın", Icons.Rounded.Logout) { showLogoutDialog = true }
            }

            Spacer(modifier = Modifier.height(48.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Havamania Premium v1.5.0",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                    color = themeColors.textPrimary.copy(alpha = 0.5f)
                )
                Text(
                    text = "Lansman Sürümü • 2026",
                    style = MaterialTheme.typography.labelSmall,
                    color = themeColors.textMuted.copy(alpha = 0.4f)
                )
            }
            Spacer(modifier = Modifier.height(100.dp))
        }

        // --- DIALOGS & SHEETS ---

        if (showThemeSheet) {
            ModalBottomSheet(
                onDismissRequest = { showThemeSheet = false },
                sheetState = sheetState,
                containerColor = themeColors.surface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = themeColors.textPrimary.copy(0.2f)) }
            ) {
                ThemeSelectionContent(selectedTheme = currentTheme) {
                    themeViewModel.setTheme(it)
                    showThemeSheet = false
                }
            }
        }

        if (showManageDataSheet) {
            ModalBottomSheet(
                onDismissRequest = { showManageDataSheet = false },
                containerColor = themeColors.surface
            ) {
                ManageDataContent(
                    onClearAiHistory = { aiHistoryViewModel.clearAll() },
                    onClearTravels = {
                        travelViewModel.clearAllPlans()
                        notificationViewModel.deleteTravelNotifications()
                    },
                    onResetCities = { themeViewModel.resetCities() },
                    onRemovePhoto = {
                        profileViewModel.removeProfileImage()
                    },
                    onResetAll = {
                        themeViewModel.resetAllData()
                        aiHistoryViewModel.clearAll()
                        travelViewModel.clearAllPlans()
                        notificationViewModel.deleteAllNotifications()
                        profileViewModel.removeProfileImage()
                    }
                )
            }
        }

        if (showLanguageDialog) {
            SettingsOptionDialog("Dil Seçin", listOf("TR" to "Türkçe", "EN" to "English"), language, { themeViewModel.setLanguage(it); showLanguageDialog = false }) { showLanguageDialog = false }
        }

        if (showToneDialog) {
            SettingsOptionDialog(
                title = "Asistan Konuşma Dili",
                options = AssistantTone.entries.map { it.name to it.title },
                currentValue = assistantTone.name,
                onSelect = {
                    themeViewModel.setAssistantTone(AssistantTone.valueOf(it))
                    showToneDialog = false
                }
            ) { showToneDialog = false }
        }

        if (showUnitDialog) {
            SettingsOptionDialog("Sıcaklık Birimi", listOf("CELSIUS" to "Celsius (°C)", "FAHRENHEIT" to "Fahrenheit (°F)"), tempUnit.name, { themeViewModel.setTempUnit(TemperatureUnit.valueOf(it)); showUnitDialog = false }) { showUnitDialog = false }
        }

        if (showLogoutDialog) {
            HavamaniaDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = "OTURUMU KAPAT",
                text = "Oturumunuzu kapatmak istediğinize emin misiniz?",
                confirmText = "ÇIKIŞ YAP",
                confirmColor = themeColors.error,
                onConfirm = {
                    authViewModel.signOut()
                    showLogoutDialog = false
                    onBack() // This will trigger the redirection logic in Activity
                }
            )
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
    }
}

@Composable
fun ManageDataContent(
    onClearAiHistory: () -> Unit,
    onClearTravels: () -> Unit,
    onResetCities: () -> Unit,
    onRemovePhoto: () -> Unit,
    onResetAll: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var confirmTitle by remember { mutableStateOf("") }
    var confirmText by remember { mutableStateOf("") }
    var isExtraCritical by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp)
            .alpha(if (isProcessing) 0.5f else 1f)
    ) {
        // ... (Header text)
        Spacer(Modifier.height(8.dp))
        Text(
            "GİZLİLİK VE VERİ",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
            color = themeColors.accent
        )
        // ... (rest of text)
        Text(
            "Verilerimi Yönet",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            color = themeColors.textPrimary
        )
        Text(
            "Uygulama verilerini buradan kontrol edebilir veya sıfırlayabilirsiniz.",
            style = MaterialTheme.typography.bodyMedium,
            color = themeColors.textSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        DataActionItem("AI Sohbet Geçmişini Temizle", Icons.Rounded.AutoAwesome) {
            if (isProcessing) return@DataActionItem
            confirmTitle = "Geçmişi Temizle"
            confirmText = "Tüm AI sohbet kayıtlarınız kalıcı olarak silinecektir. Devam etmek istiyor musunuz?"
            confirmAction = onClearAiHistory
            isExtraCritical = false
        }
        DataActionItem("Tüm Seyahatleri Sil", Icons.Rounded.Route) {
            if (isProcessing) return@DataActionItem
            confirmTitle = "Seyahatleri Sil"
            confirmText = "Oluşturduğunuz tüm seyahat planları ve analizleri silinecektir. Bu işlem geri alınamaz."
            confirmAction = onClearTravels
            isExtraCritical = false
        }
        DataActionItem("Lokasyon Listesini Sıfırla", Icons.Rounded.Map) {
            if (isProcessing) return@DataActionItem
            confirmTitle = "Şehirleri Sıfırla"
            confirmText = "Kayıtlı tüm şehirler silinecek ve varsayılan ayarlara dönülecektir."
            confirmAction = onResetCities
            isExtraCritical = false
        }
        DataActionItem("Profil Fotoğrafını Kaldır", Icons.Rounded.NoPhotography) {
            if (isProcessing) return@DataActionItem
            confirmTitle = "Fotoğrafı Kaldır"
            confirmText = "Profil fotoğrafınız kaldırılacak. Devam edilsin mi?"
            confirmAction = onRemovePhoto
            isExtraCritical = false
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = themeColors.border.copy(alpha = 0.05f))
        Spacer(modifier = Modifier.height(8.dp))

        DataActionItem("Tüm Verileri Sıfırla", Icons.Rounded.DeleteForever, themeColors.error) {
            if (isProcessing) return@DataActionItem
            confirmTitle = "KRİTİK: Tüm Verileri Sıfırla"
            confirmText = "Bu işlem tüm profilinizi, seyahatlerinizi ve ayarlarınızı kalıcı olarak silecektir. Uygulama ilk yükleme anına dönecektir. Emin misiniz?"
            confirmAction = onResetAll
            isExtraCritical = true
        }
    }

    if (confirmAction != null) {
        HavamaniaDialog(
            onDismissRequest = { if (!isProcessing) confirmAction = null },
            title = confirmTitle,
            text = if (isProcessing) "İşlem yapılıyor, lütfen bekleyin..." else confirmText,
            confirmText = if (isProcessing) "BEKLEYİN..." else if (isExtraCritical) "EVET, HER ŞEYİ SİL" else "SİL",
            confirmColor = themeColors.error,
            confirmEnabled = !isProcessing,
            dismissEnabled = !isProcessing,
            icon = if (isExtraCritical) Icons.Rounded.ReportProblem else Icons.Rounded.DeleteForever,
            onConfirm = {
                scope.launch {
                    isProcessing = true
                    confirmAction?.invoke()
                    delay(800) // Visual feedback for completion
                    isProcessing = false
                    confirmAction = null
                }
            }
        )
    }
}

@Composable
fun DataActionItem(label: String, icon: ImageVector, color: Color = HavamaniaTheme.colors.textPrimary, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmBtn: String, confirmColor: Color, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = themeColors.surface,
        title = { Text(title, fontWeight = FontWeight.Black, color = themeColors.textPrimary) },
        text = { Text(text, color = themeColors.textSecondary) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmBtn, color = confirmColor, fontWeight = FontWeight.Black) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("VAZGEÇ", color = themeColors.textPrimary, fontWeight = FontWeight.Black) } }
    )
}

@Composable
fun SettingsOptionDialog(
    title: String,
    options: List<Pair<String, String>>,
    currentValue: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = themeColors.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = themeColors.textPrimary
            )
        },
        text = {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                options.forEach { (value, label) ->
                    val isSelected = value == currentValue
                    Surface(
                        onClick = { onSelect(value) },
                        color = if (isSelected) themeColors.accent.copy(alpha = 0.05f) else Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelect(value) },
                                colors = RadioButtonDefaults.colors(selectedColor = themeColors.accent)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) themeColors.accent else themeColors.textPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("KAPAT", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black), color = themeColors.textMuted)
            }
        }
    )
}

@Composable
fun PremiumSettingsCard(onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    Surface(
        onClick = onClick,
        color = themeColors.accent,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background Pattern or Shine
            Icon(
                Icons.Rounded.AutoAwesome,
                null,
                tint = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.size(120.dp).align(Alignment.BottomEnd).offset(x = 20.dp, y = 20.dp)
            )

            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Havamania Premium",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                    Text(
                        "Gelişmiş seyahat analizleri ve AI kişiselleştirme.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Rounded.ArrowForward,
                        null,
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumThemeRow(selectedTheme: AppTheme, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 380 // Telefon genişliği eşiği

    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBoxContainer(Icons.Rounded.Palette, themeColors.accent)
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Uygulama Teması",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isSmallScreen) 15.sp else 16.sp
                    ),
                    color = themeColors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = selectedTheme.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (isSmallScreen) 11.sp else 12.sp
                    ),
                    color = themeColors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = themeColors.textSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun ThemeSelectionContent(selectedTheme: AppTheme, onThemeSelected: (AppTheme) -> Unit) {
    val themeColors = HavamaniaTheme.colors
    val displayThemes = listOf(
        AppTheme.AUTO,
        AppTheme.LIGHT,
        AppTheme.DARK,
        AppTheme.SPRING_DAY,
        AppTheme.SUMMER_DAY,
        AppTheme.AUTUMN_DAY,
        AppTheme.WINTER_DAY
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "TEMA SEÇİN",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            ),
            color = themeColors.textPrimary
        )
        Text(
            "Havamania deneyiminizi ruh halinize göre kişiselleştirin",
            style = MaterialTheme.typography.bodyMedium,
            color = themeColors.textSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))

        // 2-Column Grid Layout
        val rows = displayThemes.chunked(2)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rows.forEach { rowThemes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowThemes.forEach { theme ->
                        ThemeCardPremium(
                            theme = theme,
                            isSelected = selectedTheme == theme,
                            modifier = Modifier.weight(1f),
                            onClick = { onThemeSelected(theme) }
                        )
                    }
                    if (rowThemes.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ThemeCardPremium(theme: AppTheme, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    val themeStyleColors = ThemeFactory.createColors(theme)
    val displayName = when(theme) {
        AppTheme.SPRING_DAY -> "İlkbahar"
        AppTheme.SUMMER_DAY -> "Yaz"
        AppTheme.AUTUMN_DAY -> "Sonbahar"
        AppTheme.WINTER_DAY -> "Kış"
        AppTheme.AUTO -> "Otomatik"
        AppTheme.LIGHT -> "Açık"
        AppTheme.DARK -> "Koyu"
        else -> theme.title
    }

    val isRecommended = theme == AppTheme.AUTO

    HavamaniaGlassCard(
        modifier = modifier.height(140.dp),
        cornerRadius = 20.dp,
        alpha = if (isSelected) 0.9f else 0.4f,
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            displayName,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            ),
                            color = if (isSelected) themeColors.accent else themeColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isSelected) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Rounded.CheckCircle, null, tint = themeColors.accent, modifier = Modifier.size(14.dp))
                        }
                    }
                    if (isRecommended) {
                        Surface(
                            color = themeColors.accent.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                "ÖNERİLEN",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Black),
                                color = themeColors.accent
                            )
                        }
                    }
                }

                // Gradient Preview
                Row(
                    modifier = Modifier.fillMaxWidth().height(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val previewColors = themeStyleColors.gradientPrimary
                    previewColors.take(3).forEach { color ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(color)
                        )
                    }
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.5.dp, themeColors.accent, RoundedCornerShape(20.dp))
                )
            }
        }
    }
}


fun getThemeDesc(theme: AppTheme): String = when(theme) {
    AppTheme.AUTO -> "Mevsime ve günün saatine göre otomatik değişir"
    AppTheme.LIGHT -> "Temiz ve ferah gökyüzü tonları"
    AppTheme.DARK -> "Koyu ve modern gece atmosferi"
    AppTheme.SPRING_DAY -> "Taze bahar esintisi ve yeşil dokular"
    AppTheme.SPRING_NIGHT -> "Huzurlu bahar gecesi atmosferi"
    AppTheme.SUMMER_DAY -> "Sıcak yaz güneşi ve canlı renkler"
    AppTheme.SUMMER_NIGHT -> "Ferah yaz gecesi esintisi"
    AppTheme.AUTUMN_DAY -> "Huzurlu sonbahar ve toprak tonları"
    AppTheme.AUTUMN_NIGHT -> "Gizemli sonbahar gecesi renkleri"
    AppTheme.WINTER_DAY -> "Soğuk kış esintisi ve buzsu maviler"
    AppTheme.WINTER_NIGHT -> "Derin kış gecesi ve kristal tonlar"
}

@Composable
fun SettingsGroupLabel(title: String) {
    val themeColors = HavamaniaTheme.colors
    Text(text = title, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Black), color = themeColors.accent.copy(alpha = 0.7f), modifier = Modifier.padding(start = 12.dp, bottom = 12.dp))
}

@Composable
fun SettingsNavRow(title: String, subtitle: String? = null, icon: ImageVector, onClick: () -> Unit = {}) {
    val themeColors = HavamaniaTheme.colors
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBoxContainer(icon, themeColors.accent)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = themeColors.textPrimary)
                if (subtitle != null) { Text(subtitle, style = MaterialTheme.typography.bodySmall, color = themeColors.textSecondary) }
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = themeColors.textSecondary.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SettingsToggleRow(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val themeColors = HavamaniaTheme.colors
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBoxContainer(icon, themeColors.accent)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = themeColors.textPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = themeColors.textSecondary)
        }
        HavamaniaToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun IconBoxContainer(icon: ImageVector, color: Color) {
    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SettingsDivider() {
    val themeColors = HavamaniaTheme.colors
    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = themeColors.textPrimary.copy(alpha = 0.05f))
}
