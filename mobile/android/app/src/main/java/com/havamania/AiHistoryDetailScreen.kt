package com.havamania

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHistoryDetailScreen(
    itemId: String,
    onBack: () -> Unit,
    viewModel: AiHistoryViewModel = viewModel()
) {
    val historyItems by viewModel.historyItems.collectAsState()
    val item = remember(historyItems, itemId) { historyItems.find { it.id == itemId } }
    val themeColors = HavamaniaTheme.colors

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "ANALİZ DETAYI",
                onBack = onBack
            )
        }
    ) { padding ->
        if (item != null) {
            val dateFormat = remember { SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("tr")) }
            val dateStr = remember(item.timestamp) { dateFormat.format(Date(item.timestamp)) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelMedium,
                    color = themeColors.accent
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = themeColors.textPrimary
                )
                if (item.cityName != null) {
                    Text(
                        text = item.cityName,
                        style = MaterialTheme.typography.titleMedium,
                        color = themeColors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                HavamaniaGlassCard(alpha = 0.4f) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = item.fullText,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                            color = themeColors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Kayıt bulunamadı", color = themeColors.textPrimary)
            }
        }
    }
}
