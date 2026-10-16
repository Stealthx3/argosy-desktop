// Extracted verbatim from app/src/main/kotlin/com/nendo/argosy/data/emulator/VariantResolver.kt: VariantOption
package com.nendo.argosy.data.emulator



data class VariantOption(
    val fileId: Long?,
    val fileName: String,
    val category: String,
    val isDownloaded: Boolean,
    val isMultiDisc: Boolean,
    val fileSizeBytes: Long
)
