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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToCities: () -> Unit = {},
    themeViewModel: ThemeViewModel = viewModel(),
    travelViewModel: TravelViewModel = viewModel(),
    aiHistoryViewModel: AiHistoryViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val currentTheme by themeViewModel.currentTheme.collectAsState()
    val tempUnit by themeViewModel.tempUnit.collectAsState()
    val language by themeViewModel.language.collectAsState()
    val notificationsEnabled by themeViewModel.notificationsEnabled.collectAsState()
    val defaultCity by themeViewModel.defaultCity.collectAsState()

    val themeColors = HavamaniaTheme.colors

    var showThemeSheet by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showUnitDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showManageDataSheet by remember { mutableStateOf(false) }
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
                SettingsNavRow("Varsayılan Şehir", defaultCity.name, Icons.Rounded.LocationCity) { onNavigateToCities() }
            }

            Spacer(modifier = Modifier.height(28.dp))

            SettingsGroupLabel("GÖRÜNÜM")
            HavamaniaGlassCard {
                PremiumThemeRow(selectedTheme = currentTheme, onClick = { showThemeSheet = true })
            }

            Spacer(modifier = Modifier.height(28.dp))

            SettingsGroupLabel("BİLDİRİMLER")
            HavamaniaGlassCard {
                SettingsToggleRow("Hava Durumu Uyarıları", "Anlık bildirimler al", Icons.Rounded.Notifications, notificationsEnabled) {
                    themeViewModel.setNotificationsEnabled(it)
                }
                SettingsDivider()
                SettingsNavRow("Bildirim Tercihleri", "Detaylı özelleştirme", Icons.Rounded.SettingsSuggest) {
                    comingSoonTitle = "Akıllı hava uyarıları yakında eklenecek."
                    showComingSoonDialog = true
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            SettingsGroupLabel("HESAP")
            HavamaniaGlassCard {
                SettingsNavRow("Profil Bilgileri", "İsim, bio ve fotoğraf", Icons.Rounded.AccountCircle, onNavigateToEditProfile)
                SettingsDivider()
                SettingsNavRow("Premium Üyelik", "Tüm özellikleri aç", Icons.Rounded.WorkspacePremium) {
                    comingSoonTitle = "Havamania Premium yakında geliyor."
                    showComingSoonDialog = true
                }
                SettingsDivider()
                SettingsNavRow("Verilerimi Yönet", "Temizlik ve sıfırlama", Icons.Rounded.Storage) { showManageDataSheet = true }
                SettingsDivider()
                SettingsActionRow("Çıkış Yap", Icons.Rounded.Logout, themeColors.error) { showLogoutDialog = true }
            }

            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "Havamania v1.2.0-Premium",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = themeColors.textMuted.copy(alpha = 0.4f)
            )
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
                    onClearTravels = { travelViewModel.clearAllPlans() },
                    onResetCities = { themeViewModel.resetCities() },
                    onRemovePhoto = { themeViewModel.removeProfileImage() },
                    onResetAll = {
                        themeViewModel.resetAllData()
                        aiHistoryViewModel.clearAll()
                        travelViewModel.clearAllPlans()
                    }
                )
            }
        }

        if (showLanguageDialog) {
            SettingsOptionDialog("Dil Seçin", listOf("TR" to "Türkçe", "EN" to "English"), language, { themeViewModel.setLanguage(it); showLanguageDialog = false }) { showLanguageDialog = false }
        }

        if (showUnitDialog) {
            SettingsOptionDialog("Sıcaklık Birimi", listOf("CELSIUS" to "Celsius (°C)", "FAHRENHEIT" to "Fahrenheit (°F)"), tempUnit.name, { themeViewModel.setTempUnit(TemperatureUnit.valueOf(it)); showUnitDialog = false }) { showUnitDialog = false }
        }

        if (showLogoutDialog) {
            ConfirmDialog(
                title = "Oturumu Kapat",
                text = "Oturumu kapatmak istediğinize emin misiniz?",
                confirmBtn = "KAPAT",
                confirmColor = themeColors.error,
                onConfirm = { showLogoutDialog = false }, // In a real app, this would perform logout
                onDismiss = { showLogoutDialog = false }
            )
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

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
        Text("Verilerimi Yönet", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
        Spacer(modifier = Modifier.height(24.dp))

        DataActionItem("AI Geçmişini Temizle", Icons.Rounded.AutoAwesome) {
            confirmTitle = "AI geçmişini tamamen silmek istiyor musunuz?"; confirmAction = onClearAiHistory
        }
        DataActionItem("Seyahatleri Temizle", Icons.Rounded.Route) {
            confirmTitle = "Tüm seyahat planlarını silmek istiyor musunuz?"; confirmAction = onClearTravels
        }
        DataActionItem("Kayıtlı Şehirleri Sıfırla", Icons.Rounded.Map) {
            confirmTitle = "Kayıtlı şehirleri sıfırlamak istiyor musunuz?"; confirmAction = onResetCities
        }
        DataActionItem("Profil Fotoğrafını Kaldır", Icons.Rounded.NoPhotography) {
            confirmTitle = "Profil fotoğrafını kaldırmak istiyor musunuz?"; confirmAction = onRemovePhoto
        }
        Spacer(modifier = Modifier.height(16.dp))
        DataActionItem("Tüm Verileri Sıfırla", Icons.Rounded.DeleteForever, themeColors.error) {
            confirmTitle = "TÜM UYGULAMA VERİLERİNİ sıfırlamak istiyor musunuz? Bu işlem geri alınamaz."; confirmAction = onResetAll
        }
    }

    if (confirmAction != null) {
        ConfirmDialog("Emin misiniz?", confirmTitle, "EVET, SİL", themeColors.error, onConfirm = { confirmAction?.invoke(); confirmAction = null }) { confirmAction = null }
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
fun SettingsOptionDialog(title: String, options: List<Pair<String, String>>, currentValue: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = themeColors.surface,
        title = { Text(title, fontWeight = FontWeight.Black, color = themeColors.textPrimary) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(value) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = (value == currentValue), onClick = { onSelect(value) }, colors = RadioButtonDefaults.colors(selectedColor = themeColors.accent))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, color = themeColors.textPrimary)
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun PremiumThemeRow(selectedTheme: AppTheme, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBoxContainer(Icons.Rounded.Palette, themeColors.accent)
            Spacer(modifier = Modifier.width(16.dp))
            Text("Uygulama Teması", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = themeColors.textPrimary, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(selectedTheme.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = themeColors.accent)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.ChevronRight, null, tint = themeColors.textSecondary.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ThemeSelectionContent(selectedTheme: AppTheme, onThemeSelected: (AppTheme) -> Unit) {
    val themeColors = HavamaniaTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp)
    ) {
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

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(AppTheme.values()) { theme ->
                ThemeCardPremium(
                    theme = theme,
                    isSelected = selectedTheme == theme,
                    onClick = { onThemeSelected(theme) }
                )
            }
        }
    }
}

@Composable
fun ThemeCardPremium(theme: AppTheme, isSelected: Boolean, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    val themeStyleColors = ThemeFactory.createColors(theme)

    HavamaniaGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        alpha = if (isSelected) 0.95f else 0.45f,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        theme.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        ),
                        color = if (isSelected) themeColors.accent else themeColors.textPrimary
                    )
                    Text(
                        getThemeDesc(theme),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = themeColors.textSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            null,
                            tint = themeColors.accent,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(1.5.dp, themeColors.textMuted.copy(0.3f), CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gradient Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val previewColors = themeStyleColors.gradientPrimary
                previewColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}

fun getThemeDesc(theme: AppTheme): String = when(theme) {
    AppTheme.AUTO -> "Hava durumuna göre dinamik atmosfer"
    AppTheme.LIGHT -> "Temiz ve ferah gökyüzü tonları"
    AppTheme.DARK -> "Koyu ve modern gece atmosferi"
    AppTheme.SPRING -> "Taze bahar esintisi ve yeşil dokular"
    AppTheme.SUMMER -> "Sıcak yaz güneşi ve canlı renkler"
    AppTheme.AUTUMN -> "Huzurlu sonbahar ve toprak tonları"
    AppTheme.WINTER -> "Soğuk kış esintisi ve buzsu maviler"
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
fun SettingsActionRow(title: String, icon: ImageVector, contentColor: Color, onClick: () -> Unit = {}) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBoxContainer(icon, contentColor)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = contentColor.copy(alpha = 0.9f))
        }
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
