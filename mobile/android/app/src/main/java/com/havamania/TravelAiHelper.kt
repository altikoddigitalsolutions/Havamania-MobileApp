package com.havamania

import com.havamania.ui.theme.AssistantTone
import java.time.LocalDate
import java.util.Locale

object TravelAiHelper {

    private data class CityDetails(
        val description: String,
        val visitPlace: String,
        val localDish: String,
        val localAdvice: String,
        val cityTip: String,
        val friendGreeting: String
    )

    private val CITY_DATA = mapOf(
        "istanbul" to CityDetails(
            description = "İki kıtanın birleştiği büyüleyici metropol",
            visitPlace = "Galata Kulesi civarındaki ara sokakları keşfedip vapurla karşıya geçmeyi unutma. Boğaz havası her zaman iyi gelir.",
            localDish = "Karaköy'de taze bir balık ekmek and ardından meşhur bir baklava harika bir ikili olur.",
            localAdvice = "Beşiktaş'tan Ortaköy'e uzanan sahil şeridinde sabah yürüyüşü yapıp boğazın keyfini çıkar.",
            cityTip = "Vapur seyahatlerini gün batımına denk getirirsen İstanbul'un o eşsiz silüeti eşliğinde harika fotoğraflar yakalayabilirsin.",
            friendGreeting = "İstanbul seni o bitmek bilmeyen enerjisi ve büyüleyici manzarasıyla bekliyor."
        ),
        "ankara" to CityDetails(
            description = "Cumhuriyetin kalbi ve bozkırın modern yüzü",
            visitPlace = "Anıtkabir'in huzurlu atmosferini ve ardından Hamamönü'nün tarihi dokusunu mutlaka gör.",
            localDish = "Ankara Kalesi civarında gerçek bir Ankara döneri denemeden dönme, farkı hemen anlayacaksın.",
            localAdvice = "Kuğulu Park'ta kuğuları izleyerek kahveni yudumlayıp şehrin temposuna kısa bir mola ver.",
            cityTip = "Eymir Gölü'nde sabah saatlerinde kısa bir yürüyüş yapmak Ankara'nın o resmi havasını bir anda dağıtabilir.",
            friendGreeting = "Başkentin o ağırbaşlı ama bir o kadar da samimi atmosferine hoş geldin."
        ),
        "izmir" to CityDetails(
            description = "Ege'nin incisi ve özgürlüğün şehri",
            visitPlace = "İzmir'e gitmişken gün batımında Kordon'da kısa bir yürüyüş yapmadan dönme. Özellikle akşam saatlerinde manzara çok keyifli oluyor.",
            localDish = "Sabah fırından yeni çıkmış çıtır bir boyoz ve yanına haşlanmış yumurta İzmir klasiğidir, mutlaka denemelisin.",
            localAdvice = "Kemeraltı Çarşısı'nın tarihi dokusunda kaybolup Kızlarağası Hanı'nda kumda kahve keyfi yap.",
            cityTip = "Vaktin olursa Alaçatı'nın dar sokaklarında kaybolmayı veya Pasaport'ta çay keyfi yapmayı unutma.",
            friendGreeting = "Ege'nin incisi İzmir, o rahat ve güler yüzlü havasıyla seni karşılamaya hazır."
        ),
        "antalya" to CityDetails(
            description = "Güneşin, tarihin ve masmavi denizin buluşma noktası",
            visitPlace = "Kaleiçi'nin o daracık, tarih kokan sokaklarını ve Hadrian Kapısı'nı keşfetmelisin. Her köşe ayrı bir hikaye.",
            localDish = "Yöreye özgü tahinli piyaz ve şiş köfte ikilisini denemeni öneririm, Antalya'nın imza lezzetidir.",
            localAdvice = "Konyaaltı sahilinde yürüyüş yapıp falezlerin üzerinden denizi izlemek ruhuna iyi gelecek.",
            cityTip = "Düden Şelalesi'nin denize döküldüğü noktada güneşin batışını izlemek sana tüm yorgunluğunu unutturacak.",
            friendGreeting = "Akdeniz'in masmavi denizi ve iç ısıtan güneşi seyahatine eşlik edecek."
        ),
        "balikesir" to CityDetails(
            description = "Doğa ve denizin kucaklaştığı huzurlu rota",
            visitPlace = "Kaz Dağları'nın bol oksijenli havasını solumak veya Ayvalık Cunda'nın renkli sokaklarında gezmek harika bir seçim olur.",
            localDish = "Meşhur Balıkesir höşmerim tatlısını ve zeytinyağlıları denemeden ayrılma, tadı damağında kalacak.",
            localAdvice = "Ayvalık'ta gün batımını Şeytan Sofrası'nda izlemek bu seyahatin en unutulmaz anı olabilir.",
            cityTip = "Sabah saatlerinde Kaz Dağları'nın temiz havasını solumak oldukça keyifli bir deneyim olacaktır.",
            friendGreeting = "Balıkesir seyahatin için doğanın tüm renkleriyle iç içe bir rota planlayabilirsin."
        ),
        "mardin" to CityDetails(
            description = "Tarih ve kültürün taşa işlendiği masalsı şehir",
            visitPlace = "Eski Mardin sokaklarında adımlarken tarihin sesini duyacaksın. Zinciriye Medresesi'nin manzarası seni büyüleyecek.",
            localDish = "Kaburga dolmasını denemeden dönme, Mardin mutfağının zirve noktasıdır. Yanına bir de mırra kahvesi yakışır.",
            localAdvice = "Deyrulzafaran Manastırı'nın huzurlu bahçesinde kısa bir mola verip tarihi atmosferin tadını çıkar.",
            cityTip = "Eski Mardin sokaklarında kaybolurken fotoğraf çekmek için sabahın erken saatlerini tercih etmeni öneririm.",
            friendGreeting = "Mardin seyahatin için hava verisi sınırlı olsa bile planını rahatça oluşturabilirsin."
        ),
        "gaziantep" to CityDetails(
            description = "Gastronominin başkenti ve tarihin lezzet durağı",
            visitPlace = "Zeugma Mozaik Müzesi'ndeki o eşsiz eserleri görüp ardından Bakırcılar Çarşısı'nın ritmine kapılmalısın.",
            localDish = "Gaziantep'te tatlı için yer ayır. Özellikle iyi bir baklava ve katmer deneyimi seyahatin en unutulmaz kısmı olabilir.",
            localAdvice = "Sabah erkenden meşhur bir Beyran içerek güne Antep usulü enerjik bir başlangıç yap.",
            cityTip = "Sabah saatlerini müze ve çarşı için ayır; öğleden sonra yemek ve kısa yürüyüş planı daha rahat olur.",
            friendGreeting = "Gastronominin başkenti Gaziantep, lezzet dolu sokakları ve derin tarihiyle seni bekliyor."
        )
    )

    fun generateTravelAiSuggestion(
        city: String,
        tripType: TripType,
        forecastSnapshot: ForecastSnapshot?,
        previousSnapshot: ForecastSnapshot?,
        daysUntilTrip: Int,
        isPastTrip: Boolean = false,
        endDate: LocalDate? = null,
        tone: AssistantTone = AssistantTone.DENGELI,
        personalization: PersonalizationProfile? = null
    ): String {
        if (isPastTrip) {
            val dateStr = endDate?.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM")) ?: "geçtiğimiz günlerde"
            return when (tone) {
                AssistantTone.SAMIMI -> "HAVA ÖZETİ|Bu seyahatin $dateStr tarihinde bitti bile! Anıların arasında yerini aldı. [SEP] VALİZ TAVSİYESİ|Geçmiş seyahat için öneriye gerek yok canım. [SEP] MUTLAKA GÖR|Eski fotoğraflarına bakıp iç geçirebilirsin! [SEP] DENEMEDEN DÖNME|Bir sonraki rotan için iştahını sakla. [SEP] YEREL TAVSİYE|Yeni maceralarda görüşürüz!"
                AssistantTone.RESMI -> "HAVA ÖZETİ|Söz konusu seyahat $dateStr tarihinde tamamlanmıştır. Veriler arşivlenmiştir. [SEP] VALİZ TAVSİYESİ|Geçmiş dönem için tavsiye sunulmamaktadır. [SEP] MUTLAKA GÖR|Şehir hatıralarınızı inceleyebilirsiniz. [SEP] DENEMEDEN DÖNME|Gelecek rotalarınız için planlama yapabilirsiniz. [SEP] YEREL TAVSİYE|İyi yolculuklar dileriz."
                AssistantTone.KISA_NET -> "HAVA ÖZETİ|Tamamlandı ($dateStr). [SEP] VALİZ TAVSİYESİ|Gerekli değil. [SEP] MUTLAKA GÖR|Arşivleri incele. [SEP] DENEMEDEN DÖNME|Yeni plan yap. [SEP] YEREL TAVSİYE|Kayıtlı."
                else -> "HAVA ÖZETİ|Bu seyahat $dateStr tarihinde tamamlandı. Geçmiş rotaların arasında güvenle saklanıyor. [SEP] VALİZ TAVSİYESİ|Geçmiş seyahat verisi için valiz önerisi sunulmuyor. [SEP] MUTLAKA GÖR|Şehirdeki anılarını tazelemek için eski fotoğraflarına göz atabilirsin. [SEP] DENEMEDEN DÖNME|Bir sonraki rotan için yerel lezzet duraklarını şimdiden keşfetmeye başla. [SEP] YEREL TAVSİYE|Seyahat geçmişin, gelecek planların için harika bir rehber olacak."
            }
        }

        // Kişiselleştirme Katmanı (Personalization Layer)
        val interests = personalization?.selectedInterests ?: emptyList()
        val prefs = personalization?.weatherPreferences ?: WeatherPreferences()
        val isPersonalizationActive = personalization?.personalizationEnabled ?: true

        val windowDays = TRIP_ANALYSIS_WINDOW_DAYS // Use the constant (now 15)

        if (daysUntilTrip > windowDays) {
            return when (tone) {
                AssistantTone.SAMIMI -> "HAVA ÖZETİ|Daha vakit var! $windowDays gün kala tüm detayları önüne sereceğim. [SEP] VALİZ TAVSİYESİ|Henüz erken, valizi sonraya sakla. [SEP] MUTLAKA GÖR|O tarihlerde en popüler yerleri fısıldayacağım. [SEP] DENEMEDEN DÖNME|En taze lezzetleri o zaman seçeriz. [SEP] YEREL TAVSİYE|Şimdilik sadece heyecanını yaşa!"
                AssistantTone.RESMI -> "HAVA ÖZETİ|Hava durumu analizi seyahate $windowDays gün kala sisteme yansıtılacaktır. [SEP] VALİZ TAVSİYESİ|Tahminlerin kesinleşmesi beklenmektedir. [SEP] MUTLAKA GÖR|Popüler mekan listesi ilgili tarihlerde sunulacaktır. [SEP] DENEMEDEN DÖNME|Mevsimlik öneriler paylaşılacaktır. [SEP] YEREL TAVSİYE|Hazırlık sürecini takip ediniz."
                AssistantTone.KISA_NET -> "HAVA ÖZETİ|$windowDays gün kala aktifleşecek. [SEP] VALİZ TAVSİYESİ|Bekleyiniz. [SEP] MUTLAKA GÖR|Yakında. [SEP] DENEMEDEN DÖNME|Yakında. [SEP] YEREL TAVSİYE|Takipte kal."
                else -> "HAVA ÖZETİ|Hava durumu verileri seyahatinize $windowDays gün kala analiz edilecektir. [SEP] VALİZ TAVSİYESİ|Tahminler netleştiğinde en uygun kıyafet önerilerini burada bulacaksın. [SEP] MUTLAKA GÖR|Gideceğin şehir için en popüler mekanları o tarihlerde senin için listeleyeceğiz. [SEP] DENEMEDEN DÖNME|Yöresel lezzetlerin en tazelerini mevsime göre önereceğiz. [SEP] YEREL TAVSİYE|Şu an için sadece rotanın heyecanını yaşa, detayları bize bırak."
            }
        }

        val normalizedCity = city.lowercase(Locale("tr")).trim()
            .replace('ç', 'c').replace('ğ', 'g').replace('ı', 'i')
            .replace('ö', 'o').replace('ş', 's').replace('ü', 'u')

        val cityInfo = CITY_DATA.entries.find { normalizedCity.contains(it.key) }?.value ?: CityDetails(
            description = "Keşfedilmeyi bekleyen bir durak",
            visitPlace = "$city sokaklarını ve yerel parklarını keşfetmek her zaman iyi bir fikirdir. Şehrin ruhunu hissetmeye çalış.",
            localDish = "Bu şehre özel en meşhur yerel yemeği denemeden dönme, lezzet duraklarını yerlilere sormayı unutma.",
            localAdvice = "Merkezden biraz uzaklaşıp yerel halkın vakit geçirdiği kafelerde bir çay molası ver.",
            cityTip = "Plansız bir şekilde yürümek bazen en güzel keşiflerin kapısını açar, şehrin sesini dinle.",
            friendGreeting = "$city seyahatin için her şey hazır, keşfetmenin heyecanını yaşamaya başla!"
        )

        var weatherBlock = ""
        if (forecastSnapshot != null) {
            val precip = forecastSnapshot.precipitationProbability ?: 0
            val maxTemp = forecastSnapshot.maxTemp?.toInt() ?: 20
            val windSpeed = forecastSnapshot.windSpeed ?: 0.0
            val code = forecastSnapshot.weatherCode ?: 0
            val cond = (forecastSnapshot.conditionSummary ?: "Açık").lowercase(Locale("tr"))

            val formattedPrecip = WeatherUtils.formatRainProbability(precip)
            val windText = WeatherUtils.formatWindWithLevel(windSpeed)

            weatherBlock = when (tone) {
                AssistantTone.SAMIMI -> {
                    when {
                        code >= 95 -> "Hava biraz huysuz, fırtına çıkabilir! Rüzgar $windText civarında, dışarı çıkarken iki kere düşün canım."
                        code >= 80 -> "Sağanak yağmur geliyor, ıslanmaya hazır ol! Şemsiyeni sakın unutma tatlım."
                        precip > 60 -> "Gökyüzü biraz ağlamaklı, $formattedPrecip yağmur ihtimali var. Hazırlıklı ol!"
                        maxTemp > 30 -> "Mis gibi yaz havası! $maxTemp° ile güneşin tadını çıkarabilirsin, tam gezmelik."
                        maxTemp < 10 -> "Hava biraz sert, buz gibi olacak. Sıkı giyinmeyi unutma!"
                        else -> "Tam gezmelik harika bir hava! $maxTemp° derece ve $cond gökyüzü seni bekliyor."
                    }
                }
                AssistantTone.RESMI -> {
                    when {
                        code >= 95 -> "İlgili tarihlerde fırtına riski ve $windText hızında rüzgar öngörülmektedir. Tedbirli olunması tavsiye edilir."
                        code >= 80 -> "Kuvvetli sağanak yağış beklenmektedir. Olumsuz hava şartlarına karşı hazırlıklı olunmalıdır."
                        precip > 60 -> "Hava durumunun yağışlı geçeceği (%$precip) tahmin edilmektedir. Şemsiye bulundurulması önerilir."
                        maxTemp > 30 -> "Sıcaklık değerlerinin $maxTemp° seviyelerinde seyredeceği ve açık bir gökyüzü beklendiği bildirilmiştir."
                        maxTemp < 10 -> "Düşük sıcaklık değerleri ($maxTemp°) ve serin hava koşulları hakim olacaktır."
                        else -> "Meteorolojik koşullar seyahat için elverişli olup, sıcaklık $maxTemp° civarında seyrecektir."
                    }
                }
                AssistantTone.KISA_NET -> {
                    when {
                        code >= 95 -> "Fırtına riski, rüzgar $windText. Dikkatli ol."
                        code >= 80 -> "Sağanak yağışlı. Şemsiye al."
                        precip > 60 -> "Yağmurlu (%$precip). Önlem al."
                        maxTemp > 30 -> "Sıcak ve açık ($maxTemp°)."
                        maxTemp < 10 -> "Soğuk ($maxTemp°). Kalın giyin."
                        else -> "Hava uygun ($maxTemp°, $cond)."
                    }
                }
                AssistantTone.DETAYLI_UZMAN -> {
                    when {
                        code >= 95 -> "Troposferik instabilite: Fırtına olasılığı mevcuttur. Rüzgar vektörü $windText seviyesindedir."
                        code >= 80 -> "Presipitasyon zirvesi: Kuvvetli sağanak geçişleri öngörülmektedir. Termal konfor düşük."
                        precip > 60 -> "Atmosferik nem doygunluğu: $formattedPrecip olasılıkla yağış beklenmektedir. Görüş mesafesi etkilenebilir."
                        maxTemp > 30 -> "Termal anomali: $maxTemp° sıcaklık ve yüksek UV radyasyonu mevcuttur. Hidrasyon kritiktir."
                        maxTemp < 10 -> "Hipotermi riski: Sıcaklık $maxTemp°C civarındadır. İzolasyon sağlayan katmanlar önerilir."
                        else -> "Meteorolojik stabilite: Hava $maxTemp°C ve $cond durumdadır. Aktivite parametreleri için ideal."
                    }
                }
                else -> { // DENGELİ
                    when {
                        code >= 95 -> "Hava fırtınalı görünüyor, yağış riski yüksek. Rüzgar $windText hızına ulaşabilir. Yaklaşık $maxTemp° civarında olacak, tedbirli olmalısın."
                        code >= 80 -> "Hava sağanak yağışlı görünüyor, yağış riski yüksek. Yaklaşık $maxTemp° civarında olacak, şemsiyeni sakın unutma."
                        precip > 60 -> "Gideceğin tarihlerde gökyüzü biraz ağlamaklı görünüyor, $formattedPrecip yağmur ihtimali var. Hava yaklaşık $maxTemp° civarında olacak, şemsiyeni sakın unutma."
                        maxTemp > 30 -> "$maxTemp° ile güneşer cömert olduğu, pırıl pırıl bir gökyüzü seni bekliyor. Tam bir yaz havası var, yağmur riski ise yok denecek kadar az."
                        maxTemp < 10 -> "Hava biraz sert ve serin olacak, termometreler $maxTemp° civarında gezecek. Güneş yüzünü pek göstermeyebilir, kalın bir şeyler almanı öneririm."
                        windSpeed > 30 -> "Hava oldukça rüzgarlı ($windText) olacak. $maxTemp° sıcaklıkta bile esinti üşütebilir, rüzgar kesici bir şeyler almanı öneririm."
                        else -> "Tam gezmelik, harika bir hava seni bekliyor! $maxTemp° derece ve $cond gökyüzü seyahatine ayrı bir keyif katacak. Yağmur ihtimali $formattedPrecip."
                    }
                }
            }

            // Kişiselleştirilmiş Ek Bilgi (Personalized Extra Info)
            if (isPersonalizationActive) {
                if (interests.any { it.contains("kamp", true) || it.contains("outdoor", true) } && (precip > 30 || windSpeed > 25)) {
                    weatherBlock += " 🏕️ Kamp için zemin ıslaklığına ve rüzgar şiddetine dikkat etmeni öneririm."
                }
                if (interests.any { it.contains("bisiklet", true) || it.contains("kis_surusu", true) } && windSpeed > 20) {
                    weatherBlock += " 🚲 Rüzgar hızı sürüşü biraz zorlaştırabilir."
                }
                if (prefs.uvSensitive || interests.contains("uv_hassasiyeti")) {
                    if ((forecastSnapshot.uvIndex ?: 0.0) > 5) {
                        weatherBlock += " 🧴 UV seviyesi yüksek, cildini korumayı unutma."
                    }
                }
                if (interests.contains("cocuklar_icin") && maxTemp < 15) {
                    weatherBlock += " 👶 Minik gezginler için yedek katmanlar almayı unutma."
                }
            }
        } else {
            weatherBlock = when (tone) {
                AssistantTone.SAMIMI -> "Şu an tam veriye bakamadım ama mevsime göre hazırlan canım. Keyifli geçeceğine eminim!"
                AssistantTone.RESMI -> "Veri erişimi kısıtlılığı nedeniyle mevsim normallerine göre planlama yapılması önerilmektedir."
                AssistantTone.KISA_NET -> "Veri yok. Mevsimlik plan yap."
                else -> "Şu an hava servisine ulaşılamadı ama mevsim normallerine göre plan yapabilirsin. Genel olarak seyahat için elverişli ve güzel bir dönem."
            }
        }

        var packingBlock = ""
        if (forecastSnapshot != null) {
            val precip = forecastSnapshot.precipitationProbability ?: 0
            val maxTemp = forecastSnapshot.maxTemp?.toInt() ?: 20
            val minTemp = forecastSnapshot.minTemp?.toInt() ?: 15

            val basePacking = when {
                precip > 50 -> "Hava yağışlı geçeceği için şık bir yağmurluk ve su geçirmeyen bir ayakkabı hayat kurtarır. Valizine mutlaka sağlam bir şemsiye eklemelisin."
                maxTemp > 28 -> "Hava oldukça sıcak geçecek; pamuklu ve ferah kıyafetler seçmeni öneririm. Güneş gözlüğün ve yüksek koruyucu kremin olmazsa olmazın."
                maxTemp < 12 -> "Hava sert; kalın bir mont, atkı ve bere ikilisini valizine eklemelisin. Kat kat giyinmek seni gün boyu soğuktan koruyacaktır."
                else -> "Hafif bir ceket veya sweatshirt yanına almak mantıklı olur. Akşam serinliği için tedarikli olmakta fayda var."
            }

            packingBlock = basePacking

            // Kişiselleştirilmiş Valiz Önerisi
            if (isPersonalizationActive) {
                if (interests.any { it.contains("kamp", true) }) packingBlock += " ⛺ Uyku tulumu ve matını kontrol etmeyi unutma."
                if (interests.any { it.contains("koşu", true) }) packingBlock += " 🏃 En rahat koşu ayakkabılarını yanına almayı ihmal etme."
                if (interests.any { it.contains("fotoğraf", true) }) packingBlock += " 📷 Yedek piller ve lens temizleme kiti hayat kurtarabilir."
                if (maxTemp - minTemp > 10) packingBlock += " 🧥 Gün içindeki sıcaklık farkı fazla, hırka almanı öneririm."
            }
        } else {
            packingBlock = "Rahat bir yürüyüş ayakkabısı ve her ihtimale karşı akşamları serinleyebilecek havaya karşı hafif bir ceket valizinin olmazsa olmazı."
        }

        val tripAdvice = when(tripType) {
            TripType.BUSINESS -> "İş görüşmeleri arasında kısa bir yürüyüş yapmak enerjini tazeler. Zihnini boşaltmak için yerel bir kahve durağına uğra."
            TripType.VACATION -> "Telefonunu bir kenara bırakıp anın tadını çıkar. Bazen sadece izlemek and dinlemek en güzel hatıradır."
            else -> cityInfo.cityTip
        }

        return "HAVA ÖZETİ|$weatherBlock [SEP] VALİZ TAVSİYESİ|$packingBlock [SEP] MUTLAKA GÖR|${cityInfo.visitPlace} [SEP] DENEMEDEN DÖNME|${cityInfo.localDish} [SEP] YEREL TAVSİYE|${cityInfo.localAdvice}"
    }

    fun getCityDescription(city: String): String {
        val normalizedCity = city.lowercase(Locale("tr")).trim()
            .replace('ç', 'c').replace('ğ', 'g').replace('ı', 'i')
            .replace('ö', 'o').replace('ş', 's').replace('ü', 'u')
        return CITY_DATA.entries.find { normalizedCity.contains(it.key) }?.value?.description ?: "Keşfedilmeyi bekleyen harika bir rota"
    }

    fun generateComparisonText(old: ForecastSnapshot, new: ForecastSnapshot, tone: AssistantTone = AssistantTone.DENGELI): String {
        val changes = mutableListOf<String>()

        // Helper to format based on tone
        fun formatDiff(text: String, samimi: String, resmi: String, kisa: String, uzman: String): String {
            return when(tone) {
                AssistantTone.SAMIMI -> samimi
                AssistantTone.RESMI -> resmi
                AssistantTone.KISA_NET -> kisa
                AssistantTone.DETAYLI_UZMAN -> uzman
                else -> text
            }
        }

        // 1. Skor Karşılaştırması
        if (old.travelScore != null && new.travelScore != null) {
            val scoreDiff = new.travelScore - old.travelScore
            if (Math.abs(scoreDiff) >= 5) {
                if (scoreDiff > 0) {
                    changes.add(formatDiff(
                        "Seyahat skoru iyileşiyor.",
                        "Seyahat şansın artıyor canım, koşullar daha iyi!",
                        "Seyahat uygunluk katsayısı pozitif yönde güncellenmiştir.",
                        "Skor arttı.",
                        "Termal stabilite indeksi %${old.travelScore}'den %${new.travelScore}'e yükselerek seyahat optimizasyonunu artırmıştır."
                    ))
                } else {
                    changes.add(formatDiff(
                        "Seyahat skoru düşüyor.",
                        "Hava biraz bozuyor sanki, planlarına dikkat et tatlım.",
                        "Meteorolojik riskler nedeniyle seyahat skoru düşürülmüştür.",
                        "Skor düştü.",
                        "Atmosferik perturbasyonlar nedeniyle seyahat skoru %${new.travelScore}'e gerilemiştir."
                    ))
                }
            }
        }

        // 2. Yağış Karşılaştırması
        val oldPrecip = old.precipitationProbability ?: 0
        val newPrecip = new.precipitationProbability ?: 0
        if (Math.abs(newPrecip - oldPrecip) >= 10) {
            if (newPrecip > oldPrecip) {
                changes.add(formatDiff(
                    "Yağmur riski arttı.",
                    "Daha çok yağmur yağacak gibi, şemsiyeni hazırla canım.",
                    "Yağış olasılığı artış göstermiştir.",
                    "Yağış arttı.",
                    "Presipitasyon gradyanı %$oldPrecip'ten %$newPrecip'e yükselmiştir."
                ))
            } else {
                changes.add(formatDiff(
                    "Yağış ihtimali azaldı.",
                    "Hava açıyor canım, yağmur ihtimali düştü!",
                    "Yağış beklentisi minimize edilmiştir.",
                    "Yağış azaldı.",
                    "Bulut kapalılık oranı ve presipitasyon riski azalma eğilimindedir."
                ))
            }
        }

        return when {
            changes.isEmpty() -> formatDiff("Büyük bir değişiklik yok.", "Hava aynı canım, sürpriz yok.", "Hava koşulları stabil seyretmektedir.", "Değişim yok.", "Meteorolojik parametrelerde anlamlı bir sapma gözlemlenmemiştir.")
            changes.size == 1 -> changes.first()
            else -> changes.joinToString(" ")
        }
    }

    fun generateHistorySummary(plan: TravelPlan, tone: AssistantTone = AssistantTone.DENGELI): TravelHistorySummary {
        val duration = java.time.temporal.ChronoUnit.DAYS.between(plan.startDate, plan.endDate).toInt() + 1

        // Use the actual historical analyses list if available
        // PROOF: This comes from the 'analyses' field of the TravelPlan which is persisted in Room/Firestore.
        val historicalData = plan.analyses
        val hasData = historicalData.isNotEmpty() && historicalData.any { it.summary.isNotBlank() }

        if (!hasData) {
            return TravelHistorySummary(
                averageTemp = null,
                minTemp = null,
                maxTemp = null,
                rainyDays = -1,
                sunnyDays = -1,
                cloudyDays = -1,
                riskDayText = "Hava verisi arşivde bulunamadı.",
                comfortScore = 0,
                summaryText = "Bu seyahat için kayıtlı hava analizi bulunmuyor.",
                packingAdvice = "Geçmiş seyahat için öneri sunulmuyor.",
                nextTripAdvice = "Gelecek planların için ana ekrandaki tahminleri takip edebilirsin.",
                durationDays = duration
            )
        }

        // Derive summary from the LATEST recorded analysis for this trip in retrospective past tense
        val latestRecorded = historicalData.last()
        val avgT = latestRecorded.averageTemperature.toInt()
        val avgScore = latestRecorded.travelScore
        val rainRisk = latestRecorded.rainRiskPercent ?: 0
        val uvRisk = latestRecorded.uvRiskPercent ?: 0

        val weatherDesc = when {
            rainRisk > 40 -> "yağışlı ve bulutlu"
            avgT > 28 -> "sıcak ve güneşli"
            avgT < 10 -> "serin ve soğuk"
            else -> "ılıman ve elverişli"
        }

        val rainText = if (rainRisk > 20) "Yağmur ihtimali %$rainRisk seviyesindeydi." else "Yağmur ihtimali oldukça düşüktü."
        val uvText = if (uvRisk > 50) "UV seviyesi yüksekti." else "UV seviyesi normal seyretti."

        val summaryText = when(tone) {
            AssistantTone.SAMIMI -> "${plan.city} seyahatin $duration gün sürdü canım. Seyahatin boyunca hava genel olarak $weatherDesc idi ve sıcaklık ortalama ${avgT}°C civarındaydı. $rainText $uvText"
            AssistantTone.RESMI -> "${plan.city} seyahati $duration günlük periyotta gerçekleşmiştir. Bu süreçte hava koşulları $weatherDesc olmuş, sıcaklık ${avgT}°C olarak ölçülmüştür. $rainText $uvText"
            else -> "${plan.city} seyahatin $duration gün sürdü. Seyahatin boyunca hava genel olarak $weatherDesc idi ve sıcaklık ortalama ${avgT}°C civarındaydı. $rainText $uvText"
        }

        return TravelHistorySummary(
            averageTemp = avgT,
            minTemp = historicalData.map { it.averageTemperature }.minOrNull()?.toInt() ?: avgT,
            maxTemp = historicalData.map { it.averageTemperature }.maxOrNull()?.toInt() ?: avgT,
            rainyDays = historicalData.count { (it.rainRiskPercent ?: 0) > 50 },
            sunnyDays = historicalData.count { (it.rainRiskPercent ?: 100) < 20 },
            cloudyDays = -1,
            riskDayText = "Arşivlenen analiz verileri kullanılıyor.",
            comfortScore = avgScore,
            summaryText = summaryText,
            packingAdvice = "Kaydedilen ortalama sıcaklık: $avgT°C",
            nextTripAdvice = "Seyahat geçmişin, gelecek planların için bir rehber niteliğindedir.",
            durationDays = duration
        )
    }
}
