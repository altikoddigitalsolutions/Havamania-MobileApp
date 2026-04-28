package com.havamania

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.HavamaniaTheme

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme

    // Kullanıcının ilgi alanlarını tutan state
    val selectedInterests = remember { mutableStateListOf("Balıkçılık", "Kamp", "Spor") }
    val availableInterests = listOf(
        "Balıkçılık", "Kamp", "Yürüyüş", "Deniz",
        "Kayak", "Fotoğrafçılık", "Spor", "Bisiklet",
        "Bahçe İşleri", "Yüzme", "Dağcılık"
    )

    HavamaniaTheme {
        Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Üst Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = colorScheme.onBackground)
                    }
                    Text(
                        text = "PROFİL",
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Rounded.Settings, null, tint = colorScheme.onBackground)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                EnhancedProfileHeader("Ahmet Yılmaz", "Hava durumu meraklısı ve AI kaşifi 🌤️")
                Spacer(modifier = Modifier.height(32.dp))

                SectionHeader("İLGİ ALANLARINIZ")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableInterests.forEach { interest ->
                        val isSelected = selectedInterests.contains(interest)
                        FilterChip(
                            selected = isSelected,
                            enabled = true,
                            onClick = {
                                if (isSelected) selectedInterests.remove(interest)
                                else selectedInterests.add(interest)
                            },
                            label = { Text(interest) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = colorScheme.onSurfaceVariant,
                                selectedContainerColor = colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = colorScheme.primary,
                                selectedLeadingIconColor = colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                SectionHeader("İSTATİSTİKLER")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCardPremium(label = "Şehir", value = "12", icon = Icons.Rounded.LocationOn, Modifier.weight(1f))
                    StatCardPremium(label = "AI Sorusu", value = "148", icon = Icons.Rounded.AutoAwesome, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(28.dp))
                SectionHeader("HIZLI İŞLEMLER")
                QuickActionsGrid(onNavigateToSettings = onNavigateToSettings)

                Spacer(modifier = Modifier.height(32.dp))
                SectionHeader("UYGULAMA AYARLARI")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        SettingsRowPremium(title = "Bildirim Tercihleri", icon = Icons.Rounded.NotificationsActive)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = colorScheme.outline.copy(alpha = 0.1f))
                        SettingsRowPremium(title = "Premium Üyelik", icon = Icons.Rounded.WorkspacePremium, isPremium = true)
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    val colorScheme = MaterialTheme.colorScheme
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Black),
        color = colorScheme.primary.copy(alpha = 0.8f),
        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
    )
}

@Composable
fun StatCardPremium(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.8f)),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = colorScheme.onSurface)
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold), color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun EnhancedProfileHeader(name: String, bio: String) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(110.dp).clip(CircleShape).background(colorScheme.primary.copy(alpha = 0.1f)))
            Box(
                modifier = Modifier.size(90.dp).clip(CircleShape).background(colorScheme.surfaceVariant).border(1.5.dp, colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Person, null, tint = colorScheme.onSurface, modifier = Modifier.size(48.dp))
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(name, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold), color = colorScheme.onSurface)
        Text(bio, style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f), textAlign = TextAlign.Center)
    }
}

@Composable
fun QuickActionsGrid(onNavigateToSettings: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            QuickActionItem("Şehirleri Yönet", Icons.Rounded.Map, Modifier.weight(1f))
            QuickActionItem("AI Geçmişi", Icons.Rounded.History, Modifier.weight(1f))
        }
    }
}

@Composable
fun QuickActionItem(title: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.05f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
        }
    }
}

@Composable
fun SettingsRowPremium(title: String, icon: ImageVector, isPremium: Boolean = false, onClick: () -> Unit = {}) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (isPremium) Color(0xFFFBBF24) else colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, null, tint = colorScheme.onSurfaceVariant.copy(alpha = 0.2f), modifier = Modifier.size(18.dp))
        }
    }
}
