package com.havamania

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.havamania.ui.theme.HavamaniaTheme

@Composable
fun PermissionRationaleDialog(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = themeColors.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = themeColors.accent.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = themeColors.accent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = themeColors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = themeColors.textSecondary.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text(buttonText, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Daha Sonra", color = themeColors.textSecondary)
                }
            }
        }
    }
}

@Composable
fun LocationPermissionRationale(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    PermissionRationaleDialog(
        icon = Icons.Rounded.LocationOn,
        title = "Konum İzni Gerekli",
        description = "Havamania, bulunduğun şehre göre anlık hava analizi ve kişisel öneriler sunmak için konumunu kullanır.",
        buttonText = "KONUMU PAYLAŞ",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun NotificationPermissionRationale(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    PermissionRationaleDialog(
        icon = Icons.Rounded.Notifications,
        title = "Bildirimlere İzin Ver",
        description = "Yağış, UV ve seyahat analizlerindeki önemli değişiklikleri zamanında bildirebilmemiz için bildirim iznine ihtiyacımız var.",
        buttonText = "BİLDİRİMLERİ AÇ",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
