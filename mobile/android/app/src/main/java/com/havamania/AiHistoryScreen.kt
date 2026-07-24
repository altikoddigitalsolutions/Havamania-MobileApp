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
import com.havamania.ui.theme.HavamaniaDialog
import com.havamania.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHistoryScreen(
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: AiHistoryViewModel = viewModel()
) {
    val historyItems by viewModel.historyItems.collectAsState()
    val themeColors = HavamaniaTheme.colors

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<String?>(null) }

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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(historyItems, key = { it.id }) { item ->
                        AiHistoryCard(
                            item = item,
                            onClick = { onNavigateToChat(item.id) },
                            onDelete = { itemToDelete = item.id }
                        )
                    }
                }
            }
        }

        if (showDeleteConfirm) {
            HavamaniaDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = "Geçmişi Temizle?",
                text = "Tüm AI sohbet geçmişiniz kalıcı olarak silinecektir. Devam etmek istiyor musunuz?",
                confirmText = "Temizle",
                confirmColor = themeColors.error,
                icon = Icons.Rounded.DeleteSweep,
                onConfirm = {
                    viewModel.clearAll()
                    showDeleteConfirm = false
                }
            )
        }

        if (itemToDelete != null) {
            HavamaniaDialog(
                onDismissRequest = { itemToDelete = null },
                title = "Sohbeti Sil?",
                text = "Bu sohbet kaydı kalıcı olarak silinecektir.",
                confirmText = "Sil",
                confirmColor = themeColors.error,
                icon = Icons.Rounded.DeleteOutline,
                onConfirm = {
                    itemToDelete?.let { viewModel.deleteItem(it) }
                    itemToDelete = null
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
    val dateFormat = remember { SimpleDateFormat("d MMM, HH:mm", Locale("tr")) }
    val dateStr = remember(item.timestamp) { dateFormat.format(Date(item.timestamp)) }

    HavamaniaGlassCard(
        onClick = onClick,
        alpha = if (themeColors.isDark) 0.5f else 0.7f,
        cornerRadius = 24.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(themeColors.accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.HistoryEdu, null, tint = themeColors.accent, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        ),
                        color = themeColors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (item.cityName != null) {
                        Surface(
                            color = themeColors.accent.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                item.cityName,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = themeColors.accent
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = themeColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = themeColors.textMuted.copy(alpha = 0.8f)
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    null,
                    tint = themeColors.textMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
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
            "Henüz bir sohbet geçmişin yok.",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = themeColors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Havamania Dijital Asistanına hava, seyahat veya günlük planların hakkında soru sorabilirsin.",
            style = MaterialTheme.typography.bodyMedium,
            color = themeColors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}
