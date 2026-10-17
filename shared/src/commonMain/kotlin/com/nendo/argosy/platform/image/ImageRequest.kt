package com.nendo.argosy.platform.image

import com.nendo.argosy.platform.PlatformContext
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources

enum class CachePolicy {
    ENABLED,
    READ_ONLY,
    WRITE_ONLY,
    DISABLED
}

class ImageRequest internal constructor(
    val context: PlatformContext,
    internal val calls: List<BuilderCall>,
) {
    val data: Any? get() = calls.filterIsInstance<BuilderCall.Data>().lastOrNull()?.data

    override fun equals(other: Any?): Boolean =
        other is ImageRequest && other.context == context && other.calls == calls

    override fun hashCode(): Int = 31 * context.hashCode() + calls.hashCode()

    class Builder(private val context: PlatformContext) {
        private val calls = mutableListOf<BuilderCall>()

        fun data(data: Any?): Builder = apply { calls += BuilderCall.Data(data) }
        fun size(size: Int): Builder = apply { calls += BuilderCall.Size(size, size) }
        fun size(width: Int, height: Int): Builder = apply { calls += BuilderCall.Size(width, height) }
        fun memoryCacheKey(key: String?): Builder = apply { calls += BuilderCall.MemoryCacheKey(key) }
        fun diskCachePolicy(policy: CachePolicy): Builder = apply { calls += BuilderCall.DiskCachePolicy(policy) }
        fun memoryCachePolicy(policy: CachePolicy): Builder = apply { calls += BuilderCall.MemoryCachePolicy(policy) }
        fun transitionFactory(factory: Any): Builder = apply { calls += BuilderCall.TransitionFactory(factory) }
        fun crossfade(enable: Boolean): Builder = apply { calls += BuilderCall.Crossfade(enable) }
        fun crossfade(durationMillis: Int): Builder = apply { calls += BuilderCall.CrossfadeMillis(durationMillis) }
        fun allowHardware(enable: Boolean): Builder = apply { calls += BuilderCall.AllowHardware(enable) }
        fun addHeader(name: String, value: String): Builder = apply { calls += BuilderCall.AddHeader(name, value) }
        fun build(): ImageRequest = ImageRequest(context, calls.toList())
    }
}

internal sealed interface BuilderCall {
    data class Data(val data: Any?) : BuilderCall
    data class Size(val width: Int, val height: Int) : BuilderCall
    data class MemoryCacheKey(val key: String?) : BuilderCall
    data class DiskCachePolicy(val policy: CachePolicy) : BuilderCall
    data class MemoryCachePolicy(val policy: CachePolicy) : BuilderCall
    data class TransitionFactory(val factory: Any) : BuilderCall
    data class Crossfade(val enable: Boolean) : BuilderCall
    data class CrossfadeMillis(val durationMillis: Int) : BuilderCall
    data class AllowHardware(val enable: Boolean) : BuilderCall
    data class AddHeader(val name: String, val value: String) : BuilderCall
}
