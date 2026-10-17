package com.nendo.argosy.platform.image

import coil3.ImageLoader
import coil3.SingletonImageLoader

fun installDesktopImageLoader() {
    SingletonImageLoader.setSafe { context ->
        ImageLoader.Builder(context)
            .components { add(AndroidAssetFetcher.Factory()) }
            .build()
    }
}
