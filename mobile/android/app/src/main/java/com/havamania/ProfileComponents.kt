package com.havamania

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.havamania.ui.theme.*

const val IS_AVATAR_UPLOAD_ENABLED = false

@Composable
fun PremiumProfileHeader(
    name: String,
    bio: String,
    imageUri: String?,
    avatarVersion: Long = 0,
    isUploading: Boolean = false,
    interests: Set<String>,
    aboutMe: String,
    stats: Map<String, String>,
    onAvatarClick: () -> Unit,
    onEditClick: () -> Unit,
    onStatClick: (String) -> Unit
) {
    val themeColors = HavamaniaTheme.colors

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            val infiniteTransition = rememberInfiniteTransition(label = "profile_glow")
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.05f, targetValue = 0.2f,
                animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
                label = "glow_alpha"
            )

            Box(
                modifier = Modifier
                    .size(140.dp)
                    .blur(40.dp)
                    .background(themeColors.accent.copy(alpha = glowAlpha), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(themeColors.surfaceGlass)
                    .border(1.5.dp, Brush.linearGradient(listOf(themeColors.accent, themeColors.accent.copy(0.4f))), CircleShape)
                    .then(if (IS_AVATAR_UPLOAD_ENABLED) Modifier.clickable { onAvatarClick() } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(imageUri)
                            .diskCacheKey("$imageUri-$avatarVersion")
                            .memoryCacheKey("$imageUri-$avatarVersion")
                            .build(),
                        contentDescription = "Profil fotoğrafı",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Premium Initials Fallback
                    val initials = remember(name) {
                        name.split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .joinToString("") { it.take(1).uppercase() }
                    }

                    if (initials.isNotEmpty()) {
                        Text(
                            text = initials,
                            style = HavamaniaTheme.typography.cardTitle.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp
                            ),
                            color = themeColors.accent
                        )
                    } else {
                        Icon(Icons.Rounded.Person, null, tint = themeColors.textPrimary.copy(0.7f), modifier = Modifier.size(50.dp))
                    }
                }

                if (isUploading) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    }
                } else if (IS_AVATAR_UPLOAD_ENABLED) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Icon(Icons.Rounded.CameraAlt, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.padding(bottom = 6.dp).size(14.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(
                text = name.ifBlank { "Gezgin" },
                style = HavamaniaTheme.typography.screenTitle.copy(fontWeight = FontWeight.Black),
                color = themeColors.textPrimary
            )
        }

        if (bio.isNotBlank()) {
            Text(
                text = bio,
                style = MaterialTheme.typography.bodyMedium,
                color = themeColors.textSecondary.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val personality = remember(interests, aboutMe) { generateAiPersonality(interests, aboutMe) }
        Surface(
            color = themeColors.accent.copy(alpha = 0.12f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = themeColors.accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    personality.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, fontSize = 11.sp),
                    color = themeColors.accent
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ProfileStatItem(stats["İlgi"] ?: "0", "TERCİHLER") { onStatClick("İlgi") }
            ProfileStatItem(stats["Rota"] ?: "0", "ROTALAR") { onStatClick("Rota") }
            ProfileStatItem(stats["Analiz"] ?: "0", "KAYDEDİLEN ANALİZ") { onStatClick("Analiz") }
        }
    }
}

@Composable
fun ProfileStatItem(value: String, label: String, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(HavamaniaTheme.styles.radiusSmall)).clickable { onClick() }.padding(HavamaniaTheme.styles.spacingXS)
    ) {
        Text(value, style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Black), color = themeColors.textPrimary)
        Text(label, style = HavamaniaTheme.typography.caption.copy(fontWeight = FontWeight.Bold), color = themeColors.textMuted)
    }
}

fun generateAiPersonality(interests: Set<String>, aboutMe: String): String {
    val t = aboutMe.lowercase()
    return when {
        t.contains("firtina") || interests.contains("firtina_takibi") -> "Atmosfer Avcısı"
        t.contains("kamp") || interests.contains("kamp") || interests.contains("outdoor") -> "Doğa Gezgini"
        t.contains("cocuk") || interests.contains("cocuklar_icin") -> "Aile Meteoroloğu"
        t.contains("drone") || interests.contains("drone") -> "Gökyüzü Kaşifi"
        t.contains("motorsiklet") || interests.contains("motorsiklet") -> "Yol Savaşçısı"
        interests.contains("snowboard") || interests.contains("kayak") -> "Kış Tutkunu"
        interests.size > 12 -> "Hava Gurusu"
        interests.size > 5 -> "Atmosfer Kaşifi"
        else -> "Yeni Hava Meraklısı"
    }
}

@Composable
fun QuickActionsGrid(
    onManageCities: () -> Unit,
    onAiHistory: () -> Unit,
    onMyTravels: () -> Unit,
    onPersonalization: () -> Unit,
    onEditProfile: () -> Unit,
    onPremium: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionItem("Şehirler", Icons.Rounded.Map, Modifier.weight(1f), onManageCities)
            QuickActionItem("AI Geçmişi", Icons.Rounded.History, Modifier.weight(1f), onAiHistory)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionItem("Seyahatlerim", Icons.Rounded.Route, Modifier.weight(1f), onMyTravels)
            QuickActionItem("Tercihlerim", Icons.Rounded.AutoAwesome, Modifier.weight(1f), onPersonalization)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionItem("Premium", Icons.Rounded.WorkspacePremium, Modifier.weight(1f), onPremium)
            QuickActionItem("Profili Düzenle", Icons.Rounded.AccountCircle, Modifier.weight(1f), onEditProfile)
        }
    }
}

@Composable
fun QuickActionItem(title: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    val themeStyles = HavamaniaTheme.styles
    Surface(
        onClick = onClick,
        color = themeColors.surfaceGlass.copy(alpha = 0.3f),
        shape = RoundedCornerShape(themeStyles.radiusMedium),
        border = BorderStroke(1.dp, themeColors.border.copy(alpha = 0.1f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(themeStyles.spacingMD)
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(themeColors.accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(themeStyles.spacingMD))
            Text(
                text = title,
                style = HavamaniaTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                color = themeColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        style = HavamaniaTheme.typography.sectionTitle,
        modifier = Modifier.padding(start = HavamaniaTheme.styles.spacingXS, bottom = HavamaniaTheme.styles.spacingSM)
    )
}
