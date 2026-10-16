// Extracted verbatim from app/src/main/kotlin/com/nendo/argosy/ui/screens/doodle/DoodleModels.kt: DecodedDoodle DoodleColor CanvasSize ZoomLevel
package com.nendo.argosy.ui.screens.doodle

import androidx.compose.ui.graphics.Color

data class DecodedDoodle(
    val size: CanvasSize,
    val pixels: Map<Pair<Int, Int>, DoodleColor>
)

enum class DoodleColor(val index: Int, val hex: Long) {
    WHITE(0, 0xFFFFFFFF),
    BLACK(1, 0xFF000000),
    GRAY(2, 0xFF808080),
    RED(3, 0xFFFF0000),
    ORANGE(4, 0xFFFF8800),
    YELLOW(5, 0xFFFFFF00),
    GREEN(6, 0xFF00FF00),
    CYAN(7, 0xFF00FFFF),
    LIGHT_BLUE(8, 0xFF0088FF),
    BLUE(9, 0xFF0000FF),
    PURPLE(10, 0xFF8800FF),
    MAGENTA(11, 0xFFFF00FF),
    PINK(12, 0xFFFF88AA),
    BROWN(13, 0xFF884400),
    LIGHT_GREEN(14, 0xFF88FF88),
    SKIN(15, 0xFFFFCC88);

    val color: Color get() = Color(hex)

    companion object {
        fun fromIndex(index: Int): DoodleColor =
            entries.find { it.index == index } ?: WHITE
    }
}

enum class CanvasSize(val pixels: Int, val sizeEnum: Int) {
    SMALL(16, 0),
    MEDIUM(32, 1),
    LARGE(64, 2);

    companion object {
        fun fromEnum(value: Int): CanvasSize =
            entries.find { it.sizeEnum == value } ?: MEDIUM
    }
}

enum class ZoomLevel(val scale: Float) {
    FIT(1f),
    X2(2f),
    X4(4f),
    X8(8f);

    fun next(): ZoomLevel = when (this) {
        FIT -> X2
        X2 -> X4
        X4 -> X8
        X8 -> FIT
    }
}
