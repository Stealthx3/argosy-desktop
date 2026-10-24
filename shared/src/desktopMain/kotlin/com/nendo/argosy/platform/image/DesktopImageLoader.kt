package com.nendo.argosy.platform.image

import okio.Path.Companion.toOkioPath
import com.nendo.argosy.platform.DesktopDirectories
import coil3.disk.DiskCache
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.nendo.argosy.data.remote.romm.RomMConnectionManager
import com.nendo.argosy.data.remote.romm.RomMImageAuthInterceptor
import com.nendo.argosy.data.remote.ssl.UserCertStore
import com.nendo.argosy.data.remote.ssl.UserCertTrustManager.withUserCertTrust
import okhttp3.OkHttpClient

fun installDesktopImageLoader(rommConnectionManager: RomMConnectionManager, userCertStore: UserCertStore) {
    val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(RomMImageAuthInterceptor(rommConnectionManager))
            .withUserCertTrust(userCertStore)
            .build()
    }
    SingletonImageLoader.setSafe { context ->
        ImageLoader.Builder(context)
            .components {
                add(AndroidAssetFetcher.Factory())
                add(HostAppIconFetcher.Factory())
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(DesktopDirectories.cache.resolve(IMAGE_CACHE_DIR).toOkioPath())
                    .maxSizeBytes(IMAGE_CACHE_BYTES)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}

private const val IMAGE_CACHE_DIR = "image_cache"
private const val IMAGE_CACHE_BYTES = 512L * 1024 * 1024
