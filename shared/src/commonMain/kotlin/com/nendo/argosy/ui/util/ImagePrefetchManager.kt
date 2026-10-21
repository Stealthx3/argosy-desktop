package com.nendo.argosy.ui.util

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.platform.image.imageLoader
import com.nendo.argosy.platform.image.ImageRequest
import com.nendo.argosy.ui.common.fileImageModel
import com.nendo.argosy.di.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImagePrefetchManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val imageLoader = context.imageLoader

    fun prefetchBackgrounds(paths: List<String>) {
        paths.forEach { path ->
            try {
                val request = ImageRequest.Builder(context)
                    .data(fileImageModel(path))
                    .size(640, 360)
                    .build()
                imageLoader.enqueue(request)
            } catch (_: Exception) { }
        }
    }
}
