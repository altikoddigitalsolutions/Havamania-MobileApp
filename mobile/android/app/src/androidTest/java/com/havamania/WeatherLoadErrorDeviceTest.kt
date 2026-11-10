package com.havamania

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.havamania.ui.theme.HavamaniaTheme
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WeatherLoadErrorDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun localCitySettingsCanBeRetriedWhileOffline() {
        var retries = 0
        compose.setContent {
            HavamaniaTheme {
                WeatherLoadError(
                    message = "Şehir ayarları okunamadı.",
                    isOnline = false,
                    isRefreshing = false,
                    retryNeedsInternet = false,
                    onRetry = { retries++ }
                )
            }
        }
        compose.onNodeWithText("Şehir ayarları yüklenemedi").assertIsDisplayed()
        compose.onNodeWithText("Bağlantı yok").assertDoesNotExist()
        compose.onNodeWithText("Tekrar dene").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun forecastShowsCityLocalCacheTimeAndClearsWarningAfterFreshData() {
        val online = mutableStateOf(false)
        val weather = mutableStateOf(WeatherData(
            cityName = "İstanbul",
            timezone = "Europe/Istanbul",
            timestamp = Instant.parse("2026-11-06T22:05:00Z").toEpochMilli()
        ))
        compose.setContent {
            HavamaniaTheme {
                Box {
                    WeatherSuccessContent(
                        data = weather.value,
                        selectedHourlyWeather = null,
                        selectedForecastDate = LocalDate.of(2026, 11, 7),
                        selectedDailyForecast = null,
                        recommendation = null,
                        onSelectHour = {},
                        onSelectDaily = {},
                        scrollState = rememberScrollState(),
                        onAskAiClick = {},
                        isOnline = online.value
                    )
                }
            }
        }

        val offline = compose.onNodeWithText("Çevrimdışı hava verisi", substring = true)
        val cached = compose.onNodeWithText("Önbellekteki hava verisi", substring = true)
        offline.assertIsDisplayed()
        compose.onNodeWithText("7 Kas 2026, 01:05", substring = true).assertIsDisplayed()

        compose.runOnIdle {
            online.value = true
            weather.value = weather.value.copy(isStale = true)
        }
        offline.assertDoesNotExist()
        cached.assertIsDisplayed()

        compose.runOnIdle {
            weather.value = weather.value.copy(isStale = false, timestamp = weather.value.timestamp + 60_000)
        }
        cached.assertDoesNotExist()
    }

    @Test fun offlineErrorCanOpenPickerAndSelectSavedCity() {
        val pickerOpen = mutableStateOf(false)
        val savedCity = GeocodingResultDto(id = 7, name = "Bursa", latitude = 40.2, longitude = 29.1)
        var selected: GeocodingResultDto? = null
        compose.setContent {
            HavamaniaTheme {
                if (pickerOpen.value) {
                    WeatherCitySearchContent(
                        query = "",
                        onQueryChange = {},
                        suggestions = emptyList(),
                        status = LocationSearchStatus.IDLE,
                        onRetry = {},
                        savedCities = listOf(savedCity),
                        onSelect = { selected = it }
                    )
                } else {
                    WeatherLoadError(
                        message = "Hava durumu alınamadı.",
                        isOnline = false,
                        isRefreshing = false,
                        onChooseCity = { pickerOpen.value = true },
                        onRetry = {}
                    )
                }
            }
        }

        compose.onNodeWithText("Şehir seç").assertIsEnabled().performClick()
        compose.onNodeWithText("Kayıtlı şehirler").assertExists()
        compose.onNodeWithText("Bursa").performClick()
        compose.runOnIdle { assertEquals(savedCity, selected) }
    }

    @Test fun retryEnablesAfterReconnectAndDisablesWhileRequestRuns() {
        val online = mutableStateOf(false)
        val refreshing = mutableStateOf(false)
        var requests = 0
        compose.setContent {
            HavamaniaTheme {
                WeatherLoadError("Hava durumu alınamadı.", online.value, refreshing.value) {
                    requests++
                    refreshing.value = true
                }
            }
        }

        val retry = compose.onNodeWithText("Tekrar dene")
        retry.assertIsNotEnabled()
        compose.runOnIdle { online.value = true }
        retry.assertIsEnabled().performClick()
        compose.onNodeWithContentDescription("Tekrar dene").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(1, requests)
            refreshing.value = false
        }
        retry.assertIsEnabled()
    }
}
