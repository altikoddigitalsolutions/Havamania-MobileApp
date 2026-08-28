package com.havamania

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.LocalDate
import com.havamania.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    viewModel: WeatherViewModel = viewModel(),
    themeViewModel: com.havamania.ui.theme.ThemeViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel(),
    onNavigateToAi: (HavamaniaRecommendation, WeatherData?) -> Unit = { _, _ -> },
    onNavigateToNotifications: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val selectedHourlyWeather by viewModel.selectedHourlyWeather.collectAsStateWithLifecycle()
    val selectedForecastDate by viewModel.selectedForecastDate.collectAsStateWithLifecycle()
    val selectedDailyForecast by viewModel.selectedDailyForecast.collectAsStateWithLifecycle()
    val citySuggestions by viewModel.citySuggestions.collectAsStateWithLifecycle()
    val profileState by profileViewModel.profileState.collectAsStateWithLifecycle()
    val profile = (profileState as? ProfileState.Success)?.profile

    val userInterests by themeViewModel.userInterests.collectAsStateWithLifecycle()
    val todayRecommendation by viewModel.todayRecommendation.collectAsStateWithLifecycle()
    val userAboutMe by themeViewModel.userAboutMe.collectAsStateWithLifecycle()

    val notificationUiState by notificationViewModel.uiState.collectAsStateWithLifecycle()
    val unreadNotificationsCount = notificationUiState.unreadCount

    val themeStyles = HavamaniaTheme.styles
    val themeColors = HavamaniaTheme.colors

    LaunchedEffect(userInterests, userAboutMe, profile) {
        val personalization = if (profile != null) {
            PersonalizationProfile(
                uid = profile.uid,
                selectedInterests = profile.personalizationProfile?.selectedInterests ?: emptyList(),
                travelStyles = profile.personalizationProfile?.travelStyles ?: emptyList(),
                weatherPreferences = profile.personalizationProfile?.weatherPreferences ?: WeatherPreferences(),
                personalizationEnabled = profile.personalizationEnabled
            )
        } else null

        viewModel.updateRecommendation(userInterests, userAboutMe, personalization)
    }

    val scrollState = rememberScrollState()

    var showCitySwitcher by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(searchText) {
        if (searchText.length >= 2) {
            viewModel.searchCity(searchText)
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refreshWeather() }
    )

    HavamaniaScreen(
        modifier = Modifier.pullRefresh(pullRefreshState)
    ) { padding ->
        Crossfade(targetState = uiState, label = "state_transition", animationSpec = tween(1000)) { state ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (state) {
                    is WeatherUiState.Loading -> HomeScreenLoading()
                    is WeatherUiState.NoCity -> {
                        NoCitiesEmptyState(onAddClick = { showCitySwitcher = true })
                    }
                    is WeatherUiState.Success -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            WeatherSuccessContent(
                                data = state.data,
                                selectedHourlyWeather = selectedHourlyWeather,
                                selectedForecastDate = selectedForecastDate,
                                selectedDailyForecast = selectedDailyForecast,
                                recommendation = todayRecommendation,
                                userInterests = userInterests,
                                unreadCount = unreadNotificationsCount,
                                onSelectHour = { viewModel.selectHour(it) },
                                onSelectDaily = { viewModel.selectDailyForecast(it) },
                                scrollState = scrollState,
                                onAskAiClick = { rec -> onNavigateToAi(rec, state.data) },
                                onCityClick = { showCitySwitcher = true },
                                onNotificationsClick = onNavigateToNotifications
                            )
                        }
                    }
                    is WeatherUiState.Error -> {
                        val isOffline = !viewModel.isOnline.collectAsState().value
                        HavamaniaErrorState(
                            title = if (isOffline) "Bağlantı Yok" else "Hata",
                            description = if (isOffline) "İnternet bağlantını kontrol et." else state.message,
                            onRetry = { viewModel.refreshWeather() }
                        )
                    }
                }
            }
        }

        if (showCitySwitcher) {
            ModalBottomSheet(
                onDismissRequest = { showCitySwitcher = false },
                containerColor = themeColors.surface,
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                dragHandle = { BottomSheetDefaults.DragHandle(color = themeColors.textMuted.copy(0.2f)) }
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
                    Text(
                        "Şehir Seç",
                        style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Black),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    HavamaniaTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = "Şehir ara...",
                        leadingIcon = Icons.Rounded.Search
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(citySuggestions, key = { it.id }) { suggestion ->
                            val currentData = (uiState as? WeatherUiState.Success)?.data
                            val isSelected = currentData?.cityName == suggestion.getSafeCity() && currentData?.districtName == suggestion.getSafeDistrict()

                            Surface(
                                onClick = {
                                    viewModel.fetchWeather(suggestion.latitude, suggestion.longitude, suggestion.getSafeCity(), suggestion.getSafeDistrict())
                                    themeViewModel.setDefaultCity(suggestion)
                                    showCitySwitcher = false
                                    searchText = ""
                                },
                                color = if (isSelected) themeColors.accent.copy(alpha = 0.1f) else Color.Transparent,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            suggestion.getSafeDistrict() ?: suggestion.name,
                                            style = HavamaniaTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) themeColors.accent else themeColors.textPrimary
                                        )
                                        Text(
                                            suggestion.getSafeCity(),
                                            style = HavamaniaTheme.typography.bodySmall,
                                            color = themeColors.textMuted
                                        )
                                    }
                                    if (isSelected) Icon(Icons.Rounded.Check, contentDescription = "Seçili", tint = themeColors.accent)
                                }
                            }
                        }
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter).padding(padding),
            backgroundColor = themeColors.surface,
            contentColor = themeColors.accent,
            scale = true
        )
    }
}

@Composable
fun BoxScope.WeatherSuccessContent(
    data: WeatherData,
    selectedHourlyWeather: HourlyWeather?,
    selectedForecastDate: LocalDate,
    selectedDailyForecast: DailyForecast?,
    recommendation: HavamaniaRecommendation?,
    userInterests: Set<String> = emptySet(),
    unreadCount: Int = 0,
    onSelectHour: (HourlyWeather?) -> Unit,
    onSelectDaily: (DailyForecast) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState,
    onAskAiClick: (HavamaniaRecommendation) -> Unit,
    onCityClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    val themeStyles = HavamaniaTheme.styles
    val themeColors = HavamaniaTheme.colors
    val responsive = LocalResponsiveValues.current
    val windowSize = LocalWindowSize.current

    val displayTime by remember(selectedHourlyWeather) {
        derivedStateOf {
            selectedHourlyWeather?.time?.let {
                try {
                    val hourStr = if (it == "24:00") "0" else it.split(":")[0]
                    LocalTime.of(hourStr.toInt(), 0)
                } catch (e: Exception) { LocalTime.now() }
            } ?: LocalTime.now()
        }
    }

    val displayTemp by remember(data, selectedHourlyWeather, selectedDailyForecast) {
        derivedStateOf {
            selectedHourlyWeather?.temp ?: selectedDailyForecast?.let { "${it.minTemp}° / ${it.maxTemp}°" } ?: data.temperature
        }
    }

    val displayCondition by remember(data, selectedHourlyWeather, selectedDailyForecast) {
        derivedStateOf {
            val code = selectedHourlyWeather?.weatherCode ?: selectedDailyForecast?.weatherCode ?: data.weatherCode
            val sunrise = try { LocalTime.parse(data.sunriseTime) } catch (e: Exception) { LocalTime.of(6, 30) }
            val sunset = try { LocalTime.parse(data.sunsetTime) } catch (e: Exception) { LocalTime.of(19, 30) }
            val now = if (selectedHourlyWeather != null) {
                try { java.time.LocalDateTime.parse(selectedHourlyWeather.fullTime) } catch (e: Exception) { displayTime.atDate(LocalDate.now()) }
            } else { java.time.LocalDateTime.now() }
            WeatherUtils.getWeatherDisplayName(code, now, sunrise, sunset)
        }
    }

    val displayWeatherCode by remember(data, selectedHourlyWeather, selectedDailyForecast) {
        derivedStateOf { selectedHourlyWeather?.weatherCode ?: selectedDailyForecast?.weatherCode ?: data.weatherCode }
    }

    val displayIsDay by remember(data, selectedHourlyWeather) {
        derivedStateOf { selectedHourlyWeather?.isDay ?: true }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .then(
                if (windowSize.isTablet || windowSize.isLargeTablet) Modifier.widthIn(max = responsive.maxContentWidth)
                else Modifier.fillMaxWidth()
            )
    ) {
        Spacer(modifier = Modifier.height(themeStyles.spacingSM))

        // 1. WEATHER HERO
        EntranceAnimation(delayMillis = 50) {
            WeatherHeroCard(
                cityName = data.cityName,
                districtName = data.districtName,
                temperature = displayTemp,
                conditionLabel = displayCondition,
                weatherCode = displayWeatherCode,
                isDay = displayIsDay,
                high = selectedDailyForecast?.maxTemp?.toString()?.plus("°") ?: data.high,
                low = selectedDailyForecast?.minTemp?.toString()?.plus("°") ?: data.low,
                feelsLike = selectedHourlyWeather?.temp ?: data.feelsLike,
                humidity = data.details.find { it.title.contains("Nem") }?.value ?: "%65",
                windSpeed = data.details.find { it.title.contains("Rüzgar") }?.value ?: "12 km/sa",
                uvIndex = (data.uvIndex ?: 0).toString(),
                unreadCount = unreadCount,
                onCityClick = onCityClick,
                onNotificationsClick = onNotificationsClick,
                time = displayTime,
                latitude = data.latitude ?: 41.0082,
                longitude = data.longitude ?: 28.9784,
                sunriseTime = data.sunriseTime,
                sunsetTime = data.sunsetTime,
                parallaxOffset = scrollState.value * 0.15f,
                modifier = Modifier.padding(horizontal = themeStyles.pagePadding)
            )
        }

        Spacer(modifier = Modifier.height(themeStyles.spacingLG))

        // 2. DAILY INSIGHT
        EntranceAnimation(delayMillis = 150) {
            DailyInsightCard(data, themeColors, modifier = Modifier.padding(horizontal = themeStyles.pagePadding))
        }

        Spacer(modifier = Modifier.height(themeStyles.spacingMD))

        // 3. HOURLY FORECAST
        EntranceAnimation(delayMillis = 250) {
            val dateStr = selectedForecastDate.toString()
            val today = LocalDate.now()
            val currentHour = LocalTime.now().hour
            val filteredHourly = remember(data.hourlyForecast, dateStr) {
                data.hourlyForecast.filter { hour ->
                    val isSelectedDay = hour.fullTime.startsWith(dateStr)
                    if (isSelectedDay && selectedForecastDate == today) {
                        try { val h = hour.time.split(":")[0].toInt(); h >= currentHour } catch (e: Exception) { true }
                    } else isSelectedDay
                }
            }

            if (filteredHourly.isNotEmpty()) {
                val hourlyWithSelection = remember(filteredHourly, selectedHourlyWeather) {
                    filteredHourly.map { it.copy(isSelected = it.fullTime == selectedHourlyWeather?.fullTime) }
                }
                HourlyForecastRow(
                    items = hourlyWithSelection,
                    sunriseTime = data.sunriseTime,
                    sunsetTime = data.sunsetTime,
                    onItemSelect = { index -> onSelectHour(filteredHourly[index]) }
                )
            }
        }

        Spacer(modifier = Modifier.height(themeStyles.spacingMD))

        // 4. RECOMMENDATION / AI
        EntranceAnimation(delayMillis = 350) {
            if (recommendation != null) {
                RecommendationCard(
                    recommendation = recommendation,
                    onAskAiClick = { onAskAiClick(recommendation) },
                    modifier = Modifier.padding(horizontal = themeStyles.pagePadding)
                )
            }
        }

        Spacer(modifier = Modifier.height(themeStyles.spacingLG))

        // 5. DAILY FORECAST
        EntranceAnimation(delayMillis = 450) {
            val today = LocalDate.now()
            val futureDaily = remember(data.dailyForecast) {
                data.dailyForecast.filter { try { !LocalDate.parse(it.date).isBefore(today) } catch (e: Exception) { true } }
            }
            DailyForecastSection(
                forecasts = futureDaily,
                selectedDate = selectedForecastDate.toString(),
                onDayClick = onSelectDaily
            )
        }

        Spacer(modifier = Modifier.height(themeStyles.spacingLG))

        // 6. WEATHER DETAILS
        EntranceAnimation(delayMillis = 550) {
            Column {
                SectionLabel("HAVA DETAYLARI", Modifier.padding(horizontal = themeStyles.pagePadding + 8.dp))
                WeatherDetailsPanel(data = data, modifier = Modifier.padding(horizontal = themeStyles.spacingMD))
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun DailyInsightCard(data: WeatherData, colors: HavamaniaColors, modifier: Modifier = Modifier) {
    val insight = remember(data) { RecommendationEngine.getShortWeatherSummary(data) }
    HavamaniaCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = colors.accent.copy(alpha = 0.05f),
        borderColor = colors.accent.copy(alpha = 0.1f),
        elevation = 0.dp,
        padding = 16.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = colors.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = insight,
                style = HavamaniaTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = colors.textPrimary
            )
        }
    }
}

@Composable
fun EntranceAnimation(delayMillis: Int = 0, content: @Composable AnimatedVisibilityScope.() -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delayMillis.toLong()); visible = true }
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(600)), content = content)
}
