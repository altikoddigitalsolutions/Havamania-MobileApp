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
    themeViewModel: ThemeViewModel = viewModel(),
    weatherViewModel: WeatherViewModel = viewModel()
) {
    val registeredCities by themeViewModel.registeredCities.collectAsState()
    val defaultCity by themeViewModel.defaultCity.collectAsState()
    val citySuggestions by weatherViewModel.citySuggestions.collectAsState()
    val themeColors = HavamaniaTheme.colors

    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(searchText) {
        if (searchText.length >= 2) {
            weatherViewModel.searchCity(searchText)
        }
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
                "YENİ ŞEHİR/İLÇE EKLE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Black),
                color = themeColors.accent.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("İl veya ilçe ismi girin...", color = themeColors.textMuted) },
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
            if (citySuggestions.isNotEmpty() && searchText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HavamaniaGlassCard(alpha = 0.9f) {
                    Column {
                        citySuggestions.take(5).forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        themeViewModel.addCity(suggestion)
                                        searchText = ""
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(suggestion.name, color = themeColors.textPrimary, fontWeight = FontWeight.Bold)
                                    if (suggestion.admin1 != null && suggestion.admin1 != suggestion.name) {
                                        Text(suggestion.admin1!!, style = MaterialTheme.typography.bodySmall, color = themeColors.textSecondary)
                                    }
                                }
                                Icon(Icons.Rounded.Add, null, tint = themeColors.accent)
                            }
                            if (suggestion != citySuggestions.take(5).last()) {
                                HorizontalDivider(color = themeColors.border.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Kayıtlı Şehirler Listesi
            Text(
                "KAYITLI LOKASYONLAR",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Black),
                color = themeColors.accent.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(registeredCities, key = { it.id }) { city ->
                    val isDefault = city.id == defaultCity.id
                    CityListItem(
                        city = city,
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
                    "En az bir konum kayıtlı kalmalıdır.",
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
    city: GeocodingResultDto,
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
                        city.name,
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
                if (city.admin1 != null && city.admin1 != city.name) {
                    Text(city.admin1!!, style = MaterialTheme.typography.bodySmall, color = themeColors.textSecondary)
                }
            }

            if (!isDefault) {
                IconButton(onClick = onSetDefault) {
                    Icon(Icons.Rounded.StarBorder, null, tint = themeColors.textMuted)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = themeColors.error.copy(alpha = 0.7f))
            }
        }
    }
}
