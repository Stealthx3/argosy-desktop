// Extracted verbatim from app/src/main/kotlin/com/nendo/argosy/data/preferences/UserPreferencesRepository.kt: ThemeMode GripReserveMode SystemIconPosition GlowColorMode BoxArtOuterEffect BoxArtInnerEffect BoxArtGlowStrength BoxArtBorderStyle PlatformIndicatorStyle PlatformIndicatorContent BoxArtBorderThickness BoxArtCornerRadius BoxArtInnerEffectThickness BoxArtOuterEffectThickness BoxArtShape DefaultView DisplayRoleOverride DualScreenInputFocus GlassBorderTint GridDensity LibraryLayout SystemIconPadding
package com.nendo.argosy.data.preferences



enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System");

    companion object {
        fun fromString(value: String?): ThemeMode =
            entries.find { it.name == value } ?: SYSTEM
    }
}

enum class GripReserveMode(val displayName: String) {
    OFF("Off"),
    ON("On"),
    AUTO("Auto");

    companion object {
        fun fromString(value: String?): GripReserveMode =
            entries.firstOrNull { it.name == value } ?: OFF
    }
}

enum class SystemIconPosition {
    OFF, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT;

    companion object {
        // Cycleable corner positions only. OFF is reserved as an internal sentinel
        // (BoxArtShapes carve-out gating) and is reached via PlatformIndicatorStyle.OFF.
        val CORNERS: List<SystemIconPosition> = listOf(TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT)

        fun fromString(value: String?): SystemIconPosition =
            entries.find { it.name == value } ?: TOP_LEFT
    }
}

enum class GlowColorMode {
    AUTO, ACCENT, ACCENT_GRADIENT, COVER;

    companion object {
        fun fromString(value: String?): GlowColorMode =
            entries.find { it.name == value } ?: AUTO
    }
}

enum class BoxArtOuterEffect {
    OFF, GLOW, SHADOW, SHINE;

    companion object {
        fun fromString(value: String?): BoxArtOuterEffect =
            entries.find { it.name == value } ?: GLOW
    }
}

enum class BoxArtInnerEffect {
    OFF, GLOW, SHADOW, GLASS, SHINE;

    companion object {
        fun fromString(value: String?): BoxArtInnerEffect =
            entries.find { it.name == value } ?: SHADOW
    }
}

enum class BoxArtGlowStrength(val alpha: Float, val isShadow: Boolean = false) {
    OFF(0f),
    LOW(0.2f),
    MEDIUM(0.4f),
    HIGH(0.6f),
    SHADOW_SMALL(0.15f, isShadow = true),
    SHADOW_LARGE(0.25f, isShadow = true);

    companion object {
        fun fromString(value: String?): BoxArtGlowStrength =
            entries.find { it.name == value } ?: MEDIUM
    }
}

enum class BoxArtBorderStyle {
    SOLID, GLASS, GRADIENT;

    companion object {
        fun fromString(value: String?): BoxArtBorderStyle =
            entries.find { it.name == value } ?: SOLID
    }
}

enum class PlatformIndicatorStyle {
    OFF, TAB, SPINE;

    companion object {
        fun fromString(value: String?): PlatformIndicatorStyle =
            entries.find { it.name == value } ?: TAB
    }
}

enum class PlatformIndicatorContent {
    NAME, ICON, NAME_AND_ICON;

    companion object {
        fun fromString(value: String?): PlatformIndicatorContent =
            entries.find { it.name == value } ?: NAME
    }
}

enum class BoxArtBorderThickness(val dp: Int) {
    NONE(0), THIN(1), MEDIUM(2), THICK(4);

    companion object {
        fun fromString(value: String?): BoxArtBorderThickness =
            entries.find { it.name == value } ?: MEDIUM
    }
}

enum class BoxArtCornerRadius(val dp: Int) {
    NONE(0), SMALL(4), MEDIUM(8), LARGE(12), EXTRA_LARGE(16);

    companion object {
        fun fromString(value: String?): BoxArtCornerRadius =
            entries.find { it.name == value } ?: MEDIUM
    }
}

enum class BoxArtInnerEffectThickness(val px: Float) {
    THIN(6f), MEDIUM(16f), THICK(24f);

    companion object {
        fun fromString(value: String?): BoxArtInnerEffectThickness =
            entries.find { it.name == value } ?: MEDIUM
    }
}

enum class BoxArtOuterEffectThickness(val px: Float) {
    THIN(8f), MEDIUM(16f), THICK(24f);

    companion object {
        fun fromString(value: String?): BoxArtOuterEffectThickness =
            entries.find { it.name == value } ?: MEDIUM
    }
}

enum class BoxArtShape(val aspectRatio: Float, val displayName: String) {
    TALL(2f / 3f, "2:3"),
    STANDARD(3f / 4f, "3:4"),
    SQUARE(1f, "1:1"),
    NATIVE(3f / 4f, "Native");

    /** Whether covers should use their real image proportions instead of [aspectRatio]. */
    val isNative: Boolean get() = this == NATIVE

    companion object {
        fun fromString(value: String?): BoxArtShape =
            entries.find { it.name == value } ?: STANDARD
    }
}

enum class DefaultView {
    HOME, LIBRARY;

    companion object {
        fun fromString(value: String?): DefaultView = when (value) {
            "HOME", "SHOWCASE" -> HOME
            "LIBRARY" -> LIBRARY
            else -> HOME
        }
    }
}

enum class DisplayRoleOverride(val displayName: String) {
    AUTO("Auto"),
    STANDARD("Standard"),
    SWAPPED("Swapped");

    companion object {
        fun fromString(value: String?): DisplayRoleOverride =
            entries.find { it.name == value } ?: AUTO
    }
}

enum class DualScreenInputFocus(val displayName: String) {
    AUTO("Auto"),
    TOP("Top Screen"),
    BOTTOM("Bottom Screen");

    companion object {
        fun fromString(value: String?): DualScreenInputFocus =
            entries.find { it.name == value } ?: AUTO
    }
}

enum class GlassBorderTint(val alpha: Float) {
    OFF(0f), TINT_5(0.05f), TINT_10(0.10f), TINT_15(0.15f), TINT_20(0.20f), TINT_25(0.25f);

    companion object {
        fun fromString(value: String?): GlassBorderTint =
            entries.find { it.name == value } ?: OFF
    }
}

enum class GridDensity {
    COMPACT, NORMAL, SPACIOUS;

    companion object {
        fun fromString(value: String?): GridDensity =
            entries.find { it.name == value } ?: NORMAL
    }
}

enum class LibraryLayout {
    GRID, LIST;

    companion object {
        fun fromString(value: String?): LibraryLayout =
            entries.find { it.name == value } ?: GRID
    }
}

enum class SystemIconPadding(val dp: Int) {
    SMALL(4), MEDIUM(8), LARGE(12);

    companion object {
        fun fromString(value: String?): SystemIconPadding =
            entries.find { it.name == value } ?: MEDIUM
    }
}
