package com.havamania

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

data class InterestCategory(
    val title: String,
    val icon: ImageVector,
    val description: String,
    val interests: List<InterestItem>
)

data class InterestItem(
    val id: String,
    val label: String,
    val icon: ImageVector
)

object InterestsData {
    val categories = listOf(
        InterestCategory(
            title = "Hava & Atmosfer",
            icon = Icons.Rounded.Cloud,
            description = "Gökyüzü olaylarını ve meteorolojik detayları takip edin.",
            interests = listOf(
                InterestItem("meteoroloji", "Meteoroloji", Icons.Rounded.Analytics),
                InterestItem("firtina_takibi", "Fırtına Takibi", Icons.Rounded.Thunderstorm),
                InterestItem("extreme_hava", "Extreme Hava", Icons.Rounded.Warning),
                InterestItem("yildirim_takibi", "Yıldırım Takibi", Icons.Rounded.FlashOn),
                InterestItem("aurora", "Aurora", Icons.Rounded.AutoAwesome),
                InterestItem("bulut_fotografciligi", "Bulut Fotoğrafçılığı", Icons.Rounded.CameraAlt),
                InterestItem("gun_batimi", "Gün Batımı", Icons.Rounded.WbTwilight)
            )
        ),
        InterestCategory(
            title = "Ulaşım",
            icon = Icons.Rounded.DirectionsCar,
            description = "Yolculuklarınızı hava durumuna göre planlayın.",
            interests = listOf(
                InterestItem("motorsiklet", "Motorsiklet", Icons.Rounded.TwoWheeler),
                InterestItem("kis_surusu", "Kış Sürüşü", Icons.Rounded.AcUnit),
                InterestItem("off_road", "Off-Road", Icons.Rounded.Terrain),
                InterestItem("drone", "Drone", Icons.Rounded.AirplanemodeActive),
                InterestItem("pilotluk", "Pilotluk", Icons.Rounded.Flight),
                InterestItem("karavan", "Karavan", Icons.Rounded.RvHookup),
                InterestItem("uzun_yol", "Uzun Yol", Icons.Rounded.Route)
            )
        ),
        InterestCategory(
            title = "Outdoor",
            icon = Icons.Rounded.Landscape,
            description = "Doğa aktiviteleri için en uygun zamanı bulun.",
            interests = listOf(
                InterestItem("kamp", "Kamp", Icons.Rounded.Terrain),
                InterestItem("dagcilik", "Dağcılık", Icons.Rounded.FilterHdr),
                InterestItem("snowboard", "Snowboard", Icons.Rounded.Snowboarding),
                InterestItem("kayak", "Kayak", Icons.Rounded.DownhillSkiing),
                InterestItem("balikcilik", "Balıkçılık", Icons.Rounded.Phishing),
                InterestItem("trekking", "Trekking", Icons.Rounded.Hiking)
            )
        ),
        InterestCategory(
            title = "Sağlık",
            icon = Icons.Rounded.Healing,
            description = "Hava durumunun sağlığınız üzerindeki etkilerini izleyin.",
            interests = listOf(
                InterestItem("migren", "Migren Hassasiyeti", Icons.Rounded.Psychology),
                InterestItem("uv_hassasiyeti", "UV Hassasiyeti", Icons.Rounded.WbSunny),
                InterestItem("polen", "Polen Hassasiyeti", Icons.Rounded.LocalFlorist),
                InterestItem("basinc_hassasiyeti", "Basınç Hassasiyeti", Icons.Rounded.Compress)
            )
        ),
        InterestCategory(
            title = "Yaşam Tarzı",
            icon = Icons.Rounded.House,
            description = "Günlük rutininizi ve aile planlarınızı optimize edin.",
            interests = listOf(
                InterestItem("cocuklar_icin", "Çocuklar İçin", Icons.Rounded.ChildCare),
                InterestItem("evcil_hayvanlar", "Evcil Hayvanlar", Icons.Rounded.Pets),
                // "Hafta Sonu Kaçamakları" map to a more standard icon
                InterestItem("hafta_sonu", "Hafta Sonu", Icons.Rounded.Weekend),
                InterestItem("acik_hava", "Açık Hava Aktiviteleri", Icons.Rounded.Park)
            )
        )
    )
}
