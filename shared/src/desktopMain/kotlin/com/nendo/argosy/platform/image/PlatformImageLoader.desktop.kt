package com.nendo.argosy.platform.image

import coil3.ImageLoader
import coil3.PlatformContext as CoilContext
import coil3.SingletonImageLoader
import com.nendo.argosy.platform.PlatformContext

actual class PlatformImageLoader(private val loader: ImageLoader) {
    actual fun enqueue(request: ImageRequest) {
        loader.enqueue(request.toCoil())
    }
}

actual val PlatformContext.imageLoader: PlatformImageLoader
    get() = PlatformImageLoader(SingletonImageLoader.get(CoilContext.INSTANCE))
