package com.nendo.argosy.platform.image

import coil3.PlatformContext as CoilContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy as CoilCachePolicy
import coil3.request.ImageRequest as CoilImageRequest
import coil3.request.crossfade
import com.nendo.argosy.ui.common.AlwaysCrossfadeFactory

private fun CachePolicy.toCoil(): CoilCachePolicy = when (this) {
    CachePolicy.ENABLED -> CoilCachePolicy.ENABLED
    CachePolicy.READ_ONLY -> CoilCachePolicy.READ_ONLY
    CachePolicy.WRITE_ONLY -> CoilCachePolicy.WRITE_ONLY
    CachePolicy.DISABLED -> CoilCachePolicy.DISABLED
}

fun ImageRequest.toCoil(): CoilImageRequest {
    val builder = CoilImageRequest.Builder(CoilContext.INSTANCE)
    val headers = NetworkHeaders.Builder()
    var hasHeaders = false
    calls.forEach { call ->
        when (call) {
            is BuilderCall.Data -> builder.data(call.data)
            is BuilderCall.Size -> builder.size(call.width, call.height)
            is BuilderCall.MemoryCacheKey -> builder.memoryCacheKey(call.key)
            is BuilderCall.DiskCachePolicy -> builder.diskCachePolicy(call.policy.toCoil())
            is BuilderCall.MemoryCachePolicy -> builder.memoryCachePolicy(call.policy.toCoil())
            is BuilderCall.TransitionFactory -> builder.crossfade((call.factory as AlwaysCrossfadeFactory).durationMillis)
            is BuilderCall.Crossfade -> builder.crossfade(call.enable)
            is BuilderCall.CrossfadeMillis -> builder.crossfade(call.durationMillis)
            is BuilderCall.AllowHardware -> Unit
            is BuilderCall.AddHeader -> {
                headers.add(call.name, call.value)
                hasHeaders = true
            }
        }
    }
    if (hasHeaders) builder.httpHeaders(headers.build())
    return builder.build()
}

internal fun platformImageModel(model: Any?): Any? = if (model is ImageRequest) model.toCoil() else model
