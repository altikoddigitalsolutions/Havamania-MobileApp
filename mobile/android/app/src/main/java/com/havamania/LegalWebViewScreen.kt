package com.havamania

import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.havamania.ui.theme.HavamaniaScreen
import com.havamania.ui.theme.HavamaniaTopBar
import com.havamania.ui.theme.HavamaniaTheme

@Composable
fun LegalWebViewScreen(
    title: String,
    url: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val networkMonitor = remember { ConnectivityManagerNetworkMonitor(context) }
    val isOnline by networkMonitor.isOnline.collectAsStateWithLifecycle(initialValue = true)

    var hasError by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(title = title, onBack = onBack)
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!isOnline || hasError) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WifiOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = HavamaniaTheme.colors.accent.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = if (!isOnline) "İnternet Bağlantısı Yok" else "Bir Hata Oluştu",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = HavamaniaTheme.colors.textPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Lütfen bağlantınızı kontrol edin ve tekrar deneyin.",
                        textAlign = TextAlign.Center,
                        color = HavamaniaTheme.colors.textSecondary
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = {
                            hasError = false
                            webViewRef?.reload()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HavamaniaTheme.colors.accent)
                    ) {
                        Icon(Icons.Rounded.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text("TEKRAR DENE", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    // Sadece ana sayfa hatalarını yakala
                                    if (request?.isForMainFrame == true) {
                                        hasError = true
                                    }
                                }
                            }
                            loadUrl(url)
                            webViewRef = this
                        }
                    },
                    update = {
                        webViewRef = it
                    }
                )
            }
        }
    }
}
