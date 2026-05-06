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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import java.util.Locale
import java.time.LocalTime
import com.havamania.HomeScreenLoading
import com.havamania.WeatherErrorState
import com.havamania.ui.theme.SectionLabel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    viewModel: WeatherViewModel = viewModel(),
    themeViewModel: com.havamania.ui.theme.ThemeViewModel = viewModel(),
    travelViewModel: TravelViewModel = viewModel(),
    onNavigateToAi: (HavamaniaRecommendation) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedHour by viewModel.selectedHour.collectAsState()
    val citySuggestions by viewModel.citySuggestions.collectAsState()
    val userInterests by themeViewModel.userInterests.collectAsState()
    val travelPlans by travelViewModel.plans.collectAsState()

    val scrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme
    val themeColors = com.havamania.ui.theme.HavamaniaTheme.colors

    val currentData = (uiState as? WeatherUiState.Success)?.data
    val condition = remember(currentData, selectedHour) {
        val code = selectedHour?.weatherCode ?: currentData?.weatherCode ?: 0
        val isDay = selectedHour?.isDay ?: currentData?.isDay ?: true
        WeatherMapper.mapWeatherCodeToCondition(code, isDay)
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        // Layer 1: Atmospheric Glows & Condition Effects
        Box(modifier = Modifier.fillMaxSize().zIndex(0f)) {
            if (condition is WeatherCondition.Clear) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(themeColors.glow.copy(alpha = 0.4f), Color.Transparent),
                            center = Offset(size.width * 0.8f, size.height * 0.2f),
                            radius = size.maxDimension * 0.9f
                        )
                    )
                }
            }
        }

        // Global Noise/Grain Overlay
        val noisePoints = remember {
            List(1000) {
                Offset(kotlin.random.Random.nextFloat(), kotlin.random.Random.nextFloat())
            }
        }
        Box(modifier = Modifier.fillMaxSize().zIndex(10f).alpha(0.04f)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                noisePoints.forEach { point ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.3f),
                        radius = 1f,
                        center = Offset(point.x * size.width, point.y * size.height)
                    )
                }
            }
        }

        Crossfade(targetState = uiState, label = "state_transition", animationSpec = tween(1000)) { state ->
            when (state) {
                is WeatherUiState.Loading -> HomeScreenLoading()
                is WeatherUiState.Success -> {
                    WeatherSuccessContent(
                        data = state.data,
                        selectedHour = selectedHour,
                        userInterests = userInterests,
                        travelPlans = travelPlans,
                        onSelectHour = { viewModel.selectHour(it) },
                        scrollState = scrollState,
                        onAskAiClick = onNavigateToAi,
                        onCityClick = { showCitySwitcher = true }
                    )
                }
                is WeatherUiState.Error -> {
                    WeatherErrorState(
                        title = "Hata Oluştu",
                        description = state.message,
                        onRetry = { viewModel.refreshWeather() }
                    )
                }
            }
        }
// ... (ModalBottomSheet ve PullRefreshIndicator aynı kalacak)

        // GERÇEK ŞEHİR SEÇME MODALI
        if (showCitySwitcher) {
            ModalBottomSheet(
                onDismissRequest = { showCitySwitcher = false },
                containerColor = colorScheme.surface,
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 40.dp)
                ) {
                    Text(
                        "Şehir Seç",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Arama Girişi
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Şehir ara (örn: bal)") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Şehir Listesi
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(citySuggestions, key = { it.id }) { suggestion ->
                            val currentCity = (uiState as? WeatherUiState.Success)?.data?.cityName ?: ""
                            val isSelected = currentCity == suggestion.name

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable {
                                        viewModel.fetchWeather(
                                            suggestion.latitude,
                                            suggestion.longitude,
                                            suggestion.city,
                                            suggestion.district
                                        )
                                        showCitySwitcher = false
                                        searchText = ""
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(suggestion.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                    if (suggestion.admin1 != null && suggestion.admin1 != suggestion.name) {
                                        Text(suggestion.admin1!!, style = MaterialTheme.typography.bodySmall, color = themeColors.textSecondary)
                                    }
                                }
                                if (isSelected) {
                                    Icon(Icons.Rounded.Check, null, tint = colorScheme.primary)
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
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = colorScheme.surface,
            contentColor = colorScheme.primary,
            scale = true
        )
    }
}

// WeatherSuccessContent ve diğer yardımcı bileşenler aynı kalacak şekilde...
@Composable
fun WeatherSuccessContent(
    data: WeatherData,
    selectedHour: HourlyForecastData?,
    userInterests: Set<String> = emptySet(),
    travelPlans: List<TravelPlan> = emptyList(),
    onSelectHour: (HourlyForecastData?) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState,
    onAskAiClick: (HavamaniaRecommendation) -> Unit = {},
    onCityClick: () -> Unit = {}
) {
    // Derived state for premium performance and correct logic
    val displayTemp by remember(data, selectedHour) {
        derivedStateOf<String> { selectedHour?.temp ?: data.temperature }
    }

    val displayCondition by remember(data, selectedHour) {
        derivedStateOf<String> { selectedHour?.condition ?: data.condition }
    }

    val displayWeatherCode by remember(data, selectedHour) {
        derivedStateOf<Int> { selectedHour?.weatherCode ?: data.weatherCode }
    }

    val displayIsDay by remember(data, selectedHour) {
        derivedStateOf<Boolean> { selectedHour?.isDay ?: data.isDay }
    }

    val displayTime by remember(selectedHour) {
        derivedStateOf<LocalTime> {
            selectedHour?.time?.let {
                try {
                    val hour = it.split(":")[0].toInt()
                    LocalTime.of(hour, 0)
                } catch (e: Exception) {
                    LocalTime.now()
                }
            } ?: LocalTime.now()
        }
    }

    val density = LocalDensity.current.density

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Layer 2: Weather Hero Card (Mid-ground)
        EntranceAnimation(delayMillis = 50) {
            WeatherHeroCard(
                cityName = data.cityName,
                districtName = data.districtName,
                temperature = displayTemp,
                conditionLabel = displayCondition,
                weatherCode = displayWeatherCode,
                isDay = displayIsDay,
                high = data.high,
                low = data.low,
                feelsLike = data.feelsLike,
                humidity = data.details.find { it.title.contains("Nem") }?.value ?: "%65",
                windSpeed = data.details.find { it.title.contains("Rüzgar") }?.value ?: "12 km/s",
                uvIndex = data.details.find { it.title.contains("UV") }?.value?.filter { it.isDigit() } ?: "4",
                onCityClick = onCityClick,
                time = displayTime,
                parallaxOffset = scrollState.value * 0.12f,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .zIndex(1f)
                    .graphicsLayer {
                        this.cameraDistance = 12f * density
                    }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Layer 3: Foreground Content (Cards & Details)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(2f)
        ) {
            Column {
                EntranceAnimation(delayMillis = 150) {
                    val hourlyWithSelection = remember(data.hourlyForecast, selectedHour) {
                        data.hourlyForecast.map {
                            it.copy(isSelected = it.time == (selectedHour?.time ?: data.hourlyForecast.firstOrNull()?.time))
                        }
                    }

                    HourlyForecastRow(
                        items = hourlyWithSelection,
                        onItemSelect = { index ->
                            val selected = data.hourlyForecast[index]
                            if (selectedHour?.time != selected.time) {
                                onSelectHour(selected)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                EntranceAnimation(delayMillis = 250) {
                    AiSuggestionCard(
                        weather = data,
                        timeOfDay = displayTime.hour.let { WeatherMapper.resolveTimeOfDay(it) },
                        userInterests = userInterests,
                        travelPlans = travelPlans,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onAskAiClick = onAskAiClick
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                EntranceAnimation(delayMillis = 350) {
                    DailyForecastSection(
                        forecasts = data.dailyForecast,
                        onDayClick = { forecast ->
                            onAskAiClick(HavamaniaRecommendation(
                                message = "${forecast.day} günü için detaylı hava analizi ve aktivite önerisi yapabilir misin?",
                                type = RecommendationType.GENERAL,
                                highlightedWords = listOf(forecast.day, "analizi"),
                                priority = RecommendationPriority.MEDIUM
                            ))
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                EntranceAnimation(delayMillis = 450) {
                    Column {
                        SectionLabel("HAVA DETAYLARI", Modifier.padding(horizontal = 24.dp))
                        WeatherDetailsPanel(
                            data = data,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun EntranceAnimation(delayMillis: Int = 0, content: @Composable AnimatedVisibilityScope.() -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(600)),
        content = content
    )
}
