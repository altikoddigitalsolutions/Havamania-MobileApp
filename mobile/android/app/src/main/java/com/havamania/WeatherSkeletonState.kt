package com.havamania

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.HavamaniaTheme

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.12f),
        Color.White.copy(alpha = 0.05f),
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnim, y = translateAnim)
        )
    )
}

@Composable
fun HomeScreenLoading() {
    val styles = HavamaniaTheme.styles
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(styles.pagePadding)
            .statusBarsPadding()
    ) {
        // Hero Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(styles.radiusExtraLarge))
                .shimmerEffect()
        )

        Spacer(modifier = Modifier.height(styles.spacingLG))

        // Insight Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(styles.radiusLarge))
                .shimmerEffect()
        )

        Spacer(modifier = Modifier.height(styles.spacingLG))

        // Hourly Header
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(20.dp)
                .shimmerEffect()
        )

        Spacer(modifier = Modifier.height(styles.spacingMD))

        // Hourly List Skeleton
        Row(horizontalArrangement = Arrangement.spacedBy(styles.spacingMD)) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .size(width = 88.dp, height = 160.dp)
                        .clip(RoundedCornerShape(styles.radiusLarge))
                        .shimmerEffect()
                )
            }
        }

        Spacer(modifier = Modifier.height(styles.spacingLG))

        // Recommendation Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(styles.radiusLarge))
                .shimmerEffect()
        )

        Spacer(modifier = Modifier.height(styles.spacingLG))

        // Daily Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(styles.radiusExtraLarge))
                .shimmerEffect()
        )
    }
}

@Composable
fun TravelListSkeleton() {
    val styles = HavamaniaTheme.styles
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(styles.pagePadding)
    ) {
        // Hero Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(styles.radiusLarge))
                .shimmerEffect()
        )

        Spacer(modifier = Modifier.height(styles.spacingLG))

        // Calendar Stripe Skeleton
        Row(horizontalArrangement = Arrangement.spacedBy(styles.spacingMD)) {
            repeat(6) {
                Box(
                    modifier = Modifier
                        .size(width = 50.dp, height = 80.dp)
                        .clip(RoundedCornerShape(styles.radiusSmall))
                        .shimmerEffect()
                )
            }
        }

        Spacer(modifier = Modifier.height(styles.spacingLG))

        // List Skeletons
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(styles.radiusLarge))
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(styles.spacingMD))
        }
    }
}
