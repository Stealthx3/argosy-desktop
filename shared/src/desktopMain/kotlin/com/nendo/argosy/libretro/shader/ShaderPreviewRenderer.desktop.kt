package com.nendo.argosy.libretro.shader

import com.nendo.argosy.platform.PlatformBitmap as Bitmap
import com.swordfish.libretrodroid.ShaderConfig
import javax.inject.Inject

actual class ShaderPreviewRenderer @Inject constructor() {
    actual companion object {}

    actual suspend fun render(inputBitmap: Bitmap, shaderConfig: ShaderConfig.Custom, passParams: List<Map<String, Float>>, outputWidth: Int, outputHeight: Int): Bitmap? = null

    actual fun destroy() {
    }
}
