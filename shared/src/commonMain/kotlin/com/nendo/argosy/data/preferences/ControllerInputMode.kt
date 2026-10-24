package com.nendo.argosy.data.preferences

enum class ControllerInputMode {
    AUTO, STEAM_INPUT, SDL3;

    fun resolved(steamInputAvailable: Boolean): ControllerInputMode = when {
        this == AUTO -> if (steamInputAvailable) STEAM_INPUT else SDL3
        this == STEAM_INPUT && !steamInputAvailable -> SDL3
        else -> this
    }

    companion object {
        fun fromString(value: String?): ControllerInputMode =
            entries.find { it.name == value } ?: AUTO

        fun offered(steamInputAvailable: Boolean): List<ControllerInputMode> =
            if (steamInputAvailable) entries else listOf(AUTO, SDL3)
    }
}
