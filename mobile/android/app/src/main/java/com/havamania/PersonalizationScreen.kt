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

    val responsive = LocalResponsiveValues.current
    val windowSize = LocalWindowSize.current

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "KIŞISELLEŞTIRME",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(themeStyles.pagePadding)
                .then(
                    if (windowSize.isTablet || windowSize.isLargeTablet)
                        Modifier.widthIn(max = responsive.maxContentWidth).align(Alignment.TopCenter)
                    else Modifier.fillMaxWidth()
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Havamania'yı Sana Özel Hale Getirelim",
                style = HavamaniaTheme.typography.screenTitle.copy(fontWeight = FontWeight.Black),
                textAlign = TextAlign.Center,
                color = themeColors.textPrimary
            )
            Text(
                "Seçtiğin ilgi alanları ve tercihler, asistan önerilerini ve hava analizlerini sana özel hale getirir.",
                style = HavamaniaTheme.typography.bodyMedium,
                color = themeColors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = themeStyles.spacingSM)
            )

            Spacer(Modifier.height(themeStyles.spacingXXL))

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
                Spacer(Modifier.height(themeStyles.spacingMD))
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
                    horizontalArrangement = Arrangement.spacedBy(themeStyles.spacingSM),
                    verticalArrangement = Arrangement.spacedBy(themeStyles.spacingSM)
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

            Spacer(Modifier.height(themeStyles.spacingMD))

            // Weather Prefs Accordion
            PersonalizationAccordionItemSimple(
                title = "Hava Hassasiyetlerin",
                icon = Icons.Rounded.Thermostat,
                isSelected = expandedCategoryId == "WEATHER_PREFS",
                onToggle = { expandedCategoryId = if (expandedCategoryId == "WEATHER_PREFS") null else "WEATHER_PREFS" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(themeStyles.spacingSM)) {
                    PreferenceSwitch("Sıcağı Severim", weatherPrefs.likesHeat) { weatherPrefs = weatherPrefs.copy(likesHeat = it) }
                    PreferenceSwitch("Serin Havayı Severim", weatherPrefs.likesCool) { weatherPrefs = weatherPrefs.copy(likesCool = it) }
                    PreferenceSwitch("Yağmura Hassasım", weatherPrefs.rainSensitive) { weatherPrefs = weatherPrefs.copy(rainSensitive = it) }
                    PreferenceSwitch("Rüzgara Hassasım", weatherPrefs.windSensitive) { weatherPrefs = weatherPrefs.copy(windSensitive = it) }
                    PreferenceSwitch("Yüksek UV Hassasiyeti", weatherPrefs.uvSensitive) { weatherPrefs = weatherPrefs.copy(uvSensitive = it) }
                }
            }

            Spacer(Modifier.height(themeStyles.spacingXXL))

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
                modifier = Modifier.padding(top = themeStyles.spacingSM).minimumInteractiveComponentSize()
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
        alpha = if (isSelected) 0.6f else 0.3f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(category.icon, null, tint = themeColors.accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(themeStyles.spacingMD))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.title, style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
                if (!isSelected) {
                    Text(category.description, style = HavamaniaTheme.typography.bodySmall, color = themeColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                Spacer(Modifier.height(themeStyles.spacingMD))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(themeStyles.spacingSM),
                    verticalArrangement = Arrangement.spacedBy(themeStyles.spacingSM)
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
        alpha = if (isSelected) 0.6f else 0.3f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(themeStyles.spacingMD))
            Text(title, style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary, modifier = Modifier.weight(1f))
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
                Spacer(Modifier.height(themeStyles.spacingMD))
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
            .padding(vertical = HavamaniaTheme.styles.spacingXXS)
            .minimumInteractiveComponentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = HavamaniaTheme.typography.bodyLarge, color = HavamaniaTheme.colors.textPrimary)
        HavamaniaToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}
