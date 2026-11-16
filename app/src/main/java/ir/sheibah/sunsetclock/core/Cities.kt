package ir.sheibah.sunsetclock.core

import java.time.ZoneId

/** یک مکان جغرافیایی همراه با منطقه‌ی زمانی. */
data class Place(val lat: Double, val lng: Double, val zone: ZoneId)

data class City(
    val id: String,
    val fa: String,
    val en: String,
    val lat: Double,
    val lng: Double,
    val tz: String = "Asia/Tehran"
) {
    fun toPlace(): Place = Place(lat, lng, ZoneId.of(tz))
}

object Cities {
    const val DEFAULT_ID = "tehran"

    val all: List<City> = listOf(
        City("tehran", "تهران", "Tehran", 35.6892, 51.3890),
        City("mashhad", "مشهد", "Mashhad", 36.2605, 59.6168),
        City("isfahan", "اصفهان", "Isfahan", 32.6546, 51.6680),
        City("shiraz", "شیراز", "Shiraz", 29.5918, 52.5837),
        City("tabriz", "تبریز", "Tabriz", 38.0962, 46.2738),
        City("karaj", "کرج", "Karaj", 35.8400, 50.9391),
        City("qom", "قم", "Qom", 34.6399, 50.8759),
        City("ahvaz", "اهواز", "Ahvaz", 31.3183, 48.6706),
        City("kermanshah", "کرمانشاه", "Kermanshah", 34.3142, 47.0650),
        City("urmia", "ارومیه", "Urmia", 37.5527, 45.0761),
        City("rasht", "رشت", "Rasht", 37.2808, 49.5832),
        City("zahedan", "زاهدان", "Zahedan", 29.4963, 60.8629),
        City("kerman", "کرمان", "Kerman", 30.2839, 57.0834),
        City("yazd", "یزد", "Yazd", 31.8974, 54.3569),
        City("ardabil", "اردبیل", "Ardabil", 38.2498, 48.2933),
        City("bandarabbas", "بندرعباس", "Bandar Abbas", 27.1865, 56.2808),
        City("arak", "اراک", "Arak", 34.0954, 49.6892),
        City("hamadan", "همدان", "Hamadan", 34.7989, 48.5150),
        City("sanandaj", "سنندج", "Sanandaj", 35.3219, 46.9862),
        City("qazvin", "قزوین", "Qazvin", 36.2797, 50.0049),
        City("zanjan", "زنجان", "Zanjan", 36.6765, 48.4963),
        City("gorgan", "گرگان", "Gorgan", 36.8427, 54.4353),
        City("sari", "ساری", "Sari", 36.5633, 53.0601),
        City("bushehr", "بوشهر", "Bushehr", 28.9234, 50.8203),
        City("birjand", "بیرجند", "Birjand", 32.8649, 59.2262),
        City("khorramabad", "خرم‌آباد", "Khorramabad", 33.4878, 48.3558),
        City("ilam", "ایلام", "Ilam", 33.6374, 46.4227),
        City("yasuj", "یاسوج", "Yasuj", 30.6682, 51.5880),
        City("shahrekord", "شهرکرد", "Shahrekord", 32.3256, 50.8644),
        City("semnan", "سمنان", "Semnan", 35.5769, 53.3920),
        City("bojnurd", "بجنورد", "Bojnurd", 37.4747, 57.3290),
        City("mecca", "مکه", "Mecca", 21.3891, 39.8579, "Asia/Riyadh"),
        City("medina", "مدینه", "Medina", 24.4709, 39.6122, "Asia/Riyadh"),
        City("karbala", "کربلا", "Karbala", 32.6160, 44.0249, "Asia/Baghdad"),
        City("najaf", "نجف", "Najaf", 31.9960, 44.3150, "Asia/Baghdad"),
        City("baghdad", "بغداد", "Baghdad", 33.3152, 44.3661, "Asia/Baghdad"),
        City("istanbul", "استانبول", "Istanbul", 41.0082, 28.9784, "Europe/Istanbul"),
        City("dubai", "دبی", "Dubai", 25.2048, 55.2708, "Asia/Dubai"),
        City("kabul", "کابل", "Kabul", 34.5553, 69.2075, "Asia/Kabul"),
        City("baku", "باکو", "Baku", 40.4093, 49.8671, "Asia/Baku"),
        City("karachi", "کراچی", "Karachi", 24.8607, 67.0011, "Asia/Karachi"),
        City("london", "لندن", "London", 51.5074, -0.1278, "Europe/London"),
        City("berlin", "برلین", "Berlin", 52.5200, 13.4050, "Europe/Berlin"),
        City("toronto", "تورنتو", "Toronto", 43.6532, -79.3832, "America/Toronto")
    )

    fun byId(id: String?): City = all.firstOrNull { it.id == id } ?: all.first()
}
