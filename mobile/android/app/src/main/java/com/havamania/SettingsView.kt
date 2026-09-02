package com.havamania

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.*
import com.havamania.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToCities: () -> Unit = {},
    onNavigateToSmartAlerts: () -> Unit = {},
    themeViewModel: ThemeViewModel = viewModel(),
    travelViewModel: TravelViewModel = viewModel(),
    aiHistoryViewModel: AiHistoryViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val openLegal = { url: String ->
        try {
            uriHandler.openUri(url)
        } catch (e: Exception) {
            Toast.makeText(context, "Tarayıcı açılamadı: $url", Toast.LENGTH_SHORT).show()
        }
    }

    val currentTheme by themeViewModel.currentTheme.collectAsState()
    val tempUnit by themeViewModel.tempUnit.collectAsState()
    val language by themeViewModel.language.collectAsState()
    val assistantTone by themeViewModel.assistantTone.collectAsState()

    val notificationsEnabled: Boolean by themeViewModel.notificationsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val defaultCity: com.havamania.GeocodingResultDto? by themeViewModel.defaultCity.collectAsStateWithLifecycle(initialValue = null)
    val locationMode: LocationMode by themeViewModel.locationMode.collectAsStateWithLifecycle(initialValue = LocationMode.MANUAL)
    val isPremium: Boolean by themeViewModel.isPremium.collectAsStateWithLifecycle(initialValue = false)

    val themeColors = HavamaniaTheme.colors
    val themeStyles = HavamaniaTheme.styles
    val responsive = LocalResponsiveValues.current
    val windowSize = LocalWindowSize.current

    var showThemeSheet by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showToneDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showComingSoonDialog by remember { mutableStateOf(false) }
    var comingSoonTitle by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "AYARLAR",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(themeStyles.pagePadding)
                .then(
                    if (windowSize.isTablet || windowSize.isLargeTablet)
                        Modifier.widthIn(max = responsive.maxContentWidth).align(Alignment.TopCenter)
                    else Modifier.fillMaxWidth()
                )
        ) {
            // 1. Profil & Hesap
            SettingsGroupLabel("HESAP")
            SettingsCard {
                SettingsNavRow("Profili Düzenle", "Kişisel bilgiler ve biyografi", Icons.Rounded.AccountCircle) {
                    onNavigateToEditProfile()
                }
                SettingsDivider()
                SettingsNavRow("Kayıtlı Şehirler", "Konum tercihlerinizi yönetin", Icons.Rounded.Map) {
                    onNavigateToCities()
                }
            }

            Spacer(modifier = Modifier.height(themeStyles.spacingLG))

            // 2. Kişiselleştirme
            SettingsGroupLabel("KİŞİSELLEŞTİRME")
            SettingsCard {
                SettingsClickRow("Görünüm", currentTheme.title, Icons.Rounded.Palette) {
                    showThemeSheet = true
                }
                SettingsDivider()
                SettingsClickRow("Asistan Üslubu", assistantTone.title, Icons.Rounded.AutoAwesome) {
                    showToneDialog = true
                }
                SettingsDivider()
                SettingsNavRow("Akıllı Uyarılar", "İlgi alanlarına göre bildirimler", Icons.Rounded.NotificationsActive) {
                    onNavigateToSmartAlerts()
                }
            }

            Spacer(modifier = Modifier.height(themeStyles.spacingLG))

            // 3. Genel
            SettingsGroupLabel("GENEL")
            SettingsCard {
                SettingsClickRow("Dil", if (language == "TR") "Türkçe" else "English", Icons.Rounded.Language) {
                    showLanguageDialog = true
                }
                SettingsDivider()
                SettingsClickRow("Birimler", if (tempUnit == TemperatureUnit.CELSIUS) "°C" else "°F", Icons.Rounded.Thermostat) {
                    themeViewModel.setTempUnit(if (tempUnit == TemperatureUnit.CELSIUS) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS)
                }
                SettingsDivider()
                SettingsToggleRow("Bildirimler", "Uygulama içi duyurular", Icons.Rounded.Notifications, notificationsEnabled) {
                    themeViewModel.setNotificationsEnabled(it)
                }
            }

            Spacer(modifier = Modifier.height(themeStyles.spacingLG))

            // 4. Premium
            SettingsGroupLabel("HAVAMANİA PREMIUM")
            PremiumSettingsCard(
                isPremium = isPremium,
                onClick = {
                    if (!isPremium) {
                        comingSoonTitle = "Havamania Premium yakında tüm özellikleri ile hizmetinizde olacak."
                        showComingSoonDialog = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(themeStyles.spacingLG))

            // 5. Destek & Yasal
            SettingsGroupLabel("DESTEK VE YASAL")
            SettingsCard {
                SettingsNavRow("KVKK Aydınlatma Metni", null, Icons.Rounded.Gavel) {
                    openLegal(LegalUrls.KVKK)
                }
                SettingsDivider()
                SettingsNavRow("Gizlilik Politikası", null, Icons.Rounded.PrivacyTip) {
                    openLegal(LegalUrls.PRIVACY_POLICY)
                }
                SettingsDivider()
                SettingsNavRow("Kullanım Koşulları", null, Icons.Rounded.Description) {
                    openLegal(LegalUrls.TERMS_OF_USE)
                }
                SettingsDivider()
                SettingsClickRow(
                    title = "Önbelleği Temizle",
                    value = "Hava verilerini yenile",
                    icon = Icons.Rounded.Cached,
                    isVertical = true
                ) {
                    travelViewModel.clearAllPlans()
                    comingSoonTitle = "Hava durumu önbelleği temizlendi."
                    showComingSoonDialog = true
                }
            }

            Spacer(modifier = Modifier.height(themeStyles.spacingLG))

            // 6. Hesap Yönetimi
            SettingsGroupLabel("HESAP YÖNETİMİ")
            SettingsCard {
                SettingsClickRow("Çıkış Yap", null, Icons.Rounded.Logout, contentColor = themeColors.textPrimary) {
                    showLogoutDialog = true
                }
                SettingsDivider()
                SettingsClickRow(
                    title = "Hesabımı Sil",
                    value = "Tüm veriler kalıcı olarak silinir",
                    icon = Icons.Rounded.DeleteForever,
                    contentColor = themeColors.error,
                    isVertical = true
                ) {
                    showDeleteDialog = true
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            val appVersion = BuildConfig.VERSION_NAME
            val uid = authViewModel.currentUser?.uid ?: "legacy"
            val context = LocalContext.current
            val logoResId = context.resources.getIdentifier("havamania_logo_clean", "drawable", context.packageName)

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().alpha(0.5f)) {
                if (logoResId != 0) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = logoResId),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                    Spacer(Modifier.height(12.dp))
                }

                Text(
                    text = "Havamania v$appVersion",
                    style = HavamaniaTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                    color = themeColors.textMuted
                )
                Text(
                    text = "Teknoloji partneri: Altıkod Digital Solutions",
                    style = HavamaniaTheme.typography.caption.copy(fontSize = 8.sp),
                    color = themeColors.textMuted
                )

                Spacer(Modifier.height(8.dp))

            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showThemeSheet) {
        ThemeSelectionDialog(
            currentTheme = currentTheme,
            onThemeSelected = { themeViewModel.setTheme(it); showThemeSheet = false },
            onDismiss = { showThemeSheet = false }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = language,
            onLanguageSelected = { themeViewModel.setLanguage(it); showLanguageDialog = false },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showToneDialog) {
        ToneSelectionDialog(
            currentTone = assistantTone,
            onToneSelected = { themeViewModel.setAssistantTone(it); showToneDialog = false },
            onDismiss = { showToneDialog = false }
        )
    }

    if (showLogoutDialog) {
        HavamaniaDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = "Çıkış Yap?",
            text = "Hesabınızdan çıkış yapmak istediğinize emin misiniz?",
            confirmText = "Çıkış Yap",
            dismissText = "İptal",
            onConfirm = {
                authViewModel.signOut()
                showLogoutDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        AccountDeleteDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = { password ->
                authViewModel.deleteAccount(password = password, onComplete = { success, _ ->
                    if (success) {
                        showDeleteDialog = false
                    }
                })
            }
        )
    }

    if (showComingSoonDialog) {
        HavamaniaDialog(
            onDismissRequest = { showComingSoonDialog = false },
            title = "BİLGİ",
            text = comingSoonTitle,
            confirmText = "TAMAM",
            onConfirm = { showComingSoonDialog = false }
        )
    }
}

@Composable
private fun SettingsGroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
        color = HavamaniaTheme.colors.accent,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HavamaniaTheme.colors.surface.copy(alpha = if (HavamaniaTheme.colors.isDark) 0.5f else 0.8f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, HavamaniaTheme.colors.border.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsNavRow(title: String, subtitle: String?, icon: ImageVector, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = themeColors.accent.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = themeColors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = themeColors.textSecondary)
            }
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = themeColors.textMuted)
    }
}

@Composable
private fun SettingsClickRow(
    title: String,
    value: String?,
    icon: ImageVector,
    contentColor: Color = HavamaniaTheme.colors.textPrimary,
    isVertical: Boolean = false,
    onClick: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))

        if (isVertical) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.sp
                    ),
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false
                )
                if (value != null) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                        color = themeColors.textSecondary.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                }
            }
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = themeColors.accent
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = themeColors.textMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(title: String, subtitle: String?, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val themeColors = HavamaniaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = themeColors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = themeColors.textSecondary)
            }
        }
        HavamaniaToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsDivider() {
    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = HavamaniaTheme.colors.divider.copy(alpha = 0.05f))
}

@Composable
private fun PremiumSettingsCard(isPremium: Boolean, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    val themeStyles = HavamaniaTheme.styles

    Surface(
        onClick = onClick,
        color = if (isPremium) themeColors.success.copy(alpha = 0.1f) else themeColors.accent.copy(alpha = 0.1f),
        shape = RoundedCornerShape(themeStyles.radiusMedium),
        border = BorderStroke(1.dp, if (isPremium) themeColors.success.copy(alpha = 0.3f) else themeColors.accent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(if (isPremium) themeColors.success.copy(alpha = 0.2f) else themeColors.accent.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPremium) Icons.Rounded.VerifiedUser else Icons.Rounded.AutoAwesome,
                    null,
                    tint = if (isPremium) themeColors.success else themeColors.accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isPremium) "Havamania Premium Üyesi" else "Havamania Premium",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                    color = themeColors.textPrimary
                )
                Text(
                    if (isPremium) "Tüm özellikler açık." else "Çok yakında hizmetinizde.",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.textSecondary
                )
            }
            if (!isPremium) {
                Icon(Icons.Rounded.ChevronRight, null, tint = themeColors.accent)
            }
        }
    }
}

@Composable
fun ThemeSelectionDialog(currentTheme: AppTheme, onThemeSelected: (AppTheme) -> Unit, onDismiss: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    HavamaniaDialog(
        onDismissRequest = onDismiss,
        title = "Görünüm Seç",
        text = "",
        confirmText = "Kapat",
        onConfirm = onDismiss,
        content = {
            Column {
                AppTheme.entries.filter { it != AppTheme.AUTO }.forEach { theme ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onThemeSelected(theme) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = theme == currentTheme, onClick = { onThemeSelected(theme) })
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(theme.title, color = themeColors.textPrimary)
                    }
                }
            }
        }
    )
}

@Composable
fun LanguageSelectionDialog(currentLanguage: String, onLanguageSelected: (String) -> Unit, onDismiss: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    HavamaniaDialog(
        onDismissRequest = onDismiss,
        title = "Dil Seç",
        text = "",
        confirmText = "Kapat",
        onConfirm = onDismiss,
        content = {
            Column {
                listOf("TR" to "Türkçe", "EN" to "English").forEach { (code, name) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onLanguageSelected(code) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = code == currentLanguage, onClick = { onLanguageSelected(code) })
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(name, color = themeColors.textPrimary)
                    }
                }
            }
        }
    )
}

@Composable
fun ToneSelectionDialog(currentTone: AssistantTone, onToneSelected: (AssistantTone) -> Unit, onDismiss: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    HavamaniaDialog(
        onDismissRequest = onDismiss,
        title = "Asistan Üslubu",
        text = "",
        confirmText = "Kapat",
        onConfirm = onDismiss,
        content = {
            Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                AssistantTone.entries.forEach { tone ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onToneSelected(tone) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = tone == currentTone, onClick = { onToneSelected(tone) })
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(tone.title, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                            Text(tone.description, style = MaterialTheme.typography.bodySmall, color = themeColors.textSecondary)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun AccountDeleteDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmText by remember { mutableStateOf("") }
    val themeColors = HavamaniaTheme.colors
    val themeStyles = HavamaniaTheme.styles

    HavamaniaDialog(
        onDismissRequest = onDismiss,
        title = "Hesabımı Sil",
        text = "Hesabınız, verileriniz ve analizleriniz kalıcı olarak silinecektir. Devam etmek için şifrenizi girin ve 'SİL' yazın.",
        confirmText = "HESABI SİL",
        confirmColor = themeColors.error,
        confirmEnabled = confirmText == "SİL" && password.isNotBlank(),
        onConfirm = { onConfirm(password) },
        content = {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Şifre") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    shape = RoundedCornerShape(themeStyles.radiusSmall)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmText,
                    onValueChange = { confirmText = it },
                    label = { Text("Onaylamak için 'SİL' yazın") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(themeStyles.radiusSmall),
                    placeholder = { Text("SİL") }
                )
            }
        }
    )
}
