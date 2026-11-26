package com.havamania

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    initialRecommendation: HavamaniaRecommendation? = null,
    conversationId: String? = null,
    onRecommendationHandled: () -> Unit = {},
    onBack: () -> Unit,
    onStartNewChat: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToTravelCreate: (String, String?) -> Unit,
    viewModel: AiChatViewModel = viewModel(),
    historyViewModel: AiHistoryViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel()
) {
    val historySaveState by historyViewModel.saveState.collectAsStateWithLifecycle()
    val historyOwner by historyViewModel.detailOwner.collectAsStateWithLifecycle()
    val savingHistory = historySaveState == ProfileSaveState.Saving
    val consentState by viewModel.assistantConsent.state.collectAsStateWithLifecycle()
    AssistantConsentDialog(consentState, viewModel.assistantConsent::save, viewModel::revokeAssistantConsent,
        viewModel::retryAssistantConsent, viewModel::dismissAssistantConsent)
    val openPhotoPicker = rememberAssistantPhotoPicker(
        viewModel::photoSelectionToken,
        { uris, token -> viewModel.addPhotos(uris, token) },
        viewModel::photoPickerUnavailable
    )
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    LaunchedEffect(consentState.allowed, consentState.visible, isSending, historySaveState) {
        if (!historyViewModel.saveState.value.blocksAutomaticSend()) viewModel.resumeAfterConsent()
    }
    val requestState by viewModel.requestState.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val currentWeatherData by viewModel.weatherData.collectAsStateWithLifecycle()

    val aboutMe by themeViewModel.userAboutMe.collectAsStateWithLifecycle()
    val userInterests by themeViewModel.userInterests.collectAsStateWithLifecycle()
    val assistantTone by themeViewModel.assistantTone.collectAsStateWithLifecycle()
    val language by themeViewModel.language.collectAsStateWithLifecycle()

    // P3.5: User Profile for personalized greeting
    val userProfileRepository = remember { UserProfileRepository.getInstance() }
    val profile by userProfileRepository.profile.collectAsStateWithLifecycle()

    val upcomingPlans by viewModel.activeTravels.collectAsStateWithLifecycle()
    val activeTrip = upcomingPlans.firstOrNull()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            calendarDateChanges().collect { viewModel.refreshTravelDate() }
        }
    }

    LaunchedEffect(aboutMe, userInterests, assistantTone, language) {
        viewModel.userAboutMe = aboutMe
        viewModel.userInterests = userInterests
        viewModel.assistantTone = assistantTone
        viewModel.language = language
    }

    LaunchedEffect(conversationId) {
        if (conversationId != null) {
            viewModel.loadConversation(conversationId)
        }
    }

    val listState = rememberLazyListState()
    val themeColors = HavamaniaTheme.colors
    val themeStyles = HavamaniaTheme.styles
    val responsive = LocalResponsiveValues.current
    val windowSize = LocalWindowSize.current

    var showEndChatDialog by rememberSaveable(historyOwner) { mutableStateOf(false) }
    var showExitConfirm by remember(historyOwner) { mutableStateOf(false) }
    LaunchedEffect(historySaveState) {
        if (historySaveState == ProfileSaveState.Saved) {
            viewModel.resetChat()
            showEndChatDialog = false
            historyViewModel.resetSaveState()
            if (conversationId != null) onStartNewChat()
        }
    }

    FollowAssistantConversation(listState, messages, isLoading)

    LaunchedEffect(initialRecommendation, isSending, consentState.visible, historySaveState) {
        if (isSending || consentState.visible || historyViewModel.saveState.value.blocksAutomaticSend()) {
            return@LaunchedEffect
        }
        initialRecommendation?.let { rec ->
            val context = buildPersonalizedContext(aboutMe, userInterests)
            if (viewModel.sendRecommendation(rec.message, context)) onRecommendationHandled()
        }
    }

    val awaitingHistory = conversationId != null && messages.isEmpty()
    val historyLoadFailed = awaitingHistory && requestState == AssistantRequestState.ERROR
    val historyLoading = awaitingHistory &&
        (requestState == AssistantRequestState.IDLE || requestState == AssistantRequestState.LOADING)
    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "ASİSTAN",
                onBack = {
                    if (messages.isNotEmpty()) showExitConfirm = true else onBack()
                },
                actions = {
                    IconButton(onClick = { viewModel.assistantConsent.open() }) {
                        Icon(Icons.Rounded.PrivacyTip, "Asistan izinleri", tint = themeColors.textPrimary)
                    }
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = { showEndChatDialog = true }, enabled = !isSending && !savingHistory) {
                            Icon(Icons.Rounded.Save, "Sohbeti kaydet",
                                tint = if (isSending || savingHistory) themeColors.textMuted else themeColors.accent)
                        }
                    } else {
                        IconButton(onClick = onNavigateToHistory) {
                            Icon(Icons.Rounded.History, "Sohbet geçmişi", tint = themeColors.textPrimary)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!historyLoadFailed && !historyLoading) ChatInput(
                text = viewModel.draft,
                onTextChange = viewModel::updateDraft,
                onSend = viewModel::sendDraft,
                isSending = isSending,
                c = themeColors,
                s = themeStyles,
                photos = viewModel.selectedPhotos,
                preparingPhotos = viewModel.preparingPhotos,
                photoError = viewModel.photoError,
                onRemovePhoto = viewModel::removePhoto,
                onAttachPhoto = openPhotoPicker.takeIf { viewModel.photosEnabled },
                photoControlsEnabled = !consentState.visible && !savingHistory
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .then(
                    if (windowSize.isTablet || windowSize.isLargeTablet)
                        Modifier.widthIn(max = responsive.maxContentWidth).align(Alignment.TopCenter)
                    else Modifier.fillMaxWidth()
                )
        ) {
            if (historyLoading) {
                HavamaniaLoading(text = stringResource(R.string.assistant_history_loading))
            } else if (historyLoadFailed) {
                HavamaniaErrorState(
                    title = "Sohbet yüklenemedi",
                    description = "Sohbet geçmişi açılamadı. Yeniden deneyebilirsin.",
                    onRetry = { viewModel.loadConversation(conversationId) }
                )
            } else if (messages.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    val firstName = profile?.name?.trim()?.split(" ")?.firstOrNull() ?: ""
                    val greeting = if (firstName.isNotEmpty() && firstName.lowercase() != "user") {
                        "Merhaba $firstName! Sana nasıl yardımcı olabilirim? 😊"
                    } else {
                        "Merhaba! Sana nasıl yardımcı olabilirim? 😊"
                    }

                    WelcomeCard(greeting, themeColors)

                    if (activeTrip != null) {
                        ActiveTripContextCard(activeTrip, themeColors, enabled = !isSending) {
                            val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale("tr"))
                            val dateStr = activeTrip.startDate.format(formatter)
                            viewModel.sendMessage("${activeTrip.city} seyahati için $dateStr tarihindeki hava durumu nasıl?")
                        }
                    }

                    currentWeatherData?.let { data ->
                        AssistantWeatherCard(data, themeColors)
                    }

                    Spacer(Modifier.height(8.dp))
                    AssistantStarterSuggestions(themeColors, enabled = !isSending) { viewModel.sendMessage(it) }

                    Spacer(Modifier.height(32.dp))
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = messages, key = { it.id }) { message ->
                        ChatBubble(message, themeColors,
                            onRetry = { viewModel.sendMessage(it, isRetry = true, retryRequestId = message.retryRequestId) },
                            onActionClick = { action ->
                                if (action.type == AssistantActionType.CREATE_TRAVEL_PLAN) {
                                    onNavigateToTravelCreate(action.city ?: "", action.startDate)
                                }
                            }
                        )
                    }
                    if (isLoading) {
                        item { TypingIndicator(themeColors) }
                    }
                }
                JumpToLatestAssistantMessage(
                    listState, messages.lastIndex + if (isLoading) 1 else 0,
                    Modifier.align(Alignment.BottomEnd).padding(16.dp)
                )
            }

        }
    }

    if (showEndChatDialog || savingHistory) {
        HavamaniaDialog(
            onDismissRequest = { if (!savingHistory) showEndChatDialog = false },
            title = "Sohbeti Kaydet?",
            text = when (val state = historySaveState) {
                ProfileSaveState.Saving -> "Kayıt onayı bekleniyor…"
                is ProfileSaveState.Failed -> state.message
                else -> if (isSending) "Yanıt tamamlandığında sohbeti kaydedebilirsin."
                    else "Bu sohbeti geçmişe kaydedip yeni bir sohbet başlatmak istiyor musun?"
            },
            confirmText = if (historySaveState is ProfileSaveState.Failed) "TEKRAR DENE" else "KAYDET VE BİTİR",
            confirmEnabled = !savingHistory && !isSending && messages.isNotEmpty(),
            dismissEnabled = !savingHistory,
            onConfirm = confirm@{
                val snapshot = viewModel.messages.value
                if (viewModel.isSending.value || snapshot.isEmpty()) return@confirm
                historyViewModel.addHistoryItem(
                    id = viewModel.currentConversationId,
                    title = snapshot.firstOrNull { it.isUser }?.text?.take(30) ?: "Hava Sohbeti",
                    summary = snapshot.lastOrNull { !it.isUser }?.text?.take(100) ?: "",
                    messages = snapshot,
                    cityName = currentWeatherData?.cityName
                )
            }
        )
    }

    if (showExitConfirm) {
        HavamaniaDialog(
            onDismissRequest = { showExitConfirm = false },
            title = "Çıkış Yap?",
            text = "Sohbeti kaydetmeden çıkmak istediğine emin misin?",
            confirmText = "ÇIK",
            onConfirm = { historyViewModel.resetSaveState(); showExitConfirm = false; onBack() }
        )
    }
}

private fun ProfileSaveState.blocksAutomaticSend(): Boolean =
    this == ProfileSaveState.Saving || this == ProfileSaveState.Saved

@Composable
internal fun JumpToLatestAssistantMessage(listState: LazyListState, lastIndex: Int, modifier: Modifier = Modifier) {
    val showButton by remember(listState, lastIndex) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.let { it.index < lastIndex } == true
        }
    }
    val scope = rememberCoroutineScope()
    if (showButton) {
        FilledTonalButton(
            onClick = { scope.launch { listState.scrollToItem(lastIndex) } },
            modifier = modifier
        ) {
            Text(stringResource(R.string.assistant_jump_to_latest))
        }
    }
}

@Composable
internal fun FollowAssistantConversation(listState: LazyListState, messages: List<AltikodChatMessage>, isLoading: Boolean) {
    var previousMessageCount by rememberSaveable { mutableIntStateOf(0) }
    val latestMessage = messages.lastOrNull()
    LaunchedEffect(latestMessage?.id, messages.size, isLoading) {
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        val wasNearEnd = previousMessageCount == 0 ||
            (lastVisibleIndex != null && lastVisibleIndex >= previousMessageCount - 1)
        val newUserMessage = latestMessage?.isUser == true && messages.size > previousMessageCount
        previousMessageCount = messages.size
        if (messages.isNotEmpty() && (wasNearEnd || newUserMessage)) {
            val target = if (isLoading) messages.size else messages.lastIndex
            listState.animateScrollToItem(target)
        }
    }
}

@Composable
private fun WelcomeCard(message: String, c: HavamaniaColors) {
    Text(message, style = HavamaniaTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        color = c.textPrimary, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp))
}

fun buildPersonalizedContext(aboutMe: String, interests: Set<String>): String {
    if (aboutMe.isBlank() && interests.isEmpty()) return ""
    return "[Kullanıcı Profili]\n" +
           (if (aboutMe.isNotBlank()) "Hakkında: $aboutMe\n" else "") +
           (if (interests.isNotEmpty()) "İlgi Alanları: ${interests.joinToString()}\n" else "")
}

@Composable
internal fun ActiveTripContextCard(trip: TravelPlan, c: HavamaniaColors, enabled: Boolean, onAsk: () -> Unit) {
    val formatter = remember { java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.forLanguageTag("tr")) }
    val formattedDate = remember(trip.startDate) { trip.startDate.format(formatter) }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        color = c.accent.copy(alpha = 0.04f), shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, c.accent.copy(alpha = 0.1f))
    ) {
        BoxWithConstraints(Modifier.padding(16.dp)) {
            val summary: @Composable (Modifier) -> Unit = { modifier ->
                Row(modifier, verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.FlightTakeoff, null, tint = c.accent, modifier = Modifier.size(24.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Yaklaşan seyahat", style = HavamaniaTheme.typography.bodySmall, color = c.textSecondary)
                        Text(trip.displayName, style = HavamaniaTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = c.textPrimary)
                        Text(formattedDate, style = HavamaniaTheme.typography.bodySmall, color = c.textSecondary)
                    }
                }
            }
            val ask: @Composable (Modifier) -> Unit = { modifier ->
                TextButton(onClick = onAsk, enabled = enabled, modifier = modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = c.accent)) {
                    Text("Havasını sor", style = HavamaniaTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
            if (maxWidth / androidx.compose.ui.platform.LocalDensity.current.fontScale < 400.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    summary(Modifier.fillMaxWidth())
                    ask(Modifier.fillMaxWidth())
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    summary(Modifier.weight(1f))
                    ask(Modifier)
                }
            }
        }
    }
}
