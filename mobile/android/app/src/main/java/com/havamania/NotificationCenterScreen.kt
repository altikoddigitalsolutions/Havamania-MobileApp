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
import com.havamania.ui.theme.HavamaniaDialog
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

    // To prevent double clicks/crashes during rapid navigation
    val processingIds = remember { mutableStateListOf<String>() }

    val handleNotificationClick = remember(onNavigateToDetail) {
        { notification: NotificationItem ->
            if (!processingIds.contains(notification.id)) {
                processingIds.add(notification.id)
                viewModel.markAsRead(notification.id)

                try {
                    when (notification.actionType) {
                        NotificationActionType.WEATHER_HOME,
                        NotificationActionType.HOURLY_FORECAST,
                        NotificationActionType.DAILY_FORECAST,
                        NotificationActionType.UV_DETAIL,
                        NotificationActionType.WEATHER_ALERT -> {
                            onNavigateToDetail(Routes.WEATHER, null)
                        }
                        NotificationActionType.TRAVEL_CALENDAR -> {
                            onNavigateToDetail(Routes.CALENDAR, null)
                        }
                        NotificationActionType.TRAVEL_DETAIL -> {
                            val tripId = notification.targetId
                            if (!tripId.isNullOrBlank()) {
                                onNavigateToDetail("${Routes.CALENDAR}?focusId=$tripId", mapOf("focusId" to tripId))
                            } else {
                                onNavigateToDetail(Routes.CALENDAR, null)
                            }
                        }
                        NotificationActionType.WEEKLY_SUMMARY -> {
                            onNavigateToDetail(Routes.AI, null)
                        }
                        NotificationActionType.APP_UPDATE -> {
                            onNavigateToDetail(Routes.PROFILE, null)
                        }
                        NotificationActionType.NONE -> {
                            // If there's a deepLinkTarget as fallback
                            notification.deepLinkTarget?.let { target ->
                                 onNavigateToDetail(target, notification.relatedTripId?.let { mapOf("tripId" to it) })
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NotificationCenter", "Navigation failed", e)
                    onNavigateToDetail(Routes.WEATHER, null)
                } finally {
                    // Small delay to prevent instant re-clicks
                    scope.launch {
                        kotlinx.coroutines.delay(500)
                        processingIds.remove(notification.id)
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.ensureSeeded()
    }

    if (showDeleteAllDialog) {
        HavamaniaDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = "Tümünü Sil?",
            text = "Tüm bildirimler kalıcı olarak silinecektir. Bu işlem geri alınamaz.",
            confirmText = "Sil",
            confirmColor = themeColors.error,
            icon = Icons.Rounded.DeleteForever,
            onConfirm = {
                viewModel.deleteAllNotifications()
                scope.launch {
                    snackbarHostState.showSnackbar("Tüm bildirimler silindi")
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

            if (state.isLoading && state.notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = themeColors.accent)
                }
            } else {
                // Guaranteed non-empty list logic
                val notificationsToShow = if (state.notifications.isEmpty()) {
                    DefaultNotifications.create()
                } else {
                    state.notifications
                }

                // Filter & Group logic
                val filteredList = remember(notificationsToShow, state.activeFilter) {
                    val base = when (state.activeFilter) {
                        NotificationFilter.ALL -> notificationsToShow
                        NotificationFilter.UNREAD -> notificationsToShow.filter { !it.isRead }
                        NotificationFilter.TRAVEL -> notificationsToShow.filter { it.category == NotificationCategory.TRAVEL }
                        NotificationFilter.RAIN -> notificationsToShow.filter { it.category == NotificationCategory.RAIN }
                        NotificationFilter.UV -> notificationsToShow.filter { it.category == NotificationCategory.UV }
                        NotificationFilter.WARNING -> notificationsToShow.filter { it.category == NotificationCategory.WARNING }
                        NotificationFilter.SUMMARY -> notificationsToShow.filter { it.category == NotificationCategory.SUMMARY }
                        NotificationFilter.UPDATE -> notificationsToShow.filter { it.category == NotificationCategory.UPDATE }
                        NotificationFilter.GENERAL -> notificationsToShow.filter { it.category == NotificationCategory.GENERAL }
                        NotificationFilter.SYSTEM -> notificationsToShow.filter { it.category == NotificationCategory.SYSTEM }
                    }

                    // Tekrar edenleri grupla (Başlık ve Mesaj aynı olanları en yeniye göre teke düşür)
                    base.groupBy { it.getSafeTitle() + it.getSafeMessage() }
                        .map { it.value.maxByOrNull { n -> n.createdAt }!! }
                        .sortedByDescending { it.createdAt }
                }

                if (filteredList.isEmpty()) {
                    EmptyNotificationState(
                        onResetFilter = { viewModel.setFilter(NotificationFilter.ALL) }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredList, key = { it.id }) { notification ->
                            val isSelected = state.selectedIds.contains(notification.id)

                            SwipeableNotificationItem(
                                notification = notification,
                                isSelected = isSelected,
                                isSelectionMode = state.isSelectionMode,
                                onClick = { handleNotificationClick(notification) },
                                onLongClick = { viewModel.toggleSelection(notification.id) },
                                onDelete = { viewModel.deleteNotification(notification.id) },
                                onToggleRead = { viewModel.toggleReadStatus(notification.id) },
                                onToggleSelection = { viewModel.toggleSelection(notification.id) },
                                onNavigateToDetail = { screen, params ->
                                    handleNotificationClick(notification)
                                }
                            )
                        }
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
    onToggleSelection: () -> Unit,
    onNavigateToDetail: (String, Map<String, String>?) -> Unit
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
                onToggleSelection = onToggleSelection,
                onNavigateToDetail = onNavigateToDetail
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
    onNavigateToDetail: (String, Map<String, String>?) -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    val timeStr = remember(notification.createdAt) {
        NotificationDateFormatter.formatCreatedAt(notification.createdAt)
    }
    val eventTimeStr = remember(notification.eventAt) {
        notification.eventAt?.let { NotificationDateFormatter.formatEventTime(it) }
    }

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
        shape = RoundedCornerShape(24.dp),
        color = cardBgColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, cardBorderColor),
        shadowElevation = if (notification.isRead) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
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
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBgColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = notification.category.getIcon(),
                    contentDescription = null,
                    tint = iconBgColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = notification.getSafeTitle(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (notification.isRead) FontWeight.Bold else FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.1.sp
                        ),
                        color = themeColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = timeStr, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = themeColors.textMuted)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.getSafeMessage(),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp, fontSize = 13.sp),
                    color = if (notification.isRead) themeColors.textSecondary.copy(alpha = 0.7f) else themeColors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (eventTimeStr != null && eventTimeStr.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Schedule, null, tint = themeColors.accent.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = eventTimeStr,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = themeColors.accent.copy(alpha = 0.7f)
                        )
                    }
                }

                if (notification.actionLabel != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        onClick = { onNavigateToDetail("", null) },
                        shape = CircleShape,
                        color = themeColors.accent.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = notification.actionLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, fontSize = 10.sp),
                            color = themeColors.accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
    activeFilter: NotificationFilter,
    onFilterChange: (NotificationFilter) -> Unit
) {
    val filters = remember { NotificationFilter.entries }
    val themeColors = HavamaniaTheme.colors

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(filters) { filter ->
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
                NotificationFilter.SYSTEM -> "Sistem"
            }
            FilterChip(isSelected = activeFilter == filter, label = label, onClick = { onFilterChange(filter) })
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
fun EmptyNotificationState(onResetFilter: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = themeColors.accent.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Bu kategoride filtreye uygun bildirim bulunamadı.",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = themeColors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onResetFilter, colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)) {
            Text("FİLTREYİ TEMİZLE", fontWeight = FontWeight.Bold)
        }
    }
}
