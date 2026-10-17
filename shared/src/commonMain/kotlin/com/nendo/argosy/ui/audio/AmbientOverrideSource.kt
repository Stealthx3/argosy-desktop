package com.nendo.argosy.ui.audio

sealed interface AmbientOverrideSource {
    val displayName: String

    data class Local(
        val path: String,
        override val displayName: String
    ) : AmbientOverrideSource

    data class Remote(
        val url: String,
        val headers: Map<String, String>,
        override val displayName: String
    ) : AmbientOverrideSource
}
