package com.nendo.argosy.platform.image

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.nendo.argosy.data.apps.HostApplications
import com.nendo.argosy.data.apps.HostIcons
import com.nendo.argosy.platform.HostShell
import com.nendo.argosy.ui.coil.AppIconData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.FileSystem

class HostAppIconFetcher(private val data: AppIconData) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val icon = HostApplications.find(data.packageName)?.icon ?: return@withContext null
        val path = HostIcons.pathFor(icon) ?: return@withContext null
        val bytes = HostShell.readBytes(path) ?: return@withContext null
        SourceFetchResult(
            source = ImageSource(Buffer().write(bytes), FileSystem.SYSTEM),
            mimeType = if (path.endsWith(".svg")) "image/svg+xml" else null,
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<AppIconData> {
        override fun create(data: AppIconData, options: Options, imageLoader: ImageLoader): Fetcher =
            HostAppIconFetcher(data)
    }
}
