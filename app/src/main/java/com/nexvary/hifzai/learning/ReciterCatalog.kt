package com.nexvary.hifzai.learning

enum class ReciterCountry(val displayArabic: String) {
    EGYPT("مصر"),
    SAUDI_ARABIA("السعودية")
}

data class ReciterProfile(
    val slug: String,
    val nameArabic: String,
    val country: ReciterCountry,
    /** Explicitly false until the recording and distribution rights are reviewed. */
    val hasLicensedAudio: Boolean = false
)

/** Index of public reciter names. Not an endorsed teacher roster or audio catalogue. */
object ReciterCatalog {
    val all = listOf(
        ReciterProfile("muhammad-siddiq-al-minshawi", "محمد صديق المنشاوي", ReciterCountry.EGYPT),
        ReciterProfile("mahmoud-khalil-al-husary", "محمود خليل الحصري", ReciterCountry.EGYPT),
        ReciterProfile("abdul-basit-abd-al-samad", "عبد الباسط عبد الصمد", ReciterCountry.EGYPT),
        ReciterProfile("abdulrahman-al-sudais", "عبد الرحمن السديس", ReciterCountry.SAUDI_ARABIA),
        ReciterProfile("maher-al-muaiqly", "ماهر المعيقلي", ReciterCountry.SAUDI_ARABIA),
        ReciterProfile("ali-al-hudhaifi", "علي الحذيفي", ReciterCountry.SAUDI_ARABIA)
    )

    fun byCountry(country: ReciterCountry) = all.filter { it.country == country }
}
