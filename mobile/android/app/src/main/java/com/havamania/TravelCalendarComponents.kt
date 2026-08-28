package com.havamania

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.HavamaniaTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TravelCalendarStripe(
    selectedDate: LocalDate,
    onDateSelect: (LocalDate) -> Unit,
    tripDates: Set<LocalDate>,
    modifier: Modifier = Modifier
) {
    val themeColors = HavamaniaTheme.colors
    val themeStyles = HavamaniaTheme.styles
    val listState = rememberLazyListState()

    // Generate dates for the stripe (e.g., 30 days before and 90 days after today)
    val today = remember { LocalDate.now() }
    val dates = remember {
        (-30..90).map { today.plusDays(it.toLong()) }
    }

    LaunchedEffect(Unit) {
        // Scroll to near today or selectedDate
        val index = dates.indexOf(selectedDate).coerceAtLeast(0)
        if (index > 3) {
            listState.scrollToItem(index - 3)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("tr"))).uppercase(),
            style = HavamaniaTheme.typography.sectionTitle,
            color = themeColors.textPrimary.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = themeStyles.pagePadding)
        )

        Spacer(Modifier.height(12.dp))

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = themeStyles.pagePadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(dates, key = { it.toString() }) { date ->
                val isSelected = date == selectedDate
                val isToday = date == today
                val hasTrip = tripDates.contains(date)

                CalendarDayItem(
                    date = date,
                    isSelected = isSelected,
                    isToday = isToday,
                    hasTrip = hasTrip,
                    onClick = { onDateSelect(date) }
                )
            }
        }
    }
}

@Composable
private fun CalendarDayItem(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasTrip: Boolean,
    onClick: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    val dayName = date.format(DateTimeFormatter.ofPattern("EEE", Locale("tr"))).uppercase()
    val dayNumber = date.dayOfMonth.toString()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) themeColors.accent
                else if (isToday) themeColors.accent.copy(alpha = 0.1f)
                else Color.Transparent
            )
            .then(
                if (!isSelected && isToday) Modifier.border(1.dp, themeColors.accent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = dayName,
            style = HavamaniaTheme.typography.caption.copy(fontWeight = FontWeight.Black),
            color = if (isSelected) Color.White.copy(alpha = 0.8f) else themeColors.textMuted
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = dayNumber,
            style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Black),
            color = if (isSelected) Color.White else themeColors.textPrimary
        )

        if (hasTrip) {
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else themeColors.accent)
            )
        }
    }
}
