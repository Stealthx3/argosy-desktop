package com.nendo.argosy.platform

import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.util.LruCache

actual class PlatformLruCache<K : Any, V : Any> actual constructor(maxSize: Int) {
    private val cache = LruCache<K, V>(maxSize)

    actual fun get(key: K): V? = cache.get(key)

    actual fun put(key: K, value: V): V? = cache.put(key, value)
}

actual class PlatformMediaMetadataRetriever actual constructor() {
    private val retriever = MediaMetadataRetriever()

    actual val embeddedPicture: ByteArray? get() = retriever.embeddedPicture

    actual fun setDataSource(path: String) = retriever.setDataSource(path)

    actual fun release() = retriever.release()
}

actual class PlatformAudioAttributes internal constructor(internal val attributes: AudioAttributes) {
    actual class Builder actual constructor() {
        private val builder = AudioAttributes.Builder()

        actual fun setUsage(usage: Int): Builder = apply { builder.setUsage(usage) }

        actual fun setContentType(contentType: Int): Builder = apply { builder.setContentType(contentType) }

        actual fun build(): PlatformAudioAttributes = PlatformAudioAttributes(builder.build())
    }

    actual companion object {
        actual val USAGE_MEDIA: Int = AudioAttributes.USAGE_MEDIA
        actual val CONTENT_TYPE_MUSIC: Int = AudioAttributes.CONTENT_TYPE_MUSIC
    }
}

actual class PlatformMediaPlayer actual constructor() {
    private val player = MediaPlayer()

    actual fun setAudioAttributes(attributes: PlatformAudioAttributes) = player.setAudioAttributes(attributes.attributes)

    actual fun setDataSource(path: String) = player.setDataSource(path)

    actual fun setDataSource(context: PlatformContext, uri: PlatformUri, headers: Map<String, String>?) =
        player.setDataSource(context, uri, headers)

    actual fun setOnPreparedListener(listener: (PlatformMediaPlayer) -> Unit) =
        player.setOnPreparedListener { listener(this) }

    actual fun setOnCompletionListener(listener: (PlatformMediaPlayer) -> Unit) =
        player.setOnCompletionListener { listener(this) }

    actual fun setOnErrorListener(listener: (PlatformMediaPlayer, Int, Int) -> Boolean) =
        player.setOnErrorListener { _, what, extra -> listener(this, what, extra) }

    actual fun setVolume(leftVolume: Float, rightVolume: Float) = player.setVolume(leftVolume, rightVolume)

    actual fun prepareAsync() = player.prepareAsync()

    actual fun start() = player.start()

    actual fun stop() = player.stop()

    actual fun release() = player.release()
}
