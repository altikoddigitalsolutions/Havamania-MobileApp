package com.havamania

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

// --- VIEWMODEL ---
class AiChatViewModel(application: Application) : AndroidViewModel(application) {
    private val api = AltikodChatFactory.create()
    private val botId = "1"
    private var sessionId = UUID.randomUUID().toString()

    private val repository: WeatherRepository by lazy {
        val database = WeatherDatabase.getDatabase(application)
        WeatherRepository(weatherDao = database.weatherDao())
    }

    private val _messages = MutableStateFlow<List<AltikodChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _config = MutableStateFlow<AltikodBotConfig?>(null)
    val config = _config.asStateFlow()

    var weatherData: WeatherData? = null
    var userAboutMe: String = ""
    var userInterests: Set<String> = emptySet()

    init {
        loadConfig()
        ensureWeatherData()
    }

    private fun ensureWeatherData() {
        viewModelScope.launch {
            if (weatherData != null) return@launch
            try {
                // Shared source of truth check: try to get from default city cache
                com.havamania.ui.theme.ThemeManager.getDefaultCity(getApplication()).firstOrNull()?.let { defaultCity ->
                    repository.getWeatherData(defaultCity.latitude, defaultCity.longitude, defaultCity.name, defaultCity.district)
                        .firstOrNull()?.let { data ->
                            weatherData = data
                        }
                }
            } catch (e: Exception) {
                android.util.Log.e("AssistantData", "Failed to pre-fetch weather data: ${e.message}")
            }
        }
    }

    private fun loadConfig() {
        viewModelScope.launch {
            try {
                val cfg = api.getConfig(botId)
                _config.value = cfg
            } catch (e: Exception) {
                // Hata durumunda varsayılan config veya sessiz hata
            }
        }
    }

    fun sendMessage(text: String, systemContext: String? = null, isRetry: Boolean = false) {
        if (text.isBlank() || _isLoading.value) return

        if (!isRetry) {
            val userMsg = AltikodChatMessage(text = text, isUser = true)
            // Duplicating check for quick clicks
            val lastMsg = _messages.value.lastOrNull { it.isUser }
            if (lastMsg == null || lastMsg.text != text || System.currentTimeMillis() - lastMsg.timestamp > 2000) {
                _messages.value = _messages.value + userMsg
            }
        }

        _isLoading.value = true

        val fullQuestion = if (systemContext != null) "$systemContext $text" else text

        viewModelScope.launch {
            var retryCount = 0
            var success = false

            android.util.Log.d("AssistantRequest",
                "prompt=$text, city=${weatherData?.cityName}, " +
                "hasCurrentWeather=${weatherData != null}, " +
                "temperature=${weatherData?.temperature}, " +
                "condition=${weatherData?.condition}")

            while (!success && retryCount <= 1) {
                try {
                    // Timeout logic: 20 seconds
                    val response = kotlinx.coroutines.withTimeout(20000) {
                        api.sendMessage(botId, AltikodChatRequest(question = fullQuestion, session_id = sessionId))
                    }

                    if (response.answer.isBlank()) {
                        throw Exception("Empty response from AI")
                    }

                    _messages.value = _messages.value + AltikodChatMessage(text = response.answer, isUser = false)
                    success = true
                } catch (e: Exception) {
                    retryCount++
                    if (retryCount > 1) {
                        android.util.Log.e("AssistantError",
                            "errorType=${e.javaClass.simpleName}, " +
                            "exceptionMessage=${e.message}, " +
                            "userPrompt=$text, " +
                            "city=${weatherData?.cityName}")

                        val fallbackText = RecommendationEngine.generateAssistantFallbackReply(
                            userPrompt = text,
                            weatherData = weatherData,
                            aboutMe = userAboutMe,
                            interests = userInterests
                        )

                        _messages.value = _messages.value + AltikodChatMessage(
                            text = fallbackText,
                            isUser = false,
                            isFallback = true,
                            retryPrompt = text
                        )
                    } else {
                        android.util.Log.w("AssistantRetry", "Retrying AI request... (Attempt $retryCount)")
                        kotlinx.coroutines.delay(1000)
                    }
                }
            }
            _isLoading.value = false
        }
    }

    fun finishChat(onFinished: (List<AltikodChatMessage>) -> Unit) {
        val currentMessages = _messages.value
        if (currentMessages.isEmpty()) return

        onFinished(currentMessages)
        resetChat()
    }

    fun resetChat() {
        _messages.value = emptyList()
        sessionId = UUID.randomUUID().toString()
    }
}

// --- UI BILESENLERI ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    initialRecommendation: HavamaniaRecommendation? = null,
    weatherData: WeatherData? = null,
    onBack: () -> Unit,
    viewModel: AiChatViewModel = viewModel(),
    historyViewModel: AiHistoryViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel()
) {
    val themeColors = HavamaniaTheme.colors
    val isDark = themeColors.isDark
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val aboutMe by themeViewModel.userAboutMe.collectAsStateWithLifecycle()
    val userInterests by themeViewModel.userInterests.collectAsStateWithLifecycle()

    // Sync data with ViewModel
    LaunchedEffect(weatherData, aboutMe, userInterests) {
        viewModel.weatherData = weatherData
        viewModel.userAboutMe = aboutMe
        viewModel.userInterests = userInterests
    }

    var showEndChatDialog by remember { mutableStateOf(false) }

    val bgColors = themeColors.gradientPrimary

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // İlk girdi (initialRecommendation) varsa gönder
    LaunchedEffect(initialRecommendation) {
        initialRecommendation?.let {
            val context = buildPersonalizedContext(aboutMe, userInterests)
            viewModel.sendMessage(it.message, systemContext = context)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(bgColors)))

        Column(modifier = Modifier.fillMaxSize()) {
            HavamaniaTopBar(
                title = config?.name ?: "HAVAMANIA ASİSTAN",
                onBack = onBack,
                actions = {
                    if (messages.isNotEmpty()) {
                        Surface(
                            onClick = { showEndChatDialog = true },
                            color = themeColors.accent.copy(alpha = 0.15f),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.StopCircle,
                                    contentDescription = null,
                                    tint = themeColors.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Sohbeti Bitir",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = themeColors.accent
                                )
                            }
                        }
                    } else {
                        val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
                        val sparkleAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
                            label = "alpha"
                        )
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = themeColors.accent,
                            modifier = Modifier.size(24.dp).alpha(sparkleAlpha).padding(end = 12.dp)
                        )
                    }
                }
            )

            if (messages.isEmpty()) {
                // Başlangıç Ekranı (Empty State)
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    WelcomeCard(config?.welcome_message ?: "Merhaba! Havamania Asistan'a hoş geldiniz. Size nasıl yardımcı olabilirim?", themeColors)

                    if (aboutMe.isNotBlank() || userInterests.isNotEmpty()) {
                        PersonalizedContextCard(aboutMe, themeColors)
                    }

                    Spacer(Modifier.height(24.dp))

                    FeatureCards(
                        themeColors = themeColors,
                        onCardClick = { prompt ->
                            val context = buildPersonalizedContext(aboutMe, userInterests)
                            viewModel.sendMessage(prompt, systemContext = context)
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    QuickSuggestions(
                        onSuggestionClick = { prompt ->
                            val context = buildPersonalizedContext(aboutMe, userInterests)
                            viewModel.sendMessage(prompt, systemContext = context)
                        },
                        themeColors = themeColors
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatBubble(
                            message = message,
                            themeColors = themeColors,
                            onRetry = { prompt ->
                                viewModel.sendMessage(prompt, isRetry = true)
                            }
                        )
                    }
                    if (isLoading) {
                        item {
                            TypingIndicator(themeColors)
                        }
                    }
                }
            }

            ChatInput(
                onSend = { prompt ->
                    val context = if (messages.isEmpty()) {
                        buildPersonalizedContext(aboutMe, userInterests)
                    } else null
                    viewModel.sendMessage(prompt, systemContext = context)
                },
                isLoading = isLoading,
                themeColors = themeColors
            )
        }
    }

    if (showEndChatDialog) {
        AlertDialog(
            onDismissRequest = { showEndChatDialog = false },
            containerColor = themeColors.surface,
            title = { Text("Sohbeti sonlandırmak istiyor musun?", style = MaterialTheme.typography.titleLarge, color = themeColors.textPrimary) },
            text = { Text("Bu sohbet AI geçmişine kaydedilecek.", color = themeColors.textSecondary) },
            dismissButton = {
                TextButton(onClick = { showEndChatDialog = false }) {
                    Text("Vazgeç", color = themeColors.textSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndChatDialog = false
                        viewModel.finishChat { msgs ->
                            val firstUserMsg = msgs.firstOrNull { it.isUser }?.text ?: "AI Sohbet"
                            val firstAiMsg = msgs.firstOrNull { !it.isUser }?.text ?: "Hava durumu analizi"
                            historyViewModel.addHistoryItem(
                                title = firstUserMsg,
                                summary = firstAiMsg,
                                messages = msgs,
                                cityName = null
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("Sohbeti Bitir", color = Color.White)
                }
            }
        )
    }
}

fun buildPersonalizedContext(aboutMe: String, interests: Set<String>): String {
    if (aboutMe.isBlank() && interests.isEmpty()) return ""

    val interestsStr = if (interests.isNotEmpty()) {
        "Kullanıcının ilgi alanları: ${interests.joinToString(", ")}. "
    } else ""

    val aboutMeStr = if (aboutMe.isNotBlank()) {
        "Kullanıcı hakkında bilgi: \"$aboutMe\". "
    } else ""

    return "Sistem Talimatı: Aşağıdaki kullanıcı profiline göre daha kişiselleştirilmiş bir cevap ver. " +
            "$interestsStr$aboutMeStr"
}

@Composable
fun PersonalizedContextCard(aboutMe: String, themeColors: HavamaniaColors) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).padding(top = 16.dp).fillMaxWidth(),
        color = themeColors.accent.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = themeColors.accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Kişiselleştirilmiş mod aktif: AI seni tanıyor.",
                style = MaterialTheme.typography.labelSmall,
                color = themeColors.accent
            )
        }
    }
}

@Composable
fun WelcomeCard(message: String, themeColors: HavamaniaColors) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = themeColors.surfaceGlass.copy(alpha = 0.5f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.border.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(themeColors.accent.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = themeColors.accent, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = themeColors.textPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

enum class AssistantFeatureType { CLOTHING, ACTIVITY, TRAVEL }

data class AssistantFeature(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val prompt: String,
    val type: AssistantFeatureType
)

@Composable
fun FeatureCards(themeColors: HavamaniaColors, onCardClick: (String) -> Unit) {
    val features = remember {
        listOf(
            AssistantFeature(
                title = "Akıllı Giysi Önerisi",
                desc = "Hava durumuna göre ne giyeceğinizi söyler.",
                icon = Icons.Rounded.Checkroom,
                prompt = "Bugünkü hava durumuna göre ne giymeliyim? Sıcaklık, rüzgar, yağış ve UV durumuna göre pratik kıyafet önerisi ver.",
                type = AssistantFeatureType.CLOTHING
            ),
            AssistantFeature(
                title = "Aktivite Analizi",
                desc = "Dışarı çıkmak için en iyi zamanı belirler.",
                icon = Icons.Rounded.DirectionsRun,
                prompt = "Bugün dışarı çıkmak, yürüyüş yapmak, spor yapmak veya açık hava aktivitesi için uygun mu? Hava durumuna göre en uygun saatleri ve dikkat etmem gerekenleri söyle.",
                type = AssistantFeatureType.ACTIVITY
            ),
            AssistantFeature(
                title = "Seyahat Planlama",
                desc = "Rotalarınız için özel hava tavsiyeleri verir.",
                icon = Icons.Rounded.Route,
                prompt = "Yaklaşan seyahatlerim ve mevcut hava durumuna göre bana seyahat planlama önerisi verir misin? Valiz, ulaşım, rota ve hava riskleri açısından tavsiye ver.",
                type = AssistantFeatureType.TRAVEL
            )
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        features.forEach { feature ->
            FeatureCard(
                title = feature.title,
                desc = feature.desc,
                icon = feature.icon,
                themeColors = themeColors,
                onClick = { onCardClick(feature.prompt) }
            )
        }
    }
}

@Composable
fun FeatureCard(title: String, desc: String, icon: ImageVector, themeColors: HavamaniaColors, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = themeColors.surfaceGlass.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.border.copy(alpha = 0.1f)),
        modifier = Modifier.width(160.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, color = themeColors.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = themeColors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

data class AssistantSuggestion(
    val label: String,
    val prompt: String
)

@Composable
fun QuickSuggestions(
    onSuggestionClick: (String) -> Unit,
    themeColors: HavamaniaColors
) {
    val suggestions = remember {
        listOf(
            AssistantSuggestion(
                label = "Bugün ne giymeliyim?",
                prompt = "Bugünkü hava durumuna göre ne giymeliyim?"
            ),
            AssistantSuggestion(
                label = "Hafta sonu hava nasıl?",
                prompt = "Bulunduğum şehir için hafta sonu hava durumu nasıl görünüyor? Plan yaparken nelere dikkat etmeliyim?"
            ),
            AssistantSuggestion(
                label = "Dışarı çıkmak için uygun mu?",
                prompt = "Bugün dışarı çıkmak için hava uygun mu? Yağış, rüzgar, sıcaklık ve UV durumuna göre yorumla."
            ),
            AssistantSuggestion(
                label = "Yağmur yağacak mı?",
                prompt = "Bugün bulunduğum şehirde yağmur yağma ihtimali var mı? Hangi saatlerde dikkatli olmalıyım?"
            ),
            AssistantSuggestion(
                label = "Valizime ne almalıyım?",
                prompt = "Yaklaşan seyahatlerim ve hava durumuna göre valizime neler almalıyım?"
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Öneriler",
            style = MaterialTheme.typography.labelSmall,
            color = themeColors.textSecondary.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                Surface(
                    onClick = { onSuggestionClick(suggestion.prompt) },
                    color = themeColors.surfaceGlass.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.border.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = suggestion.label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeColors.textPrimary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun ChatBubble(
    message: AltikodChatMessage,
    themeColors: HavamaniaColors,
    onRetry: (String) -> Unit = {}
) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isUser) themeColors.accent else themeColors.surfaceGlass
    val textColor = if (message.isUser) Color.White else themeColors.textPrimary
    val shape = if (message.isUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            tonalElevation = 2.dp,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (message.isFallback && message.retryPrompt != null) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        onClick = { onRetry(message.retryPrompt) },
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Refresh, null, tint = textColor, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Tekrar dene",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatInput(onSend: (String) -> Unit, isLoading: Boolean, themeColors: HavamaniaColors) {
    var text by remember { mutableStateOf("") }
    var showMicSoon by remember { mutableStateOf(false) }

    Surface(
        color = themeColors.surfaceGlass.copy(alpha = 0.9f),
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { showMicSoon = true },
                enabled = !isLoading
            ) {
                Icon(Icons.Rounded.Mic, contentDescription = "Sesli Yaz", tint = themeColors.textSecondary)
            }

            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Bir şeyler sorun...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = themeColors.accent
                ),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.isNotBlank()) {
                            onSend(text)
                            text = ""
                        }
                    }
                ),
                enabled = !isLoading
            )

            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend(text)
                        text = ""
                    }
                },
                enabled = !isLoading && text.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = themeColors.accent,
                    contentColor = Color.White,
                    disabledContainerColor = themeColors.accent.copy(alpha = 0.5f)
                )
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Gönder")
            }
        }
    }

    if (showMicSoon) {
        AlertDialog(
            onDismissRequest = { showMicSoon = false },
            containerColor = themeColors.surface,
            title = { Text("YAKINDA", fontWeight = FontWeight.Black, color = themeColors.textPrimary) },
            text = { Text("Sesli asistan özelliği çok yakında Havamania'ya eklenecek!", color = themeColors.textSecondary) },
            confirmButton = {
                TextButton(onClick = { showMicSoon = false }) {
                    Text("TAMAM", fontWeight = FontWeight.Black, color = themeColors.accent)
                }
            }
        )
    }
}

@Composable
fun TypingIndicator(themeColors: HavamaniaColors) {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "typing")
        repeat(3) { index ->
            val delay = index * 200
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = delay),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_alpha"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(themeColors.textSecondary.copy(alpha = alpha))
            )
        }
    }
}
