package com.nendo.argosy.platform.image

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
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .crossfade(true)
            .build()
    }
}
