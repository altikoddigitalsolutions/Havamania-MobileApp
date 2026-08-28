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
import com.havamania.ui.theme.HavamaniaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TravelInspiredSplashScreen(
    onNavigate: () -> Unit,
    isReady: Boolean = true
) {
    val context = LocalContext.current

    // Animasyon State'leri
    val contentAlpha = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }

    // Kaynak Kontrolleri
    val logoResId = context.resources.getIdentifier("havamania_logo_clean", "drawable", context.packageName)
    val bgResId = context.resources.getIdentifier("splash_travel_bg", "drawable", context.packageName)

    LaunchedEffect(Unit) {
        launch {
            // İçerik (yazılar vs) yavaşça gelsin
            contentAlpha.animateTo(1f, tween(800, easing = EaseOutCubic))
        }
        progress.animateTo(1f, tween(1500, easing = LinearOutSlowInEasing))
    }

    // Navigasyon Kontrolü: Hem minimum animasyon süresi hem de veri hazır olmalı
    LaunchedEffect(isReady, progress.value) {
        if (isReady && progress.value >= 1f) {
            delay(200)
            onNavigate()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- 1. SİNEMATİK ARKA PLAN ---
        // Sistem splash ile aynı renk arka plan (Süreklilik için)
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)))

        if (bgResId != 0) {
            Image(
                painter = painterResource(id = bgResId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(contentAlpha.value * 0.7f),
                contentScale = ContentScale.Crop
            )
        }

        // --- 2. PREMIUM OVERLAY (Okunabilirlik ve Derinlik) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.3f),
                        0.5f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.7f)
                    )
                )
        )

        // --- 3. ANA MERKEZ BLOĞU ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // LOGO
            if (logoResId != 0) {
                Image(
                    painter = painterResource(id = logoResId),
                    contentDescription = "Havamania Logo",
                    modifier = Modifier.size(160.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Column(
                modifier = Modifier.alpha(contentAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // MARKA ADI
                Text(
                    text = "Havamania",
                    style = HavamaniaTheme.typography.screenTitle.copy(
                        fontSize = 32.sp,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // SLOGAN
                Text(
                    text = "Hava durumunu akıllıca takip et,\nseyahatlerini akıllıca planla.",
                    style = HavamaniaTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp
                    ),
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
            }
        }

        // --- 4. ALT KISIM: PROGRESS ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .width(160.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.value)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF00C2FF), Color(0xFF0077FF))
                        )
                    )
            )
        }
    }
}
