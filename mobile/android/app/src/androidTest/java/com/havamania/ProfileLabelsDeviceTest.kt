package com.havamania

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.*
import org.junit.Rule
import org.junit.Test

class ProfileLabelsDeviceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun negativeBiographyRemovesBothStalePersonalityAndBadge() {
        var biography by mutableStateOf("Yağmuru izlemeyi seviyorum.")
        compose.setContent { ProfileFixture(biography, emptySet()) }
        compose.onNodeWithText("YAĞMUR MERAKLISI").assertExists()
        compose.onNodeWithText("Yağmur meraklısı").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { biography = "Yağmurdan nefret ediyorum." }
        compose.onNodeWithText("HAVA MERAKLISI").assertExists()
        compose.onNodeWithText("YAĞMUR MERAKLISI").assertDoesNotExist()
        compose.onNodeWithText("Yağmur meraklısı").assertDoesNotExist()
    }
    @Test fun explicitPreferenceUpdatesBothViewsWithoutInferringSnowFromAnkara() {
        var interests by mutableStateOf(setOf("motorsiklet"))
        compose.setContent { ProfileFixture("Ankara'da yaşıyorum.", interests) }
        compose.onNodeWithText("YOL SAVAŞÇISI").assertExists()
        compose.onNodeWithText("Yol meraklısı").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Kış meraklısı").assertDoesNotExist()
        compose.runOnIdle { interests = emptySet() }
        compose.onNodeWithText("HAVA MERAKLISI").assertExists()
        compose.onNodeWithText("Yol meraklısı").assertDoesNotExist()
    }
    @Composable private fun ProfileFixture(biography: String, interests: Set<String>) {
        val colors = ThemeFactory.createColors(AppTheme.LIGHT)
        CompositionLocalProvider(LocalHavamaniaColors provides colors,
            LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
            ProvideResponsiveLayout { MaterialTheme {
                Column(Modifier.width(320.dp).height(650.dp).background(colors.background).verticalScroll(rememberScrollState())) {
                    PremiumProfileHeader("Yerel test", "", null, interests = interests, aboutMe = biography,
                        stats = mapOf("İlgi" to interests.size.toString()), onAvatarClick = {}, onStatClick = {})
                    PremiumAboutMeCard(biography, interests = interests) {}
                }
            } }
        }
    }
}
