package com.nendo.argosy.platform.image

import coil.request.CachePolicy as CoilCachePolicy
import coil.request.ImageRequest as CoilImageRequest
import coil.transition.Transition

private fun CachePolicy.toCoil(): CoilCachePolicy = when (this) {
    CachePolicy.ENABLED -> CoilCachePolicy.ENABLED
    CachePolicy.READ_ONLY -> CoilCachePolicy.READ_ONLY
    CachePolicy.WRITE_ONLY -> CoilCachePolicy.WRITE_ONLY
    CachePolicy.DISABLED -> CoilCachePolicy.DISABLED
}

fun ImageRequest.toCoil(): CoilImageRequest {
    val builder = CoilImageRequest.Builder(context)
    calls.forEach { call ->
        when (call) {
            is BuilderCall.Data -> builder.data(call.data)
            is BuilderCall.Size -> builder.size(call.width, call.height)
            is BuilderCall.MemoryCacheKey -> builder.memoryCacheKey(call.key)
            is BuilderCall.DiskCachePolicy -> builder.diskCachePolicy(call.policy.toCoil())
            is BuilderCall.MemoryCachePolicy -> builder.memoryCachePolicy(call.policy.toCoil())
            is BuilderCall.TransitionFactory -> builder.transitionFactory(call.factory as Transition.Factory)
            is BuilderCall.Crossfade -> builder.crossfade(call.enable)
            is BuilderCall.CrossfadeMillis -> builder.crossfade(call.durationMillis)
            is BuilderCall.AllowHardware -> builder.allowHardware(call.enable)
            is BuilderCall.AddHeader -> builder.addHeader(call.name, call.value)
        }
    }
    return builder.build()
}

internal fun platformImageModel(model: Any?): Any? = if (model is ImageRequest) model.toCoil() else model
