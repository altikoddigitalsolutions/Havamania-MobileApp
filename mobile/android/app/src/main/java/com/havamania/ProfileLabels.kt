package com.havamania

import java.text.Normalizer

/** Cosmetic profile labels, never health, expertise, or remote-assistant consent decisions. */
private object ProfileLabels {
    private enum class Signal(val badge: String, val personality: String) {
        STORM("Fırtına meraklısı", "Atmosfer Avcısı"),
        NATURE("Doğa dostu", "Doğa Gezgini"),
        FAMILY("Aile odaklı", "Aile Meteoroloğu"),
        SKY("Gökyüzü meraklısı", "Gökyüzü Kaşifi"),
        ROAD("Yol meraklısı", "Yol Savaşçısı"),
        WINTER("Kış meraklısı", "Kış Tutkunu"),
        TRAVEL("Seyahat meraklısı", "Rota Kaşifi"),
        SPORT("Spor meraklısı", "Spor Meraklısı"),
        RAIN("Yağmur meraklısı", "Yağmur Meraklısı"),
        PHOTO("Fotoğraf meraklısı", "Fotoğraf Meraklısı"),
        URBAN("Şehir meraklısı", "Şehir Kaşifi"),
        WEATHER("Hava meraklısı", "Hava Meraklısı")
    }
    private fun words(value: String) = value.split(' ').toSet()
    private val selectedIds = mapOf(
        Signal.STORM to words("firtina_takibi yildirim_takibi extreme_hava"),
        Signal.NATURE to words("kamp outdoor dagcilik trekking acik_hava balikcilik"),
        Signal.FAMILY to words("cocuklar_icin"),
        Signal.SKY to words("drone pilotluk aurora"),
        Signal.ROAD to words("motorsiklet motosiklet karavan off_road uzun_yol"),
        Signal.WINTER to words("snowboard kayak kis_surusu"),
        Signal.TRAVEL to words("hafta_sonu"),
        Signal.PHOTO to words("bulut_fotografciligi gun_batimi"),
        Signal.WEATHER to words("meteoroloji")
    )
    // Explicit forms avoid substring matches such as Ankara/karavan -> kar or kampüs -> kamp.
    private val topics = mapOf(
        Signal.STORM to words("firtina firtinayi firtinalari firtinalar firtinalarin yildirim yildirimlari"),
        Signal.NATURE to words("kamp kampi kampa kampta kampcilik kampci doga dogayi dogada dogaya doganin yuruyus yuruyusu yuruyuse trekking dagcilik"),
        Signal.FAMILY to words("cocuk cocuklar cocuklari cocuklarim cocuklarimla cocuklarla cocuklarimin aile ailem ailemle ailemi ailemizle"),
        Signal.SKY to words("drone dron dronla droneu pilot pilotluk pilotum gokyuzu gokyuzunu"),
        Signal.ROAD to words("motosiklet motosikleti motosikletle motorsiklet motorsikleti motorsikletle karavan karavanla offroad"),
        Signal.WINTER to words("kar kari karda kardan karla karli karin kis kisin kista kistan kisi kayak kayagi snowboard"),
        Signal.TRAVEL to words("seyahat seyahati seyahate seyahatler seyahatleri rota rotalar rotalari yolculuk yolculuklari"),
        Signal.SPORT to words("spor sporu spora sporla sporcu sporcuyum bisiklet bisikleti bisikletle kosu kosuyu kosuya"),
        Signal.RAIN to words("yagmur yagmuru yagmurda yagmurdan yagmurlu yagmurun yagmurlari"),
        Signal.PHOTO to words("fotograf fotografi fotograflari fotografcilik fotografcilikla fotografciligi fotograflarla"),
        Signal.URBAN to words("sehir sehri sehirleri sehirde sehirlerde"),
        Signal.WEATHER to words("meteoroloji meteorolojiyi")
    )
    private val affirmative = words("seviyorum severim seviyoruz severiz sevdim hoslaniyorum hoslanirim ilgileniyorum ilgilenirim ilgiliyim yapiyorum yaparim yapiyoruz kullaniyorum kullanirim geziyorum gezerim cekiyorum cekerim planliyorum planlarim gidiyorum giderim izliyorum izlerim yuruyorum yururum pilotum")
    private val negative = words("sevmiyorum sevmem sevmiyoruz hoslanmiyorum hoslanmam ilgilenmiyorum ilgilenmem yapmiyorum yapmam kullanmiyorum kullanmam istemiyorum istemem etmiyorum etmem degil degilim yok nefret korkuyorum korkarim rahatsiz rahatsizim kaciniyorum kacinirim zorunlu")
    private val negativeForms = Regex("(?:sevmiy|hoslanmiy|ilgilenmiy|yapmiy|kullanmiy|istemiy|etmiy|sevmed|hoslanmad|ilgilenmed|yapmad|kullanmad|istemed|etmed).*" )
    private val clauses = Regex("""[.!?;,\n]+|\b(?:ama|ancak|fakat|oysa)\b""")
    private val tokens = Regex("[a-z0-9]+")

    private fun selected(interests: Set<String>) = Signal.entries.filter { signal ->
        selectedIds[signal]?.any { it in interests } == true
    }
    private fun fromText(text: String): List<Signal> {
        val normalized = AiIntentParser.normalizeTurkish(Normalizer.normalize(text, Normalizer.Form.NFC))
        val found = mutableSetOf<Signal>()
        val rejectedTopics = mutableSetOf<Signal>()
        clauses.split(normalized).forEach { clause ->
            val terms = tokens.findAll(clause).map { it.value }.toSet()
            val mentioned = topics.filterValues { vocabulary -> terms.any { it in vocabulary } }.keys
            if (terms.any { it in negative || negativeForms.matches(it) }) {
                if (mentioned.isEmpty()) { rejectedTopics.addAll(found); found.clear() }
                else { rejectedTopics.addAll(mentioned); found.removeAll(mentioned) }
                return@forEach
            }
            val positive = terms.any { it in affirmative } ||
                (terms.any { it == "takip" || it == "seyahat" } && terms.any { it in setOf("ediyorum", "ediyoruz", "ederim", "ederiz") })
            if (!positive) return@forEach
            found.addAll(mentioned.filterNot { it in rejectedTopics })
        }
        return Signal.entries.filter { it in found }
    }
    fun personality(interests: Set<String>, text: String): String =
        (selected(interests).firstOrNull() ?: fromText(text).firstOrNull())?.personality ?: "Hava Meraklısı"

    fun badges(text: String, interests: Set<String>): List<String> =
        (selected(interests) + fromText(text)).distinct().take(3).map { it.badge }
}

fun generateAiPersonality(interests: Set<String>, aboutMe: String): String = ProfileLabels.personality(interests, aboutMe)
fun generateBadges(text: String, interests: Set<String> = emptySet()): List<String> = ProfileLabels.badges(text, interests)
