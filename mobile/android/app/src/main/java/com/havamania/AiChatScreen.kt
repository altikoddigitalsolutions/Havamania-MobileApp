package com.havamania

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.HavamaniaTheme
import com.havamania.ui.theme.HavamaniaScreen
import com.havamania.ui.theme.HavamaniaTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

import com.havamania.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    initialRecommendation: HavamaniaRecommendation? = null,
    onBack: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val coroutineScope = rememberCoroutineScope()
    val themeColors = HavamaniaTheme.colors

    // Başlangıç mesajı
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            if (initialRecommendation != null) {
                messages.add(ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "Havamania Önerisi için detaylı analiz hazırladım: ${initialRecommendation.message}\n\nBu durum senin için neden önemli? Mevcut hava şartlarına göre ${initialRecommendation.highlightedWords.joinToString(", ")} konularında sana özel önerilerim şunlar...",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                ))

                // Simulate detailed AI analysis
                delay(800)
                messages.add(ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "Analizime göre; ${initialRecommendation.type} odaklı bu durum yaklaşık 4 saat daha devam edecek. İlgi alanların ve planların göz önüne alındığında, bugün için en verimli strateji esnek kalmak olacaktır. Başka neyi merak ediyorsun?",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                ))
            } else {
                messages.add(ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "Merhaba! Ben Havamania Asistanı. Seyahat planların, hava durumu veya tatil hazırlıkların hakkında sana nasıl yardımcı olabilirim?",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                ))
            }
        }
    }

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "HAVAMANIA ASİSTAN",
                onBack = onBack,
                actions = {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = themeColors.accent, modifier = Modifier.size(20.dp))
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Mesaj Listesi
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { message ->
                    ChatBubbleGeneral(message)
                }
            }

            // Mesaj Giriş Alanı
            Surface(
                color = themeColors.surfaceGlass.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, themeColors.border.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Sorunuzu yazın...", color = themeColors.textSecondary.copy(alpha = 0.6f)) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = themeColors.textPrimary,
                            unfocusedTextColor = themeColors.textPrimary,
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = themeColors.border.copy(alpha = 0.15f),
                            unfocusedContainerColor = themeColors.surface.copy(alpha = 0.2f),
                            focusedContainerColor = themeColors.surface.copy(alpha = 0.3f)
                        ),
                        maxLines = 3
                    )

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                val userMsg = messageText
                                messages.add(ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    text = userMsg,
                                    isUser = true,
                                    timestamp = System.currentTimeMillis()
                                ))
                                messageText = ""

                                coroutineScope.launch {
                                    delay(1000)
                                    val aiResponse = when {
                                        userMsg.lowercase().contains("tatil") || userMsg.lowercase().contains("almalıyım") ->
                                            "Harika bir tatil planı! Gideceğin yerin hava durumuna göre yanına mutlaka güneş kremi ve akşam serinliği için ince bir ceket almanı öneririm."
                                        userMsg.lowercase().contains("yağmur") ->
                                            "Bugün yağmur bekleniyor. Dışarı çıkacaksan şemsiyeni yanına almayı ve kapalı mekan aktivitelerini tercih etmeyi unutma."
                                        else -> "Anladım. Sana en doğru öneriyi sunabilmem için hava durumu detaylarını analiz ediyorum."
                                    }
                                    messages.add(ChatMessage(
                                        id = UUID.randomUUID().toString(),
                                        text = aiResponse,
                                        isUser = false,
                                        timestamp = System.currentTimeMillis()
                                    ))
                                }
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(themeColors.accent)
                    ) {
                        Icon(Icons.Rounded.Send, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleGeneral(message: ChatMessage) {
    val themeColors = HavamaniaTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (message.isUser) themeColors.accent.copy(alpha = 0.9f) else themeColors.surfaceGlass.copy(alpha = 0.4f),
            shape = RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp,
                bottomStart = if (message.isUser) 24.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 24.dp
            ),
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                if (message.isUser) themeColors.accent else themeColors.border.copy(alpha = 0.1f)
            )
        ) {
            Text(
                text = message.text ?: "",
                modifier = Modifier.padding(16.dp),
                color = if (message.isUser) Color.White else themeColors.textPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}

