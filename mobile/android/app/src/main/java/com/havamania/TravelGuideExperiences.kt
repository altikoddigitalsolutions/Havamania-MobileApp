package com.havamania

data class TravelGuideExperience(
    val title: String,
    val category: String,
    val description: String,
    val sourceUrl: String,
    val preferredTripTypes: Set<TripType>
)

internal object TravelGuideExperiences {
    private val entries: Map<Pair<String, String?>, List<TravelGuideExperience>> = mapOf(
        ("istanbul" to null) to listOf(
            TravelGuideExperience(
                "Bir akşamı AKM programına ayır",
                "Sahne sanatları",
                "Taksim'deki Atatürk Kültür Merkezi'nin programından seyahat gününe uyan bir konser, tiyatro veya sergi seç. Bileti almadan önce sahneyi ve başlangıç saatini kontrol et, akşam planını tek bir gösterinin etrafında kur.",
                "https://akmistanbul.gov.tr/tr/etkinlikler",
                setOf(TripType.EVENT, TripType.CULTURE, TripType.ROMANTIC)
            ),
            TravelGuideExperience(
                "Galata'yı üç farklı açıdan fotoğrafla",
                "Fotoğraf",
                "Galata Kulesi'ni önce sokağın içinden, sonra Karaköy'e inerken cephelerin arasından kadraja al. Galata Köprüsü'ne geçtiğinde aynı silüeti suyu da kullanarak tekrar çek. Üç kareyi karşılaştırarak kendi küçük İstanbul serini oluştur.",
                "https://goturkiye.com/tr/istanbul/beyoglu-kultur-yolu",
                setOf(TripType.PHOTOGRAPHY, TripType.WEEKEND)
            ),
            TravelGuideExperience(
                "Atlas Pasajı'nda sinema molası",
                "Farklı bir müze",
                "İstanbul Sinema Müzesi'nde Türk sinemasına ayrılan bölümleri gez. Dışarıdaki yürüyüşü kısa tutmak istediğin bir güne bu kapalı alan durağını ekleyebilirsin.",
                "https://goturkiye.com/tr/istanbul/beyoglu-kultur-yolu",
                setOf(TripType.CULTURE, TripType.BUSINESS)
            )
        ),
        ("ankara" to null) to listOf(
            TravelGuideExperience(
                "Müzeyi bir nesnenin hikâyesiyle gez",
                "Aileyle keşif",
                "Anadolu Medeniyetleri Müzesi'nde ilgini çeken bir kap, mühür veya takı seç. Yanındaki kişiyle önce ne amaçla kullanılmış olabileceğini konuş, sonra açıklamasını birlikte oku. Bütün salonları yetiştirmek yerine birkaç esere gerçekten zaman ayır.",
                "https://goturkiye.com/tr/ankara/gor",
                setOf(TripType.FAMILY, TripType.CULTURE)
            ),
            TravelGuideExperience(
                "Kale çevresinde kapı ve pencere rotası",
                "Fotoğraf",
                "Ankara Kalesi çevresindeki eski evleri gezerken taş, ahşap ve kapı ayrıntılarına odaklan. Şehir manzarasıyla bir ev ayrıntısını eşleştiren iki kare çek. Bu rotayı Ulus'taki müze gününe ekleyebilirsin.",
                "https://goturkiye.com/tr/ankara/gor",
                setOf(TripType.PHOTOGRAPHY, TripType.WEEKEND)
            )
        ),
        ("izmir" to null) to listOf(
            TravelGuideExperience(
                "Asansör'de iki seviyeden aynı şehre bak",
                "Manzara ve fotoğraf",
                "Dario Moreno Sokağı'ndan Tarihi Asansör'e ilerle. Önce sokak seviyesinde bir kare çek, üstte ise körfezi ve çatıları kadraja al. Aynı çevrenin aşağıdan ve yukarıdan ne kadar farklı göründüğünü karşılaştır.",
                "https://goturkiye.com/tr/izmir/see",
                setOf(TripType.PHOTOGRAPHY, TripType.ROMANTIC)
            ),
            TravelGuideExperience(
                "Kemeraltı'nda tek bir temanın peşine düş",
                "Çarşı keşfi",
                "Kemeraltı ve Havra Sokağı gezisinde kendine küçük bir tema seç. Baharat tezgâhları, han avluları veya eski tabelalar olabilir. Kızlarağası Hanı çevresindeki molada en çok dikkatini çeken üç ayrıntıyı not al.",
                "https://goturkiye.com/tr/faith/sefarad-izmir",
                setOf(TripType.SHOPPING, TripType.CULTURE, TripType.GASTRONOMY)
            )
        ),
        ("antalya" to null) to listOf(
            TravelGuideExperience(
                "Hadrian Kapısı'ndan limana kendi fotoğraf hikâyeni kur",
                "Kısa keşif",
                "Hadrian Kapısı, Kaleiçi evleri ve Yat Limanı için birer kare ayır. Böylece aynı gezide taş işçiliğini, sokak yaşamını ve denizi birbirine bağlayan üç duraklık bir hikâyen olur.",
                "https://goturkiye.com/tr/antalya/gor",
                setOf(TripType.PHOTOGRAPHY, TripType.CULTURE, TripType.WEEKEND)
            ),
            TravelGuideExperience(
                "Konyaaltı'nda yürüyüşün sonunu bir manzara molasına bağla",
                "Yavaş gezi",
                "Konyaaltı sahilinde kendi tempona uygun kısa bir bölüm seç. Yürüyüşün sonunda denizi izlemek için mola ver. Tarih ve sahil gezilerini aynı güne eklediysen sıcak saatlerde uzun yürüyüş yerine bu kısa planı kullanabilirsin.",
                "https://goturkiye.com/tr/antalya/gor",
                setOf(TripType.BEACH, TripType.ROMANTIC, TripType.NATURE)
            )
        ),
        ("balikesir" to null) to listOf(
            TravelGuideExperience(
                "Kuva-yi Milliye Müzesi'nde iki koleksiyonu karşılaştır",
                "Kültür keşfi",
                "Milli Mücadele bölümünü gördükten sonra etnografya koleksiyonuna geç. Önceki kuşakların gündelik yaşamından ilgini çeken bir eşya seç. Büyük tarih anlatısını küçük bir nesnenin hikâyesiyle tamamla.",
                "https://balikesir.ktb.gov.tr/TR-65921/muzeler-ve-oren-yerleri.html",
                setOf(TripType.CULTURE, TripType.FAMILY)
            ),
            TravelGuideExperience(
                "Höşmerim molasını küçük bir lezzet karşılaştırmasına çevir",
                "Yerel ürün",
                "Bir höşmerim porsiyonunu paylaşarak başla. Peynir tadının ve irmiğin dokusuna dikkat et. Eve götürmeyi düşünüyorsan üretim tarihini ve saklama koşullarını satıcıya sor.",
                "https://goturkiye.com/tr/balikesir/tat",
                setOf(TripType.GASTRONOMY, TripType.SHOPPING)
            )
        ),
        ("mardin" to null) to listOf(
            TravelGuideExperience(
                "Telkariye vitrinden bakmakla yetinme",
                "El sanatları",
                "Mardin çarşısında telkari çalışan bir ustaya rastlarsan üretimi izlemek için izin iste. İnce gümüş tellerin bir desene nasıl dönüştüğünü sor. Fotoğraf veya video çekmeden önce ustanın onayını al.",
                "https://goturkiye.com/tr/mardin/dokun",
                setOf(TripType.CULTURE, TripType.SHOPPING)
            ),
            TravelGuideExperience(
                "Taş sokaktan ovaya uzanan iki kare",
                "Fotoğraf",
                "Eski Mardin'de önce bir abbaranın içinden sokağı kadraja al, ardından Zinciriye çevresinden ova manzarasını çek. Yakın plandaki taş dokusunu geniş manzarayla eşleştir.",
                "https://goturkiye.com/tr/mardin/gor",
                setOf(TripType.PHOTOGRAPHY, TripType.ROMANTIC)
            )
        ),
        ("gaziantep" to null) to listOf(
            TravelGuideExperience(
                "Antep mutfağını tabaktan önce müzede tanı",
                "Yerel yaşam",
                "Emine Göğüş Mutfak Müzesi'nde mutfak araçları ve sofra kültürüne bak. Sonraki yemek molanda gördüğün bir pişirme veya sunum ayrıntısını bulmaya çalış. Bu gezi, yemeklerin yalnızca adını öğrenmenin ötesine geçer.",
                "https://goturkiye.com/tr/gaziantep/tat",
                setOf(TripType.GASTRONOMY, TripType.CULTURE)
            ),
            TravelGuideExperience(
                "Zeugma'da tek bir mozaiğe zaman ayır",
                "Aileyle keşif",
                "Zeugma Mozaik Müzesi'nde bir sahne seç. Figürlerin bakışlarını ve küçük renk geçişlerini incele, yanındaki kişiyle sahnenin ne anlattığını konuş. Sonra eser açıklamasını okuyup yorumlarınızı karşılaştırın.",
                "https://goturkiye.com/tr/gaziantep/gor",
                setOf(TripType.FAMILY, TripType.CULTURE)
            )
        ),
        ("bursa" to null) to listOf(
            TravelGuideExperience(
                "Koza Han'da ipeği tanı",
                "El sanatları ve alışveriş",
                "Koza Han'ın üst katındaki dükkânlarda ipek ürünlerin dokuma ve desenlerini karşılaştır. Satıcıya ürünün malzemesini ve nasıl korunacağını sor. Alışveriş yapmasan da hanı yaşayan bir üretim ve ticaret geleneği üzerinden tanımış olursun.",
                "https://goturkiye.com/tr/bursa/gor",
                setOf(TripType.SHOPPING, TripType.CULTURE)
            ),
            TravelGuideExperience(
                "Hanlar Bölgesi'nde avlu molası",
                "Kısa bir şehir planı",
                "Ulu Cami gezisinden sonra Koza Han avlusuna geç. Cephe ve revak ayrıntılarına bakarak kısa bir tur at, sonra oturup çarşının sesini dinle. Az zamanın varsa gününü bu iki durağa odaklayabilirsin.",
                "https://goturkiye.com/tr/bursa/gor",
                setOf(TripType.BUSINESS, TripType.WEEKEND, TripType.ROMANTIC)
            )
        ),
        ("trabzon" to null) to listOf(
            TravelGuideExperience(
                "Şehir Müzesi'nde Trabzon'un gündelik sesini dinle",
                "Yerel yaşam",
                "Trabzon Şehir Müzesi'nde sesli rehber seçeneğini sor ve gündelik yaşama ayrılan bölüme zaman ayır. Geziden sonra şehirde fark ettiğin bir alışkanlığı müzede öğrendiğin hikâyeyle eşleştir.",
                "https://goturkiye.com/tr/trabzon/gor",
                setOf(TripType.CULTURE, TripType.FAMILY)
            ),
            TravelGuideExperience(
                "Köşkte mimari ayrıntı avı",
                "Fotoğraf",
                "Atatürk Köşkü gezisinde giriş, pencere ve cephe ayrıntıları için üç farklı kare düşün. İçeride fotoğrafa izin verilen alanları görevliden öğrenerek ziyaretini planla.",
                "https://goturkiye.com/tr/trabzon/gor",
                setOf(TripType.PHOTOGRAPHY, TripType.WEEKEND)
            )
        ),
        ("rize" to null) to listOf(
            TravelGuideExperience(
                "Kaleden kıyıya manzara karşılaştırması",
                "Manzara",
                "Rize Kalesi'nden baktığın kıyı çizgisini aklında tut. Sahile indiğinde aynı kent silüetine bu kez deniz seviyesinden bak. Uzun bir yayla yolculuğuna çıkmadan merkezde iki farklı bakış açısı yakalayabilirsin.",
                "https://goturkiye.com/tr/rize/gor",
                setOf(TripType.PHOTOGRAPHY, TripType.WEEKEND)
            ),
            TravelGuideExperience(
                "Sarı Ev'de bir evin hikâyesini oku",
                "Yerel mimari",
                "Rize Etnografya Müzesi'nin Sarı Ev yapısını ve gündelik eşyalarını incele. Ahşap ve taşın kullanımına bakarak kıyıda gördüğün evlerle benzerliklerini bul.",
                "https://goturkiye.com/tr/rize/gor",
                setOf(TripType.CULTURE, TripType.FAMILY)
            )
        ),
        ("ordu" to null) to listOf(
            TravelGuideExperience(
                "Boztepe'de manzarayı katman katman keşfet",
                "Fotoğraf",
                "Boztepe'de önce yakınındaki kent dokusuna, sonra kıyı çizgisine ve ufka odaklan. Bu üç katmanı ayrı karelerle anlat. Teleferik planlıyorsan o günkü çalışma durumunu önceden kontrol et.",
                "https://goturkiye.com/tr/ordu/gor",
                setOf(TripType.PHOTOGRAPHY, TripType.ROMANTIC)
            ),
            TravelGuideExperience(
                "Ordu otlarını sofrada tanı",
                "Yerel üretim ve mutfak",
                "Menüde melocan, galdirik veya sakarca kavurması varsa birini seç. Hangi bitkiyle ve hangi yöntemle hazırlandığını sor. Bildiğin ot yemekleriyle tadını ve dokusunu karşılaştır.",
                "https://goturkiye.com/tr/ordu/tat",
                setOf(TripType.GASTRONOMY, TripType.CULTURE)
            )
        ),
        ("cankiri" to null) to listOf(
            TravelGuideExperience(
                "Çivitçioğlu'nda bir desenin yapılışını sor",
                "El sanatları",
                "Çivitçioğlu Medresesi'nde üretim yapan bir sanatçı varsa çalışmasını izlemek için izin iste. Beğendiğin bir desendeki çizgi ve renkleri nasıl kurduğunu sor. Atölye katılımı için ayrıca program ve rezervasyon gerekip gerekmediğini öğren.",
                "https://goturkiye.com/tr/cankiri/gor",
                setOf(TripType.CULTURE, TripType.SHOPPING)
            ),
            TravelGuideExperience(
                "Radyo ve İletişim Müzesi'nde aile oyunu",
                "Aileyle keşif",
                "Müzedeki eski iletişim araçlarından birini seçip nasıl kullanıldığını birlikte tahmin edin. Sonra açıklamayı okuyun. Telefonun bugünkü hâline gelene kadar nelerin değiştiğini konuşarak kısa bir geziyi ortak bir keşfe çevirebilirsiniz.",
                "https://goturkiye.com/tr/cankiri/gor",
                setOf(TripType.FAMILY, TripType.CULTURE)
            )
        ),
        ("izmir" to "cesme") to listOf(
            TravelGuideExperience(
                "Kaleden limana iki bakış açısı",
                "Fotoğraf",
                "Çeşme Kalesi gezisinde limana bakan bir manzara seç. Aşağı indiğinde kaleyi limandan kadraja al. Aynı çevrenin iki yönünü gösteren küçük bir fotoğraf çifti oluştur.",
                "https://goturkiye.com/tr/cruising/cesme-limani",
                setOf(TripType.PHOTOGRAPHY, TripType.WEEKEND)
            ),
            TravelGuideExperience(
                "Arkeoloji Müzesi'nde kıyının geçmişini keşfet",
                "Kültür molası",
                "Çeşme Kalesi içindeki Arkeoloji Müzesi'ni kıyı gezisinin ayrı bir durağı yap. İlgini çeken bir buluntunun geldiği yeri öğren, o yerin bugünkü konumunu haritada bul.",
                "https://goturkiye.com/tr/cruising/cesme-limani",
                setOf(TripType.CULTURE, TripType.FAMILY)
            )
        ),
        ("izmir" to "alacati") to listOf(
            TravelGuideExperience(
                "Alaçatı sokaklarında renk ve doku rotası",
                "Fotoğraf",
                "Taş duvar, renkli kapı ve yel değirmeni için birer kare seç. Evlerin kapı önlerine girmeden sokaktan çalış. Aynı renk tonunu üç farklı ayrıntıda bulmak gezine küçük bir tema katar.",
                "https://goturkiye.com/tr/cycling/cesme-alacati-1",
                setOf(TripType.PHOTOGRAPHY, TripType.ROMANTIC)
            ),
            TravelGuideExperience(
                "Otları isimleriyle tanı",
                "Yerel ürün",
                "Bir yemek molasında arapsaçı, cibes veya şevketibostan seçeneklerinden hangisinin bulunduğunu sor. Seçtiğin otun tadını ve hazırlanışını öğren. Ot Festivali'ne denk gelmek istiyorsan seyahat yılının programını ayrıca doğrula.",
                "https://goturkiye.com/tr/cesme/tat",
                setOf(TripType.GASTRONOMY, TripType.EVENT)
            )
        ),
        ("izmir" to "urla") to listOf(
            TravelGuideExperience(
                "Klazomenai'de zeytinyağının izini sür",
                "Yerel üretim",
                "Klazomenai'deki zeytinyağı atölyesi ziyaretinde üretim düzenini incele. Zeytinin yağa dönüşürken hangi aşamalardan geçtiğini takip et. Sonraki yemek molanda kullanılan yağın çeşidini sorarak eski üretimle bugünkü sofrayı bağlayabilirsin.",
                "https://goturkiye.com/culturaljourneys/urla",
                setOf(TripType.GASTRONOMY, TripType.CULTURE)
            ),
            TravelGuideExperience(
                "Malgaca'dan Sanat Sokağı'na kısa bir keşif",
                "Çarşı ve sanat",
                "Malgaca Pazarı'ndaki gezinle Sanat Sokağı'ndaki dükkân ziyaretlerini birleştir. Beğendiğin bir ürünün kim tarafından ve nerede üretildiğini sor. Hatıra seçimini o ürünün hikâyesine göre yapabilirsin.",
                "https://goturkiye.com/culturaljourneys/urla",
                setOf(TripType.SHOPPING, TripType.WEEKEND)
            )
        ),
        ("balikesir" to "ayvalik") to listOf(
            TravelGuideExperience(
                "Merkez ve Cunda'daki Taksiyarhis yapılarını karşılaştır",
                "Mimari keşif",
                "Ayvalık merkezindeki Taksiyarhis Anıt Müzesi ile Cunda'daki yapıyı ayrı duraklar olarak planla. Taş işçiliği ve iç mekân ayrıntılarında ortak noktaları bul. Müze ziyaret kurallarını her durakta kontrol et.",
                "https://balikesir.ktb.gov.tr/TR-65923/tarihi-yapilar.html",
                setOf(TripType.CULTURE, TripType.PHOTOGRAPHY)
            ),
            TravelGuideExperience(
                "Ayvalık ürünlerinden küçük bir sofra kur",
                "Yerel ürün",
                "Zeytin veya zeytinyağı alacaksan üretim yeri ve tarihini sor. Farklı çeşitleri tadabildiğin bir yerde küçük bir karşılaştırma yap. Dönüş için alacağın miktarı saklama ve taşıma koşullarına göre seç.",
                "https://goturkiye.com/tr/balikesir/tat",
                setOf(TripType.GASTRONOMY, TripType.SHOPPING)
            )
        )
    )

    fun forLocation(city: String, district: String?, tripType: TripType?): List<TravelGuideExperience> =
        entries[city to district].orEmpty()
            .sortedByDescending { tripType in it.preferredTripTypes }
            .take(2)
}
