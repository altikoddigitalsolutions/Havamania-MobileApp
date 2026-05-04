package com.havamania

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    themeViewModel: ThemeViewModel = viewModel()
) {
    val currentName by themeViewModel.userName.collectAsState()
    val currentBio by themeViewModel.userBio.collectAsState()
    val themeColors = HavamaniaTheme.colors

    var name by remember { mutableStateOf(currentName) }
    var bio by remember { mutableStateOf(currentBio) }

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
                .padding(24.dp)
        ) {
            Text(
                "KİŞİSEL BİLGİLER",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Black),
                color = themeColors.accent.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            HavamaniaGlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Ad Soyad", color = themeColors.textSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Bio / Açıklama", color = themeColors.textSecondary) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(12.dp),
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
            }

            Spacer(modifier = Modifier.height(32.dp))

            HavamaniaPrimaryButton(
                text = "Değişiklikleri Kaydet",
                onClick = {
                    themeViewModel.updateProfile(name, bio)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
