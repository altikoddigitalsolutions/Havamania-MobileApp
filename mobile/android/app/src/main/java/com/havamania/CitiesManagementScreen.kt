package com.havamania

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitiesManagementScreen(
    onBack: () -> Unit,
    themeViewModel: ThemeViewModel = viewModel()
) {
    val registeredCities by themeViewModel.registeredCities.collectAsState()
    val defaultCity by themeViewModel.defaultCity.collectAsState()
    val themeColors = HavamaniaTheme.colors

    var searchText by remember { mutableStateOf("") }
    val filteredSuggestions = remember(searchText) {
        if (searchText.length < 2) emptyList()
        else cities.filter { it.contains(searchText, ignoreCase = true) }
            .filter { it !in registeredCities }
            .take(5)
    }

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "ŞEHİRLERİ YÖNET",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Şehir Ekleme Alanı
            Text(
                "YENİ ŞEHİR EKLE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Black),
                color = themeColors.accent.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Şehir ismi girin...", color = themeColors.textMuted) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = themeColors.accent) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = themeColors.surfaceGlass,
                    unfocusedContainerColor = themeColors.surfaceGlass,
                    unfocusedBorderColor = themeColors.border.copy(alpha = 0.2f),
                    focusedBorderColor = themeColors.accent,
                    focusedTextColor = themeColors.textPrimary,
                    unfocusedTextColor = themeColors.textPrimary,
                    cursorColor = themeColors.accent
                ),
                singleLine = true
            )

            // Autocomplete Suggestions
            if (filteredSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HavamaniaGlassCard(alpha = 0.9f) {
                    Column {
                        filteredSuggestions.forEach { city ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        themeViewModel.addCity(city)
                                        searchText = ""
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(city, color = themeColors.textPrimary)
                                Icon(Icons.Rounded.Add, null, tint = themeColors.accent)
                            }
                            if (city != filteredSuggestions.last()) {
                                HorizontalDivider(color = themeColors.border.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Kayıtlı Şehirler Listesi
            Text(
                "KAYITLI ŞEHİRLER",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Black),
                color = themeColors.accent.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(registeredCities) { city ->
                    val isDefault = city == defaultCity
                    CityListItem(
                        name = city,
                        isDefault = isDefault,
                        onDelete = {
                            if (registeredCities.size > 1) {
                                themeViewModel.removeCity(city)
                            }
                        },
                        onSetDefault = { themeViewModel.setDefaultCity(city) }
                    )
                }
            }

            if (registeredCities.size <= 1) {
                Text(
                    "En az bir şehir kayıtlı kalmalıdır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 12.dp).align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CityListItem(
    name: String,
    isDefault: Boolean,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    HavamaniaGlassCard(
        alpha = if (isDefault) 0.6f else 0.4f,
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = themeColors.textPrimary
                    )
                    if (isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = themeColors.accent.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "VARSAYILAN",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = themeColors.accent
                            )
                        }
                    }
                }
            }

            if (!isDefault) {
                IconButton(onClick = onSetDefault) {
                    Icon(Icons.Rounded.StarBorder, null, tint = themeColors.textMuted)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = Color(0xFFEF4444).copy(alpha = 0.7f))
            }
        }
    }
}
