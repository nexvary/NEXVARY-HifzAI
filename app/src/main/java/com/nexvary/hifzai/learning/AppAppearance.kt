package com.nexvary.hifzai.learning

/** Legacy installations remain dark until the learner explicitly chooses another mode. */
enum class AppAppearance(val arabic: String) {
    DARK("داكن"), LIGHT("فاتح"), SYSTEM("حسب النظام");

    fun usesDarkColors(systemDark: Boolean): Boolean = when (this) {
        DARK -> true
        LIGHT -> false
        SYSTEM -> systemDark
    }

    companion object {
        fun fromStored(value: String?): AppAppearance = entries.firstOrNull { it.name == value } ?: DARK
    }
}
