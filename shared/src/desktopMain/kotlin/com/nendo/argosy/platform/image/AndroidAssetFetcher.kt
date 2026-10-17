package com.nendo.argosy.platform.image

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.nendo.argosy.platform.PlatformAssets
import okio.FileSystem
import okio.buffer
import okio.source

class AndroidAssetFetcher(private val path: String, private val assets: PlatformAssets) : Fetcher {

    override suspend fun fetch(): FetchResult = SourceFetchResult(
        source = ImageSource(assets.stream(path).source().buffer(), FileSystem.SYSTEM),
        mimeType = null,
        dataSource = DataSource.DISK,
    )

    class Factory(private val assets: PlatformAssets = PlatformAssets()) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val path = data.path ?: return null
            if (data.scheme != "file" || !path.startsWith(PREFIX)) return null
            return AndroidAssetFetcher(path.removePrefix(PREFIX), assets)
        }
    }

    companion object {
        const val PREFIX = "/android_asset/"
    }
}
