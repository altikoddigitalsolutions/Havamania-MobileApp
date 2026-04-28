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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(onBack: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val coroutineScope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    // Başlangıç mesajı
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages.add(ChatMessage(
                id = UUID.randomUUID().toString(),
                text = "Merhaba! Ben Havamania Asistanı. Seyahat planların, hava durumu veya tatil hazırlıkların hakkında sana nasıl yardımcı olabilirim?",
                isUser = false,
                timestamp = System.currentTimeMillis()
            ))
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Havamania Asistan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = colorScheme.onBackground
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = colorScheme.onBackground) }
                }
            )
        },
        containerColor = colorScheme.background
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
                color = colorScheme.surfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Sorunuzu yazın...", color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colorScheme.onSurface,
                            unfocusedTextColor = colorScheme.onSurface,
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        maxLines = 3
                    )

                    FloatingActionButton(
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
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Rounded.Send, null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleGeneral(message: ChatMessage) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (message.isUser) colorScheme.primary.copy(alpha = 0.2f) else colorScheme.surfaceVariant.copy(alpha = 0.8f),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (message.isUser) 20.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 20.dp
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (message.isUser) colorScheme.primary.copy(alpha = 0.3f) else colorScheme.outline.copy(alpha = 0.1f)
            )
        ) {
            Text(
                text = message.text ?: "",
                modifier = Modifier.padding(14.dp),
                color = colorScheme.onSurface,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}
