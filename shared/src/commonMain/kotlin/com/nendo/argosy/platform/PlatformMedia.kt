package com.nendo.argosy.platform

expect class PlatformLruCache<K : Any, V : Any>(maxSize: Int) {
    fun get(key: K): V?

    fun put(key: K, value: V): V?
}

expect class PlatformMediaMetadataRetriever() {
    val embeddedPicture: ByteArray?

    fun setDataSource(path: String)

    fun release()
}

expect class PlatformAudioAttributes {
    class Builder() {
        fun setUsage(usage: Int): Builder

        fun setContentType(contentType: Int): Builder

        fun build(): PlatformAudioAttributes
    }

    companion object {
        val USAGE_MEDIA: Int
        val CONTENT_TYPE_MUSIC: Int
    }
}

expect class PlatformMediaPlayer() {
    fun setAudioAttributes(attributes: PlatformAudioAttributes)

    fun setDataSource(path: String)

    fun setDataSource(context: PlatformContext, uri: PlatformUri, headers: Map<String, String>?)

    fun setOnPreparedListener(listener: (PlatformMediaPlayer) -> Unit)

    fun setOnCompletionListener(listener: (PlatformMediaPlayer) -> Unit)

    fun setOnErrorListener(listener: (PlatformMediaPlayer, Int, Int) -> Boolean)

    fun setVolume(leftVolume: Float, rightVolume: Float)

    fun prepareAsync()

    fun start()

    fun stop()

    fun release()
}
