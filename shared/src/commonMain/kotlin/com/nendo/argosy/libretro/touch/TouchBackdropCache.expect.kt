package com.nendo.argosy.libretro.touch

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.platform.PlatformBitmap as Bitmap
import java.io.File

expect object TouchBackdropCache {
    fun fileFor(context: Context, platformSlug: String, orientation: Int): File

    fun save(context: Context, platformSlug: String, orientation: Int, bitmap: Bitmap?)

    fun load(context: Context, platformSlug: String, orientation: Int): Bitmap?
}
