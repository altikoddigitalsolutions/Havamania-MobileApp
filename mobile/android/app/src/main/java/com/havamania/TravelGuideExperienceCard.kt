package com.havamania

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.HavamaniaTheme

@Composable
internal fun TravelGuideExperienceCard(experience: TravelGuideExperience) {
    val colors = HavamaniaTheme.colors
    val typography = HavamaniaTheme.typography
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(experience.category, style = typography.caption, color = colors.accent)
            Spacer(Modifier.height(6.dp))
            Text(experience.title, style = typography.cardTitle, color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            Text(experience.description, style = typography.bodySmall, color = colors.textPrimary)
            TextButton(onClick = {
                runCatching { uriHandler.openUri(experience.sourceUrl) }.onFailure {
                    Toast.makeText(context, "Kaynak bağlantısı açılamadı.", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Kaynak ve ayrıntılar", color = colors.accent)
            }
        }
    }
}
