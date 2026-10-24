package com.nendo.argosy.data.preferences

/**
 * The kind of screen Argosy is shown on. Choosing one sets its starting values ([defaultUiScale],
 * [defaultSafeAreaPercent]); [hidesIdleCursor] hides the mouse cursor while a controller is in use.
 */
enum class DisplayProfile {
    HANDHELD, DESKTOP, TV;

    val defaultSafeAreaPercent: Int get() = if (this == TV) TV_SAFE_AREA_PERCENT else 0

    val defaultUiScale: Int get() = if (this == TV) TV_UI_SCALE else STANDARD_UI_SCALE

    val hidesIdleCursor: Boolean get() = this != DESKTOP

    companion object {
        const val TV_SAFE_AREA_PERCENT = 3
        const val MAX_SAFE_AREA_PERCENT = 5
        private const val TV_UI_SCALE = 125
        private const val STANDARD_UI_SCALE = 100

        fun fromString(value: String?): DisplayProfile? = entries.find { it.name == value }

        fun effective(stored: DisplayProfile?, detected: DisplayProfile?): DisplayProfile = stored ?: detected ?: DESKTOP

        fun effectiveSafeArea(stored: Int?, profile: DisplayProfile): Int =
            (stored ?: profile.defaultSafeAreaPercent).coerceIn(0, MAX_SAFE_AREA_PERCENT)
    }
}
