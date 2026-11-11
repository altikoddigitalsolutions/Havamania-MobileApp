package com.havamania

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.AppTheme
import com.havamania.ui.theme.LocalHavamaniaColors
import com.havamania.ui.theme.LocalHavamaniaTypography
import com.havamania.ui.theme.ThemeFactory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TravelGuideExperienceDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun largeTextCanReadAndOpenSourceInLightTheme() = verifyCard(AppTheme.LIGHT)

    @Test fun largeTextCanReadAndOpenSourceInDarkTheme() = verifyCard(AppTheme.DARK)

    private fun verifyCard(theme: AppTheme) {
        val experience = LocalTravelGuides.experiences("İstanbul", null, TripType.PHOTOGRAPHY).first()
        var opened: String? = null
        compose.setContent {
            val colors = ThemeFactory.createColors(theme)
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalDensity provides Density(density, 2f),
                LocalUriHandler provides object : UriHandler {
                    override fun openUri(uri: String) { opened = uri }
                }
            ) {
                MaterialTheme {
                    Box(Modifier.size(320.dp, 360.dp)) {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            TravelGuideExperienceCard(experience)
                        }
                    }
                }
            }
        }
        compose.onNodeWithText(experience.title).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(experience.description).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Kaynak ve ayrıntılar").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(experience.sourceUrl, opened) }
    }
}
