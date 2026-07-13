package com.havamania.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium Standard Dialog
 */
@Composable
fun HavamaniaDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String? = "Vazgeç",
    confirmColor: Color = HavamaniaTheme.colors.accent,
    icon: ImageVector? = null
) {
    val themeColors = HavamaniaTheme.colors

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = themeColors.surface,
        titleContentColor = themeColors.textPrimary,
        textContentColor = themeColors.textSecondary,
        icon = icon?.let { { Icon(it, null, tint = confirmColor, modifier = Modifier.size(32.dp)) } },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismissRequest()
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = confirmText.uppercase(),
                    color = confirmColor,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        },
        dismissButton = dismissText?.let {
            {
                TextButton(onClick = onDismissRequest) {
                    Text(
                        text = it.uppercase(),
                        color = themeColors.textMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

/**
 * Premium Havamania Screen Wrapper
 */
@Composable
fun HavamaniaScreen(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val colors = HavamaniaTheme.colors

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier.havamaniaScreenBackground()
        ) {
            // Subtle atmosphere glow (Top Right)
            Box(
                modifier = Modifier
                    .size(400.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 100.dp, y = (-100).dp)
                    .blur(100.dp)
                    .background(colors.accent.copy(alpha = 0.05f), RoundedCornerShape(200.dp))
            )

            content(paddingValues)
        }
    }
}

/**
 * Premium Glass Card
 */
@Composable
fun HavamaniaGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = LocalResponsiveValues.current.cardRadius,
    alpha: Float = 0.7f,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = HavamaniaTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressedState = interactionSource.collectIsPressedAsState()
    val isPressed = isPressedState.value

    val windowSize = LocalWindowSize.current
    val responsive = LocalResponsiveValues.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "pressScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius))
            .background(colors.surfaceGlass.copy(alpha = alpha))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null, // Custom feedback via scale
                        onClick = onClick
                    )
                } else Modifier
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(colors.border, Color.Transparent)
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        Column(
            modifier = Modifier.padding(if (windowSize.isCompact) 14.dp else 20.dp),
            content = content
        )
    }
}

/**
 * Premium Button
 */
@Composable
fun HavamaniaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val colors = HavamaniaTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressedState = interactionSource.collectIsPressedAsState()
    val isPressed = isPressedState.value

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isLoading) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "buttonScale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed && !isLoading) 0.6f else 0.2f,
        label = "glow"
    )

    val backgroundBrush = if (enabled && !isLoading) {
        if (colors.buttonGradient != null) {
            Brush.linearGradient(colors.buttonGradient)
        } else {
            Brush.linearGradient(listOf(colors.accent, colors.accent))
        }
    } else {
        Brush.linearGradient(listOf(colors.surface.copy(alpha = 0.3f), colors.surface.copy(alpha = 0.3f)))
    }

    Surface(
        onClick = if (!isLoading) onClick else ({}),
        enabled = enabled && !isLoading,
        modifier = modifier
            .scale(scale)
            .height(56.dp)
            .fillMaxWidth()
            .drawBehind {
                if (enabled && !isLoading) {
                    drawRoundRect(
                        color = colors.accent.copy(alpha = glowAlpha * 0.3f),
                        cornerRadius = CornerRadius(18.dp.toPx()),
                        size = size.copy(width = size.width + 4.dp.toPx(), height = size.height + 4.dp.toPx()),
                        topLeft = Offset(-2.dp.toPx(), -2.dp.toPx())
                    )
                }
            },
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = colors.onAccent,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (icon != null) {
                        Icon(icon, null, tint = colors.onAccent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = text.uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp,
                            color = colors.onAccent
                        )
                    )
                }
            }
        }
    }
}

/**
 * Premium Chip Style
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HavamaniaChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = HavamaniaTheme.colors

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surfaceGlass.copy(alpha = 0.3f),
            labelColor = colors.textSecondary,
            selectedContainerColor = colors.accent,
            selectedLabelColor = colors.onAccent,
            selectedLeadingIconColor = colors.onAccent
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colors.border.copy(alpha = 0.2f),
            selectedBorderColor = colors.accent
        )
    )
}

/**
 * Premium TextField
 */
@Composable
fun HavamaniaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null
) {
    val colors = HavamaniaTheme.colors
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, colors.border.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
        placeholder = { Text(placeholder, color = colors.textMuted) },
        leadingIcon = leadingIcon?.let { { Icon(it, null, tint = colors.accent) } },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceGlass.copy(alpha = 0.5f),
            unfocusedContainerColor = colors.surfaceGlass.copy(alpha = 0.5f),
            disabledContainerColor = colors.surfaceGlass.copy(alpha = 0.5f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            cursorColor = colors.accent
        ),
        singleLine = true
    )
}

/**
 * Premium Toggle / Switch
 */
@Composable
fun HavamaniaToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HavamaniaTheme.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onAccent,
            checkedTrackColor = colors.accent,
            uncheckedThumbColor = colors.textSecondary,
            uncheckedTrackColor = colors.surfaceGlass,
            uncheckedBorderColor = colors.border
        )
    )
}

/**
 * Section Label
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = HavamaniaTheme.colors.accent.copy(alpha = 0.8f),
        modifier = modifier.padding(bottom = 10.dp)
    )
}

/**
 * Small Detail Card (e.g., Moon Phase, Sunrise etc.)
 */
@Composable
fun DetailSmallCard(
    label: String,
    value: String,
    emoji: String,
    modifier: Modifier = Modifier
) {
    val themeColors = HavamaniaTheme.colors
    val themeStyles = HavamaniaTheme.styles
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.surfaceGlass.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(themeStyles.cardBorderWidth, themeColors.border)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textSecondary.copy(alpha = 0.6f)
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = themeColors.textPrimary
                )
            )
        }
    }
}

/**
 * Premium Top Bar
 */
@Composable
fun HavamaniaTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = HavamaniaTheme.colors
    val responsive = LocalResponsiveValues.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = responsive.pagePadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Back Button or Spacer
        Box(modifier = Modifier.widthIn(min = 48.dp)) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceGlass.copy(alpha = 0.5f))
                        .border(1.dp, colors.border.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Geri",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Center side: Title with Weight
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Black,
                fontSize = responsive.headerFontSize * 0.6f // Scale down for label style
            ),
            color = colors.textPrimary.copy(alpha = 0.9f),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Right side: Actions
        Row(
            modifier = Modifier.widthIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            actions()
        }
    }
}
