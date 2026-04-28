package com.havamania

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TravelInspiredSplashScreen(onNavigateToHome: () -> Unit) {
    val context = LocalContext.current

    // Animasyon State'leri
    val contentAlpha = remember { Animatable(0f) }
    val contentScale = remember { Animatable(0.98f) }
    val progress = remember { Animatable(0f) }

    // Kaynak Kontrolleri
    val logoResId = context.resources.getIdentifier("havamania_logo_clean", "drawable", context.packageName)
    val bgResId = context.resources.getIdentifier("splash_travel_bg", "drawable", context.packageName)
    val loadingIconResId = context.resources.getIdentifier("ic_suitcase_clean", "drawable", context.packageName)

    LaunchedEffect(Unit) {
        launch {
            contentAlpha.animateTo(1f, tween(1500, easing = EaseOutCubic))
        }
        launch {
            contentScale.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
        }

        progress.animateTo(1f, tween(2000, easing = LinearOutSlowInEasing))

        delay(200)
        onNavigateToHome()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- 1. SİNEMATİK ARKA PLAN ---
        if (bgResId != 0) {
            Image(
                painter = painterResource(id = bgResId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)))
        }

        // --- 2. PREMIUM OVERLAY (Okunabilirlik ve Derinlik) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.25f),
                        0.4f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.65f)
                    )
                )
        )

        // --- 3. ANA MERKEZ BLOĞU ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .offset(y = (-40).dp)
                .scale(contentScale.value)
                .alpha(contentAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // LOGO (Border ve Debug Border Kaldırıldı)
            if (logoResId != 0) {
                Image(
                    painter = painterResource(id = logoResId),
                    contentDescription = "Havamania Logo",
                    modifier = Modifier.size(110.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // MARKA ADI
            Text(
                text = "Havamania",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 32.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // SLOGAN
            Text(
                text = "Hava durumunu akıllıca takip et,\nseyahatlerini akıllıca planla.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
        }

        // --- 4. ALT KISIM: ZARİF LOADING ALANI ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 70.dp)
                .alpha(contentAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (loadingIconResId != 0) {
                Image(
                    painter = painterResource(id = loadingIconResId),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    alpha = 0.7f
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Yükleniyor...",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = Color.White.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // İnce ve Modern Progress Bar
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.value)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF3B82F6), Color(0xFF60A5FA))
                            )
                        )
                )
            }
        }
    }
}
