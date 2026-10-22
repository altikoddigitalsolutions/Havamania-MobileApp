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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.havamania.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitiesManagementScreen(
    onBack: () -> Unit,
    themeViewModel: ThemeViewModel = viewModel(),
    weatherViewModel: WeatherViewModel = viewModel()
) {
    val registeredCities by themeViewModel.registeredCities.collectAsStateWithLifecycle()
    val defaultCity by themeViewModel.defaultCity.collectAsStateWithLifecycle()
    val citySuggestions by weatherViewModel.citySuggestions.collectAsStateWithLifecycle()
    val themeColors = HavamaniaTheme.colors

    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(searchText) {
        weatherViewModel.searchCity(searchText)
    }

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "ŞEHİRLERİ YÖNET",
                onBack = onBack
            )
        }
    ) { padding ->
        CityManagementContent(
            searchText, { searchText = it }, citySuggestions, registeredCities, defaultCity,
            onAdd = { themeViewModel.addCity(it); searchText = "" },
            onDelete = themeViewModel::removeCity, onSetDefault = themeViewModel::setDefaultCity,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
internal fun CityManagementContent(
    query: String, onQueryChange: (String) -> Unit,
    suggestions: List<GeocodingResultDto>, cities: List<GeocodingResultDto>, defaultCity: GeocodingResultDto?,
    onAdd: (GeocodingResultDto) -> Unit, onDelete: (GeocodingResultDto) -> Unit,
    onSetDefault: (GeocodingResultDto) -> Unit, modifier: Modifier = Modifier
) {
    val colors = HavamaniaTheme.colors
    LazyColumn(
        modifier = modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "search") {
            HavamaniaGlassCard(alpha = 0.6f, cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query, onValueChange = onQueryChange,
                    label = { Text("Şehir veya ilçe ara") },
                    placeholder = { Text("En az 2 karakter yazın") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
                )
            }
        }
        if (query.trim().length >= 2) {
            items(suggestions.take(5).distinctBy { it.id }, key = { "suggestion-${it.id}" }) { suggestion ->
                HavamaniaGlassCard(onClick = { onAdd(suggestion) }, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(suggestion.displayName, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                            Text(suggestion.country, color = colors.textSecondary)
                        }
                        Icon(Icons.Rounded.AddCircle, "Konumu ekle", tint = colors.accent)
                    }
                }
            }
        }
        item(key = "heading") { Text("Kayıtlı konumlar", color = colors.accent, style = MaterialTheme.typography.titleMedium) }
        if (cities.isEmpty()) {
            item(key = "empty") {
                WeatherEmptyState(icon = Icons.Rounded.LocationCity, title = "Henüz kayıtlı konum yok",
                    description = "Şehir veya ilçe ekleyerek hava durumunu takip edebilirsiniz.", accentColor = colors.accent)
            }
        } else {
            items(cities, key = { "city-${it.id}" }) { city ->
                CityListItem(city, city.id == defaultCity?.id, cities.size > 1,
                    { onDelete(city) }, { onSetDefault(city) })
            }
        }
        item(key = "help") {
            Text("Varsayılan konumu silmek için önce başka bir konumu varsayılan yapın.",
                color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun CityListItem(
    city: GeocodingResultDto,
    isDefault: Boolean,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    Surface(
        color = themeColors.surfaceGlass.copy(alpha = if (isDefault) 0.8f else 0.4f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDefault) themeColors.accent.copy(alpha = 0.3f) else themeColors.border.copy(alpha = 0.1f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDefault) themeColors.accent.copy(alpha = 0.1f) else themeColors.surfaceGlass),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isDefault) Icons.Rounded.Home else Icons.Rounded.LocationOn,
                    null,
                    tint = if (isDefault) themeColors.accent else themeColors.textMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    city.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = themeColors.textPrimary
                )
                Text(
                    text = listOfNotNull(city.admin1?.takeIf { it != city.name }, city.country.takeIf { it.isNotBlank() }).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.textSecondary
                )
            }

            Row {
                if (!isDefault) {
                    IconButton(onClick = onSetDefault) {
                        Icon(Icons.Rounded.StarBorder, "${city.name} varsayılan konum yap", tint = themeColors.textMuted)
                    }
                }

                IconButton(
                    onClick = onDelete,
                    enabled = canDelete && !isDefault
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        "${city.name} konumunu sil",
                        tint = if (canDelete && !isDefault) themeColors.error.copy(alpha = 0.7f) else themeColors.textMuted.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}
