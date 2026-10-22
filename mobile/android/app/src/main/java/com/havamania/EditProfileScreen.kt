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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    val userProfileRepository = remember { UserProfileRepository.getInstance() }
    val profile by userProfileRepository.profile.collectAsStateWithLifecycle()
    val owner by profileViewModel.activeUid.collectAsStateWithLifecycle()
    var name by rememberSaveable(owner) { mutableStateOf("") }
    var bio by rememberSaveable(owner) { mutableStateOf("") }
    var initialized by rememberSaveable(owner) { mutableStateOf(false) }
    val saveState by profileViewModel.saveState.collectAsStateWithLifecycle()
    val saving = saveState == ProfileSaveState.Saving
    val email = profile?.takeIf { it.uid == owner }?.email.orEmpty()
    val createdAt = profile?.takeIf { it.uid == owner }?.createdAt ?: 0L

    LaunchedEffect(profile, owner) {
        profile?.takeIf { it.uid == owner }?.let {
            if (!initialized) {
                name = it.name
                bio = it.bio
                initialized = true
            }
        }
    }
    LaunchedEffect(saveState) {
        if (saveState == ProfileSaveState.Saved) {
            profileViewModel.resetSaveState()
            onBack()
        }
    }

    val responsive = LocalResponsiveValues.current
    val windowSize = LocalWindowSize.current

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "PROFİLİ DÜZENLE",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
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
                        onValueChange = { if (!saving && it.length <= 50) name = it },
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
                Column {
                    TextField(
                        value = bio,
                        enabled = !saving,
                        label = { Text("Hava kimliği") },
                        onValueChange = { if (!saving && it.length <= 500) bio = it },
                        placeholder = { Text("Hava durumuna bakış açını anlatan kısa bir yazı...", fontSize = 14.sp, color = themeColors.textMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
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

            (saveState as? ProfileSaveState.Failed)?.let { failure ->
                Text(
                    text = failure.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 12.dp).semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            HavamaniaPrimaryButton(
                text = "DEĞİŞİKLİKLERİ KAYDET",
                enabled = initialized && !saving,
                isLoading = saving,
                onClick = {
                    profileViewModel.updateProfile(name, bio)
                }
            )

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun ReadOnlyField(value: String, label: String, icon: ImageVector) {
    val colors = HavamaniaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .alpha(0.7f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = HavamaniaTheme.typography.caption, color = colors.textMuted)
            Text(value, style = HavamaniaTheme.typography.bodyMedium, color = colors.textSecondary)
        }
        Icon(Icons.Rounded.Lock, null, tint = colors.textMuted.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
    }
}
