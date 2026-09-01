package com.havamania

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.*
import java.time.LocalDateTime

@Composable
fun AssistantWeatherCard(data: WeatherData, c: HavamaniaColors) {
    Surface(
        color = c.accent.copy(alpha = 0.08f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, c.accent.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = data.cityName,
                    style = HavamaniaTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = c.textPrimary
                )
                if (data.districtName != null) {
                    Text(
                        text = " - ${data.districtName}",
                        style = HavamaniaTheme.typography.bodyMedium,
                        color = c.textSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = data.temperature,
                    style = HavamaniaTheme.typography.heroTemperature.copy(fontSize = 32.sp),
                    color = c.accent
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = data.condition,
                        style = HavamaniaTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = c.textPrimary
                    )
                    Text(
                        text = "Hissedilen: ${data.feelsLike}",
                        style = HavamaniaTheme.typography.bodySmall,
                        color = c.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: AltikodChatMessage,
    c: HavamaniaColors,
    onRetry: (String) -> Unit,
    onActionClick: (AssistantAction) -> Unit
) {
    val isUser = message.isUser

    val bubbleColor = if (isUser) c.accent else c.surface
    val contentColor = if (isUser) Color.White else c.textPrimary

    val shape = if (isUser) {
        RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
    } else {
        RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            tonalElevation = if (isUser) 0.dp else 2.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (isUser) {
                    Text(text = message.text, color = contentColor)
                } else {
                    AssistantMessageContent(text = message.text, c = c)
                }
            }
        }

        message.action?.let { action ->
            Spacer(modifier = Modifier.height(8.dp))
            AssistantActionButton(action, c, onActionClick)
        }
    }
}

@Composable
private fun AssistantMessageContent(text: String, c: HavamaniaColors) {
    val annotatedString = buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            var currentLine = line

            if (currentLine.contains("**")) {
                val parts = currentLine.split("**")
                parts.forEachIndexed { i, part ->
                    if (i % 2 == 1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Black)) {
                            append(part)
                        }
                    } else {
                        append(part)
                    }
                }
            } else if (currentLine.trim().startsWith("-") || currentLine.trim().startsWith("•")) {
                withStyle(SpanStyle(color = c.accent)) {
                    append(" • ")
                }
                append(currentLine.trim().substring(1).trim())
            } else {
                append(currentLine)
            }

            if (index < lines.size - 1) append("\n")
        }
    }

    Text(
        text = annotatedString,
        style = HavamaniaTheme.typography.bodyMedium,
        color = c.textPrimary
    )
}

@Composable
fun AssistantActionButton(
    action: AssistantAction,
    c: HavamaniaColors,
    onClick: (AssistantAction) -> Unit
) {
    val icon = when (action.type) {
        AssistantActionType.CREATE_TRAVEL_PLAN -> Icons.Rounded.CalendarMonth
        else -> Icons.Rounded.ArrowForward
    }

    Surface(
        onClick = { onClick(action) },
        color = c.accent.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, c.accent.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = c.accent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = action.label,
                style = HavamaniaTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = c.accent
            )
        }
    }
}

@Composable
fun ChatInput(
    onSend: (String) -> Unit,
    isSending: Boolean,
    c: HavamaniaColors,
    s: HavamaniaStyles,
    contextInfo: String? = null
) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface)
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        if (contextInfo != null) {
            Text(
                text = contextInfo,
                style = HavamaniaTheme.typography.caption,
                color = c.accent,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Asistan'a sor...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )

            IconButton(
                onClick = {
                    if (text.isNotBlank() && !isSending) {
                        onSend(text)
                        text = ""
                    }
                },
                enabled = text.isNotBlank() && !isSending
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Rounded.Send, null, tint = c.accent)
                }
            }
        }
    }
}

@Composable
fun TodaySummarySection(data: WeatherData, c: HavamaniaColors) {
    Surface(
        color = c.surface.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "BUGÜN ÖZET",
                style = HavamaniaTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                color = c.textMuted
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${data.cityName} için bugün hava ${data.condition.lowercase()}. Sıcaklık ${data.temperature} civarında seyrediyor.",
                style = HavamaniaTheme.typography.bodyMedium,
                color = c.textPrimary
            )
        }
    }
}

@Composable
fun FeatureCards(c: HavamaniaColors, s: HavamaniaStyles, onFeatureClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val features = listOf(
            Triple(Icons.Rounded.CloudQueue, "Hava durumunu yorumla", "Ankara'da hava nasıl?"),
            Triple(Icons.Rounded.Flight, "Seyahat havasını değerlendir", "Seyahatim için hava uygun mu?"),
            Triple(Icons.Rounded.TipsAndUpdates, "Günlük öneri ver", "Bugün ne giymeliyim?")
        )

        features.forEach { (icon, title, prompt) ->
            Surface(
                modifier = Modifier.weight(1f).height(100.dp).clickable { onFeatureClick(prompt) },
                color = c.surface.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, c.border.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(icon, null, tint = c.accent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = title,
                        style = HavamaniaTheme.typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                        textAlign = TextAlign.Center,
                        color = c.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun QuickSuggestions(onSuggestionClick: (String) -> Unit, c: HavamaniaColors, hasTrip: Boolean) {
    val suggestions = mutableListOf(
        "Bugün hava nasıl?",
        "Yağmur bekleniyor mu?",
        "Yarın hava nasıl?"
    )
    if (hasTrip) {
        suggestions.add("Seyahatim için hava uygun mu?")
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { prompt ->
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onSuggestionClick(prompt) },
                color = c.accent.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, c.accent.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.ChatBubbleOutline, null, tint = c.accent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = prompt,
                        style = HavamaniaTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = c.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicator(c: HavamaniaColors) {
    Row(
        modifier = Modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "typing")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(c.accent.copy(alpha = alpha))
            )
        }
    }
}
