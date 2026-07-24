package com.havamania

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.*
import com.havamania.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PersonalizationScreen(
    profileViewModel: ProfileViewModel = viewModel(),
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    val themeStyles = HavamaniaTheme.styles
    val profileState by profileViewModel.profileState.collectAsState()

    var selectedInterests by remember { mutableStateOf(setOf<String>()) }
    var selectedStyles by remember { mutableStateOf(setOf<String>()) }
    var weatherPrefs by remember { mutableStateOf(WeatherPreferences()) }

    var expandedCategoryId by remember { mutableStateOf<String?>(InterestsData.categories.firstOrNull()?.title) }

    LaunchedEffect(profileState) {
        if (profileState is ProfileState.Success) {
            val profile = (profileState as ProfileState.Success).profile
            selectedInterests = profile.personalizationProfile?.selectedInterests?.toSet() ?: emptySet()
            selectedStyles = profile.personalizationProfile?.travelStyles?.toSet() ?: emptySet()
            weatherPrefs = profile.personalizationProfile?.weatherPreferences ?: WeatherPreferences()
        }
    }

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "KİŞİSELLEŞTİRME",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(themeStyles.pagePadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Havamania'yı Sana Özel Hale Getirelim",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                textAlign = TextAlign.Center,
                color = themeColors.textPrimary
            )
            Text(
                "Seçtiğin ilgi alanları ve tercihler, asistan önerilerini ve hava analizlerini sana özel hale getirir.",
                style = MaterialTheme.typography.bodyMedium,
                color = themeColors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = themeStyles.spacingSmall)
            )

            Spacer(Modifier.height(themeStyles.spacingExtraLarge))

            // Accordion Sections
            for (category in InterestsData.categories) {
                PersonalizationAccordionItem(
                    category = category,
                    isSelected = expandedCategoryId == category.title,
                    onToggle = { expandedCategoryId = if (expandedCategoryId == category.title) null else category.title },
                    selectedInterests = selectedInterests,
                    onInterestToggle = { id ->
                        selectedInterests = if (selectedInterests.contains(id)) selectedInterests - id else selectedInterests + id
                    }
                )
                Spacer(Modifier.height(themeStyles.spacingMedium))
            }

            // Travel Styles Accordion
            PersonalizationAccordionItemSimple(
                title = "Seyahat Tarzın",
                icon = Icons.Rounded.FlightTakeoff,
                isSelected = expandedCategoryId == "TRAVEL_STYLES",
                onToggle = { expandedCategoryId = if (expandedCategoryId == "TRAVEL_STYLES") null else "TRAVEL_STYLES" }
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(themeStyles.spacingSmall),
                    verticalArrangement = Arrangement.spacedBy(themeStyles.spacingSmall)
                ) {
                    for (style in PersonalizationDefaults.TRAVEL_STYLES) {
                        HavamaniaChip(
                            selected = selectedStyles.contains(style),
                            onClick = {
                                selectedStyles = if (selectedStyles.contains(style)) selectedStyles - style else selectedStyles + style
                            },
                            label = style,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }
            }

            Spacer(Modifier.height(themeStyles.spacingMedium))

            // Weather Prefs Accordion
            PersonalizationAccordionItemSimple(
                title = "Hava Hassasiyetlerin",
                icon = Icons.Rounded.Thermostat,
                isSelected = expandedCategoryId == "WEATHER_PREFS",
                onToggle = { expandedCategoryId = if (expandedCategoryId == "WEATHER_PREFS") null else "WEATHER_PREFS" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(themeStyles.spacingSmall)) {
                    PreferenceSwitch("Sıcağı Severim", weatherPrefs.likesHeat) { weatherPrefs = weatherPrefs.copy(likesHeat = it) }
                    PreferenceSwitch("Serin Havayı Severim", weatherPrefs.likesCool) { weatherPrefs = weatherPrefs.copy(likesCool = it) }
                    PreferenceSwitch("Yağmura Hassasım", weatherPrefs.rainSensitive) { weatherPrefs = weatherPrefs.copy(rainSensitive = it) }
                    PreferenceSwitch("Rüzgara Hassasım", weatherPrefs.windSensitive) { weatherPrefs = weatherPrefs.copy(windSensitive = it) }
                    PreferenceSwitch("Yüksek UV Hassasiyeti", weatherPrefs.uvSensitive) { weatherPrefs = weatherPrefs.copy(uvSensitive = it) }
                }
            }

            Spacer(Modifier.height(themeStyles.spacingExtraLarge))

            HavamaniaPrimaryButton(
                text = "TERCİHLERİ KAYDET",
                onClick = {
                    profileViewModel.updatePersonalization(
                        interests = selectedInterests.toList(),
                        travelStyles = selectedStyles.toList(),
                        weatherPrefs = weatherPrefs
                    )
                    onComplete()
                }
            )

            TextButton(
                onClick = onComplete,
                modifier = Modifier.padding(top = themeStyles.spacingSmall).minimumInteractiveComponentSize()
            ) {
                Text("Şimdilik Atla", color = themeColors.textMuted, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalizationAccordionItem(
    category: InterestCategory,
    isSelected: Boolean,
    onToggle: () -> Unit,
    selectedInterests: Set<String>,
    onInterestToggle: (String) -> Unit
) {
    val themeStyles = HavamaniaTheme.styles
    val themeColors = HavamaniaTheme.colors

    HavamaniaGlassCard(
        onClick = onToggle,
        alpha = if (isSelected) 0.6f else 0.3f,
        cornerRadius = themeStyles.radiusMedium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(category.icon, null, tint = themeColors.accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(themeStyles.spacingMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
                if (!isSelected) {
                    Text(category.description, style = MaterialTheme.typography.bodySmall, color = themeColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(
                if (isSelected) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                null,
                tint = themeColors.textMuted
            )
        }

        AnimatedVisibility(
            visible = isSelected,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(themeStyles.spacingMedium))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(themeStyles.spacingSmall),
                    verticalArrangement = Arrangement.spacedBy(themeStyles.spacingSmall)
                ) {
                    for (interest in category.interests) {
                        HavamaniaChip(
                            selected = selectedInterests.contains(interest.id),
                            onClick = { onInterestToggle(interest.id) },
                            label = interest.label,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalizationAccordionItemSimple(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val themeStyles = HavamaniaTheme.styles
    val themeColors = HavamaniaTheme.colors

    HavamaniaGlassCard(
        onClick = onToggle,
        alpha = if (isSelected) 0.6f else 0.3f,
        cornerRadius = themeStyles.radiusMedium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(themeStyles.spacingMedium))
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary, modifier = Modifier.weight(1f))
            Icon(
                if (isSelected) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                null,
                tint = themeColors.textMuted
            )
        }

        AnimatedVisibility(
            visible = isSelected,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(themeStyles.spacingMedium))
                content()
            }
        }
    }
}

@Composable
private fun PreferenceSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = HavamaniaTheme.styles.spacingExtraSmall)
            .minimumInteractiveComponentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = HavamaniaTheme.colors.textPrimary)
        HavamaniaToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}
