package com.havamania

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

// --- VIEWMODEL ---
class AiChatViewModel(application: Application) : AndroidViewModel(application) {
    private val api = AltikodChatFactory.create()
    private val botId = "6"
    private var sessionId = UUID.randomUUID().toString()

    private val repository = WeatherRepository.getInstance(application)
    private val database = WeatherDatabase.getDatabase(application)
    private val dao = database.weatherDao()

    private val _messages = MutableStateFlow<List<AltikodChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _config = MutableStateFlow<AltikodBotConfig?>(null)
    val config = _config.asStateFlow()

    private val _weatherData = MutableStateFlow<WeatherData?>(null)
    val weatherData = _weatherData.asStateFlow()

    private val _activeTravels = MutableStateFlow<List<TravelPlan>>(emptyList())
    private var contextCity: String? = null

    var userAboutMe: String = ""
    var userInterests: Set<String> = emptySet()
    var assistantTone: AssistantTone = AssistantTone.DENGELI
    var language: String = "TR"

    init {
        loadConfig()
        observeWeatherState()
        loadActiveTravels()
    }

    private fun loadActiveTravels() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val entities = dao.getAllTravelPlans()
                val today = LocalDate.now()
                val active = entities.map { it.toDomain() }.filter {
                    !it.isArchived && !it.endDate.isBefore(today)
                }
                _activeTravels.value = active
            } catch (e: Exception) {
                android.util.Log.e("HAVAMANIA_AI", "Error loading travels", e)
            }
        }
    }

    private fun TravelPlanEntity.toDomain() = TravelPlan(
        id = id,
        city = city,
        latitude = latitude,
        longitude = longitude,
        tripType = try { TripType.valueOf(tripType) } catch (e: Exception) { TripType.OTHER },
        startDate = java.time.Instant.ofEpochMilli(startDate).atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
        endDate = java.time.Instant.ofEpochMilli(endDate).atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
        createdAt = createdAt,
        weatherSummary = weatherSummary,
        aiSuggestion = aiSuggestion,
        userNote = userNote,
        userRating = userRating,
        lastWeatherAnalysisText = lastWeatherAnalysisText,
        lastWeatherAnalysisDate = lastWeatherAnalysisDate,
        lastForecastSnapshot = lastForecastSnapshot,
        previousForecastSnapshot = previousForecastSnapshot,
        nextAnalysisEligibleDate = nextAnalysisEligibleDate,
        weatherAnalysisStatus = try { TravelWeatherAnalysisStatus.valueOf(weatherAnalysisStatus) } catch (e: Exception) { TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW },
        isArchived = isArchived,
        analyses = analyses,
        lastDailyNotificationDate = lastDailyNotificationDate
    )

    private fun observeWeatherState() {
        viewModelScope.launch {
            repository.currentWeatherState.collect { data ->
                _weatherData.value = data
                if (data != null) {
                    android.util.Log.d("HAVAMANIA_AI", "Shared weather data received: ${data.cityName}")
                    logWeatherState(data)
                } else {
                    android.util.Log.w("HAVAMANIA_AI", "Shared weather data is NULL")
                    tryAutoFetch()
                }
            }
        }
    }

    private fun tryAutoFetch() {
        viewModelScope.launch {
            try {
                com.havamania.ui.theme.ThemeManager.getDefaultCity(getApplication()).firstOrNull()?.let { defaultCity ->
                    android.util.Log.d("HAVAMANIA_AI", "Auto-fetching weather for ${defaultCity.name}")
                    repository.getWeatherData(defaultCity.latitude, defaultCity.longitude, defaultCity.name, defaultCity.district).collect {}
                }
            } catch (e: Exception) {
                android.util.Log.e("HAVAMANIA_AI", "Auto-fetch failed: ${e.message}")
            }
        }
    }

    private fun logWeatherState(data: WeatherData?) {
        android.util.Log.d("HAVAMANIA_AI_DEBUG", """
            Assistant weather state:
            - currentWeather: ${data != null}
            - temperature: ${data?.temperature}
            - condition: ${data?.condition}
            - city: ${data?.cityName}
            - hourlyForecast size: ${data?.hourlyForecast?.size ?: 0}
            - weather loaded bool: ${data != null}
        """.trimIndent())
    }

    private fun loadConfig() {
        viewModelScope.launch {
            try {
                val cfg = api.getConfig(botId)
                _config.value = cfg
            } catch (e: Exception) {
                android.util.Log.e("HAVAMANIA_AI", "Config loading Error", e)
            }
        }
    }

    private fun cleanMarkdown(text: String): String {
        return text.replace(Regex("\\*\\*"), "")
            .replace(Regex("###"), "")
            .replace(Regex("##"), "")
            .replace(Regex("#"), "")
            .replace(Regex("\\*"), "")
            .replace(Regex("_"), "")
            .trim()
    }

    private fun buildWeatherContext(userQuestion: String): String {
        val activeTravels = _activeTravels.value
        val currentWeatherData = _weatherData.value
        val questionCity = AiIntentParser.detectCity(userQuestion)
        val intent = AiIntentParser.detectIntent(userQuestion)

        // 1. Current Location Info
        val currentCity = currentWeatherData?.cityName ?: "Bilinmiyor"
        val currentInfo = if (currentWeatherData != null) {
            """
            MEVCUT KONUM BİLGİSİ:
            Şehir: $currentCity
            Hava Durumu: ${currentWeatherData.condition}
            Sıcaklık: ${currentWeatherData.temperature} (Hissedilen: ${currentWeatherData.feelsLike})
            Yağış İhtimali: ${WeatherUtils.formatRainProbability(currentWeatherData.precipitationProbability)}
            Rüzgar: ${currentWeatherData.windSpeed ?: "Bilinmiyor"} km/sa
            """.trimIndent()
        } else "MEVCUT KONUM BİLGİSİ: Şu an ulaşılamıyor."

        // 2. Travel Context Info (Calendar)
        val travelInfo = if (activeTravels.isNotEmpty()) {
            "TAKVİMDEKİ PLANLANMIŞ SEYAHATLER:\n" + activeTravels.joinToString("\n") {
                "- ${it.city} (Tarih: ${it.startDate} ile ${it.endDate} arası, Tip: ${it.tripType.label}, Hava: ${it.lastForecastSnapshot?.conditionSummary ?: "Henüz analiz edilmedi"})"
            }
        } else "TAKVİMDEKİ PLANLANMIŞ SEYAHATLER: Kayıtlı aktif bir seyahat planı bulunmuyor."

        // 3. Selection of specific city data if needed
        val targetCity = questionCity ?: if (intent == AiIntent.TRAVEL && activeTravels.isNotEmpty()) activeTravels.first().city else null

        val detailedCityData = if (targetCity != null) {
             val travelPlan = activeTravels.find { AiIntentParser.normalizeTurkish(it.city) == AiIntentParser.normalizeTurkish(targetCity) }
             val snapshot = travelPlan?.lastForecastSnapshot
             if (snapshot != null) {
                 "\nSORULAN ŞEHİR ANALİZİ ($targetCity):\n" +
                 "Beklenen Hava: ${snapshot.conditionSummary}, Sıcaklık: ${snapshot.maxTemp?.toInt()}°C, Yağış Riski: %${snapshot.precipitationProbability ?: 0}"
             } else ""
        } else ""

        return """
            $currentInfo

            $travelInfo
            $detailedCityData

            KRİTİK KURALLAR:
            1. 'MEVCUT KONUM' ve 'SEYAHAT DESTİNASYONU' ayrımını kesin yap.
            2. Eğer kullanıcı 'seyahatim', 'gideceğim yer', 'tatilim' gibi ifadeler kullanıyorsa SADECE 'TAKVİMDEKİ PLANLANMIŞ SEYAHATLER' bilgisini kullan.
            3. Eğer Takvim boşsa ve kullanıcı seyahati hakkında soru soruyorsa: "Henüz planlanmış bir seyahatin bulunmuyor. İstersen yeni bir seyahat planlayabiliriz." cevabını ver.
            4. Mevcut konumu (örn: $currentCity) asla bir seyahat destinasyonuymuş gibi ('$currentCity seyahatiniz' şeklinde) sunma.
            5. Kullanıcı spesifik bir şehri sormadıkça ve takvimde o şehre seyahat yoksa, mevcut konum hakkında seyahat diliyle konuşma.
            6. Markdown yasak.
        """.trimIndent()
    }

    private fun buildToneInstruction(): String {
        return when (assistantTone) {
            AssistantTone.SAMIMI -> """
                [SİSTEM ROLÜ: SAMİMİ ARKADAŞ PERSONASI]
                - Karakter: Kullanıcının çok yakın, enerjik ve neşeli bir arkadaşısın.
                - Hitap: Kesinlikle "sen" diye hitap et. "Selam", "canım", "dostum" gibi sıcak ifadeler kullan.
                - Üslup: Tamamen günlük dil. Yapay ve kurumsal görünme. İnsan gibi konuş.
                - Emoji: Bol ve yerinde emoji kullan. 😎 😊
            """.trimIndent()
            AssistantTone.RESMI -> """
                [SİSTEM ROLÜ: PROFESYONEL KURUMSAL ASİSTAN PERSONASI]
                - Karakter: Ciddi, kurumsal ve profesyonel bir asistansın.
                - Hitap: Kesinlikle "siz" diye hitap et. Saygılı ve mesafeli ol.
                - Üslup: Ciddi anlatım. Gereksiz samimiyetten kaçın.
                - Emoji: KESİNLİKLE EMOJİ KULLANMA.
            """.trimIndent()
            AssistantTone.DENGELI -> """
                [SİSTEM ROLÜ: DENGELİ REHBER PERSONASI]
                - Karakter: Bilgilendirici, nazik ve profesyonel bir rehbersin.
                - Hitap: Doğal bir dil kullan.
                - Üslup: Profesyonel ama doğal. Orta uzunlukta, kullanıcıyı yormayan cevaplar ver.
                - Emoji: Gerektiğinde 1-2 tane kullanabilirsin.
            """.trimIndent()
            AssistantTone.KISA_NET -> """
                [SİSTEM ROLÜ: VERİMLİLİK ODAKLI ASİSTAN PERSONASI]
                - Karakter: Sadece sonuca odaklı, vakit kaybetmeyen bir asistansın.
                - Üslup: En hızlı ve kısa cevabı ver. Maksimum 2-4 cümle.
                - Kural: Liste yapısı (✓) tercih et. Uzun paragraf kesinlikle yasak. Teknik detay verme.
            """.trimIndent()
            AssistantTone.DETAYLI_UZMAN -> """
                [SİSTEM ROLÜ: KIDEMLİ METEOROLOJİ UZMANI PERSONASI]
                - Karakter: Teknik bilgiye sahip, analitik düşünen bir meteoroloji danışmanısın.
                - Üslup: En detaylı mod. Neden-sonuç ilişkileri kur. Teknik açıklamalar ve meteorolojik yorumlar ekle.
                - Kural: Uzman görüşü hissi ver.
            """.trimIndent()
        }
    }

    fun sendMessage(text: String, systemContext: String? = null, isRetry: Boolean = false) {
        if (text.isBlank() || _isLoading.value) return

        val truncatedText = if (text.length > 4000) text.take(4000) else text

        if (!isRetry) {
            val userMsg = AltikodChatMessage(text = truncatedText, isUser = true)
            _messages.value = _messages.value + userMsg
        }

        _isLoading.value = true

        val weatherContext = buildWeatherContext(truncatedText)
        val toneInstruction = buildToneInstruction()
        val languageInstruction = if (language == "EN") "IMPORTANT: Answer ONLY in English." else "ÖNEMLİ: Sadece Türkçe cevap ver."
        val personalContext = if (userInterests.isNotEmpty() || userAboutMe.isNotBlank()) {
            "KULLANICI PROFİLİ:\nİlgi Alanları: ${userInterests.joinToString()}\nBilgi: $userAboutMe\n"
        } else ""

        val intent = AiIntentParser.detectIntent(truncatedText)
        val intentInstruction = when(intent) {
            AiIntent.CLOTHING -> "Giyim önerisine odaklan. Direkt ne giymesi gerektiğini söyle."
            AiIntent.ACTIVITY -> "Aktivite uygunluğuna odaklan. En iyi saatleri ve rüzgar/UV etkisini belirt."
            AiIntent.TRAVEL -> "Seyahat planlamasına odaklan. Rota ve hava riski analizi yap."
            AiIntent.PACKING -> "Valiz listesi üret. ✓ ikonlarını kullan. Örn: ✓ Şemsiye"
            AiIntent.CALENDAR -> """
                TAKVİM OPTİMİZASYONU KURALLARI:
                1. Kullanıcının 'AKTİF SEYAHATLER' listesindeki etkinlikleri tek tek listele.
                2. Her etkinliğin tarihini ve şehrini belirt.
                3. Hava verilerine göre her etkinlik için 'Düşük/Orta/Yüksek' risk puanı ver.
                4. Riskliyse, hava durumunun daha iyi olduğu alternatif bir tarih öner.
                5. 'Dikkatli olun', 'Gözden geçirin' gibi genel ifadeler yerine somut veriler (örn: %60 yağış, 35 derece sıcaklık) kullan.
                6. Eğer takvim boşsa sadece "Takviminizde yaklaşan etkinlik bulunmuyor" de.
            """.trimIndent()
            AiIntent.WEEKEND_FORECAST -> "Sadece hafta sonu hava durumuna odaklan."
            AiIntent.TRIP_RISK -> "Seyahat risklerine (fırtına, aşırı sıcak vb.) odaklan."
            AiIntent.TRIP_TIMING -> "Yolculuk için en uygun saatleri öner."
            AiIntent.TRIP_ROUTE -> "Yol durumu ve güzergah üzerindeki hava koşullarına odaklan."
            AiIntent.GENERAL_WEATHER -> "Hava durumunun genel bir özetini ve meteorolojik analizini ver."
            AiIntent.OUTDOOR_EVENT -> "Dış mekan etkinliği için hava uygunluğunu analiz et."
            AiIntent.CHAT -> "Genel bir sohbete uygun, yardımsever ve doğal bir yanıt ver."
        }

        // Tone instruction is placed AT THE END of the prompt and made extremely explicit
        val explicitTonePrompt = """

            [DİKKAT: KRİTİK TALİMATLAR]
            1. DİL: $languageInstruction
            2. ÜSLUP: $toneInstruction
            3. ODAK (İNTENT): $intentInstruction
            4. KİŞİSELLEŞTİRME: Kullanıcının profilini ve takvimindeki (varsa) seyahat planlarını doğal bir şekilde cevaba yedir.
            5. BAĞLAM KULLANIMI: Kullanıcı seyahat hakkında genel bir soru sorarsa (örn: 'Seyahat planlamama yardım et'), öncelikle AKTİF SEYAHATLER listesindeki şehirleri kontrol et ve o şehirler üzerinden cevap ver. Eğer liste boşsa, kullanıcıya hangi şehir için plan istediğini sor. Asla 'gidilecek yer' veya 'varış noktanız' gibi belirsiz terimler kullanma.
            6. VERİ YOK DURUMU: Bir seyahat için hava durumu verisi henüz mevcut değilse (örn: 10 günden uzaksa), 'Veri yok' deme. Bunun yerine 'Seyahatinize henüz vakit olduğu için tahminler yaklaştığında analiz yapabileceğim' gibi kullanıcı dostu bir açıklama yap.

            Cevabın yukarıdaki üslup, dil ve odak kurallarına %100 uymalıdır.
            Karakterine bürün ve asla bu karakterden çıkma.
        """.trimIndent()

        val fullQuestion = "$weatherContext\n$personalContext\n$explicitTonePrompt\n\nKullanıcı Sorusu: $truncatedText"

        viewModelScope.launch {
            logWeatherState(_weatherData.value)
            try {
                val response = kotlinx.coroutines.withTimeout(35000) {
                    api.sendMessage(botId, AltikodChatRequest(question = fullQuestion, session_id = sessionId))
                }

                var answer = cleanMarkdown(response.answer)
                if (answer.isBlank()) throw Exception("Empty response")

                // Check if AI didn't understand (Common error phrases)
                val isUnknownResponse = answer.contains("geçerli bir soru", ignoreCase = true) ||
                                       answer.contains("anlamadım", ignoreCase = true) ||
                                       answer.length < 5

                val isWeatherQuery = AiIntentParser.isWeatherQuery(truncatedText)
                val city = AiIntentParser.detectCity(truncatedText)
                val date = AiIntentParser.detectDate(truncatedText)

                // SEYAHAT ÖNERİSİ KURALI: Aktif şehir ile sorgulanan şehir aynıysa seyahat önerisi yapma
                val currentCity = _weatherData.value?.cityName
                val isDifferentCity = city != null && currentCity != null &&
                        AiIntentParser.normalizeTurkish(city) != AiIntentParser.normalizeTurkish(currentCity)

                // If it's a valid weather query but AI gave a generic "don't understand" answer,
                // replace it with our local detailed weather answer.
                var finalIsUnknown = isUnknownResponse
                if (isWeatherQuery && isUnknownResponse && _weatherData.value != null) {
                    answer = RecommendationEngine.generateAssistantFallbackReply(
                        userPrompt = truncatedText,
                        weatherData = _weatherData.value,
                        aboutMe = userAboutMe,
                        interests = userInterests,
                        tone = assistantTone,
                        travelPlans = _activeTravels.value
                    )
                    finalIsUnknown = false // It's now a known response (fallback)
                }

                // Enrich with action if applicable (Only if we have a successful weather-related answer and it's a DIFFERENT city)
                val detectedAction = if (isWeatherQuery && !finalIsUnknown && isDifferentCity) {
                    if (city != null) {
                        val today = LocalDate.now()
                        if (date == null || !date.isBefore(today)) {
                             AssistantAction(
                                type = AssistantActionType.CREATE_TRAVEL_PLAN,
                                label = "✈️ Seyahat Planla",
                                city = city,
                                startDate = date?.toString(),
                                tripName = "$city Seyahati"
                            )
                        } else null
                    } else null
                } else null

                val finalAnswer = if (detectedAction != null) {
                    val today = LocalDate.now()
                    val startDate = detectedAction.startDate?.let { LocalDate.parse(it) }

                    val invitationText = if (startDate != null) {
                        val d = startDate
                        val monthName = d.month.getDisplayName(TextStyle.FULL, Locale("tr", "TR"))
                        val dateLabel = "${d.dayOfMonth} $monthName"

                        when (assistantTone) {
                            AssistantTone.SAMIMI -> "Eğer $dateLabel tarihinde ${detectedAction.city}’e gitmeyi planlıyorsan, senin için harika bir seyahat planı oluşturabilirim! 😊"
                            AssistantTone.RESMI -> "$dateLabel tarihinde ${detectedAction.city} şehrine gerçekleştirmeyi planladığınız seyahat için sistemimiz üzerinden bir seyahat planı oluşturabilirsiniz."
                            AssistantTone.KISA_NET -> "Seyahat Planla: ${detectedAction.city} ($dateLabel)"
                            AssistantTone.DETAYLI_UZMAN -> "$dateLabel tarihinde ${detectedAction.city} bölgesine yapacağınız seyahatin meteorolojik risk analizini içeren kapsamlı bir seyahat planı oluşturmamı ister misiniz?"
                            else -> "Eğer $dateLabel tarihinde ${detectedAction.city}’e gitmeyi planlıyorsan, senin için harika bir seyahat planı oluşturabilirim."
                        }
                    } else {
                        when (assistantTone) {
                            AssistantTone.SAMIMI -> "Eğer ${detectedAction.city}’e gitmeyi planlıyorsan, senin için harika bir seyahat planı oluşturabilirim! 😊"
                            AssistantTone.RESMI -> "${detectedAction.city} şehrine gerçekleştirmeyi planladığınız seyahat için sistemimiz üzerinden bir seyahat planı oluşturabilirsiniz."
                            AssistantTone.KISA_NET -> "Seyahat Planla: ${detectedAction.city}"
                            AssistantTone.DETAYLI_UZMAN -> "${detectedAction.city} bölgesine yapacağınız seyahatin meteorolojik risk analizini içeren kapsamlı bir seyahat planı oluşturmamı ister misiniz?"
                            else -> "Eğer ${detectedAction.city}’e gitmeyi planlıyorsan, senin için harika bir seyahat planı oluşturabilirim."
                        }
                    }

                    "$answer\n\n$invitationText"
                } else answer

                _messages.value = _messages.value + AltikodChatMessage(
                    text = finalAnswer,
                    isUser = false,
                    action = detectedAction
                )
            } catch (e: Exception) {
                android.util.Log.e("HAVAMANIA_AI", "AI ERROR: ${e.message}")
                addErrorMessage(truncatedText, "Asistan şu an yoğun, yerel verilere göre yanıt veriyorum.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun addErrorMessage(userPrompt: String, prefix: String) {
        try {
            val fallbackText = RecommendationEngine.generateAssistantFallbackReply(
                userPrompt = userPrompt,
                weatherData = _weatherData.value,
                aboutMe = userAboutMe,
                interests = userInterests,
                tone = assistantTone,
                travelPlans = _activeTravels.value
            )

            val combinedMessage = if (_weatherData.value == null) {
                "Hava verileri şu anda alınamıyor. Lütfen daha sonra tekrar deneyin."
            } else if (fallbackText.contains("Hava durumu bilgilerini")) {
                fallbackText
            } else {
                "$prefix $fallbackText"
            }

            // Enrich with action if applicable (Only if city detected and it's a weather query)
            val isWeatherQuery = AiIntentParser.isWeatherQuery(userPrompt)
            val city = AiIntentParser.detectCity(userPrompt)
            val date = AiIntentParser.detectDate(userPrompt)

            val currentCity = _weatherData.value?.cityName
            val isDifferentCity = city != null && currentCity != null &&
                    AiIntentParser.normalizeTurkish(city) != AiIntentParser.normalizeTurkish(currentCity)

            val detectedAction = if (isWeatherQuery && city != null && isDifferentCity) {
                val today = LocalDate.now()
                if (date == null || !date.isBefore(today)) {
                    AssistantAction(
                        type = AssistantActionType.CREATE_TRAVEL_PLAN,
                        label = "✈️ Seyahat Planla",
                        city = city,
                        startDate = date?.toString(),
                        tripName = "$city Seyahati"
                    )
                } else null
            } else null

            _messages.value = _messages.value + AltikodChatMessage(
                text = combinedMessage,
                isUser = false,
                isFallback = true,
                retryPrompt = userPrompt,
                action = detectedAction
            )
        } catch (e: Exception) {
            _messages.value = _messages.value + AltikodChatMessage(
                text = "Hava durumuna şu an ulaşılamıyor, lütfen daha sonra tekrar deneyin.",
                isUser = false,
                isFallback = true,
                retryPrompt = userPrompt
            )
        }
    }

    fun finishChat(onFinished: (List<AltikodChatMessage>) -> Unit) {
        val currentMessages = _messages.value
        if (currentMessages.isEmpty()) return

        onFinished(currentMessages)
        resetChat()
    }

    fun resetChat() {
        _messages.value = emptyList()
        sessionId = UUID.randomUUID().toString()
    }
}

// --- UI BILESENLERI ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    initialRecommendation: HavamaniaRecommendation? = null,
    onBack: () -> Unit,
    onNavigateToTravelCreate: (String, String?) -> Unit = { _, _ -> },
    viewModel: AiChatViewModel = viewModel(),
    historyViewModel: AiHistoryViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel()
) {
    val themeColors = HavamaniaTheme.colors
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val currentWeatherData by viewModel.weatherData.collectAsStateWithLifecycle()
    val aboutMe by themeViewModel.userAboutMe.collectAsStateWithLifecycle()
    val userInterests by themeViewModel.userInterests.collectAsStateWithLifecycle()
    val assistantTone by themeViewModel.assistantTone.collectAsStateWithLifecycle()
    val language by themeViewModel.language.collectAsStateWithLifecycle()

    // Sync non-weather data with ViewModel
    LaunchedEffect(aboutMe, userInterests, assistantTone, language) {
        viewModel.userAboutMe = aboutMe
        viewModel.userInterests = userInterests
        viewModel.assistantTone = assistantTone
        viewModel.language = language
    }

    var showEndChatDialog by remember { mutableStateOf(false) }
    val bgColors = themeColors.gradientPrimary
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // İlk girdi (initialRecommendation) varsa gönder
    LaunchedEffect(initialRecommendation, currentWeatherData) {
        if (initialRecommendation != null && currentWeatherData != null && messages.isEmpty()) {
            val context = buildPersonalizedContext(aboutMe, userInterests)
            viewModel.sendMessage(initialRecommendation.message, systemContext = context)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(bgColors)))

        Column(modifier = Modifier.fillMaxSize()) {
            HavamaniaTopBar(
                title = config?.name ?: "HAVAMANIA ASİSTAN",
                onBack = {
                    if (messages.isNotEmpty()) {
                        viewModel.resetChat()
                    } else {
                        onBack()
                    }
                },
                // ... rest of the TopBar logic ...
                actions = {
                    if (messages.isNotEmpty()) {
                        Surface(
                            onClick = { showEndChatDialog = true },
                            color = themeColors.accent.copy(alpha = 0.15f),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.StopCircle,
                                    contentDescription = null,
                                    tint = themeColors.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Sohbeti Bitir",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = themeColors.accent
                                )
                            }
                        }
                    } else {
                        val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
                        val sparkleAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
                            label = "alpha"
                        )
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = themeColors.accent,
                            modifier = Modifier.size(24.dp).alpha(sparkleAlpha).padding(end = 12.dp)
                        )
                    }
                }
            )

            if (currentWeatherData == null && messages.isEmpty()) {
                // Weather Loading State
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = themeColors.accent)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Hava verisi yükleniyor...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = themeColors.textPrimary.copy(alpha = 0.7f)
                    )
                }
            } else if (messages.isEmpty()) {
                // Başlangıç Ekranı (Empty State)
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(Modifier.height(12.dp))
                    WelcomeCard(config?.welcome_message ?: "Merhaba! Havamania Asistan'a hoş geldiniz. Size nasıl yardımcı olabilirim?", themeColors)

                    if (aboutMe.isNotBlank() || userInterests.isNotEmpty()) {
                        PersonalizedContextCard(aboutMe, themeColors)
                    }

                    // 1. ANA HAVA DURUMU KARTI (PREMIUM)
                    if (currentWeatherData != null) {
                        AssistantWeatherCard(currentWeatherData!!, themeColors)
                    }

                    // 2. BUGÜN İÇİN ÖZET (AI ANALİZİ)
                    if (currentWeatherData != null) {
                        TodaySummarySection(currentWeatherData!!, themeColors)
                    }

                    // 3. NELER YAPABİLİRİM? (ÖZELLİKLER)
                    AssistantSectionLabel("NELER YAPABİLİRİM?")
                    FeatureCards(
                        themeColors = themeColors,
                        onCardClick = { prompt ->
                            val context = buildPersonalizedContext(aboutMe, userInterests)
                            viewModel.sendMessage(prompt, systemContext = context)
                        }
                    )

                    // 4. HIZLI SORULAR
                    AssistantSectionLabel("HIZLI SORULAR")
                    QuickSuggestions(
                        onSuggestionClick = { prompt ->
                            val context = buildPersonalizedContext(aboutMe, userInterests)
                            viewModel.sendMessage(prompt, systemContext = context)
                        },
                        themeColors = themeColors
                    )

                    Spacer(Modifier.height(32.dp))
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatBubble(
                            message = message,
                            themeColors = themeColors,
                            onRetry = { prompt ->
                                viewModel.sendMessage(prompt, isRetry = true)
                            },
                            onActionClick = { action ->
                                if (action.type == AssistantActionType.CREATE_TRAVEL_PLAN) {
                                    onNavigateToTravelCreate(action.city ?: "", action.startDate)
                                }
                            }
                        )
                    }
                    if (isLoading) {
                        item {
                            TypingIndicator(themeColors)
                        }
                    }
                }
            }

            ChatInput(
                onSend = { prompt ->
                    val context = if (messages.isEmpty()) {
                        buildPersonalizedContext(aboutMe, userInterests)
                    } else null
                    viewModel.sendMessage(prompt, systemContext = context)
                },
                isLoading = isLoading,
                themeColors = themeColors
            )
        }
    }

    if (showEndChatDialog) {
        AlertDialog(
            onDismissRequest = { showEndChatDialog = false },
            containerColor = themeColors.surface,
            title = { Text("Sohbeti sonlandırmak istiyor musun?", style = MaterialTheme.typography.titleLarge, color = themeColors.textPrimary) },
            text = { Text("Bu sohbet AI geçmişine kaydedilecek.", color = themeColors.textSecondary) },
            dismissButton = {
                TextButton(onClick = { showEndChatDialog = false }) {
                    Text("Vazgeç", color = themeColors.textSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndChatDialog = false
                        viewModel.finishChat { msgs ->
                            val firstUserMsg = msgs.firstOrNull { it.isUser }?.text ?: "AI Sohbet"
                            val firstAiMsg = msgs.firstOrNull { !it.isUser }?.text ?: "Hava durumu analizi"
                            historyViewModel.addHistoryItem(
                                title = firstUserMsg,
                                summary = firstAiMsg,
                                messages = msgs,
                                cityName = null
                            )
                        }
                        // Reset chat locally and stay on this screen to show WelcomeCard
                        viewModel.resetChat()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("Sohbeti Bitir", color = Color.White)
                }
            }
        )
    }
}

fun buildPersonalizedContext(aboutMe: String, interests: Set<String>): String {
    if (aboutMe.isBlank() && interests.isEmpty()) return ""

    val interestsStr = if (interests.isNotEmpty()) {
        "Kullanıcının ilgi alanları: ${interests.joinToString(", ")}. "
    } else ""

    val aboutMeStr = if (aboutMe.isNotBlank()) {
        "Kullanıcı hakkında bilgi: \"$aboutMe\". "
    } else ""

    return "Sistem Talimatı: Aşağıdaki kullanıcı profiline göre daha kişiselleştirilmiş bir cevap ver. " +
            "$interestsStr$aboutMeStr"
}

@Composable
fun AssistantSectionLabel(text: String) {
    val themeColors = HavamaniaTheme.colors
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.5.sp),
        color = themeColors.textPrimary.copy(alpha = 0.4f),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
fun AssistantWeatherCard(weather: WeatherData, themeColors: HavamaniaColors) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .fillMaxWidth(),
        color = themeColors.surfaceGlass.copy(alpha = 0.5f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.border.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        weather.cityName.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = themeColors.textSecondary.copy(alpha = 0.6f)
                    )
                    Text(
                        weather.condition,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = themeColors.textPrimary
                    )
                }
                Text(
                    WeatherUtils.getWeatherEmoji(weather.weatherCode),
                    fontSize = 40.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    if (weather.temperature.contains("°")) "${weather.temperature}C" else "${weather.temperature}°C",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = themeColors.textPrimary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (weather.feelsLike.contains("°")) "Hissedilen ${weather.feelsLike}C" else "Hissedilen ${weather.feelsLike}°C",
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Divider(color = themeColors.border.copy(alpha = 0.1f))
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeatherMetricItem(Icons.Rounded.WaterDrop, "NEM", "${weather.humidity ?: 0}%", themeColors)
                WeatherMetricItem(Icons.Rounded.Air, "RÜZGAR", "${weather.windSpeed?.toInt() ?: 0} km/s", themeColors)
                WeatherMetricItem(Icons.Rounded.WbSunny, "UV", "${weather.uvIndex ?: 0}", themeColors)
                WeatherMetricItem(Icons.Rounded.Umbrella, "YAĞIŞ", "${weather.precipitationProbability ?: 0}%", themeColors)
            }
        }
    }
}

@Composable
fun WeatherMetricItem(icon: ImageVector, label: String, value: String, themeColors: HavamaniaColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            color = themeColors.textSecondary.copy(alpha = 0.5f)
        )
        Text(
            value,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
            color = themeColors.textPrimary
        )
    }
}

@Composable
fun TodaySummarySection(weather: WeatherData, themeColors: HavamaniaColors) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        AssistantSectionLabel("BUGÜN İÇİN ÖZET")

        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            color = themeColors.surfaceGlass.copy(alpha = 0.3f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.border.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Bu öneriler normalde AI'dan gelir, ancak burada weather verilerine göre statik-dinamik eşleştiriyoruz.
                val summaries = remember(weather) {
                    val list = mutableListOf<Pair<ImageVector, String>>()

                    val tempValue = weather.temperature.replace("°", "").replace("C", "").trim().toDoubleOrNull() ?: 20.0
                    val uvValue = weather.uvIndex?.toDouble() ?: 0.0
                    val precipValue = weather.precipitationProbability ?: 0

                    // Giyim
                    if (tempValue > 25) {
                        list.add(Icons.Rounded.Checkroom to "İnce ve nefes alabilen kıyafetler tercih edebilirsin.")
                    } else if (tempValue > 15) {
                        list.add(Icons.Rounded.Checkroom to "Hafif bir ceket veya sweatshirt uygun olacaktır.")
                    } else {
                        list.add(Icons.Rounded.Checkroom to "Kalın ve koruyucu kıyafetler giymen önerilir.")
                    }

                    // UV
                    if (uvValue > 5.0) {
                        list.add(Icons.Rounded.WbSunny to "UV seviyesi yüksek, güneş gözlüğü ve krem önerilir.")
                    }

                    // Aktivite / Yağış
                    if (precipValue > 40) {
                        list.add(Icons.Rounded.Umbrella to "Yağış bekleniyor, yanına şemsiye almayı unutma.")
                    } else {
                        list.add(Icons.Rounded.DirectionsRun to "Açık hava aktiviteleri için uygun bir gün.")
                        list.add(Icons.Rounded.CloudOff to "Bugün yağış beklenmiyor.")
                    }

                    list.take(4)
                }

                summaries.forEachIndexed { index, summary ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            summary.first,
                            null,
                            tint = themeColors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            summary.second,
                            style = MaterialTheme.typography.bodyMedium,
                            color = themeColors.textPrimary.copy(alpha = 0.9f)
                        )
                    }
                    if (index < summaries.size - 1) {
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionChip(label: String, icon: ImageVector, themeColors: HavamaniaColors, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = themeColors.accent.copy(alpha = 0.1f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = themeColors.accent)
        }
    }
}

@Composable
fun PersonalizedContextCard(aboutMe: String, themeColors: HavamaniaColors) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).padding(top = 16.dp).fillMaxWidth(),
        color = themeColors.accent.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = themeColors.accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Kişiselleştirilmiş mod aktif: AI seni tanıyor.",
                style = MaterialTheme.typography.labelSmall,
                color = themeColors.accent
            )
        }
    }
}

@Composable
fun WelcomeCard(message: String, themeColors: HavamaniaColors) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = themeColors.surfaceGlass.copy(alpha = 0.5f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.border.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(themeColors.accent.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = themeColors.accent, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = themeColors.textPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

enum class AssistantFeatureType { CLOTHING, ACTIVITY, TRAVEL, SUITCASE, CALENDAR, ASSISTANT }

data class AssistantFeature(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val prompt: String,
    val type: AssistantFeatureType
)

@Composable
fun FeatureCards(themeColors: HavamaniaColors, onCardClick: (String) -> Unit) {
    val features = remember {
        listOf(
            AssistantFeature(
                title = "Giyim Danışmanı",
                desc = "Hava durumuna göre stil tavsiyesi.",
                icon = Icons.Rounded.Checkroom,
                prompt = "Bugünkü hava durumuna göre ne giymeliyim? Sıcaklık, rüzgar, yağış ve UV durumuna göre pratik kıyafet önerisi ver.",
                type = AssistantFeatureType.CLOTHING
            ),
            AssistantFeature(
                title = "Aktivite Rehberi",
                desc = "Dışarı çıkmak için en iyi zaman.",
                icon = Icons.Rounded.DirectionsRun,
                prompt = "Bugün dışarı çıkmak, yürüyüş yapmak, spor yapmak veya açık hava aktivitesi için uygun mu? Hava durumuna göre en uygun saatleri ve dikkat etmem gerekenleri söyle.",
                type = AssistantFeatureType.ACTIVITY
            ),
            AssistantFeature(
                title = "Seyahat Planlayıcı",
                desc = "Rotalarınız için özel tavsiyeler.",
                icon = Icons.Rounded.FlightTakeoff,
                prompt = "Hava durumuna göre seyahat planlamama yardımcı olur musun?",
                type = AssistantFeatureType.TRAVEL
            ),
            AssistantFeature(
                title = "Valiz Asistanı",
                desc = "Eksiksiz bir çanta hazırlığı.",
                icon = Icons.Rounded.WorkOutline,
                prompt = "Hava koşullarını dikkate alarak valizime neler koymam gerektiğini söyler misin?",
                type = AssistantFeatureType.SUITCASE
            ),
            AssistantFeature(
                title = "Hava Destekli Takvim",
                desc = "Etkinliklerinizi havaya uydurun.",
                icon = Icons.Rounded.CalendarMonth,
                prompt = "Önümüzdeki günlerin hava durumuna göre takvimimi nasıl optimize edebilirim?",
                type = AssistantFeatureType.CALENDAR
            ),
            AssistantFeature(
                title = "AI Asistan",
                desc = "Hava hakkında her şeyi sorun.",
                icon = Icons.Rounded.AutoAwesome,
                prompt = "Hava durumu hakkında genel bir analiz ve tavsiye verir misin?",
                type = AssistantFeatureType.ASSISTANT
            )
        )
    }

    // Grid 2 satır x 3 sütun için Column ve Row kullanımı
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        features.chunked(3).forEach { rowFeatures ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowFeatures.forEach { feature ->
                    FeatureCard(
                        title = feature.title,
                        desc = feature.desc,
                        icon = feature.icon,
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f),
                        onClick = { onCardClick(feature.prompt) }
                    )
                }
                // Eğer satırda 3'ten az kart varsa boşluk bırakmak için:
                if (rowFeatures.size < 3) {
                    repeat(3 - rowFeatures.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    desc: String,
    icon: ImageVector,
    themeColors: HavamaniaColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = themeColors.surfaceGlass.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.border.copy(alpha = 0.1f)),
        modifier = modifier.height(130.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(24.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = themeColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = themeColors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

data class AssistantSuggestion(
    val label: String,
    val prompt: String
)

@Composable
fun QuickSuggestions(
    onSuggestionClick: (String) -> Unit,
    themeColors: HavamaniaColors
) {
    val suggestions = remember {
        listOf(
            AssistantSuggestion(
                label = "Bugün ne giymeliyim?",
                prompt = "Bugünkü hava durumuna göre ne giymeliyim?"
            ),
            AssistantSuggestion(
                label = "Hafta sonu hava nasıl?",
                prompt = "Bulunduğum şehir için hafta sonu hava durumu nasıl görünüyor? Plan yaparken nelere dikkat etmeliyim?"
            ),
            AssistantSuggestion(
                label = "Dışarı çıkmak için uygun mu?",
                prompt = "Bugün dışarı çıkmak için hava uygun mu? Yağış, rüzgar, sıcaklık ve UV durumuna göre yorumla."
            ),
            AssistantSuggestion(
                label = "Yağmur yağacak mı?",
                prompt = "Bugün bulunduğum şehirde yağmur yağma ihtimali var mı? Hangi saatlerde dikkatli olmalıyım?"
            ),
            AssistantSuggestion(
                label = "Valizime ne almalıyım?",
                prompt = "Yaklaşan seyahatlerim ve hava durumuna göre valizime neler almalıyım?"
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Öneriler",
            style = MaterialTheme.typography.labelSmall,
            color = themeColors.textSecondary.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                Surface(
                    onClick = { onSuggestionClick(suggestion.prompt) },
                    color = themeColors.surfaceGlass.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.border.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = suggestion.label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeColors.textPrimary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun ChatBubble(
    message: AltikodChatMessage,
    themeColors: HavamaniaColors,
    onRetry: (String) -> Unit = {},
    onActionClick: (AssistantAction) -> Unit = {}
) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isUser) themeColors.accent else themeColors.surfaceGlass
    val textColor = if (message.isUser) Color.White else themeColors.textPrimary
    val shape = if (message.isUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            tonalElevation = 1.dp,
            shadowElevation = 0.5.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(
                    text = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 19.sp, fontSize = 14.sp)
                )

                if (message.action != null) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onActionClick(message.action) },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Route, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(message.action.label, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (message.isFallback && message.retryPrompt != null) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        onClick = { onRetry(message.retryPrompt) },
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Refresh, null, tint = textColor, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Tekrar dene",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatInput(onSend: (String) -> Unit, isLoading: Boolean, themeColors: HavamaniaColors) {
    var text by remember { mutableStateOf("") }
    var showMicSoon by remember { mutableStateOf(false) }

    Surface(
        color = themeColors.surfaceGlass.copy(alpha = 0.9f),
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { showMicSoon = true },
                enabled = !isLoading
            ) {
                Icon(Icons.Rounded.Mic, contentDescription = "Sesli Yaz", tint = themeColors.textSecondary)
            }

            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Bir şeyler sorun...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = themeColors.accent
                ),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.isNotBlank()) {
                            onSend(text)
                            text = ""
                        }
                    }
                ),
                enabled = !isLoading
            )

            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend(text)
                        text = ""
                    }
                },
                enabled = !isLoading && text.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = themeColors.accent,
                    contentColor = Color.White,
                    disabledContainerColor = themeColors.accent.copy(alpha = 0.5f)
                )
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Gönder")
            }
        }
    }

    if (showMicSoon) {
        AlertDialog(
            onDismissRequest = { showMicSoon = false },
            containerColor = themeColors.surface,
            title = { Text("YAKINDA", fontWeight = FontWeight.Black, color = themeColors.textPrimary) },
            text = { Text("Sesli asistan özelliği çok yakında Havamania'ya eklenecek!", color = themeColors.textSecondary) },
            confirmButton = {
                TextButton(onClick = { showMicSoon = false }) {
                    Text("TAMAM", fontWeight = FontWeight.Black, color = themeColors.accent)
                }
            }
        )
    }
}

@Composable
fun TypingIndicator(themeColors: HavamaniaColors) {
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Havamania düşünüyor...",
            style = MaterialTheme.typography.labelSmall,
            color = themeColors.textSecondary.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "typing")
            repeat(3) { index ->
                val delay = index * 200
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = delay),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot_alpha"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(themeColors.accent.copy(alpha = alpha))
                )
            }
        }
    }
}
