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
import androidx.compose.ui.text.style.TextAlign
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
    val profileState by profileViewModel.profileState.collectAsState()
    val themeColors = HavamaniaTheme.colors

    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var createdAt by remember { mutableLongStateOf(0L) }
    var uid by remember { mutableStateOf("") }

    LaunchedEffect(profileState) {
        if (profileState is ProfileState.Success) {
            val profile = (profileState as ProfileState.Success).profile
            name = profile.name
            bio = profile.bio
            email = profile.email
            createdAt = profile.createdAt
            uid = profile.uid
        }
    }

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. HESAP BİLGİLERİ KARTI
            SectionLabel("HESAP BİLGİLERİ")
            HavamaniaGlassCard(alpha = 0.5f) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Ad Soyad", color = themeColors.textMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Rounded.Person, null, tint = themeColors.accent) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            unfocusedBorderColor = themeColors.border.copy(alpha = 0.2f),
                            focusedBorderColor = themeColors.accent,
                            focusedTextColor = themeColors.textPrimary,
                            unfocusedTextColor = themeColors.textPrimary,
                            cursorColor = themeColors.accent
                        )
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("E-posta (Salt Okunur)", color = themeColors.textMuted) },
                        modifier = Modifier.fillMaxWidth().alpha(0.7f),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Rounded.Email, null, tint = themeColors.textMuted) },
                        trailingIcon = { Icon(Icons.Rounded.Lock, null, tint = themeColors.textMuted, modifier = Modifier.size(16.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            unfocusedBorderColor = themeColors.border.copy(alpha = 0.1f),
                            focusedBorderColor = themeColors.border.copy(alpha = 0.1f),
                            focusedTextColor = themeColors.textSecondary,
                            unfocusedTextColor = themeColors.textSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2. HAVA KİMLİĞİ (BIO) KARTI
            SectionLabel("HAVA KİMLİĞİ")
            HavamaniaGlassCard(alpha = 0.5f) {
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Motto / Bio", color = themeColors.textMuted) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("Hava durumuna bakış açını anlatan kısa bir yazı...", fontSize = 14.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        unfocusedBorderColor = themeColors.border.copy(alpha = 0.2f),
                        focusedBorderColor = themeColors.accent,
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary,
                        cursorColor = themeColors.accent
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 3. SİSTEM BİLGİLERİ KARTI
            SectionLabel("SİSTEM BİLGİLERİ")
            HavamaniaGlassCard(alpha = 0.4f) {
                val dateFormat = remember { SimpleDateFormat("d MMMM yyyy", Locale("tr")) }
                val dateStr = if (createdAt > 0) dateFormat.format(Date(createdAt)) else "---"

                ProfileInfoRow(
                    icon = Icons.Rounded.History,
                    label = "Kayıt Tarihi",
                    value = dateStr
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            HavamaniaPrimaryButton(
                text = "DEĞİŞİKLİKLERİ KAYDET",
                onClick = {
                    profileViewModel.updateProfile(name, bio)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Debug UID
            if (uid.isNotEmpty()) {
                Text(
                    text = "System ID: ${uid.takeLast(8).uppercase()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = themeColors.textMuted.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = HavamaniaTheme.colors.textPrimary
) {
    val themeColors = HavamaniaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(themeColors.accent.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = themeColors.accent.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = themeColors.textMuted)
            Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = valueColor)
        }
    }
}
