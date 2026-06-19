package com.havamania

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
        endDate: LocalDate? = null
    ): String {
        if (isPastTrip) {
            val dateStr = endDate?.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM")) ?: "geçtiğimiz günlerde"
            return "HAVA ÖZETİ|Bu seyahat $dateStr tarihinde tamamlandı. Geçmiş rotaların arasında güvenle saklanıyor. [SEP] VALİZ TAVSİYESİ|Geçmiş seyahat verisi için valiz önerisi sunulmuyor. [SEP] MUTLAKA GÖR|Şehirdeki anılarını tazelemek için eski fotoğraflarına göz atabilirsin. [SEP] DENEMEDEN DÖNME|Bir sonraki rotan için yerel lezzet duraklarını şimdiden keşfetmeye başla. [SEP] YEREL TAVSİYE|Seyahat geçmişin, gelecek planların için harika bir rehber olacak."
        }

        if (daysUntilTrip > 15) {
            return "HAVA ÖZETİ|Hava durumu verileri seyahatinize 15 gün kala analiz edilecektir. [SEP] VALİZ TAVSİYESİ|Tahminler netleştiğinde en uygun kıyafet önerilerini burada bulacaksın. [SEP] MUTLAKA GÖR|Gideceğin şehir için en popüler mekanları o tarihlerde senin için listeleyeceğiz. [SEP] DENEMEDEN DÖNME|Yöresel lezzetlerin en tazelerini mevsime göre önereceğiz. [SEP] YEREL TAVSİYE|Şu an için sadece rotanın heyecanını yaşa, detayları bize bırak."
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

        val weatherBlock = if (forecastSnapshot != null) {
            val precip = forecastSnapshot.precipitationProbability ?: 0
            val maxTemp = forecastSnapshot.maxTemp?.toInt() ?: 20
            val code = forecastSnapshot.weatherCode ?: 0
            val cond = (forecastSnapshot.conditionSummary ?: "Açık").lowercase(Locale("tr"))

            val formattedPrecip = WeatherUtils.formatRainProbability(precip)

            when {
                code >= 95 -> "Hava fırtınalı görünüyor, yağış riski yüksek. Yaklaşık $maxTemp° civarında olacak, tedbirli olmalısın."
                code >= 80 -> "Hava sağanak yağışlı görünüyor, yağış riski yüksek. Yaklaşık $maxTemp° civarında olacak, şemsiyeni sakın unutma."
                precip > 60 -> "Gideceğin tarihlerde gökyüzü biraz ağlamaklı görünüyor, $formattedPrecip yağmur ihtimali var. Hava yaklaşık $maxTemp° civarında olacak, şemsiyeni sakın unutma."
                maxTemp > 30 -> "$maxTemp° ile güneşin cömert olduğu, pırıl pırıl bir gökyüzü seni bekliyor. Tam bir yaz havası var, yağmur riski ise yok denecek kadar az."
                maxTemp < 10 -> "Hava biraz sert ve serin olacak, termometreler $maxTemp° civarında gezecek. Güneş yüzünü pek göstermeyebilir, kalın bir şeyler almanı öneririm."
                else -> "Tam gezmelik, harika bir hava seni bekliyor! $maxTemp° derece ve $cond gökyüzü seyahatine ayrı bir keyif katacak. Yağmur ihtimali $formattedPrecip."
            }
        } else {
            "Şu an hava servisine ulaşılamadı ama mevsim normallerine göre plan yapabilirsin. Genel olarak seyahat için elverişli ve güzel bir dönem."
        }

        val packingBlock = if (forecastSnapshot != null) {
            val precip = forecastSnapshot.precipitationProbability ?: 0
            val maxTemp = forecastSnapshot.maxTemp?.toInt() ?: 20
            when {
                precip > 50 -> "Yağmura karşı şık bir yağmurluk and su geçirmeyen bir ayakkabı hayat kurtarır. Valizine mutlaka bir şemsiye eklemelisin."
                maxTemp > 28 -> "Pamuklu and ferah kıyafetler seçmeni öneririm. Güneş gözlüğün and koruyucu kremin bu seyahatin olmazsa olmazı."
                maxTemp < 12 -> "Kalın bir mont and atkı/bere ikilisini valizine eklemelisin. Kat kat giyinmek seni gün boyu soğuktan koruyacaktır."
                else -> "Hafif bir ceket veya sweatshirt yanına almak mantıklı olur. Akşam serinliği için tedarikli olmakta fayda var."
            }
        } else {
            "Rahat bir yürüyüş ayakkabısı and her ihtimale karşı akşamları serinleyebilecek havaya karşı hafif bir ceket valizinin olmazsa olmazı."
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

    fun generateComparisonText(old: ForecastSnapshot, new: ForecastSnapshot): String {
        val changes = mutableListOf<String>()

        // 1. Skor Karşılaştırması (Skor farkı >= 5 puan)
        if (old.travelScore != null && new.travelScore != null) {
            val scoreDiff = new.travelScore - old.travelScore
            if (Math.abs(scoreDiff) >= 5) {
                if (scoreDiff > 0) {
                    changes.add("Seyahat uygunluk skoru %${old.travelScore}'den %${new.travelScore}'e yükseldi, koşullar iyileşiyor.")
                } else {
                    changes.add("Seyahat uygunluk skoru %${old.travelScore}'den %${new.travelScore}'e düştü. Planlarını tekrar gözden geçirmek isteyebilirsin.")
                }
            }
        }

        // 2. Yağış Karşılaştırması (Yağış farkı >= %10)
        val oldPrecip = old.precipitationProbability ?: 0
        val newPrecip = new.precipitationProbability ?: 0
        if (Math.abs(newPrecip - oldPrecip) >= 10) {
            if (newPrecip > oldPrecip) {
                changes.add("Dünkü analizde yağış olasılığı %$oldPrecip görünüyordu, bugün %$newPrecip'e yükseldi. Yağmur riski artmış.")
            } else {
                changes.add("Yağış ihtimali %$oldPrecip'ten %$newPrecip'e geriledi, daha açık bir hava bekleniyor.")
            }
        }

        // 3. Sıcaklık Karşılaştırması (Sıcaklık farkı >= 2°C)
        if (old.maxTemp != null && new.maxTemp != null) {
            val tempDiff = new.maxTemp - old.maxTemp
            if (Math.abs(tempDiff) >= 2.0) {
                if (tempDiff > 0) {
                    changes.add("Önceki tahminde maksimum sıcaklık ${old.maxTemp.toInt()}°C idi, şimdi ${new.maxTemp.toInt()}°C. Daha sıcak bir gün bekleniyor.")
                } else {
                    changes.add("Maksimum sıcaklık ${old.maxTemp.toInt()}°C'den ${new.maxTemp.toInt()}°C'ye düştü, hava serinliyor.")
                }
            }
        }

        // 4. Rüzgar Karşılaştırması (Rüzgar farkı >= 8 km/s)
        if (old.windSpeed != null && new.windSpeed != null) {
            val windDiff = new.windSpeed - old.windSpeed
            if (Math.abs(windDiff) >= 8.0) {
                if (windDiff > 0) {
                    changes.add("Rüzgar beklentisi ${old.windSpeed.toInt()} km/s'den ${new.windSpeed.toInt()} km/s'ye çıktı, açık hava planları için dikkatli olunmalı.")
                } else {
                    changes.add("Rüzgar hızı azalıyor (${old.windSpeed.toInt()} → ${new.windSpeed.toInt()} km/s), daha sakin bir hava hakim olacak.")
                }
            }
        }

        // 5. Hissedilen Sıcaklık (Fark >= 2°C)
        if (old.feelsLike != null && new.feelsLike != null) {
            val feelsDiff = new.feelsLike - old.feelsLike
            if (Math.abs(feelsDiff) >= 2.0) {
                if (feelsDiff > 0) {
                    changes.add("Hissedilen sıcaklık artışta (${old.feelsLike.toInt()}°C → ${new.feelsLike.toInt()}°C).")
                } else {
                    changes.add("Hava daha serin hissedilecek (${old.feelsLike.toInt()}°C → ${new.feelsLike.toInt()}°C).")
                }
            }
        }

        return when {
            changes.isEmpty() -> "Genel hava koşulları önceki analize göre benzer seyrediyor, büyük bir değişiklik yok."
            changes.size == 1 -> changes.first()
            else -> "Önceki analize göre bazı değişiklikler var: " + changes.joinToString(" ")
        }
    }

    fun generateHistorySummary(plan: TravelPlan): TravelHistorySummary {
        val duration = java.time.temporal.ChronoUnit.DAYS.between(plan.startDate, plan.endDate).toInt() + 1
        val snapshot = plan.lastForecastSnapshot

        // Hava verisi varsa işle, yoksa null/default dön
        val hasData = snapshot != null
        val minT = snapshot?.minTemp?.toInt() ?: 0
        val maxT = snapshot?.maxTemp?.toInt() ?: 0
        val avgT = if (hasData) (minT + maxT) / 2 else null

        val cond = snapshot?.conditionSummary?.lowercase() ?: "bilinmiyor"
        val prob = snapshot?.precipitationProbability ?: 0

        val rainy = if (!hasData) 0 else if (cond.contains("yağmur") || prob > 50) (duration * 0.4).toInt().coerceAtLeast(1) else (duration * 0.1).toInt()
        val sunny = if (!hasData) 0 else if (cond.contains("güneş") || cond.contains("açık")) (duration * 0.6).toInt().coerceAtLeast(1) else (duration * 0.3).toInt()
        val cloudy = (duration - rainy - sunny).coerceAtLeast(0)

        val summaryText = if (hasData) {
            val weatherTone = when {
                rainy > (duration / 2) -> "Hava çoğunlukla yağışlı geçmiş"
                sunny > (duration * 0.7) -> "Pırıl pırıl, güneşli bir gökyüzü eşlik etmiş"
                avgT ?: 0 > 28 -> "Sıcak ve tam bir yaz havası hakimmiş"
                else -> "Hava genel olarak dengeli görünmüş"
            }
            val comfortNote = if (rainy == 0) "Yağışlı gün sayısı düşük olduğu için açık hava planları açısından rahat bir dönem olmuş." else "Yağışa rağmen seyahat temposu korunmuş gibi görünüyor."

            "${plan.city} seyahatin $duration gün sürdü. $weatherTone; sıcaklık çok bunaltıcı seviyeye çıkmamış. $comfortNote"
        } else {
            "${plan.city} seyahatin tamamlandı. Bu döneme ait detaylı hava verisi geçmiş kayıtlarda bulunamadı ancak seyahat notlarını aşağıda saklayabilirsin."
        }

        val packingAdvice = when {
            avgT ?: 0 > 25 -> "Benzer hava koşullarında hafif kıyafet, rahat ayakkabı ve güneş koruması yeterli olur."
            avgT ?: 0 < 12 -> "Kalın mont, termal içlik ve su geçirmeyen botlar bir sonraki sefer için hayat kurtarıcı olabilir."
            else -> "Kat kat giyinmek (sweatshirt + hafif ceket) değişken hava koşulları için en iyi strateji."
        }

        val destinationTip = when {
            plan.city.contains("Bali", true) -> "Bali gibi nemli bölgelerde sabah saatlerini açık hava planları için ayırmak daha rahat olabilir."
            plan.city.contains("İstanbul", true) -> "İstanbul seyahatlerinde vapur saatlerini gün batımına denk getirmek her zaman iyi bir fikirdir."
            else -> "${plan.city} seyahatinde yerel lezzet duraklarını keşfetmek için ara sokaklara dalmaktan çekinme."
        }

        return TravelHistorySummary(
            averageTemp = avgT,
            minTemp = minT,
            maxTemp = maxT,
            rainyDays = if (hasData) rainy else -1,
            sunnyDays = if (hasData) sunny else -1,
            cloudyDays = if (hasData) cloudy else -1,
            riskDayText = "Bu seyahat kısa ama yoğun bir rota gibi görünüyor.",
            comfortScore = if (hasData) (80 + (Math.random() * 15).toInt()) else 0,
            summaryText = summaryText,
            packingAdvice = packingAdvice,
            nextTripAdvice = destinationTip,
            durationDays = duration
        )
    }
}
