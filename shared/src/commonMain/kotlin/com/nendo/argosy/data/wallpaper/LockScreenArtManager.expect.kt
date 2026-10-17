package com.nendo.argosy.data.wallpaper

import kotlinx.coroutines.Job

expect class LockScreenArtManager {
    companion object {}

    fun start()

    suspend fun showBeforeLaunch(gameId: Long)

    fun showLibraryAfterCancelledLaunch()

    fun onLiveWallpaperBound()

    fun prepareLivePreview(): Job
}
