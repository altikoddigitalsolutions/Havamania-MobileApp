package com.havamania

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    initialRecommendation: HavamaniaRecommendation? = null,
    conversationId: String? = null,
    onRecommendationHandled: () -> Unit = {},
    onBack: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToTravelCreate: (String, String?) -> Unit,
    viewModel: AiChatViewModel = viewModel(),
    historyViewModel: AiHistoryViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
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

    var showEndChatDialog by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(initialRecommendation) {
        initialRecommendation?.let { rec ->
            val context = buildPersonalizedContext(aboutMe, userInterests)
            viewModel.sendMessage(rec.message, systemContext = context)
            onRecommendationHandled()
        }
    }

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "ASİSTAN",
                onBack = {
                    if (messages.isNotEmpty()) showExitConfirm = true else onBack()
                },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = { showEndChatDialog = true }) {
                            Icon(Icons.Rounded.Save, null, tint = themeColors.accent)
                        }
                    } else {
                        IconButton(onClick = onNavigateToHistory) {
                            Icon(Icons.Rounded.History, null, tint = themeColors.textPrimary)
                        }
                    }
                }
            )
        },
        bottomBar = {
            ChatInput(
                onSend = { prompt ->
                    val context = buildPersonalizedContext(aboutMe, userInterests)
                    viewModel.sendMessage(prompt, systemContext = context)
                },
                isSending = isSending,
                c = themeColors,
                s = themeStyles,
                contextInfo = if (activeTrip != null) "Yaklaşan seyahat bağlamı kullanılıyor" else null
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
            if (messages.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    val welcomeMsg = config?.welcome_message ?: "Merhaba! Havamania Asistan'a hoş geldiniz. Size nasıl yardımcı olabilirim?"
                    val personalizedGreeting = if (!profile?.name.isNullOrBlank()) "Merhaba ${profile?.name?.split(" ")?.first()}" else "Merhaba"

                    WelcomeCard("$personalizedGreeting! $welcomeMsg", themeColors, themeStyles)

                    if (aboutMe.isNotBlank() || userInterests.isNotEmpty()) {
                        PersonalizedContextCard(aboutMe, themeColors)
                    }

                    if (activeTrip != null) {
                        ActiveTripContextCard(activeTrip, themeColors)
                    }

                    currentWeatherData?.let { data ->
                        AssistantWeatherCard(data, themeColors)
                        TodaySummarySection(data, themeColors)
                    }

                    AssistantSectionLabel("NELER YAPABİLİRİM?")
                    FeatureCards(themeColors, themeStyles) { prompt ->
                        val context = buildPersonalizedContext(aboutMe, userInterests)
                        viewModel.sendMessage(prompt, systemContext = context)
                    }

                    AssistantSectionLabel("HIZLI SORULAR")
                    QuickSuggestions(
                        onSuggestionClick = { prompt ->
                            val context = buildPersonalizedContext(aboutMe, userInterests)
                            viewModel.sendMessage(prompt, systemContext = context)
                        },
                        c = themeColors,
                        hasTrip = activeTrip != null
                    )

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
                            onRetry = { viewModel.sendMessage(it, isRetry = true) },
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
            }

            if (requestState == AssistantRequestState.ERROR && messages.isEmpty()) {
                ErrorMessage(null) {
                    val lastUserMsg = messages.lastOrNull { it.isUser }
                    if (lastUserMsg != null) viewModel.sendMessage(lastUserMsg.text, isRetry = true)
                }
            }
        }
    }

    if (showEndChatDialog) {
        HavamaniaDialog(
            onDismissRequest = { showEndChatDialog = false },
            title = "Sohbeti Kaydet?",
            text = "Bu sohbeti geçmişe kaydedip yeni bir sohbet başlatmak istiyor musun?",
            confirmText = "KAYDET VE BİTİR",
            onConfirm = {
                historyViewModel.addHistoryItem(
                    title = messages.firstOrNull { it.isUser }?.text?.take(30) ?: "Hava Sohbeti",
                    summary = messages.lastOrNull { !it.isUser }?.text?.take(100) ?: "",
                    messages = messages,
                    cityName = currentWeatherData?.cityName
                )
                viewModel.resetChat()
                showEndChatDialog = false
            }
        )
    }

    if (showExitConfirm) {
        HavamaniaDialog(
            onDismissRequest = { showExitConfirm = false },
            title = "Çıkış Yap?",
            text = "Sohbeti kaydetmeden çıkmak istediğine emin misin?",
            confirmText = "ÇIK",
            onConfirm = { showExitConfirm = false; onBack() }
        )
    }
}

@Composable
private fun WelcomeCard(message: String, c: HavamaniaColors, s: HavamaniaStyles) {
    Surface(
        color = c.surface.copy(alpha = 0.4f),
        shape = RoundedCornerShape(HavamaniaTheme.styles.radiusLarge),
        modifier = Modifier.fillMaxWidth().padding(HavamaniaTheme.styles.spacingMD)
    ) {
        Column(modifier = Modifier.padding(HavamaniaTheme.styles.spacingLG)) {
            Text("HAVAMANİA ASİSTAN", style = HavamaniaTheme.typography.sectionTitle, color = c.accent)
            Spacer(Modifier.height(HavamaniaTheme.styles.spacingSM))
            Text(message, style = HavamaniaTheme.typography.bodyLarge, color = c.textPrimary)
        }
    }
}

@Composable
private fun PersonalizedContextCard(aboutMe: String, c: HavamaniaColors) {
    Surface(
        color = c.accent.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = c.accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Kişisel tercihlerine göre özelleştirildi", style = HavamaniaTheme.typography.caption.copy(fontWeight = FontWeight.Bold), color = c.accent)
        }
    }
}

@Composable
private fun ErrorMessage(detail: String? = null, onRetry: () -> Unit) {
    HavamaniaErrorState(
        title = "Asistan Hatası",
        description = detail ?: "Asistan şu anda yanıt hazırlayamadı. Lütfen internet bağlantını kontrol et ve tekrar dene.",
        onRetry = onRetry
    )
}

@Composable
fun AssistantSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp),
        style = HavamaniaTheme.typography.sectionTitle
    )
}

fun buildPersonalizedContext(aboutMe: String, interests: Set<String>): String {
    if (aboutMe.isBlank() && interests.isEmpty()) return ""
    return "[Kullanıcı Profili]\n" +
           (if (aboutMe.isNotBlank()) "Hakkında: $aboutMe\n" else "") +
           (if (interests.isNotEmpty()) "İlgi Alanları: ${interests.joinToString()}\n" else "")
}

@Composable
private fun ActiveTripContextCard(trip: TravelPlan, c: HavamaniaColors) {
    HavamaniaCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        backgroundColor = c.accent.copy(alpha = 0.05f),
        borderColor = c.accent.copy(alpha = 0.1f),
        padding = 16.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.FlightTakeoff, null, tint = c.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Yaklaşan Seyahat",
                    style = HavamaniaTheme.typography.caption,
                    color = c.accent
                )
                Text(
                    text = "${trip.displayName} • ${trip.startDate}",
                    style = HavamaniaTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = c.textPrimary
                )
            }
        }
    }
}
