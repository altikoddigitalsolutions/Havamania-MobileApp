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
import com.havamania.ui.theme.*

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
    var isLoading by remember { mutableStateOf(true) }
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
                        text = "Sayfa şu anda açılamıyor",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = HavamaniaTheme.colors.textPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "İnternet bağlantınızı kontrol edip tekrar deneyin.",
                        textAlign = TextAlign.Center,
                        color = HavamaniaTheme.colors.textSecondary
                    )
                    Spacer(Modifier.height(32.dp))
                    HavamaniaPrimaryButton(
                        text = "TEKRAR DENE",
                        onClick = {
                            hasError = false
                            isLoading = true
                            webViewRef?.reload()
                        },
                        icon = Icons.Rounded.Refresh
                    )
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            // User request: Disable JS if not needed.
                            // Static legal pages usually don't need it.
                            settings.javaScriptEnabled = false
                            settings.domStorageEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        hasError = true
                                        isLoading = false
                                    }
                                }

                                override fun onReceivedHttpError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    errorResponse: android.webkit.WebResourceResponse?
                                ) {
                                    super.onReceivedHttpError(view, request, errorResponse)
                                    if (request?.isForMainFrame == true) {
                                        hasError = true
                                        isLoading = false
                                    }
                                }

                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: android.webkit.SslErrorHandler?,
                                    error: android.net.http.SslError?
                                ) {
                                    // Basic safety, can be enhanced
                                    hasError = true
                                    isLoading = false
                                    handler?.cancel()
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

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = HavamaniaTheme.colors.accent)
                    }
                }
            }
        }
    }
}
