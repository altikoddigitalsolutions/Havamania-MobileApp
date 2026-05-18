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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.navigation.NavController
import com.havamania.ui.theme.HavamaniaTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationCenterScreen(
    navController: NavController,
    onBack: () -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val themeColors = HavamaniaTheme.colors
    val notifications by viewModel.notifications.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    var selectedFilter by rememberSaveable { mutableStateOf(NotificationFilter.ALL) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filteredNotifications = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            NotificationFilter.ALL -> notifications
            NotificationFilter.UNREAD -> notifications.filter { !it.isRead }
            NotificationFilter.TRAVEL -> notifications.filter { it.type == NotificationType.TRAVEL }
            NotificationFilter.RAIN -> notifications.filter { it.type == NotificationType.RAIN }
            NotificationFilter.UV -> notifications.filter { it.type == NotificationType.UV }
            NotificationFilter.WARNING -> notifications.filter { it.type == NotificationType.WARNING }
            NotificationFilter.SUMMARY -> notifications.filter { it.type == NotificationType.SUMMARY }
            NotificationFilter.UPDATE -> notifications.filter { it.type == NotificationType.UPDATE }
            NotificationFilter.GENERAL -> notifications.filter { it.type == NotificationType.GENERAL }
        }
    }

    fun handleNotificationClick(notification: AppNotification) {
        if (isSelectionMode) {
            viewModel.toggleSelection(notification.id)
            return
        }

        viewModel.markAsRead(notification.id)

        when (notification.type) {
            NotificationType.TRAVEL -> navController.navigate(Routes.CALENDAR)
            NotificationType.RAIN -> navController.navigate(Routes.WEATHER)
            NotificationType.UV -> navController.navigate(Routes.WEATHER)
            NotificationType.SUMMARY -> navController.navigate(Routes.AI)
            NotificationType.WARNING -> navController.navigate(Routes.WEATHER)
            NotificationType.GENERAL -> navController.navigate(Routes.SETTINGS)
            NotificationType.UPDATE -> navController.navigate(Routes.PROFILE)
            else -> {}
        }
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSelectionMode) {
                SelectionTopBar(
                    selectedCount = selectedIds.size,
                    onExitSelection = { viewModel.exitSelectionMode() },
                    onSelectAll = { viewModel.selectAll() },
                    onMarkRead = { viewModel.markSelectedAsRead() },
                    onDelete = {
                        viewModel.deleteSelected()
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Bildirimler silindi",
                                actionLabel = "GERİ AL",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undoDelete()
                            }
                        }
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri", tint = themeColors.textPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshDemoNotifications() }) {
                            Icon(Icons.Rounded.Refresh, "Demo Yükle", tint = themeColors.accent)
                        }
                        IconButton(
                            onClick = { showDeleteAllDialog = true },
                            enabled = notifications.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Tümünü Sil",
                                tint = if (notifications.isNotEmpty()) themeColors.error else themeColors.textSecondary.copy(alpha = 0.5f)
                            )
                        }
                        if (notifications.isNotEmpty()) {
                            IconButton(onClick = { viewModel.markAllAsRead() }) {
                                Icon(Icons.Rounded.DoneAll, contentDescription = "Tümünü Okundu Yap", tint = themeColors.accent)
                            }
                        }
                    },
                    title = {
                        Text(
                            "BİLDİRİM MERKEZİ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = themeColors.textPrimary,
                        navigationIconContentColor = themeColors.textPrimary
                    )
                )
            }
        },
        containerColor = themeColors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NotificationFilterRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            if (filteredNotifications.isEmpty()) {
                EmptyNotificationState(
                    hasAnyNotifications = notifications.isNotEmpty(),
                    onResetFilter = { selectedFilter = NotificationFilter.ALL }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredNotifications, key = { it.id }) { notification ->
                        val isSelected = selectedIds.contains(notification.id)

                        SwipeableNotificationItem(
                            notification = notification,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onClick = { handleNotificationClick(notification) },
                            onLongClick = { viewModel.enterSelectionMode(notification.id) },
                            onDelete = {
                                viewModel.deleteNotification(notification.id)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Bildirim silindi",
                                        actionLabel = "GERİ AL",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoDelete()
                                    }
                                }
                            },
                            onToggleRead = { viewModel.toggleReadStatus(notification.id) },
                            onToggleSelection = { viewModel.toggleSelection(notification.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    onExitSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    TopAppBar(
        title = { Text("$selectedCount Seçildi", style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
            IconButton(onClick = onExitSelection) {
                Icon(Icons.Rounded.Close, contentDescription = "Vazgeç")
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Rounded.SelectAll, contentDescription = "Tümünü Seç")
            }
            IconButton(onClick = onMarkRead) {
                Icon(Icons.Rounded.Drafts, contentDescription = "Okundu Yap")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "Sil", tint = themeColors.error)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = themeColors.surfaceGlass,
            titleContentColor = themeColors.textPrimary,
            navigationIconContentColor = themeColors.textPrimary,
            actionIconContentColor = themeColors.textPrimary
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableNotificationItem(
    notification: AppNotification,
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

    // Theme-based action colors
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
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(IntrinsicSize.Min)
    ) {
        // Background Action Layer (Visible only when swiped)
        Box(modifier = Modifier.matchParentSize()) {
            if (offsetX.value > 8f) {
                // Left Action (Read/Unread)
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
                            contentDescription = null,
                            tint = readTextColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (notification.isRead) stringResource(R.string.notification_mark_unread) else stringResource(R.string.notification_mark_read),
                            color = readTextColor,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            if (offsetX.value < -8f) {
                // Right Action (Delete)
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
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = deleteTextColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.notification_delete),
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
                    onLongClick = {
                        if (offsetX.value == 0f) onLongClick()
                    }
                )
        ) {
            NotificationCard(
                notification = notification,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                onClick = onClick,
                onLongClick = onLongClick,
                onToggleSelection = onToggleSelection
            )
        }
    }
}

@Composable
fun NotificationCard(
    notification: AppNotification,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelection: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors

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

    val titleWeight = if (notification.isRead) FontWeight.Bold else FontWeight.Black

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = cardBgColor,
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            cardBorderColor
        ),
        shadowElevation = if (notification.isRead) 0.dp else 4.dp
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            val iconBgColor = when (notification.type) {
                NotificationType.TRAVEL -> Color(0xFF6366F1)
                NotificationType.RAIN -> Color(0xFF3B82F6)
                NotificationType.UV -> Color(0xFFF59E0B)
                NotificationType.WARNING -> Color(0xFFEF4444)
                NotificationType.SUMMARY -> Color(0xFF10B981)
                NotificationType.UPDATE -> Color(0xFF8B5CF6)
                NotificationType.GENERAL -> Color(0xFF64748B)
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
                    imageVector = notification.type.getIcon(),
                    contentDescription = null,
                    tint = iconBgColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = titleWeight,
                            letterSpacing = 0.2.sp
                        ),
                        color = themeColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = notification.timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = themeColors.textMuted
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = if (notification.isRead) themeColors.textSecondary.copy(alpha = 0.8f) else themeColors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (notification.actionText != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        onClick = { onClick() },
                        shape = CircleShape,
                        color = themeColors.accent.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = notification.actionText.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            ),
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
                        .background(
                            Brush.radialGradient(
                                colors = listOf(themeColors.accent, themeColors.accent.copy(alpha = 0.6f))
                            )
                        )
                        .border(1.5.dp, themeColors.surface, CircleShape)
                )
            }
        }
    }
}

@Composable
fun NotificationFilterRow(
    selectedFilter: NotificationFilter,
    onFilterSelected: (NotificationFilter) -> Unit
) {
    val filters = NotificationFilter.entries
    val themeColors = HavamaniaTheme.colors

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(filters) { filter ->
            val isSelected = selectedFilter == filter
            val label = when (filter) {
                NotificationFilter.ALL -> "Tümü"
                NotificationFilter.UNREAD -> "Okunmamış"
                NotificationFilter.TRAVEL -> "Seyahat"
                NotificationFilter.RAIN -> "Yağmur"
                NotificationFilter.UV -> "UV"
                NotificationFilter.WARNING -> "Uyarı"
                NotificationFilter.SUMMARY -> "Özet"
                NotificationFilter.UPDATE -> "Güncelleme"
                NotificationFilter.GENERAL -> "Genel"
            }

            Surface(
                onClick = { onFilterSelected(filter) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) themeColors.accent else themeColors.surfaceGlass.copy(alpha = 0.4f),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) themeColors.accent else themeColors.border.copy(alpha = 0.15f)
                ),
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
    }
}

@Composable
fun EmptyNotificationState(
    hasAnyNotifications: Boolean,
    onResetFilter: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(themeColors.accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (hasAnyNotifications) Icons.Rounded.SearchOff else Icons.Rounded.NotificationsNone,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = themeColors.accent
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (hasAnyNotifications) "Bu kategoride bildirim yok." else "Henüz bildiriminiz bulunmuyor.",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = themeColors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (hasAnyNotifications) "Lütfen farklı bir filtre seçmeyi deneyin." else "Yeni bildirimleriniz olduğunda burada görünecektir.",
            style = MaterialTheme.typography.bodyMedium,
            color = themeColors.textSecondary,
            textAlign = TextAlign.Center
        )

        if (hasAnyNotifications) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onResetFilter,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
            ) {
                Text("TÜMÜNÜ GÖSTER", fontWeight = FontWeight.Bold)
            }
        }
    }
}
