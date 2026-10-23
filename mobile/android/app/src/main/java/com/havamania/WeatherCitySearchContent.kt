package com.havamania

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.HavamaniaTheme

@Composable
internal fun WeatherCitySearchContent(
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<GeocodingResultDto>,
    status: LocationSearchStatus,
    onRetry: () -> Unit,
    isSelected: (GeocodingResultDto) -> Boolean = { false },
    onSelect: (GeocodingResultDto) -> Unit
) {
    val colors = HavamaniaTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxWidth().imePadding(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Şehir seç", style = HavamaniaTheme.typography.cardTitle, color = colors.textPrimary)
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = onQueryChange, singleLine = true,
                label = { Text("Şehir veya ilçe ara") },
                placeholder = { Text("En az 2 karakter yazın") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.trim().length >= 2) {
            item { LocationSearchFeedback(status, onRetry) }
            items(suggestions.distinctBy { it.id }, key = { it.id }) { suggestion ->
                Surface(onClick = { onSelect(suggestion) }, color = if (isSelected(suggestion)) colors.accent.copy(alpha = 0.1f) else colors.surface, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        if (isSelected(suggestion)) Text("Seçili", color = colors.accent)
                        Text(suggestion.getSafeDistrict() ?: suggestion.name, color = colors.textPrimary,
                            style = HavamaniaTheme.typography.bodyLarge)
                        Text(suggestion.getSafeCity(), color = colors.textMuted,
                            style = HavamaniaTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
