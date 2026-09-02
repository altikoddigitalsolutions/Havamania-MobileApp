package com.havamania

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

data class CityPersonality(
    val slogan: String,
    val description: String,
    val mustSee: List<String>,
    val food: List<String>,
    val tip: String
)

object CityPersonalityProvider {
    private val personalityMap = mapOf(
        "İstanbul" to CityPersonality(
            slogan = "İki kıtanın kucaklaştığı efsanevi şehir",
            description = "Dünyanın merkezi, tarihin ve modernizmin eşsiz sentezi.",
            mustSee = listOf("Ayasofya", "Galata Kulesi", "Kız Kulesi"),
            food = listOf("Simit", "Balık Ekmek", "Kanlıca Yoğurdu"),
            tip = "Vapur sefası yapmadan ve martılara simit atmadan dönmeyin."
        ),
        "Ankara" to CityPersonality(
            slogan = "Cumhuriyetin kalbi ve bozkırın modern yüzü",
            description = "Düzenli caddeleri, parkları ve derin tarihi mirasıyla Türkiye'nin idari merkezi.",
            mustSee = listOf("Anıtkabir", "Ankara Kalesi", "Erimtan Müzesi"),
            food = listOf("Ankara Simidi", "Ankara Tavası", "Beypazarı Kurusu"),
            tip = "Kuğulu Park'ta mola verin ve Tunalı Hilmi caddesinde yürüyüş yapın."
        ),
        "İzmir" to CityPersonality(
            slogan = "Ege'nin incisi ve özgürlüğün şehri",
            description = "Mavi denizi, palmiyeli kordonu ve sıcakkanlı insanlarıyla Akdeniz ruhu.",
            mustSee = listOf("Saat Kulesi", "Efes Antik Kenti", "Kemeraltı Çarşısı"),
            food = listOf("Boyoz", "Kumru", "İzmir Bombası"),
            tip = "Kordon'da gün batımını izlemek İzmir seyahatinin olmazsa olmazıdır."
        ),
        "Antalya" to CityPersonality(
            slogan = "Turizmin başkenti ve güneşin evi",
            description = "Turkuaz sular, antik limanlar ve bitmeyen yaz enerjisi.",
            mustSee = listOf("Kaleiçi", "Düden Şelalesi", "Aspendos"),
            food = listOf("Piyaz", "Turunç Reçeli", "Şiş Köfte"),
            tip = "Kaleiçi'nin dar sokaklarında kaybolun ve falezlerden denizi izleyin."
        ),
        "Bursa" to CityPersonality(
            slogan = "Yeşil Bursa ve Osmanlı'nın ilk payitahtı",
            description = "Uludağ'ın eteklerinde tarih, doğa ve lezzetin buluşma noktası.",
            mustSee = listOf("Ulu Cami", "Cumalıkızık", "Tophane"),
            food = listOf("İskender Kebap", "Kestane Şekeri", "Pideli Köfte"),
            tip = "Teleferik ile Uludağ'a çıkarken şehrin kuş bakışı manzarasını kaçırmayın."
        ),
        "Trabzon" to CityPersonality(
            slogan = "Karadeniz'in hırçın dalgaları ve yeşil örtüsü",
            description = "Doğanın her tonu, yaylaların serinliği ve zengin mutfak kültürü.",
            mustSee = listOf("Sümela Manastırı", "Uzungöl", "Atatürk Köşkü"),
            food = listOf("Akçaabat Köftesi", "Kuymak", "Hamsili Pilav"),
            tip = "Yayla havası almadan ve gerçek bir Karadeniz çayı içmeden dönmeyin."
        ),
        "Eskişehir" to CityPersonality(
            slogan = "Anadolu'nun Avrupa yüzlü modern şehri",
            description = "Porsuk çayı kenarında canlı sokakları, parkları ve müzeleriyle bir kültür kenti.",
            mustSee = listOf("Odunpazarı Evleri", "Sazova Parkı", "Balmumu Müzesi"),
            food = listOf("Çibörek", "Balaban Köfte", "Met Helvası"),
            tip = "Porsuk Çayı'nda gondol sefası yapmayı ve Odunpazarı'nda yürümeyi unutmayın."
        ),
        "Muğla" to CityPersonality(
            slogan = "Mavi ve yeşilin rüya gibi birleşimi",
            description = "Bodrum'dan Fethiye'ye uzanan eşsiz koyları ve antik kentleriyle Ege'nin kalbi.",
            mustSee = listOf("Ölüdeniz", "Bodrum Kalesi", "Kalyon Koyu"),
            food = listOf("Çökertme Kebabı", "Muğla Köftesi", "Kabak Çiçeği Dolması"),
            tip = "Dalyan'da tekne turuna çıkıp İztuzu plajında caretta carettaları selamlayın."
        ),
        "Konya" to CityPersonality(
            slogan = "Mevlana'nın diyarı ve hoşgörü şehri",
            description = "Selçuklu payitahtı, derin manevi mirası ve bozkırın ortasındaki medeniyet durağı.",
            mustSee = listOf("Mevlana Müzesi", "Karatay Medresesi", "Sille Köyü"),
            food = listOf("Etliekmek", "Fırın Kebabı", "Sac Arası"),
            tip = "Şeb-i Arus zamanı gitmeye çalışın veya Alaaddin Tepesi'nde mola verin."
        ),
        "Gaziantep" to CityPersonality(
            slogan = "Dünyanın lezzet başkenti",
            description = "Binlerce yıllık tarihi ve UNESCO tescilli mutfağıyla gastronomi cenneti.",
            mustSee = listOf("Zeugma Müzesi", "Antep Kalesi", "Bakırcılar Çarşısı"),
            food = listOf("Baklava", "Ali Nazik", "Beyran"),
            tip = "Sabah erkenden Beyran içmeyi ve fıstıklı baklavanın tadına bakmayı ihmal etmeyin."
        ),
        "Nevşehir" to CityPersonality(
            slogan = "Peri bacalarının ve balonların masalsı ülkesi",
            description = "Kapadokya'nın kalbi, yeraltı şehirleri ve eşsiz yer şekilleriyle büyüleyici bir rota.",
            mustSee = listOf("Göreme Açık Hava Müzesi", "Uçhisar Kalesi", "Ihlara Vadisi"),
            food = listOf("Testi Kebabı", "Kuru Fasulye", "Üzüm Pekmezi"),
            tip = "Gün doğumunda sıcak hava balonlarını izleyin veya vadi yürüyüşlerine katılın."
        ),
        "Mardin" to CityPersonality(
            slogan = "Taşın dile geldiği, zamanın durduğu yer",
            description = "Mezopotamya ovasına bakan tarihi dokusu ve dar sokaklarıyla bir açık hava müzesi.",
            mustSee = listOf("Eski Mardin", "Dara Antik Kenti", "Deyrulzafaran Manastırı"),
            food = listOf("İçli Köfte", "Sembusek", "Mardin Çöreği"),
            tip = "Abbaralardan geçerek eski şehri yürüyerek keşfedin ve telkari gümüşlerini inceleyin."
        ),
        "Çanakkale" to CityPersonality(
            slogan = "Destanların yazıldığı, boğazın bekçisi",
            description = "Tarihin akışını değiştiren cepheleri ve Troya efsanesiyle unutulmaz bir kent.",
            mustSee = listOf("Gelibolu Şehitliği", "Troya Antik Kenti", "Aynalı Çarşı"),
            food = listOf("Peynir Helvası", "Sardalya", "Lakerda"),
            tip = "Feribotla karşıya geçerken boğaz havasını içinize çekin ve şehitlikleri ziyaret edin."
        )
    )

    private val defaultPersonality = CityPersonality(
        slogan = "Keşfedilmeyi bekleyen eşsiz bir durak",
        description = "Her köşesinde ayrı bir hikaye barındıran, sürprizlerle dolu bir rota.",
        mustSee = listOf("Şehir Merkezi", "Yerel Pazarlar", "Tarihi Yapılar"),
        food = listOf("Yerel Lezzetler", "Sokak Yemekleri"),
        tip = "Yerel halkla sohbet edin, en iyi tavsiyeler onlardadır."
    )

    fun getPersonality(cityName: String): CityPersonality {
        return personalityMap[cityName] ?: personalityMap.entries.find {
            cityName.contains(it.key, ignoreCase = true)
        }?.value ?: defaultPersonality
    }
}
