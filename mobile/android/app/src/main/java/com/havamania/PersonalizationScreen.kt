package com.havamania

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizationScreen(
    profileViewModel: ProfileViewModel = viewModel(),
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    val profileState by profileViewModel.profileState.collectAsState()

    var selectedInterests by remember { mutableStateOf(setOf<String>()) }
    var selectedStyles by remember { mutableStateOf(setOf<String>()) }
    var weatherPrefs by remember { mutableStateOf(WeatherPreferences()) }

    LaunchedEffect(profileState) {
        if (profileState is ProfileState.Success) {
            // Mevcut verileri yükle (varsa)
            // Not: UserProfile modeline eklenen alanlara göre burası güncellenebilir
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Kişiselleştirme", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Havamania'yı Sana Özel Hale Getirelim",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                textAlign = TextAlign.Center
            )
            Text(
                "Seçtiğin ilgi alanları ve tercihler, asistan önerilerini ve hava analizlerini sana özel hale getirir.",
                style = MaterialTheme.typography.bodyMedium,
                color = themeColors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(32.dp))

            // İlgi Alanları
            SectionTitle("İlgi Alanların")
            InterestsData.categories.forEach { category ->
                Text(
                    category.title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = themeColors.textPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    category.interests.forEach { interest ->
                        FilterChip(
                            selected = selectedInterests.contains(interest.id),
                            onClick = {
                                selectedInterests = if (selectedInterests.contains(interest.id)) {
                                    selectedInterests - interest.id
                                } else {
                                    selectedInterests + interest.id
                                }
                            },
                            label = { Text(interest.label) },
                            leadingIcon = { Icon(interest.icon, null, modifier = Modifier.size(16.dp)) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeColors.accent,
                                selectedLabelColor = themeColors.onAccent,
                                selectedLeadingIconColor = themeColors.onAccent
                            )
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Seyahat Tarzı
            SectionTitle("Seyahat Tarzın")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PersonalizationDefaults.TRAVEL_STYLES.forEach { style ->
                    FilterChip(
                        selected = selectedStyles.contains(style),
                        onClick = {
                            selectedStyles = if (selectedStyles.contains(style)) {
                                selectedStyles - style
                            } else {
                                selectedStyles + style
                            }
                        },
                        label = { Text(style) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = themeColors.accent,
                            selectedLabelColor = themeColors.onAccent
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Hava Tercihleri
            SectionTitle("Hava Hassasiyetlerin")
            PreferenceSwitch("Sıcağı Severim", weatherPrefs.likesHeat) { weatherPrefs = weatherPrefs.copy(likesHeat = it) }
            PreferenceSwitch("Serin Havayı Severim", weatherPrefs.likesCool) { weatherPrefs = weatherPrefs.copy(likesCool = it) }
            PreferenceSwitch("Yağmura Hassasım", weatherPrefs.rainSensitive) { weatherPrefs = weatherPrefs.copy(rainSensitive = it) }
            PreferenceSwitch("Rüzgara Hassasım", weatherPrefs.windSensitive) { weatherPrefs = weatherPrefs.copy(windSensitive = it) }
            PreferenceSwitch("Yüksek UV Hassasiyeti", weatherPrefs.uvSensitive) { weatherPrefs = weatherPrefs.copy(uvSensitive = it) }

            Spacer(Modifier.height(40.dp))

            HavamaniaPrimaryButton(
                text = "TERCİHLERİ KAYDET",
                onClick = {
                    profileViewModel.updatePersonalization(
                        interests = selectedInterests.toList(),
                        travelStyles = selectedStyles.toList(),
                        weatherPrefs = weatherPrefs
                    )
                    onComplete()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )

            TextButton(onClick = onComplete, modifier = Modifier.padding(top = 8.dp)) {
                Text("Şimdilik Atla", color = themeColors.textMuted)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
        color = HavamaniaTheme.colors.accent,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )
}

@Composable
private fun PreferenceSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = HavamaniaTheme.colors.accent)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}
