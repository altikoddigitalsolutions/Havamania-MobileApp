package com.havamania

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
    themeViewModel: ThemeViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val currentTheme by themeViewModel.currentTheme.collectAsState()

    var showThemeSheet by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var animationsEnabled by remember { mutableStateOf(true) }

    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "AYARLAR",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                SettingsGroupLabel("GENEL")
                SettingsGroupCard {
                    SettingsNavRow(
                        title = "Dil",
                        subtitle = "Türkçe",
                        icon = Icons.Rounded.Language
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        title = "Sıcaklık Birimi",
                        subtitle = "Celsius (°C)",
                        icon = Icons.Rounded.Thermostat
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                SettingsGroupLabel("GÖRÜNÜM")
                SettingsGroupCard {
                    PremiumThemeRow(
                        selectedTheme = currentTheme,
                        onClick = { showThemeSheet = true }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        title = "Animasyonlar",
                        subtitle = "Görsel geçiş efektleri",
                        icon = Icons.Rounded.AutoAwesomeMotion,
                        checked = animationsEnabled,
                        onCheckedChange = { animationsEnabled = it }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                SettingsGroupLabel("BİLDİRİMLER")
                SettingsGroupCard {
                    SettingsToggleRow(
                        title = "Hava Durumu Uyarıları",
                        subtitle = "Anlık bildirimler al",
                        icon = Icons.Rounded.Notifications,
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                SettingsGroupLabel("HESAP")
                SettingsGroupCard {
                    SettingsNavRow(
                        title = "Profil Bilgileri",
                        icon = Icons.Rounded.AccountCircle
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        title = "Çıkış Yap",
                        icon = Icons.Rounded.Logout,
                        contentColor = Color(0xFFEF4444)
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "Havamania v1.1.0",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        if (showThemeSheet) {
            ModalBottomSheet(
                onDismissRequest = { showThemeSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurface.copy(0.2f)) }
            ) {
                ThemeSelectionContent(
                    selectedTheme = currentTheme,
                    onThemeSelected = {
                        themeViewModel.setTheme(it)
                        showThemeSheet = false
                    }
                )
            }
        }
    }
}

@Composable
fun PremiumThemeRow(
    selectedTheme: AppTheme,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Palette,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                "Uygulama Teması",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    selectedTheme.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Rounded.ChevronRight,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ThemeSelectionContent(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp)
    ) {
        Text(
            "Tema Seçin",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Havamania deneyiminizi kişiselleştirin",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTheme.values().toList().forEach { theme ->
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
fun ThemeCardPremium(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f)
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        theme.title,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, fontSize = 16.sp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (theme == AppTheme.AUTO) {
                        Text(
                            "Hava durumuna ve atmosfere göre uygun tema uygulanır",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                if (isSelected) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (theme != AppTheme.AUTO) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemeColorBox(theme)
                }
            }
        }
    }
}

@Composable
fun ThemeColorBox(theme: AppTheme) {
    val colors = when(theme) {
        AppTheme.LIGHT -> listOf(Color(0xFFF5F7FA), Color(0xFFEEF2F7), Color(0xFF0077FF))
        AppTheme.DARK -> listOf(Color(0xFF0B0F14), Color(0xFF1A2230), Color(0xFF00C2FF))
        AppTheme.SPRING -> listOf(Color(0xFFF0FFF4), Color(0xFFE6F9ED), Color(0xFF4ADE80))
        AppTheme.SUMMER -> listOf(Color(0xFFE0F7FF), Color(0xFFD6F4FF), Color(0xFF00B4D8))
        AppTheme.AUTUMN -> listOf(Color(0xFFFFF7ED), Color(0xFFFFEAD5), Color(0xFFF97316))
        AppTheme.WINTER -> listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFF60A5FA))
        else -> emptyList()
    }

    colors.forEach { color ->
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 14.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color)
                .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp))
        )
    }
}

@Composable
fun SettingsGroupLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Black
        ),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
    )
}

@Composable
fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth(),
        content = content
    )
}

@Composable
fun SettingsNavRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit = {}
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBoxContainer(icon, MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        IconBoxContainer(icon, MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.75f)
        )
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    icon: ImageVector,
    contentColor: Color,
    onClick: () -> Unit = {}
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBoxContainer(icon, contentColor)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = contentColor.copy(alpha = 0.9f))
        }
    }
}

@Composable
private fun IconBoxContainer(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    )
}
