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
    onStatClick: (String) -> Unit
) {
    val themeColors = HavamaniaTheme.colors

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(112.dp).background(themeColors.accent.copy(alpha = 0.06f), CircleShape))

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
                            .joinToString("") { it.take(1).uppercase(java.util.Locale.forLanguageTag("tr-TR")) }
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

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(
                text = name.ifBlank { "Gezgin" },
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                    personality.uppercase(java.util.Locale.forLanguageTag("tr")),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, fontSize = 11.sp),
                    color = themeColors.accent
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            val entries = listOf(Triple("İlgi", "Tercihler", stats["İlgi"] ?: "0"),
                Triple("Rota", "Rotalar", stats["Rota"] ?: "0"),
                Triple("Analiz", "Kaydedilen analiz", stats["Analiz"] ?: "0"))
            if (maxWidth < 360.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f) {
                Column(Modifier.fillMaxWidth()) {
                    entries.forEach { (key, label, value) ->
                        ProfileStatItem(value, label, Modifier.fillMaxWidth()) { onStatClick(key) }
                    }
                }
            } else Row(Modifier.fillMaxWidth()) {
                entries.forEach { (key, label, value) ->
                    ProfileStatItem(value, label, Modifier.weight(1f)) { onStatClick(key) }
                }
            }
        }
    }
}

@Composable
fun ProfileStatItem(value: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = HavamaniaTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(HavamaniaTheme.styles.radiusSmall))
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick).padding(8.dp)) {
        Text(value, style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Black), color = colors.textPrimary)
        Text(label, style = HavamaniaTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = colors.textSecondary)
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
    val actions = listOf(Triple("Şehirler", Icons.Rounded.Map, onManageCities),
        Triple("Asistan geçmişi", Icons.Rounded.History, onAiHistory),
        Triple("Seyahatlerim", Icons.Rounded.Route, onMyTravels),
        Triple("Tercihlerim", Icons.Rounded.AutoAwesome, onPersonalization),
        Triple("Premium", Icons.Rounded.WorkspacePremium, onPremium),
        Triple("Profili düzenle", Icons.Rounded.AccountCircle, onEditProfile))
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val singleColumn = maxWidth < 360.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            actions.chunked(if (singleColumn) 1 else 2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (title, icon, action) -> QuickActionItem(title, icon, Modifier.weight(1f), action) }
                }
            }
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
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(themeStyles.spacingMD)
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(themeStyles.spacingMD))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = HavamaniaTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                color = themeColors.textPrimary
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
