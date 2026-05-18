package com.havamania

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.HavamaniaTheme
import kotlinx.coroutines.delay

data class RecommendationStyle(
    val icon: ImageVector,
    val label: String
)

object RecommendationMapper {
    fun getStyle(type: RecommendationType): RecommendationStyle {
        val fixedTitle = "HAVAMANIA ÖNERİSİ"
        return RecommendationStyle(
            icon = Icons.Rounded.AutoAwesome,
            label = fixedTitle
        )
    }

    fun getColor(type: RecommendationType, colors: com.havamania.ui.theme.HavamaniaColors): Color {
        // ALWAYS PREMIUM BLUE/CYAN
        return Color(0xFF3B82F6) // Premium Blue
    }
}

@Composable
fun AiSuggestionCard(
    weather: WeatherData,
    phase: DayPhase,
    userInterests: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
    onAskAiClick: (HavamaniaRecommendation) -> Unit = {}
) {
    val recommendation = remember(weather, userInterests) {
        RecommendationEngine.generateTodayRecommendation(
            weatherData = weather,
            userInterests = userInterests
        )
    }

    RecommendationCard(
        recommendation = recommendation,
        onAskAiClick = { onAskAiClick(recommendation) },
        modifier = modifier
    )
}

@Composable
fun RecommendationCard(
    recommendation: HavamaniaRecommendation,
    onAskAiClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = RecommendationMapper.getStyle(recommendation.type)
    val themeColors = HavamaniaTheme.colors
    val accentColor = themeColors.accent
    val secondaryAccent = themeColors.gradientSecondary.firstOrNull() ?: themeColors.accent.copy(alpha = 0.8f)

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(1000)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(1000)),
        modifier = modifier
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "breathing")

        val gradientShift by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "gradientShift"
        )

        val shimmerProgress by infiniteTransition.animateFloat(
            initialValue = -0.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .drawBehind {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                themeColors.textPrimary.copy(alpha = 0.05f),
                                accentColor.copy(alpha = 0.04f),
                                themeColors.textPrimary.copy(alpha = 0.05f)
                            ),
                            start = Offset(size.width * gradientShift * -0.2f, size.height * gradientShift * -0.2f),
                            end = Offset(size.width * (1f + gradientShift * 0.2f), size.height * (1f + gradientShift * 0.2f))
                        )
                    )

                    val shimmerWidth = 220.dp.toPx()
                    val x = size.width * shimmerProgress
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, accentColor.copy(alpha = 0.08f), Color.Transparent),
                            startX = x - shimmerWidth,
                            endX = x + shimmerWidth
                        )
                    )

                    repeat(8) { i ->
                        val pX = ( (i * 0.15f + gradientShift * 0.3f) % 1f ) * size.width
                        val pY = ( (i * 0.08f + gradientShift * 0.1f) % 1f ) * size.height
                        drawCircle(
                            color = accentColor.copy(alpha = 0.05f),
                            radius = 1.dp.toPx(),
                            center = Offset(pX, pY)
                        )
                    }
                }
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.3f),
                            secondaryAccent.copy(alpha = 0.1f),
                            accentColor.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .blur(10.dp)
                                    .background(accentColor.copy(alpha = 0.2f), CircleShape)
                            )
                            Icon(
                                imageVector = style.icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = style.label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 3.sp,
                                shadow = Shadow(
                                    color = accentColor.copy(alpha = 0.3f),
                                    blurRadius = 8f
                                )
                            ),
                            color = themeColors.textPrimary
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = buildHighlightedText(recommendation.message, recommendation.highlightedWords, accentColor),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 26.sp,
                        color = themeColors.textPrimary.copy(alpha = 0.85f)
                    )
                )

                Spacer(Modifier.height(24.dp))

                AiAskButton(
                    accentColor = accentColor,
                    secondaryColor = secondaryAccent,
                    onClick = onAskAiClick
                )
            }
        }
    }
}

@Composable
fun AiAskButton(
    accentColor: Color,
    secondaryColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressedState = interactionSource.collectIsPressedAsState()
    val isPressed = isPressedState.value

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "pressScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(accentColor, secondaryColor)
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 32.dp, vertical = 18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "DETAYLI ANALİZ İSTE",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = Color.White,
                    fontSize = 13.sp
                )
            )
        }
    }
}

@Composable
private fun buildHighlightedText(
    text: String,
    highlightedWords: List<String>,
    highlightColor: Color
) = buildAnnotatedString {
    val words = text.split(" ")
    words.forEachIndexed { index, word ->
        val cleanWord = word.lowercase().trim(',', '.', '!', '?', ':', ';', '\"', '(', ')')
        val shouldHighlight = highlightedWords.any { it.lowercase() == cleanWord }

        if (shouldHighlight) {
            withStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                append(word)
            }
        } else {
            append(word)
        }

        if (index < words.size - 1) append(" ")
    }
}
