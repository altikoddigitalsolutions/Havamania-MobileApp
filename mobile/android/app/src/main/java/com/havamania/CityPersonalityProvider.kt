package com.havamania

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

data class CityPersonality(
    val slogan: String,
    val description: String,
    val mustSee: List<String>,
    val food: List<String>,
    val tip: String,
    val highlightsByTripType: Map<TripType, List<String>> = emptyMap()
)

object CityPersonalityProvider {
    private val personalityMap = mapOf(
        "İstanbul" to CityPersonality(
            slogan = "İki kıtanın kucaklaştığı efsanevi şehir",
            description = "Dünyanın merkezi, tarihin ve modernizmin eşsiz sentezi.",
            mustSee = listOf("Ayasofya", "Galata Kulesi", "Kız Kulesi"),
            food = listOf("Simit", "Balık Ekmek", "Kanlıca Yoğurdu"),
            tip = "Vapur sefası yapmadan ve martılara simit atmadan dönmeyin.",
            highlightsByTripType = mapOf(
                TripType.GASTRONOMY to listOf("Karaköy Güllüoğlu Baklava", "Tarihi Eminönü Balık Ekmek", "Kadıköy Çarşısı Lezzetleri"),
                TripType.CULTURE to listOf("Topkapı Sarayı", "İstanbul Arkeoloji Müzeleri", "Yerebatan Sarnıcı"),
                TripType.NATURE to listOf("Emirgan Korusu", "Belgrad Ormanı", "Atatürk Arboretumu")
            )
        ),
        "Ankara" to CityPersonality(
            slogan = "Cumhuriyetin kalbi ve bozkırın modern yüzü",
            description = "Düzenli caddeleri, parkları ve derin tarihi mirasıyla Türkiye'nin idari merkezi.",
            mustSee = listOf("Anıtkabir", "Ankara Kalesi", "Erimtan Müzesi"),
            food = listOf("Ankara Simidi", "Ankara Tavası", "Beypazarı Kurusu"),
            tip = "Kuğulu Park'ta mola verin ve Tunalı Hilmi caddesinde yürüyüş yapın.",
            highlightsByTripType = mapOf(
                TripType.CULTURE to listOf("Anadolu Medeniyetleri Müzesi", "I. Meclis Binası", "Roma Hamamı"),
                TripType.GASTRONOMY to listOf("Beypazarı Güveci", "Ankara Tavası", "Aspava Dürüm"),
                TripType.NATURE to listOf("Eymir Gölü", "Mogan Parkı", "Soğuksu Milli Parkı")
            )
        ),
        "İzmir" to CityPersonality(
            slogan = "Ege'nin incisi ve özgürlüğün şehri",
            description = "Mavi denizi, palmiyeli kordonu ve sıcakkanlı insanlarıyla Akdeniz ruhu.",
            mustSee = listOf("Saat Kulesi", "Efes Antik Kenti", "Kemeraltı Çarşısı"),
            food = listOf("Boyoz", "Kumru", "İzmir Bombası"),
            tip = "Kordon'da gün batımını izlemek İzmir seyahatinin olmazsa olmazıdır.",
            highlightsByTripType = mapOf(
                TripType.GASTRONOMY to listOf("Tarihi Havagazı Fabrikası", "Alsancak Dostlar Fırını", "Çeşme Kumrusu"),
                TripType.CULTURE to listOf("Efes Antik Kenti", "Şirince Köyü", "Agora Açık Hava Müzesi"),
                TripType.BEACH to listOf("Ilıca Plajı", "Altınkum", "Alaçatı Koyları")
            )
        ),
        "Antalya" to CityPersonality(
            slogan = "Turizmin başkenti ve güneşin evi",
            description = "Turkuaz sular, antik limanlar ve bitmeyen yaz enerjisi.",
            mustSee = listOf("Kaleiçi", "Düden Şelalesi", "Aspendos"),
            food = listOf("Piyaz", "Turunç Reçeli", "Şiş Köfte"),
            tip = "Kaleiçi'nin dar sokaklarında kaybolun ve falezlerden denizi izleyin.",
            highlightsByTripType = mapOf(
                TripType.BEACH to listOf("Konyaaltı Sahili", "Lara Plajı", "Kaputaş Plajı"),
                TripType.CULTURE to listOf("Perge Antik Kenti", "Termessos", "Side Antik Tiyatrosu"),
                TripType.NATURE to listOf("Köprülü Kanyon", "Göynük Kanyonu", "Kurşunlu Şelalesi")
            )
        ),
        "Bursa" to CityPersonality(
            slogan = "Yeşil Bursa ve Osmanlı'nın ilk payitahtı",
            description = "Uludağ'ın eteklerinde tarih, doğa ve lezzetin buluşma noktası.",
            mustSee = listOf("Ulu Cami", "Cumalıkızık", "Tophane"),
            food = listOf("İskender Kebap", "Kestane Şekeri", "Pideli Köfte"),
            tip = "Teleferik ile Uludağ'a çıkarken şehrin kuş bakışı manzarasını kaçırmayın.",
            highlightsByTripType = mapOf(
                TripType.GASTRONOMY to listOf("Tarihi Kayhan Çarşısı Pidecileri", "Kestane Şekeri Dükkanları", "İskender Kebapçısı"),
                TripType.WINTER to listOf("Uludağ Kayak Merkezi", "Keltepe Yolu"),
                TripType.CULTURE to listOf("Yeşil Türbe", "Emir Sultan", "Cumalıkızık Köyü")
            )
        ),
        "Trabzon" to CityPersonality(
            slogan = "Karadeniz'in hırçın dalgaları ve yeşil örtüsü",
            description = "Doğanın her tonu, yaylaların serinliği ve zengin mutfak kültürü.",
            mustSee = listOf("Sümela Manastırı", "Uzungöl", "Atatürk Köşkü"),
            food = listOf("Akçaabat Köftesi", "Kuymak", "Hamsili Pilav"),
            tip = "Yayla havası almadan ve gerçek bir Karadeniz çayı içmeden dönmeyin.",
            highlightsByTripType = mapOf(
                TripType.NATURE to listOf("Uzungöl", "Hıdırnebi Yaylası", "Sultan Murat Yaylası"),
                TripType.CULTURE to listOf("Sümela Manastırı", "Ayasofya Müzesi", "Trabzon Kalesi"),
                TripType.GASTRONOMY to listOf("Bordo Mavi Balıkçısı", "Akçaabat Köftecileri", "Sütlaç (Hamsiköy)")
            )
        ),
        "Rize" to CityPersonality(
            slogan = "Yeşilin ve suyun buluştuğu Kaçkarlar diyarı",
            description = "Bulut denizleri, coşkun dere ve şelaleleriyle Doğu Karadeniz'in cenneti.",
            mustSee = listOf("Ayder Yaylası", "Fırtına Deresi", "Zilkale"),
            food = listOf("Rize Kavurması", "Muhlama", "Laz Böreği"),
            tip = "Fırtına Deresi'nde rafting yapmadan ve Ayder'de kaplıcalara girmeden dönmeyin.",
            highlightsByTripType = mapOf(
                TripType.NATURE to listOf("Ayder Yaylası", "Gito Yaylası", "Palovit Şelalesi"),
                TripType.GASTRONOMY to listOf("Rize Bezi Atölyeleri", "Meşhur Rize Çayı", "Hamsili Ekmek"),
                TripType.ADVENTURE to listOf("Fırtına Deresi Zipline", "Kaçkar Dağları Tırmanışı")
            )
        ),
        "Çankırı" to CityPersonality(
            slogan = "Tarihin ve kaya tuzunun buluştuğu diyet kenti",
            description = "Hititlerden kalma kaleleri, şifalı tuz mağaraları ve samimi dokusuyla İç Anadolu.",
            mustSee = listOf("Çankırı Tuz Mağarası", "Çankırı Kalesi", "Taş Mescit"),
            food = listOf("Yumurta Tatlısı", "Çankırı Tavası", "Cızlama"),
            tip = "Dünyanın ikinci büyük tuz mağarası olan Çankırı Tuz Mağarası'nı mutlaka ziyaret edin."
        ),
        "Mardin" to CityPersonality(
            slogan = "Taşın dile geldiği, zamanın durduğu yer",
            description = "Mezopotamya ovasına bakan tarihi dokusu ve dar sokaklarıyla bir açık hava müzesi.",
            mustSee = listOf("Eski Mardin", "Dara Antik Kenti", "Deyrulzafaran Manastırı"),
            food = listOf("İçli Köfte", "Sembusek", "Mardin Çöreği"),
            tip = "Abbaralardan geçerek eski şehri yürüyerek keşfedin ve telkari gümüşlerini inceleyin.",
            highlightsByTripType = mapOf(
                TripType.CULTURE to listOf("Zinciriye Medresesi", "Kasımiye Medresesi", "Mardin Müzesi"),
                TripType.GASTRONOMY to listOf("Cercis Murat Konağı", "İncirli Köşk", "Tarihi Çaycılar")
            )
        )
    )

    private val defaultPersonality = CityPersonality(
        slogan = "Keşfedilmeyi bekleyen eşsiz bir durak",
        description = "Her köşesinde ayrı bir hikaye barındıran, sürprizlerle dolu bir rota.",
        mustSee = listOf("Şehir Merkezi", "Yerel Pazarlar", "Tarihi Yapılar"),
        food = listOf("Yerel Lezzetler", "Sokak Yemekleri"),
        tip = "Yerel halkla sohbet edin, en iyi tavsiyeler onlardadır."
    )

    fun getPersonality(cityName: String, tripType: TripType? = null): CityPersonality {
        val base = personalityMap[cityName] ?: personalityMap.entries.find {
            cityName.contains(it.key, ignoreCase = true)
        }?.value ?: defaultPersonality

        if (tripType == null || base.highlightsByTripType.isEmpty()) return base

        val customHighlights = base.highlightsByTripType[tripType] ?: return base
        return base.copy(
            mustSee = customHighlights
        )
    }
}
