package com.nendo.argosy.data.cache

data class CachedGameImages(
    val coverPath: String?,
    val backgroundPath: String?,
    val logoPath: String?
)

data class ImageCacheProgress(
    val isProcessing: Boolean = false,
    val currentGameTitle: String = "",
    val currentType: String = "",
    val cachedCount: Int = 0,
    val totalCount: Int = 0
) {
    val progressPercent: Int
        get() = if (totalCount > 0) {
            val percent = cachedCount * 100 / totalCount
            if (isProcessing && percent == 100) 99 else percent
        } else 0
}

data class CacheValidationResult(
    val deletedFiles: Int,
    val clearedPaths: Int
)
