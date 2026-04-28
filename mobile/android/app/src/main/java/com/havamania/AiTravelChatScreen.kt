package com.havamania

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTravelChatScreen(
    plan: TravelPlan,
    onBack: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val coroutineScope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages.add(ChatMessage(
                id = UUID.randomUUID().toString(),
                text = "Merhaba! ${plan.destination} seyahatin için hazırım. Hava durumunu analiz ettim, her şeyi sorabilirsin!",
                isUser = false,
                timestamp = System.currentTimeMillis()
            ))
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Seyahat Asistanı", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Seyahat Özeti Kartı
            SelectedTripHeader(plan)

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    if (message.structuredContent != null) {
                        StructuredAiBubble(message.structuredContent!!)
                    } else {
                        ChatBubbleTravel(message)
                    }
                }
            }

            // Öneri Soruları
            QuickQuestionsRow { question ->
                messages.add(ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = question,
                    isUser = true,
                    timestamp = System.currentTimeMillis()
                ))
                simulateAiResponse(question, plan, messages, coroutineScope)
            }

            ChatInputArea(
                value = messageText,
                onValueChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        val q = messageText
                        messages.add(ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = q,
                            isUser = true,
                            timestamp = System.currentTimeMillis()
                        ))
                        messageText = ""
                        simulateAiResponse(q, plan, messages, coroutineScope)
                    }
                }
            )
        }
    }
}

private fun simulateAiResponse(
    query: String,
    plan: TravelPlan,
    messages: MutableList<ChatMessage>,
    scope: kotlinx.coroutines.CoroutineScope
) {
    scope.launch {
        delay(1000)
        val structured = if (query.contains("almalıyım") || query.contains("hazırlık")) {
            StructuredAiContent(
                summary = "${plan.destination} için hazırlıkların tamamlanıyor.",
                suggestions = listOf("Rahat yürüyüş ayakkabısı al", "Müze kartını unutma"),
                warnings = listOf("Gidiş günü akşam serin olabilir"),
                packingTips = listOf("Küçük bir sırt çantası", "Taşınabilir şarj cihazı")
            )
        } else {
            null
        }

        if (structured != null) {
            messages.add(ChatMessage(
                id = UUID.randomUUID().toString(),
                structuredContent = structured,
                isUser = false,
                timestamp = System.currentTimeMillis()
            ))
        } else {
            messages.add(ChatMessage(
                id = UUID.randomUUID().toString(),
                text = "Anladım, ${plan.destination} hakkında başka ne bilmek istersin?",
                isUser = false,
                timestamp = System.currentTimeMillis()
            ))
        }
    }
}

@Composable
fun StructuredAiBubble(content: StructuredAiContent) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .background(colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        content.summary?.let {
            Text(it, fontWeight = FontWeight.Bold, color = colorScheme.primary, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))
        }

        StructuredSection("Öneriler", Icons.Rounded.Lightbulb, content.suggestions, colorScheme.primary)
        StructuredSection("Dikkat", Icons.Rounded.Warning, content.warnings, Color(0xFFF59E0B))
        StructuredSection("Valiz İpucu", Icons.Rounded.Backpack, content.packingTips, colorScheme.secondary)
    }
}

@Composable
private fun StructuredSection(title: String, icon: ImageVector, items: List<String>, accentColor: Color) {
    if (items.isEmpty()) return
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = accentColor)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = accentColor)
        }
        Spacer(Modifier.height(6.dp))
        items.forEach { item ->
            Text("• $item", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), modifier = Modifier.padding(start = 24.dp))
        }
    }
}

@Composable
fun SelectedTripHeader(plan: TravelPlan) {
    Card(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Map, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(plan.destination, fontWeight = FontWeight.Bold)
                Text("Hava analizi aktif", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun QuickQuestionsRow(onQuestionClick: (String) -> Unit) {
    val questions = listOf("Ne almalıyım?", "Yağmur riski?", "Nasıl giyinmeliyim?", "Günlük plan")
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(questions) { q ->
            SuggestionChip(
                onClick = { onQuestionClick(q) },
                label = { Text(q, fontSize = 12.sp) }
            )
        }
    }
}

@Composable
fun ChatInputArea(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Row(modifier = Modifier.padding(16.dp).navigationBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Asistana sor...") },
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                shape = RoundedCornerShape(28.dp)
            )
            IconButton(onClick = onSend, colors = IconButtonDefaults.filledIconButtonColors()) {
                Icon(Icons.Rounded.Send, null)
            }
        }
    }
}

@Composable
fun ChatBubbleTravel(message: ChatMessage) {
    val colorScheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            color = if (message.isUser) colorScheme.primary else colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(message.text ?: "", modifier = Modifier.padding(12.dp), color = if (message.isUser) colorScheme.onPrimary else colorScheme.onSurface)
        }
    }
}
