package com.havamania

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.HavamaniaChip

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TravelFilterChoices(selected: TravelFilter, onSelect: (TravelFilter) -> Unit,
    modifier: Modifier = Modifier) {
    val minimumWidth = 112.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    FlowRow(modifier.fillMaxWidth(), maxItemsInEachRow = 3,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TravelFilter.entries.forEach { filter ->
            HavamaniaChip(selected = selected == filter, onClick = { onSelect(filter) },
                label = when (filter) {
                    TravelFilter.UPCOMING -> "Yaklaşanlar"
                    TravelFilter.PAST -> "Geçmiş"
                    TravelFilter.ARCHIVED -> "Arşiv"
                }, modifier = Modifier.widthIn(min = minimumWidth).weight(1f).heightIn(min = 48.dp))
        }
    }
}
