// Extracted verbatim from app/src/main/kotlin/com/nendo/argosy/data/sync/platform/PlatformSaveHandler.kt: MemcardInfo
package com.nendo.argosy.data.sync.platform



/**
 * [name] is the on-disk memcard folder name for a real card, an upstream identifier that
 * must never be translated. The synthetic "use the inherited default" entry built by
 * [com.nendo.argosy.ui.screens.gamedetail.delegates.PerGameSettingsDelegate] sets
 * [isDefault] instead of writing a display label into [name], leaving the "Default" wording
 * to the renderer.
 */
data class MemcardInfo(
    val name: String,
    val path: String,
    val gameFolderCount: Int,
    val lastModified: Long,
    val isDefault: Boolean = false
)
