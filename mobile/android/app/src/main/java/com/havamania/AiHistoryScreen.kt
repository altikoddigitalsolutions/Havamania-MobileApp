package com.havamania

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHistoryScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: AiHistoryViewModel = viewModel()
) {
    val historyItems by viewModel.historyItems.collectAsState()
    val themeColors = HavamaniaTheme.colors

    var showDeleteConfirm by remember { mutableStateOf(false) }

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "AI GEÇMİŞİ",
                onBack = onBack,
                actions = {
                    if (historyItems.isNotEmpty()) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Rounded.DeleteSweep, null, tint = themeColors.textPrimary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (historyItems.isEmpty()) {
                AiHistoryEmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(historyItems) { item ->
                        AiHistoryCard(
                            item = item,
                            onClick = { onNavigateToDetail(item.id) },
                            onDelete = { viewModel.deleteItem(item.id) }
                        )
                    }
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = themeColors.surface,
                title = { Text("Geçmişi Temizle", fontWeight = FontWeight.Black, color = themeColors.textPrimary) },
                text = { Text("Tüm AI analiz geçmişini silmek istediğinize emin misiniz?", color = themeColors.textSecondary) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearAll()
                        showDeleteConfirm = false
                    }) {
                        Text("TEMİZLE", color = themeColors.error, fontWeight = FontWeight.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("VAZGEÇ", color = themeColors.textPrimary, fontWeight = FontWeight.Black)
                    }
                }
            )
        }
    }
}

@Composable
fun AiHistoryCard(
    item: AiHistoryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    val dateFormat = remember { SimpleDateFormat("d MMMM, HH:mm", Locale("tr")) }
    val dateStr = remember(item.timestamp) { dateFormat.format(Date(item.timestamp)) }

    HavamaniaGlassCard(
        onClick = onClick,
        alpha = 0.5f,
        cornerRadius = 24.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(themeColors.accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = themeColors.accent, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = themeColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.cityName != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "• ${item.cityName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.accent
                        )
                    }
                }
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = themeColors.textMuted
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Close, null, tint = themeColors.textMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun AiHistoryEmptyState() {
    val themeColors = HavamaniaTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(themeColors.accent.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.HistoryEdu,
                null,
                tint = themeColors.accent.copy(alpha = 0.3f),
                modifier = Modifier.size(50.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Henüz AI geçmişin yok",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = themeColors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Asistanla konuştuğunda analizlerin burada görünecek.",
            style = MaterialTheme.typography.bodyMedium,
            color = themeColors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}
