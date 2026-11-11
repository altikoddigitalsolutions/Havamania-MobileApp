package com.havamania

import java.util.Locale

/**
 * Reviewed destination content. Evidence and remaining coverage are tracked in
 * docs/SEYAHAT_REHBERI_KAPSAMI.md. Districts require their own entry.
 */
internal data class LocalTravelGuide(
    val description: String,
    val visit: String,
    val food: String,
    val experience: String
)

internal object LocalTravelGuides {
    private val cities = mapOf(
        "istanbul" to LocalTravelGuide(
            "Galata, Eminönü ve vapur hattında bir İstanbul günü",
            "Galata Kulesi'ni gezdikten sonra Karaköy'e in. Ardından Galata Köprüsü üzerinden Eminönü'ne geçerek kule, liman ve Haliç manzarasını aynı geziye bağlayabilirsin.",
            "Eminönü'nde balık ekmek dene. Ekmek arasında balık istemiyorsan Ortaköy'de kumpir ayrı bir seçenek. İkisini tek öğüne sıkıştırmak yerine bulunduğun semte göre seç.",
            "Eminönü'nden vapura binip yolculuğun kendisini geziye kat. Köprüden gördüğün silüeti bu kez denizden izle. Sefer saatini Şehir Hatları'ndan kontrol ederek dönüşünü planla."
        ),
        "ankara" to LocalTravelGuide(
            "Anadolu Medeniyetleri Müzesi ve Ankara Kalesi",
            "Anadolu Medeniyetleri Müzesi'nde Hitit ve Frig eserlerini gör, ardından Ankara Kalesi çevresindeki eski evleri gez. İki durağı aynı güne ayırarak Ulus çevresine odaklanabilirsin.",
            "Sabah Ankara simidini çayla dene. Öğle öğününde Ankara dönerini seç, yanına Çubuk turşusu ekleyebilirsin. Simidin daha küçük ve gevrek yapısını diğer şehirlerdeki simitle karşılaştır.",
            "Kale çevresindeki yürüyüşü Anadolu Medeniyetleri Müzesi ziyaretiyle birleştir. Kapalı alanı öncelemek istersen müzeden başla. Müzenin ziyaret saatini çıkmadan kontrol et."
        ),
        "izmir" to LocalTravelGuide(
            "Kemeraltı, Havra Sokağı ve Kordon",
            "Konak'ta Saat Kulesi'nden başlayıp Kemeraltı Çarşısı ve Havra Sokağı'nı gez. Kahve molanı Kızlarağası Hanı çevresinde ver. Kordon yürüyüşünü ayrı bir durak olarak planla.",
            "Kahvaltıda boyoz, gevrek ve İzmir tulumu dene. Kemeraltı gezisinde tatlı istersen şambaliyi seç. Böylece sadece bir yemek değil, kahvaltıdan çarşı molasına uzanan bir lezzet planın olur.",
            "Fotoğraf için Tarihi Asansör ve Dario Moreno Sokağı'nı aynı gezide değerlendir. Konak–Kemeraltı rotasına bunu ekleyeceksen ulaşım için ayrıca zaman bırak."
        ),
        "antalya" to LocalTravelGuide(
            "Hadrian Kapısı'ndan Kaleiçi'ne",
            "Kaleiçi gezisine Hadrian Kapısı'ndan başla, eski evlerin arasından Yat Limanı'na doğru ilerle. Tek geziyi bu çevrede tutarak Antalya'nın farklı ilçelerini aynı güne sıkıştırmamış olursun.",
            "Antalya usulü tahinli piyazı şiş köfteyle dene. Buradaki piyazın farkı tahinli tarator sosu. Tatlı için yanık dondurmayı seçebilir, kahvaltıda turunç reçelini deneyebilirsin.",
            "Kaleiçi yürüyüşüne ayrı bir sahil günü eklemek istersen Konyaaltı'nı seç. Sahili ve şehir içi tarih gezisini ayrı planlamak, sıcak saatlerde daha kısa yürüyüşler yapmana alan bırakır."
        ),
        "balikesir" to LocalTravelGuide(
            "Balıkesir merkezinde Kuva-yi Milliye Müzesi ve höşmerim",
            "Anafartalar Caddesi'ndeki Kuva-yi Milliye Müzesi'ni gez. Milli Mücadele bölümünün ardından arkeoloji ve etnografya koleksiyonlarına da zaman ayır. Merkezde tek bir müze durağı arıyorsan buradan başla.",
            "Balıkesir'de höşmerim dene. Peynir ve irmikle hazırlanan bu tatlıyı tanımak için küçük bir porsiyonla başla. Yemek seçeneği arıyorsan tirit veya saçaklı mantıyı tercih edebilirsin.",
            "Müze gezisinden sonra höşmerim molası ver. Sahile de gitmek istiyorsan Ayvalık ve Cunda için ayrı bir gün ayır, ulaşımını planına ekle."
        ),
        "mardin" to LocalTravelGuide(
            "Zinciriye Medresesi ve eski Mardin",
            "Zinciriye Medresesi'ni gör, ardından eski Mardin'in taş sokaklarını gez. Mezopotamya Ovası manzarasına zaman ayır. Deyrulzafaran Manastırı'nı eklersen ulaşımını ayrıca planla.",
            "Kaburga dolması için sofraya aç otur. Daha küçük bir öğün istiyorsan sembusek veya haşlama içli köfte olan ikbebeti seç. Tatlı molasında badem şekerini deneyebilirsin.",
            "Eski Mardin gezisini taş sokaklar ve ova manzarası çevresinde tut. Midyat'ı da görmek istersen ayrı bir gün planla. Mırra deneyeceksen sert kahve sevip sevmediğini düşünerek küçük bir fincanla başla."
        ),
        "bursa" to LocalTravelGuide(
            "Ulu Cami, Koza Han ve Kayhan lezzetleri",
            "Ulu Cami'yi gez, ardından Koza Han'ın avlusunda mola ver. Üst kattaki ipek dükkânlarına da bak. Hanlar Bölgesi'ni aynı gezinin odağı yaparak çarşı dokusunu takip edebilirsin.",
            "Kayhan Çarşısı'nda pideli köfte dene. Pide, köfte, yoğurt ve tereyağının birleşimini tatmak için bu öğüne yer ayır. Kahvaltıda tahinli pideyi, tatlı molasında kestane şekerini seçebilirsin.",
            "Koza Han'da ipek eşarpları incelerken farklı dokuma ve desenleri karşılaştır. Cumalıkızık'ı da görmek istiyorsan onu merkezdeki çarşı yürüyüşünden ayrı bir ulaşım durağı olarak planla."
        ),
        "trabzon" to LocalTravelGuide(
            "Atatürk Köşkü ve Trabzon Şehir Müzesi",
            "Atatürk Köşkü'nü gez, ardından Trabzon Şehir Müzesi'nde kentin gündelik yaşamına ayrılan bölümleri gör. İki noktanın ulaşımını planına ekle. Sümela Manastırı gezisini Maçka için ayrı bir güne ayırabilirsin.",
            "Kahvaltıda kuymak ve Vakfıkebir ekmeğini dene. Akçaabat'a gideceksen köfteyi oradaki öğününe ayır. Hamsiköy sütlacını da Maçka yönündeki bir gezide ayrıca değerlendirebilirsin.",
            "Şehir Müzesi'nde sesli rehber seçeneğini sor. Sümela veya Uzungöl'e gideceksen günün ulaşımını o bölge çevresinde kur, iki ayrı ilçe gezisini merkez turuna sıkıştırma."
        ),
        "rize" to LocalTravelGuide(
            "Rize Kalesi ve Atatürk Evi Müzesi",
            "Rize Kalesi'nden kent ve deniz manzarasını izle, ardından Atatürk Evi Müzesi'ni gez. Zilkale için Çamlıhemşin'e ayrıca gideceksen onu ayrı bir vadi günü olarak planla.",
            "Kahvaltıda muhlama ve mısır ekmeği dene. Çay molasında Rize simidini seç. Çayeli'ne gidersen kuru fasulyeyi o günün öğle öğününe ayırabilirsin.",
            "Merkezde kalacağın gün kale ve müze gezisine çay molası ekle. Fırtına Vadisi gezisi planlıyorsan yol ve hava durumunu çıkmadan kontrol et, şehir merkezi gününden ayrı tut."
        ),
        "ordu" to LocalTravelGuide(
            "Boztepe manzarası ve Ordu sahili",
            "Boztepe'ye çıkıp Karadeniz kıyısını yukarıdan gör. Teleferiği kullanacaksan çalışma durumunu kontrol et. Sahil yürüyüşünü aynı günün ikinci durağı yapabilirsin.",
            "Melocan kavurmasını dene, yanına mısır ekmeği ekleyebilirsin. Başka bir öğünde karalahana çorbası veya sarmasını seç. Fındık ürünlerini alırken sade kavrulmuş fındıkla başlayıp tadını karşılaştır.",
            "Boztepe gezisini açık görüş olan bir zamana ayır. Perşembe Yaylası'nı da görmek istiyorsan Aybastı yönüne ayrı bir ulaşım planı yap, şehir içindeki sahil turuyla aynı kısa yürüyüş gibi düşünme."
        ),
        "cankiri" to LocalTravelGuide(
            "Taş Mescit ve Çivitçioğlu Medresesi",
            "Taş Mescit'teki yılan figürlü taş işçiliğini gör. Ardından Çivitçioğlu Medresesi'nde geleneksel süsleme sanatlarını incele. Tuz Mağarası için ayrıca ulaşım ve ziyaret koşullarını kontrol et.",
            "Bir öğününde tutmaç çorbası dene. Hamur, mercimek ve sarımsaklı yoğurdun birleşimini tanıdıktan sonra başka bir öğünde sarımsaklı eti seçebilirsin. Tatlı molasına yumurta tatlısını ayır.",
            "Çivitçioğlu Medresesi ziyaretinde o gün üretim yapan sanatçı veya atölye olup olmadığını sor. Böylece yalnızca yapıyı gezmekle kalmayıp süsleme sanatının nasıl uygulandığını da görebilirsin."
        ),
        "gaziantep" to LocalTravelGuide(
            "Zeugma mozaikleri ve Antep mutfağı",
            "Zeugma Mozaik Müzesi'ni gez. Mozaiklerde yalnızca büyük sahnelere bakmakla yetinme, figürlerin yüz ve kıyafet ayrıntılarına da zaman ayır. Müze ziyaretini çarşı gezisinden ayrı bir durak olarak planla.",
            "Kahvaltıda beyran dene. Et, pirinç ve sarımsakla hazırlanan bu yemeğin acısını sipariş verirken belirt. Tatlı molasında katmer veya baklavadan birini seçerek öğünlerini aralıklı planlayabilirsin.",
            "Bir güne bütün Antep mutfağını sığdırmaya çalışma. Beyranı sabaha, baklavayı ayrı bir molaya ayır. Yemek odaklı geziyorsan Zeugma ziyaretini bu molaların arasına yerleştir."
        )
    )

    private val districts = mapOf(
        ("izmir" to "cesme") to LocalTravelGuide(
            "Çeşme Kalesi, çarşı ve sakızlı lezzetler",
            "Çeşme Kalesi ve Arkeoloji Müzesi'ni gez, ardından çarşıya in. Alaçatı'yı eklersen yel değirmenleri ve taş evler için ayrı ulaşım planı yap.",
            "Çeşme'de kumru ye, tatlı molasında damla sakızlı muhallebi veya dondurma dene. Ot yemekleri istiyorsan şevketibostan ya da deniz börülcesi olup olmadığını menüden kontrol et.",
            "Kale ve çarşı gezisinden sonra ikinci durak olarak Alaçatı'nın yel değirmenlerini seçebilirsin. Sörf denemek istersen Alaçatı'daki bir okuldan ders ve hava koşulları için bilgi al."
        ),
        ("izmir" to "alacati") to LocalTravelGuide(
            "Alaçatı'nın yel değirmenleri ve taş sokakları",
            "Yel değirmenlerini gör, ardından Alaçatı'nın taş evlerinin arasındaki sokaklarda yürü. Çeşme Kalesi'ni de eklemek istersen onu ayrı bir ulaşım durağı olarak planla.",
            "Kumruyu bir öğüne, damla sakızlı muhallebiyi ayrı bir molaya ayır. Ot yemeklerinde deniz börülcesi, arapsaçı veya şevketibostan seçeneklerini sor.",
            "Sörfe merakın varsa Alaçatı'da başlangıç dersi için bir okulla görüş. Alaçatı Ot Festivali'ne göre gezi planlayacaksan seyahat yılının tarihini organizatörden doğrula."
        ),
        ("balikesir" to "ayvalik") to LocalTravelGuide(
            "Ayvalık Taksiyarhis Anıt Müzesi ve Cunda",
            "Ayvalık merkezindeki Taksiyarhis Anıt Müzesi'ni gez. Cunda'ya geçersen adadaki Taksiyarhis yapısını da ayrı bir durak olarak gör. Aynı adla anılan bu iki yapıyı planında ayrı tut.",
            "Ayvalık tostunu bir öğününde dene. Daha uzun bir sofrada deniz börülcesi ve zeytinyağlı otları seçebilirsin. Tatlı molasında Ayvalık lor tatlısını sor.",
            "Merkezdeki müze gezisini Cunda sokakları ve sahil yürüyüşüyle tamamla. Papalina denemek istersen balığın mevsimini ve o gün bulunup bulunmadığını işletmeye sor."
        ),
        ("izmir" to "urla") to LocalTravelGuide(
            "Urla'da enginar ve yerel üretim odaklı bir gün",
            "İskele'deki Klazomenai Antik Kenti'ni ve zeytinyağı atölyesini gör. Merkez gezisinde Malgaca Pazarı ve Sanat Sokağı'nı aynı plana al. İskele ile merkez arasındaki ulaşım için ayrıca zaman bırak.",
            "Enginar, kabak çiçeği dolması ve Ege otlarıyla hazırlanan bir sofra seç. Mevsimde varsa arapsaçı veya şevketibostan dene. Siparişten önce o gün hazırlanan otları sor.",
            "Urla Bağ Rotası için ziyaret kabul eden bir üreticiden randevu alabilir veya Sanat Sokağı'ndaki dükkânları gezebilirsin. Enginar Festivali ilgini çekiyorsa seyahat yılının tarihini belediyeden doğrula."
        )
    )

    fun find(city: String, district: String?): LocalTravelGuide? {
        TravelGuideUpdates.current(city, district)?.let { return it.guide() }
        val cityKey = key(city)
        val districtKey = district?.let(::key)?.takeIf { it.isNotEmpty() && it != cityKey }
        return if (districtKey == null) cities[cityKey] else districts[cityKey to districtKey]
    }

    fun experiences(city: String, district: String?, tripType: TripType?): List<TravelGuideExperience> {
        TravelGuideUpdates.current(city, district)?.let { return it.selected(tripType) }
        if (find(city, district) == null) return emptyList()
        val cityKey = key(city)
        val districtKey = district?.let(::key)?.takeIf { it.isNotEmpty() && it != cityKey }
        return TravelGuideExperiences.forLocation(cityKey, districtKey, tripType)
    }

    internal fun key(value: String): String = value.trim().lowercase(Locale.forLanguageTag("tr"))
        .replace('ç', 'c').replace('ğ', 'g').replace('ı', 'i')
        .replace('ö', 'o').replace('ş', 's').replace('ü', 'u')
}
