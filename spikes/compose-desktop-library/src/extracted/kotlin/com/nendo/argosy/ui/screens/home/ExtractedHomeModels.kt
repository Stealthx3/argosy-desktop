// Extracted verbatim from app/src/main/kotlin/com/nendo/argosy/ui/screens/home/HomeModels.kt: GameDownloadIndicator HomePlatformUi
package com.nendo.argosy.ui.screens.home



data class GameDownloadIndicator(
    val isDownloading: Boolean = false,
    val isExtracting: Boolean = false,
    val isPaused: Boolean = false,
    val isQueued: Boolean = false,
    val progress: Float = 0f
) {
    /**
     * Whether work is moving right now. Paused is not active: nothing is progressing, and a cover
     * that keeps rippling over a stopped download states the opposite of what is true.
     */
    val isActive: Boolean get() = isDownloading || isExtracting || isQueued

    /**
     * Whether the cover should be drawn as a part-finished copy at all. Wider than [isActive],
     * because a paused transfer still has bytes on disk worth showing - it is stilled, not gone.
     */
    val isShown: Boolean get() = isActive || isPaused

    companion object {
        val NONE = GameDownloadIndicator()
    }
}

data class HomePlatformUi(
    val id: Long,
    val slug: String,
    val name: String,
    val shortName: String,
    val displayName: String,
    val logoPath: String?,
    val hasEmulator: Boolean = true
)
