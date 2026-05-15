package com.havamania

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String, Map<String, String>?) -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val themeColors = HavamaniaTheme.colors
    val context = LocalContext.current

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMarkReadConfirm by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    val vibrator = remember { context.getSystemService(Vibrator::class.java) }

    fun triggerHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = if (state.isSelectionMode) "${state.selectedIds.size} SEÇİLDİ" else "BİLDİRİM MERKEZİ",
                onBack = {
                    if (state.isSelectionMode) viewModel.clearSelection() else onBack()
                },
                actions = {
                    if (state.isSelectionMode) {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Rounded.SelectAll, null, tint = themeColors.textPrimary)
                        }
                        IconButton(
                            onClick = { showMarkReadConfirm = true },
                            enabled = state.selectedIds.isNotEmpty()
                        ) {
                            Icon(Icons.Rounded.DoneAll, null, tint = themeColors.accent)
                        }
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            enabled = state.selectedIds.isNotEmpty()
                        ) {
                            Icon(Icons.Rounded.Delete, null, tint = themeColors.error)
                        }
                    } else if (state.notifications.isNotEmpty()) {
                        IconButton(onClick = { showMarkReadConfirm = true }) {
                            Icon(Icons.Rounded.DoneAll, null, tint = themeColors.textPrimary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Summary Info
            Text(
                text = if (state.unreadCount > 0) "${state.unreadCount} yeni bildirim" else "Okunmamış bildirim yok",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (state.unreadCount > 0) themeColors.accent else themeColors.textMuted
            )

            // Filters
            NotificationFiltersRow(
                activeFilter = state.activeFilter,
                onFilterChange = {
                    triggerHaptic()
                    viewModel.setFilter(it)
                }
            )

            // Simple stable list
            Box(modifier = Modifier.weight(1f)) {
                if (state.filteredNotifications.isEmpty() && !state.isLoading) {
                    NotificationEmptyState(onBack = onBack)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = state.filteredNotifications,
                            key = { it.getSafeId() }
                        ) { notification ->
                            NotificationCard(
                                notification = notification,
                                isSelected = state.selectedIds.contains(notification.id),
                                isSelectionMode = state.isSelectionMode,
                                onToggleSelection = { viewModel.toggleSelection(notification.id) },
                                onDeleteClick = {
                                    pendingDeleteId = notification.id
                                    showDeleteConfirm = true
                                },
                                onClick = {
                                    if (state.isSelectionMode) {
                                        viewModel.toggleSelection(notification.id)
                                    } else {
                                        viewModel.markAsRead(notification.id)
                                        notification.deepLinkTarget?.let { target ->
                                            try {
                                                onNavigateToDetail(target, notification.relatedTripId?.let { mapOf("focusId" to it) })
                                            } catch (e: Exception) {
                                                Log.e("NotifCenter", "Nav error", e)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Delete Confirm
        if (showDeleteConfirm) {
            PremiumConfirmSheet(
                title = "SİLME ONAYI",
                message = if (state.isSelectionMode) "Seçili ${state.selectedIds.size} bildirim silinsin mi?" else "Bu bildirim silinsin mi?",
                confirmLabel = "SİL",
                confirmColor = themeColors.error,
                onConfirm = {
                    if (state.isSelectionMode) viewModel.deleteSelected()
                    else pendingDeleteId?.let { viewModel.deleteNotification(it) }
                    showDeleteConfirm = false
                    pendingDeleteId = null
                },
                onDismiss = {
                    showDeleteConfirm = false
                    pendingDeleteId = null
                }
            )
        }

        // Mark Read Confirm
        if (showMarkReadConfirm) {
            PremiumConfirmSheet(
                title = "OKUNDU İŞARETLE",
                message = if (state.isSelectionMode) "Seçili bildirimler okundu yapılsın mı?" else "Tüm bildirimler okundu yapılsın mı?",
                confirmLabel = "OKUNDU YAP",
                confirmColor = themeColors.accent,
                onConfirm = {
                    if (state.isSelectionMode) viewModel.markSelectedAsRead()
                    else viewModel.markAllAsRead()
                    showMarkReadConfirm = false
                },
                onDismiss = { showMarkReadConfirm = false }
            )
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    val dateFormat = remember { SimpleDateFormat("d MMMM, HH:mm", Locale("tr")) }
    val dateStr = remember(notification.createdAt) { dateFormat.format(Date(notification.createdAt)) }

    val iconInfo = when (notification.category) {
        NotificationCategory.WARNING -> Icons.Rounded.WarningAmber to themeColors.error
        NotificationCategory.TRAVEL -> Icons.Rounded.CardTravel to themeColors.accent
        else -> Icons.Rounded.Notifications to themeColors.accent
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isSelected) 0.dp else 2.dp, RoundedCornerShape(20.dp))
            .pointerInput(notification.id) {
                detectTapGestures(
                    onLongPress = { onToggleSelection() },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) themeColors.accent.copy(0.1f) else themeColors.surfaceGlass,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, themeColors.accent) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp).alpha(if (notification.isRead) 0.6f else 1f),
            verticalAlignment = Alignment.Top
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    colors = CheckboxDefaults.colors(checkedColor = themeColors.accent)
                )
                Spacer(Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(iconInfo.second.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconInfo.first, null, tint = iconInfo.second, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = notification.getSafeTitle(),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = themeColors.textPrimary
                    )
                    if (!isSelectionMode) {
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Rounded.Close, null, tint = themeColors.textMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = notification.getSafeMessage(),
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = themeColors.textMuted)
            }
        }
    }
}

@Composable
fun NotificationFiltersRow(
    activeFilter: NotificationCategory?,
    onFilterChange: (NotificationCategory?) -> Unit
) {
    val categories = remember { NotificationCategory.values().filter { it != NotificationCategory.SYSTEM } }
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            HavamaniaChip(selected = activeFilter == null, onClick = { onFilterChange(null) }, label = "Tümü")
        }
        items(categories, key = { it.name }) { category ->
            HavamaniaChip(selected = activeFilter == category, onClick = { onFilterChange(category) }, label = category.label)
        }
    }
}

@Composable
fun NotificationEmptyState(onBack: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.NotificationsNone, null, tint = themeColors.textMuted, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        Text("Henüz bildirim yok", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
        Spacer(Modifier.height(32.dp))
        HavamaniaPrimaryButton(text = "GERİ DÖN", onClick = onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumConfirmSheet(
    title: String,
    message: String,
    confirmLabel: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = themeColors.surface) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = themeColors.accent)
            Spacer(Modifier.height(16.dp))
            Text(message, textAlign = TextAlign.Center, color = themeColors.textPrimary)
            Spacer(Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("VAZGEÇ") }
                Button(onClick = onConfirm, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = confirmColor)) {
                    Text(confirmLabel, color = Color.White)
                }
            }
        }
    }
}
