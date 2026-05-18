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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.LocalDate
import com.havamania.ui.theme.SectionLabel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    viewModel: WeatherViewModel = viewModel(),
    themeViewModel: com.havamania.ui.theme.ThemeViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel(),
    onNavigateToAi: (HavamaniaRecommendation, WeatherData?) -> Unit = { _, _ -> },
    onNavigateToNotifications: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedHourlyWeather by viewModel.selectedHourlyWeather.collectAsState()
    val selectedForecastDate by viewModel.selectedForecastDate.collectAsState()
    val selectedDailyForecast by viewModel.selectedDailyForecast.collectAsState()
    val citySuggestions by viewModel.citySuggestions.collectAsState()
    val userInterests by themeViewModel.userInterests.collectAsState()
    val todayRecommendation by viewModel.todayRecommendation.collectAsState()

    val notificationUiState by notificationViewModel.uiState.collectAsState()
    val unreadNotificationsCount = notificationUiState.unreadCount

    LaunchedEffect(userInterests) {
        viewModel.updateRecommendation(userInterests)
    }

    val scrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme
    val themeColors = com.havamania.ui.theme.HavamaniaTheme.colors

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
        Box(modifier = Modifier.fillMaxSize().zIndex(0f).background(themeColors.background))

        val noisePoints = remember { List(1000) { Offset(kotlin.random.Random.nextFloat(), kotlin.random.Random.nextFloat()) } }
        Box(modifier = Modifier.fillMaxSize().zIndex(0.5f).alpha(0.04f)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                noisePoints.forEach { point ->
                    drawCircle(color = Color.White.copy(alpha = 0.3f), radius = 1f, center = Offset(point.x * size.width, point.y * size.height))
                }
            }
        }

        Crossfade(targetState = uiState, label = "state_transition", animationSpec = tween(1000)) { state ->
            when (state) {
                is WeatherUiState.Loading -> HomeScreenLoading()
                is WeatherUiState.Success -> {
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
                is WeatherUiState.Error -> {
                    WeatherErrorState(title = "Hata Oluştu", description = state.message, onRetry = { viewModel.refreshWeather() })
                }
            }
        }

        if (showCitySwitcher) {
            ModalBottomSheet(
                onDismissRequest = { showCitySwitcher = false },
                containerColor = colorScheme.surface,
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 40.dp)) {
                    Text("Şehir Seç", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                    OutlinedTextField(value = searchText, onValueChange = { searchText = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Şehir ara (örn: bal)") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, shape = RoundedCornerShape(16.dp), singleLine = true)
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(citySuggestions, key = { it.id }) { suggestion ->
                            val currentData = (uiState as? WeatherUiState.Success)?.data
                            val isSelected = currentData?.cityName == suggestion.getSafeCity() && currentData?.districtName == suggestion.getSafeDistrict()
                            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isSelected) colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent).clickable { viewModel.fetchWeather(suggestion.latitude, suggestion.longitude, suggestion.getSafeCity(), suggestion.getSafeDistrict()); themeViewModel.setDefaultCity(suggestion); showCitySwitcher = false; searchText = "" }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column { Text(suggestion.getSafeDistrict() ?: suggestion.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold); Text(suggestion.getSafeCity(), style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurface.copy(alpha = 0.6f)) }
                                if (isSelected) Icon(Icons.Rounded.Check, null, tint = colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        PullRefreshIndicator(refreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter), backgroundColor = colorScheme.surface, contentColor = colorScheme.primary, scale = true)
    }
}

@Composable
fun WeatherSuccessContent(
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
    val displayTemp by remember(data, selectedHourlyWeather, selectedDailyForecast) { derivedStateOf { selectedHourlyWeather?.temp ?: selectedDailyForecast?.let { "${it.minTemp}° / ${it.maxTemp}°" } ?: data.temperature } }
    val displayCondition by remember(data, selectedHourlyWeather, selectedDailyForecast) { derivedStateOf { val code = selectedHourlyWeather?.weatherCode ?: selectedDailyForecast?.weatherCode ?: data.weatherCode; val hour = selectedHourlyWeather?.time?.let { try { if (it == "24:00") 0 else it.split(":")[0].toInt() } catch (e: Exception) { LocalTime.now().hour } } ?: LocalTime.now().hour; WeatherMapper.getDisplayCondition(code, hour) } }
    val displayWeatherCode by remember(data, selectedHourlyWeather, selectedDailyForecast) { derivedStateOf { selectedHourlyWeather?.weatherCode ?: selectedDailyForecast?.weatherCode ?: data.weatherCode } }
    val displayIsDay by remember(data, selectedHourlyWeather) { derivedStateOf { selectedHourlyWeather?.isDay ?: true } }
    val displayTime by remember(selectedHourlyWeather) { derivedStateOf { selectedHourlyWeather?.time?.let { try { val hourStr = if (it == "24:00") "0" else it.split(":")[0]; LocalTime.of(hourStr.toInt(), 0) } catch (e: Exception) { LocalTime.now() } } ?: LocalTime.now() } }
    val density = LocalDensity.current.density

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(scrollState)) {
        Spacer(modifier = Modifier.height(20.dp))
        EntranceAnimation(delayMillis = 50) {
            WeatherHeroCard(
                cityName = data.cityName, districtName = data.districtName, temperature = displayTemp, conditionLabel = displayCondition, weatherCode = displayWeatherCode, isDay = displayIsDay, high = selectedDailyForecast?.maxTemp?.toString()?.plus("°") ?: data.high, low = selectedDailyForecast?.minTemp?.toString()?.plus("°") ?: data.low, feelsLike = selectedHourlyWeather?.temp ?: data.feelsLike, humidity = data.details.find { it.title.contains("Nem") }?.value ?: "%65", windSpeed = data.details.find { it.title.contains("Rüzgar") }?.value ?: "12 km/s", uvIndex = data.details.find { it.title.contains("UV") }?.value?.filter { it.isDigit() } ?: "4", unreadCount = unreadCount, onCityClick = onCityClick, onNotificationsClick = onNotificationsClick, time = displayTime, parallaxOffset = scrollState.value * 0.12f,
                modifier = Modifier.padding(horizontal = 16.dp).zIndex(1f).graphicsLayer { this.cameraDistance = 12f * density }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxWidth().zIndex(2f)) {
            Column {
                EntranceAnimation(delayMillis = 150) {
                    val dateStr = selectedForecastDate.toString(); val today = LocalDate.now(); val currentHour = LocalTime.now().hour
                    val filteredHourly = remember(data.hourlyForecast, dateStr) { data.hourlyForecast.filter { hour -> val isSelectedDay = hour.fullTime.startsWith(dateStr); if (isSelectedDay && selectedForecastDate == today) { try { val h = hour.time.split(":")[0].toInt(); h >= currentHour } catch (e: Exception) { true } } else isSelectedDay } }
                    if (filteredHourly.isNotEmpty()) {
                        val hourlyWithSelection = remember(filteredHourly, selectedHourlyWeather) { filteredHourly.map { it.copy(isSelected = it.fullTime == selectedHourlyWeather?.fullTime) } }
                        HourlyForecastRow(items = hourlyWithSelection, onItemSelect = { index -> onSelectHour(filteredHourly[index]) })
                    } else {
                        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) { SectionLabel("SAATLİK TAHMİN", Modifier); Text("Bu gün için saatlik tahmin henüz mevcut değil.", color = com.havamania.ui.theme.HavamaniaTheme.colors.textPrimary.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp)) }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                EntranceAnimation(delayMillis = 250) {
                    if (recommendation != null) RecommendationCard(recommendation = recommendation, onAskAiClick = { onAskAiClick(recommendation.copy(message = "Bugünkü hava durumunu detaylı analiz eder misin? Sıcaklık, yağış, rüzgar, UV ve gün içindeki değişime göre öneri ver.")) }, modifier = Modifier.padding(horizontal = 16.dp))
                }
                Spacer(modifier = Modifier.height(20.dp))
                EntranceAnimation(delayMillis = 350) {
                    val today = LocalDate.now(); val futureDaily = remember(data.dailyForecast) { data.dailyForecast.filter { try { !LocalDate.parse(it.date).isBefore(today) } catch (e: Exception) { true } } }
                    DailyForecastSection(forecasts = futureDaily, selectedDate = selectedForecastDate.toString(), onDayClick = { forecast -> onSelectDaily(forecast) })
                }
                Spacer(modifier = Modifier.height(24.dp))
                EntranceAnimation(delayMillis = 450) {
                    Column { SectionLabel("HAVA DETAYLARI", Modifier.padding(horizontal = 24.dp)); WeatherDetailsPanel(data = data, modifier = Modifier.padding(horizontal = 16.dp)) }
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun EntranceAnimation(delayMillis: Int = 0, content: @Composable AnimatedVisibilityScope.() -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delayMillis.toLong()); visible = true }
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(600)), content = content)
}
