package com.nendo.argosy.platform.image

import coil.ImageLoader
import coil.imageLoader as coilImageLoader
import com.nendo.argosy.platform.PlatformContext

actual class PlatformImageLoader(private val loader: ImageLoader) {
    actual fun enqueue(request: ImageRequest) {
        loader.enqueue(request.toCoil())
    }
}

actual val PlatformContext.imageLoader: PlatformImageLoader get() = PlatformImageLoader(coilImageLoader)
