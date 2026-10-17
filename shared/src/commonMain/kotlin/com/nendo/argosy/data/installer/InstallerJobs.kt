package com.nendo.argosy.data.installer

import com.nendo.argosy.data.remote.github.GitHubAsset

sealed class InstallerJobState {
    data object Idle : InstallerJobState()
    data object Checking : InstallerJobState()
    data class Downloading(val progress: Float) : InstallerJobState()
    data object WaitingForInstall : InstallerJobState()
    data class NeedsVariantChoice(val assets: List<GitHubAsset>) : InstallerJobState()
    data class Failed(val reason: InstallerFailure) : InstallerJobState()
}

enum class InstallerFailure {
    NO_RELEASE,
    NO_APK,
    DOWNLOAD,
    RATE_LIMITED,
    NOT_FOUND
}

data class InstallerJob(val installerId: Long, val state: InstallerJobState)
