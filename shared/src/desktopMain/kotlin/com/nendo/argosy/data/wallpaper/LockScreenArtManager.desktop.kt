package com.nendo.argosy.data.wallpaper

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Job

@Singleton
actual class LockScreenArtManager @Inject constructor() {
    actual companion object {}

    actual fun start() {
    }

    actual suspend fun showBeforeLaunch(gameId: Long) {
    }

    actual fun showLibraryAfterCancelledLaunch() {
    }

    actual fun onLiveWallpaperBound() {
    }

    actual fun prepareLivePreview(): Job = TODO("desktop: prepareLivePreview")
}
