package com.havamania

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHistoryDetailScreen(
    itemId: String,
    onBack: () -> Unit,
    viewModel: AiHistoryViewModel = viewModel()
) {
    val state by viewModel.detailState.collectAsStateWithLifecycle()
    val owner by viewModel.detailOwner.collectAsStateWithLifecycle()
    LaunchedEffect(itemId, owner) { viewModel.loadDetail(itemId) }
    val item = (state as? AiHistoryDetailState.Ready)?.item
    val themeColors = HavamaniaTheme.colors
    val themeStyles = HavamaniaTheme.styles
    val scrollState = rememberScrollState()

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "ANALİZ DETAYI",
                onBack = onBack
            )
        }
    ) { padding ->
        if (item != null) {
            val dateFormat = remember { SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("tr")) }
            val dateStr = remember(item.timestamp) { dateFormat.format(Date(item.timestamp)) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(themeStyles.pagePadding)
            ) {
                Text(
                    text = dateStr,
                    style = HavamaniaTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                    color = themeColors.accent
                )
                Spacer(modifier = Modifier.height(themeStyles.spacingSM))
                Text(
                    text = item.title,
                    style = HavamaniaTheme.typography.screenTitle.copy(fontWeight = FontWeight.Black),
                    color = themeColors.textPrimary
                )
                if (item.cityName != null) {
                    Text(
                        text = item.cityName,
                        style = HavamaniaTheme.typography.cardTitle,
                        color = themeColors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(themeStyles.spacingXXL))

                Column(verticalArrangement = Arrangement.spacedBy(themeStyles.spacingMD)) {
                    item.messages.forEach { msg ->
                        ChatBubble(
                            message = msg,
                            c = themeColors,
                            onRetry = { _ -> },
                            onActionClick = null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                when (state) {
                    AiHistoryDetailState.Loading -> {
                        CircularProgressIndicator()
                        Text("Kayıt yükleniyor", color = themeColors.textPrimary)
                    }
                    AiHistoryDetailState.Failed -> {
                        Text("Kayıt yüklenemedi. Bağlantını kontrol edip tekrar deneyebilirsin.", color = themeColors.textPrimary)
                        TextButton(onClick = { viewModel.loadDetail(itemId) }) { Text("Tekrar dene") }
                    }
                    else -> Text("Kayıt bulunamadı", color = themeColors.textPrimary)
                }
            }
        }
    }
}
