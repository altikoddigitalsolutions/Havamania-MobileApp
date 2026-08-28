package com.havamania

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val themeColors = HavamaniaTheme.colors
    val themeStyles = HavamaniaTheme.styles

    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var createdAt by remember { mutableLongStateOf(0L) }

    val userProfileRepository = remember { UserProfileRepository.getInstance() }
    val profile by userProfileRepository.profile.collectAsState()

    LaunchedEffect(profile) {
        profile?.let {
            name = it.name
            bio = it.bio
            email = it.email
            createdAt = it.createdAt
        }
    }

    val responsive = LocalResponsiveValues.current
    val windowSize = LocalWindowSize.current

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "PROFILI DÜZENLE",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = themeStyles.pagePadding)
                .then(
                    if (windowSize.isTablet || windowSize.isLargeTablet)
                        Modifier.widthIn(max = responsive.maxContentWidth).align(Alignment.TopCenter)
                    else Modifier.fillMaxWidth()
                )
        ) {
            Spacer(modifier = Modifier.height(themeStyles.spacingMD))

            // 1. HESAP BİLGİLERİ
            SectionLabel("HESAP BİLGİLERİ")
            HavamaniaGlassCard(alpha = 0.4f) {
                Column(verticalArrangement = Arrangement.spacedBy(themeStyles.spacingMD)) {
                    HavamaniaTextField(
                        value = name,
                        onValueChange = { if (it.length <= 50) name = it },
                        placeholder = "Ad Soyad",
                        leadingIcon = Icons.Rounded.Person
                    )

                    ReadOnlyField(
                        value = email,
                        label = "E-posta (Değiştirilemez)",
                        icon = Icons.Rounded.Email
                    )
                }
            }

            Spacer(modifier = Modifier.height(themeStyles.spacingLG))

            // 2. HAVA KİMLİĞİ (BIO)
            SectionLabel("HAVA KİMLİĞİ")
            HavamaniaGlassCard(alpha = 0.4f) {
                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 500) bio = it },
                    placeholder = { Text("Hava durumuna bakış açını anlatan kısa bir yazı...", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        unfocusedBorderColor = themeColors.border.copy(alpha = 0.1f),
                        focusedBorderColor = themeColors.accent,
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary,
                        cursorColor = themeColors.accent
                    )
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${bio.length}/500",
                    style = HavamaniaTheme.typography.caption,
                    color = themeColors.textMuted,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            Spacer(modifier = Modifier.height(themeStyles.spacingLG))

            // 3. SİSTEM BİLGİLERİ
            SectionLabel("SİSTEM BİLGİLERİ")
            HavamaniaGlassCard(alpha = 0.3f) {
                val dateFormat = remember { SimpleDateFormat("d MMMM yyyy", Locale("tr")) }
                val dateStr = if (createdAt > 0) dateFormat.format(Date(createdAt)) else "---"

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.History, null, tint = themeColors.accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Kayıt Tarihi", style = MaterialTheme.typography.labelSmall, color = themeColors.textMuted)
                        Text(dateStr, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = themeColors.textPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            HavamaniaPrimaryButton(
                text = "DEĞİŞİKLİKLERİ KAYDET",
                onClick = {
                    profileViewModel.updateProfile(name, bio)
                    onBack()
                }
            )

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun ReadOnlyField(value: String, label: String, icon: ImageVector) {
    val colors = HavamaniaTheme.colors
    Surface(
        color = colors.surface.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().alpha(0.7f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = HavamaniaTheme.typography.caption, color = colors.textMuted)
                Text(value, style = HavamaniaTheme.typography.bodyMedium, color = colors.textSecondary)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.Lock, null, tint = colors.textMuted.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
        }
    }
}
