package com.havamania

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.HavamaniaDesign

/**
 * Shimmer efektini oluşturan temel Brush
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.12f),
        Color.White.copy(alpha = 0.05f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translation"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
}

/**
 * Hero Card için Skeleton Placeholder
 */
@Composable
fun HeroCardSkeleton(brush: Brush) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .background(brush)
    )
}

/**
 * Saatlik Tahmin Kartları için Skeleton Placeholder
 */
@Composable
fun HourlyForecastSkeleton(brush: Brush) {
    Column {
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .size(width = 140.dp, height = 14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            items(6) {
                Box(
                    modifier = Modifier
                        .width(74.dp)
                        .height(140.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .background(brush)
                )
            }
        }
    }
}

/**
 * Detay Grid Kartları için Skeleton Placeholder
 */
@Composable
fun DetailsGridSkeleton(brush: Brush) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .background(brush)
                )
            }
        }
    }
}

/**
 * Tüm Ana Ekran Yükleme Durumu
 */
@Composable
fun HomeScreenLoading() {
    val shimmerBrush = rememberShimmerBrush()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 28.dp)
    ) {
        // 1. Hero Card Skeleton
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            HeroCardSkeleton(shimmerBrush)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Saatlik Tahmin Skeleton
        HourlyForecastSkeleton(shimmerBrush)

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Detaylar Skeleton
        DetailsGridSkeleton(shimmerBrush)
    }
}
