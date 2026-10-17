package com.nendo.argosy.platform.image

import com.nendo.argosy.platform.PlatformBitmap
import com.nendo.argosy.platform.height
import com.nendo.argosy.platform.width

actual abstract class PlatformDrawable {
    internal abstract val width: Int
    internal abstract val height: Int
}

actual val PlatformDrawable.intrinsicWidth: Int get() = width

actual val PlatformDrawable.intrinsicHeight: Int get() = height

actual open class PlatformBitmapDrawable(internal val image: PlatformBitmap) : PlatformDrawable() {
    override val width: Int get() = image.width
    override val height: Int get() = image.height
}

actual val PlatformBitmapDrawable.bitmap: PlatformBitmap? get() = image
