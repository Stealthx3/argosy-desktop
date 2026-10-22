package com.nendo.argosy.platform

actual class PlatformLruCache<K : Any, V : Any> actual constructor(private val maxSize: Int) {
    private val entries = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean = size > maxSize
    }

    actual fun get(key: K): V? = synchronized(entries) { entries[key] }

    actual fun put(key: K, value: V): V? = synchronized(entries) { entries.put(key, value) }
}

actual class PlatformMediaMetadataRetriever actual constructor() {
    actual val embeddedPicture: ByteArray? get() = null

    actual fun setDataSource(path: String) = Unit

    actual fun release() = Unit
}

actual class PlatformAudioAttributes private constructor() {
    actual class Builder actual constructor() {
        actual fun setUsage(usage: Int): Builder = this

        actual fun setContentType(contentType: Int): Builder = this

        actual fun build(): PlatformAudioAttributes = PlatformAudioAttributes()
    }

    actual companion object {
        actual val USAGE_MEDIA: Int = 1
        actual val CONTENT_TYPE_MUSIC: Int = 2
    }
}

actual class PlatformMediaPlayer actual constructor() {
    private var onError: ((PlatformMediaPlayer, Int, Int) -> Boolean)? = null

    actual fun setAudioAttributes(attributes: PlatformAudioAttributes) = Unit

    actual fun setDataSource(path: String) = Unit

    actual fun setDataSource(context: PlatformContext, uri: PlatformUri, headers: Map<String, String>?) = Unit

    actual fun setOnPreparedListener(listener: (PlatformMediaPlayer) -> Unit) = Unit

    actual fun setOnCompletionListener(listener: (PlatformMediaPlayer) -> Unit) = Unit

    actual fun setOnErrorListener(listener: (PlatformMediaPlayer, Int, Int) -> Boolean) {
        onError = listener
    }

    actual fun setVolume(leftVolume: Float, rightVolume: Float) = Unit

    actual fun prepareAsync() {
        onError?.invoke(this, MEDIA_ERROR_UNKNOWN, 0)
    }

    actual fun start() = Unit

    actual fun stop() = Unit

    actual fun release() {
        onError = null
    }

    private companion object {
        const val MEDIA_ERROR_UNKNOWN = 1
    }
}
