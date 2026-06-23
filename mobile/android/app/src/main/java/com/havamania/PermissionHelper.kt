package com.havamania

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.havamania.ui.theme.HavamaniaTheme

/**
 * Modern Permission Management Helper
 */
object PermissionHelper {

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

@Composable
fun PermissionExplanationDialog(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, null, tint = themeColors.accent) },
        title = { Text(title, fontWeight = FontWeight.Black) },
        text = { Text(description) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
            ) {
                Text("İZİN VER")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ŞİMDİ DEĞİL", color = themeColors.textSecondary)
            }
        },
        containerColor = themeColors.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    )
}

@Composable
fun LocationPermissionFlow(
    onPermissionResult: (Boolean) -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        onPermissionResult(granted)
    }

    if (showDialog) {
        PermissionExplanationDialog(
            title = "Konum İzni Gerekli",
            description = "Havamania, bulunduğun şehre göre anlık hava analizi ve kişisel öneriler sunmak için konumunu kullanır.",
            icon = Icons.Rounded.LocationOn,
            onConfirm = {
                showDialog = false
                launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            },
            onDismiss = {
                showDialog = false
                onPermissionResult(false)
            }
        )
    }
}

@Composable
fun NotificationPermissionFlow(
    onPermissionResult: (Boolean) -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult(isGranted)
    }

    if (showDialog && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PermissionExplanationDialog(
            title = "Bildirim İzni Gerekli",
            description = "Yağış, UV ve seyahat analizlerindeki önemli değişiklikleri zamanında bildirebilmemiz için bildirim iznine ihtiyacımız var.",
            icon = Icons.Rounded.Notifications,
            onConfirm = {
                showDialog = false
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onDismiss = {
                showDialog = false
                onPermissionResult(false)
            }
        )
    } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        SideEffect { onPermissionResult(true) }
    }
}
