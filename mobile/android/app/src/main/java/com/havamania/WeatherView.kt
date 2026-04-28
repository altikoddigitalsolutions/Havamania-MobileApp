package com.havamania

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.util.Locale

// Şehir Listesi Data
val cities = listOf(
    "Adana", "Adıyaman", "Afyonkarahisar", "Ağrı", "Amasya", "Ankara", "Antalya", "Artvin",
    "Aydın", "Balıkesir", "Bilecik", "Bingöl", "Bitlis", "Bolu", "Burdur", "Bursa",
    "Çanakkale", "Çankırı", "Çorum", "Denizli", "Diyarbakır", "Edirne", "Elazığ", "Erzincan",
    "Erzurum", "Eskişehir", "Gaziantep", "Giresun", "Gümüşhane", "Hakkari", "Hatay", "Isparta",
    "Mersin", "İstanbul", "İzmir", "Kars", "Kastamonu", "Kayseri", "Kırklareli", "Kırşehir",
    "Kocaeli", "Konya", "Kütahya", "Malatya", "Manisa", "Kahramanmaraş", "Mardin", "Muğla",
    "Muş", "Nevşehir", "Niğde", "Ordu", "Rize", "Sakarya", "Samsun", "Siirt",
    "Sinop", "Sivas", "Tekirdağ", "Tokat", "Trabzon", "Tunceli", "Şanlıurfa", "Uşak",
    "Van", "Yozgat", "Zonguldak", "Aksaray", "Bayburt", "Karaman", "Kırıkkale", "Batman",
    "Şırnak", "Bartın", "Ardahan", "Iğdır", "Yalova", "Karabük", "Kilis", "Osmaniye", "Düzce"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    viewModel: WeatherViewModel = viewModel(),
    onNavigateToAi: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val scrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme

    var showCitySwitcher by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    // Arama Filtreleme (Türkçe duyarlı)
    val filteredCities = remember(searchText) {
        if (searchText.isEmpty()) emptyList()
        else {
            val normalizedSearch = searchText.lowercase(Locale("tr"))
            cities.filter { it.lowercase(Locale("tr")).contains(normalizedSearch) }
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refreshWeather() }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .pullRefresh(pullRefreshState)
    ) {
        Crossfade(targetState = uiState, label = "state_transition") { state ->
            when (state) {
                is WeatherUiState.Loading -> HomeScreenLoading()
                is WeatherUiState.Success -> {
                    WeatherSuccessContent(
                        data = state.data,
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
                        items(filteredCities) { cityName ->
                            val currentCity = (uiState as? WeatherUiState.Success)?.data?.cityName ?: ""
                            val isSelected = currentCity.contains(cityName, ignoreCase = true)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable {
                                        viewModel.fetchWeather(41.0, 28.0, cityName) // Örnek koordinat, API şehirden bulacaktır
                                        showCitySwitcher = false
                                        searchText = ""
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cityName, style = MaterialTheme.typography.bodyLarge)
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
    scrollState: androidx.compose.foundation.ScrollState,
    onAskAiClick: () -> Unit = {},
    onCityClick: () -> Unit = {}
) {
    var selectedTemp by remember(data) { mutableStateOf(data.temperature) }
    var selectedTimeLabel by remember(data) { mutableStateOf("Şimdi") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // 1. Hero Card
        EntranceAnimation(delayMillis = 100) {
            WeatherHeroCard(
                cityName = data.cityName,
                temperature = selectedTemp,
                condition = if (selectedTimeLabel == "Şimdi") data.condition else "Tahmini Hava",
                high = data.high,
                low = data.low,
                feelsLike = data.feelsLike,
                onCityClick = onCityClick,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // ... (Diğer bölümler aynı kalacak)
        Spacer(modifier = Modifier.height(16.dp))
        EntranceAnimation(delayMillis = 200) {
            val hourlyWithSelection = data.hourlyForecast.map {
                it.copy(isSelected = it.time == selectedTimeLabel)
            }
            HourlyForecastSection(
                items = hourlyWithSelection,
                onItemClick = { index ->
                    val selected = data.hourlyForecast[index]
                    selectedTemp = selected.temp
                    selectedTimeLabel = selected.time
                }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        AiSuggestionCard(
            condition = data.condition,
            modifier = Modifier.padding(horizontal = 16.dp),
            onAskAiClick = onAskAiClick
        )
        Spacer(modifier = Modifier.height(16.dp))
        DailyForecastSection(forecasts = data.dailyForecast)
        Spacer(modifier = Modifier.height(16.dp))
        Column {
            SectionLabel("HAVA DETAYLARI", Modifier.padding(horizontal = 20.dp))
            WeatherDetailsGrid(
                details = data.details,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
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
