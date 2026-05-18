package com.havamania

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.HavamaniaTheme
import com.havamania.ui.theme.HavamaniaTopBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String, Map<String, String>?) -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val themeColors = HavamaniaTheme.colors
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            containerColor = themeColors.surface,
            titleContentColor = themeColors.textPrimary,
            textContentColor = themeColors.textSecondary,
            title = { Text("Tüm bildirimleri silmek istiyor musun?") },
            text = { Text("Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllNotifications()
                        showDeleteAllDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Tüm bildirimler silindi")
                        }
                    }
                ) {
                    Text("Tümünü Sil", color = themeColors.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Vazgeç", color = themeColors.textPrimary)
                }
            }
        )
    }

    Scaffold(
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
                        IconButton(onClick = { viewModel.markSelectedAsRead() }) {
                            Icon(Icons.Rounded.DoneAll, null, tint = themeColors.accent)
                        }
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Rounded.Delete, null, tint = themeColors.error)
                        }
                    } else {
                        if (state.notifications.isNotEmpty()) {
                            IconButton(
                                onClick = { showDeleteAllDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Tümünü Sil",
                                    tint = themeColors.error
                                )
                            }
                            IconButton(onClick = { viewModel.markAllAsRead() }) {
                                Icon(Icons.Rounded.DoneAll, null, tint = themeColors.accent)
                            }
                        }
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NotificationFilterRow(
                activeFilter = state.activeFilter,
                onFilterChange = { viewModel.setFilter(it) }
            )

            if (state.filteredNotifications.isEmpty() && !state.isLoading) {
                EmptyNotificationState(
                    hasAnyNotifications = state.notifications.isNotEmpty(),
                    onResetFilter = { viewModel.setFilter(null) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.filteredNotifications, key = { it.id }) { notification ->
                        val isSelected = state.selectedIds.contains(notification.id)

                        SwipeableNotificationItem(
                            notification = notification,
                            isSelected = isSelected,
                            isSelectionMode = state.isSelectionMode,
                            onClick = {
                                viewModel.markAsRead(notification.id)
                                notification.deepLinkTarget?.let { target ->
                                    onNavigateToDetail(target, notification.relatedTripId?.let { mapOf("tripId" to it) })
                                }
                            },
                            onLongClick = { viewModel.toggleSelection(notification.id) },
                            onDelete = { viewModel.deleteNotification(notification.id) },
                            onToggleRead = { viewModel.toggleReadStatus(notification.id) },
                            onToggleSelection = { viewModel.toggleSelection(notification.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableNotificationItem(
    notification: NotificationItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleRead: () -> Unit,
    onToggleSelection: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val offsetX = remember { Animatable(0f) }

    val maxOffsetPx = with(density) { 112.dp.toPx() }
    val thresholdPx = with(density) { 80.dp.toPx() }

    val themeColors = HavamaniaTheme.colors

    val readActionBg = if (themeColors.isDark) {
        Brush.horizontalGradient(listOf(Color(0xFF1E3A8A), Color(0xFF2563EB)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFFE7F0FF), Color(0xFFC9DAFF)))
    }
    val readTextColor = if (themeColors.isDark) Color.White else Color(0xFF2457D6)

    val deleteActionBg = if (themeColors.isDark) {
        Brush.horizontalGradient(listOf(Color(0xFF7F1D1D), Color(0xFFB91C1C)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFFFFE8EA), Color(0xFFFFD1D6)))
    }
    val deleteTextColor = if (themeColors.isDark) Color.White else Color(0xFFD93445)

    LaunchedEffect(notification.id) {
        offsetX.snapTo(0f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(modifier = Modifier.matchParentSize()) {
            if (offsetX.value > 8f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(108.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(readActionBg)
                        .clickable {
                            scope.launch {
                                offsetX.animateTo(0f)
                                onToggleRead()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (notification.isRead) Icons.Rounded.MarkEmailUnread else Icons.Rounded.MarkEmailRead,
                            null,
                            tint = readTextColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (notification.isRead) "Okunmadı" else "Okundu",
                            color = readTextColor,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            if (offsetX.value < -8f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(108.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(deleteActionBg)
                        .clickable {
                            scope.launch {
                                offsetX.animateTo(0f)
                                onDelete()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Delete, null, tint = deleteTextColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Sil",
                            color = deleteTextColor,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(notification.id, isSelectionMode) {
                    if (!isSelectionMode) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                scope.launch {
                                    val newOffset = (offsetX.value + dragAmount).coerceIn(-maxOffsetPx, maxOffsetPx)
                                    offsetX.snapTo(newOffset)
                                }
                            },
                            onDragEnd = {
                                scope.launch {
                                    val target = when {
                                        offsetX.value >= thresholdPx -> maxOffsetPx
                                        offsetX.value <= -thresholdPx -> -maxOffsetPx
                                        else -> 0f
                                    }
                                    offsetX.animateTo(target)
                                }
                            }
                        )
                    }
                }
                .combinedClickable(
                    onClick = {
                        if (offsetX.value != 0f) {
                            scope.launch { offsetX.animateTo(0f) }
                        } else {
                            if (isSelectionMode) onToggleSelection() else onClick()
                        }
                    },
                    onLongClick = { if (offsetX.value == 0f) onLongClick() }
                )
        ) {
            NotificationCard(
                notification = notification,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                onToggleSelection = onToggleSelection
            )
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale("tr")) }
    val timeStr = remember(notification.createdAt) { dateFormat.format(Date(notification.createdAt)) }

    val cardBgColor = if (isSelected) {
        themeColors.accent.copy(alpha = 0.15f)
    } else if (notification.isRead) {
        themeColors.surfaceGlass.copy(alpha = if (themeColors.isDark) 0.4f else 0.7f)
    } else {
        themeColors.surfaceGlass
    }

    val cardBorderColor = if (isSelected) {
        themeColors.accent
    } else if (notification.isRead) {
        themeColors.border.copy(alpha = 0.1f)
    } else {
        themeColors.accent.copy(alpha = 0.25f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = cardBgColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, cardBorderColor),
        shadowElevation = if (notification.isRead) 0.dp else 4.dp
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            val iconBgColor = when (notification.category) {
                NotificationCategory.TRAVEL -> Color(0xFF6366F1)
                NotificationCategory.RAIN -> Color(0xFF3B82F6)
                NotificationCategory.UV -> Color(0xFFF59E0B)
                NotificationCategory.WARNING -> Color(0xFFEF4444)
                NotificationCategory.SUMMARY -> Color(0xFF10B981)
                NotificationCategory.UPDATE -> Color(0xFF8B5CF6)
                else -> themeColors.accent
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(iconBgColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = notification.category.getIcon(),
                    contentDescription = null,
                    tint = iconBgColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = notification.getSafeTitle(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (notification.isRead) FontWeight.Bold else FontWeight.Black,
                            letterSpacing = 0.2.sp
                        ),
                        color = themeColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = timeStr, style = MaterialTheme.typography.labelSmall, color = themeColors.textMuted)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = notification.getSafeMessage(),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = if (notification.isRead) themeColors.textSecondary.copy(alpha = 0.8f) else themeColors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (notification.actionLabel != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        onClick = { /* Handled by parent click or deepLink */ },
                        shape = CircleShape,
                        color = themeColors.accent.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = notification.actionLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                            color = themeColors.accent,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            if (isSelectionMode) {
                Spacer(modifier = Modifier.width(12.dp))
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    colors = CheckboxDefaults.colors(checkedColor = themeColors.accent)
                )
            } else if (!notification.isRead) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(themeColors.accent, themeColors.accent.copy(alpha = 0.6f))))
                        .border(1.5.dp, themeColors.surface, CircleShape)
                )
            }
        }
    }
}

@Composable
fun NotificationFilterRow(
    activeFilter: NotificationCategory?,
    onFilterChange: (NotificationCategory?) -> Unit
) {
    val categories = remember { NotificationCategory.entries.filter { it != NotificationCategory.SYSTEM } }
    val themeColors = HavamaniaTheme.colors

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            FilterChip(isSelected = activeFilter == null, label = "Tümü", onClick = { onFilterChange(null) })
        }
        items(categories) { category ->
            FilterChip(isSelected = activeFilter == category, label = category.label, onClick = { onFilterChange(category) })
        }
    }
}

@Composable
fun FilterChip(isSelected: Boolean, label: String, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) themeColors.accent else themeColors.surfaceGlass.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, if (isSelected) themeColors.accent else themeColors.border.copy(alpha = 0.15f)),
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = if (isSelected) Color.White else themeColors.textPrimary
        )
    }
}

@Composable
fun EmptyNotificationState(hasAnyNotifications: Boolean, onResetFilter: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (hasAnyNotifications) Icons.Rounded.SearchOff else Icons.Rounded.NotificationsNone,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = themeColors.accent.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (hasAnyNotifications) "Bu kategoride bildirim yok." else "Henüz bildiriminiz bulunmuyor.",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = themeColors.textPrimary,
            textAlign = TextAlign.Center
        )
        if (hasAnyNotifications) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onResetFilter, colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)) {
                Text("TÜMÜNÜ GÖSTER", fontWeight = FontWeight.Bold)
            }
        }
    }
}
